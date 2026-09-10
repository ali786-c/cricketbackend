<?php

namespace Tests\Feature\Api\V1;

use App\Models\CricketRuleProfile;
use App\Models\Fixture;
use App\Models\PlayerProfile;
use App\Models\Team;
use App\Models\Tournament;
use App\Models\TournamentPlayer;
use App\Models\User;
use App\Modules\Scoring\Services\MatchService;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MatchConfigurationFoundationTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_no_draft_tournament_uses_immutable_profile_and_accepts_approved_explicit_lineups(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $profile = CricketRuleProfile::create([
            'name' => 'Six-a-side Tournament',
            'slug' => 'six-a-side-'.uniqid(),
            'format' => 'limited_overs',
            'innings_per_side' => 1,
            'overs_per_innings' => 6,
            'playing_xi_size' => 2,
            'maximum_wickets' => 1,
            'legal_balls_per_over' => 6,
            'max_overs_per_bowler' => 2,
            'version' => 3,
            'is_active' => true,
        ]);
        $tournament = Tournament::create([
            'name' => 'No Draft Cup',
            'slug' => 'no-draft-cup-'.uniqid(),
            'timezone' => 'Asia/Karachi',
            'status' => 'ready',
            'is_public' => false,
            'has_draft' => false,
            'squad_size' => 4,
            'default_pick_duration' => 60,
            'default_overs_per_innings' => 6,
            'ball_type' => 'tennis',
            'cricket_rule_profile_id' => $profile->id,
            'creator_id' => $admin->id,
        ]);
        $home = Team::create(['name' => 'No Draft Home', 'short_name' => 'NDH', 'is_active' => true, 'creator_id' => $admin->id]);
        $away = Team::create(['name' => 'No Draft Away', 'short_name' => 'NDA', 'is_active' => true, 'creator_id' => $admin->id]);
        $tournament->teams()->attach([$home->id, $away->id]);
        $fixture = Fixture::create([
            'tournament_id' => $tournament->id,
            'home_team_id' => $home->id,
            'away_team_id' => $away->id,
            'scheduled_at' => now(),
            'timezone' => 'Asia/Karachi',
            'status' => 'scheduled',
            'created_by' => $admin->id,
        ]);

        $match = app(MatchService::class)->createFromTeams(
            $tournament,
            $home->id,
            $away->id,
            $fixture->id,
            $admin->id,
        );

        $this->assertSame(3, $match->rule_profile_version);
        $this->assertSame(2, $match->rule_snapshot['playing_xi_size']);
        $this->assertSame(1, $match->rule_snapshot['maximum_wickets']);
        $this->assertSame('tennis', $match->rule_snapshot['ball_type']);

        $profilesByTeam = [];
        foreach ([$home, $away] as $team) {
            $profilesByTeam[$team->id] = collect(range(1, 2))->map(function (int $number) use ($tournament, $team) {
                $player = PlayerProfile::create([
                    'full_name' => $team->short_name.' Player '.$number,
                    'playing_role' => $number === 1 ? 'Batter' : 'Bowler',
                    'is_guest' => true,
                    'is_active' => true,
                ]);
                TournamentPlayer::create([
                    'tournament_id' => $tournament->id,
                    'player_profile_id' => $player->id,
                    'status' => 'approved',
                ]);
                return $player->id;
            })->all();
        }

        $headers = ['Authorization' => 'Bearer '.$admin->createToken('no-draft-match')->plainTextToken];
        foreach ([$home, $away] as $team) {
            $this->postJson(
                "/api/v1/admin/tournaments/{$tournament->slug}/matches/{$match->id}/teams/{$team->id}/playing-xi",
                ['player_ids' => $profilesByTeam[$team->id]],
                $headers,
            )->assertOk();
        }
        $this->postJson(
            "/api/v1/admin/tournaments/{$tournament->slug}/matches/{$match->id}/approve-lineup",
            [],
            $headers,
        )->assertOk()->assertJsonPath('data.status', 'toss_pending');

        // Later profile edits cannot mutate this match's saved rule snapshot.
        $profile->update(['maximum_wickets' => 2, 'version' => 4]);
        $this->assertSame(1, $match->fresh()->rule_snapshot['maximum_wickets']);
        $this->assertSame(3, $match->fresh()->rule_profile_version);
    }
}
