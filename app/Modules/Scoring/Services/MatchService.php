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

    public function createFromTeams(Tournament $tournament, int $homeTeamId, int $awayTeamId, ?int $fixtureId, int $actorId, ?int $oversPerInnings = null): CricketMatch
    {
        return $this->database->transaction(function () use ($tournament, $homeTeamId, $awayTeamId, $fixtureId, $actorId, $oversPerInnings) {
            $tournament = Tournament::query()->with('cricketRuleProfile')->lockForUpdate()->findOrFail($tournament->id);
            $profile = $tournament->cricketRuleProfile;

            // Auto-create a default rule profile if none exists
            if (! $profile || ! $profile->is_active) {
                $profile = CricketRuleProfile::updateOrCreate(
                    ['slug' => 'default-' . $tournament->id, 'is_active' => true],
                    [
                        'name' => 'Default Rules (' . $tournament->name . ')',
                        'format' => 'T20',
                        'overs_per_innings' => $tournament->default_overs_per_innings ?? 20,
                        'wickets_per_innings' => 10,
                        'playing_xi_size' => 11,
                        'max_over_per_bowler' => 4,
                        'version' => 1,
                        'is_active' => true,
                    ]
                );
                $tournament->update(['cricket_rule_profile_id' => $profile->id]);
                $profile = $tournament->fresh('cricketRuleProfile')->cricketRuleProfile;
            }

            $teamIds = collect([$homeTeamId, $awayTeamId])->map(fn ($id) => (int) $id);
            if ($teamIds->count() !== 2 || $teamIds->unique()->count() !== 2) {
                $this->fail('teams', 'A match requires two different teams.');
            }

            $teams = $tournament->teams()->whereIn('id', $teamIds)->orderBy('display_order')->get();
            if ($teams->count() !== 2) {
                $this->fail('teams', 'Both teams must belong to this tournament.');
            }

            $draft = $tournament->draft()->with(['picks.tournamentPlayer.playerProfile'])->lockForUpdate()->first();
            if (! $draft || ! $this->draftIsComplete($draft)) {
                $this->fail('match', 'The tournament draft must be completed before a match can be created.');
            }

            $selectedPicks = $draft->picks
                ->where('status', 'selected')
                ->whereIn('team_id', $teamIds)
                ->values();
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

            $effectiveOvers = $oversPerInnings ?? $tournament->default_overs_per_innings ?? $profile->overs_per_innings;
            if ($effectiveOvers < 1 || $effectiveOvers > 100) {
                $this->fail('overs_per_innings', 'Overs per innings must be between 1 and 100.');
            }

            $match = CricketMatch::create([
                'fixture_id' => $fixtureId,
                'tournament_id' => $tournament->id,
                'rule_profile_id' => $profile->id,
                'rule_profile_version' => $profile->version,
                'overs_per_innings' => $effectiveOvers,
                'status' => 'squad_selection',
                'revision' => 1,
                'last_event_at' => now(),
                'created_by' => $actorId,
                'updated_by' => $actorId,
            ]);

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

            return $match->load(['players.team', 'ruleProfile']);
        });
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
            $innings = MatchInnings::create([
                'match_id' => $match->id,
                'innings_number' => 1,
                'batting_team_id' => $battingTeamId,
                'bowling_team_id' => $bowlingTeamId,
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
