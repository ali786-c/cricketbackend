<x-app-layout>
    <x-slot name="header">
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-end gap-3">
            <div>
                <p class="cricket-kicker mb-2">Match Center</p>
                <h1 class="display-6 fw-bold mb-1">{{ $inningsView->last()['batting_team'] ?? ($teams->first()?->short_name ?: $teams->first()?->name ?: 'TBD') }} vs {{ $inningsView->last()['bowling_team'] ?? ($teams->last()?->short_name ?: $teams->last()?->name ?: 'TBD') }}</h1>
                <p class="text-secondary mb-0">
                    @if($match->tournament)
                        {{ $match->tournament->name }} ·
                    @else
                        <span class="badge text-bg-secondary">Custom Match</span>
                    @endif
                    Match #{{ $match->id }} · {{ ucfirst(str_replace('_', ' ', $match->status)) }}
                </p>
            </div>
            <div class="d-flex flex-wrap gap-2">
                @if($match->status === 'live')
                    <a href="{{ route('admin.matches.scorer', $match) }}" class="btn btn-success"><i class="fa-solid fa-stopwatch me-2"></i>Open scorer room</a>
                @endif
                @if(in_array($match->status, ['live', 'completed', 'result_pending', 'approved'], true))
                    <a href="{{ route('public.matches.show', $match) }}" class="btn btn-light" target="_blank"><i class="fa-solid fa-eye me-2"></i>Public scorecard</a>
                @endif
                <a href="{{ route('super-admin.matches.index') }}" class="btn btn-light"><i class="fa-solid fa-arrow-left me-2"></i>All matches</a>
            </div>
        </div>
    </x-slot>

    <div class="container pb-5">
        @php($latest = $inningsView->last())
        {{-- Hero score strip, like the mobile Match Center header --}}
        <div class="cricket-surface p-4 mb-4">
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                <div>
                    <span class="badge {{ $match->status === 'live' ? 'text-bg-success' : ($match->status === 'completed' || $match->status === 'approved' ? 'text-bg-dark' : 'text-bg-light') }} mb-2">{{ ucfirst(str_replace('_', ' ', $match->status)) }}</span>
                    @forelse($inningsView as $inning)
                        <div class="fw-bold fs-4">
                            {{ $inning['batting_team'] }} <span class="text-success">{{ $inning['runs'] }}/{{ $inning['wickets'] }}</span>
                            <span class="text-secondary fs-6">({{ $inning['overs'] }} / {{ $inning['maximum_overs'] }} ov)</span>
                            @if($inning['target'])<span class="text-secondary fs-6">· target {{ $inning['target'] }}</span>@endif
                        </div>
                    @empty
                        <div class="text-secondary">Innings have not started yet — toss and lineups will appear here once recorded.</div>
                    @endforelse
                    @if($match->result_summary)
                        <div class="small text-success fw-semibold mt-2"><i class="fa-solid fa-trophy me-1"></i>{{ $match->result_summary }}</div>
                    @endif
                </div>
                <div class="text-md-end small text-secondary">
                    @if($match->fixture?->venue){{ $match->fixture->venue }}{{ $match->fixture->city ? ', '.$match->fixture->city : '' }}<br>@endif
                    @if($match->fixture?->scheduled_at)Scheduled {{ $match->fixture->scheduled_at->format('d M Y, H:i') }}<br>@endif
                    Overs per innings: {{ $match->overs_per_innings ?: $match->ruleProfile?->overs_per_innings }} · {{ $match->ruleProfile?->name ?: 'Standard rules' }}<br>
                    Toss: @if($match->tossWinner){{ $match->tossWinner->short_name ?: $match->tossWinner->name }} chose to {{ $match->toss_decision }} @else not recorded @endif
                </div>
            </div>
        </div>

        {{-- Tabs like the mobile Match Center --}}
        <div class="cricket-surface overflow-hidden">
            <ul class="nav nav-tabs px-3 pt-3" role="tablist">
                <li class="nav-item" role="presentation"><button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tab-summary" type="button" role="tab"><i class="fa-solid fa-ranking-star me-1"></i>Summary</button></li>
                @if($inningsView->isNotEmpty())
                    <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-scorecard" type="button" role="tab"><i class="fa-solid fa-clipboard-list me-1"></i>Scorecard</button></li>
                    <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-stats" type="button" role="tab"><i class="fa-solid fa-chart-simple me-1"></i>Stats</button></li>
                @endif
                @if($mvp->isNotEmpty())
                    <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-mvp" type="button" role="tab"><i class="fa-solid fa-star me-1"></i>Super Stars</button></li>
                @endif
                <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-squads" type="button" role="tab"><i class="fa-solid fa-people-group me-1"></i>Squads</button></li>
            </ul>
            <div class="tab-content p-4">
                {{-- ============ SUMMARY ============ --}}
                <div class="tab-pane fade show active" id="tab-summary" role="tabpanel">
                    <div class="row g-4">
                        <div class="col-lg-6">
                            <p class="cricket-kicker mb-2">Innings progress</p>
                            <div class="vstack gap-3">
                                @forelse($inningsView as $inning)
                                    <div class="border rounded-3 p-3 bg-light">
                                        <div class="d-flex justify-content-between align-items-center mb-2">
                                            <div><span class="badge text-bg-secondary">Innings {{ $inning['number'] }} · {{ ucfirst($inning['status']) }}</span><div class="fw-bold mt-1">{{ $inning['batting_team'] }} batting</div></div>
                                            <div class="text-end"><div class="fw-bold fs-5">{{ $inning['runs'] }}/{{ $inning['wickets'] }}</div><div class="small text-secondary">{{ $inning['overs'] }} / {{ $inning['maximum_overs'] }} ov</div></div>
                                        </div>
                                        <div class="small text-secondary mb-2">
                                            Run rate {{ $inning['run_rate'] ?? '—' }}@if($inning['target'])
                                                · Target {{ $inning['target'] }}
                                            @endif
                                            @if($inning['extras'])
                                                · Extras {{ $inning['extras'] }}
                                            @endif
                                        </div>
                                        @if($inning['overs_detail'])
                                            <div class="d-flex flex-wrap gap-1">
                                                @foreach(array_slice($inning['overs_detail'], -10) as $over)
                                                    <span class="badge rounded-pill text-bg-light border" title="{{ implode(' · ', $over['notation']) }}">Ov {{ $over['over'] }}: {{ $over['runs'] }}{{ $over['wickets'] ? ' (w)' : '' }}</span>
                                                @endforeach
                                            </div>
                                        @endif
                                    </div>
                                @empty
                                    <div class="text-secondary">No innings data has been recorded for this match yet.</div>
                                @endforelse
                                @if($match->result_summary)
                                    <div class="alert alert-success mb-0"><strong>{{ $match->result_summary }}</strong></div>
                                @endif
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <p class="cricket-kicker mb-2">Recent balls</p>
                            @if($latest && $latest['recent_balls'])
                                <div class="d-flex flex-wrap gap-2 mb-4">
                                    @foreach($latest['recent_balls'] as $ball)
                                        <span class="badge rounded-pill {{ $ball['runs'] === 0 ? 'text-bg-light border' : ($ball['notation'] === 'W' ? 'text-bg-danger' : 'text-bg-success') }}">{{ $ball['over'] }} · {{ $ball['notation'] }}</span>
                                    @endforeach
                                </div>
                            @else
                                <div class="text-secondary mb-4">No deliveries recorded yet.</div>
                            @endif
                            <p class="cricket-kicker mb-2">Operational details</p>
                            <dl class="row small mb-0">
                                <dt class="col-5 text-secondary">Fixture</dt><dd class="col-7">{{ $match->fixture?->title ?? 'Draft-squad match' }} @if($match->fixture?->round_name)· {{ $match->fixture->round_name }}@endif</dd>
                                <dt class="col-5 text-secondary">Tournament</dt><dd class="col-7">@if($match->tournament){{ $match->tournament->name }} @else <span class="badge text-bg-secondary">Custom Match</span> @endif</dd>
                                <dt class="col-5 text-secondary">Format</dt><dd class="col-7">{{ $match->ruleProfile?->name ?: 'Standard' }} · {{ $match->overs_per_innings ?: $match->ruleProfile?->overs_per_innings }} overs · {{ $ballsPerOver }} balls/over</dd>
                                <dt class="col-5 text-secondary">Toss</dt><dd class="col-7">@if($match->tossWinner){{ $match->tossWinner->name }} chose to {{ $match->toss_decision }} ({{ $match->toss_recorded_at?->format('d M H:i') }}) @else Not recorded @endif</dd>
                                <dt class="col-5 text-secondary">Started</dt><dd class="col-7">{{ $match->started_at?->format('d M Y, H:i') ?? '—' }}</dd>
                                <dt class="col-5 text-secondary">Completed</dt><dd class="col-7">{{ $match->completed_at?->format('d M Y, H:i') ?? '—' }}</dd>
                                <dt class="col-5 text-secondary">Result</dt><dd class="col-7">{{ $match->result_summary ?: ucfirst($match->result_type ?: 'pending') }}</dd>
                                <dt class="col-5 text-secondary">Revision</dt><dd class="col-7">{{ $match->revision }} · last event {{ $match->last_event_at?->format('d M H:i') ?? '—' }}</dd>
                                <dt class="col-5 text-secondary">Created by</dt><dd class="col-7">{{ $match->creator?->name ?? '—' }}</dd>
                            </dl>
                        </div>
                    </div>
                </div>

                {{-- ============ SCORECARD ============ --}}
                @if($inningsView->isNotEmpty())
                    <div class="tab-pane fade" id="tab-scorecard" role="tabpanel">
                        @foreach($inningsView as $inning)
                            <div class="border rounded-3 p-3 mb-4 bg-light">
                                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3">
                                    <div>
                                        <span class="badge text-bg-{{ $inning['status'] === 'live' ? 'success' : 'secondary' }}">INNINGS {{ $inning['number'] }} · {{ ucfirst($inning['status']) }}</span>
                                        <h3 class="h5 fw-bold mt-2 mb-0">{{ $inning['batting_team'] }} <span class="text-success">{{ $inning['runs'] }}/{{ $inning['wickets'] }}</span> <span class="text-secondary fs-6">({{ $inning['overs'] }})</span></h3>
                                        <div class="small text-secondary">vs {{ $inning['bowling_team'] }} bowling</div>
                                    </div>
                                    @if($inning['extras'])<span class="badge text-bg-light border">Extras: {{ $inning['extras'] }}</span>@endif
                                </div>
                                <div class="table-responsive mb-3">
                                    <table class="table table-sm align-middle mb-0 bg-white">
                                        <thead><tr><th>Batter</th><th>Dismissal</th><th class="text-end">R</th><th class="text-end">B</th><th class="text-end">4s</th><th class="text-end">6s</th><th class="text-end">SR</th></tr></thead>
                                        <tbody>
                                            @forelse($inning['batting'] as $stat)
                                                <tr>
                                                    <td><strong>{{ $stat['name'] }}</strong></td>
                                                    <td class="text-secondary small">{{ $stat['dismissal'] }}</td>
                                                    <td class="text-end fw-semibold">{{ $stat['runs'] }}</td>
                                                    <td class="text-end">{{ $stat['balls'] }}</td>
                                                    <td class="text-end">{{ $stat['fours'] }}</td>
                                                    <td class="text-end">{{ $stat['sixes'] }}</td>
                                                    <td class="text-end">{{ $stat['strike_rate'] }}</td>
                                                </tr>
                                            @empty
                                                <tr><td colspan="7" class="text-center text-secondary py-3">No batting data recorded.</td></tr>
                                            @endforelse
                                        </tbody>
                                    </table>
                                </div>
                                <h4 class="h6 fw-bold">Bowling — {{ $inning['bowling_team'] }}</h4>
                                <div class="table-responsive mb-3">
                                    <table class="table table-sm align-middle mb-0 bg-white">
                                        <thead><tr><th>Bowler</th><th class="text-end">O</th><th class="text-end">M</th><th class="text-end">R</th><th class="text-end">W</th><th class="text-end">Wd</th><th class="text-end">Nb</th><th class="text-end">Econ</th></tr></thead>
                                        <tbody>
                                            @forelse($inning['bowling'] as $stat)
                                                <tr>
                                                    <td>{{ $stat['name'] }}</td>
                                                    <td class="text-end">{{ $stat['overs'] }}</td>
                                                    <td class="text-end">{{ $stat['maidens'] }}</td>
                                                    <td class="text-end">{{ $stat['runs'] }}</td>
                                                    <td class="text-end fw-semibold">{{ $stat['wickets'] }}</td>
                                                    <td class="text-end">{{ $stat['wides'] }}</td>
                                                    <td class="text-end">{{ $stat['no_balls'] }}</td>
                                                    <td class="text-end">{{ $stat['economy'] }}</td>
                                                </tr>
                                            @empty
                                                <tr><td colspan="8" class="text-center text-secondary py-3">No bowling data recorded.</td></tr>
                                            @endforelse
                                        </tbody>
                                    </table>
                                </div>
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <h4 class="h6 fw-bold">Fall of wickets</h4>
                                        @if($inning['fow'])
                                            <ol class="small mb-0 ps-3">
                                                @foreach($inning['fow'] as $f)
                                                    <li class="mb-1"><strong>{{ $f['batter'] }}</strong> {{ $f['score'] }}/{{ $f['wicket'] }} ({{ $f['over'] }} ov) — {{ $f['dismissal'] }}@if($f['bowler'])
                                                            b {{ $f['bowler'] }}
                                                        @endif
                                                        · p'ship {{ $f['partnership_runs'] }} ({{ $f['partnership_balls'] }})</li>
                                                @endforeach
                                            </ol>
                                        @else
                                            <div class="small text-secondary">No wickets have fallen.</div>
                                        @endif
                                    </div>
                                    <div class="col-md-6">
                                        <h4 class="h6 fw-bold">Partnerships</h4>
                                        @if($inning['partnerships'])
                                            <ul class="small mb-0 ps-3">
                                                @foreach($inning['partnerships'] as $p)
                                                    <li class="mb-1">{{ $p['batters'] ?: '—' }} — {{ $p['runs'] }} runs off {{ $p['balls'] }} balls @if($p['wicket'])
                                                            ({{ $p['wicket'] }}{{ $p['wicket'] === 1 ? 'st' : 'th' }} wkt)
                                                        @endif</li>
                                                @endforeach
                                            </ul>
                                        @else
                                            <div class="small text-secondary">No partnerships recorded yet.</div>
                                        @endif
                                    </div>
                                </div>
                            </div>
                        @endforeach
                    </div>

                    {{-- ============ STATS ============ --}}
                    <div class="tab-pane fade" id="tab-stats" role="tabpanel">
                        <div class="row g-4">
                            @forelse($inningsView as $inning)
                                <div class="col-lg-6">
                                    <div class="border rounded-3 p-3 h-100 bg-light">
                                        <div class="fw-bold mb-2">{{ $inning['batting_team'] }} innings · key numbers</div>
                                        @php($topBatters = collect($inning['batting'])->filter(fn ($b) => $b['balls'] > 0 || $b['runs'] > 0)->sortByDesc('runs')->take(3))
                                        @php($topBowlers = collect($inning['bowling'])->sortByDesc('wickets')->take(3))
                                        <div class="small text-secondary mb-1">Top scorers</div>
                                        @forelse($topBatters as $b)
                                            <div class="d-flex justify-content-between small border-bottom py-1"><span>{{ $b['name'] }}</span><span class="fw-semibold">{{ $b['runs'] }} ({{ $b['balls'] }}b, {{ $b['fours'] }}×4, {{ $b['sixes'] }}×6)</span></div>
                                        @empty
                                            <div class="small text-secondary">—</div>
                                        @endforelse
                                        <div class="small text-secondary mt-3 mb-1">Top wicket-takers</div>
                                        @forelse($topBowlers as $bw)
                                            <div class="d-flex justify-content-between small border-bottom py-1"><span>{{ $bw['name'] }}</span><span class="fw-semibold">{{ $bw['wickets'] }}/{{ $bw['runs'] }} ({{ $bw['overs'] }} ov)</span></div>
                                        @empty
                                            <div class="small text-secondary">—</div>
                                        @endforelse
                                        <div class="small text-secondary mt-3 mb-1">Over-by-over runs</div>
                                        <div class="d-flex flex-wrap gap-1">
                                            @foreach($inning['overs_detail'] as $over)
                                                <span class="badge rounded-pill {{ $over['runs'] >= 12 ? 'text-bg-success' : ($over['runs'] === 0 ? 'text-bg-light border' : 'text-bg-secondary') }}">{{ $over['over'] }}·{{ $over['runs'] }}{{ $over['wickets'] ? 'w' : '' }}</span>
                                            @endforeach
                                            @if(empty($inning['overs_detail']))<span class="small text-secondary">—</span>@endif
                                        </div>
                                    </div>
                                </div>
                            @empty
                                <div class="col-12 text-secondary">Stats unlock once scoring starts.</div>
                            @endforelse
                        </div>
                    </div>

                    {{-- ============ SUPER STARS (MVP) ============ --}}
                    @if($mvp->isNotEmpty())
                        <div class="tab-pane fade" id="tab-mvp" role="tabpanel">
                            <div class="vstack gap-2">
                                @foreach($mvp as $index => $star)
                                    <div class="d-flex align-items-center gap-3 border rounded-3 p-3 bg-light">
                                        <span class="badge {{ $index === 0 ? 'text-bg-warning' : 'text-bg-light border' }} fs-6">#{{ $index + 1 }}</span>
                                        <div class="flex-grow-1">
                                            <div class="fw-bold">{{ $star['player_name'] }}</div>
                                            <div class="small text-secondary">
                                                {{ $star['stats']['batting']['runs'] }} runs
                                                · {{ $star['stats']['bowling']['wickets'] }} wkts
                                                · {{ $star['stats']['fielding']['catches'] }} catches
                                                · {{ $star['stats']['fielding']['run_outs'] }} run-outs
                                            </div>
                                        </div>
                                        <div class="text-end">
                                            <div class="fw-bold text-success fs-5">{{ $star['points']['total'] }}</div>
                                            <div class="small text-secondary">MVP pts</div>
                                        </div>
                                    </div>
                                @endforeach
                            </div>
                        </div>
                    @endif
                @endif

                {{-- ============ SQUADS ============ --}}
                <div class="tab-pane fade" id="tab-squads" role="tabpanel">
                    <div class="row g-4">
                        @forelse($squads as $squad)
                            <div class="col-lg-6">
                                <div class="border rounded-3 p-3 h-100 bg-light">
                                    <div class="d-flex justify-content-between align-items-center mb-2">
                                        <div class="fw-bold">{{ $squad['team_name'] }}</div>
                                        <span class="badge text-bg-secondary">{{ count($squad['players']) }} players</span>
                                    </div>
                                    <ul class="list-group list-group-flush">
                                        @foreach($squad['players'] as $player)
                                            <li class="list-group-item bg-transparent d-flex justify-content-between align-items-center px-0 py-2">
                                                <div>
                                                    <span class="fw-semibold">{{ $player['name'] }}</span>
                                                    @if($player['is_captain'])<span class="badge text-bg-warning ms-1">C</span>@endif
                                                    @if($player['is_wicketkeeper'])<span class="badge text-bg-info ms-1">WK</span>@endif
                                                    <div class="small text-secondary">{{ $player['role'] ?: 'Role not set' }} · {{ str_replace('_', ' ', $player['selection']) }}</div>
                                                </div>
                                                @if($player['batting_order'])<span class="badge text-bg-light border">#{{ $player['batting_order'] }}</span>@endif
                                            </li>
                                        @endforeach
                                    </ul>
                                </div>
                            </div>
                        @empty
                            <div class="col-12 text-secondary">No squads registered for this match yet.</div>
                        @endforelse
                    </div>
                </div>
            </div>
        </div>
    </div>
</x-app-layout>
