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
            $table->dropForeign(['tournament_id']);

            $table->dropUnique(['tournament_id', 'name']);
            $table->dropUnique(['tournament_id', 'display_order']);
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete();
            
            $table->unique(['tournament_id', 'name']);
            $table->unique(['tournament_id', 'display_order']);
        });

        // 2. Fixtures
        Schema::table('fixtures', function (Blueprint $table) {
            $table->dropForeign(['tournament_id']);

            $table->dropUnique(['tournament_id', 'match_number']);
            $table->dropIndex(['tournament_id', 'scheduled_at']);
            $table->dropIndex(['tournament_id', 'status']);
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete();
            
            $table->unique(['tournament_id', 'match_number']);
            $table->index(['tournament_id', 'scheduled_at']);
            $table->index(['tournament_id', 'status']);
        });

        // 3. Matches
        Schema::table('matches', function (Blueprint $table) {
            $table->dropForeign(['tournament_id']);

            $table->dropIndex(['tournament_id', 'status']);
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete();
            
            $table->index(['tournament_id', 'status']);
        });

        // 4. Stages
        Schema::table('stages', function (Blueprint $table) {
            $table->dropForeign(['tournament_id']);

            $table->dropIndex(['tournament_id', 'order']);
            $table->dropIndex(['tournament_id', 'status']);
            
            $table->unsignedBigInteger('tournament_id')->nullable()->change();
            $table->foreign('tournament_id')->references('id')->on('tournaments')->cascadeOnDelete();
            
            $table->index(['tournament_id', 'order']);
            $table->index(['tournament_id', 'status']);
        });

        // 5. Match Players
        Schema::table('match_players', function (Blueprint $table) {
            $table->dropForeign(['tournament_player_id']);

            $table->dropUnique(['match_id', 'tournament_player_id']);
            
            $table->unsignedBigInteger('tournament_player_id')->nullable()->change();
            $table->foreign('tournament_player_id')->references('id')->on('tournament_players')->restrictOnDelete();
            
            $table->foreignId('player_profile_id')->nullable()->after('tournament_player_id')->constrained('player_profiles')->restrictOnDelete();
            
            $table->unique(['match_id', 'player_profile_id']);
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
