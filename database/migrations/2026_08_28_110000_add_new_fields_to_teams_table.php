<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('teams', function (Blueprint $table): void {
            $table->string('vice_captain_name')->nullable()->after('short_name');
            $table->string('manager_name')->nullable()->after('vice_captain_name');
            $table->string('wicketkeeper_name')->nullable()->after('manager_name');
            $table->string('status')->nullable()->default('pending')->after('is_active');
        });
    }

    public function down(): void
    {
        Schema::table('teams', function (Blueprint $table): void {
            $table->dropColumn([
                'vice_captain_name',
                'manager_name',
                'wicketkeeper_name',
                'status',
            ]);
        });
    }
};
