<?php

namespace App\Support;

use App\Models\CricketMatch;
use App\Models\Fixture;
use App\Models\Tournament;
use App\Models\User;

class ViewerPermissions
{
    public static function forTournament(?User $user, Tournament $tournament): array
    {
        $canManage = $user !== null
            && $user->can('manage tournaments')
            && $user->can('manage', $tournament);

        return [
            'can_view' => $tournament->publiclyVisibleNow() || $canManage,
            'can_manage' => $canManage,
            'can_edit_fixture' => $canManage,
            'can_manage_lineup' => $canManage,
            'can_score' => false,
            'can_submit_result' => false,
        ];
    }

    public static function forFixture(?User $user, Fixture $fixture): array
    {
        $canManage = $user !== null
            && $user->can('manage tournaments')
            && $user->can('manage', $fixture);
        $public = $fixture->tournament?->publiclyVisibleNow() === true;

        return [
            'can_view' => $public || $canManage,
            'can_manage' => $canManage,
            'can_edit_fixture' => $canManage,
            'can_manage_lineup' => false,
            'can_score' => false,
            'can_submit_result' => false,
        ];
    }

    public static function forMatch(?User $user, CricketMatch $match): array
    {
        $canManage = $user !== null
            && $user->can('manage tournaments')
            && $user->can('manage', $match);
        $canScore = $user !== null
            && $user->can('control draft')
            && $user->can('score', $match);
        $public = $match->tournament?->publiclyVisibleNow() === true
            && in_array($match->status, ['live', 'completed', 'result_pending', 'approved'], true);

        return [
            'can_view' => $public || $canManage || $canScore,
            'can_manage' => $canManage,
            'can_edit_fixture' => $canManage,
            'can_manage_lineup' => $canManage,
            'can_score' => $canScore,
            'can_submit_result' => $canScore,
        ];
    }
}
