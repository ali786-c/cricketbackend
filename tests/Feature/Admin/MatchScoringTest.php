<?php

namespace Tests\Feature\Admin;

use App\Models\CricketMatch;
use App\Models\CricketRuleProfile;
use App\Models\InningsBattingStat;
use App\Models\MatchInnings;
use App\Models\MatchPlayer;
use App\Models\PlayerProfile;
use App\Models\Team;
use App\Models\Tournament;
use App\Models\TournamentPlayer;
use App\Models\User;
use Database\Seeders\CricketRuleProfileSeeder;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MatchScoringTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([RolePermissionSeeder::class, CricketRuleProfileSeeder::class]);
    }

    public function test_delivery_ledger_derives_total_runs_and_batting_stats(): void
    {
        [$admin, $match, $innings, $batters, $bowler] = $this->liveMatch();
        $this->actingAs($admin)->post(route('admin.matches.scorer.deliveries.store', $match), [
            'striker_id' => $batters[0]->id,
            'non_striker_id' => $batters[1]->id,
            'bowler_id' => $bowler->id,
            'runs_off_bat' => 4,
            'expected_revision' => $match->revision,
        ])->assertRedirect();

        $this->assertDatabaseHas('match_deliveries', ['innings_id' => $innings->id, 'runs_off_bat' => 4, 'total_runs' => 4, 'is_legal_delivery' => 1]);
        $this->assertSame(4, $innings->fresh()->total_runs);
        $this->assertSame(1, $innings->fresh()->legal_balls);
        $this->assertSame(4, InningsBattingStat::query()->where('innings_id', $innings->id)->where('match_player_id', $batters[0]->id)->value('runs'));
        $this->assertSame($batters[0]->id, $innings->fresh()->current_striker_id);
        $this->assertSame($batters[1]->id, $innings->fresh()->current_non_striker_id);
        $this->assertSame($bowler->id, $innings->fresh()->current_bowler_id);
    }

    public function test_active_batters_rotate_and_undo_restores_pre_delivery_state(): void
    {
        [$admin, $match, $innings, $batters, $bowler] = $this->liveMatch();

        $this->actingAs($admin)->post(route('admin.matches.scorer.deliveries.store', $match), [
            'striker_id' => $batters[0]->id,
            'non_striker_id' => $batters[1]->id,
            'bowler_id' => $bowler->id,
            'runs_off_bat' => 1,
            'expected_revision' => $match->revision,
        ])->assertRedirect();

        $afterRun = $innings->fresh();
        $this->assertSame($batters[1]->id, $afterRun->current_striker_id);
        $this->assertSame($batters[0]->id, $afterRun->current_non_striker_id);

        $this->actingAs($admin)->post(route('admin.matches.scorer.undo', $match), [
            'reason' => 'Restore active players',
        ])->assertRedirect();

        $afterUndo = $innings->fresh();
        $this->assertSame($batters[0]->id, $afterUndo->current_striker_id);
        $this->assertSame($batters[1]->id, $afterUndo->current_non_striker_id);
        $this->assertSame($bowler->id, $afterUndo->current_bowler_id);
    }

    public function test_mobile_selected_openers_are_accepted_on_the_first_delivery(): void
    {
        [$admin, $match, $innings, $batters, $bowler] = $this->liveMatch();
        $innings->update([
            'current_striker_id' => $batters[0]->id,
            'current_non_striker_id' => $batters[1]->id,
        ]);

        app(\App\Modules\Scoring\Services\MatchScoringService::class)->recordDelivery($match, [
            'striker_id' => $batters[1]->id,
            'non_striker_id' => $batters[0]->id,
            'bowler_id' => $bowler->id,
            'runs_off_bat' => 2,
        ], $admin->id, $match->revision);

        $this->assertSame(2, $innings->fresh()->total_runs);
        $this->assertDatabaseHas('match_deliveries', [
            'innings_id' => $innings->id,
            'striker_id' => $batters[1]->id,
            'non_striker_id' => $batters[0]->id,
        ]);
    }

    public function test_wide_adds_runs_without_a_legal_ball(): void
    {
        [$admin, $match, $innings, $batters, $bowler] = $this->liveMatch();
        $this->actingAs($admin)->post(route('admin.matches.scorer.deliveries.store', $match), [
            'striker_id' => $batters[0]->id, 'non_striker_id' => $batters[1]->id, 'bowler_id' => $bowler->id,
            'wides' => 1, 'expected_revision' => $match->revision,
        ])->assertRedirect();

        $delivery = $match->deliveries()->latest('id')->first();
        $this->assertFalse($delivery->is_legal_delivery);
        $this->assertSame(1, $innings->fresh()->total_runs);
        $this->assertSame(0, $innings->fresh()->legal_balls);
    }

    public function test_bowled_dismissal_is_rejected_on_a_no_ball(): void
    {
        [$admin, $match, $innings, $batters, $bowler] = $this->liveMatch();
        $this->actingAs($admin)->from(route('admin.matches.scorer', $match))->post(route('admin.matches.scorer.deliveries.store', $match), [
            'striker_id' => $batters[0]->id, 'non_striker_id' => $batters[1]->id, 'bowler_id' => $bowler->id,
            'no_balls' => 1, 'wicket' => ['dismissed_player_id' => $batters[0]->id, 'dismissal_type' => 'bowled'],
            'expected_revision' => $match->revision,
        ])->assertSessionHasErrors('wicket.dismissal_type');
        $this->assertDatabaseCount('match_deliveries', 0);
    }

    public function test_next_innings_inherits_the_match_specific_overs_limit(): void
    {
        [$admin, $match, $innings] = $this->liveMatch();
        $match->update(['overs_per_innings' => 8]);
        $innings->update(['maximum_overs' => 8, 'status' => 'completed']);

        $next = app(\App\Modules\Scoring\Services\MatchScoringService::class)->startNextInnings($match->fresh(), $admin->id);

        $this->assertSame(2, $next->innings_number);
        $this->assertSame(8, $next->maximum_overs);
    }

    public function test_stale_revision_is_rejected_and_undo_voids_last_delivery(): void
    {
        [$admin, $match, $innings, $batters, $bowler] = $this->liveMatch();
        $payload = ['striker_id' => $batters[0]->id, 'non_striker_id' => $batters[1]->id, 'bowler_id' => $bowler->id, 'runs_off_bat' => 1, 'expected_revision' => $match->revision];
        $this->actingAs($admin)->post(route('admin.matches.scorer.deliveries.store', $match), $payload)->assertRedirect();
        $this->actingAs($admin)->from(route('admin.matches.scorer', $match))->post(route('admin.matches.scorer.deliveries.store', $match), $payload)->assertSessionHasErrors('revision');
        $this->actingAs($admin)->post(route('admin.matches.scorer.undo', $match), ['reason' => 'Corrected scorer mistake'])->assertRedirect();
        $this->assertNotNull($match->deliveries()->latest('id')->first()->fresh()->voided_at);
        $this->assertSame(0, $innings->fresh()->total_runs);
    }

    public function test_another_admin_cannot_score_or_change_the_match_through_api_or_web(): void
    {
        [$owner, $match, $innings, $batters, $bowler] = $this->liveMatch();

        $this->actingAs($owner)->post(route('admin.matches.scorer.deliveries.store', $match), [
            'striker_id' => $batters[0]->id,
            'non_striker_id' => $batters[1]->id,
            'bowler_id' => $bowler->id,
            'runs_off_bat' => 1,
            'expected_revision' => $match->revision,
        ])->assertRedirect();

        $match = $match->fresh();
        $delivery = $match->deliveries()->latest('id')->firstOrFail();
        $deliveryCount = $match->deliveries()->count();
        $revision = $match->revision;
        $runs = $innings->fresh()->total_runs;

        $other = User::factory()->create();
        $other->assignRole('admin');
        $this->actingAs($other);

        $deliveryPayload = [
            'striker_id' => $batters[0]->id,
            'non_striker_id' => $batters[1]->id,
            'bowler_id' => $bowler->id,
            'runs_off_bat' => 4,
            'expected_revision' => $revision,
        ];

        $this->postJson("/api/v1/matches/{$match->id}/deliveries", $deliveryPayload)->assertForbidden();
        $this->postJson("/api/v1/matches/{$match->id}/deliveries/sync", [])->assertForbidden();
        $this->patchJson("/api/v1/deliveries/{$delivery->id}", ['runs_off_bat' => 6])->assertForbidden();
        $this->postJson("/api/v1/matches/{$match->id}/next-innings")->assertForbidden();
        $this->postJson("/api/v1/matches/{$match->id}/result/submit")->assertForbidden();
        $this->postJson("/api/v1/matches/{$match->id}/undo", ['reason' => 'Unauthorized correction'])->assertForbidden();
        $tournamentSlug = $match->tournament->slug;
        $this->patchJson("/api/v1/admin/tournaments/{$tournamentSlug}/matches/{$match->id}/overs", ['overs_per_innings' => 6])->assertForbidden();
        $this->postJson("/api/v1/admin/tournaments/{$tournamentSlug}/matches/{$match->id}/teams/{$batters[0]->team_id}/playing-xi", ['player_ids' => []])->assertForbidden();
        $this->postJson("/api/v1/admin/tournaments/{$tournamentSlug}/matches/{$match->id}/approve-lineup")->assertForbidden();
        $this->postJson("/api/v1/admin/tournaments/{$tournamentSlug}/matches/{$match->id}/toss", ['winner_team_id' => 0, 'decision' => 'bat'])->assertForbidden();
        $this->postJson("/api/v1/admin/matches/{$match->id}/result/submit")->assertForbidden();
        $this->postJson("/api/v1/admin/matches/{$match->id}/result/approve")->assertForbidden();

        $this->get(route('admin.matches.scorer', $match))->assertForbidden();
        $this->post(route('admin.matches.scorer.deliveries.store', $match), $deliveryPayload)->assertForbidden();
        $this->post(route('admin.matches.scorer.next-innings', $match))->assertForbidden();
        $this->post(route('admin.matches.scorer.undo', $match), ['reason' => 'Unauthorized correction'])->assertForbidden();
        $this->post(route('admin.matches.result.submit', $match))->assertForbidden();
        $this->post(route('admin.matches.result.approve', $match))->assertForbidden();
        $this->post(route('admin.tournaments.matches.overs', [$match->tournament, $match]), ['overs_per_innings' => 6])->assertForbidden();
        $this->post(route('admin.tournaments.matches.approve-lineup', [$match->tournament, $match]))->assertForbidden();
        $this->post(route('admin.tournaments.matches.toss', [$match->tournament, $match]), ['winner_team_id' => 0, 'decision' => 'bat'])->assertForbidden();

        $this->getJson("/api/v1/admin/tournaments/{$match->tournament->slug}/matches")
            ->assertForbidden();

        $this->postJson("/api/v1/admin/tournaments/0/matches/{$match->id}/approve-lineup")
            ->assertNotFound();

        $this->assertSame($deliveryCount, $match->deliveries()->count());
        $this->assertSame($revision, $match->fresh()->revision);
        $this->assertSame($runs, $innings->fresh()->total_runs);
        $this->assertSame(1, $delivery->fresh()->runs_off_bat);
    }

    public function test_offline_idempotency_key_is_bound_to_its_original_match_and_actor(): void
    {
        [$owner, $match, $innings, $batters, $bowler] = $this->liveMatch();
        $uuid = (string) \Illuminate\Support\Str::uuid();
        $delivery = [
            'local_uuid' => $uuid,
            'local_sequence' => 1,
            'device_timestamp' => now()->toISOString(),
            'striker_id' => $batters[0]->id,
            'non_striker_id' => $batters[1]->id,
            'bowler_id' => $bowler->id,
            'runs_off_bat' => 1,
        ];

        $this->actingAs($owner)->postJson("/api/v1/matches/{$match->id}/deliveries/sync", [
            'device_id' => 'owner-device',
            'base_revision' => $match->revision,
            'deliveries' => [$delivery],
        ])->assertOk()->assertJsonPath('data.deliveries.0.local_uuid', $uuid);

        $other = User::factory()->create();
        $other->assignRole('admin');
        $this->actingAs($other)->postJson("/api/v1/matches/{$match->id}/deliveries/sync", [
            'device_id' => 'other-device',
            'deliveries' => [$delivery],
        ])->assertForbidden()->assertJsonPath('code', 'match_edit_forbidden');

        [$secondOwner, $secondMatch, $secondInnings, $secondBatters, $secondBowler] = $this->liveMatch();
        $replayed = array_merge($delivery, [
            'striker_id' => $secondBatters[0]->id,
            'non_striker_id' => $secondBatters[1]->id,
            'bowler_id' => $secondBowler->id,
        ]);
        $this->actingAs($secondOwner)->postJson("/api/v1/matches/{$secondMatch->id}/deliveries/sync", [
            'device_id' => 'second-owner-device',
            'base_revision' => $secondMatch->revision,
            'deliveries' => [$replayed],
        ])->assertConflict();

        $this->assertSame(1, $match->deliveries()->where('local_uuid', $uuid)->count());
        $this->assertSame(1, $innings->fresh()->total_runs);
        $this->assertSame(0, $secondMatch->deliveries()->count());
        $this->assertSame(0, $secondInnings->fresh()->total_runs);
    }

    private function liveMatch(): array
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();
        $profile->update(['playing_xi_size' => 2]);
        $tournament = Tournament::create(['name' => 'Scoring Cup', 'slug' => 'scoring-cup-'.uniqid(), 'status' => 'live', 'timezone' => 'Asia/Karachi', 'squad_size' => 2, 'default_pick_duration' => 60, 'cricket_rule_profile_id' => $profile->id, 'creator_id' => $admin->id]);
        $teams = [
            Team::create(['tournament_id' => $tournament->id, 'name' => 'Bat Team', 'short_name' => 'BAT', 'display_order' => 1]),
            Team::create(['tournament_id' => $tournament->id, 'name' => 'Bowl Team', 'short_name' => 'BWL', 'display_order' => 2]),
        ];
        $match = CricketMatch::create(['tournament_id' => $tournament->id, 'rule_profile_id' => $profile->id, 'rule_profile_version' => $profile->version, 'overs_per_innings' => 20, 'status' => 'live', 'revision' => 1, 'created_by' => $admin->id]);
        $batters = collect();
        foreach (['A Batter', 'B Batter'] as $name) {
            $user = User::factory()->create(['name' => $name]);
            $user->playerProfile()->update(['full_name' => $name, 'playing_role' => 'Batter']);
            $player = $user->playerProfile()->firstOrFail();
            $tp = TournamentPlayer::create(['tournament_id' => $tournament->id, 'player_profile_id' => $player->id, 'status' => 'approved']);
            $batters->push(MatchPlayer::create(['match_id' => $match->id, 'team_id' => $teams[0]->id, 'tournament_player_id' => $tp->id, 'player_name_snapshot' => $name, 'player_role_snapshot' => 'Batter', 'selection_type' => 'playing_xi', 'batting_order' => $batters->count() + 1]));
        }
        $bowlerUser = User::factory()->create(['name' => 'A Bowler']);
        $bowlerUser->playerProfile()->update(['full_name' => 'A Bowler', 'playing_role' => 'Bowler']);
        $bowlerProfile = $bowlerUser->playerProfile()->firstOrFail();
        $bowlerTp = TournamentPlayer::create(['tournament_id' => $tournament->id, 'player_profile_id' => $bowlerProfile->id, 'status' => 'approved']);
        $bowler = MatchPlayer::create(['match_id' => $match->id, 'team_id' => $teams[1]->id, 'tournament_player_id' => $bowlerTp->id, 'player_name_snapshot' => 'A Bowler', 'player_role_snapshot' => 'Bowler', 'selection_type' => 'playing_xi']);
        $secondBowlerUser = User::factory()->create(['name' => 'B Bowler']);
        $secondBowlerUser->playerProfile()->update(['full_name' => 'B Bowler', 'playing_role' => 'Bowler']);
        $secondBowlerProfile = $secondBowlerUser->playerProfile()->firstOrFail();
        $secondTp = TournamentPlayer::create(['tournament_id' => $tournament->id, 'player_profile_id' => $secondBowlerProfile->id, 'status' => 'approved']);
        MatchPlayer::create(['match_id' => $match->id, 'team_id' => $teams[1]->id, 'tournament_player_id' => $secondTp->id, 'player_name_snapshot' => 'B Bowler', 'player_role_snapshot' => 'Bowler', 'selection_type' => 'playing_xi']);
        $innings = MatchInnings::create(['match_id' => $match->id, 'innings_number' => 1, 'batting_team_id' => $teams[0]->id, 'bowling_team_id' => $teams[1]->id, 'status' => 'live', 'maximum_overs' => 20, 'started_at' => now()]);
        $match->update(['current_innings_id' => $innings->id]);
        return [$admin, $match->fresh(), $innings, $batters->values(), $bowler];
    }
}
