<?php

namespace App\Http\Controllers\SuperAdmin;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use App\Modules\Analytics\Services\MVPPointsService;
use Illuminate\Http\Request;
use Illuminate\View\View;

class MatchController extends Controller
{
    public function index(Request $request): View
    {
        $query = CricketMatch::with(['tournament', 'fixture.homeTeam', 'fixture.awayTeam'])->latest();

        if ($status = $request->string('status')->toString()) {
            $query->where('status', $status);
        }

        return view('super-admin.matches.index', [
            'matches' => $query->paginate(20)->withQueryString(),
            'selectedStatus' => $request->string('status')->toString(),
            'statuses' => ['scheduled', 'toss_pending', 'live', 'completed', 'abandoned', 'cancelled'],
            'statusCounts' => CricketMatch::query()->selectRaw('status, COUNT(*) as total')->groupBy('status')->pluck('total', 'status'),
        ]);
    }

    /**
     * Mobile "Match Center"-style detail view: every visible detail for one match,
     * gathered from the server-side scoring tables (innings, batting, bowling,
     * deliveries, wickets, squads, toss and result).
     */
    public function show(Request $request, CricketMatch $match, MVPPointsService $mvpService): View
    {
        $match->load([
            'tournament',
            'fixture.homeTeam',
            'fixture.awayTeam',
            'ruleProfile',
            'tossWinner',
            'winner',
            'creator',
            'innings.battingTeam',
            'innings.bowlingTeam',
            'innings.battingStats.player',
            'innings.battingStats.dismissedBy',
            'innings.battingStats.fielder',
            'innings.bowlingStats.player',
            'innings.deliveries.striker',
            'innings.deliveries.nonStriker',
            'innings.deliveries.bowler',
            'innings.deliveries.wicket.dismissedPlayer',
            'innings.deliveries.wicket.creditedBowler',
            'innings.deliveries.wicket.fielder',
        ]);

        $ballsPerOver = max(1, (int) ($match->ruleProfile?->legal_balls_per_over ?: 6));
        $teams = collect([$match->fixture?->homeTeam, $match->fixture?->awayTeam])
            ->filter()
            ->keyBy('id');

        $inningsView = $match->innings->map(fn ($inning) => $this->buildInningsView($inning, $ballsPerOver));

        $squads = $match->players
            ->sortBy([['selection_type', 'asc'], ['batting_order', 'asc'], ['player_name_snapshot', 'asc']])
            ->groupBy('team_id')
            ->map(fn ($players, $teamId) => [
                'team' => $teams->get((int) $teamId),
                'team_name' => $teams->get((int) $teamId)?->name ?: $teams->get((int) $teamId)?->short_name ?: 'Team',
                'players' => $players->map(fn ($player) => [
                    'name' => $player->player_name_snapshot,
                    'role' => $player->player_role_snapshot,
                    'selection' => $player->selection_type,
                    'batting_order' => $player->batting_order,
                    'is_captain' => (bool) $player->is_captain,
                    'is_wicketkeeper' => (bool) $player->is_wicketkeeper,
                ])->values()->all(),
            ])
            ->values();

        $mvp = $mvpService->getMatchMVP($match)
            ->filter(fn ($row) => $row['points']['total'] > 0)
            ->take(5)
            ->values();

        return view('super-admin.matches.show', [
            'match' => $match,
            'inningsView' => $inningsView,
            'squads' => $squads,
            'mvp' => $mvp,
            'teams' => $teams,
            'ballsPerOver' => $ballsPerOver,
        ]);
    }

    private function buildInningsView($inning, int $ballsPerOver): array
    {
        $deliveries = $inning->deliveries->whereNull('voided_at')->sortBy('sequence_number')->values();

        // Single pass over the delivery log: cumulative score, fall of wickets and partnerships.
        $cumulativeRuns = 0;
        $legalBalls = 0;
        $partnershipRuns = 0;
        $partnershipBalls = 0;
        $fow = [];
        $partnerships = [];

        foreach ($deliveries as $delivery) {
            $cumulativeRuns += (int) $delivery->total_runs;
            if ($delivery->is_legal_delivery) {
                $legalBalls++;
                $partnershipBalls++;
            }
            $partnershipRuns += (int) $delivery->total_runs;

            $wicket = $delivery->wicket;
            if (!$wicket || !$wicket->is_valid_wicket) {
                continue;
            }

            $dismissedName = $wicket->dismissedPlayer?->player_name_snapshot ?? $delivery->striker?->player_name_snapshot ?? 'Batter';
            $notOutBatter = $delivery->striker?->player_name_snapshot === $dismissedName
                ? $delivery->nonStriker?->player_name_snapshot
                : $delivery->striker?->player_name_snapshot;

            $fow[] = [
                'wicket' => count($fow) + 1,
                'score' => $cumulativeRuns,
                'over' => intdiv($legalBalls, $ballsPerOver).'.'.($legalBalls % $ballsPerOver),
                'batter' => $dismissedName,
                'bowler' => $wicket->creditedBowler?->player_name_snapshot ?? $delivery->bowler?->player_name_snapshot,
                'fielder' => $wicket->fielder?->player_name_snapshot,
                'dismissal' => str_replace('_', ' ', $wicket->dismissal_type),
                'partnership_runs' => $partnershipRuns,
                'partnership_balls' => $partnershipBalls,
            ];

            $partnerships[] = [
                'batters' => trim($dismissedName.', '.$notOutBatter, ', '),
                'runs' => $partnershipRuns,
                'balls' => $partnershipBalls,
                'wicket' => count($fow),
            ];

            $partnershipRuns = 0;
            $partnershipBalls = 0;
        }

        if ($partnershipRuns > 0 || $partnershipBalls > 0) {
            $currentPair = $deliveries->last();
            $partnerships[] = [
                'batters' => trim(($currentPair?->striker?->player_name_snapshot ?? '').', '.($currentPair?->nonStriker?->player_name_snapshot ?? ''), ', '),
                'runs' => $partnershipRuns,
                'balls' => $partnershipBalls,
                'wicket' => null,
            ];
        }

        $batting = $inning->battingStats
            ->sortBy([['batting_position', 'asc'], ['id', 'asc']])
            ->values()
            ->map(fn ($stat) => [
                'name' => $stat->player?->player_name_snapshot ?? 'Unknown',
                'status' => $stat->status,
                'dismissal' => $stat->dismissal_type
                    ? str_replace('_', ' ', ucfirst($stat->dismissal_type))
                        .($stat->fielder?->player_name_snapshot ? ' ('.$stat->fielder->player_name_snapshot.')' : '')
                        .($stat->dismissedBy?->player_name_snapshot ? ' b '.$stat->dismissedBy->player_name_snapshot : '')
                    : ($stat->status === 'did_not_bat' ? 'Did not bat' : 'Not out'),
                'runs' => (int) $stat->runs,
                'balls' => (int) $stat->balls,
                'fours' => (int) $stat->fours,
                'sixes' => (int) $stat->sixes,
                'strike_rate' => (int) $stat->balls > 0 ? number_format((float) $stat->strike_rate, 1) : '0.0',
            ]);

        $bowling = $inning->bowlingStats
            ->filter(fn ($stat) => (int) $stat->legal_balls > 0 || (int) $stat->runs_conceded > 0)
            ->values()
            ->map(fn ($stat) => [
                'name' => $stat->player?->player_name_snapshot ?? 'Unknown',
                'overs' => intdiv((int) $stat->legal_balls, $ballsPerOver).'.'.((int) $stat->legal_balls % $ballsPerOver),
                'maidens' => (int) $stat->maidens,
                'runs' => (int) $stat->runs_conceded,
                'wickets' => (int) $stat->wickets,
                'wides' => (int) $stat->wides,
                'no_balls' => (int) $stat->no_balls,
                'economy' => number_format((float) $stat->economy, 2),
            ]);

        $overs = $deliveries
            ->groupBy('over_number')
            ->map(fn ($overDeliveries, $overNumber) => [
                'over' => (int) $overNumber,
                'runs' => (int) $overDeliveries->sum('total_runs'),
                'wickets' => (int) $overDeliveries->filter(fn ($d) => $d->wicket && $d->wicket->is_valid_wicket)->count(),
                'notation' => $overDeliveries->map(fn ($d) => $d->notation())->values()->all(),
            ])
            ->values()
            ->all();

        return [
            'model' => $inning,
            'number' => (int) $inning->innings_number,
            'status' => $inning->status,
            'batting_team_id' => (int) $inning->batting_team_id,
            'bowling_team_id' => (int) $inning->bowling_team_id,
            'batting_team' => $inning->battingTeam?->short_name ?: $inning->battingTeam?->name ?: 'Team',
            'bowling_team' => $inning->bowlingTeam?->short_name ?: $inning->bowlingTeam?->name ?: 'Team',
            'runs' => (int) $inning->total_runs,
            'wickets' => (int) $inning->wickets,
            'overs' => $inning->oversDisplay($ballsPerOver),
            'maximum_overs' => (int) $inning->maximum_overs,
            'target' => $inning->target_runs ? (int) $inning->target_runs : null,
            'run_rate' => $inning->legal_balls > 0 ? round((float) $inning->total_runs / ((int) $inning->legal_balls / $ballsPerOver), 2) : null,
            'extras' => (int) $deliveries->sum(fn ($d) => (int) ($d->wides + $d->no_balls + $d->byes + $d->leg_byes + $d->penalty_runs)),
            'batting' => $batting->all(),
            'bowling' => $bowling->all(),
            'fow' => $fow,
            'partnerships' => $partnerships,
            'overs_detail' => $overs,
            'recent_balls' => $deliveries->sortByDesc('sequence_number')->take(12)->map(fn ($d) => [
                'over' => $d->over_number.'.'.$d->ball_number,
                'notation' => $d->notation(),
                'runs' => (int) $d->total_runs,
            ])->values()->all(),
        ];
    }
}
