<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('match_innings', function (Blueprint $table) {
            $table->foreignId('current_striker_id')->nullable()->after('bowling_team_id')->constrained('match_players')->nullOnDelete();
            $table->foreignId('current_non_striker_id')->nullable()->after('current_striker_id')->constrained('match_players')->nullOnDelete();
            $table->foreignId('current_bowler_id')->nullable()->after('current_non_striker_id')->constrained('match_players')->nullOnDelete();
        });
    }

    public function down(): void
    {
        Schema::table('match_innings', function (Blueprint $table) {
            $table->dropConstrainedForeignId('current_bowler_id');
            $table->dropConstrainedForeignId('current_non_striker_id');
            $table->dropConstrainedForeignId('current_striker_id');
        });
    }
};
