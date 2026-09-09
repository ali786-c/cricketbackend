<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\PlayerProfile;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CustomPlayerController extends Controller
{
    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:255'],
            'role' => ['nullable', 'string', 'max:50'],
            'city' => ['nullable', 'string', 'max:100'],
        ]);

        $profile = PlayerProfile::create([
            'user_id' => null,
            'full_name' => $data['name'],
            'playing_role' => $data['role'] ?? null,
            'city' => $data['city'] ?? null,
            'is_guest' => true,
            'is_active' => true,
        ]);

        return response()->json([
            'data' => [
                'id' => $profile->id,
                'name' => $profile->full_name,
                'role' => $profile->playing_role,
            ]
        ], 201);
    }
}
