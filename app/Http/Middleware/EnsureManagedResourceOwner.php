<?php

namespace App\Http\Middleware;

use App\Models\AuditLog;
use App\Models\CricketMatch;
use App\Models\Fixture;
use App\Models\MatchDelivery;
use App\Models\Tournament;
use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;
use Symfony\Component\HttpFoundation\Response;

class EnsureManagedResourceOwner
{
    public function handle(Request $request, Closure $next): Response
    {
        $route = $request->route();
        $routeName = $route?->getName() ?? '';
        $isManagementRoute = str_starts_with($routeName, 'admin.')
            || str_starts_with($routeName, 'api.v1.admin.');

        if ($isManagementRoute) {
            $tournament = $request->route('tournament');

            if ($tournament instanceof Tournament) {
                Gate::authorize('manage', $tournament);
            }
        }

        $response = $next($request);

        $this->auditSuperAdminOverride($request);

        return $response;
    }

    private function auditSuperAdminOverride(Request $request): void
    {
        $user = $request->user();
        if (! $user?->hasRole('super_admin') || $request->isMethodSafe()) {
            return;
        }

        $resource = collect(['match', 'matchDelivery', 'fixture', 'tournament'])
            ->map(fn (string $parameter) => $request->route($parameter))
            ->first(fn ($value) => $value instanceof CricketMatch
                || $value instanceof MatchDelivery
                || $value instanceof Fixture
                || $value instanceof Tournament);

        if (! $resource) {
            return;
        }

        $match = $resource instanceof MatchDelivery ? $resource->match : ($resource instanceof CricketMatch ? $resource : null);
        $fixture = $resource instanceof Fixture ? $resource : null;
        $tournament = $resource instanceof Tournament
            ? $resource
            : ($match?->tournament ?? $fixture?->tournament);
        $ownerId = $tournament?->creator_id
            ?? $match?->created_by
            ?? $fixture?->created_by;

        if ((int) $ownerId === (int) $user->id) {
            return;
        }

        AuditLog::create([
            'user_id' => $user->id,
            'tournament_id' => $tournament?->id,
            'action' => 'super_admin.ownership_override',
            'auditable_type' => $resource::class,
            'auditable_id' => $resource->getKey(),
            'metadata' => [
                'route' => $request->route()?->getName(),
                'method' => $request->method(),
                'owner_user_id' => $ownerId,
            ],
            'ip_address' => $request->ip(),
            'user_agent' => substr((string) $request->userAgent(), 0, 1000),
        ]);
    }
}
