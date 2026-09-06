<!doctype html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <title>{{ $title ?? config('app.name', 'Cricket Draft OS') }}</title>
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
<body class="public-shell">
    @php
        $workspace = route('login');
        if (auth()->check()) {
            $user = auth()->user();
            $workspace = match (true) {
                $user->hasRole('super_admin') => route('super-admin.dashboard'),
                $user->hasRole('admin') => route('admin.dashboard'),
                $user->hasRole('captain') => route('captain.dashboard'),
                $user->hasRole('player') => route('player.tournaments.index'),
                default => route('dashboard'),
            };
        }
    @endphp

    <nav class="navbar navbar-expand-lg cricket-topbar sticky-top">
        <div class="container py-2 py-lg-3">
            <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="{{ route('home') }}">
                <span class="cricket-brand-mark"><i class="fa-solid fa-baseball"></i></span>
                <span>Cricket Draft <span class="text-success">OS</span></span>
            </a>
            <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#publicNav" aria-controls="publicNav" aria-expanded="false" aria-label="Toggle navigation">
                <i class="fa-solid fa-bars"></i>
            </button>
            <div class="collapse navbar-collapse" id="publicNav">
                <div class="navbar-nav mx-lg-auto align-items-lg-center gap-lg-1 mt-3 mt-lg-0">
                    <a class="nav-link px-lg-3 {{ request()->routeIs('home') ? 'active' : '' }}" href="{{ route('home') }}">Platform</a>
                    <a class="nav-link px-lg-3 {{ request()->routeIs('public.features') ? 'active' : '' }}" href="{{ route('public.features') }}">Features</a>
                    <a class="nav-link px-lg-3 {{ request()->routeIs('public.how-it-works') ? 'active' : '' }}" href="{{ route('public.how-it-works') }}">How it works</a>
                    <a class="nav-link px-lg-3 {{ request()->routeIs('public.tournaments.*') ? 'active' : '' }}" href="{{ route('public.tournaments.index') }}">Tournaments</a>
                    <a class="nav-link px-lg-3 {{ request()->routeIs('public.live.center') || request()->routeIs('public.draft.*') ? 'active' : '' }}" href="{{ route('public.live.center') }}">Live drafts</a>
                </div>
                <div class="d-flex flex-column flex-lg-row align-items-stretch align-items-lg-center gap-2 mt-3 mt-lg-0">
                    @if (auth()->check())
                        <a href="{{ $workspace }}" class="btn btn-success btn-sm">Open workspace <i class="fa-solid fa-arrow-up-right-from-square ms-1"></i></a>
                    @else
                        <a href="{{ route('login') }}" class="btn btn-light btn-sm">Log in</a>
                        <a href="{{ route('register') }}" class="btn btn-success btn-sm">Join the platform <i class="fa-solid fa-arrow-right ms-1"></i></a>
                    @endif
                </div>
            </div>
        </div>
    </nav>

    <main>{{ $slot }}</main>

    <footer class="public-footer">
        <div class="container py-5">
            <div class="row g-4 justify-content-between">
                <div class="col-lg-5">
                    <a class="d-inline-flex align-items-center gap-2 fw-bold text-decoration-none text-dark mb-3" href="{{ route('home') }}">
                        <span class="cricket-brand-mark"><i class="fa-solid fa-baseball"></i></span>
                        <span>Cricket Draft <span class="text-success">OS</span></span>
                    </a>
                    <p class="text-secondary mb-0" style="max-width: 28rem;">The operating system for tournament drafts, live cricket scoring, fixtures, standings, and mobile-ready match data.</p>
                </div>
                <div class="col-6 col-lg-2">
                    <div class="small fw-bold text-uppercase text-secondary mb-3" style="letter-spacing:.11em;">Product</div>
                    <div class="vstack gap-2 small"><a href="{{ route('public.features') }}">Features</a><a href="{{ route('public.how-it-works') }}">How it works</a><a href="{{ route('public.tournaments.index') }}">Tournaments</a><a href="{{ route('public.live.center') }}">Live drafts</a></div>
                </div>
                <div class="col-6 col-lg-2">
                    <div class="small fw-bold text-uppercase text-secondary mb-3" style="letter-spacing:.11em;">Explore</div>
                    <div class="vstack gap-2 small"><a href="{{ route('login') }}">Log in</a><a href="{{ route('register') }}">Player registration</a><a href="{{ route('home') }}#match-coverage">Match coverage</a></div>
                </div>
            </div>
            <div class="border-top mt-5 pt-4 d-flex flex-column flex-md-row justify-content-between gap-2 small text-secondary"><span>© {{ now()->year }} Cricket Draft OS</span><span>Authoritative data · Responsive by default · Built for cricket operations</span></div>
        </div>
    </footer>
</body>
</html>
