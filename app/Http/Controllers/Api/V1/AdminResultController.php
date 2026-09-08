<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use App\Modules\Scoring\Services\MatchResultService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminResultController extends Controller
{
    public function __construct(private readonly MatchResultService $results)
    {
    }

    public function submit(Request $request, CricketMatch $match): JsonResponse
    {
        $this->authorizeCreator($match->tournament, $request);
        return response()->json(['data' => $this->results->submit($match, (int) $request->user()->id), 'message' => 'Match result submitted for approval.']);
    }

    public function approve(Request $request, CricketMatch $match): JsonResponse
    {
        $this->authorizeCreator($match->tournament, $request);
        return response()->json(['data' => $this->results->approve($match, (int) $request->user()->id), 'message' => 'Match result approved and standings rebuilt.']);
    }

    private function authorizeCreator(\App\Models\Tournament $tournament, Request $request): void
    {
        abort_if($tournament->creator_id !== $request->user()->id, 403, 'You can only manage results for tournaments you created.');
    }
}
