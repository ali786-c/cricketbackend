<?php

namespace App\Policies;

use App\Models\Team;
use App\Models\User;

class TeamPolicy
{
    public function before(User $user, string $ability): ?bool
    {
        return $user->hasRole('super_admin') ? true : null;
    }

    public function manage(User $user, Team $team): bool
    {
        if ((int) $team->creator_id === (int) $user->id) {
            return true;
        }

        return $team->tournaments()
            ->where('tournaments.creator_id', $user->id)
            ->exists();
    }
}
