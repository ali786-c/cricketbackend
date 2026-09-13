<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::table('match_deliveries', function (Blueprint $table) {
            $table->uuid('undo_uuid')->nullable()->unique()->after('void_reason');
        });
    }

    public function down(): void
    {
        Schema::table('match_deliveries', fn (Blueprint $table) => $table->dropColumn('undo_uuid'));
    }
};
