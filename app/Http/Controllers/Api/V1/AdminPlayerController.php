<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Tournament;
use App\Models\TournamentPlayer;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminPlayerController extends Controller
{
    public function index(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        $query = $tournament->tournamentPlayers()->with('playerProfile.user')->latest();
        if ($request->filled('status')) {
            $query->where('status', $request->string('status')->toString());
        }
        return response()->json(['data' => $query->paginate(30)]);
    }

    public function approve(Request $request, Tournament $tournament, TournamentPlayer $registration): JsonResponse
    {
        $this->belongs($tournament, $registration, $request);
        $registration->update(['status' => 'approved', 'reviewed_by' => $request->user()->id, 'reviewed_at' => now(), 'review_notes' => null]);
        return response()->json(['data' => $registration->fresh()->load('playerProfile.user'), 'message' => 'Player approved for this tournament.']);
    }

    public function reject(Request $request, Tournament $tournament, TournamentPlayer $registration): JsonResponse
    {
        $this->belongs($tournament, $registration, $request);
        $data = $request->validate(['review_notes' => ['nullable', 'string', 'max:2000']]);
        $registration->update(['status' => 'rejected', 'reviewed_by' => $request->user()->id, 'reviewed_at' => now(), 'review_notes' => $data['review_notes'] ?? null]);
        return response()->json(['data' => $registration->fresh()->load('playerProfile.user'), 'message' => 'Player registration rejected.']);
    }

    public function storeManual(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        $data = $request->validate([
            'name' => ['required', 'string', 'max:255'],
            'role' => ['nullable', 'string', 'max:50'],
            'city' => ['nullable', 'string', 'max:100'],
        ]);

        $profile = \App\Models\PlayerProfile::create([
            'user_id' => null,
            'full_name' => $data['name'],
            'playing_role' => $data['role'] ?? null,
            'city' => $data['city'] ?? null,
            'is_guest' => true,
            'is_active' => true,
        ]);

        $registration = $tournament->tournamentPlayers()->create([
            'player_profile_id' => $profile->id,
            'status' => 'approved',
            'reviewed_by' => $request->user()->id,
            'reviewed_at' => now(),
        ]);

        return response()->json([
            'data' => [
                'id' => $registration->id,
                'player_profile_id' => $profile->id,
                'unique_code' => $profile->unique_code,
                'full_name' => $profile->full_name,
                'playing_role' => $profile->playing_role,
                'city' => $profile->city,
                'status' => 'approved',
            ],
            'message' => 'Guest player created and approved successfully.'
        ], 201);
    }

    private function belongs(Tournament $tournament, TournamentPlayer $registration, Request $request): void
    {
        $this->authorizeCreator($tournament, $request);
        abort_unless($registration->tournament_id === $tournament->id, 404);
    }

    private function authorizeCreator(Tournament $tournament, Request $request): void
    {
        abort_if($tournament->creator_id !== $request->user()->id, 403, 'You can only manage players for tournaments you created.');
    }
}
