<?php if (isset($component)) { $__componentOriginal9ac128a9029c0e4701924bd2d73d7f54 = $component; } ?>
<?php if (isset($attributes)) { $__attributesOriginal9ac128a9029c0e4701924bd2d73d7f54 = $attributes; } ?>
<?php $component = App\View\Components\AppLayout::resolve([] + (isset($attributes) && $attributes instanceof Illuminate\View\ComponentAttributeBag ? $attributes->all() : [])); ?>
<?php $component->withName('app-layout'); ?>
<?php if ($component->shouldRender()): ?>
<?php $__env->startComponent($component->resolveView(), $component->data()); ?>
<?php if (isset($attributes) && $attributes instanceof Illuminate\View\ComponentAttributeBag): ?>
<?php $attributes = $attributes->except(\App\View\Components\AppLayout::ignoredParameterNames()); ?>
<?php endif; ?>
<?php $component->withAttributes([]); ?>
     <?php $__env->slot('header', null, []); ?> 
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-end gap-3">
            <div>
                <p class="cricket-kicker mb-2">Platform governance</p>
                <h1 class="display-6 fw-bold mb-2">Super Admin Control Plane</h1>
                <p class="text-secondary mb-0">A single operational view of identities, tournaments, live matches, APIs, sessions, security, and system health.</p>
            </div>
            <div class="d-flex flex-wrap gap-2">
                <a href="<?php echo e(route('super-admin.users.index')); ?>" class="btn btn-success"><i class="fa-solid fa-users-gear me-2"></i>Govern users</a>
                <a href="<?php echo e(route('super-admin.tournaments.index')); ?>" class="btn btn-light"><i class="fa-solid fa-trophy me-2"></i>Monitor tournaments</a>
                <a href="<?php echo e(route('super-admin.health')); ?>" class="btn btn-light"><i class="fa-solid fa-heart-pulse me-2"></i>System health</a>
            </div>
        </div>
     <?php $__env->endSlot(); ?>

    <div class="container pb-5">
        <div class="row g-3 mb-4">
            <div class="col-6 col-xl-3"><div class="cricket-surface p-4 h-100"><div class="small text-secondary mb-1">Total users</div><div class="display-6 fw-bold"><?php echo e(number_format($userCount)); ?></div><div class="small text-secondary mt-2"><?php echo e($roleCounts['player'] ?? 0); ?> players · <?php echo e($roleCounts['captain'] ?? 0); ?> captains</div><a href="<?php echo e(route('super-admin.users.index')); ?>" class="small text-success fw-semibold d-inline-block mt-3">Open identity governance <i class="fa-solid fa-arrow-right ms-1"></i></a></div></div>
            <div class="col-6 col-xl-3"><div class="cricket-surface p-4 h-100"><div class="small text-secondary mb-1">Tournament fleet</div><div class="display-6 fw-bold"><?php echo e(number_format($tournamentCount)); ?></div><div class="small text-secondary mt-2"><?php echo e($tournamentStatuses['live'] ?? 0); ?> live · <?php echo e($tournamentStatuses['completed'] ?? 0); ?> completed</div><a href="<?php echo e(route('super-admin.tournaments.index')); ?>" class="small text-success fw-semibold d-inline-block mt-3">Open fleet oversight <i class="fa-solid fa-arrow-right ms-1"></i></a></div></div>
            <div class="col-6 col-xl-3"><div class="cricket-surface p-4 h-100"><div class="small text-secondary mb-1">Live operations</div><div class="display-6 fw-bold text-success"><?php echo e(number_format($liveMatchCount)); ?></div><div class="small text-secondary mt-2"><?php echo e($tournamentStatuses['live'] ?? 0); ?> live tournaments · <?php echo e($fixtureStatuses['scheduled'] ?? 0); ?> scheduled fixtures</div><a href="<?php echo e(route('super-admin.tournaments.index', ['status' => 'live'])); ?>" class="small text-success fw-semibold d-inline-block mt-3">Inspect active rooms <i class="fa-solid fa-arrow-right ms-1"></i></a></div></div>
            <div class="col-6 col-xl-3"><div class="cricket-surface p-4 h-100"><div class="small text-secondary mb-1">API posture</div><div class="display-6 fw-bold"><?php echo e(number_format($activeApiClientCount)); ?><span class="fs-5 text-secondary">/<?php echo e($apiClientCount); ?></span></div><div class="small text-secondary mt-2"><?php echo e($activeApiTokenCount); ?> active sessions · <?php echo e($expiredTokenCount); ?> expired</div><a href="<?php echo e(route('super-admin.api-clients.index')); ?>" class="small text-success fw-semibold d-inline-block mt-3">Govern API clients <i class="fa-solid fa-arrow-right ms-1"></i></a></div></div>
        </div>

        <?php if($alerts->isNotEmpty()): ?>
            <div class="cricket-surface p-4 mb-4 border-start border-4 border-warning">
                <div class="d-flex justify-content-between align-items-center gap-3 mb-3"><div><p class="cricket-kicker mb-1">Attention required</p><h2 class="h4 fw-bold mb-0">Operational alerts</h2></div><span class="badge text-bg-warning"><?php echo e($alerts->count()); ?> open</span></div>
                <div class="row g-2">
                    <?php $__currentLoopData = $alerts; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $alert): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                        <div class="col-12 col-lg-6"><a href="<?php echo e($alert['href']); ?>" class="d-flex align-items-center justify-content-between gap-3 p-3 rounded-3 text-decoration-none bg-light border"><div><div class="fw-semibold text-dark"><?php echo e($alert['label']); ?></div><div class="small text-secondary">Review this item from the linked governance module.</div></div><span class="badge <?php echo e($alert['level'] === 'danger' ? 'text-bg-danger' : ($alert['level'] === 'warning' ? 'text-bg-warning' : 'text-bg-info')); ?>"><?php echo e($alert['value']); ?></span></a></div>
                    <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                </div>
            </div>
        <?php endif; ?>

        <div class="row g-4 mb-4">
            <div class="col-xl-8">
                <div class="cricket-surface p-4 p-lg-5 h-100">
                    <div class="d-flex justify-content-between align-items-start gap-3 mb-4"><div><p class="cricket-kicker mb-1">Platform fleet</p><h2 class="h3 fw-bold mb-1">Tournament status map</h2><p class="text-secondary mb-0">See where every competition is in its lifecycle without opening separate admin workspaces.</p></div><a href="<?php echo e(route('super-admin.tournaments.index')); ?>" class="btn btn-sm btn-light">View all tournaments</a></div>
                    <div class="row g-2 mb-4">
                        <?php $__currentLoopData = ['draft' => 'Draft', 'registration' => 'Registration', 'ready' => 'Ready', 'live' => 'Live', 'completed' => 'Completed', 'cancelled' => 'Cancelled']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $status => $label): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                            <div class="col-6 col-md-4"><a class="text-decoration-none" href="<?php echo e(route('super-admin.tournaments.index', ['status' => $status])); ?>"><div class="p-3 rounded-3 bg-light border h-100"><div class="small text-secondary"><?php echo e($label); ?></div><div class="h3 fw-bold mb-0 text-dark"><?php echo e($tournamentStatuses[$status] ?? 0); ?></div></div></a></div>
                        <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-3"><div><p class="cricket-kicker mb-1">Live operations</p><h3 class="h5 fw-bold mb-0">Active tournaments</h3></div><span class="small text-secondary"><?php echo e($liveTournaments->count()); ?> shown</span></div>
                    <?php $__empty_1 = true; $__currentLoopData = $liveTournaments; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $tournament): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?>
                        <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 border-bottom py-3"><div><div class="d-flex align-items-center gap-2"><span class="status-dot bg-success"></span><div class="fw-bold"><?php echo e($tournament->name); ?></div><span class="badge text-bg-success">Live</span></div><div class="small text-secondary mt-1"><?php echo e($tournament->teams_count); ?> teams · <?php echo e($tournament->matches_count); ?> matches · <?php echo e($tournament->tournament_players_count ?? 0); ?> registrations</div></div><a href="<?php echo e(route('super-admin.tournaments.show', $tournament)); ?>" class="btn btn-sm btn-outline-success">Inspect fleet view</a></div>
                    <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?>
                        <div class="cricket-surface-soft p-4 text-center text-secondary">No live tournaments at the moment.</div>
                    <?php endif; ?>
                    <div class="d-flex justify-content-between align-items-center mb-3 mt-5"><div><p class="cricket-kicker mb-1">Match rooms</p><h3 class="h5 fw-bold mb-0">Live matches</h3></div><span class="small text-secondary"><?php echo e($liveMatches->count()); ?> active</span></div>
                    <?php $__empty_1 = true; $__currentLoopData = $liveMatches; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $match): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?>
                        <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 border-bottom py-3"><div><div class="fw-semibold"><?php echo e($match->fixture?->homeTeam?->short_name ?: $match->fixture?->homeTeam?->name ?: 'TBD'); ?> <span class="text-secondary">vs</span> <?php echo e($match->fixture?->awayTeam?->short_name ?: $match->fixture?->awayTeam?->name ?: 'TBD'); ?></div><div class="small text-secondary"><?php echo e($match->tournament?->name ?: 'Tournament'); ?> · revision <?php echo e($match->revision); ?></div></div><a href="<?php echo e(route('admin.matches.scorer', $match)); ?>" class="btn btn-sm btn-outline-success">Open scorer</a></div>
                    <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?>
                        <div class="cricket-surface-soft p-4 text-center text-secondary">No live match rooms at the moment.</div>
                    <?php endif; ?>
                </div>
            </div>
            <div class="col-xl-4">
                <div class="cricket-surface p-4 p-lg-5 mb-4">
                    <p class="cricket-kicker mb-1">Identity distribution</p><h2 class="h4 fw-bold mb-4">Users by role</h2>
                    <?php $__currentLoopData = ['super_admin' => 'Super Admins', 'admin' => 'Administrators', 'captain' => 'Captains', 'player' => 'Players']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $role => $label): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                        <div class="d-flex justify-content-between align-items-center mb-3"><span class="text-secondary"><?php echo e($label); ?></span><span class="fw-bold"><?php echo e($roleCounts[$role] ?? 0); ?></span></div>
                    <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                    <a href="<?php echo e(route('super-admin.users.index')); ?>" class="btn btn-light w-100 mt-2">Review all identities</a>
                </div>
                <div class="cricket-surface p-4 p-lg-5">
                    <p class="cricket-kicker mb-1">Control center</p><h2 class="h4 fw-bold mb-4">Governance modules</h2>
                    <div class="vstack gap-2">
                        <a href="<?php echo e(route('super-admin.users.index')); ?>" class="btn btn-light text-start"><i class="fa-solid fa-users-gear text-success me-2"></i>Users and roles <span class="float-end text-secondary"><?php echo e($userCount); ?></span></a>
                        <a href="<?php echo e(route('super-admin.tournaments.index')); ?>" class="btn btn-light text-start"><i class="fa-solid fa-trophy text-success me-2"></i>Tournament fleet <span class="float-end text-secondary"><?php echo e($tournamentCount); ?></span></a>
                        <a href="<?php echo e(route('super-admin.api-clients.index')); ?>" class="btn btn-light text-start"><i class="fa-solid fa-plug text-success me-2"></i>API clients</a>
                        <a href="<?php echo e(route('super-admin.api-sessions.index')); ?>" class="btn btn-light text-start"><i class="fa-solid fa-mobile-screen-button text-success me-2"></i>API sessions</a>
                        <a href="<?php echo e(route('super-admin.audit-logs.index')); ?>" class="btn btn-light text-start"><i class="fa-solid fa-clock-rotate-left text-success me-2"></i>Audit explorer</a>
                        <a href="<?php echo e(route('super-admin.health')); ?>" class="btn btn-light text-start"><i class="fa-solid fa-heart-pulse text-success me-2"></i>System health</a>
                    </div>
                </div>
            </div>
        </div>

        <div class="cricket-surface p-4 p-lg-5">
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-3"><div><p class="cricket-kicker mb-1">Security trail</p><h2 class="h3 fw-bold mb-1">Recent platform activity</h2><p class="text-secondary mb-0">The latest administrative and operational actions across the platform.</p></div><div class="d-flex gap-3"><a href="<?php echo e(route('super-admin.audit-logs.export')); ?>" class="btn btn-sm btn-light"><i class="fa-solid fa-download me-1"></i>CSV export</a><a href="<?php echo e(route('super-admin.audit-logs.index')); ?>" class="btn btn-sm btn-outline-success">View all logs</a></div></div>
            <div class="table-responsive"><table class="table align-middle mb-0"><thead><tr><th>Action</th><th>Actor</th><th>Scope</th><th>Time</th></tr></thead><tbody><?php $__empty_1 = true; $__currentLoopData = $recentAuditLogs; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $log): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?><tr><td><span class="badge text-bg-light"><?php echo e($log->action); ?></span></td><td><?php echo e($log->user?->name ?: 'System'); ?><div class="small text-secondary"><?php echo e($log->ip_address ?: 'No IP recorded'); ?></div></td><td><?php echo e($log->tournament?->name ?: ($log->auditable_type ? class_basename($log->auditable_type).' #'.$log->auditable_id : 'Platform')); ?></td><td class="text-secondary"><?php echo e($log->created_at?->format('d M Y H:i')); ?></td></tr><?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?><tr><td colspan="4" class="text-center py-4 text-secondary">No audit activity yet.</td></tr><?php endif; ?></tbody></table></div>
        </div>
    </div>
 <?php echo $__env->renderComponent(); ?>
<?php endif; ?>
<?php if (isset($__attributesOriginal9ac128a9029c0e4701924bd2d73d7f54)): ?>
<?php $attributes = $__attributesOriginal9ac128a9029c0e4701924bd2d73d7f54; ?>
<?php unset($__attributesOriginal9ac128a9029c0e4701924bd2d73d7f54); ?>
<?php endif; ?>
<?php if (isset($__componentOriginal9ac128a9029c0e4701924bd2d73d7f54)): ?>
<?php $component = $__componentOriginal9ac128a9029c0e4701924bd2d73d7f54; ?>
<?php unset($__componentOriginal9ac128a9029c0e4701924bd2d73d7f54); ?>
<?php endif; ?>
<?php /**PATH C:\Users\Muhammad Aliyan\Downloads\cricket-draft-source\resources\views/super-admin/dashboard.blade.php ENDPATH**/ ?>