<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\ApiClient;
use App\Models\AuditLog;
use App\Models\CricketMatch;
use App\Models\Fixture;
use App\Models\PlayerProfile;
use App\Models\Team;
use App\Models\Tournament;
use App\Models\TournamentPlayer;
use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Route;
use Illuminate\Support\Facades\Schema;
use Laravel\Sanctum\PersonalAccessToken;

class SuperAdminController extends Controller
{
    public function dashboard(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $now = now();
        $activeTokens = PersonalAccessToken::query()->where(function ($query) use ($now) {
            $query->whereNull('expires_at')->orWhere('expires_at', '>', $now);
        })->count();

        return response()->json(['data' => [
            'users' => User::count(),
            'roles' => collect(['super_admin', 'admin', 'captain', 'player'])->mapWithKeys(fn (string $role) => [$role => User::role($role)->count()]),
            'tournaments' => Tournament::count(),
            'tournament_statuses' => Tournament::query()->select('status', DB::raw('COUNT(*) as total'))->groupBy('status')->pluck('total', 'status'),
            'live_matches' => CricketMatch::where('status', 'live')->count(),
            'match_statuses' => CricketMatch::query()->select('status', DB::raw('COUNT(*) as total'))->groupBy('status')->pluck('total', 'status'),
            'fixture_statuses' => Fixture::query()->select('status', DB::raw('COUNT(*) as total'))->groupBy('status')->pluck('total', 'status'),
            'pending_registrations' => TournamentPlayer::where('status', 'pending')->count(),
            'api_clients' => ApiClient::count(),
            'active_api_clients' => ApiClient::where('is_active', true)->count(),
            'active_sessions' => $activeTokens,
            'expired_sessions' => PersonalAccessToken::whereNotNull('expires_at')->where('expires_at', '<=', $now)->count(),
            'recent_audit_logs' => AuditLog::with(['user', 'tournament'])->latest()->limit(15)->get(),
        ]]);
    }

    public function clients(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $query = ApiClient::with('creator')->latest();
        if ($search = trim((string) $request->string('search'))) {
            $query->where(fn ($builder) => $builder->where('name', 'like', "%{$search}%")->orWhere('slug', 'like', "%{$search}%"));
        }
        if ($request->has('active')) {
            $query->where('is_active', $request->boolean('active'));
        }
        return response()->json(['data' => $query->paginate(30)->withQueryString()]);
    }

    public function createClient(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $data = $request->validate(['name' => ['required', 'string', 'max:120'], 'slug' => ['required', 'alpha_dash', 'max:80', 'unique:api_clients,slug'], 'platform' => ['required', 'in:android,ios,web,internal,other'], 'version' => ['nullable', 'string', 'max:40'], 'rate_limit_per_minute' => ['required', 'integer', 'min:10', 'max:10000'], 'notes' => ['nullable', 'string', 'max:2000']]);
        $client = ApiClient::create([...$data, 'created_by' => $request->user()->id]);
        $this->audit($request, 'api_client.created', $client->id, ['name' => $client->name, 'platform' => $client->platform]);
        return response()->json(['data' => $client, 'message' => 'API client created successfully.'], 201);
    }

    public function toggleClient(Request $request, ApiClient $apiClient): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $apiClient->update(['is_active' => ! $apiClient->is_active]);
        $this->audit($request, 'api_client.toggled', $apiClient->id, ['is_active' => $apiClient->is_active]);
        return response()->json(['data' => $apiClient->fresh(), 'message' => 'API client status updated.']);
    }

    public function sessions(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $query = PersonalAccessToken::with('tokenable')->latest();
        if ($search = trim((string) $request->string('search'))) {
            $query->where(function ($builder) use ($search) {
                $builder->where('name', 'like', "%{$search}%")
                    ->orWhereHasMorph('tokenable', [User::class], fn ($user) => $user->where('name', 'like', "%{$search}%")->orWhere('email', 'like', "%{$search}%"));
            });
        }
        if ($request->string('status')->toString() === 'expired') {
            $query->whereNotNull('expires_at')->where('expires_at', '<=', now());
        } elseif ($request->string('status')->toString() === 'active') {
            $query->where(function ($builder) { $builder->whereNull('expires_at')->orWhere('expires_at', '>', now()); });
        }
        return response()->json(['data' => $query->paginate(50)->withQueryString()]);
    }

    public function revokeSession(Request $request, PersonalAccessToken $token): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $id = $token->id;
        $token->delete();
        $this->audit($request, 'api_session.revoked', $id, ['reason' => 'super_admin_action'], PersonalAccessToken::class);
        return response()->json(['data' => ['id' => $id], 'message' => 'API session revoked.']);
    }

    public function auditLogs(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $query = $this->filteredAuditQuery($request);
        return response()->json(['data' => $query->paginate(50)->withQueryString()]);
    }

    public function health(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $database = 'healthy';
        $databaseMs = null;
        $started = microtime(true);
        try { DB::connection()->getPdo(); DB::select('SELECT 1'); $databaseMs = round((microtime(true) - $started) * 1000); } catch (\Throwable) { $database = 'unavailable'; }
        $failedJobs = Schema::hasTable('failed_jobs') ? DB::table('failed_jobs')->count() : null;
        $queuedJobs = Schema::hasTable('jobs') ? DB::table('jobs')->count() : null;
        return response()->json(['data' => [
            'checked_at' => now()->toIso8601String(),
            'application' => app()->isDownForMaintenance() ? 'maintenance' : 'healthy',
            'database' => $database,
            'database_response_ms' => $databaseMs,
            'scheduler' => 'manual_verification_required',
            'queue' => ['queued' => $queuedJobs, 'failed' => $failedJobs],
            'api' => Route::has('api.v1.auth.login') ? 'online' : 'misconfigured',
            'storage' => is_writable(storage_path()) ? 'writable' : 'check_permissions',
            'security' => ['debug' => (bool) config('app.debug'), 'https_url' => str_starts_with((string) config('app.url'), 'https://')],
            'environment' => ['app' => app()->environment(), 'php' => PHP_VERSION, 'laravel' => app()->version(), 'timezone' => config('app.timezone')],
        ]]);
    }

    public function users(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $query = User::with('roles')->withCount(['tokens', 'auditLogs'])->latest();
        if ($search = trim((string) $request->string('search'))) {
            $query->where(fn ($builder) => $builder->where('name', 'like', "%{$search}%")->orWhere('email', 'like', "%{$search}%"));
        }
        if ($role = $request->string('role')->toString()) {
            $query->role($role);
        }
        return response()->json(['data' => $query->paginate(50)->withQueryString()]);
    }

    public function user(Request $request, User $user): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $user->load('roles', 'playerProfile')->loadCount(['tokens', 'auditLogs', 'reviewedTournamentPlayers', 'selectedDraftPicks']);
        return response()->json(['data' => ['user' => $user, 'sessions' => $user->tokens()->latest()->take(20)->get(), 'audit_logs' => $user->auditLogs()->with('tournament')->latest()->take(20)->get()]]);
    }

    public function updateUserRole(Request $request, User $user): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $data = $request->validate(['role' => ['required', 'in:admin,captain,player,super_admin']]);
        $oldRole = $user->getRoleNames()->first();
        if ($user->is($request->user()) && $data['role'] !== 'super_admin') abort(422, 'You cannot remove your own Super Admin access.');
        if ($oldRole === 'super_admin' && $data['role'] !== 'super_admin' && User::role('super_admin')->count() <= 1) abort(422, 'The platform must retain at least one Super Admin.');
        $user->syncRoles([$data['role']]);
        $this->audit($request, 'super_admin.user_role_changed', $user->id, ['before' => ['role' => $oldRole], 'after' => ['role' => $data['role']]], User::class);
        return response()->json(['data' => $user->fresh()->load('roles'), 'message' => 'User role updated successfully.']);
    }

    public function revokeUserSessions(Request $request, User $user): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $count = $user->tokens()->count();
        $user->tokens()->delete();
        $this->audit($request, 'super_admin.user_sessions_revoked', $user->id, ['token_count' => $count], User::class);
        return response()->json(['data' => ['user_id' => $user->id, 'revoked' => $count], 'message' => 'User API sessions revoked.']);
    }

    public function tournaments(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $query = Tournament::query()->withCount(['teams', 'tournamentPlayers', 'matches', 'fixtures', 'auditLogs'])->latest();
        if ($search = trim((string) $request->string('search'))) $query->where(fn ($builder) => $builder->where('name', 'like', "%{$search}%")->orWhere('season_name', 'like', "%{$search}%")->orWhere('city', 'like', "%{$search}%"));
        if ($status = $request->string('status')->toString()) $query->where('status', $status);
        return response()->json(['data' => $query->paginate(50)->withQueryString()]);
    }

    public function tournament(Request $request, Tournament $tournament): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $tournament->loadCount(['teams', 'tournamentPlayers', 'matches', 'fixtures', 'auditLogs'])->load(['teams.captain.user', 'matches.fixture.homeTeam', 'matches.fixture.awayTeam', 'fixtures.homeTeam', 'fixtures.awayTeam']);
        return response()->json(['data' => $tournament]);
    }

    public function matches(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $query = CricketMatch::with(['tournament', 'fixture.homeTeam', 'fixture.awayTeam'])->latest();
        if ($status = $request->string('status')->toString()) {
            $query->where('status', $status);
        }
        return response()->json(['data' => $query->paginate(50)->withQueryString()]);
    }

    public function teams(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $query = Team::with('tournament')->latest();
        if ($search = trim((string) $request->string('search'))) {
            $query->where(fn ($builder) => $builder->where('name', 'like', "%{$search}%")->orWhere('short_name', 'like', "%{$search}%"));
        }
        return response()->json(['data' => $query->paginate(50)->withQueryString()]);
    }

    public function players(Request $request): JsonResponse
    {
        $this->ensureSuperAdmin($request);
        $query = PlayerProfile::with('user')->latest();
        if ($search = trim((string) $request->string('search'))) {
            $query->where(fn ($builder) => $builder->where('full_name', 'like', "%{$search}%")->orWhereHas('user', fn ($q) => $q->where('email', 'like', "%{$search}%")));
        }
        return response()->json(['data' => $query->paginate(50)->withQueryString()]);
    }

    private function filteredAuditQuery(Request $request)
    {
        $query = AuditLog::with(['user', 'tournament'])->latest();
        if ($search = trim((string) $request->string('search'))) $query->where(fn ($builder) => $builder->where('action', 'like', "%{$search}%")->orWhere('ip_address', 'like', "%{$search}%")->orWhereHas('user', fn ($user) => $user->where('name', 'like', "%{$search}%")->orWhere('email', 'like', "%{$search}%")));
        if ($action = $request->string('action')->toString()) $query->where('action', $action);
        if ($userId = $request->integer('user_id')) $query->where('user_id', $userId);
        if ($from = $request->string('from')->toString()) $query->whereDate('created_at', '>=', $from);
        if ($to = $request->string('to')->toString()) $query->whereDate('created_at', '<=', $to);
        return $query;
    }

    private function ensureSuperAdmin(Request $request): void
    {
        abort_unless($request->user()?->roles()->where('name', 'super_admin')->where('guard_name', 'web')->exists(), 403);
    }

    private function audit(Request $request, string $action, int $auditableId, array $metadata, string $auditableType = ApiClient::class): void
    {
        AuditLog::create(['user_id' => $request->user()->id, 'action' => $action, 'auditable_type' => $auditableType, 'auditable_id' => $auditableId, 'metadata' => $metadata, 'ip_address' => $request->ip(), 'user_agent' => substr((string) $request->userAgent(), 0, 1000)]);
    }
}
