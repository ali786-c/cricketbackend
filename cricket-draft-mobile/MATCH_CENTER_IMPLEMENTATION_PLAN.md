# Match Center Stabilization and Offline-First Implementation Plan

**Source audit:** `MATCH_CENTER_FLOW_AUDIT.md`
**Scope:** Android application, Laravel API/backend, Room database, match lifecycle, scoring, synchronization, and test coverage
**Execution style:** Incremental phases with explicit entry/exit gates
**Status:** Phase 1 shared contract and backend Phase 2 are complete; the Room identity,
custom-match lifecycle, local-first scoring, Match Center mapping, Matches tab,
player public-ID, and completion-navigation repairs are implemented and awaiting
the final Android build/device verification gate.

## Current implementation checkpoint (2026-09-10)

- [x] Persist local fixture before create-match navigation or server sync.
- [x] Store separate backend operational `serverMatchId` and `serverRevision`.
- [x] Add idempotent backend custom-match start covering XI, toss, and innings.
- [x] Persist backend MatchPlayer IDs for delivery synchronization.
- [x] Persist deliveries in Room before attempting the API.
- [x] Batch rapid delivery uploads and prevent overlapping slow-network calls.
- [x] Keep failed deliveries retryable instead of deleting them.
- [x] Make Match Center render Room immediately, then reconcile the API.
- [x] Restore real fixture lists in the Matches tab, including incomplete matches.
- [x] Remove hardcoded scorer rosters and fresh-install dummy player seeding.
- [x] Preserve Room data with an explicit 16→17 migration.
- [x] Map real innings-one/current-innings score, players, rules, and status.
- [x] Search/display the stable six-digit public player ID.
- [x] Save completion locally and navigate Home before background reconciliation.
- [x] Include custom-match direct player relationships in profile match history.
- [ ] Run Android JVM/instrumented tests (local Android Studio JBR is incomplete).
- [ ] Execute the five physical UI/device scenarios and record evidence.

## 1. Goal

Build one consistent match system in which:

- custom matches use the configuration selected during custom-match creation;
- tournament matches inherit an immutable snapshot of tournament rules;
- local, fixture, tournament, team, player, innings, and operational match IDs are never confused;
- every scoring action is saved locally before UI confirmation;
- scoring remains responsive with slow or unavailable internet;
- data synchronizes automatically in the background when connectivity permits;
- backend remains authoritative for accepted global ordering, revisions, innings, results, and standings;
- failures are visible and recoverable rather than silently discarded.

## 2. Non-negotiable architecture decisions

These decisions apply to every phase:

1. **Room is the mobile source of truth.** Compose observes `Flow`; network responses update Room.
2. **No network call on the scoring interaction path.** A tap ends after a successful local transaction.
3. **Separate identities.** Local UUIDs and backend numeric IDs have distinct fields and types.
4. **Immutable match rules.** Rules are frozen when the operational match is created.
5. **Immutable scoring events.** Corrections are explicit edit/undo events, not silent history mutation.
6. **Idempotent commands.** Creates and scoring events carry client UUID/idempotency keys.
7. **One normalized vocabulary.** API enums use stable machine values such as `tennis`, `scheduled`, and `squad_selection`.
8. **Explicit failure states.** No silent catch, fabricated IDs, or scoring-rule fallbacks.
9. **Backward-compatible rollout.** Room and backend migrations must preserve existing user data.
10. **Tests gate every phase.** A phase is not complete only because code compiles.

## 3. Target end-to-end flow

```text
Configuration
  -> save local match draft + immutable rule snapshot
  -> enqueue idempotent fixture/match creation
  -> receive and persist serverFixtureId/serverMatchId
  -> select XI using stable player identities
  -> approve XI
  -> record toss
  -> backend and Room reach LIVE
  -> local atomic scoring events + asynchronous sync
  -> reconcile authoritative server revisions
  -> next innings
  -> complete match
  -> submit/approve result
  -> rebuild tournament standings
```

## 4. Delivery phases overview

| Phase | Name | Primary result | Depends on |
|---:|---|---|---|
| 0 | Safety baseline | Reproducible tests and captured contracts | None |
| 1 | Shared domain contract | One configuration, identity, and lifecycle vocabulary | 0 |
| 2 | Backend rules and APIs | Correct immutable configuration and idempotent creation | 1 |
| 3 | Room schema and migration | Durable normalized local source of truth | 1–2 contracts |
| 4 | Identity and creation pipeline | Correct server fixture/match IDs persisted | 2–3 |
| 5 | Custom configuration flow | Custom settings drive the whole match | 2–4 |
| 6 | Tournament inheritance | Matches consistently inherit tournament rules | 2–4 |
| 7 | Lineup, approval, and toss | Real backend state transitions | 4–6 |
| 8 | Offline scoring engine | Atomic local scoring without network dependency | 3, 5–7 |
| 9 | Background synchronization | Near-live, retryable, acknowledged uploads | 8 |
| 10 | Match Center read/reconcile | Correct live state, polling, scorecards, status | 7–9 |
| 11 | Innings, results, standings | Complete match lifecycle | 8–10 |
| 12 | Migration cleanup and release | Legacy removal, observability, release gates | All |

## Phase 0 — Safety baseline and characterization

### Objective

Create a reliable safety net before changing schemas or lifecycle behavior.

### Tasks

#### Repository/build

- [ ] Repair the Android Gradle/JBR environment so JVM tests can run from the repository.
- [ ] Document exact PHP, Laravel, database, JDK, Gradle, and Android SDK versions.
- [ ] Record clean baseline commands for backend tests, Android unit tests, lint, and migrations.
- [ ] Do not mix unrelated existing worktree changes into implementation commits.

#### Characterization tests

- [ ] Add tests that reproduce local/server fixture ID confusion.
- [ ] Add a test proving the operational `match_id` is currently discarded.
- [ ] Add custom-match state test with `tournament_id = null`.
- [ ] Add mapper test for innings one and away-team batting first.
- [ ] Add polling test for an unchanged server response.
- [ ] Add scorer test proving blank match context/zero player IDs prevent valid persistence.
- [ ] Add backend tests for fallback rule-profile field names.
- [ ] Capture current API request/response fixtures as contract-test samples.

### Deliverables

- Baseline test suite and commands.
- Failing regression tests for every P0 defect.
- API contract samples stored as test fixtures.

### Exit gate

- Test commands run locally/CI.
- Every known P0 issue has a reproducible automated test or a documented reason why an integration test is required later.
- No production behavior has been intentionally changed.

## Phase 1 — Shared domain contract and naming

**Status:** Complete (implemented and contract-tested on 2026-09-10)

### Objective

Define the vocabulary before changing persistence or endpoints.

### Tasks

#### Match configuration

- [x] Define `MatchConfiguration` on Android and an equivalent backend DTO/value object.
- [x] Include format, innings per side, overs, playing-XI size, maximum wickets, legal balls per over, bowler limits, ball type, extras, last-man-standing, and optional caps.
- [x] Define validation invariants and supported ranges once.
- [x] Keep `squadSize`, `playingXiSize`, and `maximumWickets` independent.
- [x] Define configuration `origin`, `version`, and `lockedAt`.

#### Identities

- [x] Define separate `LocalMatchId`, `ServerFixtureId`, `ServerMatchId`, `TournamentId`, `TeamId`, `PlayerId`, `MatchPlayerId`, and `InningsId` concepts.
- [x] Decide wire/database representation for client-generated UUIDs.
- [x] Prohibit `hashCode()` and formatted-string identity conversion in the new contract.

#### States and enums

- [x] Define the complete lifecycle: local draft/pending sync plus backend squad selection, lineup approval, toss, live, innings break, completion, result approval, abandonment, and cancellation.
- [x] Normalize ball type, format, toss decision, dismissal type, extras, and sync-state wire values.
- [x] Define which transitions may be queued offline and which require confirmed backend state.

#### API error contract

- [x] Define stable error codes, field errors, retryability, current server revision, and correlation ID.
- [x] Separate unauthenticated, unauthorized, validation, conflict, not found, throttled, transient server, and network failures.

### Deliverables

- Contract section checked into documentation.
- Kotlin domain types with no Android dependency.
- PHP DTO/value object and validation rules.
- JSON examples for custom and tournament matches.

### Exit gate

- Android and backend fixtures serialize the same normalized values.
- No ambiguous generic `id` is used in the new contract.
- Product decisions for last-man-standing, no-draft tournaments, and offline lifecycle commands are recorded.

### Phase 1 verification

- Canonical Kotlin domain configuration, typed identities, lifecycle/enums, and API error contract added.
- Equivalent validated PHP configuration, lifecycle enum, and error envelope added.
- Laravel match creation builds immutable snapshots through the shared configuration contract.
- Retrofit request/state DTOs carry the complete configuration vocabulary.
- Identity rules, offline policy, aliases, and JSON examples are in `MATCH_CENTER_SHARED_CONTRACT.md`.
- PHP regression run: **18 tests passed, 100 assertions**.
- Android contract tests are added; execution remains blocked by the Phase 0 workstation JDK issue.

## Phase 2 — Backend rule correctness and API foundation

**Status:** Complete (implemented and regression-verified on 2026-09-10)

### Objective

Make the backend capable of representing and returning the intended system before mobile relies on it.

### Tasks

#### Rule correctness

- [x] Replace `wickets_per_innings` with `maximum_wickets`.
- [x] Replace `max_over_per_bowler` with `max_overs_per_bowler`.
- [x] Audit migrations, seeders, model fillable fields, requests, services, and tests for the same mismatch.
- [x] Stop silently generating incomplete fallback profiles.
- [x] Validate all supplied custom rule fields at fixture creation (legacy requests remain temporarily supported).

#### Immutable snapshots

- [x] Add a match-rule JSON snapshot column.
- [x] Copy custom configuration into the match snapshot.
- [x] Copy tournament profile/version into each operational match.
- [x] Ensure later tournament edits do not affect existing match snapshots.

#### API changes

- [x] Extend custom fixture/match creation to accept complete configuration.
- [x] Add a client UUID to custom fixture creation and carry it into the operational match.
- [x] Return server fixture ID, server match ID, status, revision, rules, teams, and players from fixture-to-match creation.
- [x] Add authenticated state access for custom/private matches.
- [x] Return explicit fixture home/away identity independently of batting order.
- [x] Return active innings, target, striker, non-striker, bowler, complete scorecard, and rule snapshot.
- [x] Make custom fixture and fixture-to-match creation idempotent on repeat requests.

#### Tournament eligibility

- [x] Branch match-player eligibility by `has_draft`.
- [x] Draft tournament: use approved drafted team players.
- [x] No-draft tournament: create match-player squad entries from explicitly selected approved tournament player profiles.
- [x] Reject drafted match creation when either team lacks sufficient approved players; enforce the equivalent no-draft invariant at lineup submission/approval, where team assignment becomes known.

### Tests

- [x] Backend feature tests for rule validation and snapshot immutability.
- [x] Feature tests for custom creation and no-draft tournament match creation.
- [x] Idempotency tests for repeated custom fixture and operational-match requests.
- [x] Authorization tests for custom owner isolation, authenticated custom state, and public spectator state.
- [x] Complete no-draft and draft eligibility matrix, including repaired pivot/profile/ownership test fixtures.

### Exit gate

- Backend can create correct custom and tournament operational matches without hardcoded scoring rules.
- Repeating a create request cannot create duplicates.
- State API provides everything required to reconstruct Match Center.
- Old clients remain supported temporarily or a coordinated version gate is in place.

### Phase 2 progress log — 2026-09-10

Completed implementation:

- canonical backend rule-field names;
- fixture configuration and operational-match rule snapshots;
- client UUID persistence and idempotent custom/tournament fixture creation support;
- idempotent fixture-to-operational-match retries;
- complete custom configuration validation;
- custom/private authenticated state authorization and explicit fixture home/away payload;
- richer creation/state responses with revision, rules, teams, and players;
- custom fixture/match owner authorization;
- draft/no-draft branch and approved-profile lineup materialization for no-draft tournaments;
- qualified tournament-team queries to avoid ambiguous pivot SQL;
- persistent active striker, non-striker, and bowler state, including strike rotation and undo restoration;
- active-player payloads in match state;
- minimum drafted-squad eligibility at creation and exact no-draft eligibility at lineup approval;
- removal of tournament rule-profile auto-generation: an active tournament profile is mandatory;
- repair of the unreachable post-commit `DeliveryRecorded` broadcast path.

Compatibility decision:

- API v1 custom requests without configuration remain deliberately supported through the complete seeded `default-custom` profile until the Phase 5 mobile rollout. New clients must send the full configuration snapshot. This is a versioned compatibility policy, not an implicitly generated fallback.
- Tournament matches have no compatibility fallback: their tournament must reference an active rule profile before match creation.

Verification evidence:

- `C:\php83\php.exe -l <all modified PHP files>` — passed.
- `C:\php83\php.exe artisan test tests/Feature/PublicMatchTest.php tests/Feature/Api/V1/CustomMatchSyncTest.php tests/Feature/Api/V1/MatchConfigurationFoundationTest.php` — **17 tests passed, 101 assertions**.
- `C:\php83\php.exe artisan test tests/Feature/Admin/MatchTest.php tests/Feature/Admin/MatchScoringTest.php tests/Feature/Api/V1/AdminApiTest.php` — **17 tests passed, 105 assertions**.
- Combined Phase 2 verification: **34 tests passed, 206 assertions**.
- PHP syntax validation passed for every Phase 2 PHP file and migration; `git diff --check` reports no whitespace errors.

## Phase 3 — Room schema and safe migration

### Objective

Create a normalized, durable mobile source of truth without losing existing matches.

### New/updated entities

- [ ] `MatchEntity`: local UUID, nullable server fixture/match IDs, tournament ID, source, local/server lifecycle, revision, timestamps.
- [ ] `MatchRuleSnapshotEntity`: complete immutable rules and origin/version.
- [ ] `MatchTeamEntity`: team identities and home/away role.
- [ ] `MatchPlayerEntity`: local/server player and match-player IDs, team, role, selection type.
- [ ] `MatchInningsEntity`: identities, batting/bowling teams, target, totals, state.
- [ ] `MatchEventEntity`: immutable local scoring/lifecycle event.
- [ ] `SyncOutboxEntity`: event UUID, aggregate, sequence, payload, dependency, status, attempts, retry time, error.
- [ ] `SyncCheckpointEntity`: server revision and last acknowledged local sequence.

### Constraints/indexes

- [ ] Unique non-null server fixture ID.
- [ ] Unique non-null server match ID.
- [ ] Unique event UUID.
- [ ] Unique `(localMatchId, deviceId, localSequence)`.
- [ ] Index pending outbox by match/status/next-attempt.
- [ ] Foreign keys with deliberate delete behavior.

### Migration

- [ ] Add an explicit Room migration; do not use destructive migration.
- [ ] Preserve legacy fixture/local score records.
- [ ] Convert known server IDs only when provenance is certain.
- [ ] Leave ambiguous/hash IDs unresolved rather than treating them as real server IDs.
- [ ] Mark incomplete legacy configuration as `requires_review`.
- [ ] Retain legacy columns during a compatibility window.

### Tests

- [ ] Migration tests from every currently shipped Room version.
- [ ] DAO transaction, uniqueness, ordering, Flow, and cascade tests.
- [ ] Process-death reconstruction test from Room only.

### Exit gate

- Existing database snapshots migrate without data loss.
- A match can be fully reconstructed locally with no ViewModel memory or network.
- Invalid identity/configuration states are explicit.

## Phase 4 — Identity-safe creation pipeline

### Objective

Persist and use the correct identities through every screen and API call.

### Tasks

- [ ] Create match draft locally first with a UUID.
- [ ] Store configuration and teams in the same Room transaction.
- [ ] Enqueue fixture creation with the client UUID.
- [ ] Persist returned `serverFixtureId` without replacing the local primary key.
- [ ] Enqueue operational-match creation dependent on `serverFixtureId`.
- [ ] Persist returned `serverMatchId`, revision, and status.
- [ ] Make repository operations resolve by local ID internally and require server match ID for server scoring.
- [ ] Replace navigation string IDs with a local match key or typed route argument.
- [ ] Delete all `hashCode()` ID fallbacks and tournament-ID parsing heuristics.
- [ ] Prevent scoring setup until required server identities exist when online lifecycle confirmation is mandatory.
- [ ] Show synchronization state while still allowing safe offline preparation.

### Tests

- [ ] Custom online, custom offline-then-online, tournament online, and tournament offline-then-online creation.
- [ ] Duplicate request/retry.
- [ ] App termination between fixture and operational-match creation.
- [ ] Two local matches with identical team names.

### Exit gate

- Every API request uses a verified server ID.
- Navigation never transports a fixture ID as a match ID.
- Creation resumes safely after interruption and cannot duplicate server records.

## Phase 5 — Custom match configuration flow

### Objective

Make user-selected custom settings authoritative throughout the match.

### Tasks

- [ ] Replace hardcoded date/time with current localized values and explicit timezone.
- [ ] Replace display strings with normalized enum-backed selectors.
- [ ] Load supported options/ranges from domain configuration, not scattered Composable lists.
- [ ] Add independent playing-XI size, maximum wickets, and optional squad size.
- [ ] Add legal balls per over, innings count, bowler limit, extras, and optional special rules where product supports them.
- [ ] Validate locally using the same invariants as backend.
- [ ] Save exact configuration snapshot atomically with the local match.
- [ ] Send the snapshot through idempotent creation.
- [ ] Compare server-returned snapshot and fail visibly on mismatch.
- [ ] Lock configuration after the agreed lifecycle boundary.

### Tests

- [ ] 5/6/8-ball overs where supported.
- [ ] Small-team matches where XI and wickets differ.
- [ ] Tennis/leather/hard/tape/indoor normalization.
- [ ] Boundary and invalid configurations.
- [ ] Offline creation followed by delayed server creation.

### Exit gate

- The same snapshot is visible in form confirmation, Room, request, backend match, state response, and scorer.
- No match-affecting value is silently hardcoded downstream.

## Phase 6 — Tournament configuration inheritance

### Objective

Ensure every tournament match belongs to its tournament and consistently uses tournament settings.

### Tasks

- [ ] Add full rule-profile selection/editing to tournament setup.
- [ ] Send and persist `cricket_rule_profile_id` and complete returned profile.
- [ ] Store tournament IDs explicitly in local fixtures/matches.
- [ ] At match creation, snapshot the active tournament profile/version.
- [ ] Remove custom-rule controls from tournament scheduling unless overrides are an explicit product feature.
- [ ] If overrides are allowed, model and audit them explicitly rather than mutating the tournament profile.
- [ ] Enforce both teams belong to the tournament.
- [ ] Implement separate drafted and no-draft roster paths.
- [ ] Lock tournament rule changes at the proper lifecycle point and preserve old match snapshots.

### Tests

- [ ] Tournament profile inheritance.
- [ ] Tournament edited after one match is created: old match unchanged, new match receives new version.
- [ ] Cross-tournament team rejection.
- [ ] Draft and no-draft squads.
- [ ] Offline cached tournament creation with eventual reconciliation.

### Exit gate

- Every tournament match has an explicit tournament ID and immutable tournament-derived rule snapshot.
- No generic default profile is silently substituted.

## Phase 7 — Playing XI, approval, and toss lifecycle

### Objective

Replace local simulation with real, recoverable backend transitions.

### Tasks

- [ ] Load eligible players with local player ID, server player ID, and backend match-player ID.
- [ ] Remove name-based identity and implement the currently empty server-ID mapping.
- [ ] Select exactly `playingXiSize` for each team.
- [ ] Persist selections and lifecycle commands locally.
- [ ] Submit both playing XIs to the correct custom/tournament routes.
- [ ] Approve lineup only after both server submissions succeed.
- [ ] Persist toss winner by team ID and decision enum.
- [ ] Record toss and confirm backend created innings one/Live state.
- [ ] Resume setup correctly after offline work, app restart, partial sync, or validation failure.
- [ ] Do not navigate into scorer mode until required local data exists; clearly indicate when server activation is pending.

### Tests

- [ ] Too few/many players, duplicates, wrong-team player, unresolved player IDs.
- [ ] Partial completion and retry at every transition.
- [ ] Away team wins toss and bats first.
- [ ] Offline lineup/toss queue and later ordered sync.
- [ ] Reopening setup after process death.

### Exit gate

- Mobile and backend agree on XI, toss, batting/bowling teams, innings, status, and player IDs.
- No success callback fires before required local transaction/state transition succeeds.

## Phase 8 — Transactional offline scoring engine

### Objective

Make every ball durable, configuration-aware, and independent of network speed.

### Tasks

- [ ] Move scoring rules from `LiveScorerViewModel` into a pure Kotlin scoring engine/domain use case.
- [ ] Feed it only the immutable match snapshot and current local projection.
- [ ] Represent runs, extras, wicket, retirement, correction, undo, striker selection, bowler selection, and innings commands explicitly.
- [ ] In one Room transaction, append event, update projection, and enqueue outbox.
- [ ] Make Room Flow drive scorer UI state.
- [ ] Reset state automatically when local match ID changes.
- [ ] Remove global/shared mutable scorer leakage.
- [ ] Validate maximum wickets, overs, legal balls, bowler limits, and chase completion using snapshot rules.
- [ ] Retain event history for audit and deterministic rebuild.
- [ ] Replace silent persistence catches with actionable local errors.

### Tests

- [ ] Pure scoring-engine table tests for runs/extras/wickets/strike/over boundaries.
- [ ] Custom balls-per-over and maximum-wicket tests.
- [ ] Atomicity/failure injection tests.
- [ ] Undo/edit event tests.
- [ ] Projection rebuild from event log.
- [ ] Process kill immediately after a tap.

### Exit gate

- Airplane-mode scoring works through a full innings and survives process death.
- No HTTP request occurs in a score-button handler.
- Replaying stored events produces the same projection.

## Phase 9 — Near-live background synchronization

### Objective

Synchronize continuously when possible without lagging or risking local scoring.

### Tasks

#### Foreground sync

- [ ] One sync actor/mutex per operational match.
- [ ] Trigger after Room outbox commits; debounce short bursts.
- [ ] Batch by strict local sequence with a safe server-enforced limit.
- [ ] Send event UUIDs, device ID, local sequence, and base revision.
- [ ] Verify acknowledgment for each UUID before marking it synced.
- [ ] Persist returned revision and server projection transactionally.

#### WorkManager recovery

- [ ] Enqueue unique one-time work per match on each pending event.
- [ ] Require connected network; validate actual internet before sending.
- [ ] Use bounded exponential backoff.
- [ ] Retain periodic work as recovery sweep only.
- [ ] Do not place live deliveries behind unrelated entity queues.

#### Failure handling

- [ ] Retry timeout, connection, 429, and 5xx failures.
- [ ] Dead-letter 401/403/404/409/422 according to explicit policy.
- [ ] Never delete failed events automatically.
- [ ] Add retry-now and diagnostic details for operators.
- [ ] Show pending count and last successful sync without intrusive UI.

### Tests

- [ ] Offline, slow, intermittent, captive portal, timeout, 429, 500, and reconnect.
- [ ] Duplicate responses and missing acknowledgments.
- [ ] App/process restart and WorkManager retry.
- [ ] Hundreds of queued balls with bounded batches.
- [ ] No UI-frame/input delay under slow network.

### Exit gate

- Local scoring latency is independent of HTTP latency.
- Every queued event reaches acknowledged or visible needs-attention state.
- Duplicate retries do not duplicate backend deliveries.

## Phase 10 — Match Center authoritative read and reconciliation

### Objective

Render correct local/offline data immediately and safely reconcile server truth.

### Tasks

- [ ] Replace `ScheduledFixture` network mapping with dedicated `MatchCenterState`.
- [ ] Preserve fixture home/away separately from batting/bowling order.
- [ ] Correct innings-one/current-innings mapping.
- [ ] Map complete configuration, IDs, revisions, scorecards, extras, FOW, partnerships, target, and result.
- [ ] Write remote snapshots to Room; never overlay stale fields blindly.
- [ ] Expose cached freshness, connectivity, pending count, and sync issue in UI state.
- [ ] Replace nullable polling results with Changed/Unchanged/NotFound/Unauthorized/NetworkError outcomes.
- [ ] Continue polling after Unchanged.
- [ ] Poll only while lifecycle/UI requires it; cancel correctly on disposal.
- [ ] Prefer push/realtime transport later only if backend adds it; polling remains valid initially.

### Tests

- [ ] Away team bats first without swapping home/away.
- [ ] Correct innings-one and innings-two score.
- [ ] Unchanged polling continues.
- [ ] Offline cached view and stale indicator.
- [ ] Private/custom authorization.
- [ ] Local pending events plus newer server snapshot reconciliation.

### Exit gate

- Viewer and scorer screens render the same cricket state from Room.
- Network errors cannot masquerade as successful fresh data.
- Polling does not stop merely because no ball was recorded.

## Phase 11 — Innings, results, and tournament standings

### Objective

Complete the lifecycle instead of changing only local status fields.

### Tasks

- [ ] Express declare/end innings as a local command/outbox event.
- [ ] Sync it only after preceding deliveries are acknowledged.
- [ ] Call backend next-innings and reconcile target/batting order/current innings.
- [ ] Detect automatic innings completion consistently on local/backend engines.
- [ ] Queue/submit match result after final delivery/innings confirmation.
- [ ] Implement authorized approval and rejection flow.
- [ ] Reconcile final summary, winner, result type, MVP, and status.
- [ ] Refresh/rebuild tournament standings only after approved result.
- [ ] Support tie, no-result, abandonment, cancellation, and configured tie method.

### Tests

- [ ] Overs complete, all out, manual declaration, and successful chase.
- [ ] Command ordering with pending final-over deliveries.
- [ ] Tie/no-result/win/abandoned cases.
- [ ] Submit/approve/reject authorization and idempotency.
- [ ] Standings and net-run-rate with configured balls per over.

### Exit gate

- A custom and tournament match can complete end to end online and offline-then-online.
- Tournament standings change only from approved authoritative results.

## Phase 12 — Legacy cleanup, observability, and release

### Objective

Remove dangerous compatibility paths only after the replacement is proven.

### Tasks

- [ ] Remove hash-ID fallback, heuristic tournament parsing, name-based identity, and hardcoded scoring fallbacks.
- [ ] Remove legacy local-only status/toss/innings mutations.
- [ ] Remove obsolete JSON score/squad columns after migration window.
- [ ] Add structured logs with correlation ID, local match UUID, server match ID, event UUID, attempt, and result—without personal/sensitive data.
- [ ] Add metrics for pending age/count, sync latency, failure codes, revision conflicts, and dead letters.
- [ ] Add a support/export diagnostic screen for match sync health.
- [ ] Add backend alerts for rising sync failures/conflicts.
- [ ] Run database rollback rehearsal and Android migration rehearsal.
- [ ] Perform staged rollout with feature flag/version compatibility checks.
- [ ] Update API and mobile documentation after final contracts stabilize.

### Final release matrix

- [ ] Custom match: online end-to-end.
- [ ] Custom match: fully offline, process restart, later sync.
- [ ] Tournament with draft: online and offline-then-online.
- [ ] Tournament without draft: online and offline-then-online.
- [ ] Very slow/intermittent network without scoring lag.
- [ ] Duplicate/reordered requests without duplicate balls.
- [ ] Two-device conflict produces safe operator resolution.
- [ ] Private/custom authorization.
- [ ] Non-six-ball configuration where supported.
- [ ] Match completion, approval, standings, and historical reopening.

### Exit gate

- All P0/P1 audit items are closed or explicitly accepted by product/security owners.
- No known path silently loses a confirmed local scoring action.
- Production monitoring and recovery procedures are documented.

## 5. Cross-phase Definition of Done

Every phase must meet all applicable items:

- [ ] Backend and Android contracts agree and have automated serialization tests.
- [ ] Database changes include forward migration and tested rollback/recovery strategy.
- [ ] New repository APIs expose Room-backed `Flow` for state.
- [ ] UI has Loading/Ready/Offline/Syncing/Error or equivalent explicit states.
- [ ] No swallowed exception can hide data loss.
- [ ] Authentication/authorization is tested, not inferred from UI role.
- [ ] Unit, integration, and relevant UI tests pass.
- [ ] Static analysis/lint and backend formatting pass.
- [ ] Documentation and audit status are updated.
- [ ] Manual verification notes and evidence are attached to the phase.
- [ ] Legacy code is removed only when its replacement is verified.

## 6. Commit and review strategy

Use small reviewable changes rather than one large rewrite:

1. Contract/types and tests.
2. Backend migration and behavior.
3. Android Room migration and repository.
4. Feature wiring behind a flag.
5. End-to-end tests.
6. Legacy removal in a separate change.

Schema migration, business logic, and large UI redesign should not be combined in one commit. Each commit should state which phase checklist items it closes.

## 7. Execution tracking template

Copy this block under the active phase during implementation:

```text
Phase:
Status: Not started | In progress | Blocked | Verification | Complete
Owner:
Started:

Completed items:
-

Current work:
-

Blockers/decisions:
-

Tests executed:
- Command:
  Result:

Files changed:
-

Next step:
-
```

## 8. First implementation action

Start with **Phase 0**, not UI refactoring. Repair the Android test environment and add regression tests for ID loss, custom null-tournament state, rule-field mismatch, first-innings mapping, polling, and scorer identity. Then execute Phase 1 contract decisions before creating migrations.

This order prevents new Room and API schemas from encoding the same current ambiguities under different class names.
