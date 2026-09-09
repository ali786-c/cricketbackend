<?php

namespace Tests\Feature\Api\V1;

use App\Models\CricketMatch;
use App\Models\Fixture;
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
        $token = $user->createToken('android-test')->accessToken;

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

        // Duplicate creation is rejected (idempotent safety)
        $this->postJson("/api/v1/custom/fixtures/{$fixtureId}/create-match", [], [
            'Authorization' => 'Bearer '.$token,
        ])->assertStatus(422);
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
}
