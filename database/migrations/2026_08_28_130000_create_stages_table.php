<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('stages', function (Blueprint $table): void {
            $table->id();
            $table->foreignId('tournament_id')->constrained()->cascadeOnDelete();
            $table->string('name');
            $table->string('type')->default('points_table');
            $table->unsignedSmallInteger('order')->default(0);
            $table->unsignedSmallInteger('number_of_teams')->default(0);
            $table->unsignedSmallInteger('matches_per_team')->default(0);
            $table->unsignedSmallInteger('points_for_win')->default(2);
            $table->unsignedSmallInteger('points_for_tie')->default(1);
            $table->unsignedSmallInteger('points_for_no_result')->default(1);
            $table->unsignedSmallInteger('points_for_loss')->default(0);
            $table->string('qualification_rule')->default('top_2');
            $table->unsignedSmallInteger('qualification_count')->default(2);
            $table->string('status')->default('draft');
            $table->unsignedSmallInteger('teams_count')->default(0);
            $table->unsignedSmallInteger('matches_count')->default(0);
            $table->unsignedSmallInteger('completed_matches')->default(0);
            $table->timestamps();

            $table->index(['tournament_id', 'order']);
            $table->index(['tournament_id', 'status']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('stages');
    }
};
