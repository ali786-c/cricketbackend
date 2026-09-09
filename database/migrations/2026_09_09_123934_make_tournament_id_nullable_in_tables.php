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
            try { $table->dropForeign(['tournament_id']); } catch(\Exception $e) {}
            try { $table->dropUnique('teams_tournament_id_name_unique'); } catch(\Exception $e) {}
            try { $table->dropUnique('teams_tournament_id_display_order_unique'); } catch(\Exception $e) {}
            try { $table->dropUnique(['tournament_id', 'name']); } catch(\Exception $e) {}
            try { $table->dropUnique(['tournament_id', 'display_order']); } catch(\Exception $e) {}
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            
            try { $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete(); } catch(\Exception $e) {}
            try { $table->unique(['tournament_id', 'name']); } catch(\Exception $e) {}
            try { $table->unique(['tournament_id', 'display_order']); } catch(\Exception $e) {}
        });

        // 2. Fixtures
        Schema::table('fixtures', function (Blueprint $table) {
            try { $table->dropForeign(['tournament_id']); } catch(\Exception $e) {}
            try { $table->dropUnique('fixtures_tournament_id_match_number_unique'); } catch(\Exception $e) {}
            try { $table->dropIndex('fixtures_tournament_id_scheduled_at_index'); } catch(\Exception $e) {}
            try { $table->dropIndex('fixtures_tournament_id_status_index'); } catch(\Exception $e) {}
            try { $table->dropUnique(['tournament_id', 'match_number']); } catch(\Exception $e) {}
            try { $table->dropIndex(['tournament_id', 'scheduled_at']); } catch(\Exception $e) {}
            try { $table->dropIndex(['tournament_id', 'status']); } catch(\Exception $e) {}
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            
            try { $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete(); } catch(\Exception $e) {}
            try { $table->unique(['tournament_id', 'match_number']); } catch(\Exception $e) {}
            try { $table->index(['tournament_id', 'scheduled_at']); } catch(\Exception $e) {}
            try { $table->index(['tournament_id', 'status']); } catch(\Exception $e) {}
        });

        // 3. Matches
        Schema::table('matches', function (Blueprint $table) {
            try { $table->dropForeign(['tournament_id']); } catch(\Exception $e) {}
            try { $table->dropIndex('matches_tournament_id_status_index'); } catch(\Exception $e) {}
            try { $table->dropIndex(['tournament_id', 'status']); } catch(\Exception $e) {}
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            
            try { $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete(); } catch(\Exception $e) {}
            try { $table->index(['tournament_id', 'status']); } catch(\Exception $e) {}
        });

        // 4. Stages
        Schema::table('stages', function (Blueprint $table) {
            try { $table->dropForeign(['tournament_id']); } catch(\Exception $e) {}
            try { $table->dropIndex('stages_tournament_id_order_index'); } catch(\Exception $e) {}
            try { $table->dropIndex('stages_tournament_id_status_index'); } catch(\Exception $e) {}
            try { $table->dropIndex(['tournament_id', 'order']); } catch(\Exception $e) {}
            try { $table->dropIndex(['tournament_id', 'status']); } catch(\Exception $e) {}
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            
            try { $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete(); } catch(\Exception $e) {}
            try { $table->index(['tournament_id', 'order']); } catch(\Exception $e) {}
            try { $table->index(['tournament_id', 'status']); } catch(\Exception $e) {}
        });

        // 5. Match Players
        Schema::table('match_players', function (Blueprint $table) {
            try { $table->dropForeign(['tournament_player_id']); } catch(\Exception $e) {}
            try { $table->dropUnique('match_players_match_id_tournament_player_id_unique'); } catch(\Exception $e) {}
            try { $table->dropUnique(['match_id', 'tournament_player_id']); } catch(\Exception $e) {}
            
            $table->unsignedBigInteger('tournament_player_id')->nullable()->change();
            
            try { $table->foreign('tournament_player_id')->references('id')->on('tournament_players')->restrictOnDelete(); } catch(\Exception $e) {}
            
            if (!Schema::hasColumn('match_players', 'player_profile_id')) {
                $table->foreignId('player_profile_id')->nullable()->after('tournament_player_id')->constrained('player_profiles')->restrictOnDelete();
                try { $table->unique(['match_id', 'player_profile_id']); } catch(\Exception $e) {}
            }
            
            try { $table->unique(['match_id', 'tournament_player_id']); } catch(\Exception $e) {}
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
