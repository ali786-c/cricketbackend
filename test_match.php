$user = App\Models\User::first();
if (!$user) { echo "No user found.\n"; exit; }

echo "Creating Teams...\n";
$homeTeam = App\Models\Team::firstOrCreate(['name' => 'AI Test Home'], ['is_active' => true, 'status' => 'pending', 'creator_id' => $user->id]);
$awayTeam = App\Models\Team::firstOrCreate(['name' => 'AI Test Away'], ['is_active' => true, 'status' => 'pending', 'creator_id' => $user->id]);

echo "Creating Fixture...\n";
$fixture = App\Models\Fixture::create([
    'tournament_id' => null,
    'home_team_id' => $homeTeam->id,
    'away_team_id' => $awayTeam->id,
    'status' => 'scheduled',
    'match_date' => now()->addDays(1),
    'venue' => 'AI Test Venue',
    'created_by' => $user->id,
    'updated_by' => $user->id,
]);

echo "Creating Match...\n";
$matchService = app(App\Modules\Scoring\Services\MatchService::class);
$match = $matchService->createFromTeams(null, $homeTeam->id, $awayTeam->id, $fixture->id, $user->id);

echo "Creating Players...\n";
$player1 = App\Models\PlayerProfile::create(['full_name' => 'AI Player 1', 'is_active' => true, 'is_guest' => true]);
$player2 = App\Models\PlayerProfile::create(['full_name' => 'AI Player 2', 'is_active' => true, 'is_guest' => true]);

echo "Submitting Playing XI...\n";
$matchService->submitPlayingXi($match, $homeTeam->id, [$player1->id, $player2->id], $user->id);

echo "SUCCESS! Match ID: " . $match->id . "\n";
