<?php

namespace Tests\Feature\Admin;

use App\Models\CricketRuleProfile;
use App\Models\Draft;
use App\Models\DraftPick;
use App\Models\DraftRound;
use App\Models\Tournament;
use App\Models\User;
use Database\Seeders\CricketRuleProfileSeeder;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class TournamentTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([RolePermissionSeeder::class, CricketRuleProfileSeeder::class]);
    }

    public function test_non_admin_cannot_open_admin_dashboard(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)
            ->get(route('admin.dashboard'))
            ->assertForbidden();
    }

    public function test_admin_can_create_a_tournament(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');

        $response = $this->actingAs($admin)->post(route('admin.tournaments.store'), [
            'name' => 'Summer Cricket Cup',
            'season_name' => '2026 Season',
            'slug' => 'summer-cricket-cup',
            'description' => 'The first local tournament.',
            'location' => 'Lahore',
            'venue' => 'Gaddafi Stadium',
            'city' => 'Lahore',
            'timezone' => 'Asia/Karachi',
            'starts_on' => '2026-09-01',
            'ends_on' => '2026-09-07',
            'registration_opens_at' => '2026-08-20 09:00',
            'registration_closes_at' => '2026-08-28 23:59',
            'is_public' => 1,
            'squad_size' => 3,
            'default_pick_duration' => 60,
            'default_overs_per_innings' => 15,
        ]);

        $tournament = Tournament::query()->where('slug', 'summer-cricket-cup')->first();

        $response->assertRedirect(route('admin.tournaments.show', $tournament));
        $this->assertNotNull($tournament);
        $this->assertSame('draft', $tournament->status);
        $this->assertSame('2026 Season', $tournament->season_name);
        $this->assertSame('Gaddafi Stadium', $tournament->venue);
        $this->assertSame('Lahore', $tournament->city);
        $this->assertTrue($tournament->is_public);
        $this->assertSame(15, $tournament->default_overs_per_innings);
    }

    public function test_admin_can_create_a_tournament_with_a_rule_profile(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();

        $this->actingAs($admin)->post(route('admin.tournaments.store'), [
            'name' => 'Rules Cup',
            'season_name' => '2026 Season',
            'slug' => 'rules-cup',
            'description' => 'Rules profile test tournament.',
            'location' => 'Lahore',
            'venue' => 'Test Ground',
            'city' => 'Lahore',
            'timezone' => 'Asia/Karachi',
            'squad_size' => 11,
            'default_pick_duration' => 60,
            'cricket_rule_profile_id' => $profile->id,
        ])->assertRedirect();

        $this->assertDatabaseHas('tournaments', [
            'slug' => 'rules-cup',
            'cricket_rule_profile_id' => $profile->id,
        ]);
    }

    public function test_admin_tournament_form_shows_active_rule_profiles(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');

        $this->actingAs($admin)
            ->get(route('admin.tournaments.create'))
            ->assertOk()
            ->assertSee('Standard T20')
            ->assertSee('Community 10 Over');
    }

    public function test_rule_profile_is_locked_after_draft_setup_begins(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();
        $alternate = CricketRuleProfile::query()->where('slug', 'community-10-over')->firstOrFail();
        $tournament = $this->createTournament('live', $admin);
        $tournament->update(['cricket_rule_profile_id' => $profile->id]);
        Draft::create(['tournament_id' => $tournament->id, 'status' => 'live', 'revision' => 1]);

        $this->actingAs($admin)->put(route('admin.tournaments.update', $tournament), [
            'name' => $tournament->name,
            'season_name' => $tournament->season_name,
            'slug' => $tournament->slug,
            'description' => $tournament->description,
            'location' => $tournament->location,
            'venue' => $tournament->venue,
            'city' => $tournament->city,
            'timezone' => $tournament->timezone ?: 'Asia/Karachi',
            'squad_size' => $tournament->squad_size,
            'default_pick_duration' => $tournament->default_pick_duration,
            'cricket_rule_profile_id' => $alternate->id,
        ])->assertSessionHasErrors('cricket_rule_profile_id');

        $this->assertSame($profile->id, $tournament->fresh()->cricket_rule_profile_id);
    }

    public function test_admin_can_update_professional_tournament_configuration(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $tournament = $this->createTournament('draft', $admin);

        $this->actingAs($admin)->put(route('admin.tournaments.update', $tournament), [
            'name' => 'Updated Cup',
            'season_name' => 'Season 2',
            'slug' => $tournament->slug,
            'description' => 'Updated description',
            'location' => 'Punjab',
            'venue' => 'National Stadium',
            'city' => 'Karachi',
            'timezone' => 'Asia/Karachi',
            'starts_on' => '2026-10-01',
            'ends_on' => '2026-10-05',
            'registration_opens_at' => '2026-09-01 09:00',
            'registration_closes_at' => '2026-09-20 23:59',
            'is_public' => 0,
            'squad_size' => 4,
            'default_pick_duration' => 90,
            'default_overs_per_innings' => 10,
        ])->assertRedirect();

        $updated = $tournament->fresh();
        $this->assertSame('Updated Cup', $updated->name);
        $this->assertSame('Season 2', $updated->season_name);
        $this->assertSame('National Stadium', $updated->venue);
        $this->assertSame('Karachi', $updated->city);
        $this->assertFalse($updated->is_public);
        $this->assertSame(10, $updated->default_overs_per_innings);
        $this->assertDatabaseHas('audit_logs', [
            'tournament_id' => $updated->id,
            'action' => 'tournament.configuration_updated',
        ]);
    }

    public function test_admin_workspace_shows_complete_move_tournament_to_options(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $tournament = $this->createTournament('draft', $admin);

        $this->actingAs($admin)->get(route('admin.tournaments.show', $tournament))
            ->assertOk()
            ->assertSee('Move tournament to')
            ->assertSee('Registration open')
            ->assertSee('Ready for draft')
            ->assertSee('Live / draft started')
            ->assertSee('Completed')
            ->assertSee('Cancelled');
    }

    public function test_admin_can_move_tournament_through_registration_and_ready_states(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $tournament = $this->createTournament('draft', $admin);

        $this->actingAs($admin)->post(route('admin.tournaments.status.transition', $tournament), ['status' => 'registration'])
            ->assertRedirect();
        $this->assertSame('registration', $tournament->fresh()->status);

        $this->actingAs($admin)->post(route('admin.tournaments.status.transition', $tournament), ['status' => 'ready'])
            ->assertRedirect();

        $this->assertSame('ready', $tournament->fresh()->status);
        $this->assertDatabaseHas('audit_logs', [
            'tournament_id' => $tournament->id,
            'action' => 'tournament.status_changed',
        ]);
    }

    public function test_admin_can_move_directly_to_live_without_draft_prerequisite(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $tournament = $this->createTournament('ready', $admin);

        $response = $this->actingAs($admin)->from(route('admin.tournaments.show', $tournament))
            ->post(route('admin.tournaments.status.transition', $tournament), ['status' => 'live']);

        $response->assertRedirect();
        $this->assertSame('live', $tournament->fresh()->status);

        $draft = Draft::create(['tournament_id' => $tournament->id, 'status' => 'setup', 'revision' => 0]);
        $round = DraftRound::create([
            'draft_id' => $draft->id,
            'round_number' => 1,
            'status' => 'pending',
        ]);
        DraftPick::create([
            'draft_id' => $draft->id,
            'draft_round_id' => $round->id,
            'team_id' => $tournament->teams()->create([
                'name' => 'Lifecycle Team',
                'short_name' => 'LT',
                'display_order' => 1,
            ])->id,
            'pick_number' => 1,
            'pick_duration' => 60,
            'status' => 'pending',
        ]);
        $this->actingAs($admin)->post(route('admin.tournaments.status.transition', $tournament), ['status' => 'completed'])
            ->assertRedirect();

        $this->assertSame('completed', $tournament->fresh()->status);
    }

    public function test_admin_can_view_locked_draft_setup_but_cannot_update_it(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $tournament = $this->createTournament('live', $admin);
        $tournament->update(['has_draft' => true]);
        $team = $tournament->teams()->create([
            'name' => 'Locked Team',
            'short_name' => 'LT',
            'display_order' => 1,
        ]);
        $draft = Draft::create([
            'tournament_id' => $tournament->id,
            'status' => 'live',
            'current_pick_number' => 1,
            'pick_duration' => 60,
            'revision' => 1,
        ]);
        $round = DraftRound::create(['draft_id' => $draft->id, 'round_number' => 1, 'status' => 'active']);
        DraftPick::create([
            'draft_id' => $draft->id,
            'draft_round_id' => $round->id,
            'team_id' => $team->id,
            'pick_number' => 1,
            'pick_duration' => 60,
            'status' => 'active',
        ]);

        $this->actingAs($admin)
            ->get(route('admin.tournaments.draft.setup', $tournament))
            ->assertOk()
            ->assertSee('Read-only · locked')
            ->assertSee('Configured rounds and picks')
            ->assertDontSee('Save draft setup');

        $this->actingAs($admin)
            ->put(route('admin.tournaments.draft.setup.update', $tournament), [
                'rounds' => [[
                    'round_number' => 1,
                    'name' => 'Round 1',
                    'picks' => [[
                        'pick_number' => 1,
                        'team_id' => $team->id,
                        'pick_duration' => 60,
                    ]],
                ]],
            ])
            ->assertSessionHasErrors('draft');
    }

    public function test_admin_can_move_from_any_lifecycle_status_to_any_other_status(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $tournament = $this->createTournament('draft', $admin);

        $response = $this->actingAs($admin)->post(route('admin.tournaments.status.transition', $tournament), ['status' => 'completed']);

        $response->assertRedirect();
        $this->assertSame('completed', $tournament->fresh()->status);
        $this->assertDatabaseHas('audit_logs', [
            'tournament_id' => $tournament->id,
            'action' => 'tournament.status_changed',
        ]);
    }

    public function test_another_admin_cannot_manage_or_list_the_owners_tournament(): void
    {
        $owner = User::factory()->create();
        $owner->assignRole('admin');
        $other = User::factory()->create();
        $other->assignRole('admin');
        $tournament = $this->createTournament('draft', $owner);

        $this->actingAs($other)
            ->get(route('admin.tournaments.show', $tournament))
            ->assertForbidden();
        $this->actingAs($other)
            ->get(route('admin.tournaments.fixtures.index', $tournament))
            ->assertForbidden();
        $this->actingAs($other)
            ->post(route('admin.tournaments.status.transition', $tournament), ['status' => 'live'])
            ->assertForbidden();
        $this->actingAs($other)
            ->get(route('admin.tournaments.index'))
            ->assertOk()
            ->assertDontSee($tournament->name);

        $token = $other->createToken('cross-owner-test')->plainTextToken;
        $this->withToken($token)
            ->postJson("/api/v1/admin/tournaments/{$tournament->slug}/status", ['status' => 'live'])
            ->assertForbidden();
        $this->withToken($token)
            ->getJson("/api/v1/admin/tournaments/{$tournament->slug}/teams")
            ->assertForbidden();

        $this->assertSame('draft', $tournament->fresh()->status);
    }

    private function createTournament(string $status, User $owner): Tournament
    {
        return Tournament::create([
            'name' => 'Lifecycle Cup '.uniqid(),
            'slug' => 'lifecycle-cup-'.uniqid(),
            'status' => $status,
            'squad_size' => 3,
            'default_pick_duration' => 60,
            'creator_id' => $owner->id,
        ]);
    }
}
