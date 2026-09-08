<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Tournament;
use App\Models\TournamentPlayer;
use App\Modules\Draft\Services\DraftService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;

class AdminDraftController extends Controller
{
    public function __construct(private readonly DraftService $drafts)
    {
    }

    public function state(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        abort_unless($tournament->draft, 404);
        return response()->json(['data' => $this->drafts->state($tournament->draft, $request->user())]);
    }

    public function start(Request $request, Tournament $tournament): JsonResponse
    {
        return $this->draftResponse($tournament, $this->drafts->startRound($this->draft($tournament, $request), $request->user()), 'Next pick started.');
    }

    public function extend(Request $request, Tournament $tournament): JsonResponse
    {
        $data = $request->validate(['seconds' => ['required', 'integer', 'min:1', 'max:3600']]);
        return $this->draftResponse($tournament, $this->drafts->extendTime($this->draft($tournament, $request), $request->user(), (int) $data['seconds']), 'Draft timer extended.');
    }

    public function skip(Request $request, Tournament $tournament): JsonResponse
    {
        return $this->draftResponse($tournament, $this->drafts->skipExpiredPick($this->draft($tournament, $request), $request->user()), 'Expired pick skipped.');
    }

    public function pause(Request $request, Tournament $tournament): JsonResponse
    {
        return $this->draftResponse($tournament, $this->drafts->pauseDraft($this->draft($tournament, $request), $request->user()), 'Draft paused.');
    }

    public function resume(Request $request, Tournament $tournament): JsonResponse
    {
        return $this->draftResponse($tournament, $this->drafts->resumeDraft($this->draft($tournament, $request), $request->user()), 'Draft resumed.');
    }

    public function undo(Request $request, Tournament $tournament): JsonResponse
    {
        return $this->draftResponse($tournament, $this->drafts->undoLatestPick($this->draft($tournament, $request), $request->user()), 'Latest pick undone.');
    }

    public function selectPlayer(Request $request, Tournament $tournament): JsonResponse
    {
        $data = $request->validate(['pick_number' => ['required', 'integer', 'min:1'], 'tournament_player_id' => ['required', 'integer']]);
        $player = TournamentPlayer::query()->where('tournament_id', $tournament->id)->findOrFail($data['tournament_player_id']);
        return $this->draftResponse($tournament, $this->drafts->adminSelectPlayer($this->draft($tournament, $request), $request->user(), (int) $data['pick_number'], $player), 'Player selected manually.');
    }

    public function removePlayer(Request $request, Tournament $tournament): JsonResponse
    {
        $data = $request->validate(['pick_number' => ['required', 'integer', 'min:1']]);
        return $this->draftResponse($tournament, $this->drafts->removeSelectedPlayer($this->draft($tournament, $request), $request->user(), (int) $data['pick_number']), 'Selected player removed.');
    }

    public function reassignPlayer(Request $request, Tournament $tournament): JsonResponse
    {
        $data = $request->validate(['from_pick_number' => ['required', 'integer', 'min:1'], 'to_pick_number' => ['required', 'integer', 'min:1', 'different:from_pick_number']]);
        return $this->draftResponse($tournament, $this->drafts->reassignPlayer($this->draft($tournament, $request), $request->user(), (int) $data['from_pick_number'], (int) $data['to_pick_number']), 'Player reassigned.');
    }

    private function draftResponse(Tournament $tournament, $draft, string $message): JsonResponse
    {
        return response()->json(['data' => $this->drafts->state($draft), 'message' => $message]);
    }

    private function draft(Tournament $tournament, Request $request)
    {
        $this->authorizeCreator($tournament, $request);
        return $tournament->draft()->firstOrFail();
    }

    private function authorizeCreator(Tournament $tournament, Request $request): void
    {
        abort_if($tournament->creator_id !== $request->user()->id, 403, 'You can only manage drafts for tournaments you created.');
    }
}
