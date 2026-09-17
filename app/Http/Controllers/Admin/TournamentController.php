<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Http\Requests\Admin\StoreTournamentRequest;
use App\Http\Requests\Admin\UpdateTournamentRequest;
use App\Models\AuditLog;
use App\Models\CricketRuleProfile;
use App\Models\Tournament;
use Illuminate\Database\DatabaseManager;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Arr;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\ValidationException;
use Illuminate\View\View;

class TournamentController extends Controller
{
    public function index(Request $request): View
    {
        return view('admin.tournaments.index', [
            'tournaments' => Tournament::query()
                ->where('creator_id', $request->user()->id)
                ->latest()
                ->paginate(12),
        ]);
    }

    public function create(): View
    {
        return view('admin.tournaments.create', [
            'ruleProfiles' => CricketRuleProfile::query()->where('is_active', true)->orderByDesc('is_system')->orderBy('name')->get(),
        ]);
    }

    public function store(StoreTournamentRequest $request): RedirectResponse
    {
        $validated = $request->validated();
        $attributes = Arr::only($validated, [
            'name', 'season_name', 'slug', 'description', 'location', 'venue', 'city', 'timezone',
            'starts_on', 'ends_on', 'registration_opens_at', 'registration_closes_at',
            'squad_size', 'default_pick_duration', 'cricket_rule_profile_id', 'default_overs_per_innings',
        ]);
        $attributes = $this->normalizeRegistrationWindow($attributes);
        $attributes['status'] = 'draft';
        $attributes['creator_id'] = $request->user()->id;
        $attributes['is_public'] = $request->boolean('is_public', true);
        $attributes['logo_path'] = $request->hasFile('logo') ? $request->file('logo')->store('tournaments', 'public') : null;
        $attributes['banner_path'] = $request->hasFile('banner') ? $request->file('banner')->store('tournaments', 'public') : null;

        $tournament = Tournament::create($attributes);

        return redirect()
            ->route('admin.tournaments.show', $tournament)
            ->with('status', 'Tournament created successfully.');
    }

    public function edit(Tournament $tournament): View
    {
        return view('admin.tournaments.edit', [
            'tournament' => $tournament,
            'ruleProfiles' => CricketRuleProfile::query()->where('is_active', true)->orderByDesc('is_system')->orderBy('name')->get(),
        ]);
    }

    public function update(UpdateTournamentRequest $request, Tournament $tournament): RedirectResponse
    {
        $validated = $request->validated();
        $attributes = Arr::only($validated, [
            'name', 'season_name', 'slug', 'description', 'location', 'venue', 'city', 'timezone',
            'starts_on', 'ends_on', 'registration_opens_at', 'registration_closes_at',
            'squad_size', 'default_pick_duration', 'cricket_rule_profile_id', 'default_overs_per_innings',
        ]);
        $attributes = $this->normalizeRegistrationWindow($attributes);
        $attributes['is_public'] = $request->boolean('is_public');

        if ($tournament->draft && $tournament->draft->status !== 'setup') {
            if (array_key_exists('squad_size', $attributes) && (int) $attributes['squad_size'] !== (int) $tournament->squad_size) {
                throw ValidationException::withMessages(['squad_size' => 'Squad size is locked after draft setup begins.']);
            }
            if (array_key_exists('cricket_rule_profile_id', $attributes) && (int) $attributes['cricket_rule_profile_id'] !== (int) $tournament->cricket_rule_profile_id) {
                throw ValidationException::withMessages(['cricket_rule_profile_id' => 'Match rules are locked after draft setup begins.']);
            }
        }

        $before = $tournament->only([
            'name', 'season_name', 'slug', 'description', 'location', 'venue', 'city', 'timezone',
            'starts_on', 'ends_on', 'registration_opens_at', 'registration_closes_at', 'is_public',
            'logo_path', 'banner_path', 'squad_size',             'default_pick_duration', 'cricket_rule_profile_id', 'default_overs_per_innings',
        ]);

        if ($request->boolean('remove_logo') && $tournament->logo_path) {
            Storage::disk('public')->delete($tournament->logo_path);
            $attributes['logo_path'] = null;
        }
        if ($request->boolean('remove_banner') && $tournament->banner_path) {
            Storage::disk('public')->delete($tournament->banner_path);
            $attributes['banner_path'] = null;
        }
        if ($request->hasFile('logo')) {
            if ($tournament->logo_path) {
                Storage::disk('public')->delete($tournament->logo_path);
            }
            $attributes['logo_path'] = $request->file('logo')->store('tournaments', 'public');
        }
        if ($request->hasFile('banner')) {
            if ($tournament->banner_path) {
                Storage::disk('public')->delete($tournament->banner_path);
            }
            $attributes['banner_path'] = $request->file('banner')->store('tournaments', 'public');
        }

        $tournament->update($attributes);
        $after = $tournament->fresh()->only(array_keys($before));

        AuditLog::create([
            'user_id' => $request->user()?->id,
            'tournament_id' => $tournament->id,
            'action' => 'tournament.configuration_updated',
            'before' => $before,
            'after' => $after,
            'metadata' => ['changed_fields' => array_keys(array_diff_assoc($after, $before))],
            'ip_address' => $request->ip(),
            'user_agent' => $request->userAgent(),
        ]);

        return redirect()
            ->route('admin.tournaments.show', $tournament)
            ->with('status', 'Tournament configuration updated successfully.');
    }

    public function show(Tournament $tournament): View
    {
        return view('admin.tournaments.show', [
            'tournament' => $tournament->load(['draft', 'cricketRuleProfile']),
            'statusTransitions' => $this->statusTransitions($tournament->status),
            'statusOptions' => $this->statusOptions(),
        ]);
    }

    public function transition(Request $request, Tournament $tournament, DatabaseManager $database): RedirectResponse
    {
        $validated = $request->validate([
            'status' => ['required', 'string', 'in:draft,registration,ready,live,completed,cancelled'],
        ]);

        $from = $tournament->status;
        $to = $validated['status'];
        $allowed = $this->statusTransitions($from);

        if ($to !== $from && ! in_array($to, $allowed, true)) {
            throw ValidationException::withMessages([
                'status' => "The selected tournament status is not supported: {$to}.",
            ]);
        }

        $database->transaction(function () use ($tournament, $from, $to, $request): void {
            $before = $tournament->only(['status', 'published_at']);
            $tournament->update([
                'status' => $to,
                'published_at' => $to === 'live' ? ($tournament->published_at ?: now()) : $tournament->published_at,
            ]);

            AuditLog::create([
                'user_id' => $request->user()?->id,
                'tournament_id' => $tournament->id,
                'action' => 'tournament.status_changed',
                'before' => $before,
                'after' => $tournament->fresh()->only(['status', 'published_at']),
                'metadata' => ['from' => $from, 'to' => $to],
                'ip_address' => $request->ip(),
                'user_agent' => $request->userAgent(),
            ]);
        });

        return redirect()
            ->route('admin.tournaments.show', $tournament)
            ->with('status', "Tournament status changed to {$to}.");
    }

    private function normalizeRegistrationWindow(array $attributes): array
    {
        $timezone = $attributes['timezone'] ?? config('app.timezone', 'UTC');

        foreach (['registration_opens_at', 'registration_closes_at'] as $field) {
            if (! empty($attributes[$field])) {
                $attributes[$field] = Carbon::parse($attributes[$field], $timezone)->utc();
            }
        }

        return $attributes;
    }

    private function statusOptions(): array
    {
        return [
            'draft' => 'Draft',
            'registration' => 'Registration open',
            'ready' => 'Ready for draft',
            'live' => 'Live / draft started',
            'completed' => 'Completed',
            'cancelled' => 'Cancelled',
        ];
    }

    private function statusTransitions(string $status): array
    {
        return array_values(array_diff(array_keys($this->statusOptions()), [$status]));
    }

    public function destroy(Tournament $tournament): RedirectResponse
    {
        $tournament->delete();

        return redirect()
            ->route('admin.tournaments.index')
            ->with('status', 'Tournament deleted successfully.');
    }
}
