<?php

namespace App\Modules\Draft\Services;

use App\Models\AuditLog;
use App\Models\Draft;
use App\Models\DraftPick;
use App\Models\DraftRound;
use App\Models\TournamentPlayer;
use App\Models\User;
use Illuminate\Database\DatabaseManager;
use Illuminate\Validation\ValidationException;

class DraftService
{
    public function __construct(private readonly DatabaseManager $database) {}

    public function startDraft(Draft $draft, User $actor): Draft
    {
        return $this->startRound($draft, $actor);
    }

    public function startRound(Draft $draft, User $actor): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor) {
            $draft = Draft::query()->with('tournament')->lockForUpdate()->findOrFail($draft->id);

            if (! $actor->can('control draft')) {
                $this->fail('draft', 'You are not authorized to start the next pick.');
            }

            if (! in_array($draft->status, ['setup', 'paused'], true)) {
                $this->fail('draft', 'The next pick can only be started while the draft is paused or in setup.');
            }

            if ($draft->picks()->where('status', 'active')->exists()) {
                $this->fail('draft', 'The current pick is already active.');
            }

            $nextPick = $draft->picks()
                ->where('status', 'pending')
                ->orderBy('pick_number')
                ->first();

            if (! $nextPick) {
                $this->fail('pick', 'There is no pending pick ready to start.');
            }

            $nextRound = $nextPick->round()->first();
            $hasIncompleteEarlierRound = $draft->rounds()
                ->where('round_number', '<', $nextRound->round_number)
                ->where('status', '!=', 'completed')
                ->exists();

            if ($hasIncompleteEarlierRound) {
                $this->fail('round', 'Complete the previous round before starting this pick.');
            }

            $now = now();
            $nextRound->update([
                'status' => 'active',
                'started_at' => $nextRound->started_at ?: $now,
                'completed_at' => null,
            ]);
            $nextPick->update([
                'status' => 'active',
                'started_at' => $now,
                'expired_at' => null,
            ]);

            $draft->update([
                'status' => 'live',
                'current_pick_number' => $nextPick->pick_number,
                'pick_started_at' => $now,
                'pick_duration' => $nextPick->pick_duration,
                'started_at' => $draft->started_at ?: $now,
                'paused_at' => null,
                'revision' => $draft->revision + 1,
            ]);

            if ($draft->tournament->status === 'ready') {
                $draft->tournament->update([
                    'status' => 'live',
                    'published_at' => $draft->tournament->published_at ?: $now,
                ]);
            }

            $action = $draft->getOriginal('status') === 'setup' ? 'draft.started' : 'draft.pick_started';
            $this->audit($actor, $draft, $action, null, $draft->fresh()->toArray(), [
                'round_number' => $nextRound->round_number,
                'pick_number' => $nextPick->pick_number,
            ]);

            return $draft->fresh(['tournament', 'picks.team', 'picks.round']);
        });
    }

    public function makePick(Draft $draft, User $captain, TournamentPlayer $player): DraftPick
    {
        $expired = false;

        $result = $this->database->transaction(function () use ($draft, $captain, $player, &$expired) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);
            $activePick = $draft->picks()->where('status', 'active')->lockForUpdate()->first();

            if (! $activePick) {
                $this->fail('draft', 'There is no active pick right now.');
            }

            if ($draft->status !== 'live') {
                $this->fail('draft', 'The draft is not accepting picks right now.');
            }

            if (! $captain->can('make draft pick')) {
                $this->fail('captain', 'You are not authorized to make a draft pick.');
            }

            $isCaptainOfTeam = $activePick->team
                ->captainAssignments()
                ->where('user_id', $captain->id)
                ->whereNull('revoked_at')
                ->exists();

            if (! $isCaptainOfTeam) {
                $this->fail('captain', 'It is not your team\'s turn.');
            }

            if ($activePick->started_at?->addSeconds($activePick->pick_duration)->isPast()) {
                $this->markExpired($draft, $activePick, $captain);
                $expired = true;

                return null;
            }

            $player = TournamentPlayer::query()->lockForUpdate()->findOrFail($player->id);

            if ($player->tournament_id !== $draft->tournament->id || $player->status !== 'approved') {
                $this->fail('player', 'This player is not approved for this tournament.');
            }

            $alreadySelected = $draft->picks()
                ->where('tournament_player_id', $player->id)
                ->where('status', 'selected')
                ->exists();

            if ($alreadySelected) {
                $this->fail('player', 'This player has already been selected.');
            }

            $activePick->update([
                'status' => 'selected',
                'tournament_player_id' => $player->id,
                'selected_by' => $captain->id,
                'selected_at' => now(),
            ]);

            $this->advanceToNextPick($draft, $activePick, $captain);

            return $activePick->fresh(['team', 'round', 'tournamentPlayer.playerProfile']);
        });

        if ($expired) {
            $this->fail('timer', 'The pick timer has expired. Please wait for the administrator.');
        }

        return $result;
    }

    public function adminSelectPlayer(Draft $draft, User $actor, int $pickNumber, TournamentPlayer $player): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor, $pickNumber, $player) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);

            if (! $actor->can('control draft')) {
                $this->fail('draft', 'You are not authorized to make an admin selection.');
            }

            $pick = $draft->picks()->where('pick_number', $pickNumber)->lockForUpdate()->first();
            if (! $pick || ! in_array($pick->status, ['active', 'expired', 'pending'], true)) {
                $this->fail('pick', 'This pick is not available for an admin selection.');
            }

            $currentPick = $draft->picks()->whereIn('status', ['active', 'expired'])->where('id', '<>', $pick->id)->lockForUpdate()->first();
            if ($pick->status === 'pending') {
                if ($currentPick || ! in_array($draft->status, ['setup', 'paused'], true)) {
                    $this->fail('pick', 'Only the next paused pick can be selected manually.');
                }

                $nextPending = $draft->picks()->where('status', 'pending')->orderBy('pick_number')->first();
                if (! $nextPending || $nextPending->id !== $pick->id) {
                    $this->fail('pick', 'Only the next pending pick can be selected manually.');
                }
            } elseif (! in_array($draft->status, ['live', 'paused', 'expired'], true)) {
                $this->fail('draft', 'The current pick is not available for manual selection.');
            }

            $player = TournamentPlayer::query()->lockForUpdate()->findOrFail($player->id);
            if ($player->tournament_id !== $draft->tournament_id || $player->status !== 'approved') {
                $this->fail('player', 'This player is not approved for this tournament.');
            }

            $alreadySelected = $draft->picks()
                ->where('tournament_player_id', $player->id)
                ->where('status', 'selected')
                ->exists();
            if ($alreadySelected) {
                $this->fail('player', 'This player has already been selected.');
            }

            $before = $pick->toArray();
            $pick->round()->update([
                'status' => 'active',
                'started_at' => $pick->round->started_at ?: now(),
                'completed_at' => null,
            ]);
            $pick->update([
                'status' => 'selected',
                'tournament_player_id' => $player->id,
                'selected_by' => $actor->id,
                'selected_at' => now(),
                'expired_at' => null,
                'skipped_at' => null,
            ]);

            $this->audit($actor, $draft, 'draft.admin_player_selected', $before, $pick->fresh()->toArray(), [
                'pick_number' => $pick->pick_number,
                'player_id' => $player->id,
            ]);
            $this->advanceToNextPick($draft, $pick, $actor);

            return $draft->fresh(['picks.team', 'picks.round']);
        });
    }

    public function reassignPlayer(Draft $draft, User $actor, int $fromPickNumber, int $toPickNumber): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor, $fromPickNumber, $toPickNumber) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);

            if (! $actor->can('control draft')) {
                $this->fail('draft', 'You are not authorized to reassign a player.');
            }
            if ($fromPickNumber === $toPickNumber) {
                $this->fail('pick', 'Choose a different destination pick.');
            }
            if (! in_array($draft->status, ['paused', 'completed'], true) || $draft->picks()->whereIn('status', ['active', 'expired'])->exists()) {
                $this->fail('draft', 'Reassignment is only available while no pick is active.');
            }

            $sourcePick = $draft->picks()->where('pick_number', $fromPickNumber)->where('status', 'selected')->lockForUpdate()->first();
            $targetPick = $draft->picks()->where('pick_number', $toPickNumber)->where('status', 'pending')->lockForUpdate()->first();
            if (! $sourcePick || ! $targetPick) {
                $this->fail('pick', 'The source must be selected and the destination must be pending.');
            }

            $before = [
                'source' => $sourcePick->toArray(),
                'target' => $targetPick->toArray(),
            ];
            $playerId = $sourcePick->tournament_player_id;
            $selectedBy = $sourcePick->selected_by;
            $selectedAt = $sourcePick->selected_at;

            $sourcePick->update([
                'status' => 'pending',
                'tournament_player_id' => null,
                'selected_by' => null,
                'selected_at' => null,
                'started_at' => null,
                'expired_at' => null,
                'skipped_at' => null,
            ]);
            $targetPick->round()->update([
                'status' => 'active',
                'started_at' => $targetPick->round->started_at ?: now(),
                'completed_at' => null,
            ]);
            $targetPick->update([
                'status' => 'selected',
                'tournament_player_id' => $playerId,
                'selected_by' => $selectedBy ?: $actor->id,
                'selected_at' => $selectedAt ?: now(),
                'expired_at' => null,
                'skipped_at' => null,
            ]);
            $sourcePick->round()->update([
                'status' => 'active',
                'completed_at' => null,
            ]);

            $draft->update([
                'status' => 'paused',
                'current_pick_number' => null,
                'pick_started_at' => null,
                'pick_duration' => null,
                'paused_at' => now(),
                'completed_at' => null,
                'revision' => $draft->revision + 1,
            ]);
            if ($draft->tournament()->where('status', 'completed')->exists()) {
                $draft->tournament()->update(['status' => 'live']);
            }

            $this->audit($actor, $draft, 'draft.admin_player_reassigned', $before, [
                'source' => $sourcePick->fresh()->toArray(),
                'target' => $targetPick->fresh()->toArray(),
            ], [
                'from_pick_number' => $fromPickNumber,
                'to_pick_number' => $toPickNumber,
                'player_id' => $playerId,
            ]);

            return $draft->fresh(['picks.team', 'picks.round']);
        });
    }

    public function removeSelectedPlayer(Draft $draft, User $actor, int $pickNumber): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor, $pickNumber) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);

            if (! $actor->can('control draft')) {
                $this->fail('draft', 'You are not authorized to remove a selected player.');
            }

            $pick = $draft->picks()->where('pick_number', $pickNumber)->where('status', 'selected')->lockForUpdate()->first();
            if (! $pick) {
                $this->fail('pick', 'Only a selected pick can be removed.');
            }

            $before = $pick->toArray();
            $wasCompleted = $draft->status === 'completed' || $draft->tournament()->where('status', 'completed')->exists();
            $currentActivePick = $draft->picks()
                ->whereIn('status', ['active', 'expired'])
                ->where('id', '<>', $pick->id)
                ->lockForUpdate()
                ->first();
            if ($currentActivePick) {
                $currentActivePick->update([
                    'status' => 'pending',
                    'started_at' => null,
                    'expired_at' => null,
                ]);
                if ($currentActivePick->draft_round_id !== $pick->draft_round_id) {
                    $currentActivePick->round()->update([
                        'status' => 'pending',
                        'started_at' => null,
                        'completed_at' => null,
                    ]);
                }
            }
            $pick->update([
                'status' => 'pending',
                'tournament_player_id' => null,
                'selected_by' => null,
                'selected_at' => null,
                'started_at' => null,
                'expired_at' => null,
                'skipped_at' => null,
            ]);
            $pick->round()->update([
                'status' => 'active',
                'completed_at' => null,
            ]);

            $pausedAt = now();
            $draft->update([
                'status' => 'paused',
                'current_pick_number' => null,
                'pick_started_at' => null,
                'pick_duration' => null,
                'paused_at' => $pausedAt,
                'completed_at' => null,
                'revision' => $draft->revision + 1,
            ]);
            if ($wasCompleted) {
                $draft->tournament()->update(['status' => 'live']);
            }

            $this->audit($actor, $draft, 'draft.admin_player_removed', $before, $pick->fresh()->toArray(), ['pick_number' => $pick->pick_number]);

            return $draft->fresh(['picks.team', 'picks.round']);
        });
    }

    public function extendTime(Draft $draft, User $actor, int $seconds): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor, $seconds) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);
            $activePick = $draft->picks()->whereIn('status', ['active', 'expired'])->lockForUpdate()->first();

            if (! $actor->can('control draft')) {
                $this->fail('draft', 'You are not authorized to extend the timer.');
            }

            if (! $activePick || ! in_array($draft->status, ['live', 'expired'], true)) {
                $this->fail('draft', 'There is no expired or active pick to extend.');
            }

            if ($seconds < 1 || $seconds > 3600) {
                $this->fail('seconds', 'Extension must be between 1 and 3600 seconds.');
            }

            $now = now();
            $hasExpired = $draft->status === 'expired'
                || $activePick->status === 'expired'
                || $activePick->started_at?->addSeconds($activePick->pick_duration)->isPast();
            $startedAt = $hasExpired ? $now : ($activePick->started_at ?: $now);
            $newDuration = $hasExpired ? $seconds : ((int) $activePick->pick_duration + $seconds);

            $activePick->update([
                'status' => 'active',
                'started_at' => $startedAt,
                'expired_at' => null,
                'pick_duration' => $newDuration,
                'extension_count' => $activePick->extension_count + 1,
                'total_extension_seconds' => $activePick->total_extension_seconds + $seconds,
            ]);

            $draft->update([
                'status' => 'live',
                'pick_started_at' => $startedAt,
                'pick_duration' => $newDuration,
                'paused_at' => null,
                'revision' => $draft->revision + 1,
            ]);

            $this->audit($actor, $draft, 'draft.timer_extended', $activePick->getOriginal(), $activePick->fresh()->toArray(), ['seconds' => $seconds]);

            return $draft->fresh(['picks.team', 'picks.round']);
        });
    }

    public function skipExpiredPick(Draft $draft, User $actor): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);
            $activePick = $draft->picks()->whereIn('status', ['active', 'expired'])->lockForUpdate()->first();

            if (! $actor->can('control draft')) {
                $this->fail('draft', 'You are not authorized to skip a pick.');
            }

            if (! $activePick || $draft->status !== 'expired') {
                $this->fail('draft', 'Only an expired active pick can be skipped.');
            }

            $activePick->update([
                'status' => 'skipped',
                'skipped_at' => now(),
            ]);

            $this->advanceToNextPick($draft, $activePick, $actor);

            return $draft->fresh(['picks.team', 'picks.round']);
        });
    }

    public function pauseDraft(Draft $draft, User $actor): Draft
    {
        return $this->setDraftStatus($draft, $actor, 'paused', 'draft.paused');
    }

    public function resumeDraft(Draft $draft, User $actor): Draft
    {
        return $this->setDraftStatus($draft, $actor, 'live', 'draft.resumed');
    }

    public function undoLatestPick(Draft $draft, User $actor): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);
            $latestPick = $draft->picks()->where('status', 'selected')->latest('selected_at')->lockForUpdate()->first();

            if (! $actor->can('undo latest pick')) {
                $this->fail('draft', 'You are not authorized to undo a pick.');
            }

            if (! $latestPick) {
                $this->fail('draft', 'There is no selected pick to undo.');
            }

            $before = $latestPick->toArray();
            $currentActivePick = $draft->picks()
                ->where('status', 'active')
                ->where('id', '<>', $latestPick->id)
                ->lockForUpdate()
                ->first();

            if ($currentActivePick) {
                $currentActivePick->update([
                    'status' => 'pending',
                    'started_at' => null,
                ]);
            }

            $latestPick->round()->update([
                'status' => 'active',
                'completed_at' => null,
            ]);
            $latestPick->update([
                'status' => 'active',
                'tournament_player_id' => null,
                'selected_by' => null,
                'selected_at' => null,
                'skipped_at' => null,
            ]);

            $draft->update([
                'status' => 'live',
                'current_pick_number' => $latestPick->pick_number,
                'pick_started_at' => now(),
                'pick_duration' => $latestPick->pick_duration,
                'revision' => $draft->revision + 1,
            ]);

            $this->audit($actor, $draft, 'draft.pick_undone', $before, $latestPick->fresh()->toArray());

            return $draft->fresh(['picks.team', 'picks.round']);
        });
    }

    public function resetDraft(Draft $draft, User $actor): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);

            if (! $actor->can('control draft')) {
                $this->fail('draft', 'You are not authorized to reset the draft.');
            }

            $before = $draft->toArray();

            $draft->picks()->update([
                'status' => 'pending',
                'tournament_player_id' => null,
                'selected_by' => null,
                'selected_at' => null,
                'started_at' => null,
                'skipped_at' => null,
            ]);

            $draft->rounds()->update([
                'status' => 'pending',
                'started_at' => null,
                'completed_at' => null,
            ]);

            $minPickNum = $draft->picks()->min('pick_number') ?: 1;

            $draft->update([
                'status' => 'setup',
                'current_pick_number' => $minPickNum,
                'pick_started_at' => null,
                'revision' => $draft->revision + 1,
            ]);

            $this->audit($actor, $draft, 'draft.reset', $before, $draft->fresh()->toArray());

            return $draft->fresh(['picks.team', 'picks.round']);
        });
    }

    public function state(Draft $draft, ?User $viewer = null): array
    {
        $draft->loadMissing([
            'tournament',
            'picks.team.activeCaptain.user',
            'picks.round',
            'picks.tournamentPlayer.playerProfile',
            'rounds.picks',
        ]);

        $activePick = $draft->picks->firstWhere('status', 'active')
            ?? $draft->picks->firstWhere('status', 'expired');

        if ($draft->status === 'live' && $activePick?->started_at?->addSeconds($activePick->pick_duration)->isPast()) {
            $this->database->transaction(function () use ($draft, $viewer) {
                $lockedDraft = Draft::query()->lockForUpdate()->findOrFail($draft->id);
                $lockedPick = $lockedDraft->picks()->where('status', 'active')->lockForUpdate()->first();

                if ($lockedDraft->status === 'live' && $lockedPick?->started_at?->addSeconds($lockedPick->pick_duration)->isPast()) {
                    $this->markExpired($lockedDraft, $lockedPick, $viewer);
                }
            });

            $draft->refresh()->load([
                'tournament',
                'picks.team.activeCaptain.user',
                'picks.round',
                'picks.tournamentPlayer.playerProfile',
            ]);
            $activePick = $draft->picks->firstWhere('status', 'active')
                ?? $draft->picks->firstWhere('status', 'expired');
        }

        $serverNow = now();
        $expiresAt = null;
        $remaining = null;
        if ($activePick?->started_at && in_array($draft->status, ['live', 'expired'], true)) {
            $expiresAt = $activePick->started_at->copy()->addSeconds($activePick->pick_duration);
            $remaining = max(0, $serverNow->diffInSeconds($expiresAt, false));
        }

        $captainCanPick = false;
        if ($viewer && $activePick) {
            $captainCanPick = $activePick->team?->activeCaptain?->user_id === $viewer->id && $draft->status === 'live';
        }
        $captainTeam = null;
        if ($viewer && $viewer->can('make draft pick')) {
            $captainTeam = $draft->picks
                ->pluck('team')
                ->filter()
                ->unique('id')
                ->first(fn ($team) => $team->activeCaptain?->user_id === $viewer->id);
        }

        $selectedPlayerIds = $draft->picks
            ->where('status', 'selected')
            ->pluck('tournament_player_id')
            ->filter()
            ->values()
            ->all();

        $availablePlayersQuery = TournamentPlayer::query()
            ->with('playerProfile')
            ->where('tournament_id', $draft->tournament_id)
            ->approved();

        if ($selectedPlayerIds !== []) {
            $availablePlayersQuery->whereNotIn('id', $selectedPlayerIds);
        }

        $availablePlayers = $availablePlayersQuery->orderBy('id')->get();
        $isAdminViewer = $viewer?->can('control draft') ?? false;
        $isCaptainViewer = $viewer?->can('make draft pick') ?? false;
        $availablePlayerPayload = ($isAdminViewer || $isCaptainViewer)
            ? $availablePlayers->map(fn (TournamentPlayer $player) => [
                'id' => $player->id,
                'full_name' => $player->playerProfile?->full_name,
                'playing_role' => $player->playerProfile?->playing_role,
                'city' => $player->playerProfile?->city,
            ])->values()
            : collect();
        $remainingPlayerPayload = $isAdminViewer ? $availablePlayerPayload : collect();
        $statusCounts = $draft->picks->countBy('status');
        $rounds = $draft->rounds->sortBy('round_number')->values();
        $nextRound = $rounds->firstWhere('status', 'pending');
        $nextPendingPick = $draft->picks->firstWhere('status', 'pending');
        $canStartNextPick = ! $activePick && $nextPendingPick && in_array($draft->status, ['setup', 'paused'], true);
        $teamSquads = $draft->picks
            ->pluck('team')
            ->filter()
            ->unique('id')
            ->values()
            ->map(function ($team) use ($draft) {
                $selectedPlayers = $draft->picks
                    ->where('team_id', $team->id)
                    ->where('status', 'selected')
                    ->map(fn (DraftPick $pick) => [
                        'pick_number' => $pick->pick_number,
                        'full_name' => $pick->tournamentPlayer?->playerProfile?->full_name,
                        'playing_role' => $pick->tournamentPlayer?->playerProfile?->playing_role,
                        'city' => $pick->tournamentPlayer?->playerProfile?->city,
                        'selected_at' => $pick->selected_at?->toISOString(),
                    ])
                    ->values();

                return [
                    'id' => $team->id,
                    'name' => $team->name,
                    'short_name' => $team->short_name,
                    'selected_count' => $selectedPlayers->count(),
                    'selected_players' => $selectedPlayers,
                ];
            });

        return [
            'id' => $draft->id,
            'status' => $draft->status,
            'revision' => $draft->revision,
            'current_pick_number' => $activePick?->pick_number,
            'current_round' => $activePick?->round?->round_number,
            'current_team' => $activePick?->team?->only(['id', 'name', 'short_name']),
            'captain_team' => $captainTeam?->only(['id', 'name', 'short_name']),
            'can_start_next_pick' => $canStartNextPick,
            'next_pick' => $nextPendingPick ? [
                'pick_number' => $nextPendingPick->pick_number,
                'round_number' => $nextPendingPick->round?->round_number,
                'team' => $nextPendingPick->team?->only(['id', 'name', 'short_name']),
            ] : null,
            'can_start_next_round' => $canStartNextPick && $nextPendingPick?->draft_round_id === $nextRound?->id,
            'next_round' => $nextRound ? [
                'round_number' => $nextRound->round_number,
                'name' => $nextRound->name,
            ] : null,
            'rounds' => $rounds->map(fn (DraftRound $round) => [
                'round_number' => $round->round_number,
                'name' => $round->name,
                'status' => $round->status,
                'total' => $round->picks->count(),
                'selected' => $round->picks->where('status', 'selected')->count(),
                'skipped' => $round->picks->where('status', 'skipped')->count(),
                'pending' => $round->picks->where('status', 'pending')->count(),
            ])->values(),
            'team_squads' => $teamSquads,
            'pending_picks' => $draft->picks->where('status', 'pending')->map(fn (DraftPick $pick) => [
                'pick_number' => $pick->pick_number,
                'round' => $pick->round?->round_number,
                'team' => $pick->team?->only(['id', 'name', 'short_name']),
            ])->values(),
            'remaining_players' => $remainingPlayerPayload,
            'timer' => [
                'remaining_seconds' => $remaining,
                'started_at' => $activePick?->started_at?->toISOString(),
                'expires_at' => $expiresAt?->toISOString(),
                'server_now' => $serverNow->toISOString(),
                'duration' => $activePick?->pick_duration,
                'expired' => $draft->status === 'expired',
            ],
            'captain_can_pick' => $captainCanPick,
            'summary' => [
                'total' => $draft->picks->count(),
                'selected' => $statusCounts->get('selected', 0),
                'active' => $statusCounts->get('active', 0),
                'expired' => $statusCounts->get('expired', 0),
                'skipped' => $statusCounts->get('skipped', 0),
                'pending' => $statusCounts->get('pending', 0),
            ],
            'available_players' => $availablePlayerPayload,
            'picks' => $draft->picks->map(fn (DraftPick $pick) => [
                'pick_number' => $pick->pick_number,
                'round' => $pick->round?->round_number,
                'status' => $pick->status,
                'team' => $pick->team?->only(['id', 'name', 'short_name']),
                'player' => $pick->tournamentPlayer?->playerProfile?->only(['id', 'full_name', 'playing_role', 'city']),
            ])->values(),
        ];
    }

    private function setDraftStatus(Draft $draft, User $actor, string $status, string $action): Draft
    {
        return $this->database->transaction(function () use ($draft, $actor, $status, $action) {
            $draft = Draft::query()->lockForUpdate()->findOrFail($draft->id);

            if (! $actor->can('control draft')) {
                $this->fail('draft', 'You are not authorized to control this draft.');
            }

            if (! in_array($status, ['live', 'paused'], true)) {
                $this->fail('draft', 'Invalid draft status transition.');
            }

            if ($status === 'paused' && ! in_array($draft->status, ['live', 'expired'], true)) {
                $this->fail('draft', 'Only a live or expired draft can be paused.');
            }

            if ($status === 'live' && $draft->status !== 'paused') {
                $this->fail('draft', 'Only a paused draft can be resumed.');
            }

            if ($status === 'live' && ! $draft->picks()->whereIn('status', ['active', 'expired'])->exists()) {
                $this->fail('draft', 'There is no active pick to resume. Start the next pick instead.');
            }

            $draft->update([
                'status' => $status,
                'paused_at' => $status === 'paused' ? now() : null,
                'revision' => $draft->revision + 1,
            ]);

            $this->audit($actor, $draft, $action, null, $draft->fresh()->toArray());

            return $draft->fresh(['picks.team', 'picks.round']);
        });
    }

    private function advanceToNextPick(Draft $draft, DraftPick $completedPick, User $actor): void
    {
        $nextPick = $draft->picks()
            ->where('status', 'pending')
            ->where('pick_number', '>', $completedPick->pick_number)
            ->orderBy('pick_number')
            ->first();

        if (! $nextPick) {
            $completedAt = now();
            $completedPick->round()->update([
                'status' => 'completed',
                'completed_at' => $completedAt,
            ]);
            $draft->update([
                'status' => 'completed',
                'current_pick_number' => null,
                'pick_started_at' => null,
                'pick_duration' => null,
                'completed_at' => $completedAt,
                'revision' => $draft->revision + 1,
            ]);
            $draft->tournament()->update(['status' => 'completed']);

            $this->audit($actor, $draft, 'draft.completed', null, $draft->fresh()->toArray());

            return;
        }

        $pausedAt = now();
        if ($nextPick->draft_round_id !== $completedPick->draft_round_id) {
            $completedPick->round()->update([
                'status' => 'completed',
                'completed_at' => $pausedAt,
            ]);
        }

        $draft->update([
            'status' => 'paused',
            'current_pick_number' => null,
            'pick_started_at' => null,
            'pick_duration' => null,
            'paused_at' => $pausedAt,
            'revision' => $draft->revision + 1,
        ]);

        $this->audit($actor, $draft, 'draft.pick_completed', null, $draft->fresh()->toArray(), [
            'completed_pick_number' => $completedPick->pick_number,
            'next_pick_number' => $nextPick->pick_number,
            'next_round_number' => $nextPick->round?->round_number,
        ]);
    }

    private function markExpired(Draft $draft, DraftPick $pick, ?User $actor): void
    {
        $pick->update(['status' => 'expired', 'expired_at' => now()]);
        $draft->update([
            'status' => 'expired',
            'revision' => $draft->revision + 1,
        ]);
        $this->audit($actor, $draft, 'draft.timer_expired', null, $pick->fresh()->toArray());
    }

    private function audit(?User $actor, Draft $draft, string $action, ?array $before, ?array $after, array $metadata = []): void
    {
        $request = app()->bound('request') ? request() : null;

        AuditLog::create([
            'user_id' => $actor?->id,
            'tournament_id' => $draft->tournament_id,
            'draft_id' => $draft->id,
            'action' => $action,
            'before' => $before,
            'after' => $after,
            'metadata' => $metadata,
            'ip_address' => $request?->ip(),
            'user_agent' => $request?->userAgent(),
        ]);
    }

    private function fail(string $key, string $message): never
    {
        throw ValidationException::withMessages([$key => $message]);
    }
}
