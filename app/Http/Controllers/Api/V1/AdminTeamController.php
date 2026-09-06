<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Team;
use App\Models\TeamCaptain;
use App\Models\Tournament;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

class AdminTeamController extends Controller
{
    /**
     * List teams for a tournament.
     */
    public function index(Tournament $tournament): JsonResponse
    {
        $teams = $tournament->teams()
            ->with('activeCaptain.user')
            ->withCount('draftPicks')
            ->orderBy('display_order')
            ->get();

        return response()->json(['data' => $teams]);
    }

    /**
     * Create a new team in a tournament.
     */
    public function store(Request $request, Tournament $tournament): JsonResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:100'],
            'short_name' => ['nullable', 'string', 'max:10'],
            'vice_captain_name' => ['nullable', 'string', 'max:100'],
            'manager_name' => ['nullable', 'string', 'max:100'],
            'wicketkeeper_name' => ['nullable', 'string', 'max:100'],
        ]);

        $team = Team::create([
            'tournament_id' => $tournament->id,
            'name' => $data['name'],
            'short_name' => $data['short_name'] ?? strtoupper(Str::limit($data['name'], 3, '')),
            'display_order' => $tournament->teams()->count() + 1,
            'is_active' => true,
            'status' => 'pending',
            'vice_captain_name' => $data['vice_captain_name'] ?? null,
            'manager_name' => $data['manager_name'] ?? null,
            'wicketkeeper_name' => $data['wicketkeeper_name'] ?? null,
            'creator_id' => $request->user()?->id,
        ]);

        return response()->json([
            'data' => $team,
            'message' => 'Team created successfully.',
        ], 201);
    }

    /**
     * Delete a team from a tournament.
     */
    public function destroy(Tournament $tournament, Team $team): JsonResponse
    {
        abort_unless($team->tournament_id === $tournament->id, 404);

        // Check if team has draft picks
        if ($team->draftPicks()->where('status', 'selected')->exists()) {
            throw ValidationException::withMessages([
                'team' => 'Cannot delete a team that has selected draft picks.',
            ]);
        }

        // Revoke active captain
        $team->captainAssignments()->whereNull('revoked_at')->update(['revoked_at' => now()]);

        $team->delete();

        return response()->json(['message' => 'Team deleted successfully.']);
    }

    /**
     * Assign a captain to a team.
     */
    public function assignCaptain(Request $request, Tournament $tournament, Team $team): JsonResponse
    {
        abort_unless($team->tournament_id === $tournament->id, 404);

        $data = $request->validate([
            'user_id' => ['required', 'integer', 'exists:users,id'],
        ]);

        $user = User::findOrFail($data['user_id']);

        // Check if user has a player profile registered in this tournament
        $playerProfile = $user->playerProfile;
        abort_unless($playerProfile, 422, 'This user does not have a player profile.');

        $isRegistered = $tournament->tournamentPlayers()
            ->where('player_profile_id', $playerProfile->id)
            ->where('status', 'approved')
            ->exists();

        abort_unless($isRegistered, 422, 'This player is not approved for this tournament.');

        // Check if user is already captain of another team in this tournament
        $existingCaptaincy = Team::where('tournament_id', $tournament->id)
            ->whereHas('captainAssignments', function ($query) use ($user) {
                $query->where('user_id', $user->id)->whereNull('revoked_at');
            })
            ->first();

        if ($existingCaptaincy && $existingCaptaincy->id !== $team->id) {
            throw ValidationException::withMessages([
                'user_id' => "This user is already captain of \"{$existingCaptaincy->name}\".",
            ]);
        }

        // Revoke any existing captain for this team
        $team->captainAssignments()->whereNull('revoked_at')->update(['revoked_at' => now()]);

        // Assign new captain
        TeamCaptain::create([
            'team_id' => $team->id,
            'user_id' => $user->id,
            'assigned_at' => now(),
        ]);

        return response()->json([
            'data' => $team->fresh('activeCaptain.user'),
            'message' => "Captain assigned to {$team->name}.",
        ]);
    }

    /**
     * Remove captain from a team.
     */
    public function removeCaptain(Tournament $tournament, Team $team): JsonResponse
    {
        abort_unless($team->tournament_id === $tournament->id, 404);

        $revoked = $team->captainAssignments()
            ->whereNull('revoked_at')
            ->update(['revoked_at' => now()]);

        if (!$revoked) {
            throw ValidationException::withMessages([
                'team' => 'This team does not have an active captain.',
            ]);
        }

        return response()->json([
            'data' => $team->fresh('activeCaptain'),
            'message' => "Captain removed from {$team->name}.",
        ]);
    }
}
