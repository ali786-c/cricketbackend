<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use App\Models\MatchPlayer;
use App\Models\PlayerProfile;
use App\Models\Tournament;
use App\Modules\Scoring\Services\MatchService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AdminMatchController extends Controller
{
    public function __construct(private readonly MatchService $matches)
    {
    }

    public function index(Tournament $tournament): JsonResponse
    {
        return response()->json(['data' => $tournament->matches()->with(['fixture', 'ruleProfile', 'tossWinner', 'players.team'])->latest()->paginate(20)]);
    }

    public function store(Request $request, Tournament $tournament): JsonResponse
    {
        $this->authorizeCreator($tournament, $request);
        $data = $request->validate(['home_team_id' => ['required', 'integer', 'different:away_team_id'], 'away_team_id' => ['required', 'integer', 'different:home_team_id'], 'fixture_id' => ['nullable', 'integer'], 'overs_per_innings' => ['nullable', 'integer', 'min:1', 'max:100']]);
        $match = $this->matches->createFromTeams($tournament, (int) $data['home_team_id'], (int) $data['away_team_id'], isset($data['fixture_id']) ? (int) $data['fixture_id'] : null, (int) $request->user()->id, isset($data['overs_per_innings']) ? (int) $data['overs_per_innings'] : null);
        return response()->json(['data' => $match, 'message' => 'Operational match created from the completed draft squad.'], 201);
    }

    public function show(Request $request, Tournament $tournament, CricketMatch $match): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        return response()->json(['data' => $match->load(['fixture', 'ruleProfile', 'players.team', 'innings'])]);
    }

    public function updateOvers(Request $request, Tournament $tournament, CricketMatch $match): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        $data = $request->validate(['overs_per_innings' => ['required', 'integer', 'min:1', 'max:100']]);
        return response()->json(['data' => $this->matches->updateOversPerInnings($match, (int) $data['overs_per_innings'], (int) $request->user()->id), 'message' => 'Match overs updated successfully.']);
    }

    public function customPlayingXi(Request $request, CricketMatch $match, int $team): JsonResponse
    {
        $this->authorizeCustomMatch($match, $request);
        $data = $request->validate([
            'player_ids' => ['required', 'array'], 
            'player_ids.*' => ['integer'],
            'new_players' => ['nullable', 'array'],
            'new_players.*.name' => ['required', 'string', 'max:100'],
            'new_players.*.role' => ['nullable', 'string', 'max:50']
        ]);

        $playerIds = $data['player_ids'];

        if (!empty($data['new_players'])) {
            foreach ($data['new_players'] as $newPlayer) {
                $profile = \App\Models\PlayerProfile::create([
                    'full_name' => $newPlayer['name'],
                    'playing_role' => $newPlayer['role'] ?? null,
                    'is_guest' => true,
                    'is_active' => true,
                ]);
                $playerIds[] = $profile->id;
            }
        }

        // Custom and no-draft tournament clients submit PlayerProfile IDs because
        // no MatchPlayer squad exists until lineup selection. Draft matches keep
        // submitting the pre-created MatchPlayer IDs.
        if ($match->tournament_id === null || ! $match->tournament?->has_draft) {
            $profiles = PlayerProfile::query()->whereIn('id', $playerIds)->get()->keyBy('id');
            abort_unless($profiles->count() === count(array_unique($playerIds)), 422, 'Every selected player profile must exist.');

            if ($match->tournament_id !== null) {
                $approvedProfileIds = $match->tournament->tournamentPlayers()
                    ->where('status', 'approved')
                    ->whereIn('player_profile_id', $playerIds)
                    ->pluck('player_profile_id');
                abort_unless($approvedProfileIds->count() === count(array_unique($playerIds)), 422, 'Every no-draft player must be approved for this tournament.');
            }

            $playerIds = collect($playerIds)->unique()->map(function (int $profileId) use ($match, $team, $profiles) {
                $profile = $profiles->get($profileId);
                $registration = $match->tournament_id === null ? null : $match->tournament
                    ->tournamentPlayers()->where('player_profile_id', $profileId)->first();
                return MatchPlayer::query()->firstOrCreate(
                    ['match_id' => $match->id, 'player_profile_id' => $profileId],
                    [
                        'team_id' => $team,
                        'tournament_player_id' => $registration?->id,
                        'player_name_snapshot' => $profile->full_name,
                        'player_role_snapshot' => $profile->playing_role,
                        'selection_type' => 'squad',
                    ],
                )->id;
            })->all();
        }

        return response()->json(['data' => $this->matches->submitPlayingXi($match, $team, $playerIds, (int) $request->user()->id)]);
    }

    public function playingXi(Request $request, Tournament $tournament, CricketMatch $match, int $team): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        return $this->customPlayingXi($request, $match, $team);
    }

    public function customApproveLineup(Request $request, CricketMatch $match): JsonResponse
    {
        $this->authorizeCustomMatch($match, $request);
        return response()->json(['data' => $this->matches->approveLineup($match, (int) $request->user()->id)]);
    }

    public function approveLineup(Request $request, Tournament $tournament, CricketMatch $match): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        return $this->customApproveLineup($request, $match);
    }

    public function customToss(Request $request, CricketMatch $match): JsonResponse
    {
        $this->authorizeCustomMatch($match, $request);
        $data = $request->validate(['toss_winner_team_id' => ['required', 'integer'], 'toss_decision' => ['required', 'in:bat,field']]);
        return response()->json(['data' => $this->matches->recordToss($match, (int) $data['toss_winner_team_id'], $data['toss_decision'], (int) $request->user()->id)]);
    }

    public function toss(Request $request, Tournament $tournament, CricketMatch $match): JsonResponse
    {
        $this->belongs($tournament, $match, $request);
        return $this->customToss($request, $match);
    }

    private function belongs(Tournament $tournament, CricketMatch $match, Request $request): void
    {
        $this->authorizeCreator($tournament, $request);
        abort_unless($match->tournament_id === $tournament->id, 404);
    }

    private function authorizeCreator(Tournament $tournament, Request $request): void
    {
        abort_if($tournament->creator_id !== $request->user()->id, 403, 'You can only manage matches for tournaments you created.');
    }

    private function authorizeCustomMatch(CricketMatch $match, Request $request): void
    {
        if ($match->tournament_id !== null) return;
        abort_if((int) $match->created_by !== (int) $request->user()->id && ! $request->user()->hasRole('super_admin'), 403, 'You can only manage custom matches you created.');
    }
}
