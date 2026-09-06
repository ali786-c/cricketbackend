<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use App\Modules\Scoring\Services\MatchScoringService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class MatchScoringController extends Controller
{
    public function __construct(private readonly MatchScoringService $scoring)
    {
    }

    public function show(CricketMatch $match): View
    {
        abort_unless($match->status === 'live', 409, 'Scoring is only available for a live match.');
        $match->load(['tournament', 'ruleProfile', 'players.team', 'innings.battingTeam', 'innings.bowlingTeam', 'innings.deliveries.striker', 'innings.deliveries.nonStriker', 'innings.deliveries.bowler', 'innings.deliveries.wicket']);
        $innings = $match->innings->firstWhere('id', $match->current_innings_id);
        return view('admin.matches.scorer', [
            'tournament' => $match->tournament,
            'match' => $match,
            'innings' => $innings,
            'batters' => $match->players->where('team_id', $innings?->batting_team_id)->where('selection_type', 'playing_xi'),
            'bowlers' => $match->players->where('team_id', $innings?->bowling_team_id)->where('selection_type', 'playing_xi'),
        ]);
    }

    public function store(Request $request, CricketMatch $match): RedirectResponse
    {
        $validated = $request->validate([
            'striker_id' => ['required', 'integer'],
            'non_striker_id' => ['required', 'integer', 'different:striker_id'],
            'bowler_id' => ['required', 'integer'],
            'runs_off_bat' => ['nullable', 'integer', 'min:0', 'max:6'],
            'wides' => ['nullable', 'integer', 'min:0', 'max:6'],
            'no_balls' => ['nullable', 'integer', 'min:0', 'max:6'],
            'byes' => ['nullable', 'integer', 'min:0', 'max:6'],
            'leg_byes' => ['nullable', 'integer', 'min:0', 'max:6'],
            'penalty_runs' => ['nullable', 'integer', 'min:0', 'max:6'],
            'commentary' => ['nullable', 'string', 'max:1000'],
            'expected_revision' => ['nullable', 'integer', 'min:0'],
            'wicket' => ['nullable', 'array'],
            'wicket.dismissed_player_id' => ['nullable', 'integer'],
            'wicket.dismissal_type' => ['nullable', 'string'],
            'wicket.fielder_id' => ['nullable', 'integer'],
            'wicket.runs_completed' => ['nullable', 'integer', 'min:0', 'max:6'],
            'wicket.notes' => ['nullable', 'string', 'max:500'],
        ]);
        $this->scoring->recordDelivery($match, $validated, (int) $request->user()->id, isset($validated['expected_revision']) ? (int) $validated['expected_revision'] : null);
        return back()->with('status', 'Delivery recorded successfully.');
    }

    public function startNextInnings(Request $request, CricketMatch $match): RedirectResponse
    {
        $this->scoring->startNextInnings($match, (int) $request->user()->id);
        return back()->with('status', 'The next innings is live.');
    }

    public function undo(Request $request, CricketMatch $match): RedirectResponse
    {
        $validated = $request->validate(['reason' => ['required', 'string', 'min:5', 'max:500']]);
        $this->scoring->undoLastDelivery($match, (int) $request->user()->id, $validated['reason']);
        return back()->with('status', 'The latest delivery was voided and the scorecard was rebuilt.');
    }
}
