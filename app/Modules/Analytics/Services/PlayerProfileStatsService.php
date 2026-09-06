<?php

namespace App\Modules\Analytics\Services;

use App\Models\InningsBattingStat;
use App\Models\InningsBowlingStat;
use App\Models\MatchPlayer;
use App\Models\MatchWicket;
use App\Models\PlayerProfile;
use App\Models\TournamentPlayer;

class PlayerProfileStatsService
{
    public function getPlayerStats(PlayerProfile $player, array $filters = []): array
    {
        $tpIds = TournamentPlayer::query()->where('player_profile_id', $player->id)->pluck('id')->all();

        $query = MatchPlayer::query()
            ->whereIn('tournament_player_id', $tpIds)
            ->whereHas('match', function ($q) use ($filters) {
                $q->where('status', 'completed');

                // 1. Year Filter
                if (!empty($filters['year'])) {
                    $q->whereYear('starts_on', $filters['year']);
                }

                // 2. Format Filter
                if (!empty($filters['format'])) {
                    $q->whereHas('ruleProfile', function ($r) use ($filters) {
                        $r->where('format', $filters['format']);
                    });
                }

                // 3. Ball Type Filter
                if (!empty($filters['ball_type'])) {
                    $q->whereHas('tournament', function ($t) use ($filters) {
                        $t->where('ball_type', $filters['ball_type']);
                    });
                }

                // 4. Data Source Filter
                if (!empty($filters['data_source'])) {
                    $q->whereHas('tournament', function ($t) use ($filters) {
                        $t->where('data_source', $filters['data_source']);
                    });
                }
            });

        $matchPlayers = $query->get();
        $matchPlayerIds = $matchPlayers->pluck('id')->all();

        // 1. Batting Stats Aggregation
        $battingStats = InningsBattingStat::query()
            ->whereIn('match_player_id', $matchPlayerIds)
            ->get();

        $runs = $battingStats->sum('runs');
        $balls = $battingStats->sum('balls');
        $fours = $battingStats->sum('fours');
        $sixes = $battingStats->sum('sixes');

        $dismissedCount = MatchWicket::query()
            ->whereIn('dismissed_player_id', $matchPlayerIds)
            ->whereHas('delivery', fn($q) => $q->whereNull('voided_at'))
            ->count();

        $battingAverage = $dismissedCount > 0 ? round($runs / $dismissedCount, 2) : ($runs > 0 ? $runs : 0.0);
        $battingStrikeRate = $balls > 0 ? round(($runs / $balls) * 100, 2) : 0.0;

        // 2. Bowling Stats Aggregation
        $bowlingStats = InningsBowlingStat::query()
            ->whereIn('match_player_id', $matchPlayerIds)
            ->get();

        $wickets = $bowlingStats->sum('wickets');
        $legalBalls = $bowlingStats->sum('legal_balls');
        $runsConceded = $bowlingStats->sum('runs_conceded');
        $maidens = $bowlingStats->sum('maidens');

        $bowlingAverage = $wickets > 0 ? round($runsConceded / $wickets, 2) : 0.0;
        $bowlingStrikeRate = $wickets > 0 ? round($legalBalls / $wickets, 2) : 0.0;
        $economy = $legalBalls > 0 ? round(($runsConceded / $legalBalls) * 6, 2) : 0.0;

        // 3. General stats
        $matchesPlayed = $matchPlayers->where('selection_type', 'playing_xi')->count();

        return [
            'matches_played' => $matchesPlayed,
            'batting' => [
                'runs' => $runs,
                'balls' => $balls,
                'average' => (float) $battingAverage,
                'strike_rate' => (float) $battingStrikeRate,
                'fours' => $fours,
                'sixes' => $sixes,
                'dismissals' => $dismissedCount,
            ],
            'bowling' => [
                'wickets' => $wickets,
                'legal_balls' => $legalBalls,
                'runs_conceded' => $runsConceded,
                'economy' => (float) $economy,
                'average' => (float) $bowlingAverage,
                'strike_rate' => (float) $bowlingStrikeRate,
                'maidens' => $maidens,
            ],
        ];
    }

    public function getPlayerInsights(PlayerProfile $player): array
    {
        $tpIds = TournamentPlayer::query()->where('player_profile_id', $player->id)->pluck('id')->all();

        // 1. Strike Rate Trend over last 10 matches
        $last10MatchPlayers = MatchPlayer::query()
            ->whereIn('tournament_player_id', $tpIds)
            ->whereHas('match', fn($q) => $q->where('status', 'completed'))
            ->with(['match', 'match.fixture.homeTeam', 'match.fixture.awayTeam'])
            ->latest('id')
            ->take(10)
            ->get();

        $strikeRateTrend = $last10MatchPlayers->map(function ($mp) {
            $batting = InningsBattingStat::query()->where('match_player_id', $mp->id)->first();
            $runs = $batting?->runs ?? 0;
            $balls = $batting?->balls ?? 0;
            $sr = $balls > 0 ? round(($runs / $balls) * 100, 2) : 0.0;

            // Opponent name — resolve through fixture
            $match = $mp->match;
            $opponent = 'Opponent';
            if ($match && $match->fixture) {
                $homeTeamId = $match->fixture->home_team_id;
                $opponent = $mp->team_id === $homeTeamId
                    ? ($match->fixture->awayTeam?->short_name ?? 'Opponent')
                    : ($match->fixture->homeTeam?->short_name ?? 'Opponent');
            }

            return [
                'match_id' => $mp->match_id,
                'opponent' => $opponent,
                'runs' => $runs,
                'balls' => $balls,
                'strike_rate' => (float) $sr,
            ];
        })->reverse()->values()->all();

        // 2. Wicket splits by bowler type
        $matchPlayerIds = MatchPlayer::query()
            ->whereIn('tournament_player_id', $tpIds)
            ->pluck('id')
            ->all();

        $wickets = MatchWicket::query()
            ->whereIn('dismissed_player_id', $matchPlayerIds)
            ->whereHas('delivery', fn($q) => $q->whereNull('voided_at'))
            ->with(['creditedBowler', 'creditedBowler.tournamentPlayer', 'creditedBowler.tournamentPlayer.playerProfile'])
            ->get();

        $fastDismissals = 0;
        $spinDismissals = 0;
        $otherDismissals = 0;

        foreach ($wickets as $wicket) {
            $bowlerProfile = $wicket->creditedBowler?->tournamentPlayer?->playerProfile;
            if (!$bowlerProfile) {
                $otherDismissals++;
                continue;
            }

            $style = strtolower($bowlerProfile->bowling_style ?? '');
            if (str_contains($style, 'fast') || str_contains($style, 'medium') || str_contains($style, 'pace')) {
                $fastDismissals++;
            } elseif (str_contains($style, 'spin') || str_contains($style, 'break') || str_contains($style, 'orthodox') || str_contains($style, 'chinaman')) {
                $spinDismissals++;
            } else {
                $otherDismissals++;
            }
        }

        return [
            'strike_rate_trend' => $strikeRateTrend,
            'wicket_splits' => [
                'fast' => $fastDismissals,
                'spin' => $spinDismissals,
                'other_unknown' => $otherDismissals,
            ]
        ];
    }
}
