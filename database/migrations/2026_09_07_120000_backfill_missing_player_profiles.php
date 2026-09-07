<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

/**
 * Backfill player_profiles for user accounts that don't have one.
 *
 * The User::created hook (app/Models/User.php) auto-creates a profile for new
 * accounts, but users created before that hook was deployed (or inserted via
 * seeders/scripts) are missing one — which makes them invisible to the unified
 * search API (GET /api/v1/search), which queries player_profiles.is_active = 1.
 *
 * This migration is idempotent: it only inserts rows for users without a
 * profile and can be re-run safely.
 */
return new class extends Migration
{
    public function up(): void
    {
        $usersWithoutProfile = DB::table('users as u')
            ->leftJoin('player_profiles as p', 'p.user_id', '=', 'u.id')
            ->whereNull('p.id')
            ->select('u.id', 'u.name')
            ->get();

        foreach ($usersWithoutProfile as $user) {
            DB::table('player_profiles')->insert([
                'user_id' => $user->id,
                'full_name' => $user->name,
                'unique_code' => $this->generateUniqueCode(),
                'is_active' => true,
                'created_at' => now(),
                'updated_at' => now(),
            ]);
        }
    }

    private function generateUniqueCode(): string
    {
        do {
            $code = 'PLR-' . strtoupper(Str::random(5));
        } while (DB::table('player_profiles')->where('unique_code', $code)->exists());

        return $code;
    }

    public function down(): void
    {
        // Intentionally a no-op: the backfilled rows are real user data and
        // deleting them on rollback would be destructive.
    }
};
