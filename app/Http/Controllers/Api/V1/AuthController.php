<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\ApiClient;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\ValidationException;
use Laravel\Sanctum\PersonalAccessToken;

class AuthController extends Controller
{
    public function register(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'name' => ['required', 'string', 'max:255'],
            'email' => ['required', 'string', 'email', 'max:255', 'unique:users'],
            'password' => ['required', 'string', 'min:8'],
            'device_name' => ['required', 'string', 'max:100'],
            'client_slug' => ['nullable', 'string', 'exists:api_clients,slug'],
        ]);

        $user = \App\Models\User::create([
            'name' => $validated['name'],
            'email' => $validated['email'],
            'password' => Hash::make($validated['password']),
        ]);

        $user->assignRole(['player', 'admin']);

        $client = ! empty($validated['client_slug']) ? ApiClient::query()->where('slug', $validated['client_slug'])->first() : null;
        if ($validated['client_slug'] ?? false) {
            if (! $client || ! $client->is_active) throw ValidationException::withMessages(['client_slug' => 'This API client is disabled.']);
            $client->update(['last_seen_at' => now()]);
        }
        $abilities = $user->getRoleNames()->map(fn ($role) => 'role:'.$role)->push('profile:read');
        if ($client) $abilities->push('client:'.$client->slug);
        $token = $user->createToken($validated['device_name'], $abilities->values()->all());

        return response()->json([
            'data' => $this->userPayload($user->load('playerProfile')),
            'token' => $token->plainTextToken,
            'token_type' => 'Bearer',
        ], 201);
    }
    public function login(Request $request): JsonResponse
    {
        $validated = $request->validate([
            'email' => ['required', 'email'],
            'password' => ['required', 'string'],
            'device_name' => ['required', 'string', 'max:100'],
            'client_slug' => ['nullable', 'string', 'exists:api_clients,slug'],
        ]);

        $user = \App\Models\User::query()->where('email', $validated['email'])->first();
        if (! $user || ! Hash::check($validated['password'], $user->password)) {
            throw ValidationException::withMessages(['email' => 'The provided credentials are incorrect.']);
        }

        $client = ! empty($validated['client_slug']) ? ApiClient::query()->where('slug', $validated['client_slug'])->first() : null;
        if ($validated['client_slug'] ?? false) {
            if (! $client || ! $client->is_active) throw ValidationException::withMessages(['client_slug' => 'This API client is disabled.']);
            $client->update(['last_seen_at' => now()]);
        }
        $abilities = $user->getRoleNames()->map(fn ($role) => 'role:'.$role)->push('profile:read');
        if ($client) $abilities->push('client:'.$client->slug);
        $token = $user->createToken($validated['device_name'], $abilities->values()->all());

        return response()->json([
            'data' => $this->userPayload($user),
            'token' => $token->plainTextToken,
            'token_type' => 'Bearer',
        ]);
    }

    public function me(Request $request): JsonResponse
    {
        return response()->json(['data' => $this->userPayload($request->user())]);
    }

    public function logout(Request $request): JsonResponse
    {
        $currentToken = $request->user()->currentAccessToken();
        if ($currentToken instanceof PersonalAccessToken) {
            $currentToken->delete();
        } elseif ($request->bearerToken()) {
            PersonalAccessToken::findToken($request->bearerToken())?->delete();
        }
        return response()->json(['message' => 'The current API session has been revoked.']);
    }

    public function logoutAll(Request $request): JsonResponse
    {
        $request->user()->tokens()->delete();
        return response()->json(['message' => 'All API sessions have been revoked.']);
    }

    private function userPayload($user): array
    {
        return [
            'id' => $user->id,
            'name' => $user->name,
            'email' => $user->email,
            'roles' => $user->getRoleNames()->values(),
            'permissions' => $user->getAllPermissions()->pluck('name')->values(),
            'player_profile' => $user->playerProfile ? [
                'id' => $user->playerProfile->id,
                'full_name' => $user->playerProfile->full_name,
                'playing_role' => $user->playerProfile->playing_role,
            ] : null,
        ];
    }
}
