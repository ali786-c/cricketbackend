<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\CricketMatch;
use App\Modules\Scoring\Services\MatchScoringService;
use App\Modules\Scoring\Services\MatchResultService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Gate;
use Illuminate\Support\Str;

class ScoringController extends Controller
{
    public function __construct(
        private readonly MatchScoringService $scoring,
        private readonly MatchResultService $results
    ) {}

    public function store(Request $request, CricketMatch $match): JsonResponse
    {
        Gate::authorize('score', $match);
        $validated = $request->validate([
            'striker_id' => ['required', 'integer'], 'non_striker_id' => ['required', 'integer', 'different:striker_id'], 'bowler_id' => ['required', 'integer'],
            'runs_off_bat' => ['nullable', 'integer', 'min:0', 'max:6'], 'wides' => ['nullable', 'integer', 'min:0', 'max:6'], 'no_balls' => ['nullable', 'integer', 'min:0', 'max:6'],
            'byes' => ['nullable', 'integer', 'min:0', 'max:6'], 'leg_byes' => ['nullable', 'integer', 'min:0', 'max:6'], 'penalty_runs' => ['nullable', 'integer', 'min:0', 'max:6'],
            'commentary' => ['nullable', 'string', 'max:1000'], 'expected_revision' => ['nullable', 'integer', 'min:0'], 'wicket' => ['nullable', 'array'],
            'wicket.dismissed_player_id' => ['nullable', 'integer'], 'wicket.dismissal_type' => ['nullable', 'string'], 'wicket.fielder_id' => ['nullable', 'integer'],
            'wicket.runs_completed' => ['nullable', 'integer', 'min:0', 'max:6'], 'wicket.notes' => ['nullable', 'string', 'max:500'],
            'wagon_x' => ['nullable', 'numeric', 'min:0', 'max:100'],
            'wagon_y' => ['nullable', 'numeric', 'min:0', 'max:100'],
        ]);
        $delivery = $this->scoring->recordDelivery($match, $validated, (int) $request->user()->id, isset($validated['expected_revision']) ? (int) $validated['expected_revision'] : null);
        return response()->json(['data' => ['delivery_id' => $delivery->id, 'revision' => $delivery->revision, 'notation' => $delivery->notation()]]);
    }

    public function sync(Request $request, CricketMatch $match): JsonResponse
    {
        Gate::authorize('score', $match);
        $validated = $request->validate([
            'device_id' => ['nullable', 'string', 'max:191'],
            'base_revision' => ['nullable', 'integer', 'min:0'],
            'deliveries' => ['required', 'array', 'min:1'],
            'deliveries.*.local_uuid' => ['required', 'string', 'uuid'],
            'deliveries.*.local_sequence' => ['nullable', 'integer', 'min:1'],
            'deliveries.*.device_timestamp' => ['required', 'string'],
            'deliveries.*.striker_id' => ['required', 'integer'],
            'deliveries.*.non_striker_id' => ['required', 'integer', 'different:deliveries.*.striker_id'],
            'deliveries.*.bowler_id' => ['required', 'integer'],
            'deliveries.*.runs_off_bat' => ['nullable', 'integer', 'min:0', 'max:6'],
            'deliveries.*.wides' => ['nullable', 'integer', 'min:0', 'max:6'],
            'deliveries.*.no_balls' => ['nullable', 'integer', 'min:0', 'max:6'],
            'deliveries.*.byes' => ['nullable', 'integer', 'min:0', 'max:6'],
            'deliveries.*.leg_byes' => ['nullable', 'integer', 'min:0', 'max:6'],
            'deliveries.*.penalty_runs' => ['nullable', 'integer', 'min:0', 'max:6'],
            'deliveries.*.commentary' => ['nullable', 'string', 'max:1000'],
            'deliveries.*.wicket' => ['nullable', 'array'],
            'deliveries.*.wicket.dismissed_player_id' => ['nullable', 'integer'],
            'deliveries.*.wicket.dismissal_type' => ['nullable', 'string'],
            'deliveries.*.wicket.fielder_id' => ['nullable', 'integer'],
            'deliveries.*.wicket.runs_completed' => ['nullable', 'integer', 'min:0', 'max:6'],
            'deliveries.*.wicket.notes' => ['nullable', 'string', 'max:500'],
            'deliveries.*.wagon_x' => ['nullable', 'numeric', 'min:0', 'max:100'],
            'deliveries.*.wagon_y' => ['nullable', 'numeric', 'min:0', 'max:100'],
        ]);

        $actorId = (int) $request->user()->id;
        $correlationId = $request->header('X-Correlation-ID', (string) Str::uuid());

        $responseList = \DB::transaction(function () use ($match, $validated, $actorId, $correlationId) {
            $match = CricketMatch::query()->lockForUpdate()->findOrFail($match->id);
            Gate::authorize('score', $match);
            $localUuids = collect($validated['deliveries'])->pluck('local_uuid')->all();

            // A local UUID is a global idempotency key. It may only ever be
            // acknowledged for the same match and actor that first claimed it.
            $claimedDeliveries = \App\Models\MatchDelivery::query()
                ->whereIn('local_uuid', $localUuids)
                ->get()
                ->keyBy('local_uuid');
            $hasForeignClaim = $claimedDeliveries->contains(fn ($delivery) =>
                (int) $delivery->match_id !== (int) $match->id
                || (int) $delivery->recorded_by !== $actorId
            );
            if ($hasForeignClaim) {
                abort(409, 'idempotency_key_conflict');
            }
            $existingDeliveries = $claimedDeliveries;

            $newDeliveries = collect($validated['deliveries'])
                ->filter(fn ($d) => !$existingDeliveries->has($d['local_uuid']))
                ->sortBy(fn ($delivery) => sprintf(
                    '%020d:%s',
                    $delivery['local_sequence'] ?? PHP_INT_MAX,
                    $delivery['device_timestamp'],
                ))
                ->values();

            if ($newDeliveries->isNotEmpty()
                && isset($validated['base_revision'])
                && (int) $validated['base_revision'] !== (int) $match->revision) {
                Log::warning('match_sync_revision_conflict', [
                    'correlation_id' => $correlationId,
                    'server_match_id' => $match->id,
                    'base_revision' => (int) $validated['base_revision'],
                    'server_revision' => (int) $match->revision,
                    'delivery_count' => $newDeliveries->count(),
                ]);
                abort(409, 'revision_conflict');
            }

            $results = [];

            // Add already existing ones to result first so client gets acknowledgment
            foreach ($existingDeliveries as $uuid => $delivery) {
                $results[] = [
                    'local_uuid' => $uuid,
                    'delivery_id' => $delivery->id,
                    'revision' => $delivery->revision,
                    'notation' => $delivery->notation(),
                    'status' => 'already_sync',
                ];
            }

            // Process new ones sequentially
            foreach ($newDeliveries as $d) {
                $match = $match->fresh();
                $delivery = $this->scoring->recordDelivery($match, $d, $actorId, (int) $match->revision);

                $results[] = [
                    'local_uuid' => $d['local_uuid'],
                    'delivery_id' => $delivery->id,
                    'revision' => $delivery->revision,
                    'notation' => $delivery->notation(),
                    'status' => 'synced',
                ];
            }

            return $results;
        });

        // Fetch fresh match metrics
        $match = $match->fresh();
        Log::info('match_sync_completed', [
            'correlation_id' => $correlationId,
            'server_match_id' => $match->id,
            'acknowledged_count' => count($responseList),
            'revision' => $match->revision,
        ]);
        $innings = $match->innings()->whereKey($match->current_innings_id)->first();

        return response()->json([
            'data' => [
                'deliveries' => $responseList,
                'match' => [
                    'id' => $match->id,
                    'status' => $match->status,
                    'revision' => $match->revision,
                    'total_runs' => $innings?->total_runs ?? 0,
                    'wickets' => $innings?->wickets ?? 0,
                    'legal_balls' => $innings?->legal_balls ?? 0,
                ]
            ]
        ]);
    }

    public function nextInnings(Request $request, CricketMatch $match): JsonResponse
    {
        Gate::authorize('score', $match);
        $innings = $this->scoring->startNextInnings($match, (int) $request->user()->id);
        return response()->json(['data' => ['innings_id' => $innings->id, 'match_id' => $match->id]]);
    }

    public function submitResult(Request $request, CricketMatch $match): JsonResponse
    {
        Gate::authorize('submitResult', $match);
        $result = $this->results->submit($match, (int) $request->user()->id);
        return response()->json([
            'data' => $result,
            'message' => 'Match result submitted for approval.'
        ]);
    }

    public function undo(Request $request, CricketMatch $match): JsonResponse
    {
        Gate::authorize('score', $match);
        $validated = $request->validate([
            'reason' => ['required', 'string', 'min:5', 'max:500'],
            'client_uuid' => ['nullable', 'uuid'],
        ]);
        $clientUuid = $validated['client_uuid'] ?? null;
        $existing = $clientUuid ? \App\Models\MatchDelivery::query()
            ->where('match_id', $match->id)->where('undo_uuid', $clientUuid)->first() : null;
        if (! $existing) {
            $this->scoring->undoLastDelivery($match, (int) $request->user()->id, $validated['reason'], $clientUuid);
        }
        $match = $match->fresh();
        $innings = $match->innings()->whereKey($match->current_innings_id)->first();
        return response()->json(['data' => [
            'id' => $match->id, 'status' => $match->status, 'revision' => $match->revision,
            'total_runs' => $innings?->total_runs ?? 0, 'wickets' => $innings?->wickets ?? 0,
            'legal_balls' => $innings?->legal_balls ?? 0,
        ]]);
    }

    public function mvp(CricketMatch $match, \App\Modules\Analytics\Services\MVPPointsService $mvpService): JsonResponse
    {
        return response()->json([
            'data' => $mvpService->getMatchMVP($match)
        ]);
    }

    public function editDelivery(Request $request, \App\Models\MatchDelivery $matchDelivery, \App\Modules\Scoring\Services\MatchRecalculationService $recalcService): JsonResponse
    {
        Gate::authorize('update', $matchDelivery);
        $validated = $request->validate([
            'striker_id' => ['sometimes', 'integer', 'exists:match_players,id'],
            'non_striker_id' => ['sometimes', 'integer', 'exists:match_players,id', 'different:striker_id'],
            'bowler_id' => ['sometimes', 'integer', 'exists:match_players,id'],
            'runs_off_bat' => ['sometimes', 'integer', 'min:0', 'max:6'],
            'wides' => ['sometimes', 'integer', 'min:0', 'max:6'],
            'no_balls' => ['sometimes', 'integer', 'min:0', 'max:6'],
            'byes' => ['sometimes', 'integer', 'min:0', 'max:6'],
            'leg_byes' => ['sometimes', 'integer', 'min:0', 'max:6'],
            'penalty_runs' => ['sometimes', 'integer', 'min:0', 'max:6'],
            'commentary' => ['nullable', 'string', 'max:1000'],
            'wagon_x' => ['nullable', 'numeric', 'min:0', 'max:100'],
            'wagon_y' => ['nullable', 'numeric', 'min:0', 'max:100'],
        ]);

        \DB::transaction(function () use ($matchDelivery, $validated, $recalcService) {
            $matchDelivery->update($validated);

            $matchDelivery->update([
                'total_runs' => $matchDelivery->runs_off_bat + $matchDelivery->wides + $matchDelivery->no_balls + $matchDelivery->byes + $matchDelivery->leg_byes + $matchDelivery->penalty_runs,
                'is_legal_delivery' => ($matchDelivery->wides === 0 && $matchDelivery->no_balls === 0),
            ]);

            $recalcService->recalculateMatch($matchDelivery->match);
        });

        return response()->json([
            'message' => 'Delivery corrected and match scorecards recalculated successfully.',
            'data' => $matchDelivery->fresh(['striker', 'nonStriker', 'bowler', 'wicket']),
        ]);
    }
}
