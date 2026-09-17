<?php

namespace Tests\Feature\Admin;

use App\Models\CricketRuleProfile;
use App\Models\Draft;
use App\Models\DraftPick;
use App\Models\DraftRound;
use App\Models\Fixture;
use App\Models\PlayerProfile;
use App\Models\Team;
use App\Models\TeamCaptain;
use App\Models\Tournament;
use App\Models\TournamentPlayer;
use App\Models\User;
use Database\Seeders\CricketRuleProfileSeeder;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class FixtureTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(RolePermissionSeeder::class);
        $this->seed(CricketRuleProfileSeeder::class);
    }

    public function test_admin_can_create_and_view_a_scheduled_fixture(): void
    {
        [$admin, $tournament, $home, $away] = $this->tournamentWithTeams();

        $this->actingAs($admin)->post(route('admin.tournaments.fixtures.store', $tournament), [
            'home_team_id' => $home->id,
            'away_team_id' => $away->id,
            'round_number' => 1,
            'round_name' => 'Group stage',
            'match_number' => 1,
            'scheduled_at' => '2026-09-01T18:00',
            'venue' => 'City Cricket Ground',
            'city' => 'Lahore',
            'timezone' => 'Asia/Karachi',
            'notes' => 'Opening match',
        ])->assertRedirect(route('admin.tournaments.fixtures.index', $tournament));

        $fixture = Fixture::query()->firstOrFail();
        $this->assertSame('scheduled', $fixture->status);
        $this->assertSame('Group stage', $fixture->round_name);
        $this->assertSame('Asia/Karachi', $fixture->timezone);
        $this->assertSame('2026-09-01 13:00:00', $fixture->scheduled_at->format('Y-m-d H:i:s'));

        $this->actingAs($admin)->get(route('admin.tournaments.fixtures.index', $tournament))
            ->assertOk()
            ->assertSee('City Cricket Ground')
            ->assertSee('Ali Panthers')
            ->assertSee('Lahore Lions');
    }

    public function test_fixture_rejects_teams_from_another_tournament(): void
    {
        [$admin, $tournament, $home, $away] = $this->tournamentWithTeams();
        $otherTournament = Tournament::create(['name' => 'Other Cup', 'slug' => 'other-cup', 'timezone' => 'Asia/Karachi', 'status' => 'ready', 'squad_size' => 3, 'default_pick_duration' => 60]);
        $foreignTeam = Team::create(['tournament_id' => $otherTournament->id, 'name' => 'Foreign XI', 'short_name' => 'FX', 'display_order' => 1, 'is_active' => true]);

        $this->actingAs($admin)->from(route('admin.tournaments.fixtures.create', $tournament))
            ->post(route('admin.tournaments.fixtures.store', $tournament), [
                'home_team_id' => $foreignTeam->id,
                'away_team_id' => $away->id,
                'scheduled_at' => '2026-09-01T18:00',
                'timezone' => 'Asia/Karachi',
            ])->assertSessionHasErrors('teams');

        $this->assertDatabaseCount('fixtures', 0);
    }

    public function test_admin_can_postpone_and_reschedule_a_fixture(): void
    {
        [$admin, $tournament, $home, $away] = $this->tournamentWithTeams();
        $fixture = Fixture::create(['tournament_id' => $tournament->id, 'home_team_id' => $home->id, 'away_team_id' => $away->id, 'scheduled_at' => now()->addDay(), 'timezone' => 'Asia/Karachi', 'status' => 'scheduled', 'created_by' => $admin->id]);

        $this->actingAs($admin)->post(route('admin.tournaments.fixtures.status', [$tournament, $fixture]), ['status' => 'postponed'])->assertRedirect();
        $this->assertSame('postponed', $fixture->fresh()->status);
        $this->actingAs($admin)->post(route('admin.tournaments.fixtures.status', [$tournament, $fixture]), ['status' => 'scheduled'])->assertRedirect();
        $this->assertSame('scheduled', $fixture->fresh()->status);
    }

    public function test_admin_can_create_an_operational_match_from_a_fixture(): void
    {
        [$admin, $tournament, $home, $away] = $this->completedDraftTournament();
        $fixture = Fixture::create(['tournament_id' => $tournament->id, 'home_team_id' => $home->id, 'away_team_id' => $away->id, 'scheduled_at' => now()->addDay(), 'timezone' => 'Asia/Karachi', 'status' => 'scheduled', 'created_by' => $admin->id]);

        $this->actingAs($admin)->post(route('admin.tournaments.fixtures.create-match', [$tournament, $fixture]))
            ->assertRedirect();

        $this->assertSame('in_progress', $fixture->fresh()->status);
        $this->assertDatabaseHas('matches', ['fixture_id' => $fixture->id, 'tournament_id' => $tournament->id, 'status' => 'squad_selection']);
        $this->assertDatabaseCount('match_players', 4);
    }

    public function test_two_user_matrix_rejects_cross_owner_fixture_mutations_and_nested_ids(): void
    {
        [$owner, $tournament, $home, $away] = $this->tournamentWithTeams();
        $tournament->update(['creator_id' => $owner->id]);
        $fixture = Fixture::create([
            'tournament_id' => $tournament->id,
            'home_team_id' => $home->id,
            'away_team_id' => $away->id,
            'scheduled_at' => now()->addDay(),
            'timezone' => 'Asia/Karachi',
            'status' => 'scheduled',
            'created_by' => $owner->id,
        ]);
        [$foreignOwner, $foreignTournament, $foreignHome, $foreignAway] = $this->tournamentWithTeams();
        $foreignTournament->update(['creator_id' => $foreignOwner->id]);
        $foreignFixture = Fixture::create([
            'tournament_id' => $foreignTournament->id,
            'home_team_id' => $foreignHome->id,
            'away_team_id' => $foreignAway->id,
            'scheduled_at' => now()->addDays(2),
            'timezone' => 'Asia/Karachi',
            'status' => 'scheduled',
            'created_by' => $foreignOwner->id,
        ]);
        $attacker = User::factory()->create();
        $attacker->assignRole('admin');

        $this->actingAs($attacker)
            ->put(route('admin.tournaments.fixtures.update', [$tournament, $fixture]), [])
            ->assertForbidden();
        $this->actingAs($attacker)
            ->post(route('admin.tournaments.fixtures.status', [$tournament, $fixture]), ['status' => 'postponed'])
            ->assertForbidden();
        $this->actingAs($attacker)
            ->post(route('admin.tournaments.fixtures.create-match', [$tournament, $fixture]))
            ->assertForbidden();

        $token = $attacker->createToken('fixture-attack')->plainTextToken;
        $headers = ['Authorization' => 'Bearer '.$token];
        $this->putJson("/api/v1/admin/tournaments/{$tournament->slug}/fixtures/{$fixture->id}", [], $headers)->assertForbidden();
        $this->postJson("/api/v1/admin/tournaments/{$tournament->slug}/fixtures/{$fixture->id}/status", ['status' => 'postponed'], $headers)->assertForbidden();
        $this->postJson("/api/v1/admin/tournaments/{$tournament->slug}/fixtures/{$fixture->id}/create-match", [], $headers)->assertForbidden();
        $this->deleteJson("/api/v1/admin/tournaments/{$tournament->slug}/fixtures/{$fixture->id}", [], $headers)->assertForbidden();

        // Owner's parent ID combined with another owner's child must never pass.
        $this->app['auth']->forgetGuards();
        $ownerToken = $owner->createToken('nested-id-owner')->plainTextToken;
        $this->putJson("/api/v1/admin/tournaments/{$tournament->slug}/fixtures/{$foreignFixture->id}", [], [
            'Authorization' => 'Bearer '.$ownerToken,
        ])->assertNotFound();

        $this->assertSame('scheduled', $fixture->fresh()->status);
        $this->assertSame('scheduled', $foreignFixture->fresh()->status);
        $this->assertDatabaseCount('matches', 0);
    }

    public function test_super_admin_cross_owner_override_is_allowed_and_audited(): void
    {
        [$owner, $tournament, $home, $away] = $this->tournamentWithTeams();
        $tournament->update(['creator_id' => $owner->id]);
        $fixture = Fixture::create([
            'tournament_id' => $tournament->id,
            'home_team_id' => $home->id,
            'away_team_id' => $away->id,
            'scheduled_at' => now()->addDay(),
            'timezone' => 'Asia/Karachi',
            'status' => 'scheduled',
            'created_by' => $owner->id,
        ]);
        $superAdmin = User::factory()->create();
        $superAdmin->assignRole('super_admin');

        $this->actingAs($superAdmin)
            ->post(route('admin.tournaments.fixtures.status', [$tournament, $fixture]), ['status' => 'postponed'])
            ->assertRedirect();

        $this->assertSame('postponed', $fixture->fresh()->status);
        $this->assertDatabaseHas('audit_logs', [
            'user_id' => $superAdmin->id,
            'action' => 'super_admin.ownership_override',
            'auditable_type' => Fixture::class,
            'auditable_id' => $fixture->id,
        ]);
    }

    private function tournamentWithTeams(): array
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $tournament = Tournament::create(['name' => 'Fixture Cup', 'slug' => 'fixture-cup-'.uniqid(), 'timezone' => 'Asia/Karachi', 'status' => 'ready', 'squad_size' => 3, 'default_pick_duration' => 60, 'creator_id' => $admin->id]);
        $home = Team::create(['tournament_id' => $tournament->id, 'name' => 'Ali Panthers', 'short_name' => 'AP', 'display_order' => 1, 'is_active' => true]);
        $away = Team::create(['tournament_id' => $tournament->id, 'name' => 'Lahore Lions', 'short_name' => 'LL', 'display_order' => 2, 'is_active' => true]);
        $tournament->teams()->attach([$home->id, $away->id]);
        return [$admin, $tournament, $home, $away];
    }

    private function completedDraftTournament(): array
    {
        [$admin, $tournament, $home, $away] = $this->tournamentWithTeams();
        $profile = CricketRuleProfile::query()->firstOrFail();
        $profile->update(['playing_xi_size' => 2, 'maximum_wickets' => 1]);
        $tournament->update(['cricket_rule_profile_id' => $profile->id, 'has_draft' => true]);
        $players = [];
        foreach (['Salman Khan', 'Hamza Ali', 'Usman Shah', 'Bilal Ahmed'] as $index => $name) {
            $user = User::factory()->create(['name' => $name]);
            $user->assignRole('player');
            $user->playerProfile()->update(['full_name' => $name, 'playing_role' => $index === 0 ? 'Batter' : 'Bowler']);
            $profile = $user->playerProfile()->firstOrFail();
            $players[] = TournamentPlayer::create(['tournament_id' => $tournament->id, 'player_profile_id' => $profile->id, 'status' => 'approved']);
        }
        $draft = Draft::create(['tournament_id' => $tournament->id, 'status' => 'completed', 'completed_at' => now()]);
        $round = DraftRound::create(['draft_id' => $draft->id, 'round_number' => 1, 'name' => 'Round 1', 'status' => 'completed']);
        foreach ([[$home, $players[0]], [$home, $players[1]], [$away, $players[2]], [$away, $players[3]]] as $index => [$team, $player]) {
            DraftPick::create(['draft_id' => $draft->id, 'draft_round_id' => $round->id, 'team_id' => $team->id, 'pick_number' => $index + 1, 'pick_duration' => 60, 'status' => 'selected', 'tournament_player_id' => $player->id, 'selected_at' => now()]);
        }
        return [$admin, $tournament->fresh(), $home, $away];
    }
}
