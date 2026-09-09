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
        try { Schema::table('teams', function (Blueprint $table) { $table->dropForeign(['tournament_id']); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropUnique('teams_tournament_id_name_unique'); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropUnique('teams_tournament_id_display_order_unique'); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropUnique(['tournament_id', 'name']); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->dropUnique(['tournament_id', 'display_order']); }); } catch (\Exception $e) {}
        
        Schema::table('teams', function (Blueprint $table) {
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
        });

        try { Schema::table('teams', function (Blueprint $table) { $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete(); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->unique(['tournament_id', 'name']); }); } catch (\Exception $e) {}
        try { Schema::table('teams', function (Blueprint $table) { $table->unique(['tournament_id', 'display_order']); }); } catch (\Exception $e) {}

        // 2. Fixtures
        try { Schema::table('fixtures', function (Blueprint $table) { $table->dropForeign(['tournament_id']); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->dropUnique('fixtures_tournament_id_match_number_unique'); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->dropIndex('fixtures_tournament_id_scheduled_at_index'); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->dropIndex('fixtures_tournament_id_status_index'); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->dropUnique(['tournament_id', 'match_number']); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->dropIndex(['tournament_id', 'scheduled_at']); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->dropIndex(['tournament_id', 'status']); }); } catch (\Exception $e) {}

        Schema::table('fixtures', function (Blueprint $table) {
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
        });

        try { Schema::table('fixtures', function (Blueprint $table) { $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete(); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->unique(['tournament_id', 'match_number']); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->index(['tournament_id', 'scheduled_at']); }); } catch (\Exception $e) {}
        try { Schema::table('fixtures', function (Blueprint $table) { $table->index(['tournament_id', 'status']); }); } catch (\Exception $e) {}

        // 3. Matches
        try { Schema::table('matches', function (Blueprint $table) { $table->dropForeign(['tournament_id']); }); } catch (\Exception $e) {}
        try { Schema::table('matches', function (Blueprint $table) { $table->dropIndex('matches_tournament_id_status_index'); }); } catch (\Exception $e) {}
        try { Schema::table('matches', function (Blueprint $table) { $table->dropIndex(['tournament_id', 'status']); }); } catch (\Exception $e) {}
        
        Schema::table('matches', function (Blueprint $table) {
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
        });
        
        try { Schema::table('matches', function (Blueprint $table) { $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete(); }); } catch (\Exception $e) {}
        try { Schema::table('matches', function (Blueprint $table) { $table->index(['tournament_id', 'status']); }); } catch (\Exception $e) {}

        // 4. Stages
        try { Schema::table('stages', function (Blueprint $table) { $table->dropForeign(['tournament_id']); }); } catch (\Exception $e) {}
        try { Schema::table('stages', function (Blueprint $table) { $table->dropIndex('stages_tournament_id_order_index'); }); } catch (\Exception $e) {}
        try { Schema::table('stages', function (Blueprint $table) { $table->dropIndex('stages_tournament_id_status_index'); }); } catch (\Exception $e) {}
        try { Schema::table('stages', function (Blueprint $table) { $table->dropIndex(['tournament_id', 'order']); }); } catch (\Exception $e) {}
        try { Schema::table('stages', function (Blueprint $table) { $table->dropIndex(['tournament_id', 'status']); }); } catch (\Exception $e) {}

        Schema::table('stages', function (Blueprint $table) {
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
        });
        
        try { Schema::table('stages', function (Blueprint $table) { $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete(); }); } catch (\Exception $e) {}
        try { Schema::table('stages', function (Blueprint $table) { $table->index(['tournament_id', 'order']); }); } catch (\Exception $e) {}
        try { Schema::table('stages', function (Blueprint $table) { $table->index(['tournament_id', 'status']); }); } catch (\Exception $e) {}

        // 5. Match Players
        try { Schema::table('match_players', function (Blueprint $table) { $table->dropForeign(['tournament_player_id']); }); } catch (\Exception $e) {}
        try { Schema::table('match_players', function (Blueprint $table) { $table->dropUnique('match_players_match_id_tournament_player_id_unique'); }); } catch (\Exception $e) {}
        try { Schema::table('match_players', function (Blueprint $table) { $table->dropUnique(['match_id', 'tournament_player_id']); }); } catch (\Exception $e) {}
        
        Schema::table('match_players', function (Blueprint $table) {
            $table->unsignedBigInteger('tournament_player_id')->nullable()->change();
        });
        
        try { Schema::table('match_players', function (Blueprint $table) { $table->foreign('tournament_player_id')->references('id')->on('tournament_players')->restrictOnDelete(); }); } catch (\Exception $e) {}
        
        if (!Schema::hasColumn('match_players', 'player_profile_id')) {
            Schema::table('match_players', function (Blueprint $table) {
                $table->foreignId('player_profile_id')->nullable()->after('tournament_player_id')->constrained('player_profiles')->restrictOnDelete();
            });
            try { Schema::table('match_players', function (Blueprint $table) { $table->unique(['match_id', 'player_profile_id']); }); } catch (\Exception $e) {}
        }
        
        try { Schema::table('match_players', function (Blueprint $table) { $table->unique(['match_id', 'tournament_player_id']); }); } catch (\Exception $e) {}
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
