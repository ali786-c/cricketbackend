<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('fixtures', function (Blueprint $table): void {
            $table->unsignedBigInteger('stage_id')->nullable()->after('tournament_id');
            $table->string('stage_name')->nullable()->after('stage_id');
            $table->string('match_type')->nullable()->default('normal')->after('status');
            $table->string('umpire1')->nullable()->after('match_type');
            $table->string('umpire2')->nullable()->after('umpire1');
        });
    }

    public function down(): void
    {
        Schema::table('fixtures', function (Blueprint $table): void {
            $table->dropColumn([
                'stage_id',
                'stage_name',
                'match_type',
                'umpire1',
                'umpire2',
            ]);
        });
    }
};
