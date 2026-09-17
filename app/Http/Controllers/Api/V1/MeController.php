<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use App\Models\Fixture;
use App\Models\Tournament;
use App\Support\ViewerPermissions;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class MeController extends Controller
{
    public function tournaments(Request $request): JsonResponse
    {
        $user = $request->user();
        $items = Tournament::query()
            ->where('creator_id', $user->id)
            ->with('cricketRuleProfile')
            ->withCount(['teams', 'fixtures', 'matches'])
            ->latest()
            ->get()
            ->map(fn (Tournament $tournament) => [
                'id' => $tournament->id,
                'owner_user_id' => $user->id,
                'name' => $tournament->name,
                'slug' => $tournament->slug,
                'status' => $tournament->status,
                'is_public' => (bool) $tournament->is_public,
                'starts_on' => $tournament->starts_on?->toDateString(),
                'venue' => $tournament->venue ?: $tournament->location,
                'city' => $tournament->city,
                'teams_count' => $tournament->teams_count,
                'fixtures_count' => $tournament->fixtures_count,
                'matches_count' => $tournament->matches_count,
                'viewer_permissions' => ViewerPermissions::forTournament($user, $tournament),
            ])->values();

        return response()->json(['data' => $items]);
    }

    public function fixtures(Request $request): JsonResponse
    {
        $user = $request->user();
        $items = Fixture::query()
            ->where(function ($query) use ($user) {
                $query->where(function ($custom) use ($user) {
                    $custom->whereNull('tournament_id')->where('created_by', $user->id);
                })->orWhereHas('tournament', fn ($tournament) => $tournament->where('creator_id', $user->id));
            })
            ->with(['tournament', 'homeTeam', 'awayTeam', 'match'])
            ->latest()
            ->get()
            ->map(fn (Fixture $fixture) => $this->fixturePayload($fixture, $user));

        return response()->json(['data' => $items->values()]);
    }

    public function matches(Request $request): JsonResponse
    {
        $user = $request->user();
        $items = CricketMatch::query()
            ->where(function ($query) use ($user) {
                $query->where(function ($custom) use ($user) {
                    $custom->whereNull('tournament_id')->where('created_by', $user->id);
                })->orWhereHas('tournament', fn ($tournament) => $tournament->where('creator_id', $user->id));
            })
            ->with(['tournament', 'fixture.homeTeam', 'fixture.awayTeam'])
            ->latest()
            ->get()
            ->map(fn (CricketMatch $match) => [
                'id' => $match->id,
                'owner_user_id' => $user->id,
                'fixture_id' => $match->fixture_id,
                'tournament_id' => $match->tournament_id,
                'tournament_name' => $match->tournament?->name,
                'status' => $match->status,
                'revision' => $match->revision,
                'result_summary' => $match->result_summary,
                'home_team' => $this->teamPayload($match->fixture?->homeTeam),
                'away_team' => $this->teamPayload($match->fixture?->awayTeam),
                'scheduled_at' => $match->fixture?->scheduled_at?->toIso8601String(),
                'venue' => $match->fixture?->venue,
                'viewer_permissions' => ViewerPermissions::forMatch($user, $match),
            ])->values();

        return response()->json(['data' => $items]);
    }

    private function fixturePayload(Fixture $fixture, $user): array
    {
        return [
            'id' => $fixture->id,
            'owner_user_id' => $user->id,
            'match_id' => $fixture->match?->id,
            'tournament_id' => $fixture->tournament_id,
            'tournament_name' => $fixture->tournament?->name,
            'status' => $fixture->status,
            'scheduled_at' => $fixture->scheduled_at?->toIso8601String(),
            'venue' => $fixture->venue,
            'city' => $fixture->city,
            'home_team' => $this->teamPayload($fixture->homeTeam),
            'away_team' => $this->teamPayload($fixture->awayTeam),
            'viewer_permissions' => ViewerPermissions::forFixture($user, $fixture),
        ];
    }

    private function teamPayload($team): ?array
    {
        return $team ? ['id' => $team->id, 'name' => $team->name, 'short_name' => $team->short_name] : null;
    }
}
