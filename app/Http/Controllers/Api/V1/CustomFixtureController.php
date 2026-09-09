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

    public function createMatch(Request $request, Fixture $fixture): JsonResponse
    {
        $match = $this->fixtures->createMatch($fixture, (int) $request->user()->id);
        return response()->json(['data' => ['match_id' => $match->id, 'status' => $match->status], 'message' => 'Operational match created for custom fixture.'], 201);
    }
    
    private function validated(Request $request): array
    {
        // Simple validation rule that ignores tournament specific stuff
        return $request->validate([
            'home_team_id' => ['required', 'integer', 'exists:teams,id'],
            'away_team_id' => ['required', 'integer', 'exists:teams,id', 'different:home_team_id'],
            'scheduled_at' => ['required', 'date'],
            'venue' => ['nullable', 'string', 'max:100'],
            'city' => ['nullable', 'string', 'max:50'],
            'timezone' => ['nullable', 'string', 'timezone'],
        ]);
    }
}
