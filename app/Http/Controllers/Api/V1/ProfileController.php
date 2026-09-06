<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\PlayerProfile;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ProfileController extends Controller
{
    public function show(Request $request): JsonResponse
    {
        return response()->json(['data' => $this->payload($request->user()->playerProfile)]);
    }

    public function update(Request $request): JsonResponse
    {
        $data = $request->validate([
            'full_name' => ['required', 'string', 'max:150'],
            'phone' => ['nullable', 'string', 'max:30'],
            'city' => ['nullable', 'string', 'max:100'],
            'playing_role' => ['required', 'string', 'in:Batter,Bowler,All-rounder,Wicketkeeper'],
            'batting_style' => ['nullable', 'string', 'max:100'],
            'bowling_style' => ['nullable', 'string', 'max:100'],
            'bio' => ['nullable', 'string', 'max:2000'],
        ]);
        $profile = PlayerProfile::updateOrCreate(['user_id' => $request->user()->id], [...$data, 'is_active' => true]);
        return response()->json(['data' => $this->payload($profile), 'message' => 'Player profile saved successfully.']);
    }

    private function payload(?PlayerProfile $profile): ?array
    {
        if (! $profile) return null;
        return ['id' => $profile->id, 'full_name' => $profile->full_name, 'phone' => $profile->phone, 'city' => $profile->city, 'playing_role' => $profile->playing_role, 'batting_style' => $profile->batting_style, 'bowling_style' => $profile->bowling_style, 'bio' => $profile->bio, 'is_active' => $profile->is_active];
    }

    public function stats(Request $request, PlayerProfile $playerProfile, \App\Modules\Analytics\Services\PlayerProfileStatsService $statsService): JsonResponse
    {
        $filters = $request->validate([
            'year' => ['nullable', 'integer', 'min:2000', 'max:2099'],
            'format' => ['nullable', 'string', 'max:50'],
            'ball_type' => ['nullable', 'string', 'in:leather,tennis'],
            'data_source' => ['nullable', 'string', 'in:verified,manual'],
        ]);

        return response()->json([
            'data' => $statsService->getPlayerStats($playerProfile, $filters)
        ]);
    }

    public function insights(PlayerProfile $playerProfile, \App\Modules\Analytics\Services\PlayerProfileStatsService $statsService): JsonResponse
    {
        return response()->json([
            'data' => $statsService->getPlayerInsights($playerProfile)
        ]);
    }

    public function matches(PlayerProfile $playerProfile): JsonResponse
    {
        // match_players has tournament_player_id, NOT player_profile_id
        // So we join through tournament_players to find matches for this player
        $tournamentPlayerIds = \App\Models\TournamentPlayer::query()
            ->where('player_profile_id', $playerProfile->id)
            ->pluck('id');

        $matches = \App\Models\MatchPlayer::query()
            ->whereIn('tournament_player_id', $tournamentPlayerIds)
            ->with(['match.tournament', 'match.fixture.homeTeam', 'match.fixture.awayTeam', 'inningsBattingStats', 'inningsBowlingStats'])
            ->latest()
            ->get()
            ->map(fn ($mp) => [
                'id' => $mp->match?->id,
                'tournament_name' => $mp->match?->tournament?->name,
                'opponent' => $mp->match?->fixture
                    ? ($mp->team_id === $mp->match->fixture->home_team_id
                        ? $mp->match->fixture->awayTeam?->name
                        : $mp->match->fixture->homeTeam?->name)
                    : 'TBD',
                'venue' => $mp->match?->fixture?->venue,
                'date' => $mp->match?->fixture?->scheduled_at?->toDateString(),
                'runs' => $mp->inningsBattingStats->sum('runs'),
                'wickets' => $mp->inningsBowlingStats->sum('wickets'),
                'overs_bowled' => \round($mp->inningsBowlingStats->sum('legal_balls') / 6, 1),
                'result' => $mp->match?->result_summary,
                'match_type' => $mp->match?->tournament?->cricketRuleProfile?->format,
            ]);

        return response()->json(['data' => $matches]);
    }

    public function teams(PlayerProfile $playerProfile): JsonResponse
    {
        $teams = \App\Models\TournamentPlayer::query()
            ->where('player_profile_id', $playerProfile->id)
            ->where('status', 'approved')
            ->with(['team', 'tournament'])
            ->get()
            ->map(fn ($tp) => [
                'team_id' => $tp->team_id,
                'team_name' => $tp->team?->name,
                'tournament_name' => $tp->tournament?->name,
                'role' => $tp->playing_role,
                'is_captain' => $tp->is_captain ?? false,
                'matches_played' => \App\Models\MatchPlayer::query()
                    ->where('player_profile_id', $playerProfile->id)
                    ->where('team_id', $tp->team_id)
                    ->count(),
            ]);

        return response()->json(['data' => $teams]);
    }
}
