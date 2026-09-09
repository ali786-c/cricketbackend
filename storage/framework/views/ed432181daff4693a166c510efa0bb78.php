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
                <p class="cricket-kicker mb-2">Match Center</p>
                <h1 class="display-6 fw-bold mb-1"><?php echo e($inningsView->last()['batting_team'] ?? ($teams->first()?->short_name ?: $teams->first()?->name ?: 'TBD')); ?> vs <?php echo e($inningsView->last()['bowling_team'] ?? ($teams->last()?->short_name ?: $teams->last()?->name ?: 'TBD')); ?></h1>
                <p class="text-secondary mb-0">
                    <?php if($match->tournament): ?>
                        <?php echo e($match->tournament->name); ?> ·
                    <?php else: ?>
                        <span class="badge text-bg-secondary">Custom Match</span>
                    <?php endif; ?>
                    Match #<?php echo e($match->id); ?> · <?php echo e(ucfirst(str_replace('_', ' ', $match->status))); ?>

                </p>
            </div>
            <div class="d-flex flex-wrap gap-2">
                <?php if($match->status === 'live'): ?>
                    <a href="<?php echo e(route('admin.matches.scorer', $match)); ?>" class="btn btn-success"><i class="fa-solid fa-stopwatch me-2"></i>Open scorer room</a>
                <?php endif; ?>
                <?php if(in_array($match->status, ['live', 'completed', 'result_pending', 'approved'], true)): ?>
                    <a href="<?php echo e(route('public.matches.show', $match)); ?>" class="btn btn-light" target="_blank"><i class="fa-solid fa-eye me-2"></i>Public scorecard</a>
                <?php endif; ?>
                <a href="<?php echo e(route('super-admin.matches.index')); ?>" class="btn btn-light"><i class="fa-solid fa-arrow-left me-2"></i>All matches</a>
            </div>
        </div>
     <?php $__env->endSlot(); ?>

    <div class="container pb-5">
        <?php ($latest = $inningsView->last()); ?>
        
        <div class="cricket-surface p-4 mb-4">
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                <div>
                    <span class="badge <?php echo e($match->status === 'live' ? 'text-bg-success' : ($match->status === 'completed' || $match->status === 'approved' ? 'text-bg-dark' : 'text-bg-light')); ?> mb-2"><?php echo e(ucfirst(str_replace('_', ' ', $match->status))); ?></span>
                    <?php $__empty_1 = true; $__currentLoopData = $inningsView; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $inning): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?>
                        <div class="fw-bold fs-4">
                            <?php echo e($inning['batting_team']); ?> <span class="text-success"><?php echo e($inning['runs']); ?>/<?php echo e($inning['wickets']); ?></span>
                            <span class="text-secondary fs-6">(<?php echo e($inning['overs']); ?> / <?php echo e($inning['maximum_overs']); ?> ov)</span>
                            <?php if($inning['target']): ?><span class="text-secondary fs-6">· target <?php echo e($inning['target']); ?></span><?php endif; ?>
                        </div>
                    <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?>
                        <div class="text-secondary">Innings have not started yet — toss and lineups will appear here once recorded.</div>
                    <?php endif; ?>
                    <?php if($match->result_summary): ?>
                        <div class="small text-success fw-semibold mt-2"><i class="fa-solid fa-trophy me-1"></i><?php echo e($match->result_summary); ?></div>
                    <?php endif; ?>
                </div>
                <div class="text-md-end small text-secondary">
                    <?php if($match->fixture?->venue): ?><?php echo e($match->fixture->venue); ?><?php echo e($match->fixture->city ? ', '.$match->fixture->city : ''); ?><br><?php endif; ?>
                    <?php if($match->fixture?->scheduled_at): ?>Scheduled <?php echo e($match->fixture->scheduled_at->format('d M Y, H:i')); ?><br><?php endif; ?>
                    Overs per innings: <?php echo e($match->overs_per_innings ?: $match->ruleProfile?->overs_per_innings); ?> · <?php echo e($match->ruleProfile?->name ?: 'Standard rules'); ?><br>
                    Toss: <?php if($match->tossWinner): ?><?php echo e($match->tossWinner->short_name ?: $match->tossWinner->name); ?> chose to <?php echo e($match->toss_decision); ?> <?php else: ?> not recorded <?php endif; ?>
                </div>
            </div>
        </div>

        
        <div class="cricket-surface overflow-hidden">
            <ul class="nav nav-tabs px-3 pt-3" role="tablist">
                <li class="nav-item" role="presentation"><button class="nav-link active" data-bs-toggle="tab" data-bs-target="#tab-summary" type="button" role="tab"><i class="fa-solid fa-ranking-star me-1"></i>Summary</button></li>
                <?php if($inningsView->isNotEmpty()): ?>
                    <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-scorecard" type="button" role="tab"><i class="fa-solid fa-clipboard-list me-1"></i>Scorecard</button></li>
                    <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-stats" type="button" role="tab"><i class="fa-solid fa-chart-simple me-1"></i>Stats</button></li>
                <?php endif; ?>
                <?php if($mvp->isNotEmpty()): ?>
                    <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-mvp" type="button" role="tab"><i class="fa-solid fa-star me-1"></i>Super Stars</button></li>
                <?php endif; ?>
                <li class="nav-item" role="presentation"><button class="nav-link" data-bs-toggle="tab" data-bs-target="#tab-squads" type="button" role="tab"><i class="fa-solid fa-people-group me-1"></i>Squads</button></li>
            </ul>
            <div class="tab-content p-4">
                
                <div class="tab-pane fade show active" id="tab-summary" role="tabpanel">
                    <div class="row g-4">
                        <div class="col-lg-6">
                            <p class="cricket-kicker mb-2">Innings progress</p>
                            <div class="vstack gap-3">
                                <?php $__empty_1 = true; $__currentLoopData = $inningsView; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $inning): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?>
                                    <div class="border rounded-3 p-3 bg-light">
                                        <div class="d-flex justify-content-between align-items-center mb-2">
                                            <div><span class="badge text-bg-secondary">Innings <?php echo e($inning['number']); ?> · <?php echo e(ucfirst($inning['status'])); ?></span><div class="fw-bold mt-1"><?php echo e($inning['batting_team']); ?> batting</div></div>
                                            <div class="text-end"><div class="fw-bold fs-5"><?php echo e($inning['runs']); ?>/<?php echo e($inning['wickets']); ?></div><div class="small text-secondary"><?php echo e($inning['overs']); ?> / <?php echo e($inning['maximum_overs']); ?> ov</div></div>
                                        </div>
                                        <div class="small text-secondary mb-2">
                                            Run rate <?php echo e($inning['run_rate'] ?? '—'); ?><?php if($inning['target']): ?>
                                                · Target <?php echo e($inning['target']); ?>

                                            <?php endif; ?>
                                            <?php if($inning['extras']): ?>
                                                · Extras <?php echo e($inning['extras']); ?>

                                            <?php endif; ?>
                                        </div>
                                        <?php if($inning['overs_detail']): ?>
                                            <div class="d-flex flex-wrap gap-1">
                                                <?php $__currentLoopData = array_slice($inning['overs_detail'], -10); $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $over): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                                                    <span class="badge rounded-pill text-bg-light border" title="<?php echo e(implode(' · ', $over['notation'])); ?>">Ov <?php echo e($over['over']); ?>: <?php echo e($over['runs']); ?><?php echo e($over['wickets'] ? ' (w)' : ''); ?></span>
                                                <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                                            </div>
                                        <?php endif; ?>
                                    </div>
                                <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?>
                                    <div class="text-secondary">No innings data has been recorded for this match yet.</div>
                                <?php endif; ?>
                                <?php if($match->result_summary): ?>
                                    <div class="alert alert-success mb-0"><strong><?php echo e($match->result_summary); ?></strong></div>
                                <?php endif; ?>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <p class="cricket-kicker mb-2">Recent balls</p>
                            <?php if($latest && $latest['recent_balls']): ?>
                                <div class="d-flex flex-wrap gap-2 mb-4">
                                    <?php $__currentLoopData = $latest['recent_balls']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $ball): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                                        <span class="badge rounded-pill <?php echo e($ball['runs'] === 0 ? 'text-bg-light border' : ($ball['notation'] === 'W' ? 'text-bg-danger' : 'text-bg-success')); ?>"><?php echo e($ball['over']); ?> · <?php echo e($ball['notation']); ?></span>
                                    <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                                </div>
                            <?php else: ?>
                                <div class="text-secondary mb-4">No deliveries recorded yet.</div>
                            <?php endif; ?>
                            <p class="cricket-kicker mb-2">Operational details</p>
                            <dl class="row small mb-0">
                                <dt class="col-5 text-secondary">Fixture</dt><dd class="col-7"><?php echo e($match->fixture?->title ?? 'Draft-squad match'); ?> <?php if($match->fixture?->round_name): ?>· <?php echo e($match->fixture->round_name); ?><?php endif; ?></dd>
                                <dt class="col-5 text-secondary">Tournament</dt><dd class="col-7"><?php if($match->tournament): ?><?php echo e($match->tournament->name); ?> <?php else: ?> <span class="badge text-bg-secondary">Custom Match</span> <?php endif; ?></dd>
                                <dt class="col-5 text-secondary">Format</dt><dd class="col-7"><?php echo e($match->ruleProfile?->name ?: 'Standard'); ?> · <?php echo e($match->overs_per_innings ?: $match->ruleProfile?->overs_per_innings); ?> overs · <?php echo e($ballsPerOver); ?> balls/over</dd>
                                <dt class="col-5 text-secondary">Toss</dt><dd class="col-7"><?php if($match->tossWinner): ?><?php echo e($match->tossWinner->name); ?> chose to <?php echo e($match->toss_decision); ?> (<?php echo e($match->toss_recorded_at?->format('d M H:i')); ?>) <?php else: ?> Not recorded <?php endif; ?></dd>
                                <dt class="col-5 text-secondary">Started</dt><dd class="col-7"><?php echo e($match->started_at?->format('d M Y, H:i') ?? '—'); ?></dd>
                                <dt class="col-5 text-secondary">Completed</dt><dd class="col-7"><?php echo e($match->completed_at?->format('d M Y, H:i') ?? '—'); ?></dd>
                                <dt class="col-5 text-secondary">Result</dt><dd class="col-7"><?php echo e($match->result_summary ?: ucfirst($match->result_type ?: 'pending')); ?></dd>
                                <dt class="col-5 text-secondary">Revision</dt><dd class="col-7"><?php echo e($match->revision); ?> · last event <?php echo e($match->last_event_at?->format('d M H:i') ?? '—'); ?></dd>
                                <dt class="col-5 text-secondary">Created by</dt><dd class="col-7"><?php echo e($match->creator?->name ?? '—'); ?></dd>
                            </dl>
                        </div>
                    </div>
                </div>

                
                <?php if($inningsView->isNotEmpty()): ?>
                    <div class="tab-pane fade" id="tab-scorecard" role="tabpanel">
                        <?php $__currentLoopData = $inningsView; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $inning): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                            <div class="border rounded-3 p-3 mb-4 bg-light">
                                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3">
                                    <div>
                                        <span class="badge text-bg-<?php echo e($inning['status'] === 'live' ? 'success' : 'secondary'); ?>">INNINGS <?php echo e($inning['number']); ?> · <?php echo e(ucfirst($inning['status'])); ?></span>
                                        <h3 class="h5 fw-bold mt-2 mb-0"><?php echo e($inning['batting_team']); ?> <span class="text-success"><?php echo e($inning['runs']); ?>/<?php echo e($inning['wickets']); ?></span> <span class="text-secondary fs-6">(<?php echo e($inning['overs']); ?>)</span></h3>
                                        <div class="small text-secondary">vs <?php echo e($inning['bowling_team']); ?> bowling</div>
                                    </div>
                                    <?php if($inning['extras']): ?><span class="badge text-bg-light border">Extras: <?php echo e($inning['extras']); ?></span><?php endif; ?>
                                </div>
                                <div class="table-responsive mb-3">
                                    <table class="table table-sm align-middle mb-0 bg-white">
                                        <thead><tr><th>Batter</th><th>Dismissal</th><th class="text-end">R</th><th class="text-end">B</th><th class="text-end">4s</th><th class="text-end">6s</th><th class="text-end">SR</th></tr></thead>
                                        <tbody>
                                            <?php $__empty_1 = true; $__currentLoopData = $inning['batting']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $stat): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?>
                                                <tr>
                                                    <td><strong><?php echo e($stat['name']); ?></strong></td>
                                                    <td class="text-secondary small"><?php echo e($stat['dismissal']); ?></td>
                                                    <td class="text-end fw-semibold"><?php echo e($stat['runs']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['balls']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['fours']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['sixes']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['strike_rate']); ?></td>
                                                </tr>
                                            <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?>
                                                <tr><td colspan="7" class="text-center text-secondary py-3">No batting data recorded.</td></tr>
                                            <?php endif; ?>
                                        </tbody>
                                    </table>
                                </div>
                                <h4 class="h6 fw-bold">Bowling — <?php echo e($inning['bowling_team']); ?></h4>
                                <div class="table-responsive mb-3">
                                    <table class="table table-sm align-middle mb-0 bg-white">
                                        <thead><tr><th>Bowler</th><th class="text-end">O</th><th class="text-end">M</th><th class="text-end">R</th><th class="text-end">W</th><th class="text-end">Wd</th><th class="text-end">Nb</th><th class="text-end">Econ</th></tr></thead>
                                        <tbody>
                                            <?php $__empty_1 = true; $__currentLoopData = $inning['bowling']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $stat): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?>
                                                <tr>
                                                    <td><?php echo e($stat['name']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['overs']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['maidens']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['runs']); ?></td>
                                                    <td class="text-end fw-semibold"><?php echo e($stat['wickets']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['wides']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['no_balls']); ?></td>
                                                    <td class="text-end"><?php echo e($stat['economy']); ?></td>
                                                </tr>
                                            <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?>
                                                <tr><td colspan="8" class="text-center text-secondary py-3">No bowling data recorded.</td></tr>
                                            <?php endif; ?>
                                        </tbody>
                                    </table>
                                </div>
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <h4 class="h6 fw-bold">Fall of wickets</h4>
                                        <?php if($inning['fow']): ?>
                                            <ol class="small mb-0 ps-3">
                                                <?php $__currentLoopData = $inning['fow']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $f): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                                                    <li class="mb-1"><strong><?php echo e($f['batter']); ?></strong> <?php echo e($f['score']); ?>/<?php echo e($f['wicket']); ?> (<?php echo e($f['over']); ?> ov) — <?php echo e($f['dismissal']); ?><?php if($f['bowler']): ?>
                                                            b <?php echo e($f['bowler']); ?>

                                                        <?php endif; ?>
                                                        · p'ship <?php echo e($f['partnership_runs']); ?> (<?php echo e($f['partnership_balls']); ?>)</li>
                                                <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                                            </ol>
                                        <?php else: ?>
                                            <div class="small text-secondary">No wickets have fallen.</div>
                                        <?php endif; ?>
                                    </div>
                                    <div class="col-md-6">
                                        <h4 class="h6 fw-bold">Partnerships</h4>
                                        <?php if($inning['partnerships']): ?>
                                            <ul class="small mb-0 ps-3">
                                                <?php $__currentLoopData = $inning['partnerships']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $p): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                                                    <li class="mb-1"><?php echo e($p['batters'] ?: '—'); ?> — <?php echo e($p['runs']); ?> runs off <?php echo e($p['balls']); ?> balls <?php if($p['wicket']): ?>
                                                            (<?php echo e($p['wicket']); ?><?php echo e($p['wicket'] === 1 ? 'st' : 'th'); ?> wkt)
                                                        <?php endif; ?></li>
                                                <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                                            </ul>
                                        <?php else: ?>
                                            <div class="small text-secondary">No partnerships recorded yet.</div>
                                        <?php endif; ?>
                                    </div>
                                </div>
                            </div>
                        <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                    </div>

                    
                    <div class="tab-pane fade" id="tab-stats" role="tabpanel">
                        <div class="row g-4">
                            <?php $__empty_1 = true; $__currentLoopData = $inningsView; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $inning): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?>
                                <div class="col-lg-6">
                                    <div class="border rounded-3 p-3 h-100 bg-light">
                                        <div class="fw-bold mb-2"><?php echo e($inning['batting_team']); ?> innings · key numbers</div>
                                        <?php ($topBatters = collect($inning['batting'])->filter(fn ($b) => $b['balls'] > 0 || $b['runs'] > 0)->sortByDesc('runs')->take(3)); ?>
                                        <?php ($topBowlers = collect($inning['bowling'])->sortByDesc('wickets')->take(3)); ?>
                                        <div class="small text-secondary mb-1">Top scorers</div>
                                        <?php $__empty_2 = true; $__currentLoopData = $topBatters; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $b): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_2 = false; ?>
                                            <div class="d-flex justify-content-between small border-bottom py-1"><span><?php echo e($b['name']); ?></span><span class="fw-semibold"><?php echo e($b['runs']); ?> (<?php echo e($b['balls']); ?>b, <?php echo e($b['fours']); ?>×4, <?php echo e($b['sixes']); ?>×6)</span></div>
                                        <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_2): ?>
                                            <div class="small text-secondary">—</div>
                                        <?php endif; ?>
                                        <div class="small text-secondary mt-3 mb-1">Top wicket-takers</div>
                                        <?php $__empty_2 = true; $__currentLoopData = $topBowlers; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $bw): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_2 = false; ?>
                                            <div class="d-flex justify-content-between small border-bottom py-1"><span><?php echo e($bw['name']); ?></span><span class="fw-semibold"><?php echo e($bw['wickets']); ?>/<?php echo e($bw['runs']); ?> (<?php echo e($bw['overs']); ?> ov)</span></div>
                                        <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_2): ?>
                                            <div class="small text-secondary">—</div>
                                        <?php endif; ?>
                                        <div class="small text-secondary mt-3 mb-1">Over-by-over runs</div>
                                        <div class="d-flex flex-wrap gap-1">
                                            <?php $__currentLoopData = $inning['overs_detail']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $over): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                                                <span class="badge rounded-pill <?php echo e($over['runs'] >= 12 ? 'text-bg-success' : ($over['runs'] === 0 ? 'text-bg-light border' : 'text-bg-secondary')); ?>"><?php echo e($over['over']); ?>·<?php echo e($over['runs']); ?><?php echo e($over['wickets'] ? 'w' : ''); ?></span>
                                            <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                                            <?php if(empty($inning['overs_detail'])): ?><span class="small text-secondary">—</span><?php endif; ?>
                                        </div>
                                    </div>
                                </div>
                            <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?>
                                <div class="col-12 text-secondary">Stats unlock once scoring starts.</div>
                            <?php endif; ?>
                        </div>
                    </div>

                    
                    <?php if($mvp->isNotEmpty()): ?>
                        <div class="tab-pane fade" id="tab-mvp" role="tabpanel">
                            <div class="vstack gap-2">
                                <?php $__currentLoopData = $mvp; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $index => $star): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                                    <div class="d-flex align-items-center gap-3 border rounded-3 p-3 bg-light">
                                        <span class="badge <?php echo e($index === 0 ? 'text-bg-warning' : 'text-bg-light border'); ?> fs-6">#<?php echo e($index + 1); ?></span>
                                        <div class="flex-grow-1">
                                            <div class="fw-bold"><?php echo e($star['player_name']); ?></div>
                                            <div class="small text-secondary">
                                                <?php echo e($star['stats']['batting']['runs']); ?> runs
                                                · <?php echo e($star['stats']['bowling']['wickets']); ?> wkts
                                                · <?php echo e($star['stats']['fielding']['catches']); ?> catches
                                                · <?php echo e($star['stats']['fielding']['run_outs']); ?> run-outs
                                            </div>
                                        </div>
                                        <div class="text-end">
                                            <div class="fw-bold text-success fs-5"><?php echo e($star['points']['total']); ?></div>
                                            <div class="small text-secondary">MVP pts</div>
                                        </div>
                                    </div>
                                <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                            </div>
                        </div>
                    <?php endif; ?>
                <?php endif; ?>

                
                <div class="tab-pane fade" id="tab-squads" role="tabpanel">
                    <div class="row g-4">
                        <?php $__empty_1 = true; $__currentLoopData = $squads; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $squad): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); $__empty_1 = false; ?>
                            <div class="col-lg-6">
                                <div class="border rounded-3 p-3 h-100 bg-light">
                                    <div class="d-flex justify-content-between align-items-center mb-2">
                                        <div class="fw-bold"><?php echo e($squad['team_name']); ?></div>
                                        <span class="badge text-bg-secondary"><?php echo e(count($squad['players'])); ?> players</span>
                                    </div>
                                    <ul class="list-group list-group-flush">
                                        <?php $__currentLoopData = $squad['players']; $__env->addLoop($__currentLoopData); foreach($__currentLoopData as $player): $__env->incrementLoopIndices(); $loop = $__env->getLastLoop(); ?>
                                            <li class="list-group-item bg-transparent d-flex justify-content-between align-items-center px-0 py-2">
                                                <div>
                                                    <span class="fw-semibold"><?php echo e($player['name']); ?></span>
                                                    <?php if($player['is_captain']): ?><span class="badge text-bg-warning ms-1">C</span><?php endif; ?>
                                                    <?php if($player['is_wicketkeeper']): ?><span class="badge text-bg-info ms-1">WK</span><?php endif; ?>
                                                    <div class="small text-secondary"><?php echo e($player['role'] ?: 'Role not set'); ?> · <?php echo e(str_replace('_', ' ', $player['selection'])); ?></div>
                                                </div>
                                                <?php if($player['batting_order']): ?><span class="badge text-bg-light border">#<?php echo e($player['batting_order']); ?></span><?php endif; ?>
                                            </li>
                                        <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); ?>
                                    </ul>
                                </div>
                            </div>
                        <?php endforeach; $__env->popLoop(); $loop = $__env->getLastLoop(); if ($__empty_1): ?>
                            <div class="col-12 text-secondary">No squads registered for this match yet.</div>
                        <?php endif; ?>
                    </div>
                </div>
            </div>
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
<?php /**PATH C:\Users\Muhammad Aliyan\Downloads\cricket-draft-source\resources\views/super-admin/matches/show.blade.php ENDPATH**/ ?>