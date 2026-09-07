<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('player_profiles', function (Blueprint $table) {
            // Drop existing foreign key and unique constraint
            $table->dropForeign(['user_id']);
            $table->dropUnique(['user_id']);
            
            // Modify column to be nullable
            $table->unsignedBigInteger('user_id')->nullable()->change();
            
            // Re-add foreign key constraint without unique (since multiple guests might claim? No, user_id should be unique if not null, but MySQL unique treats nulls as distinct so unique works)
            $table->unique('user_id');
            $table->foreign('user_id')->references('id')->on('users')->cascadeOnDelete();
            
            // Add is_guest column
            $table->boolean('is_guest')->default(false)->after('bio');
        });
    }

    public function down(): void
    {
        Schema::table('player_profiles', function (Blueprint $table) {
            $table->dropColumn('is_guest');
            
            // Revert user_id to not null
            $table->dropForeign(['user_id']);
            $table->dropUnique(['user_id']);
            
            $table->unsignedBigInteger('user_id')->nullable(false)->change();
            
            $table->unique('user_id');
            $table->foreign('user_id')->references('id')->on('users')->cascadeOnDelete();
        });
    }
};
