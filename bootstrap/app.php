<?php

use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Console\Scheduling\Schedule;
use Spatie\Permission\Middleware\PermissionMiddleware;
use Spatie\Permission\Middleware\RoleMiddleware;
use Spatie\Permission\Middleware\RoleOrPermissionMiddleware;
use App\Http\Middleware\SecurityHeaders;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Symfony\Component\HttpFoundation\Response;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->alias([
            'role' => RoleMiddleware::class,
            'permission' => PermissionMiddleware::class,
            'role_or_permission' => RoleOrPermissionMiddleware::class,
            'draft.enabled' => \App\Http\Middleware\EnsureDraftIsEnabled::class,
            'managed.owner' => \App\Http\Middleware\EnsureManagedResourceOwner::class,
        ]);
        $middleware->append(SecurityHeaders::class);
    })
    ->withSchedule(function (Schedule $schedule): void {
        $schedule->command('drafts:expire-picks')->everyMinute();
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        // Laravel may convert AuthorizationException into an HTTP exception
        // before custom render callbacks run. Normalize the final API 403 so
        // every policy/middleware denial has a stable machine-readable code.
        $exceptions->respond(function (Response $response, \Throwable $exception, Request $request) {
            if ($response->getStatusCode() !== 403 || ! $request->is('api/*')) {
                return $response;
            }
            $isMatchMutation = $request->is('api/v1/matches/*') && ! $request->isMethod('GET');

            Log::warning('security_authorization_denied', [
                'user_id' => $request->user()?->id,
                'route' => $request->route()?->getName(),
                'method' => $request->method(),
                'status' => 403,
                'code' => $isMatchMutation ? 'match_edit_forbidden' : 'resource_edit_forbidden',
            ]);

            return response()->json([
                'message' => $isMatchMutation
                    ? 'Only the creator can edit this match.'
                    : 'You are not allowed to modify this resource.',
                'code' => $isMatchMutation ? 'match_edit_forbidden' : 'resource_edit_forbidden',
            ], 403);
        });
    })->create();
