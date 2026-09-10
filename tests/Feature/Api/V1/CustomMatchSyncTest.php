<?php

namespace Tests\Feature\Api\V1;

use App\Models\CricketMatch;
use App\Models\PlayerProfile;
use App\Models\Fixture;
use App\Models\MatchInnings;
use App\Models\Team;
use App\Models\User;
use Database\Seeders\CricketRuleProfileSeeder;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CustomMatchSyncTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
        $this->seed(CricketRuleProfileSeeder::class);
    }

    private function authenticatedUser(string $role = 'admin'): array
    {
        $user = User::factory()->create();
        $user->assignRole($role);
        $token = $user->createToken('android-test')->plainTextToken;

        return [$user, $token];
    }

    public function test_custom_team_creation_creates_global_team_visible_in_superadmin(): void
    {
        [$user, $token] = $this->authenticatedUser();

        $this->postJson('/api/v1/custom/teams', ['name' => 'Ali Panthers', 'short_name' => 'ALP'], [
            'Authorization' => 'Bearer '.$token,
        ])->assertCreated()->assertJsonPath('data.name', 'Ali Panthers');

        $team = Team::query()->where('name', 'Ali Panthers')->firstOrFail();
        $this->assertSame($user->id, (int) $team->creator_id);
        $this->assertTrue((bool) $team->is_active);
        $this->assertSame(0, $team->tournaments()->count()); // global — not tied to a tournament

        // SuperAdmin Teams tab lists ALL teams (paginate keeps it visible)
        $superAdmin = User::factory()->create();
        $superAdmin->assignRole('super_admin');
        $this->actingAs($superAdmin)->get(route('super-admin.teams.index'))
            ->assertOk()
            ->assertSee('Ali Panthers');
    }

    public function test_custom_team_creation_is_idempotent_by_name(): void
    {
        [, $token] = $this->authenticatedUser();

        $this->postJson('/api/v1/custom/teams', ['name' => 'Lahore Lions'], ['Authorization' => 'Bearer '.$token])->assertCreated();
        $this->postJson('/api/v1/custom/teams', ['name' => 'Lahore Lions'], ['Authorization' => 'Bearer '.$token])->assertCreated();

        $this->assertSame(1, Team::query()->where('name', 'Lahore Lions')->count());
    }

    public function test_custom_fixture_auto_creates_missing_teams_from_names(): void
    {
        [, $token] = $this->authenticatedUser();

        $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Rawalpindi Kings',
            'away_team_name' => 'Islamabad Blasters',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
            'venue' => 'Main Ground',
        ], ['Authorization' => 'Bearer '.$token])->assertCreated();

        $this->assertDatabaseHas('teams', ['name' => 'Rawalpindi Kings']);
        $this->assertDatabaseHas('teams', ['name' => 'Islamabad Blasters']);

        $fixture = Fixture::query()->latest('id')->firstOrFail();
        $this->assertNull($fixture->tournament_id); // custom/standalone
        $this->assertNotNull($fixture->home_team_id);
        $this->assertNotNull($fixture->away_team_id);
        $this->assertSame('scheduled', $fixture->status);
    }

    public function test_custom_fixture_appears_in_superadmin_fixtures_tab(): void
    {
        [, $token] = $this->authenticatedUser();

        $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Karachi Kings XI',
            'away_team_name' => 'Multan Warriors',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
        ], ['Authorization' => 'Bearer '.$token])->assertCreated();

        $superAdmin = User::factory()->create();
        $superAdmin->assignRole('super_admin');
        $this->actingAs($superAdmin)->get(route('super-admin.fixtures.index'))
            ->assertOk()
            ->assertSee('Custom Match')
            ->assertSee('Karachi Kings XI')
            ->assertSee('Multan Warriors');
    }

    public function test_superadmin_fixtures_tab_is_rejected_for_regular_admin(): void
    {
        [$user] = $this->authenticatedUser('admin');
        $this->actingAs($user)->get(route('super-admin.fixtures.index'))->assertForbidden();
    }

    public function test_operational_match_is_created_from_custom_fixture(): void
    {
        [, $token] = $this->authenticatedUser();

        $create = $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Peshawar Zalzala',
            'away_team_name' => 'Quetta Gladiators B',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
        ], ['Authorization' => 'Bearer '.$token])->assertCreated();

        $fixtureId = $create->json('data.id');

        $match =        $this->postJson("/api/v1/custom/fixtures/{$fixtureId}/create-match", [], [
            'Authorization' => 'Bearer '.$token,
        ])->assertCreated();

        $matchId = $match->json('data.match_id');
        $this->assertDatabaseHas('matches', ['id' => $matchId, 'fixture_id' => $fixtureId]);
        $this->assertNull(CricketMatch::find($matchId)->tournament_id);

        // Duplicate creation returns the original match (idempotent retry safety).
        $duplicate = $this->postJson("/api/v1/custom/fixtures/{$fixtureId}/create-match", [], [
            'Authorization' => 'Bearer '.$token,
        ])->assertCreated();
        $this->assertSame($matchId, $duplicate->json('data.match_id'));
        $this->assertSame(1, CricketMatch::query()->where('fixture_id', $fixtureId)->count());
    }

    public function test_custom_configuration_is_snapshotted_on_operational_match(): void
    {
        [, $token] = $this->authenticatedUser();
        $clientUuid = '5ca1ab1e-8baf-4f79-9637-914898a88490';

        $payload = [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Sixes Home',
            'away_team_name' => 'Sixes Away',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
            'client_uuid' => $clientUuid,
            'configuration' => [
                'format' => 'custom',
                'innings_per_side' => 1,
                'overs_per_innings' => 6,
                'squad_size' => 8,
                'playing_xi_size' => 6,
                'maximum_wickets' => 5,
                'legal_balls_per_over' => 6,
                'max_overs_per_bowler' => 2,
                'ball_type' => 'tennis',
                'no_ball_runs' => 1,
                'wide_runs' => 1,
            ],
        ];

        $fixtureResponse = $this->postJson('/api/v1/custom/fixtures', $payload, [
            'Authorization' => 'Bearer '.$token,
        ])->assertCreated();
        $fixtureId = $fixtureResponse->json('data.id');

        // Retrying fixture creation with the same client UUID returns the same row.
        $retriedFixture = $this->postJson('/api/v1/custom/fixtures', $payload, [
            'Authorization' => 'Bearer '.$token,
        ])->assertOk();
        $this->assertSame($fixtureId, $retriedFixture->json('data.id'));
        $this->assertSame(1, Fixture::query()->where('client_uuid', $clientUuid)->count());

        $matchResponse = $this->postJson("/api/v1/custom/fixtures/{$fixtureId}/create-match", [], [
            'Authorization' => 'Bearer '.$token,
        ])->assertCreated();
        $match = CricketMatch::query()->findOrFail($matchResponse->json('data.match_id'));

        $this->assertSame($clientUuid, $match->client_uuid);
        $this->assertSame(6, $match->overs_per_innings);
        $this->assertSame('custom', $match->rule_snapshot['format']);
        $this->assertSame(6, $match->rule_snapshot['playing_xi_size']);
        $this->assertSame(8, $match->rule_snapshot['squad_size']);
        $this->assertSame(5, $match->rule_snapshot['maximum_wickets']);
        $this->assertSame('tennis', $match->rule_snapshot['ball_type']);
        $this->assertSame('custom', $match->rule_snapshot['origin']);
        $this->assertNotNull($match->rule_snapshot['locked_at']);

        // The match keeps its immutable snapshot even if the profile changes later.
        $match->ruleProfile->update(['maximum_wickets' => 4]);
        $this->assertSame(5, $match->fresh()->rule_snapshot['maximum_wickets']);
    }

    public function test_custom_configuration_rejects_wickets_not_below_playing_xi(): void
    {
        [, $token] = $this->authenticatedUser();

        $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Invalid Home',
            'away_team_name' => 'Invalid Away',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
            'configuration' => [
                'format' => 'custom',
                'innings_per_side' => 1,
                'overs_per_innings' => 6,
                'playing_xi_size' => 5,
                'maximum_wickets' => 5,
                'legal_balls_per_over' => 6,
                'ball_type' => 'tennis',
            ],
        ], ['Authorization' => 'Bearer '.$token])
            ->assertUnprocessable()
            ->assertJsonValidationErrors('configuration.maximum_wickets');
    }

    public function test_another_admin_cannot_manage_or_read_someone_elses_custom_match(): void
    {
        [$owner, $ownerToken] = $this->authenticatedUser();
        [$other, $otherToken] = $this->authenticatedUser();
        $this->assertNotSame($owner->id, $other->id);

        $fixtureResponse = $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Private Home',
            'away_team_name' => 'Private Away',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
        ], ['Authorization' => 'Bearer '.$ownerToken])->assertCreated();
        $fixtureId = $fixtureResponse->json('data.id');
        $this->assertSame($owner->id, (int) Fixture::query()->findOrFail($fixtureId)->created_by);
        $this->assertFalse($other->hasRole('super_admin'));
        $this->app['auth']->forgetGuards();

        $this->postJson("/api/v1/custom/fixtures/{$fixtureId}/create-match", [], [
            'Authorization' => 'Bearer '.$otherToken,
        ])->assertForbidden();

        $this->app['auth']->forgetGuards();
        $matchResponse = $this->postJson("/api/v1/custom/fixtures/{$fixtureId}/create-match", [], [
            'Authorization' => 'Bearer '.$ownerToken,
        ])->assertCreated();

        $this->app['auth']->forgetGuards();
        $this->getJson('/api/v1/matches/'.$matchResponse->json('data.match_id').'/state', [
            'Authorization' => 'Bearer '.$otherToken,
        ])->assertNotFound();
    }

    /**
     * Phase 0 regression specification. This remains red until the authenticated
     * match-state policy supports standalone matches without dereferencing a
     * null tournament.
     */
    public function test_custom_live_match_state_is_available_to_its_authenticated_scorer(): void
    {
        [, $token] = $this->authenticatedUser();

        $create = $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Offline Home',
            'away_team_name' => 'Offline Away',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
        ], ['Authorization' => 'Bearer '.$token])->assertCreated();

        $fixture = Fixture::query()->findOrFail($create->json('data.id'));
        $createdMatch = $this->postJson("/api/v1/custom/fixtures/{$fixture->id}/create-match", [], [
            'Authorization' => 'Bearer '.$token,
        ])->assertCreated();

        $match = CricketMatch::query()->findOrFail($createdMatch->json('data.match_id'));
        $match->update(['status' => 'live']);
        MatchInnings::create([
            'match_id' => $match->id,
            'innings_number' => 1,
            'batting_team_id' => $fixture->home_team_id,
            'bowling_team_id' => $fixture->away_team_id,
            'status' => 'live',
            'maximum_overs' => $match->overs_per_innings,
            'started_at' => now(),
        ]);

        $this->getJson("/api/v1/matches/{$match->id}/state", [
            'Authorization' => 'Bearer '.$token,
        ])->assertOk()
            ->assertJsonPath('data.id', $match->id)
            ->assertJsonPath('data.status', 'live');
    }

    public function test_custom_fixture_status_transition_accepts_in_progress(): void
    {
        [, $token] = $this->authenticatedUser();

        $create = $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Sialkot Stallions',
            'away_team_name' => 'Faisalabad Falcons',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
        ], ['Authorization' => 'Bearer '.$token])->assertCreated();

        $fixtureId = $create->json('data.id');

        // scheduled → postponed is a legal transition; scheduled → in_progress is not
        $this->postJson("/api/v1/custom/fixtures/{$fixtureId}/status", ['status' => 'in_progress'], [
            'Authorization' => 'Bearer '.$token,
        ])->assertStatus(422);

        $this->postJson("/api/v1/custom/fixtures/{$fixtureId}/status", ['status' => 'postponed'], [
            'Authorization' => 'Bearer '.$token,
        ])->assertOk();

        $this->assertSame('postponed', Fixture::find($fixtureId)->status);
    }

    public function test_custom_fixture_can_be_deleted_before_match_creation(): void
    {
        [, $token] = $this->authenticatedUser();

        $create = $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'Abbottabad Tigers',
            'away_team_name' => 'Mardan Mavericks',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
        ], ['Authorization' => 'Bearer '.$token])->assertCreated();

        $fixtureId = $create->json('data.id');

        $this->deleteJson("/api/v1/custom/fixtures/{$fixtureId}", [], [
            'Authorization' => 'Bearer '.$token,
        ])->assertOk();

        $this->assertDatabaseMissing('fixtures', ['id' => $fixtureId]);
    }

    public function test_guest_player_can_be_created_without_a_user_account(): void
    {
        [, $token] = $this->authenticatedUser();

        $this->postJson('/api/v1/custom/players', [
            'name' => 'Lineup Guest Batter',
            'role' => 'Batter',
        ], ['Authorization' => 'Bearer '.$token])
            ->assertCreated()
            ->assertJsonPath('data.name', 'Lineup Guest Batter');

        $profile = PlayerProfile::where('full_name', 'Lineup Guest Batter')->firstOrFail();
        $this->assertNull($profile->user_id);
        $this->assertTrue((bool) $profile->is_guest);
        $this->assertNotNull($profile->unique_code);
    }

    public function test_super_admin_players_page_shows_guest_players(): void
    {
        $superAdmin = User::factory()->create();
        $superAdmin->assignRole('super_admin');

        [, $token] = $this->authenticatedUser();

        $this->postJson('/api/v1/custom/players', [
            'name' => 'Guest Allrounder',
            'role' => 'All-Rounder',
        ], ['Authorization' => 'Bearer '.$token])->assertCreated();

        $this->actingAs($superAdmin)
            ->get(route('super-admin.players.index'))
            ->assertOk()
            ->assertSee('Guest Allrounder')
            ->assertSee('Guest');
    }

    public function test_mobile_custom_start_persists_lineups_toss_and_first_innings_idempotently(): void
    {
        [, $token] = $this->authenticatedUser();
        $headers = ['Authorization' => 'Bearer '.$token];
        $fixtureResponse = $this->postJson('/api/v1/custom/fixtures', [
            'home_team_id' => 0,
            'away_team_id' => 0,
            'home_team_name' => 'HI',
            'away_team_name' => 'HI2',
            'scheduled_at' => '2026-09-12T16:00:00.000000Z',
            'configuration' => [
                'format' => 'custom', 'innings_per_side' => 1, 'overs_per_innings' => 2,
                'playing_xi_size' => 2, 'maximum_wickets' => 1, 'legal_balls_per_over' => 6,
                'max_overs_per_bowler' => 1, 'ball_type' => 'tennis',
            ],
        ], $headers)->assertCreated();
        $matchResponse = $this->postJson('/api/v1/custom/fixtures/'.$fixtureResponse->json('data.id').'/create-match', [], $headers)->assertCreated();
        $matchId = $matchResponse->json('data.match_id');
        $payload = [
            'home_lineup' => [['name' => 'HI Batter 1'], ['name' => 'HI Batter 2']],
            'away_lineup' => [['name' => 'HI2 Bowler 1'], ['name' => 'HI2 Bowler 2']],
            'toss_winner' => 'home',
            'toss_decision' => 'bat',
        ];

        $this->postJson("/api/v1/matches/{$matchId}/start-custom", $payload, $headers)
            ->assertOk()
            ->assertJsonPath('data.status', 'live')
            ->assertJsonCount(4, 'data.players')
            ->assertJsonPath('data.revision', 5);
        $this->postJson("/api/v1/matches/{$matchId}/start-custom", $payload, $headers)
            ->assertOk()->assertJsonPath('data.status', 'live');

        $match = CricketMatch::findOrFail($matchId);
        $this->assertNotNull($match->current_innings_id);
        $this->assertNotNull($match->toss_recorded_at);
        $this->assertSame(1, $match->innings()->count());
        $this->assertSame(4, $match->players()->where('selection_type', 'playing_xi')->count());
    }
}
