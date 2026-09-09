<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Facades\DB;
use App\Models\PlayerProfile;

return new class extends Migration
{
    public function up(): void
    {
        // Change existing unique_codes from PLR-XXXX to 6-digit random number
        $profiles = PlayerProfile::all();
        foreach ($profiles as $profile) {
            do {
                $code = (string) random_int(100000, 999999);
                $exists = PlayerProfile::where('unique_code', $code)->exists();
            } while ($exists);

            $profile->update(['unique_code' => $code]);
        }
    }

    public function down(): void
    {
        // Revert backfill not possible perfectly, but no schema changes to rollback.
    }
};
