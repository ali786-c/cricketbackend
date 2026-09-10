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
        $data = $this->validated($request);
        if (! empty($data['client_uuid'])) {
            $existing = Fixture::query()
                ->where('client_uuid', $data['client_uuid'])
                ->where('created_by', $request->user()->id)
                ->first();
            if ($existing) {
                return response()->json(['data' => $existing->load(['homeTeam', 'awayTeam', 'match']), 'message' => 'Custom fixture already exists.']);
            }
        }
        $fixture = $this->fixtures->create(null, $data, (int) $request->user()->id);
        return response()->json(['data' => $fixture->load(['homeTeam', 'awayTeam']), 'message' => 'Custom fixture created successfully.'], 201);
    }

    public function update(Request $request, Fixture $fixture): JsonResponse
    {
        $this->authorizeFixture($fixture, $request);
        $fixture = $this->fixtures->update($fixture, $this->validated($request), (int) $request->user()->id);
        return response()->json(['data' => $fixture, 'message' => 'Custom fixture updated successfully.']);
    }

    public function status(Request $request, Fixture $fixture): JsonResponse
    {
        $this->authorizeFixture($fixture, $request);
        $data = $request->validate(['status' => ['required', 'in:scheduled,in_progress,postponed,completed,cancelled']]);
        return response()->json(['data' => $this->fixtures->transition($fixture, $data['status'], (int) $request->user()->id)]);
    }

    public function destroy(Request $request, Fixture $fixture): JsonResponse
    {
        $this->authorizeFixture($fixture, $request);
        abort_if($fixture->match()->exists(), 422, 'This fixture already has an operational match.');

        $fixture->update(['updated_by' => (int) $request->user()->id]);
        $fixture->delete();

        return response()->json(['data' => null, 'message' => 'Custom fixture deleted successfully.']);
    }

    public function createMatch(Request $request, Fixture $fixture): JsonResponse
    {
        $this->authorizeFixture($fixture, $request);
        $match = $this->fixtures->createMatch($fixture, (int) $request->user()->id);
        $match->load(['fixture.homeTeam', 'fixture.awayTeam', 'players.team']);
        return response()->json(['data' => [
            'match_id' => $match->id,
            'fixture_id' => $match->fixture_id,
            'client_uuid' => $match->client_uuid,
            'status' => $match->status,
            'revision' => $match->revision,
            'rule_snapshot' => $match->rule_snapshot,
            'home_team' => $match->fixture?->homeTeam,
            'away_team' => $match->fixture?->awayTeam,
            'players' => $match->players,
        ], 'message' => 'Operational match created for custom fixture.'], 201);
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
            'client_uuid' => ['nullable', 'uuid'],
            'configuration' => ['nullable', 'array'],
            'configuration.format' => ['required_with:configuration', 'string', 'max:30'],
            'configuration.innings_per_side' => ['required_with:configuration', 'integer', 'min:1', 'max:4'],
            'configuration.overs_per_innings' => ['required_with:configuration', 'integer', 'min:1', 'max:100'],
            'configuration.squad_size' => ['nullable', 'integer', 'min:2', 'max:200', 'gte:configuration.playing_xi_size'],
            'configuration.playing_xi_size' => ['required_with:configuration', 'integer', 'min:2', 'max:99'],
            'configuration.maximum_wickets' => ['required_with:configuration', 'integer', 'min:1', 'max:98', 'lt:configuration.playing_xi_size'],
            'configuration.legal_balls_per_over' => ['required_with:configuration', 'integer', 'min:1', 'max:12'],
            'configuration.max_overs_per_bowler' => ['nullable', 'integer', 'min:1', 'max:100'],
            'configuration.ball_type' => ['required_with:configuration', 'in:leather,tennis,hard_ball,tape_ball,indoor'],
            'configuration.no_ball_runs' => ['nullable', 'integer', 'min:0', 'max:10'],
            'configuration.wide_runs' => ['nullable', 'integer', 'min:0', 'max:10'],
            'configuration.wide_runs_to_batsman' => ['nullable', 'boolean'],
            'configuration.noball_runs_to_batsman' => ['nullable', 'boolean'],
            'configuration.last_man_standing' => ['nullable', 'boolean'],
            'configuration.max_balls_per_over' => ['nullable', 'integer', 'min:1', 'max:24'],
            'configuration.max_runs_per_over' => ['nullable', 'integer', 'min:1', 'max:100'],
            'configuration.origin' => ['nullable', 'in:custom'],
            'configuration.version' => ['nullable', 'integer', 'min:1'],
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

    private function authorizeFixture(Fixture $fixture, Request $request): void
    {
        abort_if($fixture->tournament_id !== null, 404);
        abort_if((int) $fixture->created_by !== (int) $request->user()->id && ! $request->user()->hasRole('super_admin'), 403, 'You can only manage custom fixtures you created.');
    }
}
