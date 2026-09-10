<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('fixtures', function (Blueprint $table): void {
            $table->uuid('client_uuid')->nullable()->unique()->after('id');
            $table->json('configuration_snapshot')->nullable()->after('notes');
        });

        Schema::table('matches', function (Blueprint $table): void {
            $table->uuid('client_uuid')->nullable()->unique()->after('id');
            $table->json('rule_snapshot')->nullable()->after('rule_profile_version');
        });
    }

    public function down(): void
    {
        Schema::table('matches', function (Blueprint $table): void {
            $table->dropUnique(['client_uuid']);
            $table->dropColumn(['client_uuid', 'rule_snapshot']);
        });

        Schema::table('fixtures', function (Blueprint $table): void {
            $table->dropUnique(['client_uuid']);
            $table->dropColumn(['client_uuid', 'configuration_snapshot']);
        });
    }
};
