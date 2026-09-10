<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class MatchController extends Controller
{
    public function state(Request $request, CricketMatch $match): JsonResponse
    {
        $match->load(['tournament', 'fixture.homeTeam', 'fixture.awayTeam', 'ruleProfile', 'players.team', 'tossWinner', 'innings.battingTeam', 'innings.bowlingTeam', 'innings.currentStriker', 'innings.currentNonStriker', 'innings.currentBowler', 'innings.battingStats.player', 'innings.bowlingStats.player', 'innings.deliveries.wicket']);
        $visibleStatus = in_array($match->status, ['live', 'completed', 'result_pending', 'approved'], true);
        $publiclyVisible = $match->tournament?->publiclyVisibleNow() === true && $visibleStatus;
        $user = $request->user('sanctum');
        $ownerVisible = $user && (int) $match->created_by === (int) $user->id;
        $tournamentOwnerVisible = $user && $match->tournament && (int) $match->tournament->creator_id === (int) $user->id;
        $privilegedVisible = $user && $user->hasRole('super_admin');
        abort_unless($publiclyVisible || $ownerVisible || $tournamentOwnerVisible || $privilegedVisible, 404);

        $rules = $match->rule_snapshot ?: $this->ruleSnapshot($match);
        $ballsPerOver = (int) ($rules['legal_balls_per_over'] ?? 6);
        return response()->json([
            'data' => [
                'id' => $match->id,
                'revision' => $match->revision,
                'status' => $match->status,
                'result_type' => $match->result_type,
                'result_summary' => $match->result_summary,
                'winner_team_id' => $match->winner_team_id,
                'overs_per_innings' => (int) ($match->overs_per_innings ?: $match->ruleProfile?->overs_per_innings),
                'fixture' => [
                    'id' => $match->fixture?->id,
                    'tournament_id' => $match->tournament_id,
                    'home_team' => $this->teamData($match->fixture?->homeTeam),
                    'away_team' => $this->teamData($match->fixture?->awayTeam),
                    'scheduled_at' => $match->fixture?->scheduled_at?->toIso8601String(),
                    'venue' => $match->fixture?->venue,
                    'city' => $match->fixture?->city,
                    'timezone' => $match->fixture?->timezone,
                ],
                'rule_snapshot' => $rules,
                'toss' => [
                    'winner_team' => $this->teamData($match->tossWinner),
                    'decision' => $match->toss_decision,
                ],
                'players' => $match->players->map(fn ($player) => [
                    'match_player_id' => $player->id,
                    'player_profile_id' => $player->player_profile_id,
                    'tournament_player_id' => $player->tournament_player_id,
                    'team_id' => $player->team_id,
                    'name' => $player->player_name_snapshot,
                    'role' => $player->player_role_snapshot,
                    'selection_type' => $player->selection_type,
                    'batting_order' => $player->batting_order,
                ])->values(),
                'innings' => $match->innings->map(fn ($inning) => [
                    'id' => $inning->id,
                    'number' => $inning->innings_number,
                    'batting_team' => ['id' => $inning->battingTeam?->id, 'name' => $inning->battingTeam?->name, 'short_name' => $inning->battingTeam?->short_name],
                    'bowling_team' => ['id' => $inning->bowlingTeam?->id, 'name' => $inning->bowlingTeam?->name, 'short_name' => $inning->bowlingTeam?->short_name],
                    'runs' => $inning->total_runs,
                    'wickets' => $inning->wickets,
                    'legal_balls' => $inning->legal_balls,
                    'maximum_overs' => (int) $inning->maximum_overs,
                    'overs' => $inning->oversDisplay($ballsPerOver),
                    'target' => $inning->target_runs,
                    'status' => $inning->status,
                    'active_players' => [
                        'striker' => $this->playerData($inning->currentStriker),
                        'non_striker' => $this->playerData($inning->currentNonStriker),
                        'bowler' => $this->playerData($inning->currentBowler),
                    ],
                    'batting' => $inning->battingStats->sortBy('batting_position')->values()->map(fn ($stat) => [
                        'player' => $stat->player?->player_name_snapshot,
                        'dismissal' => $stat->dismissal_type,
                        'runs' => $stat->runs,
                        'balls' => $stat->balls,
                        'fours' => $stat->fours,
                        'sixes' => $stat->sixes,
                        'strike_rate' => (float) $stat->strike_rate,
                    ]),
                    'bowling' => $inning->bowlingStats->values()->map(fn ($stat) => [
                        'player' => $stat->player?->player_name_snapshot,
                        'overs' => intdiv((int) $stat->legal_balls, $ballsPerOver).'.'.((int) $stat->legal_balls % $ballsPerOver),
                        'runs' => $stat->runs_conceded,
                        'wickets' => $stat->wickets,
                        'wides' => $stat->wides,
                        'no_balls' => $stat->no_balls,
                        'economy' => (float) $stat->economy,
                    ]),
                    'recent_deliveries' => $inning->deliveries->whereNull('voided_at')->sortByDesc('sequence_number')->take(12)->values()->map(fn ($delivery) => [
                        'over' => $delivery->over_number.'.'.$delivery->ball_number,
                        'notation' => $delivery->notation(),
                        'total_runs' => $delivery->total_runs,
                    ]),
                ])->values(),
            ],
        ]);
    }

    private function teamData($team): ?array
    {
        return $team ? ['id' => $team->id, 'name' => $team->name, 'short_name' => $team->short_name] : null;
    }

    private function playerData($player): ?array
    {
        return $player ? [
            'match_player_id' => $player->id,
            'team_id' => $player->team_id,
            'name' => $player->player_name_snapshot,
            'role' => $player->player_role_snapshot,
        ] : null;
    }

    private function ruleSnapshot(CricketMatch $match): array
    {
        $profile = $match->ruleProfile;
        return [
            'profile_id' => $profile?->id,
            'version' => $match->rule_profile_version,
            'format' => $profile?->format,
            'innings_per_side' => $profile?->innings_per_side,
            'overs_per_innings' => $match->overs_per_innings ?: $profile?->overs_per_innings,
            'playing_xi_size' => $profile?->playing_xi_size,
            'maximum_wickets' => $profile?->maximum_wickets,
            'legal_balls_per_over' => $profile?->legal_balls_per_over,
            'max_overs_per_bowler' => $profile?->max_overs_per_bowler,
            'ball_type' => $match->tournament?->ball_type,
            'no_ball_runs' => $profile?->no_ball_runs,
            'wide_runs' => $profile?->wide_runs,
            'wide_runs_to_batsman' => $profile?->wide_runs_to_batsman,
            'noball_runs_to_batsman' => $profile?->noball_runs_to_batsman,
            'last_man_standing' => $profile?->last_man_standing,
            'max_balls_per_over' => $profile?->max_balls_per_over,
            'max_runs_per_over' => $profile?->max_runs_per_over,
        ];
    }
}
