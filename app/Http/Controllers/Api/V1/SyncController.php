<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Tournament;
use App\Modules\Draft\Services\DraftService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class SyncController extends Controller
{
    public function __construct(private readonly DraftService $drafts)
    {
    }

    public function tournament(Request $request, Tournament $tournament): JsonResponse
    {
        abort_unless($tournament->publiclyVisibleNow(), 404);
        $draft = $tournament->draft;
        $draftState = $draft ? $this->drafts->state($draft, $request->user()) : null;
        $matches = $tournament->matches()->whereIn('status', ['live', 'completed', 'result_pending', 'approved'])->with(['fixture.homeTeam', 'fixture.awayTeam', 'tossWinner', 'winner', 'ruleProfile', 'innings'])->latest('last_event_at')->limit(30)->get();
        $revision = max((int) ($draftState['revision'] ?? 0), (int) $matches->max('revision'));
        $requested = (int) $request->integer('revision', -1);
        $stages = $tournament->stages()->orderBy('order')->get();

        // Map matches to expose homeTeam/awayTeam at top level for mobile clients.
        $matchesData = $matches->map(fn ($match) => array_merge(
            $match->only(['id', 'tournament_id', 'status', 'overs_per_innings', 'revision', 'result_summary', 'toss_decision', 'started_at', 'completed_at', 'last_event_at']),
            [
                'home_team' => $match->fixture?->homeTeam ? $match->fixture->homeTeam->only(['id', 'name', 'short_name', 'logo_path']) : null,
                'away_team' => $match->fixture?->awayTeam ? $match->fixture->awayTeam->only(['id', 'name', 'short_name', 'logo_path']) : null,
                'toss_winner' => $match->tossWinner ? $match->tossWinner->only(['id', 'name', 'short_name']) : null,
                'winner' => $match->winner ? $match->winner->only(['id', 'name', 'short_name']) : null,
                'innings_count' => $match->innings->count(),
            ]
        ));

        return response()->json([
            'data' => [
                'changed' => $requested < 0 || $requested !== $revision,
                'revision' => $revision,
                'server_time' => now()->toIso8601String(),
                'tournament' => $tournament->only([
                    'id', 'name', 'season_name', 'slug', 'status', 'is_public',
                    'published_at', 'organizer_name', 'contact_info',
                    'competition_structure', 'tournament_code', 'ball_type',
                    'default_overs_per_innings', 'squad_size',
                ]),
                'draft' => $draftState,
                'stages' => $stages,
                'fixtures' => $tournament->fixtures()->with(['homeTeam', 'awayTeam'])->latest('scheduled_at')->limit(50)->get(),
                'matches' => $matchesData,
                'standings' => $tournament->standings()->with('team')->orderByDesc('points')->orderByDesc('net_run_rate')->get(),
            ],
        ]);
    }
}
