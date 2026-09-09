<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Team;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CustomTeamController extends Controller
{
    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:50'],
            'short_name' => ['nullable', 'string', 'max:10'],
        ]);

        $team = Team::firstOrCreate(
            ['name' => $data['name']],
            [
                'short_name' => $data['short_name'] ?? null,
                'creator_id' => $request->user()->id,
                'is_active' => true,
            ]
        );

        return response()->json(['data' => $team, 'message' => 'Custom team created successfully.'], 201);
    }
}
