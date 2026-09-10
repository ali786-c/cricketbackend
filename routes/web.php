<?php

use App\Http\Controllers\Admin\AuditLogController;
use App\Http\Controllers\Admin\DashboardController;
use App\Http\Controllers\Admin\MatchController;
use App\Http\Controllers\Admin\MatchScoringController;
use App\Http\Controllers\Admin\MatchResultController;
use App\Http\Controllers\Admin\DraftController;
use App\Http\Controllers\Admin\FixtureController;
use App\Http\Controllers\Admin\DraftSetupController;
use App\Http\Controllers\Admin\PlayerApprovalController;
use App\Http\Controllers\Admin\PlayerImportController;
use App\Http\Controllers\Admin\TeamController;
use App\Http\Controllers\Admin\TournamentController;
use App\Http\Controllers\Admin\UserController;
use App\Http\Controllers\Admin\ReportController as AdminReportController;
use App\Http\Controllers\SuperAdmin\ApiClientController as SuperApiClientController;
use App\Http\Controllers\SuperAdmin\ApiSessionController as SuperApiSessionController;
use App\Http\Controllers\SuperAdmin\DashboardController as SuperDashboardController;
use App\Http\Controllers\SuperAdmin\GovernanceController as SuperGovernanceController;
use App\Http\Controllers\SuperAdmin\TournamentController as SuperTournamentController;
use App\Http\Controllers\SuperAdmin\UserController as SuperUserController;
use App\Http\Controllers\SuperAdmin\MatchController as SuperMatchController;
use App\Http\Controllers\SuperAdmin\FixtureController as SuperFixtureController;
use App\Http\Controllers\SuperAdmin\TeamController as SuperTeamController;
use App\Http\Controllers\SuperAdmin\PlayerController as SuperPlayerController;
use App\Http\Controllers\Captain\DashboardController as CaptainDashboardController;
use App\Http\Controllers\Captain\DraftController as CaptainDraftController;
use App\Http\Controllers\Captain\ReportController as CaptainReportController;
use App\Http\Controllers\Player\ProfileController as PlayerProfileController;
use App\Http\Controllers\Player\TournamentRegistrationController;
use App\Http\Controllers\ProfileController;
use App\Http\Controllers\Public\DraftController as PublicDraftController;
use App\Http\Controllers\Public\HomeController as PublicHomeController;
use App\Http\Controllers\Public\MatchController as PublicMatchController;
use App\Http\Controllers\Public\ReportController as PublicReportController;
use App\Http\Controllers\Public\TournamentController as PublicTournamentController;
use Illuminate\Support\Facades\Route;

Route::get('/', PublicHomeController::class)->name('home');

Route::get('/system-check', function () {
    return view('system-check');
})->middleware(['auth', 'role:admin'])->name('system.check');

Route::view('/features', 'public.features')->name('public.features');
Route::view('/how-it-works', 'public.how-it-works')->name('public.how-it-works');
Route::get('/tournaments', [PublicTournamentController::class, 'index'])
    ->name('public.tournaments.index');

Route::get('/live-drafts', [PublicDraftController::class, 'index'])
    ->name('public.live.center');
Route::get('/tournaments/{tournament}/draft/live', [PublicDraftController::class, 'show'])
    ->middleware('draft.enabled')
    ->name('public.draft.show');
Route::get('/tournaments/{tournament}/draft/live/state', [PublicDraftController::class, 'state'])
    ->middleware('draft.enabled')
    ->name('public.draft.state');
Route::get('/tournaments/{tournament}/matches', [PublicMatchController::class, 'index'])
    ->name('public.matches.index');
Route::get('/matches/{match}', [PublicMatchController::class, 'show'])
    ->name('public.matches.show');
Route::get('/matches/{match}/state', [PublicMatchController::class, 'state'])
    ->name('public.matches.state');
Route::get('/tournaments/{tournament}/standings', [PublicMatchController::class, 'standings'])
    ->name('public.standings');
Route::get('/tournaments/{tournament}/reports', [PublicReportController::class, 'show'])
    ->name('public.reports.show');
Route::get('/tournaments/{tournament}/reports/{type}.pdf', [PublicReportController::class, 'pdf'])
    ->name('public.reports.pdf');

Route::get('/dashboard', function () {
    return view('dashboard');
})->middleware(['auth', 'verified'])->name('dashboard');

Route::middleware(['auth', 'verified', 'role:admin'])
    ->prefix('admin')
    ->name('admin.')
    ->group(function () {
        Route::get('/', DashboardController::class)->name('dashboard');

        Route::get('users', [UserController::class, 'index'])
            ->middleware('permission:manage users')
            ->name('users.index');
        Route::post('users', [UserController::class, 'store'])
            ->middleware('permission:manage users')
            ->name('users.store');
        Route::post('users/{user}/promote-captain', [UserController::class, 'promoteCaptain'])
            ->middleware('permission:manage users')
            ->name('users.promote-captain');
        Route::delete('users/{user}/captain', [UserController::class, 'revokeCaptain'])
            ->middleware('permission:manage users')
            ->name('users.revoke-captain');
        Route::post('users/export-captains', [UserController::class, 'exportCaptains'])
            ->middleware('permission:manage users')
            ->name('users.export-captains');
        Route::post('users/{user}/reset-password', [UserController::class, 'resetPassword'])
            ->middleware('permission:manage users')
            ->name('users.reset-password');

        Route::resource('tournaments', TournamentController::class)
            ->only(['index', 'create', 'store', 'show', 'edit', 'update', 'destroy'])
            ->middleware('permission:manage tournaments');
        Route::post('tournaments/{tournament}/status', [TournamentController::class, 'transition'])
            ->middleware('permission:manage tournaments')
            ->name('tournaments.status.transition');

        Route::get('tournaments/{tournament}/fixtures', [FixtureController::class, 'index'])
            ->middleware('permission:manage tournaments')
            ->name('tournaments.fixtures.index');
        Route::get('tournaments/{tournament}/fixtures/pdf', [FixtureController::class, 'pdf'])
            ->middleware('permission:manage tournaments')
            ->name('tournaments.fixtures.pdf');
        Route::get('tournaments/{tournament}/fixtures/create', [FixtureController::class, 'create'])
            ->middleware('permission:manage tournaments')
            ->name('tournaments.fixtures.create');
        Route::post('tournaments/{tournament}/fixtures', [FixtureController::class, 'store'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.fixtures.store');
        Route::get('tournaments/{tournament}/fixtures/{fixture}/edit', [FixtureController::class, 'edit'])
            ->middleware('permission:manage tournaments')
            ->name('tournaments.fixtures.edit');
        Route::put('tournaments/{tournament}/fixtures/{fixture}', [FixtureController::class, 'update'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.fixtures.update');
        Route::post('tournaments/{tournament}/fixtures/{fixture}/status', [FixtureController::class, 'transition'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.fixtures.status');
        Route::post('tournaments/{tournament}/fixtures/{fixture}/create-match', [FixtureController::class, 'createMatch'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.fixtures.create-match');

        Route::get('tournaments/{tournament}/matches', [MatchController::class, 'index'])
            ->middleware('permission:manage tournaments')
            ->name('tournaments.matches.index');
        Route::get('tournaments/{tournament}/matches/create', [MatchController::class, 'create'])
            ->middleware('permission:manage tournaments')
            ->name('tournaments.matches.create');
        Route::post('tournaments/{tournament}/matches', [MatchController::class, 'store'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.matches.store');
        Route::get('tournaments/{tournament}/matches/{match}', [MatchController::class, 'show'])
            ->middleware('permission:manage tournaments')
            ->name('tournaments.matches.show');
        Route::post('tournaments/{tournament}/matches/{match}/overs', [MatchController::class, 'updateOvers'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.matches.overs');
        Route::post('tournaments/{tournament}/matches/{match}/teams/{team}/playing-xi', [MatchController::class, 'submitXi'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.matches.playing-xi');
        Route::post('tournaments/{tournament}/matches/{match}/approve-lineup', [MatchController::class, 'approveLineup'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.matches.approve-lineup');
        Route::post('tournaments/{tournament}/matches/{match}/toss', [MatchController::class, 'recordToss'])
            ->middleware(['permission:manage tournaments', 'throttle:30,1'])
            ->name('tournaments.matches.toss');
        Route::get('matches/{match}/scorer', [MatchScoringController::class, 'show'])
            ->middleware('permission:control draft')
            ->name('matches.scorer');
        Route::post('matches/{match}/scorer/deliveries', [MatchScoringController::class, 'store'])
            ->middleware(['permission:control draft', 'throttle:120,1'])
            ->name('matches.scorer.deliveries.store');
        Route::post('matches/{match}/result/submit', [MatchResultController::class, 'submit'])
            ->middleware(['permission:control draft', 'throttle:30,1'])
            ->name('matches.result.submit');
        Route::post('matches/{match}/result/approve', [MatchResultController::class, 'approve'])
            ->middleware(['permission:control draft', 'throttle:30,1'])
            ->name('matches.result.approve');
        Route::post('matches/{match}/scorer/next-innings', [MatchScoringController::class, 'startNextInnings'])
            ->middleware(['permission:control draft', 'throttle:30,1'])
            ->name('matches.scorer.next-innings');
        Route::post('matches/{match}/scorer/undo', [MatchScoringController::class, 'undo'])
            ->middleware(['permission:control draft', 'throttle:30,1'])
            ->name('matches.scorer.undo');

        Route::get('tournaments/{tournament}/teams', [TeamController::class, 'index'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.index');
        Route::post('tournaments/{tournament}/teams', [TeamController::class, 'store'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.store');
        Route::post('tournaments/{tournament}/teams/{team}/captain', [TeamController::class, 'assignCaptain'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.captain.assign');
        Route::delete('tournaments/{tournament}/teams/{team}/captain', [TeamController::class, 'revokeCaptain'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.captain.revoke');
        Route::delete('tournaments/{tournament}/teams/{team}', [TeamController::class, 'destroy'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.destroy');
        Route::get('tournaments/{tournament}/teams/{team}/players', [TeamController::class, 'managePlayers'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.players.index');
        Route::post('tournaments/{tournament}/teams/{team}/players', [TeamController::class, 'addPlayer'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.players.store');
        Route::delete('tournaments/{tournament}/teams/{team}/players/{draftPick}', [TeamController::class, 'removePlayer'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.players.destroy');
        Route::post('tournaments/{tournament}/teams/export-captains', [TeamController::class, 'exportTournamentCaptains'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.export-captains');
        Route::get('tournaments/{tournament}/teams/pdf', [TeamController::class, 'pdf'])
            ->middleware('permission:manage teams')
            ->name('tournaments.teams.pdf');

        Route::get('player-import-template.csv', [PlayerImportController::class, 'template'])
            ->middleware('permission:manage players')
            ->name('players.import.template');
        Route::get('tournaments/{tournament}/players', [PlayerApprovalController::class, 'index'])
            ->middleware('permission:manage players')
            ->name('tournaments.players.index');
        Route::get('tournaments/{tournament}/players/pdf', [PlayerApprovalController::class, 'pdf'])
            ->middleware('permission:manage players')
            ->name('tournaments.players.pdf');
        Route::post('tournaments/{tournament}/players/import', [PlayerImportController::class, 'store'])
            ->middleware(['permission:manage players', 'throttle:10,1'])
            ->name('tournaments.players.import');
        Route::post('tournaments/{tournament}/players', [PlayerApprovalController::class, 'store'])
            ->middleware('permission:manage players')
            ->name('tournaments.players.store');
        Route::put('tournaments/{tournament}/players/{tournamentPlayer}', [PlayerApprovalController::class, 'update'])
            ->middleware('permission:manage players')
            ->name('tournaments.players.update');
        Route::delete('tournaments/{tournament}/players/{tournamentPlayer}', [PlayerApprovalController::class, 'destroy'])
            ->middleware('permission:manage players')
            ->name('tournaments.players.destroy');
        Route::delete('tournaments/{tournament}/players-delete-all', [PlayerApprovalController::class, 'destroyAll'])
            ->middleware('permission:manage players')
            ->name('tournaments.players.destroy-all');
        Route::get('tournaments/{tournament}/audit-logs', [AuditLogController::class, 'index'])
            ->middleware('permission:control draft')
            ->name('tournaments.audit-logs.index');
        Route::get('tournaments/{tournament}/reports', [AdminReportController::class, 'index'])
            ->middleware('permission:control draft')
            ->name('tournaments.reports.index');
        Route::get('tournaments/{tournament}/reports/{type}.pdf', [AdminReportController::class, 'pdf'])
            ->middleware('permission:control draft')
            ->name('tournaments.reports.pdf');
        Route::post('tournaments/{tournament}/players/{tournamentPlayer}/approve', [PlayerApprovalController::class, 'approve'])
            ->middleware('permission:approve players')
            ->name('tournaments.players.approve');
        Route::post('tournaments/{tournament}/players/{tournamentPlayer}/reject', [PlayerApprovalController::class, 'reject'])
            ->middleware('permission:approve players')
            ->name('tournaments.players.reject');

        Route::middleware('draft.enabled')->group(function () {
            Route::get('tournaments/{tournament}/draft/setup', [DraftSetupController::class, 'edit'])
                ->middleware('permission:configure draft')
                ->name('tournaments.draft.setup');
            Route::put('tournaments/{tournament}/draft/setup', [DraftSetupController::class, 'update'])
                ->middleware('permission:configure draft')
                ->name('tournaments.draft.setup.update');
            Route::get('tournaments/{tournament}/draft/setup/pdf', [DraftSetupController::class, 'pdf'])
                ->middleware('permission:configure draft')
                ->name('tournaments.draft.setup.pdf');
            Route::get('tournaments/{tournament}/draft/setup/sample-csv', [DraftSetupController::class, 'downloadSample'])
                ->middleware('permission:configure draft')
                ->name('tournaments.draft.setup.sample-csv');
            Route::post('tournaments/{tournament}/draft/setup/import-csv', [DraftSetupController::class, 'importCsv'])
                ->middleware('permission:configure draft')
                ->name('tournaments.draft.setup.import-csv');

            Route::get('tournaments/{tournament}/draft', [DraftController::class, 'show'])
                ->middleware('permission:control draft')
                ->name('tournaments.draft.control');
            Route::get('tournaments/{tournament}/draft/state', [DraftController::class, 'state'])
                ->middleware('permission:control draft')
                ->name('tournaments.draft.state');
            Route::get('tournaments/{tournament}/draft/history.csv', [DraftController::class, 'exportHistory'])
                ->middleware('permission:control draft')
                ->name('tournaments.draft.history.export');
            Route::post('tournaments/{tournament}/draft/start', [DraftController::class, 'start'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.start');
            Route::post('tournaments/{tournament}/draft/select-player', [DraftController::class, 'selectPlayer'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.select-player');
            Route::post('tournaments/{tournament}/draft/remove-player', [DraftController::class, 'removePlayer'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.remove-player');
            Route::post('tournaments/{tournament}/draft/reassign-player', [DraftController::class, 'reassignPlayer'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.reassign-player');
            Route::post('tournaments/{tournament}/draft/extend', [DraftController::class, 'extend'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.extend');
            Route::post('tournaments/{tournament}/draft/skip', [DraftController::class, 'skip'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.skip');
            Route::post('tournaments/{tournament}/draft/pause', [DraftController::class, 'pause'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.pause');
            Route::post('tournaments/{tournament}/draft/resume', [DraftController::class, 'resume'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.resume');
            Route::post('tournaments/{tournament}/draft/undo', [DraftController::class, 'undo'])
                ->middleware(['permission:undo latest pick', 'throttle:30,1'])
                ->name('tournaments.draft.undo');
            Route::post('tournaments/{tournament}/draft/reset', [DraftController::class, 'reset'])
                ->middleware(['permission:control draft', 'throttle:30,1'])
                ->name('tournaments.draft.reset');
        });
    });

Route::middleware(['auth', 'verified', 'role:super_admin'])->prefix('super-admin')->name('super-admin.')->group(function () {
    Route::get('/', SuperDashboardController::class)->middleware('permission:manage system')->name('dashboard');
    Route::get('api-clients', [SuperApiClientController::class, 'index'])->middleware('permission:manage api clients')->name('api-clients.index');
    Route::get('api-clients/create', [SuperApiClientController::class, 'create'])->middleware('permission:manage api clients')->name('api-clients.create');
    Route::post('api-clients', [SuperApiClientController::class, 'store'])->middleware('permission:manage api clients')->name('api-clients.store');
    Route::post('api-clients/{apiClient}/toggle', [SuperApiClientController::class, 'toggle'])->middleware('permission:manage api clients')->name('api-clients.toggle');
    Route::get('api-sessions', [SuperApiSessionController::class, 'index'])->middleware('permission:manage api clients')->name('api-sessions.index');
    Route::delete('api-sessions/{token}', [SuperApiSessionController::class, 'revoke'])->middleware('permission:revoke api tokens')->name('api-sessions.revoke');
    Route::post('api-sessions/revoke-expired', [SuperApiSessionController::class, 'revokeExpired'])->middleware(['permission:revoke api tokens', 'throttle:10,1'])->name('api-sessions.revoke-expired');
    Route::get('audit-logs', [SuperGovernanceController::class, 'auditLogs'])->middleware('permission:view all audit logs')->name('audit-logs.index');
    Route::get('audit-logs/export.csv', [SuperGovernanceController::class, 'exportAuditLogs'])->middleware('permission:view all audit logs')->name('audit-logs.export');
    Route::get('health', [SuperGovernanceController::class, 'health'])->middleware('permission:view system health')->name('health');
    Route::get('users', [SuperUserController::class, 'index'])->middleware('permission:manage system')->name('users.index');
    Route::get('users/{user}', [SuperUserController::class, 'show'])->middleware('permission:manage system')->name('users.show');
    Route::post('users/{user}/role', [SuperUserController::class, 'updateRole'])->middleware(['permission:manage system', 'throttle:30,1'])->name('users.role.update');
    Route::post('users/{user}/revoke-sessions', [SuperUserController::class, 'revokeSessions'])->middleware(['permission:manage system', 'throttle:30,1'])->name('users.sessions.revoke');
    Route::get('tournaments', [SuperTournamentController::class, 'index'])->middleware('permission:manage system')->name('tournaments.index');
    Route::get('tournaments/{tournament}', [SuperTournamentController::class, 'show'])->middleware('permission:manage system')->name('tournaments.show');
    Route::get('matches', [SuperMatchController::class, 'index'])->middleware('permission:manage system')->name('matches.index');
    Route::get('matches/{match}/state', [SuperMatchController::class, 'state'])->middleware('permission:manage system')->name('matches.state');
    Route::get('matches/{match}', [SuperMatchController::class, 'show'])->middleware('permission:manage system')->name('matches.show');
    Route::get('fixtures', [SuperFixtureController::class, 'index'])->middleware('permission:manage system')->name('fixtures.index');
    Route::get('teams', [SuperTeamController::class, 'index'])->middleware('permission:manage system')->name('teams.index');
    Route::get('players', [SuperPlayerController::class, 'index'])->middleware('permission:manage system')->name('players.index');
});

Route::middleware(['auth', 'verified', 'role:player'])->prefix('player')->name('player.')->group(function () {
    Route::get('/', fn () => redirect()->route('player.tournaments.index'))->name('dashboard');
    Route::get('profile', [PlayerProfileController::class, 'edit'])->name('profile.edit');
    Route::put('profile', [PlayerProfileController::class, 'update'])->name('profile.update');
    Route::get('tournaments', [TournamentRegistrationController::class, 'index'])->name('tournaments.index');
    Route::post('tournaments/{tournament}/register', [TournamentRegistrationController::class, 'store'])->name('tournaments.register');
});

Route::middleware(['auth', 'verified', 'role:captain'])->prefix('captain')->name('captain.')->group(function () {
    Route::get('/', CaptainDashboardController::class)->name('dashboard');
    Route::get('tournaments/{tournament}/draft', [CaptainDraftController::class, 'show'])->name('draft.show');
    Route::get('tournaments/{tournament}/draft/state', [CaptainDraftController::class, 'state'])->name('draft.state');
    Route::get('tournaments/{tournament}/reports', [CaptainReportController::class, 'index'])->name('reports.index');
    Route::get('tournaments/{tournament}/reports/{type}.pdf', [CaptainReportController::class, 'pdf'])->name('reports.pdf');
    Route::post('tournaments/{tournament}/draft/pick', [CaptainDraftController::class, 'pick'])
        ->middleware('throttle:10,1')
        ->name('draft.pick');
});

Route::middleware('auth')->group(function () {
    Route::get('/profile', [ProfileController::class, 'edit'])->name('profile.edit');
    Route::patch('/profile', [ProfileController::class, 'update'])->name('profile.update');
    Route::delete('/profile', [ProfileController::class, 'destroy'])->name('profile.destroy');
});

require __DIR__.'/auth.php';
