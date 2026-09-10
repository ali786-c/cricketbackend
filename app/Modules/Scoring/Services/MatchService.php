<?php

namespace App\Modules\Scoring\Services;

use App\Models\AuditLog;
use App\Models\CricketMatch;
use App\Models\CricketRuleProfile;
use App\Models\Draft;
use App\Models\MatchInnings;
use App\Models\MatchPlayer;
use App\Models\Team;
use App\Models\Tournament;
use Illuminate\Database\DatabaseManager;
use Illuminate\Support\Carbon;
use Illuminate\Validation\ValidationException;

class MatchService
{
    public function __construct(private readonly DatabaseManager $database)
    {
    }

    public function createFromTeams(?Tournament $tournament, int $homeTeamId, int $awayTeamId, ?int $fixtureId, int $actorId, ?int $oversPerInnings = null, ?array $configuration = null, ?string $clientUuid = null): CricketMatch
    {
        return $this->database->transaction(function () use ($tournament, $homeTeamId, $awayTeamId, $fixtureId, $actorId, $oversPerInnings, $configuration, $clientUuid) {
            $profile = null;
            if ($tournament) {
                $tournament = Tournament::query()->with('cricketRuleProfile')->lockForUpdate()->findOrFail($tournament->id);
                $profile = $tournament->cricketRuleProfile;

                if (! $profile || ! $profile->is_active) {
                    $this->fail('configuration', 'Select an active cricket rule profile before creating a tournament match.');
                }
            } else {
                $profile = $configuration
                    ? $this->createCustomProfile($configuration, $clientUuid)
                    : CricketRuleProfile::query()->where('slug', 'default-custom')->first();
                if (! $profile) $this->fail('configuration', 'Custom match configuration is required.');
            }

            $teamIds = collect([$homeTeamId, $awayTeamId])->map(fn ($id) => (int) $id);
            if ($teamIds->count() !== 2 || $teamIds->unique()->count() !== 2) {
                $this->fail('teams', 'A match requires two different teams.');
            }

            if ($tournament) {
                $teams = $tournament->teams()->whereIn('teams.id', $teamIds)->orderBy('teams.display_order')->get();
                if ($teams->count() !== 2) {
                    $this->fail('teams', 'Both teams must belong to this tournament.');
                }

                $selectedPicks = collect();
                if ($tournament->has_draft) {
                    $draft = $tournament->draft()->with(['picks.tournamentPlayer.playerProfile'])->lockForUpdate()->first();
                    if (! $draft || ! $this->draftIsComplete($draft)) {
                        $this->fail('match', 'The tournament draft must be completed before a match can be created.');
                    }
                    $selectedPicks = $draft->picks->where('status', 'selected')->whereIn('team_id', $teamIds)->values();
                    if ($selectedPicks->isEmpty() || $selectedPicks->pluck('team_id')->unique()->count() !== 2) {
                        $this->fail('teams', 'Both teams must have drafted players before a match can be created.');
                    }
                    $playerIds = $selectedPicks->pluck('tournament_player_id')->filter();
                    if ($playerIds->count() !== $playerIds->unique()->count()) {
                        $this->fail('players', 'A drafted player cannot appear twice in a match squad.');
                    }
                    foreach ($selectedPicks as $pick) {
                        if (! $pick->tournamentPlayer || $pick->tournamentPlayer->status !== 'approved') {
                            $this->fail('players', 'Every match player must be an approved tournament player.');
                        }
                    }
                    foreach ($teamIds as $teamId) {
                        if ($selectedPicks->where('team_id', $teamId)->count() < (int) $profile->playing_xi_size) {
                            $this->fail('players', 'Each drafted team needs at least '.$profile->playing_xi_size.' approved players before match creation.');
                        }
                    }
                }
            } else {
                $teams = \App\Models\Team::whereIn('id', $teamIds)->get();
                if ($teams->count() !== 2) {
                    $this->fail('teams', 'Both custom teams must exist.');
                }
            }

            $effectiveOvers = $oversPerInnings
                ?? ($tournament ? $tournament->default_overs_per_innings : $profile->overs_per_innings)
                ?? $profile->overs_per_innings;
            if ($effectiveOvers < 1 || $effectiveOvers > 100) {
                $this->fail('overs_per_innings', 'Overs per innings must be between 1 and 100.');
            }

            $match = CricketMatch::create([
                'fixture_id' => $fixtureId,
                'client_uuid' => $clientUuid,
                'tournament_id' => $tournament ? $tournament->id : null,
                'rule_profile_id' => $profile->id,
                'rule_profile_version' => $profile->version,
                'rule_snapshot' => $this->profileSnapshot($profile, $tournament?->ball_type ?? ($configuration['ball_type'] ?? null)),
                'overs_per_innings' => $effectiveOvers,
                'status' => 'squad_selection',
                'revision' => 1,
                'last_event_at' => now(),
                'created_by' => $actorId,
                'updated_by' => $actorId,
            ]);

            if ($tournament) {
                foreach ($selectedPicks as $pick) {
                    $profileData = $pick->tournamentPlayer->playerProfile;
                    MatchPlayer::create([
                        'match_id' => $match->id,
                        'team_id' => $pick->team_id,
                        'tournament_player_id' => $pick->tournament_player_id,
                        'draft_pick_id' => $pick->id,
                        'player_name_snapshot' => $profileData->full_name,
                        'player_role_snapshot' => $profileData->playing_role,
                        'selection_type' => 'squad',
                    ]);
                }
            }

            return $match->load(['players.team', 'ruleProfile']);
        });
    }

    private function createCustomProfile(array $configuration, ?string $clientUuid): CricketRuleProfile
    {
        $slug = 'custom-match-'.($clientUuid ?: (string) \Illuminate\Support\Str::uuid());
        return CricketRuleProfile::create([
            'name' => 'Custom Match Rules',
            'slug' => $slug,
            'format' => strtolower($configuration['format']),
            'innings_per_side' => $configuration['innings_per_side'],
            'overs_per_innings' => $configuration['overs_per_innings'],
            'playing_xi_size' => $configuration['playing_xi_size'],
            'maximum_wickets' => $configuration['maximum_wickets'],
            'legal_balls_per_over' => $configuration['legal_balls_per_over'],
            'max_overs_per_bowler' => $configuration['max_overs_per_bowler'] ?? null,
            'no_ball_runs' => $configuration['no_ball_runs'] ?? 1,
            'wide_runs' => $configuration['wide_runs'] ?? 1,
            'wide_runs_to_batsman' => $configuration['wide_runs_to_batsman'] ?? false,
            'noball_runs_to_batsman' => $configuration['noball_runs_to_batsman'] ?? false,
            'last_man_standing' => $configuration['last_man_standing'] ?? false,
            'max_balls_per_over' => $configuration['max_balls_per_over'] ?? null,
            'max_runs_per_over' => $configuration['max_runs_per_over'] ?? null,
            'version' => 1,
            'is_system' => false,
            'is_active' => true,
        ]);
    }

    private function profileSnapshot(CricketRuleProfile $profile, ?string $ballType): array
    {
        return [
            'profile_id' => $profile->id,
            'version' => $profile->version,
            'format' => strtolower($profile->format),
            'innings_per_side' => $profile->innings_per_side,
            'overs_per_innings' => $profile->overs_per_innings,
            'playing_xi_size' => $profile->playing_xi_size,
            'maximum_wickets' => $profile->maximum_wickets,
            'legal_balls_per_over' => $profile->legal_balls_per_over,
            'max_overs_per_bowler' => $profile->max_overs_per_bowler,
            'ball_type' => $ballType ? strtolower($ballType) : null,
            'no_ball_runs' => $profile->no_ball_runs,
            'wide_runs' => $profile->wide_runs,
            'wide_runs_to_batsman' => $profile->wide_runs_to_batsman,
            'noball_runs_to_batsman' => $profile->noball_runs_to_batsman,
            'last_man_standing' => $profile->last_man_standing,
            'max_balls_per_over' => $profile->max_balls_per_over,
            'max_runs_per_over' => $profile->max_runs_per_over,
        ];
    }

    public function updateOversPerInnings(CricketMatch $match, int $oversPerInnings, int $actorId): CricketMatch
    {
        return $this->database->transaction(function () use ($match, $oversPerInnings, $actorId) {
            $locked = CricketMatch::query()->lockForUpdate()->findOrFail($match->id);
            if (in_array($locked->status, ['live', 'innings_break', 'completed', 'result_pending', 'approved', 'rejected', 'abandoned', 'cancelled'], true)) {
                $this->fail('overs_per_innings', 'The overs limit is locked after the match starts.');
            }
            if ($oversPerInnings < 1 || $oversPerInnings > 100) {
                $this->fail('overs_per_innings', 'Overs per innings must be between 1 and 100.');
            }

            $before = ['overs_per_innings' => $locked->overs_per_innings];
            $locked->update(['overs_per_innings' => $oversPerInnings]);
            AuditLog::create([
                'user_id' => $actorId,
                'tournament_id' => $locked->tournament_id,
                'action' => 'match.overs_updated',
                'auditable_type' => CricketMatch::class,
                'auditable_id' => $locked->id,
                'before' => $before,
                'after' => ['overs_per_innings' => $oversPerInnings],
                'metadata' => ['source' => 'admin', 'match_id' => $locked->id],
            ]);

            return $locked->fresh(['players.team', 'ruleProfile', 'fixture']);
        });
    }

    public function submitPlayingXi(CricketMatch $match, int $teamId, array $playerIds, int $actorId): CricketMatch
    {
        return $this->database->transaction(function () use ($match, $teamId, $playerIds, $actorId) {
            $match = CricketMatch::query()->with('ruleProfile')->lockForUpdate()->findOrFail($match->id);
            if (! in_array($match->status, ['squad_selection', 'lineup_pending'], true)) {
                $this->fail('match', 'Playing XI cannot be changed after lineup approval begins.');
            }

            $playerIds = collect($playerIds)->map(fn ($id) => (int) $id)->filter()->unique()->values();
            if ($playerIds->count() !== (int) $match->ruleProfile->playing_xi_size) {
                $this->fail('players', 'Select exactly '.$match->ruleProfile->playing_xi_size.' players for the playing XI.');
            }

            $teamPlayers = $match->players()->where('team_id', $teamId)->get();
            if ($teamPlayers->count() === 0 || $playerIds->diff($teamPlayers->pluck('id'))->isNotEmpty()) {
                $this->fail('players', 'Every selected player must belong to this match team.');
            }

            $match->players()->where('team_id', $teamId)->update([
                'selection_type' => 'squad',
                'batting_order' => null,
                'approved_at' => null,
                'approved_by' => null,
            ]);
            foreach ($playerIds as $position => $matchPlayerId) {
                $match->players()->where('id', $matchPlayerId)->update([
                    'selection_type' => 'playing_xi',
                    'batting_order' => $position + 1,
                ]);
            }

            $match->update([
                'status' => 'lineup_pending',
                'updated_by' => $actorId,
                'revision' => $match->revision + 1,
                'last_event_at' => now(),
            ]);
            return $match->fresh(['players.team', 'ruleProfile']);
        });
    }

    public function approveLineup(CricketMatch $match, int $actorId): CricketMatch
    {
        return $this->database->transaction(function () use ($match, $actorId) {
            $match = CricketMatch::query()->with(['ruleProfile', 'players'])->lockForUpdate()->findOrFail($match->id);
            if (! in_array($match->status, ['squad_selection', 'lineup_pending'], true)) {
                $this->fail('match', 'This match is not awaiting lineup approval.');
            }
            $teamIds = $match->players->pluck('team_id')->unique()->values();
            if ($teamIds->count() !== 2) {
                $this->fail('teams', 'A match lineup requires exactly two teams.');
            }
            foreach ($teamIds as $teamId) {
                if ($match->players->where('team_id', $teamId)->where('selection_type', 'playing_xi')->count() !== (int) $match->ruleProfile->playing_xi_size) {
                    $this->fail('players', 'Each team must submit a complete playing XI before approval.');
                }
            }

            $match->players()->where('selection_type', 'playing_xi')->update([
                'approved_at' => now(),
                'approved_by' => $actorId,
            ]);
            $match->update([
                'status' => 'toss_pending',
                'updated_by' => $actorId,
                'revision' => $match->revision + 1,
                'last_event_at' => now(),
            ]);
            return $match->fresh(['players.team', 'ruleProfile']);
        });
    }

    public function recordToss(CricketMatch $match, int $winnerTeamId, string $decision, int $actorId): CricketMatch
    {
        return $this->database->transaction(function () use ($match, $winnerTeamId, $decision, $actorId) {
            $match = CricketMatch::query()->with(['players', 'ruleProfile'])->lockForUpdate()->findOrFail($match->id);
            if ($match->status !== 'toss_pending') {
                $this->fail('match', 'Toss can only be recorded after both playing XIs are approved.');
            }
            if (! in_array($decision, ['bat', 'field'], true)) {
                $this->fail('toss_decision', 'Choose whether the toss winner will bat or field.');
            }
            if (! $match->players->pluck('team_id')->contains($winnerTeamId)) {
                $this->fail('toss_winner_team_id', 'The toss winner must be one of the match teams.');
            }

            $teamIds = $match->players->pluck('team_id')->unique()->values();
            $otherTeamId = $teamIds->first(fn ($teamId) => (int) $teamId !== $winnerTeamId);
            $battingTeamId = $decision === 'bat' ? $winnerTeamId : $otherTeamId;
            $bowlingTeamId = $decision === 'bat' ? $otherTeamId : $winnerTeamId;
            $openingBatters = $match->players
                ->where('team_id', $battingTeamId)
                ->where('selection_type', 'playing_xi')
                ->sortBy('batting_order')
                ->take(2)
                ->values();
            $innings = MatchInnings::create([
                'match_id' => $match->id,
                'innings_number' => 1,
                'batting_team_id' => $battingTeamId,
                'bowling_team_id' => $bowlingTeamId,
                'current_striker_id' => $openingBatters->get(0)?->id,
                'current_non_striker_id' => $openingBatters->get(1)?->id,
                'status' => 'live',
                'maximum_overs' => $this->effectiveOversPerInnings($match),
                'started_at' => now(),
            ]);
            $match->update([
                'toss_winner_team_id' => $winnerTeamId,
                'toss_decision' => $decision,
                'toss_recorded_at' => now(),
                'updated_by' => $actorId,
                'revision' => $match->revision + 1,
                'last_event_at' => now(),
                'current_innings_id' => $innings->id,
                'started_at' => now(),
                'status' => 'live',
            ]);
            return $match->fresh(['players.team', 'tossWinner', 'ruleProfile', 'innings']);
        });
    }

    private function effectiveOversPerInnings(CricketMatch $match): int
    {
        return (int) ($match->overs_per_innings ?: $match->ruleProfile?->overs_per_innings);
    }

    private function draftIsComplete(Draft $draft): bool
    {
        if ($draft->status === 'completed') {
            return true;
        }
        return ! $draft->picks->contains(fn ($pick) => in_array($pick->status, ['pending', 'active', 'expired'], true));
    }

    private function fail(string $key, string $message): never
    {
        throw ValidationException::withMessages([$key => $message]);
    }
}
