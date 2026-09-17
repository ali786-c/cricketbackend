<?php

use App\Models\CricketMatch;
use App\Models\Fixture;
use App\Models\Tournament;
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Tournament::query()->whereNull('creator_id')->eachById(function (Tournament $tournament) {
            $candidates = collect()
                ->merge(CricketMatch::query()->where('tournament_id', $tournament->id)->whereNotNull('created_by')->pluck('created_by'))
                ->merge(Fixture::query()->where('tournament_id', $tournament->id)->whereNotNull('created_by')->pluck('created_by'))
                ->map(fn ($id) => (int) $id)->unique()->values();

            if ($candidates->count() === 1) {
                DB::table('tournaments')->where('id', $tournament->id)->update(['creator_id' => $candidates->first()]);
            } else {
                $this->recordUnresolved(Tournament::class, $tournament->id, $tournament->id, $candidates->all());
            }
        });

        DB::table('fixtures')->whereNull('created_by')->eachById(function ($fixture) {
            $ownerId = $fixture->tournament_id
                ? DB::table('tournaments')->where('id', $fixture->tournament_id)->value('creator_id')
                : null;
            if ($ownerId) {
                DB::table('fixtures')->where('id', $fixture->id)->update(['created_by' => $ownerId]);
            } else {
                $this->recordUnresolved(Fixture::class, $fixture->id, $fixture->tournament_id, []);
            }
        });

        DB::table('matches')->whereNull('created_by')->eachById(function ($match) {
            $ownerId = $match->tournament_id
                ? DB::table('tournaments')->where('id', $match->tournament_id)->value('creator_id')
                : DB::table('fixtures')->where('id', $match->fixture_id)->value('created_by');
            if ($ownerId) {
                DB::table('matches')->where('id', $match->id)->update(['created_by' => $ownerId]);
            } else {
                $this->recordUnresolved(CricketMatch::class, $match->id, $match->tournament_id, []);
            }
        });

        DB::table('fixtures')->whereNotNull('tournament_id')->whereNotNull('created_by')->eachById(function ($fixture) {
            $ownerId = DB::table('tournaments')->where('id', $fixture->tournament_id)->value('creator_id');
            if ($ownerId && (int) $ownerId !== (int) $fixture->created_by) {
                $this->recordIssue('ownership.owner_mismatch', Fixture::class, $fixture->id, $fixture->tournament_id, [
                    'canonical_owner_user_id' => (int) $ownerId,
                    'recorded_owner_user_id' => (int) $fixture->created_by,
                ]);
            }
        });

        DB::table('matches')->whereNotNull('created_by')->eachById(function ($match) {
            $ownerId = $match->tournament_id
                ? DB::table('tournaments')->where('id', $match->tournament_id)->value('creator_id')
                : DB::table('fixtures')->where('id', $match->fixture_id)->value('created_by');
            if ($ownerId && (int) $ownerId !== (int) $match->created_by) {
                $this->recordIssue('ownership.owner_mismatch', CricketMatch::class, $match->id, $match->tournament_id, [
                    'canonical_owner_user_id' => (int) $ownerId,
                    'recorded_owner_user_id' => (int) $match->created_by,
                ]);
            }
        });

        Schema::table('tournaments', fn (Blueprint $table) => $table->index(['creator_id', 'status'], 'tournaments_creator_status_index'));
        Schema::table('fixtures', fn (Blueprint $table) => $table->index(['created_by', 'tournament_id'], 'fixtures_creator_tournament_index'));
        Schema::table('matches', fn (Blueprint $table) => $table->index(['created_by', 'tournament_id'], 'matches_creator_tournament_index'));
    }

    public function down(): void
    {
        Schema::table('matches', fn (Blueprint $table) => $table->dropIndex('matches_creator_tournament_index'));
        Schema::table('fixtures', fn (Blueprint $table) => $table->dropIndex('fixtures_creator_tournament_index'));
        Schema::table('tournaments', fn (Blueprint $table) => $table->dropIndex('tournaments_creator_status_index'));
    }

    private function recordUnresolved(string $type, int $id, ?int $tournamentId, array $candidates): void
    {
        $this->recordIssue('ownership.backfill_unresolved', $type, $id, $tournamentId, [
            'candidate_user_ids' => $candidates,
        ]);
    }

    private function recordIssue(string $action, string $type, int $id, ?int $tournamentId, array $metadata): void
    {
        DB::table('audit_logs')->insert([
            'tournament_id' => $tournamentId,
            'action' => $action,
            'auditable_type' => $type,
            'auditable_id' => $id,
            'metadata' => json_encode($metadata),
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }
};
