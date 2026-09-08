<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Stage;
use App\Models\Tournament;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminStageController extends Controller
{
    /**
     * List stages for a tournament.
     */
    public function index(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        $stages = $tournament->stages()->orderBy('order')->get();
        return response()->json(['data' => $stages]);
    }

    /**
     * Create a new stage.
     */
    public function store(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        $data = $request->validate([
            'name' => ['required', 'string', 'max:100'],
            'type' => ['sometimes', 'string', 'in:points_table,league,knockout,match,series,qualifier,eliminator,quarter_final,semi_final,final,custom'],
            'order' => ['nullable', 'integer', 'min:0'],
            'number_of_teams' => ['nullable', 'integer', 'min:2'],
            'matches_per_team' => ['nullable', 'integer', 'min:1'],
            'points_for_win' => ['nullable', 'integer', 'min:0'],
            'points_for_tie' => ['nullable', 'integer', 'min:0'],
            'points_for_no_result' => ['nullable', 'integer', 'min:0'],
            'points_for_loss' => ['nullable', 'integer', 'min:0'],
            'qualification_rule' => ['nullable', 'string', 'in:top_1,top_2,top_4,custom'],
            'qualification_count' => ['nullable', 'integer', 'min:1'],
        ]);

        // Auto-set order if not provided
        if (!isset($data['order'])) {
            $data['order'] = $tournament->stages()->max('order') + 1;
        }

        $data['status'] = 'draft';
        $stage = $tournament->stages()->create($data);

        return response()->json(['data' => $stage, 'message' => 'Stage created successfully.'], 201);
    }

    /**
     * Show a single stage.
     */
    public function show(Request $request, Tournament $tournament, Stage $stage): JsonResponse
    {
        $this->belongs($tournament, $stage, $request);
        return response()->json(['data' => $stage->load('fixtures')]);
    }

    /**
     * Update a stage.
     */
    public function update(Request $request, Tournament $tournament, Stage $stage): JsonResponse
    {
        $this->belongs($tournament, $stage, $request);

        $data = $request->validate([
            'name' => ['sometimes', 'string', 'max:100'],
            'type' => ['sometimes', 'string', 'in:points_table,league,knockout,match,series,qualifier,eliminator,quarter_final,semi_final,final,custom'],
            'order' => ['sometimes', 'integer', 'min:0'],
            'number_of_teams' => ['sometimes', 'integer', 'min:2'],
            'matches_per_team' => ['sometimes', 'integer', 'min:1'],
            'points_for_win' => ['sometimes', 'integer', 'min:0'],
            'points_for_tie' => ['sometimes', 'integer', 'min:0'],
            'points_for_no_result' => ['sometimes', 'integer', 'min:0'],
            'points_for_loss' => ['sometimes', 'integer', 'min:0'],
            'qualification_rule' => ['sometimes', 'string', 'in:top_1,top_2,top_4,custom'],
            'qualification_count' => ['sometimes', 'integer', 'min:1'],
            'status' => ['sometimes', 'string', 'in:draft,active,completed'],
        ]);

        $stage->update($data);

        return response()->json(['data' => $stage->fresh(), 'message' => 'Stage updated successfully.']);
    }

    /**
     * Delete a stage.
     */
    public function destroy(Request $request, Tournament $tournament, Stage $stage): JsonResponse
    {
        $this->belongs($tournament, $stage, $request);

        // Check if stage has fixtures
        if ($stage->fixtures()->count() > 0) {
            abort(422, 'Cannot delete a stage that has fixtures.');
        }

        $stage->delete();

        return response()->json(['message' => 'Stage deleted successfully.']);
    }

    private function belongs(Tournament $tournament, Stage $stage, Request $request): void
    {
        $this->authorizeCreator($tournament, $request);
        abort_unless($stage->tournament_id === $tournament->id, 404);
    }

    private function authorizeCreator(Tournament $tournament, Request $request): void
    {
        abort_if($tournament->creator_id !== $request->user()->id, 403, 'You can only manage stages for tournaments you created.');
    }
}
