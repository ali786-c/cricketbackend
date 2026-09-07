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
        Schema::create('player_stats', function (Blueprint $table) {
            $table->id();
            $table->foreignId('player_profile_id')->constrained()->cascadeOnDelete();
            $table->integer('matches_played')->default(0);
            $table->integer('runs_scored')->default(0);
            $table->integer('wickets_taken')->default(0);
            $table->decimal('batting_average', 5, 2)->nullable();
            $table->decimal('bowling_average', 5, 2)->nullable();
            $table->decimal('strike_rate', 5, 2)->nullable();
            $table->decimal('economy_rate', 5, 2)->nullable();
            $table->integer('highest_score')->nullable();
            $table->string('best_bowling')->nullable();
            $table->integer('fifties')->default(0);
            $table->integer('hundreds')->default(0);
            $table->integer('catches')->default(0);
            $table->timestamps();

            $table->unique('player_profile_id'); // One stats record per player
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('player_stats');
    }
};
