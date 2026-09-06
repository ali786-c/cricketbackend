<?php

namespace App\Modules\Analytics\Services;

use App\Models\CricketMatch;
use App\Models\DraftPick;
use App\Models\Team;

class TeamComparisonService
{
    public function getClassifiedSquad(Team $team): array
    {
        $picks = DraftPick::query()
            ->where('team_id', $team->id)
            ->whereNotNull('tournament_player_id')
            ->with(['tournamentPlayer.playerProfile'])
            ->get();

        $classified = [
            'batters' => [],
            'bowlers' => [],
            'all_rounders' => [],
            'wicketkeepers' => [],
        ];

        foreach ($picks as $pick) {
            $tp = $pick->tournamentPlayer;
            if (!$tp) continue;

            $profile = $tp->playerProfile;
            $role = $profile?->playing_role ?? 'All-rounder';

            $playerData = [
                'id' => $tp->id,
                'full_name' => $profile?->full_name,
                'playing_role' => $role,
                'is_captain' => (bool) $pick->is_captain,
                'is_vice_captain' => (bool) $pick->is_vice_captain,
                'is_wicketkeeper' => (bool) $pick->is_wicketkeeper,
            ];

            switch ($role) {
                case 'Batter':
                    $classified['batters'][] = $playerData;
                    break;
                case 'Bowler':
                    $classified['bowlers'][] = $playerData;
                    break;
                case 'Wicketkeeper':
                    $classified['wicketkeepers'][] = $playerData;
                    break;
                case 'All-rounder':
                default:
                    $classified['all_rounders'][] = $playerData;
                    break;
            }
        }

        return $classified;
    }

    public function compareTeams(int $team1Id, int $team2Id): array
    {
        $t1 = Team::findOrFail($team1Id);
        $t2 = Team::findOrFail($team2Id);

        $matches = CricketMatch::query()
            ->where('status', 'completed')
            ->whereHas('fixture', function ($q) use ($team1Id, $team2Id) {
                $q->where(function ($q1) use ($team1Id, $team2Id) {
                    $q1->where('home_team_id', $team1Id)->where('away_team_id', $team2Id);
                })->orWhere(function ($q2) use ($team1Id, $team2Id) {
                    $q2->where('home_team_id', $team2Id)->where('away_team_id', $team1Id);
                });
            })
            ->with(['fixture.homeTeam', 'fixture.awayTeam', 'winner'])
            ->latest('id')
            ->get();

        $t1Wins = 0;
        $t2Wins = 0;
        $ties = 0;

        $encounters = [];
        foreach ($matches as $match) {
            $winnerId = $match->winner_team_id;
            if ($winnerId === $team1Id) {
                $t1Wins++;
                $resultText = "{$t1->short_name} won";
            } elseif ($winnerId === $team2Id) {
                $t2Wins++;
                $resultText = "{$t2->short_name} won";
            } else {
                $ties++;
                $resultText = "Tie / No Result";
            }

            $encounters[] = [
                'match_id' => $match->id,
                'date' => $match->completed_at?->toDateString(),
                'home_team' => [
                    'id' => $match->fixture?->home_team_id,
                    'short_name' => $match->fixture?->homeTeam?->short_name,
                ],
                'away_team' => [
                    'id' => $match->fixture?->away_team_id,
                    'short_name' => $match->fixture?->awayTeam?->short_name,
                ],
                'winner_id' => $winnerId,
                'result_text' => $resultText,
            ];
        }

        return [
            'team1' => [
                'id' => $t1->id,
                'name' => $t1->name,
                'short_name' => $t1->short_name,
                'logo_path' => $t1->logo_path,
                'wins' => $t1Wins,
            ],
            'team2' => [
                'id' => $t2->id,
                'name' => $t2->name,
                'short_name' => $t2->short_name,
                'logo_path' => $t2->logo_path,
                'wins' => $t2Wins,
            ],
            'summary' => [
                'total_encounters' => $matches->count(),
                'team1_wins' => $t1Wins,
                'team2_wins' => $t2Wins,
                'ties_no_results' => $ties,
            ],
            'encounters' => $encounters,
        ];
    }
}
