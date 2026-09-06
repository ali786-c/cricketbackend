<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Modules\Analytics\Services\UnifiedSearchService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class SearchController extends Controller
{
    /**
     * GET /api/v1/search?q=QUERY
     *
     * Query params:
     *  - q: search term (required, min 2 chars)
     *  - type: comma-separated types to search (players, teams, tournaments, matches). Default: all
     *  - limit: max results per type (1-50, default 10)
     *  - city: filter players by city
     *  - playing_role: filter players by role (batsman, bowler, all-rounder, wicketkeeper)
     *  - tournament_id: filter teams/matches by tournament
     *  - status: filter tournaments by status
     */
    public function search(Request $request, UnifiedSearchService $searchService): JsonResponse
    {
        $validated = $request->validate([
            'q' => ['required', 'string', 'min:2', 'max:100'],
            'type' => ['nullable', 'string'],
            'limit' => ['nullable', 'integer', 'min:1', 'max:50'],
            'city' => ['nullable', 'string', 'max:100'],
            'playing_role' => ['nullable', 'string', 'max:50'],
            'tournament_id' => ['nullable', 'integer'],
            'status' => ['nullable', 'string'],
        ]);

        $types = $validated['type']
            ? array_map('trim', explode(',', $validated['type']))
            : ['players', 'teams', 'tournaments', 'matches'];

        $results = $searchService->search($validated['q'], [
            'types' => $types,
            'limit' => $validated['limit'] ?? 10,
            'city' => $validated['city'] ?? null,
            'playing_role' => $validated['playing_role'] ?? null,
            'tournament_id' => $validated['tournament_id'] ?? null,
            'status' => $validated['status'] ?? null,
        ]);

        return response()->json(['data' => $results]);
    }

    /**
     * GET /api/v1/search/lookup/CODE
     *
     * Look up a single entity by its unique code.
     * Supports: TEAM-XXXXX, PLR-XXXXX, or tournament slug
     */
    public function lookup(string $code, UnifiedSearchService $searchService): JsonResponse
    {
        $result = $searchService->findByCode($code);

        if (!$result) {
            return response()->json([
                'error' => 'not_found',
                'message' => "No entity found with code: {$code}",
            ], 404);
        }

        return response()->json(['data' => $result]);
    }
}
