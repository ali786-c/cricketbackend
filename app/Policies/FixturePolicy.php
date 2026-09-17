<?php

namespace App\Policies;

use App\Models\Fixture;
use App\Models\User;

class FixturePolicy
{
    public function before(User $user, string $ability): ?bool
    {
        return $user->hasRole('super_admin') ? true : null;
    }

    public function manage(User $user, Fixture $fixture): bool
    {
        if ($fixture->tournament_id !== null) {
            return $fixture->tournament !== null
                && $user->can('manageFixtures', $fixture->tournament);
        }

        return (int) $fixture->created_by === (int) $user->id;
    }

    public function update(User $user, Fixture $fixture): bool { return $this->manage($user, $fixture); }
    public function transition(User $user, Fixture $fixture): bool { return $this->manage($user, $fixture); }
    public function createMatch(User $user, Fixture $fixture): bool { return $this->manage($user, $fixture); }
    public function delete(User $user, Fixture $fixture): bool { return $this->manage($user, $fixture); }
}
