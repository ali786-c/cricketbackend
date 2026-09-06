<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Tournament;
use Illuminate\Http\JsonResponse;

class TournamentController extends Controller
{
    public function index(): JsonResponse
    {
        $tournaments = Tournament::query()->where('is_public', true)->whereIn('status', ['registration', 'ready', 'live', 'completed'])->with('cricketRuleProfile')->latest('starts_on')->get();
        return response()->json(['data' => $tournaments->map(fn ($tournament) => $this->summary($tournament))->values()]);
    }

    public function show(Tournament $tournament): JsonResponse
    {
        $this->ensurePublic($tournament);
        $tournament->load('cricketRuleProfile');
        return response()->json(['data' => $this->summary($tournament, true)]);
    }

    public function teams(Tournament $tournament): JsonResponse
    {
        $this->ensurePublic($tournament);
        return response()->json(['data' => $tournament->teams()->where('is_active', true)->withCount(['matchPlayers'])->orderBy('display_order')->get()->map(fn ($team) => ['id' => $team->id, 'name' => $team->name, 'short_name' => $team->short_name, 'logo_path' => $team->logo_path, 'squad_count' => $team->match_players_count, 'creator_id' => $team->creator_id])->values()]);
    }

    public function players(Tournament $tournament): JsonResponse
    {
        $this->ensurePublic($tournament);
        return response()->json(['data' => $tournament->tournamentPlayers()->where('status', 'approved')->with('playerProfile')->get()->map(fn ($player) => ['id' => $player->id, 'full_name' => $player->playerProfile?->full_name, 'playing_role' => $player->playerProfile?->playing_role, 'city' => $player->playerProfile?->city])->values()]);
    }

    public function standings(Tournament $tournament): JsonResponse
    {
        $this->ensurePublic($tournament);
        return response()->json(['data' => $tournament->standings()->with('team')->orderByDesc('points')->orderByDesc('net_run_rate')->get()->map(fn ($standing) => ['position' => null, 'team' => ['id' => $standing->team?->id, 'name' => $standing->team?->name, 'short_name' => $standing->team?->short_name], 'played' => $standing->played, 'wins' => $standing->wins, 'losses' => $standing->losses, 'ties' => $standing->ties, 'no_results' => $standing->no_results, 'points' => $standing->points, 'net_run_rate' => (float) $standing->net_run_rate])->values()]);
    }

    public function fixtures(Tournament $tournament): JsonResponse
    {
        $this->ensurePublic($tournament);
        $fixtures = $tournament->fixtures()->with(['homeTeam', 'awayTeam', 'match'])->get();
        return response()->json(['data' => $fixtures->map(fn ($fixture) => [
            'id' => $fixture->id,
            'round_number' => $fixture->round_number,
            'round_name' => $fixture->round_name,
            'match_number' => $fixture->match_number,
            'scheduled_at' => $fixture->scheduled_at?->toIso8601String(),
            'timezone' => $fixture->timezone,
            'venue' => $fixture->venue,
            'city' => $fixture->city,
            'status' => $fixture->status,
            'home_team' => $fixture->homeTeam ? ['id' => $fixture->homeTeam->id, 'name' => $fixture->homeTeam->name, 'short_name' => $fixture->homeTeam->short_name] : null,
            'away_team' => $fixture->awayTeam ? ['id' => $fixture->awayTeam->id, 'name' => $fixture->awayTeam->name, 'short_name' => $fixture->awayTeam->short_name] : null,
            'match_id' => $fixture->match?->id,
            'match_status' => $fixture->match?->status,
        ])->values()]);
    }

    private function summary(Tournament $tournament, bool $detail = false): array
    {
        $data = [
            'id' => $tournament->id,
            'name' => $tournament->name,
            'season_name' => $tournament->season_name,
            'slug' => $tournament->slug,
            'status' => $tournament->status,
            'venue' => $tournament->venue ?: $tournament->location,
            'city' => $tournament->city,
            'timezone' => $tournament->timezone,
            'starts_on' => $tournament->starts_on?->toDateString(),
            'ends_on' => $tournament->ends_on?->toDateString(),
            'rule_profile' => $tournament->cricketRuleProfile ? [
                'name' => $tournament->cricketRuleProfile->name,
                'format' => $tournament->cricketRuleProfile->format,
                'overs_per_innings' => $tournament->cricketRuleProfile->overs_per_innings,
                'legal_balls_per_over' => $tournament->cricketRuleProfile->legal_balls_per_over,
            ] : null,
        ];
        if ($detail) $data['fixtures_count'] = $tournament->fixtures()->count();
        return $data;
    }

    public function compare(\Illuminate\Http\Request $request, Tournament $tournament, \App\Modules\Analytics\Services\PlayerComparisonService $comparisonService): JsonResponse
    {
        $this->ensurePublic($tournament);
        $validated = $request->validate([
            'player1_id' => ['required', 'integer'],
            'player2_id' => ['required', 'integer'],
        ]);
        $comparison = $comparisonService->comparePlayers($tournament, (int) $validated['player1_id'], (int) $validated['player2_id']);
        return response()->json(['data' => $comparison]);
    }

    private function ensurePublic(Tournament $tournament): void
    {
        abort_unless($tournament->publiclyVisibleNow(), 404);
    }

    public function squad(Team $team, \App\Modules\Analytics\Services\TeamComparisonService $teamService): JsonResponse
    {
        return response()->json([
            'data' => $teamService->getClassifiedSquad($team)
        ]);
    }

    public function updateDesignations(\Illuminate\Http\Request $request, Team $team): JsonResponse
    {
        $validated = $request->validate([
            'captain_player_id' => ['nullable', 'integer', 'exists:tournament_players,id'],
            'vice_captain_player_id' => ['nullable', 'integer', 'exists:tournament_players,id', 'different:captain_player_id'],
            'wicketkeeper_player_id' => ['nullable', 'integer', 'exists:tournament_players,id'],
        ]);

        \DB::transaction(function () use ($team, $validated) {
            \App\Models\DraftPick::query()->where('team_id', $team->id)->update([
                'is_captain' => false,
                'is_vice_captain' => false,
                'is_wicketkeeper' => false,
            ]);

            if (!empty($validated['captain_player_id'])) {
                \App\Models\DraftPick::query()
                    ->where('team_id', $team->id)
                    ->where('tournament_player_id', $validated['captain_player_id'])
                    ->update(['is_captain' => true]);
            }

            if (!empty($validated['vice_captain_player_id'])) {
                \App\Models\DraftPick::query()
                    ->where('team_id', $team->id)
                    ->where('tournament_player_id', $validated['vice_captain_player_id'])
                    ->update(['is_vice_captain' => true]);
            }

            if (!empty($validated['wicketkeeper_player_id'])) {
                \App\Models\DraftPick::query()
                    ->where('team_id', $team->id)
                    ->where('tournament_player_id', $validated['wicketkeeper_player_id'])
                    ->update(['is_wicketkeeper' => true]);
            }
        });

        return response()->json(['message' => 'Team officer designations updated successfully.']);
    }

    public function compareTeams(\Illuminate\Http\Request $request, \App\Modules\Analytics\Services\TeamComparisonService $teamService): JsonResponse
    {
        $validated = $request->validate([
            'team1_id' => ['required', 'integer'],
            'team2_id' => ['required', 'integer', 'different:team1_id'],
        ]);

        return response()->json([
            'data' => $teamService->compareTeams((int) $validated['team1_id'], (int) $validated['team2_id'])
        ]);
    }

    public function simulateStandings(Tournament $tournament, \App\Modules\Analytics\Services\StandingsSimulationService $simulationService): JsonResponse
    {
        $this->ensurePublic($tournament);
        return response()->json([
            'data' => $simulationService->simulateQualifications($tournament)
        ]);
    }
}
