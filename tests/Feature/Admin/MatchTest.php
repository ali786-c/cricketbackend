<?php

namespace Tests\Feature\Admin;

use App\Models\CricketMatch;
use App\Models\CricketRuleProfile;
use App\Models\Draft;
use App\Models\DraftPick;
use App\Models\DraftRound;
use App\Models\PlayerProfile;
use App\Models\Team;
use App\Models\Tournament;
use App\Models\TournamentPlayer;
use App\Models\User;
use Database\Seeders\CricketRuleProfileSeeder;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class MatchTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([RolePermissionSeeder::class, CricketRuleProfileSeeder::class]);
    }

    public function test_admin_can_create_match_from_completed_draft_with_snapshots(): void
    {
        [$admin, $tournament, $teams] = $this->completedDraftTournament();

        $this->actingAs($admin)->post(route('admin.tournaments.matches.store', $tournament), [
            'home_team_id' => $teams[0]->id,
            'away_team_id' => $teams[1]->id,
        ])->assertRedirect();

        $match = CricketMatch::query()->firstOrFail();
        $this->assertSame('squad_selection', $match->status);
        $this->assertSame('t20-standard', $match->ruleProfile->slug);
        $this->assertSame(14, $match->overs_per_innings);
        $this->assertSame(4, $match->players()->count());
        $this->assertDatabaseHas('match_players', [
            'match_id' => $match->id,
            'team_id' => $teams[0]->id,
            'player_name_snapshot' => 'Player 1',
            'player_role_snapshot' => 'Batter',
            'selection_type' => 'squad',
        ]);
    }

    public function test_admin_can_update_pre_live_overs_but_not_after_match_start(): void
    {
        [$admin, $tournament, $teams] = $this->completedDraftTournament();
        $this->actingAs($admin)->post(route('admin.tournaments.matches.store', $tournament), [
            'home_team_id' => $teams[0]->id,
            'away_team_id' => $teams[1]->id,
        ]);
        $match = CricketMatch::query()->firstOrFail();

        $this->actingAs($admin)->post(route('admin.tournaments.matches.overs', [$tournament, $match]), ['overs_per_innings' => 6])->assertRedirect();
        $this->assertSame(6, $match->fresh()->overs_per_innings);
        $this->assertDatabaseHas('audit_logs', ['action' => 'match.overs_updated', 'auditable_id' => $match->id]);

        $match->update(['status' => 'live']);
        $this->actingAs($admin)->from(route('admin.tournaments.matches.show', [$tournament, $match]))
            ->post(route('admin.tournaments.matches.overs', [$tournament, $match]), ['overs_per_innings' => 10])
            ->assertSessionHasErrors('overs_per_innings');
        $this->assertSame(6, $match->fresh()->overs_per_innings);
    }

    public function test_match_creation_rejects_an_incomplete_draft(): void
    {
        [$admin, $tournament, $teams] = $this->completedDraftTournament();
        $tournament->draft()->update(['status' => 'live']);
        $tournament->draft->picks()->latest('pick_number')->first()->update(['status' => 'pending', 'tournament_player_id' => null]);

        $this->actingAs($admin)->from(route('admin.tournaments.matches.create', $tournament))
            ->post(route('admin.tournaments.matches.store', $tournament), [
                'home_team_id' => $teams[0]->id,
                'away_team_id' => $teams[1]->id,
            ])->assertSessionHasErrors('match');

        $this->assertDatabaseCount('matches', 0);
    }

    public function test_admin_can_submit_playing_xis_approve_lineups_and_record_toss(): void
    {
        [$admin, $tournament, $teams] = $this->completedDraftTournament();
        $this->actingAs($admin)->post(route('admin.tournaments.matches.store', $tournament), [
            'home_team_id' => $teams[0]->id,
            'away_team_id' => $teams[1]->id,
            'overs_per_innings' => 8,
        ]);
        $match = CricketMatch::query()->firstOrFail();
        $this->assertSame(8, $match->overs_per_innings);
        $players = $match->players()->get()->groupBy('team_id');

        $this->actingAs($admin)->post(route('admin.tournaments.matches.playing-xi', [$tournament, $match, $teams[0]->id]), [
            'player_ids' => $players[$teams[0]->id]->pluck('id')->all(),
        ])->assertRedirect();
        $this->actingAs($admin)->post(route('admin.tournaments.matches.playing-xi', [$tournament, $match, $teams[1]->id]), [
            'player_ids' => $players[$teams[1]->id]->pluck('id')->all(),
        ])->assertRedirect();
        $this->assertSame('lineup_pending', $match->fresh()->status);

        $this->actingAs($admin)->post(route('admin.tournaments.matches.approve-lineup', [$tournament, $match]))
            ->assertRedirect();
        $this->assertSame('toss_pending', $match->fresh()->status);
        $this->assertSame(4, $match->fresh()->players()->where('selection_type', 'playing_xi')->whereNotNull('approved_at')->count());

        $this->actingAs($admin)->post(route('admin.tournaments.matches.toss', [$tournament, $match]), [
            'toss_winner_team_id' => $teams[0]->id,
            'toss_decision' => 'bat',
        ])->assertRedirect();

        $live = $match->fresh();
        $this->assertSame('live', $live->status);
        $this->assertSame($teams[0]->id, $live->toss_winner_team_id);
        $this->assertSame('bat', $live->toss_decision);
        $this->assertSame(8, $live->innings()->first()->maximum_overs);
    }

    private function completedDraftTournament(): array
    {
        $admin = User::factory()->create(['password' => Hash::make('password')]);
        $admin->assignRole('admin');
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();
        $profile->update(['playing_xi_size' => 2]);

        $tournament = Tournament::create([
            'name' => 'Match Cup',
            'slug' => 'match-cup-'.uniqid(),
            'status' => 'completed',
            'timezone' => 'Asia/Karachi',
            'squad_size' => 2,
            'default_pick_duration' => 60,
            'default_overs_per_innings' => 14,
            'cricket_rule_profile_id' => $profile->id,
            'has_draft' => true,
            'creator_id' => $admin->id,
        ]);
        $teams = collect([
            Team::create(['tournament_id' => $tournament->id, 'name' => 'Alpha XI', 'short_name' => 'AX', 'display_order' => 1]),
            Team::create(['tournament_id' => $tournament->id, 'name' => 'Bravo XI', 'short_name' => 'BX', 'display_order' => 2]),
        ]);
        $tournament->teams()->attach($teams->pluck('id'));
        $draft = Draft::create(['tournament_id' => $tournament->id, 'status' => 'completed', 'revision' => 9, 'completed_at' => now()]);
        $round = DraftRound::create(['draft_id' => $draft->id, 'round_number' => 1, 'status' => 'completed', 'completed_at' => now()]);
        $pickNumber = 1;
        foreach ($teams as $teamIndex => $team) {
            for ($slot = 1; $slot <= 2; $slot++) {
                $user = User::factory()->create(['name' => 'Player '.$pickNumber]);
                $user->playerProfile()->update(['full_name' => 'Player '.$pickNumber, 'playing_role' => 'Batter']);
                $player = $user->playerProfile()->firstOrFail();
                $tournamentPlayer = TournamentPlayer::create(['tournament_id' => $tournament->id, 'player_profile_id' => $player->id, 'status' => 'approved', 'reviewed_by' => $admin->id, 'reviewed_at' => now()]);
                DraftPick::create(['draft_id' => $draft->id, 'draft_round_id' => $round->id, 'team_id' => $team->id, 'pick_number' => $pickNumber++, 'pick_duration' => 60, 'status' => 'selected', 'tournament_player_id' => $tournamentPlayer->id, 'selected_by' => $admin->id, 'selected_at' => now()]);
            }
        }
        return [$admin, $tournament, $teams];
    }
}
