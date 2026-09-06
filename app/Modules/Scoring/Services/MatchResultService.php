<?php

namespace App\Modules\Scoring\Services;

use App\Models\CricketMatch;
use App\Models\Tournament;
use App\Modules\Tournament\Services\StandingsService;
use Illuminate\Database\DatabaseManager;
use Illuminate\Support\Collection;
use Illuminate\Validation\ValidationException;

class MatchResultService
{
    public function __construct(private readonly DatabaseManager $database, private readonly StandingsService $standings)
    {
    }

    public function submit(CricketMatch $match, int $actorId): CricketMatch
    {
        return $this->database->transaction(function () use ($match, $actorId) {
            $match = CricketMatch::query()->with(['innings.battingTeam', 'innings.bowlingTeam', 'ruleProfile'])->lockForUpdate()->findOrFail($match->id);
            if ($match->status !== 'completed') $this->fail('match', 'Only a completed match can be submitted for result approval.');
            $innings = $match->innings->sortBy('innings_number')->values();
            $expectedInnings = (int) $match->ruleProfile->innings_per_side * 2;
            if ($innings->count() < $expectedInnings) $this->fail('match', 'Both teams\' configured innings must be completed first.');

            // Group innings by batting team and aggregate runs.
            $teamTotals = $innings->groupBy('batting_team_id')->map(function ($teamInnings, $teamId) {
                return [
                    'team_id' => (int) $teamId,
                    'total_runs' => $teamInnings->sum('total_runs'),
                    'total_wickets' => $teamInnings->sum('wickets'),
                    'last_innings' => $teamInnings->last(),
                ];
            })->values();

            $resultType = 'tie';
            $winner = null;
            $summary = 'Match tied';

            if ($teamTotals->count() === 2) {
                $teamA = $teamTotals[0];
                $teamB = $teamTotals[1];

                if ($teamA['total_runs'] > $teamB['total_runs']) {
                    $winner = $teamA['team_id'];
                    $resultType = 'win';
                    $margin = $teamA['total_runs'] - $teamB['total_runs'];
                    // Determine winner's team name from the last innings they batted
                    $winnerTeam = $teamA['last_innings']->battingTeam;
                    $summary = $winnerTeam?->short_name.' won by '.$margin.' runs';
                } elseif ($teamB['total_runs'] > $teamA['total_runs']) {
                    $winner = $teamB['team_id'];
                    $resultType = 'win';
                    $winnerTeam = $teamB['last_innings']->battingTeam;
                    $wicketsRemaining = max(0, (int) $match->ruleProfile->maximum_wickets - (int) $teamB['total_wickets']);
                    $summary = $winnerTeam?->short_name.' won by '.$wicketsRemaining.' wickets';
                }
            }
            $match->update(['winner_team_id' => $winner, 'result_type' => $resultType, 'result_summary' => $summary, 'result_submitted_at' => now(), 'result_submitted_by' => $actorId, 'status' => 'result_pending', 'revision' => $match->revision + 1, 'last_event_at' => now(), 'updated_by' => $actorId]);
            return $match->fresh(['winner', 'innings']);
        });
    }

    public function approve(CricketMatch $match, int $actorId): CricketMatch
    {
        return $this->database->transaction(function () use ($match, $actorId) {
            $match = CricketMatch::query()->lockForUpdate()->findOrFail($match->id);
            if ($match->status !== 'result_pending') $this->fail('match', 'Only a submitted result can be approved.');
            $match->update(['status' => 'approved', 'approved_at' => now(), 'result_approved_by' => $actorId, 'revision' => $match->revision + 1, 'last_event_at' => now(), 'updated_by' => $actorId]);
            $this->standings->rebuild($match->tournament_id);
            return $match->fresh(['winner']);
        });
    }

    private function fail(string $key, string $message): never
    {
        throw ValidationException::withMessages([$key => $message]);
    }
}
