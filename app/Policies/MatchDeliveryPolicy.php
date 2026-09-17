<?php

namespace App\Policies;

use App\Models\MatchDelivery;
use App\Models\User;

class MatchDeliveryPolicy
{
    public function before(User $user, string $ability): ?bool
    {
        return $user->hasRole('super_admin') ? true : null;
    }

    public function update(User $user, MatchDelivery $delivery): bool
    {
        $match = $delivery->match;

        return $match !== null && $user->can('score', $match);
    }
}
