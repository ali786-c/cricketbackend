<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        // 1. Teams
        Schema::table('teams', function (Blueprint $table) {
            $sm = Schema::getConnection()->getDoctrineSchemaManager();
            $foreignKeys = $sm->listTableForeignKeys('teams');
            foreach ($foreignKeys as $fk) {
                if (in_array('tournament_id', $fk->getLocalColumns())) {
                    $table->dropForeign($fk->getName());
                }
            }

            $indexes = $sm->listTableIndexes('teams');
            if (array_key_exists('teams_tournament_id_name_unique', $indexes)) {
                $table->dropUnique('teams_tournament_id_name_unique');
            }
            if (array_key_exists('teams_tournament_id_display_order_unique', $indexes)) {
                $table->dropUnique('teams_tournament_id_display_order_unique');
            }
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete();
            
            $table->unique(['tournament_id', 'name']);
            $table->unique(['tournament_id', 'display_order']);
        });

        // 2. Fixtures
        Schema::table('fixtures', function (Blueprint $table) {
            $sm = Schema::getConnection()->getDoctrineSchemaManager();
            $foreignKeys = $sm->listTableForeignKeys('fixtures');
            foreach ($foreignKeys as $fk) {
                if (in_array('tournament_id', $fk->getLocalColumns())) {
                    $table->dropForeign($fk->getName());
                }
            }

            $indexes = $sm->listTableIndexes('fixtures');
            if (array_key_exists('fixtures_tournament_id_match_number_unique', $indexes)) {
                $table->dropUnique('fixtures_tournament_id_match_number_unique');
            }
            if (array_key_exists('fixtures_tournament_id_scheduled_at_index', $indexes)) {
                $table->dropIndex('fixtures_tournament_id_scheduled_at_index');
            }
            if (array_key_exists('fixtures_tournament_id_status_index', $indexes)) {
                $table->dropIndex('fixtures_tournament_id_status_index');
            }
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete();
            
            $table->unique(['tournament_id', 'match_number']);
            $table->index(['tournament_id', 'scheduled_at']);
            $table->index(['tournament_id', 'status']);
        });

        // 3. Matches
        Schema::table('matches', function (Blueprint $table) {
            $sm = Schema::getConnection()->getDoctrineSchemaManager();
            $foreignKeys = $sm->listTableForeignKeys('matches');
            foreach ($foreignKeys as $fk) {
                if (in_array('tournament_id', $fk->getLocalColumns())) {
                    $table->dropForeign($fk->getName());
                }
            }

            $indexes = $sm->listTableIndexes('matches');
            if (array_key_exists('matches_tournament_id_status_index', $indexes)) {
                $table->dropIndex('matches_tournament_id_status_index');
            }
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete();
            
            $table->index(['tournament_id', 'status']);
        });

        // 4. Stages
        Schema::table('stages', function (Blueprint $table) {
            $sm = Schema::getConnection()->getDoctrineSchemaManager();
            $foreignKeys = $sm->listTableForeignKeys('stages');
            foreach ($foreignKeys as $fk) {
                if (in_array('tournament_id', $fk->getLocalColumns())) {
                    $table->dropForeign($fk->getName());
                }
            }

            $indexes = $sm->listTableIndexes('stages');
            if (array_key_exists('stages_tournament_id_order_index', $indexes)) {
                $table->dropIndex('stages_tournament_id_order_index');
            }
            if (array_key_exists('stages_tournament_id_status_index', $indexes)) {
                $table->dropIndex('stages_tournament_id_status_index');
            }
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete();
            
            $table->index(['tournament_id', 'order']);
            $table->index(['tournament_id', 'status']);
        });

        // 5. Match Players
        Schema::table('match_players', function (Blueprint $table) {
            $sm = Schema::getConnection()->getDoctrineSchemaManager();
            $foreignKeys = $sm->listTableForeignKeys('match_players');
            foreach ($foreignKeys as $fk) {
                if (in_array('tournament_player_id', $fk->getLocalColumns())) {
                    $table->dropForeign($fk->getName());
                }
            }

            $indexes = $sm->listTableIndexes('match_players');
            if (array_key_exists('match_players_match_id_tournament_player_id_unique', $indexes)) {
                $table->dropUnique('match_players_match_id_tournament_player_id_unique');
            }
            
            $table->unsignedBigInteger('tournament_player_id')->nullable()->change();
            $table->foreign('tournament_player_id')->references('id')->on('tournament_players')->restrictOnDelete();
            
            if (!Schema::hasColumn('match_players', 'player_profile_id')) {
                $table->foreignId('player_profile_id')->nullable()->after('tournament_player_id')->constrained('player_profiles')->restrictOnDelete();
                $table->unique(['match_id', 'player_profile_id']);
            }
            
            $table->unique(['match_id', 'tournament_player_id']);
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        // Reversing these nullable columns would result in data loss for standalone matches.
        // Therefore, we only drop the added column in match_players.
        Schema::table('match_players', function (Blueprint $table) {
            $table->dropForeign(['player_profile_id']);
            $table->dropUnique(['match_id', 'player_profile_id']);
            $table->dropColumn('player_profile_id');
        });
    }
};
