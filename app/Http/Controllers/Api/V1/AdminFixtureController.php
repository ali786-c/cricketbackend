<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Fixture;
use App\Models\Tournament;
use App\Modules\Tournament\Services\FixtureService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminFixtureController extends Controller
{
    public function __construct(private readonly FixtureService $fixtures)
    {
    }

    public function index(Tournament $tournament): JsonResponse
    {
        return response()->json(['data' => $tournament->fixtures()->with(['homeTeam', 'awayTeam', 'match'])->paginate(20)]);
    }

    public function store(Request $request, Tournament $tournament): JsonResponse
    {
        $fixture = $this->fixtures->create($tournament, $this->validated($request), (int) $request->user()->id);
        return response()->json(['data' => $fixture->load(['homeTeam', 'awayTeam']), 'message' => 'Fixture created successfully.'], 201);
    }

    public function update(Request $request, Tournament $tournament, Fixture $fixture): JsonResponse
    {
        $this->belongs($tournament, $fixture);
        $fixture = $this->fixtures->update($fixture, $this->validated($request), (int) $request->user()->id);
        return response()->json(['data' => $fixture, 'message' => 'Fixture updated successfully.']);
    }

    public function status(Request $request, Tournament $tournament, Fixture $fixture): JsonResponse
    {
        $this->belongs($tournament, $fixture);
        $data = $request->validate(['status' => ['required', 'in:scheduled,in_progress,postponed,completed,cancelled']]);
        return response()->json(['data' => $this->fixtures->transition($fixture, $data['status'], (int) $request->user()->id)]);
    }

    public function createMatch(Request $request, Tournament $tournament, Fixture $fixture): JsonResponse
    {
        $this->belongs($tournament, $fixture);
        $match = $this->fixtures->createMatch($fixture, (int) $request->user()->id);
        return response()->json(['data' => ['match_id' => $match->id, 'status' => $match->status], 'message' => 'Operational match created.'], 201);
    }

    private function validated(Request $request): array
    {
        return $request->validate(['home_team_id' => ['required', 'integer'], 'away_team_id' => ['required', 'integer', 'different:home_team_id'], 'round_number' => ['nullable', 'integer', 'min:1', 'max:999'], 'round_name' => ['nullable', 'string', 'max:100'], 'match_number' => ['nullable', 'integer', 'min:1', 'max:9999'], 'scheduled_at' => ['required', 'date'], 'venue' => ['nullable', 'string', 'max:255'], 'city' => ['nullable', 'string', 'max:100'], 'timezone' => ['required', 'timezone'], 'notes' => ['nullable', 'string', 'max:2000']]);
    }

    /**
     * Delete a fixture.
     */
    public function destroy(Tournament $tournament, Fixture $fixture): JsonResponse
    {
        $this->belongs($tournament, $fixture);
        $fixture->delete();
        return response()->json(['message' => 'Fixture deleted successfully.']);
    }

    private function belongs(Tournament $tournament, Fixture $fixture): void
    {
        abort_unless($fixture->tournament_id === $tournament->id, 404);
    }
}
