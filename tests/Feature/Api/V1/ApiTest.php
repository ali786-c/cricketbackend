<?php

namespace Tests\Feature\Api\V1;

use App\Models\ApiClient;
use App\Models\CricketRuleProfile;
use App\Models\PlayerProfile;
use App\Models\Team;
use App\Models\TournamentPlayer;
use App\Models\Tournament;
use App\Models\User;
use Database\Seeders\CricketRuleProfileSeeder;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Auth;
use Tests\TestCase;

class ApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
        $this->seed(CricketRuleProfileSeeder::class);
    }

    public function test_mobile_user_can_login_and_fetch_current_identity(): void
    {
        $user = User::factory()->create(['email' => 'mobile@example.com', 'password' => 'password']);
        $user->assignRole('captain');

        $login = $this->postJson('/api/v1/auth/login', ['email' => $user->email, 'password' => 'password', 'device_name' => 'android-test']);
        $login->assertOk()->assertJsonStructure(['data' => ['id', 'roles', 'permissions'], 'token', 'token_type']);
        $token = $login->json('token');

        $this->withHeader('Authorization', 'Bearer '.$token)->getJson('/api/v1/auth/me')
            ->assertOk()->assertJsonPath('data.email', 'mobile@example.com')->assertJsonPath('data.roles.0', 'captain');
    }

    public function test_invalid_api_login_is_rejected(): void
    {
        User::factory()->create(['email' => 'mobile@example.com', 'password' => 'password']);
        $this->postJson('/api/v1/auth/login', ['email' => 'mobile@example.com', 'password' => 'wrong-password', 'device_name' => 'ios-test'])
            ->assertStatus(422)->assertJsonValidationErrors('email');
    }

    public function test_disabled_api_client_cannot_create_a_mobile_session(): void
    {
        $user = User::factory()->create(['email' => 'mobile@example.com', 'password' => 'password']);
        ApiClient::create(['name' => 'Disabled App', 'slug' => 'disabled-app', 'platform' => 'android', 'is_active' => false, 'created_by' => $user->id]);
        $this->postJson('/api/v1/auth/login', ['email' => $user->email, 'password' => 'password', 'device_name' => 'android-test', 'client_slug' => 'disabled-app'])->assertStatus(422)->assertJsonValidationErrors('client_slug');
    }

    public function test_active_api_client_is_marked_seen_when_a_mobile_session_is_created(): void
    {
        $user = User::factory()->create(['email' => 'mobile@example.com', 'password' => 'password']);
        $client = ApiClient::create(['name' => 'Android App', 'slug' => 'android-app', 'platform' => 'android', 'is_active' => true, 'created_by' => $user->id]);
        $this->postJson('/api/v1/auth/login', ['email' => $user->email, 'password' => 'password', 'device_name' => 'android-test', 'client_slug' => $client->slug])->assertOk()->assertJsonPath('data.id', $user->id);
        $this->assertNotNull($client->fresh()->last_seen_at);
    }

    public function test_authenticated_player_can_update_profile_and_register_for_a_tournament(): void
    {
        $user = User::factory()->create(['email' => 'player@example.com', 'password' => 'password']);
        $user->assignRole('player');
        $token = $user->createToken('mobile')->plainTextToken;
        $headers = ['Authorization' => 'Bearer '.$token];
        $this->withHeaders($headers)->patchJson('/api/v1/profile', ['full_name' => 'Mobile Player', 'phone' => '03001234567', 'city' => 'Lahore', 'playing_role' => 'Batter'])->assertOk();
        $profile = PlayerProfile::query()->where('user_id', $user->id)->firstOrFail();
        $tournament = Tournament::create(['name' => 'Registration API Cup', 'slug' => 'registration-api-cup', 'status' => 'registration', 'is_public' => true, 'timezone' => 'Asia/Karachi']);
        $this->withHeaders($headers)->postJson('/api/v1/tournaments/'.$tournament->slug.'/registration')->assertOk()->assertJsonPath('data.status', 'pending');
        $this->assertDatabaseHas('tournament_players', ['tournament_id' => $tournament->id, 'player_profile_id' => $profile->id, 'status' => 'pending']);
    }

    public function test_public_tournament_api_returns_teams_and_approved_players(): void
    {
        $profile = CricketRuleProfile::query()->firstOrFail();
        $tournament = Tournament::create(['name' => 'Resources Cup', 'slug' => 'resources-cup', 'status' => 'live', 'is_public' => true, 'timezone' => 'Asia/Karachi', 'cricket_rule_profile_id' => $profile->id]);
        $team = Team::create(['tournament_id' => $tournament->id, 'name' => 'Ali Panthers', 'short_name' => 'AP', 'display_order' => 1, 'is_active' => true]);
        $playerUser = User::factory()->create();
        $playerProfile = PlayerProfile::create(['user_id' => $playerUser->id, 'full_name' => 'API Batter', 'playing_role' => 'Batter']);
        TournamentPlayer::create(['tournament_id' => $tournament->id, 'player_profile_id' => $playerProfile->id, 'status' => 'approved']);
        $this->getJson('/api/v1/tournaments/'.$tournament->slug.'/teams')->assertOk()->assertJsonFragment(['short_name' => 'AP']);
        $this->getJson('/api/v1/tournaments/'.$tournament->slug.'/players')->assertOk()->assertJsonFragment(['full_name' => 'API Batter']);
    }

    public function test_public_tournament_api_returns_only_visible_tournaments(): void
    {
        $profile = CricketRuleProfile::query()->firstOrFail();
        Tournament::create(['name' => 'Visible Cup', 'slug' => 'visible-cup', 'status' => 'live', 'is_public' => true, 'timezone' => 'Asia/Karachi', 'cricket_rule_profile_id' => $profile->id]);
        Tournament::create(['name' => 'Private Cup', 'slug' => 'private-cup', 'status' => 'live', 'is_public' => false, 'timezone' => 'Asia/Karachi', 'cricket_rule_profile_id' => $profile->id]);

        $this->getJson('/api/v1/tournaments')->assertOk()->assertJsonFragment(['name' => 'Visible Cup'])->assertJsonMissing(['name' => 'Private Cup']);
    }

    public function test_team_unique_code_is_the_same_public_id_in_search_and_lookup(): void
    {
        $team = Team::create([
            'name' => 'Canonical Team',
            'short_name' => 'CT',
            'unique_code' => 'TEAM-A1B2C',
            'is_active' => true,
        ]);

        $this->getJson('/api/v1/search?q=team-a1b2c&type=teams')
            ->assertOk()
            ->assertJsonPath('data.teams.0.id', $team->id)
            ->assertJsonPath('data.teams.0.unique_code', 'TEAM-A1B2C');

        $this->getJson('/api/v1/search/lookup/team-a1b2c')
            ->assertOk()
            ->assertJsonPath('data.type', 'team')
            ->assertJsonPath('data.data.id', $team->id)
            ->assertJsonPath('data.data.unique_code', 'TEAM-A1B2C');
    }

    public function test_authenticated_user_can_revoke_current_api_token(): void
    {
        $user = User::factory()->create(['password' => 'password']);
        $token = $user->createToken('test-device')->plainTextToken;
        $this->withHeader('Authorization', 'Bearer '.$token)->postJson('/api/v1/auth/logout')->assertOk();
        $this->assertDatabaseCount('personal_access_tokens', 0);
        Auth::forgetGuards();
        $this->withHeader('Authorization', 'Bearer '.$token)->getJson('/api/v1/auth/me')->assertUnauthorized();
    }
}
