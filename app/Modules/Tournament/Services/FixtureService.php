<?php

namespace App\Modules\Tournament\Services;

use App\Models\Fixture;
use App\Models\Team;
use App\Models\Tournament;
use App\Modules\Scoring\Services\MatchService;
use Illuminate\Database\DatabaseManager;
use Illuminate\Support\Carbon;
use Illuminate\Validation\ValidationException;

class FixtureService
{
    public function __construct(private readonly DatabaseManager $database, private readonly MatchService $matchService)
    {
    }

    public function create(?Tournament $tournament, array $data, int $actorId): Fixture
    {
        return $this->database->transaction(function () use ($tournament, $data, $actorId) {
            if ($tournament) {
                $this->assertTournamentCanSchedule($tournament);
            }
            $teams = $this->teamsFor($tournament, (int) $data['home_team_id'], (int) $data['away_team_id']);
            if (isset($data['match_number']) && $tournament) {
                if (Fixture::query()->where('tournament_id', $tournament->id)->where('match_number', $data['match_number'])->exists()) {
                    $this->fail('match_number', 'This match number is already assigned in the tournament.');
                }
            }
            if ($tournament) {
                $this->assertNoScheduleConflict($tournament, $teams[0]->id, $teams[1]->id, $data['scheduled_at']);
            }

            return Fixture::create([
                'tournament_id' => $tournament?->id,
                'home_team_id' => $teams[0]->id,
                'away_team_id' => $teams[1]->id,
                'round_number' => $data['round_number'] ?? null,
                'round_name' => $data['round_name'] ?? null,
                'match_number' => $data['match_number'] ?? null,
                'scheduled_at' => Carbon::parse($data['scheduled_at'], $data['timezone'] ?? $tournament?->timezone ?: 'UTC')->utc(),
                'venue' => $data['venue'] ?? null,
                'city' => $data['city'] ?? null,
                'timezone' => $data['timezone'] ?? $tournament?->timezone ?: 'UTC',
                'status' => 'scheduled',
                'notes' => $data['notes'] ?? null,
                'client_uuid' => $data['client_uuid'] ?? null,
                'configuration_snapshot' => $data['configuration'] ?? null,
                'created_by' => $actorId,
                'updated_by' => $actorId,
            ]);
        });
    }

    public function update(Fixture $fixture, array $data, int $actorId): Fixture
    {
        return $this->database->transaction(function () use ($fixture, $data, $actorId) {
            $fixture = Fixture::query()->lockForUpdate()->findOrFail($fixture->id);
            if ($fixture->match()->exists() || in_array($fixture->status, ['in_progress', 'completed', 'cancelled'], true)) {
                $this->fail('fixture', 'This fixture is locked after its operational match begins.');
            }
            $tournament = $fixture->tournament;
            if ($tournament) {
                $this->assertTournamentCanSchedule($tournament);
            }
            $teams = $this->teamsFor($tournament, (int) $data['home_team_id'], (int) $data['away_team_id']);
            if (isset($data['match_number']) && $tournament) {
                if (Fixture::query()->where('tournament_id', $tournament->id)->where('match_number', $data['match_number'])->where('id', '<>', $fixture->id)->exists()) {
                    $this->fail('match_number', 'This match number is already assigned in the tournament.');
                }
            }
            $fixture->update([
                'home_team_id' => $teams[0]->id,
                'away_team_id' => $teams[1]->id,
                'round_number' => $data['round_number'] ?? null,
                'round_name' => $data['round_name'] ?? null,
                'match_number' => $data['match_number'] ?? null,
                'scheduled_at' => Carbon::parse($data['scheduled_at'], $data['timezone'] ?? $tournament?->timezone ?: 'UTC')->utc(),
                'venue' => $data['venue'] ?? null,
                'city' => $data['city'] ?? null,
                'timezone' => $data['timezone'] ?? $tournament?->timezone ?: 'UTC',
                'notes' => $data['notes'] ?? null,
                'configuration_snapshot' => $data['configuration'] ?? $fixture->configuration_snapshot,
                'updated_by' => $actorId,
            ]);
            return $fixture->fresh(['homeTeam', 'awayTeam']);
        });
    }

    public function transition(Fixture $fixture, string $to, int $actorId): Fixture
    {
        return $this->database->transaction(function () use ($fixture, $to, $actorId) {
            $fixture = Fixture::query()->lockForUpdate()->findOrFail($fixture->id);
            $allowed = match ($fixture->status) {
                'scheduled' => ['postponed', 'cancelled'],
                'postponed' => ['scheduled', 'cancelled'],
                'in_progress' => ['completed', 'cancelled'],
                default => [],
            };
            if (! in_array($to, $allowed, true)) $this->fail('status', "Fixture cannot move from {$fixture->status} to {$to}.");
            if ($to === 'completed' && ! in_array($fixture->match?->status, ['completed', 'approved'], true)) $this->fail('status', 'The fixture can only be completed after its match is completed.');
            $fixture->update(['status' => $to, 'updated_by' => $actorId]);
            return $fixture->fresh(['homeTeam', 'awayTeam', 'match']);
        });
    }

    public function createMatch(Fixture $fixture, int $actorId)
    {
        $fixture->load(['tournament', 'homeTeam', 'awayTeam']);
        $existingMatch = $fixture->match()->first();
        if ($existingMatch) return $existingMatch->load(['players.team', 'ruleProfile', 'fixture']);
        if (! in_array($fixture->status, ['scheduled', 'postponed'], true)) $this->fail('fixture', 'A match can only be created from a scheduled or postponed fixture.');
        $match = $this->matchService->createFromTeams(
            $fixture->tournament,
            $fixture->home_team_id,
            $fixture->away_team_id,
            $fixture->id,
            $actorId,
            null,
            $fixture->configuration_snapshot,
            $fixture->client_uuid,
        );
        $fixture->update(['status' => 'in_progress', 'updated_by' => $actorId]);
        return $match;
    }

    private function teamsFor(?Tournament $tournament, int $homeId, int $awayId): array
    {
        if ($homeId === $awayId) $this->fail('teams', 'A fixture requires two different teams.');
        if ($tournament) {
            $teams = $tournament->teams()->whereIn('teams.id', [$homeId, $awayId])->where('teams.is_active', true)->get()->keyBy('id');
            if ($teams->count() !== 2) $this->fail('teams', 'Both fixture teams must be active teams in this tournament.');
            return [$teams->get($homeId), $teams->get($awayId)];
        } else {
            $teams = Team::whereIn('id', [$homeId, $awayId])->get()->keyBy('id');
            if ($teams->count() !== 2) $this->fail('teams', 'Both fixture teams must exist.');
            return [$teams->get($homeId), $teams->get($awayId)];
        }
    }

    private function assertNoScheduleConflict(Tournament $tournament, int $homeId, int $awayId, string $scheduledAt): void
    {
        $when = Carbon::parse($scheduledAt, $tournament->timezone ?: 'UTC')->utc();
        $exists = Fixture::query()->where('tournament_id', $tournament->id)->where('scheduled_at', $when)->whereIn('status', ['scheduled', 'postponed', 'in_progress'])->where(function ($query) use ($homeId, $awayId) {
            $query->where(function ($q) use ($homeId, $awayId) { $q->where('home_team_id', $homeId)->where('away_team_id', $awayId); })->orWhere(function ($q) use ($homeId, $awayId) { $q->where('home_team_id', $awayId)->where('away_team_id', $homeId); });
        })->exists();
        if ($exists) $this->fail('scheduled_at', 'This team matchup already has a fixture at the selected time.');
    }

    private function assertTournamentCanSchedule(Tournament $tournament): void
    {
        if (in_array($tournament->status, ['completed', 'cancelled'], true)) $this->fail('tournament', 'Fixtures cannot be changed after a tournament is completed or cancelled.');
    }

    private function fail(string $key, string $message): never
    {
        throw ValidationException::withMessages([$key => $message]);
    }
}
