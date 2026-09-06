<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('tournaments', function (Blueprint $table): void {
            $table->string('organizer_name')->nullable()->after('description');
            $table->string('contact_info')->nullable()->after('organizer_name');
            $table->string('competition_structure')->nullable()->default('League')->after('default_overs_per_innings');
            $table->string('tournament_code')->nullable()->unique()->after('competition_structure');
        });
    }

    public function down(): void
    {
        Schema::table('tournaments', function (Blueprint $table): void {
            $table->dropColumn([
                'organizer_name',
                'contact_info',
                'competition_structure',
                'tournament_code',
            ]);
        });
    }
};
