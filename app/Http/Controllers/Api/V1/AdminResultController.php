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
        $this->authorizeCreator($match, $request);
        return response()->json(['data' => $this->results->submit($match, (int) $request->user()->id), 'message' => 'Match result submitted for approval.']);
    }

    public function approve(Request $request, CricketMatch $match): JsonResponse
    {
        $this->authorizeCreator($match, $request);
        return response()->json(['data' => $this->results->approve($match, (int) $request->user()->id), 'message' => 'Match result approved and standings rebuilt.']);
    }

    public function reject(Request $request, CricketMatch $match): JsonResponse
    {
        $this->authorizeCreator($match, $request);
        return response()->json(['data' => $this->results->reject($match, (int) $request->user()->id), 'message' => 'Result rejected and returned for correction.']);
    }

    public function exception(Request $request, CricketMatch $match): JsonResponse
    {
        $this->authorizeCreator($match, $request);
        $data = $request->validate(['type' => ['required', 'in:no_result,abandoned,cancelled']]);
        return response()->json(['data' => $this->results->recordException($match, (int) $request->user()->id, $data['type'])]);
    }

    private function authorizeCreator(CricketMatch $match, Request $request): void
    {
        $ownerId = $match->tournament?->creator_id ?? $match->created_by;
        abort_if((int) $ownerId !== (int) $request->user()->id && ! $request->user()->hasRole('super_admin'), 403, 'You can only manage results for matches you created.');
    }
}
