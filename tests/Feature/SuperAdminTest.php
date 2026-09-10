<?php

namespace Tests\Feature;

use App\Models\ApiClient;
use App\Models\AuditLog;
use App\Models\Tournament;
use App\Models\User;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class SuperAdminTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
    }

    public function test_super_admin_can_open_control_plane_and_regular_admin_cannot(): void
    {
        $superAdmin = $this->userWithRole('super_admin');
        $admin = $this->userWithRole('admin');

        $this->actingAs($superAdmin)->get(route('super-admin.dashboard'))->assertOk()->assertSee('Super Admin Control Plane');
        $this->actingAs($admin)->get(route('super-admin.dashboard'))->assertForbidden();
    }

    public function test_super_admin_can_register_and_disable_an_api_client(): void
    {
        $superAdmin = $this->userWithRole('super_admin');
        $payload = ['name' => 'Android App', 'slug' => 'android-app', 'platform' => 'android', 'version' => '1.0.0', 'rate_limit_per_minute' => 240, 'notes' => 'Mobile client'];

        $this->actingAs($superAdmin)->post(route('super-admin.api-clients.store'), $payload)->assertRedirect(route('super-admin.api-clients.index'));
        $client = ApiClient::query()->firstOrFail();
        $this->assertTrue($client->is_active);
        $this->assertDatabaseHas('audit_logs', ['action' => 'api_client.created', 'auditable_id' => $client->id]);

        $this->actingAs($superAdmin)->post(route('super-admin.api-clients.toggle', $client))->assertRedirect();
        $this->assertFalse($client->fresh()->is_active);
    }

    public function test_dashboard_exposes_platform_governance_modules_and_metrics(): void
    {
        $superAdmin = $this->userWithRole('super_admin');
        $this->userWithRole('player');
        Tournament::create(['name' => 'Fleet Cup', 'slug' => 'fleet-cup', 'status' => 'live', 'is_public' => true, 'timezone' => 'Asia/Karachi']);

        $this->actingAs($superAdmin)->get(route('super-admin.dashboard'))
            ->assertOk()
            ->assertSee('Tournament status map')
            ->assertSee('Identity distribution')
            ->assertSee('Governance modules')
            ->assertSee(route('super-admin.users.index'))
            ->assertSee(route('super-admin.tournaments.index'));
    }

    public function test_super_admin_can_govern_user_roles_and_revoke_all_user_sessions(): void
    {
        $superAdmin = $this->userWithRole('super_admin');
        $target = $this->userWithRole('player');
        $target->createToken('phone')->accessToken;

        $this->actingAs($superAdmin)->get(route('super-admin.users.index', ['role' => 'player']))->assertOk()->assertSee($target->email);
        $this->actingAs($superAdmin)->post(route('super-admin.users.role.update', $target), ['role' => 'captain'])->assertRedirect();
        $this->assertTrue($target->fresh()->hasRole('captain'));
        $this->actingAs($superAdmin)->post(route('super-admin.users.sessions.revoke', $target))->assertRedirect();
        $this->assertDatabaseMissing('personal_access_tokens', ['tokenable_id' => $target->id]);
        $this->assertDatabaseHas('audit_logs', ['action' => 'super_admin.user_role_changed', 'auditable_id' => $target->id]);
        $this->assertDatabaseHas('audit_logs', ['action' => 'super_admin.user_sessions_revoked', 'auditable_id' => $target->id]);
    }

    public function test_super_admin_cannot_remove_the_last_super_admin_role(): void
    {
        $superAdmin = $this->userWithRole('super_admin');
        $this->actingAs($superAdmin)->post(route('super-admin.users.role.update', $superAdmin), ['role' => 'admin'])
            ->assertSessionHasErrors('role');
        $this->assertTrue($superAdmin->fresh()->hasRole('super_admin'));
    }

    public function test_super_admin_can_filter_tournament_fleet_and_open_oversight_detail(): void
    {
        $superAdmin = $this->userWithRole('super_admin');
        $tournament = Tournament::create(['name' => 'Fleet Oversight Cup', 'slug' => 'fleet-oversight-cup', 'season_name' => '2026', 'status' => 'completed', 'is_public' => true, 'timezone' => 'Asia/Karachi']);

        $this->actingAs($superAdmin)->get(route('super-admin.tournaments.index', ['status' => 'completed']))
            ->assertOk()->assertSee('Fleet Oversight Cup');
        $this->actingAs($superAdmin)->get(route('super-admin.tournaments.show', $tournament))
            ->assertOk()->assertSee('Operational profile')->assertSee('Tournament audit activity');
    }

    public function test_super_admin_can_filter_and_export_audit_logs_and_view_diagnostics(): void
    {
        $superAdmin = $this->userWithRole('super_admin');
        AuditLog::create(['user_id' => $superAdmin->id, 'action' => 'verification.special_event', 'metadata' => ['source' => 'test']]);

        $this->actingAs($superAdmin)->get(route('super-admin.audit-logs.index', ['search' => 'verification.special_event']))
            ->assertOk()->assertSee('verification.special_event');
        $this->actingAs($superAdmin)->get(route('super-admin.audit-logs.export', ['search' => 'verification.special_event']))
            ->assertOk()->assertHeader('content-type', 'text/csv; charset=UTF-8');
        $this->actingAs($superAdmin)->get(route('super-admin.health'))
            ->assertOk()->assertSee('Runtime profile')->assertSee('Production checklist')->assertSee('Database');
    }

    public function test_super_admin_can_revoke_an_api_session_and_view_governance_pages(): void
    {
        $superAdmin = $this->userWithRole('super_admin');
        $mobileUser = $this->userWithRole('captain');
        $token = $mobileUser->createToken('android-device')->accessToken;

        $this->actingAs($superAdmin)->get(route('super-admin.api-sessions.index'))->assertOk()->assertSee('android-device');
        $this->actingAs($superAdmin)->delete(route('super-admin.api-sessions.revoke', $token))->assertRedirect();
        $this->assertDatabaseMissing('personal_access_tokens', ['id' => $token->id]);
        $this->actingAs($superAdmin)->get(route('super-admin.audit-logs.index'))->assertOk();
        $this->actingAs($superAdmin)->get(route('super-admin.health'))->assertOk()->assertSee('System health');
    }

    public function test_super_admin_match_center_shows_full_match_details(): void
    {
        $superAdmin = $this->userWithRole('super_admin');

        $ruleProfile = \App\Models\CricketRuleProfile::create(['name' => 'Test 6 Over', 'slug' => 'test-6-over', 'format' => 't20', 'overs_per_innings' => 6, 'legal_balls_per_over' => 6]);
        $homeTeam = \App\Models\Team::create(['name' => 'Lahore Lions', 'short_name' => 'LHR']);
        $awayTeam = \App\Models\Team::create(['name' => 'Karachi Kings', 'short_name' => 'KHI']);
        $tournament = Tournament::create(['name' => 'Center Cup', 'slug' => 'center-cup', 'status' => 'live', 'is_public' => true, 'timezone' => 'Asia/Karachi', 'cricket_rule_profile_id' => $ruleProfile->id]);
        $fixture = \App\Models\Fixture::create(['tournament_id' => $tournament->id, 'home_team_id' => $homeTeam->id, 'away_team_id' => $awayTeam->id, 'status' => 'in_progress', 'scheduled_at' => now(), 'venue' => 'Gaddafi Stadium']);

        $match = \App\Models\CricketMatch::create(['fixture_id' => $fixture->id, 'tournament_id' => $tournament->id, 'rule_profile_id' => $ruleProfile->id, 'overs_per_innings' => 6, 'status' => 'live', 'revision' => 6]);

        $ayan = \App\Models\MatchPlayer::create(['match_id' => $match->id, 'team_id' => $homeTeam->id, 'player_name_snapshot' => 'Ayan', 'player_role_snapshot' => 'Batsman', 'selection_type' => 'playing_xi', 'batting_order' => 1]);
        $umar = \App\Models\MatchPlayer::create(['match_id' => $match->id, 'team_id' => $homeTeam->id, 'player_name_snapshot' => 'Umar', 'selection_type' => 'playing_xi', 'batting_order' => 2]);
        $bilal = \App\Models\MatchPlayer::create(['match_id' => $match->id, 'team_id' => $homeTeam->id, 'player_name_snapshot' => 'Bilal', 'selection_type' => 'playing_xi', 'batting_order' => 3]);
        $sam = \App\Models\MatchPlayer::create(['match_id' => $match->id, 'team_id' => $awayTeam->id, 'player_name_snapshot' => 'Sam', 'player_role_snapshot' => 'Bowler', 'selection_type' => 'playing_xi', 'batting_order' => 1, 'is_captain' => true]);
        $zain = \App\Models\MatchPlayer::create(['match_id' => $match->id, 'team_id' => $awayTeam->id, 'player_name_snapshot' => 'Zain', 'selection_type' => 'playing_xi', 'batting_order' => 2]);

        $innings = \App\Models\MatchInnings::create(['match_id' => $match->id, 'innings_number' => 1, 'batting_team_id' => $homeTeam->id, 'bowling_team_id' => $awayTeam->id, 'status' => 'live', 'maximum_overs' => 6, 'total_runs' => 13, 'wickets' => 2, 'legal_balls' => 6]);
        $match->update(['current_innings_id' => $innings->id]);

        $ballSpecs = [
            ['over' => 1, 'ball' => 1, 'runs' => 4, 'striker' => $ayan],
            ['over' => 1, 'ball' => 2, 'runs' => 0, 'striker' => $ayan, 'wicket' => ['dismissed' => $ayan, 'type' => 'caught', 'fielder' => $zain]],
            ['over' => 1, 'ball' => 3, 'runs' => 1, 'striker' => $bilal],
            ['over' => 1, 'ball' => 4, 'runs' => 0, 'striker' => $bilal, 'wicket' => ['dismissed' => $bilal, 'type' => 'bowled']],
            ['over' => 1, 'ball' => 5, 'runs' => 6, 'striker' => $umar],
            ['over' => 1, 'ball' => 6, 'runs' => 2, 'striker' => $umar],
        ];
        foreach ($ballSpecs as $index => $spec) {
            $delivery = \App\Models\MatchDelivery::create([
                'match_id' => $match->id, 'innings_id' => $innings->id,
                'over_number' => $spec['over'], 'ball_number' => $spec['ball'], 'sequence_number' => $index + 1,
                'striker_id' => $spec['striker']->id, 'non_striker_id' => ($spec['striker']->is($ayan) || $spec['striker']->is($bilal)) ? $umar->id : $ayan->id,
                'bowler_id' => $sam->id, 'runs_off_bat' => $spec['runs'], 'total_runs' => $spec['runs'],
                'is_legal_delivery' => true, 'recorded_at' => now(), 'revision' => $index + 1,
            ]);
            if (isset($spec['wicket'])) {
                $wicket = \App\Models\MatchWicket::create(['delivery_id' => $delivery->id, 'dismissed_player_id' => $spec['wicket']['dismissed']->id, 'dismissal_type' => $spec['wicket']['type'], 'credited_bowler_id' => $sam->id, 'fielder_id' => $spec['wicket']['fielder']->id ?? null, 'is_valid_wicket' => true]);
                $delivery->update(['wicket_id' => $wicket->id]);
            }
        }

        \App\Models\InningsBattingStat::create(['innings_id' => $innings->id, 'match_player_id' => $ayan->id, 'batting_position' => 1, 'runs' => 12, 'balls' => 9, 'fours' => 2, 'sixes' => 1, 'strike_rate' => 133.33, 'dismissal_type' => 'caught', 'dismissed_by' => $sam->id, 'fielder_id' => $zain->id, 'status' => 'out']);
        \App\Models\InningsBattingStat::create(['innings_id' => $innings->id, 'match_player_id' => $bilal->id, 'batting_position' => 3, 'runs' => 1, 'balls' => 2, 'fours' => 0, 'sixes' => 0, 'strike_rate' => 50, 'dismissal_type' => 'bowled', 'dismissed_by' => $sam->id, 'status' => 'out']);
        \App\Models\InningsBattingStat::create(['innings_id' => $innings->id, 'match_player_id' => $umar->id, 'batting_position' => 2, 'runs' => 0, 'balls' => 0, 'fours' => 0, 'sixes' => 0, 'strike_rate' => 0, 'status' => 'did_not_bat']);
        \App\Models\InningsBowlingStat::create(['innings_id' => $innings->id, 'match_player_id' => $sam->id, 'legal_balls' => 6, 'maidens' => 0, 'runs_conceded' => 13, 'wickets' => 2, 'no_balls' => 0, 'wides' => 0, 'economy' => 13]);

        // Matches directory links into the Match Center.
        $this->actingAs($superAdmin)->get(route('super-admin.matches.index'))
            ->assertOk()
            ->assertSee(route('super-admin.matches.show', $match));

        $this->actingAs($superAdmin)->get(route('super-admin.matches.show', $match))
            ->assertOk()
            ->assertSee('Match Center')
            ->assertSee('Center Cup')
            ->assertSee('Lahore Lions')
            ->assertSee('Karachi Kings')
            ->assertSee('13/2')
            ->assertSee('Ayan')
            ->assertSee('Caught (Zain) b Sam')
            ->assertSee('Fall of wickets')
            ->assertSee('13/2')
            ->assertSee("p'ship 4 (2)", false)
            ->assertSee('Gaddafi Stadium')
            ->assertSee('Super Stars')
            ->assertSee('Squads');

        $this->actingAs($superAdmin)->getJson(route('super-admin.matches.state', $match))
            ->assertOk()
            ->assertJsonPath('revision', 6)
            ->assertJsonPath('status', 'live');

        // A regular admin has no access to the super admin control plane.
        $this->actingAs($this->userWithRole('admin'))->get(route('super-admin.matches.show', $match))->assertForbidden();
    }

    public function test_super_admin_match_center_works_for_standalone_custom_matches(): void
    {
        $superAdmin = $this->userWithRole('super_admin');

        $ruleProfile = \App\Models\CricketRuleProfile::create(['name' => 'Custom 5 Over', 'slug' => 'custom-5-over', 'format' => 't20', 'overs_per_innings' => 5, 'legal_balls_per_over' => 6]);
        $homeTeam = \App\Models\Team::create(['name' => 'Street Warriors', 'short_name' => 'STW']);
        $awayTeam = \App\Models\Team::create(['name' => 'Gully Stars', 'short_name' => 'GUL']);
        $fixture = \App\Models\Fixture::create(['tournament_id' => null, 'home_team_id' => $homeTeam->id, 'away_team_id' => $awayTeam->id, 'status' => 'completed', 'scheduled_at' => now()]);
        $match = \App\Models\CricketMatch::create(['fixture_id' => $fixture->id, 'tournament_id' => null, 'rule_profile_id' => $ruleProfile->id, 'overs_per_innings' => 5, 'status' => 'completed', 'result_type' => 'win', 'result_summary' => 'Street Warriors won by 12 runs']);

        $this->actingAs($superAdmin)->get(route('super-admin.matches.show', $match))
            ->assertOk()
            ->assertSee('Custom Match')
            ->assertSee('Street Warriors won by 12 runs')
            ->assertSee('Innings have not started yet');
    }

    private function userWithRole(string $role): User
    {
        $user = User::factory()->create();
        $user->assignRole($role);
        return $user;
    }
}
