@extends('layouts.app')

@section('content')
<div class="container py-4" x-data="{
    runs_off_bat: 0, wides: 0, no_balls: 0, byes: 0, leg_byes: 0, penalty_runs: 0, wicketEnabled: false,
    setRuns(value) { this.runs_off_bat = value; this.wides = 0; this.no_balls = 0; this.byes = 0; this.leg_byes = 0; },
    setWide() { this.runs_off_bat = 0; this.wides = 1; this.no_balls = 0; this.byes = 0; this.leg_byes = 0; },
    setNoBall() { this.runs_off_bat = 0; this.wides = 0; this.no_balls = 1; this.byes = 0; this.leg_byes = 0; },
    setBye() { this.runs_off_bat = 0; this.wides = 0; this.no_balls = 0; this.byes = 1; this.leg_byes = 0; },
    setLegBye() { this.runs_off_bat = 0; this.wides = 0; this.no_balls = 0; this.byes = 0; this.leg_byes = 1; }
}">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div><p class="cricket-kicker mb-1">Live scorer</p><h1 class="h2 fw-bold mb-1">Match #{{ $match->id }}</h1><p class="text-secondary mb-0">{{ $innings?->battingTeam?->name }} batting · {{ $innings?->bowlingTeam?->name }} bowling · {{ $match->ruleProfile?->name }}</p></div>
        <a href="{{ route('admin.tournaments.matches.show', [$tournament, $match]) }}" class="btn btn-outline-primary">Back to match</a>
    </div>
    @if(session('status'))<div class="alert alert-success">{{ session('status') }}</div>@endif
    @if($errors->any())<div class="alert alert-danger"><ul class="mb-0">@foreach($errors->all() as $error)<li>{{ $error }}</li>@endforeach</ul></div>@endif
    @if($innings)
    <div class="row g-4">
        <div class="col-xl-8">
            <div class="cricket-surface p-4 mb-4">
                <div class="d-flex justify-content-between align-items-center"><div><span class="badge text-bg-success">INNINGS {{ $innings->innings_number }}</span><h2 class="display-5 fw-bold mt-2 mb-0">{{ $innings->total_runs }}/{{ $innings->wickets }}</h2><div class="text-secondary fs-5">{{ $innings->oversDisplay($match->ruleProfile->legal_balls_per_over) }} overs · {{ $innings->battingTeam?->short_name }}</div></div><div class="text-end"><div class="small text-secondary">Rule limit</div><strong>{{ $innings->maximum_overs }} overs</strong><div class="small text-secondary mt-2">Revision {{ $match->revision }}</div></div></div>
            </div>
            <div class="cricket-surface p-4 mb-4">
                <h2 class="h5 fw-bold mb-3">Record delivery</h2>
                <form method="POST" action="{{ route('admin.matches.scorer.deliveries.store', $match) }}">
                    @csrf
                    <input type="hidden" name="expected_revision" value="{{ $match->revision }}">
                    <div class="row g-3 mb-3"><div class="col-md-4"><label class="form-label">Striker</label><select name="striker_id" class="form-select" required>@foreach($batters as $player)<option value="{{ $player->id }}">{{ $player->player_name_snapshot }}</option>@endforeach</select></div><div class="col-md-4"><label class="form-label">Non-striker</label><select name="non_striker_id" class="form-select" required>@foreach($batters as $player)<option value="{{ $player->id }}">{{ $player->player_name_snapshot }}</option>@endforeach</select></div><div class="col-md-4"><label class="form-label">Bowler</label><select name="bowler_id" class="form-select" required>@foreach($bowlers as $player)<option value="{{ $player->id }}">{{ $player->player_name_snapshot }}</option>@endforeach</select></div></div>
                    <div class="small fw-bold text-secondary text-uppercase mb-2">Quick outcome</div>
                    <div class="d-flex flex-wrap gap-2 mb-3">@foreach([0,1,2,3,4,5,6] as $run)<button type="button" class="btn btn-outline-dark" @click="setRuns({{ $run }})">{{ $run }}</button>@endforeach<button type="button" class="btn btn-outline-warning" @click="setWide()">Wide</button><button type="button" class="btn btn-outline-warning" @click="setNoBall()">No-ball</button><button type="button" class="btn btn-outline-info" @click="setBye()">Bye</button><button type="button" class="btn btn-outline-info" @click="setLegBye()">Leg-bye</button></div>
                    <div class="row g-3 mb-3"><div class="col-md-2"><label class="form-label">Bat runs</label><input type="number" name="runs_off_bat" x-model.number="runs_off_bat" min="0" max="6" class="form-control"></div><div class="col-md-2"><label class="form-label">Wides</label><input type="number" name="wides" x-model.number="wides" min="0" max="6" class="form-control"></div><div class="col-md-2"><label class="form-label">No-balls</label><input type="number" name="no_balls" x-model.number="no_balls" min="0" max="6" class="form-control"></div><div class="col-md-2"><label class="form-label">Byes</label><input type="number" name="byes" x-model.number="byes" min="0" max="6" class="form-control"></div><div class="col-md-2"><label class="form-label">Leg-byes</label><input type="number" name="leg_byes" x-model.number="leg_byes" min="0" max="6" class="form-control"></div><div class="col-md-2"><label class="form-label">Penalty</label><input type="number" name="penalty_runs" x-model.number="penalty_runs" min="0" max="6" class="form-control"></div></div>
                    <div class="form-check form-switch mb-3"><input class="form-check-input" type="checkbox" id="wicketEnabled" x-model="wicketEnabled"><label class="form-check-label fw-bold" for="wicketEnabled">Wicket on this delivery</label></div>
                    <div class="row g-3 mb-3" x-show="wicketEnabled"><div class="col-md-4"><label class="form-label">Dismissed player</label><select name="wicket[dismissed_player_id]" class="form-select" :disabled="!wicketEnabled">@foreach($batters as $player)<option value="{{ $player->id }}">{{ $player->player_name_snapshot }}</option>@endforeach</select></div><div class="col-md-4"><label class="form-label">Dismissal</label><select name="wicket[dismissal_type]" class="form-select" :disabled="!wicketEnabled"><option value="bowled">Bowled</option><option value="caught">Caught</option><option value="lbw">LBW</option><option value="run_out">Run out</option><option value="stumped">Stumped</option><option value="hit_wicket">Hit wicket</option></select></div><div class="col-md-4"><label class="form-label">Fielder (optional)</label><select name="wicket[fielder_id]" class="form-select" :disabled="!wicketEnabled"><option value="">Not applicable</option>@foreach($bowlers as $player)<option value="{{ $player->id }}">{{ $player->player_name_snapshot }}</option>@endforeach</select></div></div>
                    <input name="commentary" class="form-control mb-3" placeholder="Optional commentary">
                    <button class="btn btn-primary" type="submit"><i class="fa-solid fa-check me-2"></i>Confirm delivery</button>
                </form>
            </div>
            <div class="cricket-surface p-4"><h2 class="h5 fw-bold mb-3">Recent balls</h2><div class="d-flex flex-wrap gap-2">@forelse($innings->deliveries->whereNull('voided_at')->sortByDesc('sequence_number')->take(12) as $delivery)<span class="badge rounded-pill text-bg-light border">{{ $delivery->over_number }}.{{ $delivery->ball_number }} · {{ $delivery->notation() }}</span>@empty<span class="text-secondary">No deliveries yet.</span>@endforelse</div></div>
        </div>
        <div class="col-xl-4"><div class="cricket-surface p-4 sticky-xl-top" style="top: 1rem"><h2 class="h5 fw-bold">Controller actions</h2>@if($innings->status === 'completed' && $innings->innings_number < ($match->ruleProfile->innings_per_side * 2))<form method="POST" action="{{ route('admin.matches.scorer.next-innings', $match) }}" class="mb-3">@csrf<button class="btn btn-success w-100" type="submit">Start next innings</button></form>@endif<p class="small text-secondary">Corrections void the latest event instead of deleting history. The cached scorecard is rebuilt from non-voided deliveries.</p><form method="POST" action="{{ route('admin.matches.scorer.undo', $match) }}">@csrf<input name="reason" class="form-control mb-2" placeholder="Reason for undo" required minlength="5"><button class="btn btn-outline-danger w-100" type="submit">Undo latest delivery</button></form><hr><h3 class="h6 fw-bold">Current innings</h3><dl class="row small mb-0"><dt class="col-7">Total</dt><dd class="col-5 text-end">{{ $innings->total_runs }}/{{ $innings->wickets }}</dd><dt class="col-7">Legal balls</dt><dd class="col-5 text-end">{{ $innings->legal_balls }}</dd><dt class="col-7">Status</dt><dd class="col-5 text-end">{{ ucfirst($innings->status) }}</dd></dl></div></div>
    </div>
    @else<div class="alert alert-warning">No active innings exists for this match.</div>@endif
</div>
@endsection
