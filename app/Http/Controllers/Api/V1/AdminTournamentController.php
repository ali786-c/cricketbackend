<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\Tournament;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Arr;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

class AdminTournamentController extends Controller
{
    public function index(): JsonResponse
    {
        return response()->json(['data' => Tournament::withCount(['teams', 'tournamentPlayers', 'fixtures', 'matches'])->latest()->paginate(20)]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $this->validated($request, true);
        $data['is_public'] = $request->boolean('is_public', true);
        $data['has_draft'] = $request->boolean('has_draft', false);
        $data['ball_type'] = $request->input('ball_type', 'leather');
        $data['data_source'] = $request->input('data_source', 'verified');
        $data['organizer_name'] = $request->input('organizer_name');
        $data['contact_info'] = $request->input('contact_info');
        $data['competition_structure'] = $request->input('competition_structure', 'League');
        $data['tournament_code'] = $request->input('tournament_code');
        $data['logo_path'] = $request->hasFile('logo') ? $request->file('logo')->store('tournaments', 'public') : null;
        $data['banner_path'] = $request->hasFile('banner') ? $request->file('banner')->store('tournaments', 'public') : null;
        $tournament = Tournament::create(array_merge($data, ['status' => 'draft']));
        return response()->json(['data' => $tournament, 'message' => 'Tournament created successfully.'], 201);
    }

    public function show(Tournament $tournament): JsonResponse
    {
        return response()->json(['data' => $tournament->load(['draft', 'cricketRuleProfile'])->loadCount(['teams', 'tournamentPlayers', 'fixtures', 'matches'])]);
    }

    public function update(Request $request, Tournament $tournament): JsonResponse
    {
        $data = $this->validated($request, false);
        if ($tournament->draft && $tournament->draft->status !== 'setup') {
            foreach (['squad_size', 'cricket_rule_profile_id', 'default_overs_per_innings'] as $field) {
                if (array_key_exists($field, $data) && (int) $data[$field] !== (int) $tournament->{$field}) {
                    throw ValidationException::withMessages([$field => 'This field is locked after draft setup begins.']);
                }
            }
        }
        $before = $tournament->only(array_keys($data));
        if ($request->boolean('remove_logo') && $tournament->logo_path) {
            Storage::disk('public')->delete($tournament->logo_path);
            $data['logo_path'] = null;
        }
        if ($request->boolean('remove_banner') && $tournament->banner_path) {
            Storage::disk('public')->delete($tournament->banner_path);
            $data['banner_path'] = null;
        }
        if ($request->hasFile('logo')) {
            if ($tournament->logo_path) Storage::disk('public')->delete($tournament->logo_path);
            $data['logo_path'] = $request->file('logo')->store('tournaments', 'public');
        }
        if ($request->hasFile('banner')) {
            if ($tournament->banner_path) Storage::disk('public')->delete($tournament->banner_path);
            $data['banner_path'] = $request->file('banner')->store('tournaments', 'public');
        }
        $data['organizer_name'] = $request->input('organizer_name', $tournament->organizer_name);
        $data['contact_info'] = $request->input('contact_info', $tournament->contact_info);
        $data['competition_structure'] = $request->input('competition_structure', $tournament->competition_structure);
        $data['tournament_code'] = $request->input('tournament_code', $tournament->tournament_code);

        $tournament->update(array_merge($data, [
            'is_public' => $request->boolean('is_public', $tournament->is_public),
            'has_draft' => $request->boolean('has_draft', $tournament->has_draft),
            'ball_type' => $request->input('ball_type', $tournament->ball_type),
            'data_source' => $request->input('data_source', $tournament->data_source),
        ]));
        AuditLog::create(['user_id' => $request->user()->id, 'tournament_id' => $tournament->id, 'action' => 'tournament.configuration_updated', 'before' => $before, 'after' => $tournament->fresh()->only(array_keys($before)), 'metadata' => ['source' => 'api'], 'ip_address' => $request->ip(), 'user_agent' => $request->userAgent()]);
        return response()->json(['data' => $tournament->fresh(), 'message' => 'Tournament updated successfully.']);
    }

    public function status(Request $request, Tournament $tournament): JsonResponse
    {
        $data = $request->validate(['status' => ['required', 'in:draft,registration,ready,live,completed,cancelled']]);
        $from = $tournament->status;
        $to = $data['status'];
        if ($to !== $from && ! in_array($to, $this->transitions($from), true)) {
            throw ValidationException::withMessages(['status' => "The selected tournament status is not supported: {$to}."]);
        }
        $tournament->update(['status' => $to, 'published_at' => $to === 'live' ? ($tournament->published_at ?: now()) : $tournament->published_at]);
        AuditLog::create(['user_id' => $request->user()->id, 'tournament_id' => $tournament->id, 'action' => 'tournament.status_changed', 'before' => ['status' => $from], 'after' => ['status' => $to], 'metadata' => ['source' => 'api', 'from' => $from, 'to' => $to], 'ip_address' => $request->ip(), 'user_agent' => $request->userAgent()]);
        return response()->json(['data' => $tournament->fresh(), 'message' => "Tournament status changed to {$to}."]);
    }

    private function validated(Request $request, bool $creating): array
    {
        $required = $creating ? 'required' : 'sometimes';
        $rules = [
            'name' => [$required, 'string', 'max:150'],
            'season_name' => ['nullable', 'string', 'max:100'],
            'slug' => [$required, 'string', 'max:180', 'alpha_dash', Rule::unique('tournaments', 'slug')->ignore($request->route('tournament')?->id)],
            'description' => ['nullable', 'string', 'max:5000'],
            'location' => ['nullable', 'string', 'max:150'],
            'venue' => ['nullable', 'string', 'max:150'],
            'city' => ['nullable', 'string', 'max:100'],
            'timezone' => [$creating ? 'required' : 'sometimes', 'timezone'],
            'starts_on' => ['nullable', 'date'],
            'ends_on' => ['nullable', 'date', 'after_or_equal:starts_on'],
            'registration_opens_at' => ['nullable', 'date'],
            'registration_closes_at' => ['nullable', 'date', 'after_or_equal:registration_opens_at'],
            'squad_size' => [$creating ? 'required' : 'sometimes', 'integer', 'min:1', 'max:99'],
            'has_draft' => ['sometimes', 'boolean'],
            'ball_type' => ['sometimes', 'string', 'in:leather,tennis,hard_ball,tape_ball,indoor'],
            'data_source' => ['sometimes', 'string', 'in:verified,manual'],
            'default_pick_duration' => [$creating ? 'required' : 'sometimes', 'integer', 'min:5', 'max:3600'],
            'cricket_rule_profile_id' => ['nullable', 'integer', 'exists:cricket_rule_profiles,id'],
            'default_overs_per_innings' => ['nullable', 'integer', 'min:1', 'max:100'],
            'is_public' => ['sometimes', 'boolean'],
            'organizer_name' => ['nullable', 'string', 'max:200'],
            'contact_info' => ['nullable', 'string', 'max:500'],
            'competition_structure' => ['nullable', 'string', 'in:League,Knockout,Group+Playoffs,Custom'],
            'tournament_code' => ['nullable', 'string', 'max:50', 'unique:tournaments,tournament_code'],
            'logo' => ['nullable', 'image', 'mimes:jpg,jpeg,png,webp', 'max:5120'],
            'banner' => ['nullable', 'image', 'mimes:jpg,jpeg,png,webp', 'max:10240'],
            'remove_logo' => ['sometimes', 'boolean'],
            'remove_banner' => ['sometimes', 'boolean']
        ];
        return Arr::only($request->validate($rules), array_keys($rules));
    }

    private function transitions(string $status): array
    {
        return array_values(array_diff(['draft', 'registration', 'ready', 'live', 'completed', 'cancelled'], [$status]));
    }
}
