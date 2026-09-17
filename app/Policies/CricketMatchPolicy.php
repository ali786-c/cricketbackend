<?php

namespace App\Policies;

use App\Models\CricketMatch;
use App\Models\User;

class CricketMatchPolicy
{
    public function before(User $user, string $ability): ?bool
    {
        return $user->hasRole('super_admin') ? true : null;
    }

    public function manage(User $user, CricketMatch $match): bool
    {
        return $this->ownerId($match) === (int) $user->id;
    }

    public function score(User $user, CricketMatch $match): bool
    {
        return $this->manage($user, $match);
    }

    public function configure(User $user, CricketMatch $match): bool { return $this->manage($user, $match); }
    public function manageLineup(User $user, CricketMatch $match): bool { return $this->manage($user, $match); }
    public function recordToss(User $user, CricketMatch $match): bool { return $this->manage($user, $match); }
    public function correctDelivery(User $user, CricketMatch $match): bool { return $this->manage($user, $match); }
    public function undo(User $user, CricketMatch $match): bool { return $this->manage($user, $match); }
    public function startNextInnings(User $user, CricketMatch $match): bool { return $this->manage($user, $match); }

    public function submitResult(User $user, CricketMatch $match): bool
    {
        return $this->manage($user, $match);
    }

    public function approveResult(User $user, CricketMatch $match): bool
    {
        return $this->manage($user, $match);
    }

    private function ownerId(CricketMatch $match): int
    {
        // Legacy tournament rows may not have creator_id populated yet. Keep
        // created_by as a safe fallback until the ownership backfill phase.
        $tournamentOwnerId = $match->tournament_id !== null
            ? $match->tournament()->value('creator_id')
            : null;

        return (int) ($tournamentOwnerId ?? $match->created_by ?? 0);
    }
}
