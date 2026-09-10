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

    public function index(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        return response()->json(['data' => $tournament->fixtures()->with(['homeTeam', 'awayTeam', 'match'])->get()]);
    }

    public function store(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        $data = $this->validated($request);
        if (! empty($data['client_uuid'])) {
            $existing = $tournament->fixtures()
                ->where('client_uuid', $data['client_uuid'])
                ->where('created_by', $request->user()->id)
                ->first();
            if ($existing) {
                return response()->json(['data' => $existing->load(['homeTeam', 'awayTeam', 'match']), 'message' => 'Fixture already exists.']);
            }
        }
        $fixture = $this->fixtures->create($tournament, $data, (int) $request->user()->id);
        return response()->json(['data' => $fixture->load(['homeTeam', 'awayTeam']), 'message' => 'Fixture created successfully.'], 201);
    }

    public function update(Request $request, Tournament $tournament, Fixture $fixture): JsonResponse
    {
        $this->belongs($tournament, $fixture, $request);
        $fixture = $this->fixtures->update($fixture, $this->validated($request), (int) $request->user()->id);
        return response()->json(['data' => $fixture, 'message' => 'Fixture updated successfully.']);
    }

    public function status(Request $request, Tournament $tournament, Fixture $fixture): JsonResponse
    {
        $this->belongs($tournament, $fixture, $request);
        $data = $request->validate(['status' => ['required', 'in:scheduled,in_progress,postponed,completed,cancelled']]);
        return response()->json(['data' => $this->fixtures->transition($fixture, $data['status'], (int) $request->user()->id)]);
    }

    public function createMatch(Request $request, Tournament $tournament, Fixture $fixture): JsonResponse
    {
        $this->belongs($tournament, $fixture, $request);
        $match = $this->fixtures->createMatch($fixture, (int) $request->user()->id);
        return response()->json(['data' => $this->matchCreatedData($match), 'message' => 'Operational match created.'], 201);
    }

    private function validated(Request $request): array
    {
        $data = $request->validate([
            'home_team_id' => ['required', 'integer'],
            'away_team_id' => ['required', 'integer'],
            'home_team_name' => ['nullable', 'string', 'max:100'],
            'away_team_name' => ['nullable', 'string', 'max:100'],
            'round_number' => ['nullable', 'integer', 'min:1', 'max:999'], 
            'round_name' => ['nullable', 'string', 'max:100'], 
            'match_number' => ['nullable', 'integer', 'min:1', 'max:9999'], 
            'scheduled_at' => ['required', 'date'], 
            'venue' => ['nullable', 'string', 'max:255'], 
            'city' => ['nullable', 'string', 'max:100'], 
            'timezone' => ['required', 'timezone'], 
            'notes' => ['nullable', 'string', 'max:2000'],
            'client_uuid' => ['nullable', 'uuid'],
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
        
        // Let FixtureService handle if these teams belong to the tournament or not

        return $data;
    }

    /**
     * Delete a fixture.
     */
    public function destroy(Request $request, Tournament $tournament, Fixture $fixture): JsonResponse
    {
        $this->belongs($tournament, $fixture, $request);
        $fixture->delete();
        return response()->json(['message' => 'Fixture deleted successfully.']);
    }

    private function belongs(Tournament $tournament, Fixture $fixture, Request $request): void
    {
        $this->authorizeCreator($tournament, $request);
        abort_unless($fixture->tournament_id === $tournament->id, 404);
    }

    private function authorizeCreator(Tournament $tournament, Request $request): void
    {
        abort_if($tournament->creator_id !== $request->user()->id, 403, 'You can only manage fixtures for tournaments you created.');
    }

    private function matchCreatedData($match): array
    {
        $match->load(['fixture.homeTeam', 'fixture.awayTeam', 'players.team']);
        return [
            'match_id' => $match->id,
            'fixture_id' => $match->fixture_id,
            'client_uuid' => $match->client_uuid,
            'status' => $match->status,
            'revision' => $match->revision,
            'rule_snapshot' => $match->rule_snapshot,
            'home_team' => $match->fixture?->homeTeam,
            'away_team' => $match->fixture?->awayTeam,
            'players' => $match->players,
        ];
    }
}
