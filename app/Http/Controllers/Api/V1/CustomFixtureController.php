<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Fixture;
use App\Modules\Tournament\Services\FixtureService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CustomFixtureController extends Controller
{
    public function __construct(private readonly FixtureService $fixtures)
    {
    }

    public function store(Request $request): JsonResponse
    {
        $fixture = $this->fixtures->create(null, $this->validated($request), (int) $request->user()->id);
        return response()->json(['data' => $fixture->load(['homeTeam', 'awayTeam']), 'message' => 'Custom fixture created successfully.'], 201);
    }

    public function update(Request $request, Fixture $fixture): JsonResponse
    {
        $fixture = $this->fixtures->update($fixture, $this->validated($request), (int) $request->user()->id);
        return response()->json(['data' => $fixture, 'message' => 'Custom fixture updated successfully.']);
    }

    public function status(Request $request, Fixture $fixture): JsonResponse
    {
        $data = $request->validate(['status' => ['required', 'in:scheduled,in_progress,postponed,completed,cancelled']]);
        return response()->json(['data' => $this->fixtures->transition($fixture, $data['status'], (int) $request->user()->id)]);
    }

    public function destroy(Request $request, Fixture $fixture): JsonResponse
    {
        abort_if($fixture->match()->exists(), 422, 'This fixture already has an operational match.');

        $fixture->update(['updated_by' => (int) $request->user()->id]);
        $fixture->delete();

        return response()->json(['data' => null, 'message' => 'Custom fixture deleted successfully.']);
    }

    public function createMatch(Request $request, Fixture $fixture): JsonResponse
    {
        $match = $this->fixtures->createMatch($fixture, (int) $request->user()->id);
        return response()->json(['data' => ['match_id' => $match->id, 'status' => $match->status], 'message' => 'Operational match created for custom fixture.'], 201);
    }
    
    private function validated(Request $request): array
    {
        $data = $request->validate([
            'home_team_id' => ['required', 'integer'],
            'away_team_id' => ['required', 'integer'],
            'home_team_name' => ['nullable', 'string', 'max:100'],
            'away_team_name' => ['nullable', 'string', 'max:100'],
            'scheduled_at' => ['required', 'date'],
            'venue' => ['nullable', 'string', 'max:100'],
            'city' => ['nullable', 'string', 'max:50'],
            'timezone' => ['nullable', 'string', 'timezone'],
        ]);

        // Auto-create global teams if ID is 0 and name is provided
        if ($data['home_team_id'] === 0 && !empty($data['home_team_name'])) {
            $team = \App\Models\Team::firstOrCreate(
                ['name' => $data['home_team_name']],
                ['is_active' => true, 'status' => 'pending', 'creator_id' => $request->user()?->id]
            );
            $data['home_team_id'] = $team->id;
        }
        
        if ($data['away_team_id'] === 0 && !empty($data['away_team_name'])) {
            $team = \App\Models\Team::firstOrCreate(
                ['name' => $data['away_team_name']],
                ['is_active' => true, 'status' => 'pending', 'creator_id' => $request->user()?->id]
            );
            $data['away_team_id'] = $team->id;
        }

        // Validate existence after creation
        if (!\App\Models\Team::where('id', $data['home_team_id'])->exists()) {
            abort(422, 'Invalid home team.');
        }
        if (!\App\Models\Team::where('id', $data['away_team_id'])->exists() || $data['home_team_id'] === $data['away_team_id']) {
            abort(422, 'Invalid away team or teams are the same.');
        }

        return $data;
    }
}
