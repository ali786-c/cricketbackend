<?php

namespace Tests\Feature\Admin;

use App\Models\CricketMatch;
use App\Models\CricketRuleProfile;
use App\Models\MatchInnings;
use App\Models\Team;
use App\Models\Tournament;
use App\Models\User;
use App\Modules\Scoring\Services\MatchResultService;
use Database\Seeders\CricketRuleProfileSeeder;
use Database\Seeders\RolePermissionSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MatchResultTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([RolePermissionSeeder::class, CricketRuleProfileSeeder::class]);
    }

    public function test_completed_match_result_is_submitted_then_approved_into_standings(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();
        $tournament = Tournament::create(['name' => 'Results Cup', 'slug' => 'results-cup-'.uniqid(), 'status' => 'live', 'is_public' => true, 'timezone' => 'Asia/Karachi', 'squad_size' => 2, 'default_pick_duration' => 60, 'cricket_rule_profile_id' => $profile->id]);
        $firstTeam = Team::create(['tournament_id' => $tournament->id, 'name' => 'First XI', 'short_name' => 'FST', 'display_order' => 1]);
        $secondTeam = Team::create(['tournament_id' => $tournament->id, 'name' => 'Second XI', 'short_name' => 'SND', 'display_order' => 2]);
        $match = CricketMatch::create(['tournament_id' => $tournament->id, 'rule_profile_id' => $profile->id, 'rule_profile_version' => 1, 'status' => 'completed', 'completed_at' => now(), 'revision' => 4, 'created_by' => $admin->id]);
        $first = MatchInnings::create(['match_id' => $match->id, 'innings_number' => 1, 'batting_team_id' => $firstTeam->id, 'bowling_team_id' => $secondTeam->id, 'status' => 'completed', 'maximum_overs' => 20, 'total_runs' => 100, 'legal_balls' => 120, 'completed_reason' => 'overs_complete', 'completed_at' => now()]);
        MatchInnings::create(['match_id' => $match->id, 'innings_number' => 2, 'batting_team_id' => $secondTeam->id, 'bowling_team_id' => $firstTeam->id, 'status' => 'completed', 'target_runs' => 101, 'maximum_overs' => 20, 'total_runs' => 80, 'legal_balls' => 120, 'completed_reason' => 'overs_complete', 'completed_at' => now()]);
        $match->update(['current_innings_id' => $first->id]);

        $this->actingAs($admin)->post(route('admin.matches.result.submit', $match))->assertRedirect();
        $this->assertSame('result_pending', $match->fresh()->status);
        $this->assertSame($firstTeam->id, $match->fresh()->winner_team_id);

        // A rejected result returns to completed and can be corrected/submitted again.
        app(MatchResultService::class)->reject($match->fresh(), $admin->id);
        $this->assertSame('completed', $match->fresh()->status);
        app(MatchResultService::class)->submit($match->fresh(), $admin->id);
        $this->assertSame('result_pending', $match->fresh()->status);

        $this->actingAs($admin)->post(route('admin.matches.result.approve', $match))->assertRedirect();
        $standing = $tournament->standings()->where('team_id', $firstTeam->id)->firstOrFail();
        $this->assertSame(2, $standing->points);
        $this->assertSame(1, $standing->wins);
        $this->assertSame(1, $standing->played);
        $this->assertSame(100, (int) $standing->runs_for);
    }

    public function test_custom_match_result_can_be_approved_without_tournament_standings(): void
    {
        $admin = User::factory()->create();
        $admin->assignRole('admin');
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();
        $firstTeam = Team::create(['name' => 'Custom A', 'short_name' => 'CTA']);
        $secondTeam = Team::create(['name' => 'Custom B', 'short_name' => 'CTB']);
        $match = CricketMatch::create(['rule_profile_id' => $profile->id, 'rule_profile_version' => 1, 'status' => 'completed', 'completed_at' => now(), 'revision' => 2, 'created_by' => $admin->id]);
        MatchInnings::create(['match_id' => $match->id, 'innings_number' => 1, 'batting_team_id' => $firstTeam->id, 'bowling_team_id' => $secondTeam->id, 'status' => 'completed', 'maximum_overs' => 20, 'total_runs' => 10, 'legal_balls' => 6, 'completed_reason' => 'overs_complete', 'completed_at' => now()]);
        MatchInnings::create(['match_id' => $match->id, 'innings_number' => 2, 'batting_team_id' => $secondTeam->id, 'bowling_team_id' => $firstTeam->id, 'status' => 'completed', 'target_runs' => 11, 'maximum_overs' => 20, 'total_runs' => 11, 'legal_balls' => 5, 'completed_reason' => 'target_reached', 'completed_at' => now()]);

        $service = app(MatchResultService::class);
        $service->submit($match, $admin->id);
        $this->actingAs($admin)->postJson('/api/v1/admin/matches/'.$match->id.'/result/approve')->assertOk();

        $this->assertSame('approved', $match->fresh()->status);
        $this->assertSame($secondTeam->id, $match->fresh()->winner_team_id);
    }

    public function test_exceptional_result_states_are_explicit(): void
    {
        $admin = User::factory()->create();
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();
        $tournament = Tournament::create(['name' => 'Weather Cup', 'slug' => 'weather-cup-'.uniqid(), 'status' => 'live', 'is_public' => true, 'timezone' => 'Asia/Karachi', 'squad_size' => 2, 'default_pick_duration' => 60, 'cricket_rule_profile_id' => $profile->id]);
        $service = app(MatchResultService::class);

        $noResult = CricketMatch::create(['tournament_id' => $tournament->id, 'rule_profile_id' => $profile->id, 'rule_profile_version' => 1, 'status' => 'live', 'revision' => 1, 'created_by' => $admin->id]);
        $service->recordException($noResult, $admin->id, 'no_result');
        $this->assertSame('result_pending', $noResult->fresh()->status);
        $this->assertSame('no_result', $noResult->fresh()->result_type);
        $service->approve($noResult->fresh(), $admin->id);
        $this->assertSame('approved', $noResult->fresh()->status);

        $abandoned = CricketMatch::create(['tournament_id' => $tournament->id, 'rule_profile_id' => $profile->id, 'rule_profile_version' => 1, 'status' => 'live', 'revision' => 1, 'created_by' => $admin->id]);
        $service->recordException($abandoned, $admin->id, 'abandoned');
        $this->assertSame('abandoned', $abandoned->fresh()->status);
        $this->assertSame('no_result', $abandoned->fresh()->result_type);

        $cancelled = CricketMatch::create(['tournament_id' => $tournament->id, 'rule_profile_id' => $profile->id, 'rule_profile_version' => 1, 'status' => 'live', 'revision' => 1, 'created_by' => $admin->id]);
        $service->recordException($cancelled, $admin->id, 'cancelled');
        $this->assertSame('cancelled', $cancelled->fresh()->status);
        $this->assertNull($cancelled->fresh()->result_type);
    }

    public function test_equal_completed_scores_submit_as_a_tie(): void
    {
        $admin = User::factory()->create();
        $profile = CricketRuleProfile::query()->where('slug', 't20-standard')->firstOrFail();
        $firstTeam = Team::create(['name' => 'Tie A', 'short_name' => 'TIA']);
        $secondTeam = Team::create(['name' => 'Tie B', 'short_name' => 'TIB']);
        $match = CricketMatch::create(['rule_profile_id' => $profile->id, 'rule_profile_version' => 1, 'status' => 'completed', 'completed_at' => now(), 'revision' => 2, 'created_by' => $admin->id]);
        MatchInnings::create(['match_id' => $match->id, 'innings_number' => 1, 'batting_team_id' => $firstTeam->id, 'bowling_team_id' => $secondTeam->id, 'status' => 'completed', 'maximum_overs' => 20, 'total_runs' => 25, 'legal_balls' => 12, 'completed_reason' => 'overs_complete', 'completed_at' => now()]);
        MatchInnings::create(['match_id' => $match->id, 'innings_number' => 2, 'batting_team_id' => $secondTeam->id, 'bowling_team_id' => $firstTeam->id, 'status' => 'completed', 'target_runs' => 26, 'maximum_overs' => 20, 'total_runs' => 25, 'legal_balls' => 12, 'completed_reason' => 'overs_complete', 'completed_at' => now()]);

        app(MatchResultService::class)->submit($match, $admin->id);

        $this->assertSame('tie', $match->fresh()->result_type);
        $this->assertNull($match->fresh()->winner_team_id);
        $this->assertSame('Match tied', $match->fresh()->result_summary);
    }
}
