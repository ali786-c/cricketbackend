<?php

namespace Database\Seeders;

use App\Models\CricketRuleProfile;
use Illuminate\Database\Seeder;

class CricketRuleProfileSeeder extends Seeder
{
    public function run(): void
    {
        CricketRuleProfile::updateOrCreate(
            ['slug' => 't20-standard'],
            [
                'name' => 'Standard T20',
                'format' => 't20',
                'innings_per_side' => 1,
                'overs_per_innings' => 20,
                'playing_xi_size' => 11,
                'maximum_wickets' => 10,
                'legal_balls_per_over' => 6,
                'max_overs_per_bowler' => 4,
                'no_ball_runs' => 1,
                'wide_runs' => 1,
                'win_points' => 2,
                'tie_points' => 1,
                'no_result_points' => 1,
                'loss_points' => 0,
                'tie_method' => 'points',
                'version' => 1,
                'is_system' => true,
                'is_active' => true,
            ]
        );

        CricketRuleProfile::updateOrCreate(
            ['slug' => 'community-10-over'],
            [
                'name' => 'Community 10 Over',
                'format' => 'limited_overs',
                'innings_per_side' => 1,
                'overs_per_innings' => 10,
                'playing_xi_size' => 11,
                'maximum_wickets' => 10,
                'legal_balls_per_over' => 6,
                'max_overs_per_bowler' => 2,
                'no_ball_runs' => 1,
                'wide_runs' => 1,
                'win_points' => 2,
                'tie_points' => 1,
                'no_result_points' => 1,
                'loss_points' => 0,
                'tie_method' => 'points',
                'version' => 1,
                'is_system' => true,
                'is_active' => true,
            ]
        );

        // Compatibility profile for pre-configuration custom clients. New custom
        // matches create an immutable profile from their supplied configuration.
        CricketRuleProfile::updateOrCreate(
            ['slug' => 'default-custom'],
            [
                'name' => 'Legacy Custom Match Defaults',
                'format' => 'custom',
                'innings_per_side' => 1,
                'overs_per_innings' => 20,
                'playing_xi_size' => 11,
                'maximum_wickets' => 10,
                'legal_balls_per_over' => 6,
                'max_overs_per_bowler' => 4,
                'no_ball_runs' => 1,
                'wide_runs' => 1,
                'win_points' => 2,
                'tie_points' => 1,
                'no_result_points' => 1,
                'loss_points' => 0,
                'tie_method' => 'points',
                'version' => 1,
                'is_system' => true,
                'is_active' => true,
            ]
        );
    }
}
