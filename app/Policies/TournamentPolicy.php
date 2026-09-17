<?php

namespace App\Policies;

use App\Models\Tournament;
use App\Models\User;

class TournamentPolicy
{
    public function before(User $user, string $ability): ?bool
    {
        return $user->hasRole('super_admin') ? true : null;
    }

    public function manage(User $user, Tournament $tournament): bool
    {
        return (int) $tournament->creator_id === (int) $user->id;
    }

    public function update(User $user, Tournament $tournament): bool { return $this->manage($user, $tournament); }
    public function delete(User $user, Tournament $tournament): bool { return $this->manage($user, $tournament); }
    public function manageTeams(User $user, Tournament $tournament): bool { return $this->manage($user, $tournament); }
    public function managePlayers(User $user, Tournament $tournament): bool { return $this->manage($user, $tournament); }
    public function manageFixtures(User $user, Tournament $tournament): bool { return $this->manage($user, $tournament); }
    public function manageDraft(User $user, Tournament $tournament): bool { return $this->manage($user, $tournament); }
    public function manageMatches(User $user, Tournament $tournament): bool { return $this->manage($user, $tournament); }
}
