<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use App\Models\Tournament;
use App\Modules\Scoring\Services\MatchService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminMatchController extends Controller
{
    public function __construct(private readonly MatchService $matches)
    {
    }

    public function index(Tournament $tournament): JsonResponse
    {
        return response()->json(['data' => $tournament->matches()->with(['fixture', 'ruleProfile', 'tossWinner', 'players.team'])->latest()->paginate(20)]);
    }

    public function store(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        $data = $request->validate(['home_team_id' => ['required', 'integer', 'different:away_team_id'], 'away_team_id' => ['required', 'integer', 'different:home_team_id'], 'fixture_id' => ['nullable', 'integer'], 'overs_per_innings' => ['nullable', 'integer', 'min:1', 'max:100']]);
        $match = $this->matches->createFromTeams($tournament, (int) $data['home_team_id'], (int) $data['away_team_id'], isset($data['fixture_id']) ? (int) $data['fixture_id'] : null, (int) $request->user()->id, isset($data['overs_per_innings']) ? (int) $data['overs_per_innings'] : null);
        return response()->json(['data' => $match, 'message' => 'Operational match created from the completed draft squad.'], 201);
    }

    public function show(Request $request, Tournament $tournament, CricketMatch $match): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        return response()->json(['data' => $match->load(['fixture', 'ruleProfile', 'players.team', 'innings'])]);
    }

    public function updateOvers(Request $request, Tournament $tournament, CricketMatch $match): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        $data = $request->validate(['overs_per_innings' => ['required', 'integer', 'min:1', 'max:100']]);
        return response()->json(['data' => $this->matches->updateOversPerInnings($match, (int) $data['overs_per_innings'], (int) $request->user()->id), 'message' => 'Match overs updated successfully.']);
    }

    public function playingXi(Request $request, Tournament $tournament, CricketMatch $match, int $team): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        $data = $request->validate(['player_ids' => ['required', 'array'], 'player_ids.*' => ['integer']]);
        return response()->json(['data' => $this->matches->submitPlayingXi($match, $team, $data['player_ids'], (int) $request->user()->id)]);
    }

    public function approveLineup(Request $request, Tournament $tournament, CricketMatch $match): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        return response()->json(['data' => $this->matches->approveLineup($match, (int) $request->user()->id)]);
    }

    public function toss(Request $request, Tournament $tournament, CricketMatch $match): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        $data = $request->validate(['toss_winner_team_id' => ['required', 'integer'], 'toss_decision' => ['required', 'in:bat,field']]);
        return response()->json(['data' => $this->matches->recordToss($match, (int) $data['toss_winner_team_id'], $data['toss_decision'], (int) $request->user()->id)]);
    }

    private function belongs(Tournament $tournament, CricketMatch $match, Request $request): void
    {
        $this->authorizeCreator($tournament, $request);
        abort_unless($match->tournament_id === $tournament->id, 404);
    }

    private function authorizeCreator(Tournament $tournament, Request $request): void
    {
        abort_if($tournament->creator_id !== $request->user()->id, 403, 'You can only manage matches for tournaments you created.');
    }
}
