<?php

namespace App\Modules\Tournament\Services;

use App\Models\Tournament;
use App\Models\TournamentStanding;
use App\Models\Team;
use Illuminate\Database\DatabaseManager;

class StandingsService
{
    public function __construct(private readonly DatabaseManager $database)
    {
    }

    public function rebuild(int $tournamentId): void
    {
        $this->database->transaction(function () use ($tournamentId) {
            $tournament = Tournament::query()->with('cricketRuleProfile')->findOrFail($tournamentId);
            // Support both the canonical pivot membership and legacy/direct
            // tournament_id ownership used by fixture and mobile creation.
            $pivotTeams = $tournament->teams()->where('teams.is_active', true)->get();
            $directTeams = Team::query()->where('tournament_id', $tournament->id)->where('is_active', true)->get();
            $matchTeamIds = $tournament->matches()->with('innings:id,match_id,batting_team_id,bowling_team_id')
                ->get()->flatMap(fn ($match) => $match->innings->flatMap(fn ($innings) => [$innings->batting_team_id, $innings->bowling_team_id]))
                ->filter()->unique()->values();
            $matchTeams = Team::query()->whereIn('id', $matchTeamIds)->where('is_active', true)->get();
            $teams = $pivotTeams->concat($directTeams)->concat($matchTeams)->unique('id')->values();
            foreach ($teams as $team) {
                TournamentStanding::updateOrCreate(['tournament_id' => $tournament->id, 'team_id' => $team->id], [
                    'played' => 0, 'wins' => 0, 'losses' => 0, 'ties' => 0, 'no_results' => 0,
                    'points' => 0, 'runs_for' => 0, 'balls_faced' => 0, 'runs_against' => 0, 'balls_bowled' => 0, 'net_run_rate' => 0,
                ]);
            }
            $matches = $tournament->matches()->where('status', 'approved')->with('innings')->get();
            foreach ($matches as $match) {
                if (! in_array($match->result_type, ['win', 'tie', 'no_result'], true)) continue;
                $innings = $match->innings->sortBy('innings_number')->values();
                foreach ($innings as $inning) {
                    $standing = TournamentStanding::query()->where('tournament_id', $tournament->id)->where('team_id', $inning->batting_team_id)->lockForUpdate()->first();
                    if (! $standing) continue;
                    $standing->runs_for += $inning->total_runs;
                    $standing->balls_faced += $inning->legal_balls;
                    $standing->runs_against += $innings->where('bowling_team_id', $inning->batting_team_id)->sum('total_runs');
                    $standing->balls_bowled += $innings->where('bowling_team_id', $inning->batting_team_id)->sum('legal_balls');
                    $standing->save();
                }
                $teamIds = $innings->pluck('batting_team_id')->unique();
                foreach ($teamIds as $teamId) {
                    $standing = TournamentStanding::query()->where('tournament_id', $tournament->id)->where('team_id', $teamId)->lockForUpdate()->first();
                    if (! $standing) continue;
                    $standing->played++;
                    if ($match->result_type === 'tie') $standing->ties++;
                    elseif ($match->result_type === 'no_result') $standing->no_results++;
                    elseif ((int) $match->winner_team_id === (int) $teamId) $standing->wins++;
                    else $standing->losses++;
                    $standing->points += $match->result_type === 'win'
                        ? ((int) $match->winner_team_id === (int) $teamId ? $tournament->cricketRuleProfile->win_points : $tournament->cricketRuleProfile->loss_points)
                        : ($match->result_type === 'tie' ? $tournament->cricketRuleProfile->tie_points : $tournament->cricketRuleProfile->no_result_points);
                    $ballsPerOver = (int) ($tournament->cricketRuleProfile?->legal_balls_per_over ?: 6);
                    $standing->net_run_rate = $standing->balls_faced > 0 && $standing->balls_bowled > 0
                        ? round(($standing->runs_for / ($standing->balls_faced / $ballsPerOver)) - ($standing->runs_against / ($standing->balls_bowled / $ballsPerOver)), 3)
                        : 0;
                    $standing->save();
                }
            }
        });
    }
}
