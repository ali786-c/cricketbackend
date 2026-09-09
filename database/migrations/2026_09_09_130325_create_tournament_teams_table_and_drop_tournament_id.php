<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        // 1. Create pivot table
        if (!Schema::hasTable('tournament_teams')) {
            Schema::create('tournament_teams', function (Blueprint $table) {
                $table->id();
                $table->foreignId('tournament_id')->constrained()->cascadeOnDelete();
                $table->foreignId('team_id')->constrained()->cascadeOnDelete();
                $table->timestamps();

                // Prevent duplicate team registrations in same tournament
                $table->unique(['tournament_id', 'team_id']);
            });
        }

        // 2. Migrate existing data from teams table to pivot
        try {
            DB::statement('INSERT IGNORE INTO tournament_teams (tournament_id, team_id, created_at, updated_at) SELECT tournament_id, id, created_at, updated_at FROM teams WHERE tournament_id IS NOT NULL');
        } catch (\Exception $e) {}

        // 3. Drop foreign key and column from teams table
        try { Schema::table('teams', function (Blueprint $table) { $table->dropUnique('teams_tournament_id_name_unique'); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropUnique('teams_tournament_id_display_order_unique'); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropUnique(['tournament_id', 'name']); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropUnique(['tournament_id', 'display_order']); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropForeign(['tournament_id']); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropForeign('teams_tournament_id_foreign'); }); } catch (\Exception $e) {}

        try {
            Schema::table('teams', function (Blueprint $table) {
                $table->dropColumn('tournament_id');
            });
        } catch (\Exception $e) {}
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::table('teams', function (Blueprint $table) {
            $table->foreignId('tournament_id')->nullable()->constrained()->nullOnDelete();
            $table->unique(['tournament_id', 'name']);
        });

        // Restore data
        DB::statement('UPDATE teams t JOIN tournament_teams tt ON t.id = tt.team_id SET t.tournament_id = tt.tournament_id');

        Schema::dropIfExists('tournament_teams');
    }
};
