<?php

namespace Tests\Feature\Api\V1;

use App\Models\CricketMatch;
use App\Models\CricketRuleProfile;
use App\Models\Draft;
use App\Models\DraftPick;
use App\Models\DraftRound;
use App\Models\Fixture;
use App\Models\PlayerProfile;
use App\Models\Team;
use App\Models\Tournament;
use App\Models\TournamentPlayer;
use App\Models\User;
use Database\Seeders\CricketRuleProfileSeeder;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Auth;
use Tests\TestCase;

class AdminApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
        $this->seed(CricketRuleProfileSeeder::class);
    }

    public function test_admin_can_create_and_transition_a_tournament(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $headers = ['Authorization' => 'Bearer '.$admin->createToken('admin-mobile')->plainTextToken];
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();

        $created = $this->withHeaders($headers)->postJson('/api/v1/admin/tournaments', [
            'name' => 'Mobile Admin Cup',
            'slug' => 'mobile-admin-cup',
            'season_name' => '2026',
            'timezone' => 'Asia/Karachi',
            'squad_size' => 11,
            'default_pick_duration' => 60,
            'default_overs_per_innings' => 12,
            'cricket_rule_profile_id' => $profile->id,
        ]);

        $created->assertCreated()
            ->assertJsonPath('data.status', 'draft')
            ->assertJsonPath('data.cricket_rule_profile_id', $profile->id)
            ->assertJsonPath('data.cricket_rule_profile.version', $profile->version)
            ->assertJsonPath('data.default_overs_per_innings', $profile->overs_per_innings);
        $this->withHeaders($headers)->postJson('/api/v1/admin/tournaments/mobile-admin-cup/status', ['status' => 'registration'])
            ->assertOk()->assertJsonPath('data.status', 'registration');
        $this->withHeaders($headers)->postJson('/api/v1/admin/tournaments/mobile-admin-cup/status', ['status' => 'live'])
            ->assertOk()->assertJsonPath('data.status', 'live');
        $this->assertDatabaseHas('audit_logs', ['action' => 'tournament.status_changed']);
    }

    public function test_rule_profiles_endpoint_only_returns_active_profiles(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        CricketRuleProfile::query()->where('slug', 't20-standard')->update(['is_active' => false]);

        $response = $this->withToken($admin->createToken('profiles')->plainTextToken)
            ->getJson('/api/v1/admin/rule-profiles');

        $response->assertOk();
        $this->assertNotEmpty($response->json('data'));
        $this->assertTrue(collect($response->json('data'))->every(fn (array $profile) => $profile['is_active'] === true));
    }

    public function test_rule_profile_is_required_and_locked_after_match_creation(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $profiles = CricketRuleProfile::query()->where('is_active', true)->take(2)->get();
        $headers = ['Authorization' => 'Bearer '.$admin->createToken('rules-lock')->plainTextToken];

        $this->withHeaders($headers)->postJson('/api/v1/admin/tournaments', [
            'name' => 'Missing Profile Cup', 'slug' => 'missing-profile-cup',
            'timezone' => 'Asia/Karachi', 'squad_size' => 11, 'default_pick_duration' => 60,
        ])->assertUnprocessable()->assertJsonValidationErrors('cricket_rule_profile_id');

        $tournament = Tournament::create([
            'name' => 'Locked Rules Cup', 'slug' => 'locked-rules-cup', 'status' => 'ready',
            'timezone' => 'Asia/Karachi', 'creator_id' => $admin->id,
            'cricket_rule_profile_id' => $profiles[0]->id,
        ]);
        CricketMatch::create([
            'tournament_id' => $tournament->id, 'rule_profile_id' => $profiles[0]->id,
            'rule_profile_version' => $profiles[0]->version, 'overs_per_innings' => $profiles[0]->overs_per_innings,
            'status' => 'squad_selection', 'revision' => 1, 'created_by' => $admin->id,
        ]);

        $this->withHeaders($headers)->patchJson('/api/v1/admin/tournaments/'.$tournament->slug, [
            'cricket_rule_profile_id' => $profiles[1]->id,
        ])->assertUnprocessable()->assertJsonValidationErrors('cricket_rule_profile_id');
    }

    public function test_admin_can_approve_a_tournament_player_registration(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $player = User::factory()->create();
        $player->playerProfile()->update(['full_name' => 'API Registration Player', 'playing_role' => 'Batter']);
        $profile = $player->playerProfile()->firstOrFail();
        $tournament = Tournament::create(['name' => 'Approval API Cup', 'slug' => 'approval-api-cup', 'status' => 'registration', 'is_public' => true, 'timezone' => 'Asia/Karachi', 'creator_id' => $admin->id]);
        $registration = TournamentPlayer::create(['tournament_id' => $tournament->id, 'player_profile_id' => $profile->id, 'status' => 'pending']);
        $headers = ['Authorization' => 'Bearer '.$admin->createToken('admin-mobile')->plainTextToken];

        $this->withHeaders($headers)->postJson('/api/v1/admin/tournaments/'.$tournament->slug.'/players/'.$registration->id.'/approve')
            ->assertOk()->assertJsonPath('data.status', 'approved');
        $this->assertDatabaseHas('tournament_players', ['id' => $registration->id, 'status' => 'approved', 'reviewed_by' => $admin->id]);
    }

    public function test_regular_admin_cannot_access_super_admin_governance_api(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $superAdmin = User::factory()->create();
        $superAdmin->assignRole('super_admin');
        $adminToken = $admin->createToken('admin')->plainTextToken;
        $superAdminToken = $superAdmin->createToken('super-admin')->plainTextToken;

        $this->getJson('/api/v1/super-admin/health', ['Authorization' => 'Bearer '.$adminToken])->assertForbidden();
        Auth::forgetGuards();
        $this->getJson('/api/v1/super-admin/health', ['Authorization' => 'Bearer '.$superAdminToken])->assertOk()->assertJsonPath('data.api', 'online');
    }

    public function test_admin_api_can_update_pre_live_match_overs_only(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();
        $tournament = Tournament::create(['name' => 'Match Overs API Cup', 'slug' => 'match-overs-api-cup', 'status' => 'ready', 'is_public' => true, 'timezone' => 'Asia/Karachi', 'cricket_rule_profile_id' => $profile->id, 'creator_id' => $admin->id]);
        $match = CricketMatch::create(['tournament_id' => $tournament->id, 'rule_profile_id' => $profile->id, 'rule_profile_version' => $profile->version, 'overs_per_innings' => 20, 'status' => 'squad_selection', 'revision' => 1, 'created_by' => $admin->id]);
        $headers = ['Authorization' => 'Bearer '.$admin->createToken('admin-match-overs')->plainTextToken];

        $this->withHeaders($headers)->patchJson('/api/v1/admin/tournaments/'.$tournament->slug.'/matches/'.$match->id.'/overs', ['overs_per_innings' => 6])
            ->assertOk()->assertJsonPath('data.overs_per_innings', 6);
        $match->update(['status' => 'live']);
        $this->withHeaders($headers)->patchJson('/api/v1/admin/tournaments/'.$tournament->slug.'/matches/'.$match->id.'/overs', ['overs_per_innings' => 10])
            ->assertStatus(422);
    }

    public function test_my_resources_and_viewer_permissions_are_scoped_to_the_authenticated_owner(): void
    {
        $owner = User::factory()->create();
        $owner->assignRole('admin');
        $other = User::factory()->create();
        $other->assignRole('admin');
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();

        $ownedTournament = Tournament::create([
            'name' => 'Owner Only Cup', 'slug' => 'owner-only-cup', 'status' => 'live',
            'is_public' => true, 'timezone' => 'Asia/Karachi', 'creator_id' => $owner->id,
            'cricket_rule_profile_id' => $profile->id,
        ]);
        $otherTournament = Tournament::create([
            'name' => 'Other User Cup', 'slug' => 'other-user-cup', 'status' => 'live',
            'is_public' => true, 'timezone' => 'Asia/Karachi', 'creator_id' => $other->id,
            'cricket_rule_profile_id' => $profile->id,
        ]);
        $home = Team::create(['name' => 'Owner Home', 'short_name' => 'OH', 'creator_id' => $owner->id]);
        $away = Team::create(['name' => 'Owner Away', 'short_name' => 'OA', 'creator_id' => $owner->id]);
        $fixture = Fixture::create([
            'tournament_id' => $ownedTournament->id,
            'home_team_id' => $home->id,
            'away_team_id' => $away->id,
            'scheduled_at' => now()->addDay(),
            'timezone' => 'Asia/Karachi',
            'created_by' => $owner->id,
        ]);
        $match = CricketMatch::create([
            'fixture_id' => $fixture->id,
            'tournament_id' => $ownedTournament->id,
            'rule_profile_id' => $profile->id,
            'rule_profile_version' => $profile->version,
            'overs_per_innings' => 20,
            'status' => 'live',
            'revision' => 1,
            'created_by' => $owner->id,
        ]);
        CricketMatch::create([
            'tournament_id' => $otherTournament->id,
            'rule_profile_id' => $profile->id,
            'rule_profile_version' => $profile->version,
            'overs_per_innings' => 20,
            'status' => 'live',
            'revision' => 1,
            'created_by' => $other->id,
        ]);

        $ownerToken = $owner->createToken('owner-resources')->plainTextToken;
        $this->withToken($ownerToken)->getJson('/api/v1/me/tournaments')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $ownedTournament->id)
            ->assertJsonPath('data.0.viewer_permissions.can_manage', true);
        $this->withToken($ownerToken)->getJson('/api/v1/me/fixtures')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $fixture->id)
            ->assertJsonPath('data.0.viewer_permissions.can_edit_fixture', true);
        $this->withToken($ownerToken)->getJson('/api/v1/me/matches')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $match->id)
            ->assertJsonPath('data.0.viewer_permissions.can_score', true);

        Auth::forgetGuards();
        $otherToken = $other->createToken('public-viewer')->plainTextToken;
        $this->withToken($otherToken)->getJson("/api/v1/matches/{$match->id}/state")
            ->assertOk()
            ->assertJsonPath('data.viewer_permissions.can_manage', false)
            ->assertJsonPath('data.viewer_permissions.can_score', false)
            ->assertJsonPath('data.viewer_permissions.can_view', true);
    }

    public function test_super_admin_api_can_govern_users_and_tournaments(): void
    {
        $superAdmin = User::factory()->create();
        $superAdmin->assignRole('super_admin');
        $target = User::factory()->create();
        $target->assignRole('player');
        $target->createToken('phone');
        $tournament = Tournament::create(['name' => 'Governance API Cup', 'slug' => 'governance-api-cup', 'status' => 'live', 'is_public' => true, 'timezone' => 'Asia/Karachi']);
        $headers = ['Authorization' => 'Bearer '.$superAdmin->createToken('super-admin-mobile')->plainTextToken];

        $this->withHeaders($headers)->getJson('/api/v1/super-admin/users?role=player')->assertOk()->assertJsonPath('data.data.0.email', $target->email);
        $this->withHeaders($headers)->postJson('/api/v1/super-admin/users/'.$target->id.'/role', ['role' => 'captain'])->assertOk()->assertJsonPath('data.roles.0.name', 'captain');
        $this->withHeaders($headers)->postJson('/api/v1/super-admin/users/'.$target->id.'/revoke-sessions')->assertOk()->assertJsonPath('data.revoked', 1);
        $this->withHeaders($headers)->getJson('/api/v1/super-admin/tournaments?status=live')->assertOk()->assertJsonPath('data.data.0.slug', $tournament->slug);
        $this->withHeaders($headers)->getJson('/api/v1/super-admin/tournaments/'.$tournament->slug)->assertOk()->assertJsonPath('data.slug', $tournament->slug);
        $this->assertDatabaseHas('audit_logs', ['action' => 'super_admin.user_role_changed', 'auditable_id' => $target->id]);
    }

    public function test_super_admin_api_health_returns_diagnostics(): void
    {
        $superAdmin = User::factory()->create();
        $superAdmin->assignRole('super_admin');
        $headers = ['Authorization' => 'Bearer '.$superAdmin->createToken('super-admin-health')->plainTextToken];

        $this->withHeaders($headers)->getJson('/api/v1/super-admin/health')->assertOk()->assertJsonStructure(['data' => ['checked_at', 'database', 'queue', 'security', 'environment']]);
    }

    public function test_admin_can_start_next_pick_and_receive_revision_aware_state(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $tournament = Tournament::create(['name' => 'Draft Admin API Cup', 'slug' => 'draft-admin-api-cup', 'status' => 'ready', 'is_public' => true, 'timezone' => 'Asia/Karachi', 'has_draft' => true, 'creator_id' => $admin->id]);
        $team = Team::create(['tournament_id' => $tournament->id, 'name' => 'API Team', 'short_name' => 'API', 'is_active' => true, 'display_order' => 1]);
        $tournament->teams()->attach($team->id);
        $draft = Draft::create(['tournament_id' => $tournament->id, 'status' => 'setup', 'revision' => 1]);
        $round = DraftRound::create(['draft_id' => $draft->id, 'round_number' => 1, 'name' => 'Round 1', 'status' => 'pending']);
        DraftPick::create(['draft_id' => $draft->id, 'draft_round_id' => $round->id, 'team_id' => $team->id, 'pick_number' => 1, 'pick_duration' => 30, 'status' => 'pending']);
        $headers = ['Authorization' => 'Bearer '.$admin->createToken('admin-mobile')->plainTextToken];

        $this->withHeaders($headers)->getJson('/api/v1/admin/tournaments/'.$tournament->slug.'/draft/state')
            ->assertOk()->assertJsonPath('data.can_start_next_pick', true);
        $this->withHeaders($headers)->postJson('/api/v1/admin/tournaments/'.$tournament->slug.'/draft/start')
            ->assertOk()->assertJsonPath('data.status', 'live')->assertJsonPath('data.current_pick_number', 1);
        $this->getJson('/api/v1/tournaments/'.$tournament->slug.'/sync')->assertOk()->assertJsonStructure(['data' => ['revision', 'server_time', 'draft', 'fixtures', 'matches', 'standings']]);
    }
}
