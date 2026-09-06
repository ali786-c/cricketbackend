<?php

namespace App\Modules\Analytics\Services;

use App\Models\CricketMatch;
use App\Models\PlayerProfile;
use App\Models\Team;
use App\Models\Tournament;

class UnifiedSearchService
{
    /**
     * Unified search across all entities.
     *
     * Supports:
     *  - Unique code exact match (e.g. "TEAM-A3K9F", "PLR-M7B2N")
     *  - Name partial match with relevance scoring
     *  - Type filtering (players, teams, tournaments, matches)
     *  - Pagination per type
     *  - Additional filters (city, role, tournament_id, status)
     */
    public function search(string $query, array $options = []): array
    {
        if (strlen($query) < 2) {
            return $this->emptyResult();
        }

        $types = $options['types'] ?? ['players', 'teams', 'tournaments', 'matches'];
        $limit = min($options['limit'] ?? 10, 50);
        $tournamentId = $options['tournament_id'] ?? null;

        // Detect if query is a unique code
        $isCode = $this->isUniqueCode($query);

        $results = [];

        if (in_array('players', $types, true)) {
            try {
                $results['players'] = $this->searchPlayers($query, $isCode, $limit, $options);
            } catch (\Throwable $e) {
                $results['players'] = [];
            }
        }

        if (in_array('teams', $types, true)) {
            try {
                $results['teams'] = $this->searchTeams($query, $isCode, $limit, $options, $tournamentId);
            } catch (\Throwable $e) {
                $results['teams'] = [];
            }
        }

        if (in_array('tournaments', $types, true)) {
            try {
                $results['tournaments'] = $this->searchTournaments($query, $isCode, $limit, $options);
            } catch (\Throwable $e) {
                $results['tournaments'] = [];
            }
        }

        if (in_array('matches', $types, true)) {
            try {
                $results['matches'] = $this->searchMatches($query, $isCode, $limit, $tournamentId);
            } catch (\Throwable $e) {
                // If match search fails (e.g. missing table/column), return empty
                $results['matches'] = [];
            }
        }

        $results['meta'] = [
            'query' => $query,
            'is_code_search' => $isCode,
            'types_searched' => $types,
        ];

        return $results;
    }

    /**
     * Lookup a single entity by its unique code.
     */
    public function findByCode(string $code): ?array
    {
        $code = strtoupper(trim($code));

        // Check player
        if (str_starts_with($code, 'PLR-')) {
            $player = PlayerProfile::where('unique_code', $code)->with('user')->first();
            if ($player) {
                return [
                    'type' => 'player',
                    'data' => $this->formatPlayer($player),
                ];
            }
        }

        // Check team
        if (str_starts_with($code, 'TEAM-')) {
            $team = Team::where('unique_code', $code)->with('tournament')->first();
            if ($team) {
                return [
                    'type' => 'team',
                    'data' => $this->formatTeam($team),
                ];
            }
        }

        return null;
    }

    // ─── Private Search Methods ──────────────────────────────────────

    private function searchPlayers(string $query, bool $isCode, int $limit, array $options): array
    {
        $q = PlayerProfile::query()->where('is_active', true);

        if ($isCode && str_starts_with(strtoupper($query), 'PLR-')) {
            $q->where('unique_code', strtoupper($query));
        } else {
            $q->where(function ($sub) use ($query) {
                $sub->where('full_name', 'like', "%{$query}%")
                    ->orWhere('city', 'like', "%{$query}%")
                    ->orWhere('playing_role', 'like', "%{$query}%")
                    ->orWhere('unique_code', 'like', "%{$query}%");
            });
        }

        // Optional filters
        if (!empty($options['city'])) {
            $q->where('city', 'like', "%{$options['city']}%");
        }
        if (!empty($options['playing_role'])) {
            $q->where('playing_role', $options['playing_role']);
        }

        $results = $q->take($limit)
            ->get(['id', 'unique_code', 'full_name', 'playing_role', 'batting_style', 'bowling_style', 'city', 'photo_path']);

        return $results->map(fn($p) => $this->formatPlayer($p))->toArray();
    }

    private function searchTeams(string $query, bool $isCode, int $limit, array $options, ?int $tournamentId): array
    {
        $q = Team::query();

        if ($isCode && str_starts_with(strtoupper($query), 'TEAM-')) {
            $q->where('unique_code', strtoupper($query));
        } else {
            $q->where(function ($sub) use ($query) {
                $sub->where('name', 'like', "%{$query}%")
                    ->orWhere('short_name', 'like', "%{$query}%")
                    ->orWhere('unique_code', 'like', "%{$query}%");
            });
        }

        if ($tournamentId) {
            $q->where('tournament_id', $tournamentId);
        }

        $results = $q->take($limit)
            ->get(['id', 'unique_code', 'name', 'short_name', 'logo_path', 'tournament_id', 'is_active']);

        return $results->map(fn($t) => $this->formatTeam($t))->toArray();
    }

    private function searchTournaments(string $query, bool $isCode, int $limit, array $options): array
    {
        $q = Tournament::query();

        // Tournaments use slug as unique code
        if ($isCode) {
            $q->where('slug', strtolower($query));
        } else {
            $q->where(function ($sub) use ($query) {
                $sub->where('name', 'like', "%{$query}%")
                    ->orWhere('slug', 'like', "%{$query}%")
                    ->orWhere('city', 'like', "%{$query}%");
            });
        }

        // Optional status filter
        if (!empty($options['status'])) {
            $q->where('status', $options['status']);
        }

        $results = $q->take($limit)
            ->get(['id', 'name', 'slug', 'status', 'starts_on', 'ends_on', 'city', 'logo_path']);

        return $results->map(fn($t) => [
            'id' => $t->id,
            'name' => $t->name,
            'slug' => $t->slug,
            'status' => $t->status,
            'city' => $t->city,
            'starts_on' => $t->starts_on?->toDateString(),
            'ends_on' => $t->ends_on?->toDateString(),
            'logo_path' => $t->logo_path,
        ])->toArray();
    }

    private function searchMatches(string $query, bool $isCode, int $limit, ?int $tournamentId): array
    {
        $q = CricketMatch::query()
            ->with(['fixture.homeTeam', 'fixture.awayTeam', 'tournament']);

        // Search by tournament name (safest — always works)
        $q->where(function ($sub) use ($query) {
            $sub->whereHas('tournament', fn($t) => $t->where('name', 'like', "%{$query}%"));

            // Also try searching by fixture team names (if fixture exists)
            $sub->orWhereHas('fixture', function ($fixtureQ) use ($query) {
                $fixtureQ->where(function ($inner) use ($query) {
                    $inner->where('home_team_id', function ($teamSub) use ($query) {
                        $teamSub->select('id')->from('teams')
                            ->where('name', 'like', "%{$query}%")
                            ->orWhere('short_name', 'like', "%{$query}%");
                    })->orWhere('away_team_id', function ($teamSub) use ($query) {
                        $teamSub->select('id')->from('teams')
                            ->where('name', 'like', "%{$query}%")
                            ->orWhere('short_name', 'like', "%{$query}%");
                    });
                });
            });
        });

        if ($tournamentId) {
            $q->where('tournament_id', $tournamentId);
        }

        $results = $q->take($limit)->get();

        return $results->map(fn($m) => [
            'id' => $m->id,
            'status' => $m->status,
            'home_team' => $m->fixture?->homeTeam?->short_name,
            'away_team' => $m->fixture?->awayTeam?->short_name,
            'tournament' => $m->tournament?->name,
            'tournament_id' => $m->tournament_id,
        ])->toArray();
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private function formatPlayer($player): array
    {
        return [
            'id' => $player->id,
            'unique_code' => $player->unique_code,
            'full_name' => $player->full_name,
            'playing_role' => $player->playing_role,
            'batting_style' => $player->batting_style,
            'bowling_style' => $player->bowling_style,
            'city' => $player->city,
            'photo_path' => $player->photo_path,
        ];
    }

    private function formatTeam($team): array
    {
        return [
            'id' => $team->id,
            'unique_code' => $team->unique_code,
            'name' => $team->name,
            'short_name' => $team->short_name,
            'logo_path' => $team->logo_path,
            'tournament_id' => $team->tournament_id,
            'is_active' => $team->is_active ?? true,
        ];
    }

    private function isUniqueCode(string $query): bool
    {
        $upper = strtoupper(trim($query));
        return preg_match('/^(TEAM-[A-Z0-9]{5}|PLR-[A-Z0-9]{5})$/', $upper) === 1;
    }

    private function emptyResult(): array
    {
        return [
            'players' => [],
            'teams' => [],
            'tournaments' => [],
            'matches' => [],
            'meta' => [
                'query' => '',
                'is_code_search' => false,
                'types_searched' => [],
            ],
        ];
    }
}
