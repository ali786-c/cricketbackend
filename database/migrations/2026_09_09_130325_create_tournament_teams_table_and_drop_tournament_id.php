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
        Schema::create('tournament_teams', function (Blueprint $table) {
            $table->id();
            $table->foreignId('tournament_id')->constrained()->cascadeOnDelete();
            $table->foreignId('team_id')->constrained()->cascadeOnDelete();
            $table->timestamps();

            // Prevent duplicate team registrations in same tournament
            $table->unique(['tournament_id', 'team_id']);
        });

        // 2. Migrate existing data from teams table to pivot
        DB::statement('INSERT INTO tournament_teams (tournament_id, team_id, created_at, updated_at) SELECT tournament_id, id, created_at, updated_at FROM teams WHERE tournament_id IS NOT NULL');

        // 3. Drop foreign key and column from teams table
        Schema::table('teams', function (Blueprint $table) {
            // First drop unique constraint if it exists
            $sm = Schema::getConnection()->getDoctrineSchemaManager();
            $indexesFound = $sm->listTableIndexes('teams');
            if(array_key_exists('teams_tournament_id_name_unique', $indexesFound)) {
                $table->dropUnique('teams_tournament_id_name_unique');
            }

            // Drop foreign key if exists
            $foreignKeys = $sm->listTableForeignKeys('teams');
            foreach ($foreignKeys as $fk) {
                if (in_array('tournament_id', $fk->getLocalColumns())) {
                    $table->dropForeign($fk->getName());
                }
            }

            // Drop column
            $table->dropColumn('tournament_id');
        });
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
