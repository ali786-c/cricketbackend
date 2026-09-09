<!DOCTYPE html>
<html lang="<?php echo e(str_replace('_', '-', app()->getLocale())); ?>">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="<?php echo e(csrf_token()); ?>">
    <title><?php echo e(config('app.name', 'Cricket Draft System')); ?></title>
    <?php echo app('Illuminate\Foundation\Vite')(['resources/css/app.css', 'resources/js/app.js']); ?>
</head>
<body>
    <?php
        $user = auth()->user();
    ?>
    <nav class="navbar navbar-expand-lg cricket-topbar sticky-top">
        <div class="container py-2 py-lg-3">
            <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="<?php echo e($user?->hasRole('super_admin') ? route('super-admin.dashboard') : ($user?->hasRole('admin') ? route('admin.dashboard') : ($user?->hasRole('captain') ? route('captain.dashboard') : ($user?->hasRole('player') ? route('player.tournaments.index') : route('dashboard'))))); ?>">
                <span class="cricket-brand-mark"><i class="fa-solid fa-baseball"></i></span>
                <span class="d-none d-sm-inline">Cricket Draft <span class="text-success">OS</span></span>
            </a>

            <div class="d-flex align-items-center gap-2 gap-lg-3 ms-auto">
                <?php if($user?->hasRole('super_admin')): ?>
                    <div class="dropdown d-lg-none">
                        <button class="btn btn-sm btn-light" data-bs-toggle="dropdown" aria-expanded="false" aria-label="Open navigation"><i class="fa-solid fa-bars"></i></button>
                        <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2">
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.dashboard')); ?>"><i class="fa-solid fa-shield-halved me-2 text-success"></i>Control plane</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.users.index')); ?>"><i class="fa-solid fa-users-gear me-2 text-success"></i>Users and roles</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.tournaments.index')); ?>"><i class="fa-solid fa-trophy me-2 text-success"></i>Tournament fleet</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.matches.index')); ?>"><i class="fa-solid fa-baseball-bat-ball me-2 text-success"></i>Matches</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.fixtures.index')); ?>"><i class="fa-regular fa-calendar-days me-2 text-success"></i>Fixtures</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.teams.index')); ?>"><i class="fa-solid fa-people-group me-2 text-success"></i>Teams</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.players.index')); ?>"><i class="fa-solid fa-person-running me-2 text-success"></i>Players</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.api-clients.index')); ?>"><i class="fa-solid fa-plug me-2 text-success"></i>API clients</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.api-sessions.index')); ?>"><i class="fa-solid fa-mobile-screen-button me-2 text-success"></i>API sessions</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.audit-logs.index')); ?>"><i class="fa-solid fa-clock-rotate-left me-2 text-success"></i>Audit logs</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('super-admin.health')); ?>"><i class="fa-solid fa-heart-pulse me-2 text-success"></i>System health</a></li>
                        </ul>
                    </div>
                    <div class="d-none d-lg-flex align-items-center gap-1">
                        <a href="<?php echo e(route('super-admin.dashboard')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.dashboard') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-shield-halved me-1"></i>Control plane</a>
                        <a href="<?php echo e(route('super-admin.users.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.users.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-users-gear me-1"></i>Users</a>
                        <a href="<?php echo e(route('super-admin.tournaments.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.tournaments.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-trophy me-1"></i>Fleet</a>
                        <a href="<?php echo e(route('super-admin.matches.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.matches.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-baseball-bat-ball me-1"></i>Matches</a>
                        <a href="<?php echo e(route('super-admin.fixtures.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.fixtures.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-regular fa-calendar-days me-1"></i>Fixtures</a>
                        <a href="<?php echo e(route('super-admin.teams.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.teams.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-people-group me-1"></i>Teams</a>
                        <a href="<?php echo e(route('super-admin.players.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.players.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-person-running me-1"></i>Players</a>
                        <a href="<?php echo e(route('super-admin.api-clients.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.api-clients.*') || request()->routeIs('super-admin.api-sessions.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-plug me-1"></i>API</a>
                        <a href="<?php echo e(route('super-admin.audit-logs.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('super-admin.audit-logs.*') || request()->routeIs('super-admin.health') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-shield-halved me-1"></i>Security</a>
                    </div>
                <?php elseif($user?->hasRole('admin')): ?>
                    <div class="dropdown d-lg-none">
                        <button class="btn btn-sm btn-light" data-bs-toggle="dropdown" aria-expanded="false" aria-label="Open navigation"><i class="fa-solid fa-bars"></i></button>
                        <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2">
                            <li><a class="dropdown-item" href="<?php echo e(route('admin.dashboard')); ?>"><i class="fa-solid fa-grid-2 me-2 text-success"></i>Overview</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('admin.tournaments.index')); ?>"><i class="fa-solid fa-trophy me-2 text-success"></i>Tournaments</a></li>
                            <li><a class="dropdown-item" href="<?php echo e(route('admin.users.index')); ?>"><i class="fa-solid fa-users-gear me-2 text-success"></i>Users</a></li>
                        </ul>
                    </div>
                    <div class="d-none d-lg-flex align-items-center gap-1">
                        <a href="<?php echo e(route('admin.dashboard')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('admin.dashboard') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-grid-2 me-1"></i>Overview</a>
                        <a href="<?php echo e(route('admin.tournaments.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('admin.tournaments.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-trophy me-1"></i>Tournaments</a>
                        <a href="<?php echo e(route('admin.users.index')); ?>" class="btn btn-sm <?php echo e(request()->routeIs('admin.users.*') ? 'btn-success' : 'btn-light'); ?>"><i class="fa-solid fa-users-gear me-1"></i>Users</a>
                    </div>
                <?php elseif($user?->hasRole('captain')): ?>
                    <a href="<?php echo e(route('captain.dashboard')); ?>" class="btn btn-sm btn-light"><i class="fa-solid fa-shield-halved me-1"></i><span class="d-none d-sm-inline">Captain workspace</span><span class="d-sm-none">Workspace</span></a>
                <?php elseif($user?->hasRole('player')): ?>
                    <a href="<?php echo e(route('player.tournaments.index')); ?>" class="btn btn-sm btn-light"><i class="fa-solid fa-trophy me-1"></i>Tournaments</a>
                <?php endif; ?>

                <div class="vr d-none d-sm-block"></div>
                <div class="d-none d-md-block text-end">
                    <div class="small fw-bold"><?php echo e($user?->name); ?></div>
                    <div class="text-secondary" style="font-size: .68rem; text-transform: uppercase; letter-spacing: .1em;"><?php echo e($user?->getRoleNames()->first() ?: 'Member'); ?></div>
                </div>
                <div class="dropdown">
                    <button class="btn btn-sm btn-light rounded-circle p-0" style="width: 2.35rem; height: 2.35rem;" data-bs-toggle="dropdown" aria-expanded="false" aria-label="Account menu">
                        <span class="fw-bold text-success"><?php echo e(strtoupper(substr($user?->name ?? 'U', 0, 1))); ?></span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2">
                        <li><span class="dropdown-item-text small text-secondary">Signed in as <?php echo e($user?->email); ?></span></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item" href="<?php echo e(route('profile.edit')); ?>"><i class="fa-solid fa-user-pen me-2 text-success"></i>Profile</a></li>
                        <li><form method="POST" action="<?php echo e(route('logout')); ?>"><?php echo csrf_field(); ?><button type="submit" class="dropdown-item text-danger"><i class="fa-solid fa-arrow-right-from-bracket me-2"></i>Log out</button></form></li>
                    </ul>
                </div>
            </div>
        </div>
    </nav>

    <?php if(isset($header)): ?>
        <header class="cricket-page-header py-4 py-lg-5">
            <div class="container"><?php echo e($header); ?></div>
        </header>
    <?php endif; ?>

    <main>
        <?php echo $__env->yieldContent('content'); ?>
        <?php if(isset($slot)): ?>
            <?php echo e($slot); ?>

        <?php endif; ?>
    </main>
</body>
</html>
<?php /**PATH C:\Users\Muhammad Aliyan\Downloads\cricket-draft-source\resources\views/layouts/app.blade.php ENDPATH**/ ?>