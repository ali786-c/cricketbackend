<?php

namespace App\Modules\Scoring\Services;

use App\Models\CricketMatch;
use App\Models\InningsBattingStat;
use App\Models\InningsBowlingStat;
use App\Models\MatchDelivery;
use App\Models\MatchInnings;
use App\Models\MatchWicket;
use Illuminate\Database\DatabaseManager;
use Illuminate\Support\Collection;
use Illuminate\Validation\ValidationException;

class MatchScoringService
{
    private const DISMISSALS = [
        'bowled', 'caught', 'caught_and_bowled', 'lbw', 'run_out', 'stumped',
        'hit_wicket', 'retired_hurt', 'retired_out', 'obstructing_the_field',
        'hit_the_ball_twice', 'timed_out', 'absent',
    ];

    public function __construct(private readonly DatabaseManager $database)
    {
    }

    public function recordDelivery(CricketMatch $match, array $data, int $actorId, ?int $expectedRevision = null): MatchDelivery
    {
        $delivery = $this->database->transaction(function () use ($match, $data, $actorId, $expectedRevision) {
            $match = CricketMatch::query()->with('ruleProfile')->lockForUpdate()->findOrFail($match->id);
            if ($match->status !== 'live' || ! $match->current_innings_id) {
                $this->fail('match', 'Scoring is only available for a live match.');
            }
            if ($expectedRevision !== null && (int) $match->revision !== $expectedRevision) {
                $this->fail('revision', 'This scoring screen is stale. Refresh the match before recording the ball.');
            }

            $innings = MatchInnings::query()->lockForUpdate()->findOrFail($match->current_innings_id);
            if ($innings->status !== 'live') {
                $this->fail('innings', 'The current innings is not live.');
            }
            $profile = $match->ruleProfile;

            // Calculate dynamic over number and ball number
            $latestDelivery = $innings->deliveries()->whereNull('voided_at')->orderBy('sequence_number', 'desc')->first();
            if (! $latestDelivery) {
                $overNumber = 1;
                $ballNumber = 1;
            } else {
                $currentOverNum = $latestDelivery->over_number;
                $currentOverDeliveries = $innings->deliveries()
                    ->whereNull('voided_at')
                    ->where('over_number', $currentOverNum)
                    ->get();
                
                $legalInCurrent = $currentOverDeliveries->where('is_legal_delivery', true)->count();
                $totalInCurrent = $currentOverDeliveries->count();
                
                $overIsComplete = ($legalInCurrent >= (int) $profile->legal_balls_per_over)
                    || ($profile->max_balls_per_over !== null && $totalInCurrent >= (int) $profile->max_balls_per_over);
                
                if ($overIsComplete) {
                    $overNumber = $currentOverNum + 1;
                    $ballNumber = 1;
                } else {
                    $overNumber = $currentOverNum;
                    $ballNumber = $legalInCurrent + 1;
                }
            }

            if ($overNumber > (int) $innings->maximum_overs) {
                $this->fail('innings', 'The maximum overs for this innings are complete.');
            }

            if ($profile->max_runs_per_over !== null && $latestDelivery && $overNumber === $latestDelivery->over_number) {
                $runsInCurrentOver = $innings->deliveries()
                    ->whereNull('voided_at')
                    ->where('over_number', $latestDelivery->over_number)
                    ->sum('total_runs');
                
                if ($runsInCurrentOver >= (int) $profile->max_runs_per_over) {
                    $this->fail('innings', 'The maximum runs limit for this over has been reached.');
                }
            }

            $striker = $innings->match->players()->whereKey((int) ($data['striker_id'] ?? 0))->where('team_id', $innings->batting_team_id)->where('selection_type', 'playing_xi')->first();
            $nonStriker = $innings->match->players()->whereKey((int) ($data['non_striker_id'] ?? 0))->where('team_id', $innings->batting_team_id)->where('selection_type', 'playing_xi')->first();
            $bowler = $innings->match->players()->whereKey((int) ($data['bowler_id'] ?? 0))->where('team_id', $innings->bowling_team_id)->where('selection_type', 'playing_xi')->first();
            if (! $striker || ! $nonStriker || $striker->id === $nonStriker->id) $this->fail('players', 'Select two different batting players from the playing XI.');
            if (! $bowler) $this->fail('bowler_id', 'Select a bowler from the opposing playing XI.');
            if ($innings->current_striker_id && (int) $innings->current_striker_id !== (int) $striker->id) $this->fail('striker_id', 'The striker does not match the current innings state.');
            if ($innings->current_non_striker_id && (int) $innings->current_non_striker_id !== (int) $nonStriker->id) $this->fail('non_striker_id', 'The non-striker does not match the current innings state.');
            if ($innings->current_bowler_id && (int) $innings->current_bowler_id !== (int) $bowler->id) $this->fail('bowler_id', 'The bowler cannot change during an over.');
            $bowlerLegalBalls = $innings->deliveries()->whereNull('voided_at')->where('bowler_id', $bowler->id)->where('is_legal_delivery', true)->count();
            if ($bowlerLegalBalls >= ((int) $profile->max_overs_per_bowler * (int) $profile->legal_balls_per_over)) {
                $this->fail('bowler_id', 'This bowler has reached the maximum overs allowed by the rule profile.');
            }

            $runs = $this->normaliseRuns($data);
            $wicketInput = $data['wicket'] ?? null;
            $this->validateExtras($runs, $profile);
            $this->validateWicket($wicketInput, $runs, $innings, $match);

            $legal = $runs['wides'] === 0 && $runs['no_balls'] === 0;
            $sequence = ((int) $innings->deliveries()->whereNull('voided_at')->max('sequence_number')) + 1;
            $delivery = MatchDelivery::create([
                'match_id' => $match->id,
                'innings_id' => $innings->id,
                'over_number' => $overNumber,
                'ball_number' => $ballNumber,
                'sequence_number' => $sequence,
                'striker_id' => $striker->id,
                'non_striker_id' => $nonStriker->id,
                'bowler_id' => $bowler->id,
                ...$runs,
                'total_runs' => array_sum($runs),
                'is_legal_delivery' => $legal,
                'commentary' => $data['commentary'] ?? null,
                'recorded_by' => $actorId,
                'recorded_at' => now(),
                'revision' => $match->revision + 1,
                'local_uuid' => $data['local_uuid'] ?? null,
                'device_timestamp' => isset($data['device_timestamp']) ? \Illuminate\Support\Carbon::parse($data['device_timestamp']) : null,
                'wagon_x' => isset($data['wagon_x']) ? (float) $data['wagon_x'] : null,
                'wagon_y' => isset($data['wagon_y']) ? (float) $data['wagon_y'] : null,
            ]);

            if ($wicketInput) {
                $dismissed = $match->players()->whereKey((int) $wicketInput['dismissed_player_id'])->firstOrFail();
                $wicket = MatchWicket::create([
                    'delivery_id' => $delivery->id,
                    'dismissed_player_id' => $dismissed->id,
                    'dismissal_type' => $wicketInput['dismissal_type'],
                    'credited_bowler_id' => $this->bowlerGetsCredit($wicketInput['dismissal_type']) ? $bowler->id : null,
                    'fielder_id' => $wicketInput['fielder_id'] ?? null,
                    'runs_completed' => (int) ($wicketInput['runs_completed'] ?? 0),
                    'is_valid_wicket' => true,
                    'notes' => $wicketInput['notes'] ?? null,
                ]);
                $delivery->update(['wicket_id' => $wicket->id]);
            }

            [$nextStrikerId, $nextNonStrikerId] = $this->nextBatters($striker->id, $nonStriker->id, $runs, $wicketInput);
            $legalBallsAfter = (int) $innings->legal_balls + ($legal ? 1 : 0);
            $deliveriesInOver = $innings->deliveries()->whereNull('voided_at')->where('over_number', $overNumber)->count();
            $overComplete = ($legal && $legalBallsAfter % (int) $profile->legal_balls_per_over === 0)
                || ($profile->max_balls_per_over !== null && $deliveriesInOver >= (int) $profile->max_balls_per_over);
            if ($overComplete) {
                [$nextStrikerId, $nextNonStrikerId] = [$nextNonStrikerId, $nextStrikerId];
            }

            $innings->update([
                'total_runs' => $innings->total_runs + $delivery->total_runs,
                'wickets' => $innings->wickets + ($wicketInput ? 1 : 0),
                'legal_balls' => $legalBallsAfter,
                'current_striker_id' => $nextStrikerId,
                'current_non_striker_id' => $nextNonStrikerId,
                'current_bowler_id' => $overComplete ? null : $bowler->id,
            ]);
            $match->update(['revision' => $match->revision + 1, 'last_event_at' => now(), 'updated_by' => $actorId]);
            $this->rebuildStats($innings->fresh(['match']), $match->ruleProfile);
            $this->completeIfRequired($match->fresh(), $innings->fresh(), $profile);

            return $delivery->fresh(['striker', 'nonStriker', 'bowler', 'wicket.dismissedPlayer']);
        });

        event(new \App\Modules\Scoring\Events\DeliveryRecorded($delivery));

        return $delivery;
    }

    public function startNextInnings(CricketMatch $match, int $actorId): MatchInnings
    {
        return $this->database->transaction(function () use ($match, $actorId) {
            $match = CricketMatch::query()->with('ruleProfile')->lockForUpdate()->findOrFail($match->id);
            $current = MatchInnings::query()->lockForUpdate()->findOrFail($match->current_innings_id);
            if ($current->status !== 'completed') $this->fail('innings', 'The current innings must be completed before starting the next innings.');
            $totalInnings = (int) $match->ruleProfile->innings_per_side * 2;
            if ((int) $current->innings_number >= $totalInnings) $this->fail('innings', 'All innings for this match are already complete.');

            $nextNumber = $current->innings_number + 1;
            $openingBatters = $match->players()
                ->where('team_id', $current->bowling_team_id)
                ->where('selection_type', 'playing_xi')
                ->orderBy('batting_order')
                ->take(2)
                ->get();
            $innings = MatchInnings::create([
                'match_id' => $match->id,
                'innings_number' => $nextNumber,
                'batting_team_id' => $current->bowling_team_id,
                'bowling_team_id' => $current->batting_team_id,
                'current_striker_id' => $openingBatters->get(0)?->id,
                'current_non_striker_id' => $openingBatters->get(1)?->id,
                'status' => 'live',
                'target_runs' => $current->total_runs + 1,
                'maximum_overs' => (int) ($match->overs_per_innings ?: $match->ruleProfile->overs_per_innings),
                'started_at' => now(),
            ]);
            $match->update(['current_innings_id' => $innings->id, 'status' => 'live', 'revision' => $match->revision + 1, 'last_event_at' => now(), 'updated_by' => $actorId]);
            return $innings;
        });
    }

    public function undoLastDelivery(CricketMatch $match, int $actorId, string $reason): void
    {
        $this->database->transaction(function () use ($match, $actorId, $reason) {
            $match = CricketMatch::query()->with('ruleProfile')->lockForUpdate()->findOrFail($match->id);
            if (! in_array($match->status, ['live', 'innings_break'], true) || ! $match->current_innings_id) $this->fail('match', 'Only a live match delivery can be undone.');
            $innings = MatchInnings::query()->lockForUpdate()->findOrFail($match->current_innings_id);
            $delivery = $innings->deliveries()->whereNull('voided_at')->latest('sequence_number')->first();
            if (! $delivery) $this->fail('delivery', 'There is no delivery to undo.');
            $delivery->update(['voided_at' => now(), 'void_reason' => $reason, 'revision' => $delivery->revision + 1]);
            $this->recalculateInningsCache($innings);
            $innings->update([
                'current_striker_id' => $delivery->striker_id,
                'current_non_striker_id' => $delivery->non_striker_id,
                'current_bowler_id' => $delivery->bowler_id,
            ]);
            $this->rebuildStats($innings->fresh(['match']), $match->ruleProfile);
            $match->update(['status' => 'live', 'revision' => $match->revision + 1, 'last_event_at' => now(), 'updated_by' => $actorId]);
        });

        event(new \App\Modules\Scoring\Events\DeliveryUndone($match->fresh()));
    }

    public function rebuildStats(MatchInnings $innings, $profile = null): void
    {
        $profile ??= $innings->match->ruleProfile;
        $deliveries = $innings->deliveries()->with(['wicket', 'wicket.dismissedPlayer'])->whereNull('voided_at')->get();
        $players = $innings->match->players()->where('selection_type', 'playing_xi')->get();
        foreach ($players->where('team_id', $innings->batting_team_id) as $player) {
            $bat = $deliveries->where('striker_id', $player->id);
            $wicket = $deliveries->pluck('wicket')->filter(fn ($w) => $w && $w->dismissed_player_id === $player->id)->first();
            $runs = $bat->sum('runs_off_bat');
            if ($profile?->wide_runs_to_batsman) {
                $runs += $bat->sum('wides');
            }
            if ($profile?->noball_runs_to_batsman) {
                $runs += $bat->sum('no_balls');
            }
            $balls = $bat->where('is_legal_delivery', true)->count();
            InningsBattingStat::updateOrCreate(['innings_id' => $innings->id, 'match_player_id' => $player->id], [
                'batting_position' => $player->batting_order,
                'runs' => $runs,
                'balls' => $balls,
                'fours' => $bat->where('runs_off_bat', 4)->count(),
                'sixes' => $bat->where('runs_off_bat', 6)->count(),
                'strike_rate' => $balls > 0 ? round(($runs / $balls) * 100, 2) : 0,
                'dismissal_type' => $wicket?->dismissal_type,
                'dismissed_by' => $wicket?->credited_bowler_id,
                'fielder_id' => $wicket?->fielder_id,
                'status' => $wicket ? 'out' : ($bat->isNotEmpty() ? 'not_out' : 'did_not_bat'),
            ]);
        }

        foreach ($players->where('team_id', $innings->bowling_team_id) as $player) {
            $balls = $deliveries->where('bowler_id', $player->id);
            $legalBalls = $balls->where('is_legal_delivery', true)->count();
            $conceded = $balls->sum(fn ($delivery) => $delivery->total_runs - $delivery->byes - $delivery->leg_byes - $delivery->penalty_runs);
            $wickets = $balls->pluck('wicket')->filter(fn ($w) => $w && $w->is_valid_wicket && $w->credited_bowler_id === $player->id)->count();
            InningsBowlingStat::updateOrCreate(['innings_id' => $innings->id, 'match_player_id' => $player->id], [
                'legal_balls' => $legalBalls,
                'maidens' => $this->maidens($balls),
                'runs_conceded' => $conceded,
                'wickets' => $wickets,
                'no_balls' => $balls->sum('no_balls'),
                'wides' => $balls->sum('wides'),
                'economy' => $legalBalls > 0 ? round(($conceded / ($legalBalls / (int) ($profile?->legal_balls_per_over ?: 6))), 2) : 0,
            ]);
        }
    }

    private function recalculateInningsCache(MatchInnings $innings): void
    {
        $deliveries = $innings->deliveries()->whereNull('voided_at')->with('wicket')->get();
        $innings->update([
            'total_runs' => $deliveries->sum('total_runs'),
            'wickets' => $deliveries->pluck('wicket')->filter(fn ($w) => $w && $w->is_valid_wicket)->count(),
            'legal_balls' => $deliveries->where('is_legal_delivery', true)->count(),
            'status' => 'live',
            'completed_reason' => null,
            'completed_at' => null,
        ]);
    }

    private function completeIfRequired(CricketMatch $match, MatchInnings $innings, $profile): void
    {
        $reason = null;
        if ($innings->target_runs !== null && $innings->total_runs >= $innings->target_runs) $reason = 'target_reached';
        elseif ($innings->wickets >= ($profile->last_man_standing ? $profile->playing_xi_size : $profile->maximum_wickets)) $reason = 'all_out';
        elseif ($innings->legal_balls >= $innings->maximum_overs * $profile->legal_balls_per_over) $reason = 'overs_complete';
        if ($reason) {
            $innings->update(['status' => 'completed', 'completed_reason' => $reason, 'completed_at' => now()]);
            if ((int) $innings->innings_number >= ((int) $profile->innings_per_side * 2)) {
                $match->update(['status' => 'completed', 'completed_at' => now(), 'revision' => $match->revision + 1, 'last_event_at' => now()]);
            }
        }
    }

    private function normaliseRuns(array $data): array
    {
        return [
            'runs_off_bat' => (int) ($data['runs_off_bat'] ?? 0),
            'wides' => (int) ($data['wides'] ?? 0),
            'no_balls' => (int) ($data['no_balls'] ?? 0),
            'byes' => (int) ($data['byes'] ?? 0),
            'leg_byes' => (int) ($data['leg_byes'] ?? 0),
            'penalty_runs' => (int) ($data['penalty_runs'] ?? 0),
        ];
    }

    private function nextBatters(int $strikerId, int $nonStrikerId, array $runs, ?array $wicket): array
    {
        $runningRuns = $runs['runs_off_bat'] + $runs['byes'] + $runs['leg_byes'];
        if ($runningRuns % 2 === 1) {
            [$strikerId, $nonStrikerId] = [$nonStrikerId, $strikerId];
        }

        if ($wicket) {
            $dismissedId = (int) ($wicket['dismissed_player_id'] ?? 0);
            if ($strikerId === $dismissedId) $strikerId = 0;
            if ($nonStrikerId === $dismissedId) $nonStrikerId = 0;
        }

        return [$strikerId ?: null, $nonStrikerId ?: null];
    }

    private function validateExtras(array $runs, $profile): void
    {
        foreach ($runs as $value) if ($value < 0 || $value > 6) $this->fail('runs', 'Each delivery run component must be between 0 and 6.');
        if ($runs['wides'] > 0 && ($runs['no_balls'] || $runs['byes'] || $runs['leg_byes'] || $runs['runs_off_bat'])) $this->fail('extras', 'A wide cannot also contain bat runs, no-balls, byes, or leg-byes.');
        if ($runs['no_balls'] > 0 && ($runs['wides'] || $runs['byes'] || $runs['leg_byes'])) $this->fail('extras', 'A no-ball cannot also contain wides, byes, or leg-byes.');
        if (($runs['byes'] > 0 && $runs['leg_byes'] > 0) || ($runs['byes'] > 0 && $runs['runs_off_bat'] > 0) || ($runs['leg_byes'] > 0 && $runs['runs_off_bat'] > 0)) $this->fail('extras', 'Choose one valid scoring category for byes or leg-byes.');
        if ($runs['wides'] > 0 && $runs['wides'] < $profile->wide_runs) $this->fail('extras', 'A wide must include the configured wide penalty.');
        if ($runs['no_balls'] > 0 && $runs['no_balls'] < $profile->no_ball_runs) $this->fail('extras', 'A no-ball must include the configured no-ball penalty.');
    }

    private function validateWicket(?array $wicket, array $runs, MatchInnings $innings, CricketMatch $match): void
    {
        if (! $wicket) return;
        $type = $wicket['dismissal_type'] ?? '';
        if (! in_array($type, self::DISMISSALS, true)) $this->fail('wicket.dismissal_type', 'Choose a supported dismissal type.');
        if ($runs['no_balls'] > 0 && ! in_array($type, ['hit_the_ball_twice', 'obstructing_the_field', 'run_out'], true)) $this->fail('wicket.dismissal_type', 'This dismissal is not permitted on a no-ball.');
        if ($runs['wides'] > 0 && ! in_array($type, ['hit_wicket', 'obstructing_the_field', 'run_out', 'stumped'], true)) $this->fail('wicket.dismissal_type', 'This dismissal is not permitted on a wide.');
        $player = $match->players()->whereKey((int) ($wicket['dismissed_player_id'] ?? 0))->where('team_id', $innings->batting_team_id)->where('selection_type', 'playing_xi')->first();
        if (! $player) $this->fail('wicket.dismissed_player_id', 'The dismissed player must be in the batting playing XI.');
        $alreadyOut = MatchWicket::query()->where('dismissed_player_id', $player->id)->whereHas('delivery', fn ($q) => $q->where('innings_id', $innings->id)->whereNull('voided_at'))->exists();
        if ($alreadyOut) $this->fail('wicket.dismissed_player_id', 'That player has already been dismissed.');
    }

    private function bowlerGetsCredit(string $type): bool
    {
        return in_array($type, ['bowled', 'caught', 'caught_and_bowled', 'lbw', 'stumped', 'hit_wicket'], true);
    }

    private function maidens(Collection $balls): int
    {
        return $balls->groupBy('over_number')->filter(fn ($over) => $over->sum('total_runs') === 0 && $over->where('is_legal_delivery', true)->count() > 0)->count();
    }

    private function fail(string $key, string $message): never
    {
        throw ValidationException::withMessages([$key => $message]);
    }
}
