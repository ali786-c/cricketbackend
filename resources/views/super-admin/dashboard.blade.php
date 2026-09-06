<x-app-layout>
    <x-slot name="header">
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-end gap-3">
            <div>
                <p class="cricket-kicker mb-2">Platform governance</p>
                <h1 class="display-6 fw-bold mb-2">Super Admin Control Plane</h1>
                <p class="text-secondary mb-0">A single operational view of identities, tournaments, live matches, APIs, sessions, security, and system health.</p>
            </div>
            <div class="d-flex flex-wrap gap-2">
                <a href="{{ route('super-admin.users.index') }}" class="btn btn-success"><i class="fa-solid fa-users-gear me-2"></i>Govern users</a>
                <a href="{{ route('super-admin.tournaments.index') }}" class="btn btn-light"><i class="fa-solid fa-trophy me-2"></i>Monitor tournaments</a>
                <a href="{{ route('super-admin.health') }}" class="btn btn-light"><i class="fa-solid fa-heart-pulse me-2"></i>System health</a>
            </div>
        </div>
    </x-slot>

    <div class="container pb-5">
        <div class="row g-3 mb-4">
            <div class="col-6 col-xl-3"><div class="cricket-surface p-4 h-100"><div class="small text-secondary mb-1">Total users</div><div class="display-6 fw-bold">{{ number_format($userCount) }}</div><div class="small text-secondary mt-2">{{ $roleCounts['player'] ?? 0 }} players · {{ $roleCounts['captain'] ?? 0 }} captains</div><a href="{{ route('super-admin.users.index') }}" class="small text-success fw-semibold d-inline-block mt-3">Open identity governance <i class="fa-solid fa-arrow-right ms-1"></i></a></div></div>
            <div class="col-6 col-xl-3"><div class="cricket-surface p-4 h-100"><div class="small text-secondary mb-1">Tournament fleet</div><div class="display-6 fw-bold">{{ number_format($tournamentCount) }}</div><div class="small text-secondary mt-2">{{ $tournamentStatuses['live'] ?? 0 }} live · {{ $tournamentStatuses['completed'] ?? 0 }} completed</div><a href="{{ route('super-admin.tournaments.index') }}" class="small text-success fw-semibold d-inline-block mt-3">Open fleet oversight <i class="fa-solid fa-arrow-right ms-1"></i></a></div></div>
            <div class="col-6 col-xl-3"><div class="cricket-surface p-4 h-100"><div class="small text-secondary mb-1">Live operations</div><div class="display-6 fw-bold text-success">{{ number_format($liveMatchCount) }}</div><div class="small text-secondary mt-2">{{ $tournamentStatuses['live'] ?? 0 }} live tournaments · {{ $fixtureStatuses['scheduled'] ?? 0 }} scheduled fixtures</div><a href="{{ route('super-admin.tournaments.index', ['status' => 'live']) }}" class="small text-success fw-semibold d-inline-block mt-3">Inspect active rooms <i class="fa-solid fa-arrow-right ms-1"></i></a></div></div>
            <div class="col-6 col-xl-3"><div class="cricket-surface p-4 h-100"><div class="small text-secondary mb-1">API posture</div><div class="display-6 fw-bold">{{ number_format($activeApiClientCount) }}<span class="fs-5 text-secondary">/{{ $apiClientCount }}</span></div><div class="small text-secondary mt-2">{{ $activeApiTokenCount }} active sessions · {{ $expiredTokenCount }} expired</div><a href="{{ route('super-admin.api-clients.index') }}" class="small text-success fw-semibold d-inline-block mt-3">Govern API clients <i class="fa-solid fa-arrow-right ms-1"></i></a></div></div>
        </div>

        @if($alerts->isNotEmpty())
            <div class="cricket-surface p-4 mb-4 border-start border-4 border-warning">
                <div class="d-flex justify-content-between align-items-center gap-3 mb-3"><div><p class="cricket-kicker mb-1">Attention required</p><h2 class="h4 fw-bold mb-0">Operational alerts</h2></div><span class="badge text-bg-warning">{{ $alerts->count() }} open</span></div>
                <div class="row g-2">
                    @foreach($alerts as $alert)
                        <div class="col-12 col-lg-6"><a href="{{ $alert['href'] }}" class="d-flex align-items-center justify-content-between gap-3 p-3 rounded-3 text-decoration-none bg-light border"><div><div class="fw-semibold text-dark">{{ $alert['label'] }}</div><div class="small text-secondary">Review this item from the linked governance module.</div></div><span class="badge {{ $alert['level'] === 'danger' ? 'text-bg-danger' : ($alert['level'] === 'warning' ? 'text-bg-warning' : 'text-bg-info') }}">{{ $alert['value'] }}</span></a></div>
                    @endforeach
                </div>
            </div>
        @endif

        <div class="row g-4 mb-4">
            <div class="col-xl-8">
                <div class="cricket-surface p-4 p-lg-5 h-100">
                    <div class="d-flex justify-content-between align-items-start gap-3 mb-4"><div><p class="cricket-kicker mb-1">Platform fleet</p><h2 class="h3 fw-bold mb-1">Tournament status map</h2><p class="text-secondary mb-0">See where every competition is in its lifecycle without opening separate admin workspaces.</p></div><a href="{{ route('super-admin.tournaments.index') }}" class="btn btn-sm btn-light">View all tournaments</a></div>
                    <div class="row g-2 mb-4">
                        @foreach(['draft' => 'Draft', 'registration' => 'Registration', 'ready' => 'Ready', 'live' => 'Live', 'completed' => 'Completed', 'cancelled' => 'Cancelled'] as $status => $label)
                            <div class="col-6 col-md-4"><a class="text-decoration-none" href="{{ route('super-admin.tournaments.index', ['status' => $status]) }}"><div class="p-3 rounded-3 bg-light border h-100"><div class="small text-secondary">{{ $label }}</div><div class="h3 fw-bold mb-0 text-dark">{{ $tournamentStatuses[$status] ?? 0 }}</div></div></a></div>
                        @endforeach
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-3"><div><p class="cricket-kicker mb-1">Live operations</p><h3 class="h5 fw-bold mb-0">Active tournaments</h3></div><span class="small text-secondary">{{ $liveTournaments->count() }} shown</span></div>
                    @forelse($liveTournaments as $tournament)
                        <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 border-bottom py-3"><div><div class="d-flex align-items-center gap-2"><span class="status-dot bg-success"></span><div class="fw-bold">{{ $tournament->name }}</div><span class="badge text-bg-success">Live</span></div><div class="small text-secondary mt-1">{{ $tournament->teams_count }} teams · {{ $tournament->matches_count }} matches · {{ $tournament->tournament_players_count ?? 0 }} registrations</div></div><a href="{{ route('super-admin.tournaments.show', $tournament) }}" class="btn btn-sm btn-outline-success">Inspect fleet view</a></div>
                    @empty
                        <div class="cricket-surface-soft p-4 text-center text-secondary">No live tournaments at the moment.</div>
                    @endforelse
                    <div class="d-flex justify-content-between align-items-center mb-3 mt-5"><div><p class="cricket-kicker mb-1">Match rooms</p><h3 class="h5 fw-bold mb-0">Live matches</h3></div><span class="small text-secondary">{{ $liveMatches->count() }} active</span></div>
                    @forelse($liveMatches as $match)
                        <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 border-bottom py-3"><div><div class="fw-semibold">{{ $match->fixture?->homeTeam?->short_name ?: $match->fixture?->homeTeam?->name ?: 'TBD' }} <span class="text-secondary">vs</span> {{ $match->fixture?->awayTeam?->short_name ?: $match->fixture?->awayTeam?->name ?: 'TBD' }}</div><div class="small text-secondary">{{ $match->tournament?->name ?: 'Tournament' }} · revision {{ $match->revision }}</div></div><a href="{{ route('admin.matches.scorer', $match) }}" class="btn btn-sm btn-outline-success">Open scorer</a></div>
                    @empty
                        <div class="cricket-surface-soft p-4 text-center text-secondary">No live match rooms at the moment.</div>
                    @endforelse
                </div>
            </div>
            <div class="col-xl-4">
                <div class="cricket-surface p-4 p-lg-5 mb-4">
                    <p class="cricket-kicker mb-1">Identity distribution</p><h2 class="h4 fw-bold mb-4">Users by role</h2>
                    @foreach(['super_admin' => 'Super Admins', 'admin' => 'Administrators', 'captain' => 'Captains', 'player' => 'Players'] as $role => $label)
                        <div class="d-flex justify-content-between align-items-center mb-3"><span class="text-secondary">{{ $label }}</span><span class="fw-bold">{{ $roleCounts[$role] ?? 0 }}</span></div>
                    @endforeach
                    <a href="{{ route('super-admin.users.index') }}" class="btn btn-light w-100 mt-2">Review all identities</a>
                </div>
                <div class="cricket-surface p-4 p-lg-5">
                    <p class="cricket-kicker mb-1">Control center</p><h2 class="h4 fw-bold mb-4">Governance modules</h2>
                    <div class="vstack gap-2">
                        <a href="{{ route('super-admin.users.index') }}" class="btn btn-light text-start"><i class="fa-solid fa-users-gear text-success me-2"></i>Users and roles <span class="float-end text-secondary">{{ $userCount }}</span></a>
                        <a href="{{ route('super-admin.tournaments.index') }}" class="btn btn-light text-start"><i class="fa-solid fa-trophy text-success me-2"></i>Tournament fleet <span class="float-end text-secondary">{{ $tournamentCount }}</span></a>
                        <a href="{{ route('super-admin.api-clients.index') }}" class="btn btn-light text-start"><i class="fa-solid fa-plug text-success me-2"></i>API clients</a>
                        <a href="{{ route('super-admin.api-sessions.index') }}" class="btn btn-light text-start"><i class="fa-solid fa-mobile-screen-button text-success me-2"></i>API sessions</a>
                        <a href="{{ route('super-admin.audit-logs.index') }}" class="btn btn-light text-start"><i class="fa-solid fa-clock-rotate-left text-success me-2"></i>Audit explorer</a>
                        <a href="{{ route('super-admin.health') }}" class="btn btn-light text-start"><i class="fa-solid fa-heart-pulse text-success me-2"></i>System health</a>
                    </div>
                </div>
            </div>
        </div>

        <div class="cricket-surface p-4 p-lg-5">
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-3"><div><p class="cricket-kicker mb-1">Security trail</p><h2 class="h3 fw-bold mb-1">Recent platform activity</h2><p class="text-secondary mb-0">The latest administrative and operational actions across the platform.</p></div><div class="d-flex gap-3"><a href="{{ route('super-admin.audit-logs.export') }}" class="btn btn-sm btn-light"><i class="fa-solid fa-download me-1"></i>CSV export</a><a href="{{ route('super-admin.audit-logs.index') }}" class="btn btn-sm btn-outline-success">View all logs</a></div></div>
            <div class="table-responsive"><table class="table align-middle mb-0"><thead><tr><th>Action</th><th>Actor</th><th>Scope</th><th>Time</th></tr></thead><tbody>@forelse($recentAuditLogs as $log)<tr><td><span class="badge text-bg-light">{{ $log->action }}</span></td><td>{{ $log->user?->name ?: 'System' }}<div class="small text-secondary">{{ $log->ip_address ?: 'No IP recorded' }}</div></td><td>{{ $log->tournament?->name ?: ($log->auditable_type ? class_basename($log->auditable_type).' #'.$log->auditable_id : 'Platform') }}</td><td class="text-secondary">{{ $log->created_at?->format('d M Y H:i') }}</td></tr>@empty<tr><td colspan="4" class="text-center py-4 text-secondary">No audit activity yet.</td></tr>@endforelse</tbody></table></div>
        </div>
    </div>
</x-app-layout>
