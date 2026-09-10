# Match Center End-to-End Flow Audit

**Scope:** Android custom matches, Android tournament matches, Match Center read/scoring flows, API contracts, and Laravel processing
**Method:** Static, line-by-line trace of the current mobile and backend source
**Overall status:** **Not production-safe.** Viewing some server-backed tournament matches can work, but starting, scoring, syncing, changing innings, and completing a match are not connected end to end.

## 1. Executive conclusion

The central design defect is that the application treats three different records as if they share one ID:

| Identity | Meaning | Created by |
|---|---|---|
| Local fixture ID | Room/UI record, often `m_<timestamp>` | Android |
| Server fixture ID | Row in backend `fixtures` | Custom/tournament fixture API |
| Operational match ID | Row in backend `cricket_matches`; required by state/scoring APIs | `create-match` API |

Match Center and delivery sync require the **operational match ID**. The custom flow continues to navigate with the local fixture ID, while some tournament offline data substitutes a Java/Kotlin hash code. The mobile app calls the API that creates the operational match but discards its returned `match_id`. Consequently, a fixture can appear locally as Live while its backend match remains in `squad_selection`, or the app may call `/matches/{id}` with an ID that does not exist.

There are four release-blocking breaks:

1. **Operational match identity is lost.** `FixtureRepository.createOperationalMatchIfNeeded()` ignores the response containing `match_id` (`FixtureRepository.kt:476-487`).
2. **The mobile “start match” flow never performs backend lineup approval or toss.** It only stores player names/toss locally and then reports success (`MainViewModel.kt:88-139`).
3. **The scorer does not receive usable server player IDs.** `setPlayerServerIds()` is empty, IDs remain `0`, and Match Center never calls `setMatchContext()` (`LiveScorerViewModel.kt:27-33,69-79,825-869`; `MatchCenterScreen.kt:59-70`).
4. **Custom Match Center state crashes/fails on the backend.** The public API dereferences `$match->tournament` although a custom match has no tournament (`app/Http/Controllers/Api/V1/MatchController.php:11-14`).

## 2. Intended backend lifecycle

The backend implements this state machine:

```text
fixture
  -> create operational match
  -> squad_selection
  -> submit XI for both teams
  -> approve lineup
  -> toss_pending
  -> record toss
  -> live (innings 1 created)
  -> deliveries
  -> next innings
  -> deliveries
  -> completed
  -> submit result
  -> result_pending
  -> approve result
  -> approved
```

Important API operations are present:

- Custom match creation: `POST /api/v1/custom/fixtures/{fixture}/create-match` (`routes/api.php:61`).
- Tournament match creation: `POST /api/v1/admin/tournaments/{tournament}/fixtures/{fixture}/create-match` (`routes/api.php:114`).
- Custom lineup/toss: tournament placeholder `0` routes (`routes/api.php:120-122`).
- Tournament lineup/toss: normal tournament routes (`routes/api.php:124-125` and the adjacent toss route).
- Delivery batch sync: `POST /api/v1/matches/{match}/deliveries/sync` (`routes/api.php:73`).
- Next innings: `POST /api/v1/matches/{match}/next-innings` (`routes/api.php:75`).
- Result submit/approve: `routes/api.php:127-128`.

The Android `ApiService` declares lineup, approval, toss, result, and delivery endpoints (`ApiService.kt:670-735`), but declaration is not integration: the principal UI flow does not invoke most of them.

## 3. Custom match flow: actual behavior

### 3.1 Creation

`CreateMatchScreen` generates a local ID such as `m_<timestamp>` (`CreateMatchScreen.kt:542-545,595-613`). `CreateMatchViewModel` saves teams and fixture locally, then attempts background synchronization (`CreateMatchViewModel.kt:43-68,81-111`).

`FixtureRepository.saveFixtureWithSync()`:

- saves the Room fixture;
- creates an admin fixture with `tournamentId = "0"`;
- queues a sync operation;
- sends team IDs as `0` and relies on team names for custom fixtures (`FixtureRepository.kt:327-398`).

`pushPendingFixtureToServer()` calls the custom fixture endpoint and may store the server fixture ID (`FixtureRepository.kt:436-469`). However:

- any HTTP 2xx is treated as success even if the response body or ID is absent;
- invalid local dates silently become today's date (`FixtureRepository.kt:400-425`);
- local wall-clock time is suffixed with `Z` without a UTC conversion.

### 3.2 Toss and lineup

Navigation carries the same local identifier from Create Match to Toss, then Toss/Lineup, then Match Center (`MainActivity.kt:176-177,439-495`).

The toss screen calls `MainViewModel.saveTossDetails()`, which only updates local fixture data (`MainViewModel.kt:75-79`). Lineup selections are lists of **names**, not stable backend player IDs.

`MainViewModel.startMatch()` then:

1. reads the local fixture using the navigation ID;
2. sets its local status to `Live`;
3. stores toss and squad names locally;
4. pushes the custom fixture/status;
5. calls operational match creation;
6. invokes the success callback unconditionally (`MainViewModel.kt:88-139`).

It does **not** call:

- submit playing XI for home;
- submit playing XI for away;
- approve lineup;
- record toss.

Therefore the backend operational match remains before `live`, while Android claims it is Live.

### 3.3 Entry into Match Center

`createOperationalMatchIfNeeded()` calls the correct custom `create-match` route, but returns `Unit` and discards the response (`FixtureRepository.kt:476-487`). The response model explicitly contains `match_id` (`TournamentModels.kt:325-332`). No local schema field preserves it.

Match Center consequently requests:

```text
GET /api/v1/matches/m_<timestamp>/state
```

instead of:

```text
GET /api/v1/matches/<cricket_matches.id>/state
```

Laravel route-model binding returns 404 for the first form. If the correct custom operational ID is somehow supplied, the API still fails because `MatchController` calls `$match->tournament->publiclyVisibleNow()` on a null tournament (`app/Http/Controllers/Api/V1/MatchController.php:11-14`).

**Custom-flow verdict: BROKEN end to end.** Local UI simulation may appear functional, but server state, server scoring, recovery on another device, and authoritative results do not work.

## 4. Tournament match flow: actual behavior

### 4.1 Fixture listing and identifier selection

The tournament fixture API includes both fixture `id` and nullable operational `match_id` (`TournamentModels.kt:103-124`). The backend constructs `match_id` from the fixture's related match (`app/Http/Controllers/Api/V1/TournamentController.php:45-58`).

The tournament tabs correctly prefer it:

```kotlin
val matchId = fixture.match_id?.toString() ?: fixture.id.toString()
```

(`TournamentMatchesTab.kt:96-100`; similar logic in `TournamentHomeTab.kt:294,494`). Live/completed cards navigate directly to Match Center; pre-live cards enter the toss/lineup flow (`TournamentMatchesTab.kt:140-163`).

This is correct only when `match_id` exists. When it is absent, the fallback is a **fixture ID**, yet all later variables call it `matchId`. The start path never converts that fixture ID into the newly created operational ID.

### 4.2 Offline/local tournament fallback is invalid

`Fixture.toTournamentFixtureData2()` manufactures both `id` and `match_id` using `id.hashCode()` (`TournamentViewModel.kt:486-501`). A hash is neither a server fixture ID nor a server match ID. Collisions are possible, values are semantically meaningless to Laravel route binding, and later Match Center/scoring requests can only fail.

### 4.3 Starting a tournament match

Tournament UI enters the same `MainActivity` toss/lineup route and the same `MainViewModel.startMatch()` used by custom matches (`MainActivity.kt:293-309,439-495`). Thus the same missing calls occur: no server XI submission, approval, or toss.

There is an additional repository ambiguity: `extractTournamentId()` derives tournament identity heuristically from local fixture data (`FixtureRepository.kt:492-501`). A match workflow should not infer relational identity from a formatted string.

The backend's `MatchService.createFromTeams()` also requires a completed tournament draft (`app/Modules/Scoring/Services/MatchService.php:79`). This conflicts with tournaments configured without a draft unless another path supplies eligible squads. That is a backend business-rule gap needing an explicit no-draft roster rule.

**Tournament-flow verdict:**

- Viewing an already-live public tournament match with a real `match_id`: **partially works**.
- Starting any scheduled tournament match from mobile: **broken/incomplete**.
- Offline/local fallback tournament match: **broken**.
- Scoring and completion: **broken** for the reasons below.

## 5. Match Center read path

### 5.1 Load sequence

`MatchCenterScreen` calls `loadFixture(matchId)` (`MatchCenterScreen.kt:59-70`). `MatchCenterViewModel` first runs broad pending push/pull synchronization, then subscribes to `MatchApiRepository.getMatchState()` (`MatchCenterViewModel.kt:47-84`).

The repository emits a Room fixture first, then calls `GET /api/v1/matches/{matchId}/state` (`MatchApiRepository.kt:29-79`). This cache-first approach is reasonable, but exceptions and non-2xx responses are swallowed, so stale Room data can look like a successful live connection.

### 5.2 Public visibility prevents legitimate admin/scorer loading

The state endpoint only returns matches whose tournament is publicly visible and whose status is one of live/completed/result_pending/approved (`app/Http/Controllers/Api/V1/MatchController.php:11-14`). This endpoint is used even for an authenticated organizer/scorer. It therefore cannot support:

- private tournament scoring;
- squad selection or toss screens;
- custom matches;
- administrative recovery before `live`.

A separate authenticated admin state endpoint, or authorization-aware state policy, is needed.

### 5.3 Incorrect response mapping

`MatchStateData.toScheduledFixture()` has severe semantic errors (`MatchStateResponse.kt:93-126`):

- It treats first-innings batting team as `homeTeam` and bowling team as `awayTeam`. If the away team wins the toss and bats, displayed home/away identity is swapped.
- `currentRuns`, `currentWickets`, and `oversBowled` read only `secondInnings`; during innings one the UI shows zero.
- It hardcodes `ballType = "Leather"`, `matchType = "T20"`, `wickets = 10`.
- It leaves venue/date/time empty.
- Bowling overs assume six balls per over (`MatchStateResponse.kt:146-150`) even though rule profiles can differ.
- It drops revision, innings IDs, team IDs, match-player IDs, striker/non-striker/bowler, target, extras, fall of wickets, partnerships, recent-delivery detail, and structured result data.

The repository then overlays cached local names, venue, date, time, wickets, and overs on top of server data (`MatchApiRepository.kt:44-63,93-112`). This can overwrite authoritative server values with stale local values.

### 5.4 Polling stops when nothing changes

`pollMatchState()` returns `null` when the fetched value equals cache (`MatchApiRepository.kt:146-153`). `MatchCenterViewModel` interprets `null` as “fixture no longer found” and stops polling (`MatchCenterViewModel.kt:110-132`). A normal ten-second interval with no new ball therefore terminates live updates.

### 5.5 UI writes are local-only

Match Center's fixture/status update methods call Room repository updates despite comments implying server synchronization (`MatchCenterViewModel.kt:146-165`). Back navigation locally forces Live in `MainActivity.kt:505-515`, which can corrupt completed/other status locally.

**Read-path verdict:** Existing public tournament data can render, but team identity, innings-one score, rule metadata, freshness, and error visibility are unreliable.

## 6. Scoring and delivery synchronization

### 6.1 Match context is never connected

`LiveScorerViewModel` requires `setMatchContext(matchId, inningsNumber)` before it persists a delivery (`LiveScorerViewModel.kt:65-72,825-835`). `MatchCenterScreen` loads the match but never calls it (`MatchCenterScreen.kt:59-70`). As a result, `persistDelivery()` returns immediately while `matchId` is blank.

### 6.2 Player identity is unusable

The scorer initializes striker, non-striker, and bowler server IDs to `0` (`LiveScorerViewModel.kt:31-33`). `setPlayerServerIds()` has an empty body (`LiveScorerViewModel.kt:74-79`). Persisted deliveries consequently contain IDs `0` (`LiveScorerViewModel.kt:849-869`).

Backend scoring validates that each player belongs to the approved XI and correct batting/bowling team. Zero cannot pass those checks. Player display names are not sufficient; the UI must carry backend `match_player`/player IDs through roster selection and scorer state.

### 6.3 Local scoring is a separate, non-authoritative engine

The scorer mutates Compose state immediately and queues a Room delivery. It does not derive each next state from the backend revision. The backend `MatchScoringService` is the authoritative engine: it locks the match, verifies live/current innings, validates player roles and expected revision, records delivery/wicket data, and rebuilds statistics (`app/Modules/Scoring/Services/MatchScoringService.php`, especially the delivery validation/update path around lines 20-130).

Without a complete server snapshot and stable IDs, the two engines can diverge in strike rotation, legal-ball counting, wickets, totals, and innings state.

### 6.4 Delivery sync failure modes

`DeliverySyncRepository.syncMatchDeliveries()` (`DeliverySyncRepository.kt:70-139`) has these defects:

- It sends the stored navigation `matchId`; today that may be a local ID, fixture ID, or hash rather than operational ID.
- It formats a device-local timestamp with a literal `Z`, falsely declaring UTC.
- It selects only records with status `pending`; records marked `failed` are not retried by this method.
- Any HTTP 2xx marks every pending row synced without validating per-delivery acknowledgments/UUIDs.
- It deletes synced rows immediately, reducing audit and recovery capability.

Backend batch sync has useful UUID idempotency and a transaction (`ScoringController.php:33-104`), but:

- `device_timestamp` is validated only as a string and sorted lexicographically (`ScoringController.php:38,74`);
- there is no visible batch-size limit;
- a single invalid new item rolls back the batch;
- acknowledgment ordering differs when existing UUIDs and new deliveries are mixed.

### 6.5 Shared scorer state can leak across matches

`MainActivity` owns one shared `LiveScorerViewModel` and passes it into Match Center. It contains mutable initialization flags, history, squads, match context, and scoring state. Without a mandatory reset keyed by operational match ID, opening another match can reuse the previous match's players or score.

**Scoring verdict: BROKEN.** On-screen tapping can update local UI, but reliable API delivery recording is not currently achievable.

## 7. Innings and result completion

The backend exposes `next-innings`, result submit, and result approve routes (`routes/api.php:75,127-128`). The Android service exposes result endpoints (`ApiService.kt:708-723`).

However, Match Center's declare-innings callback only changes local fixture fields/status (`MainActivity.kt:520-553`). It never calls backend `next-innings`. There is no completed end-to-end UI sequence that submits and approves the backend result.

This creates contradictory states:

- Android may show innings two while backend still expects innings one;
- subsequent delivery sync is rejected for the wrong innings;
- Android may show Completed while backend remains Live;
- standings are not rebuilt because backend result approval never occurs.

**Completion verdict: BROKEN.**

## 8. Feature status matrix

| Feature | Custom | Tournament | Evidence/result |
|---|---:|---:|---|
| Create local fixture | Working locally | Working locally/API-dependent | Room records are created |
| Sync fixture | Partial | Partial | IDs and error handling are unsafe |
| Create operational match | API called | API called when inferred | Returned `match_id` discarded |
| Select lineup in UI | Visual/local | Visual/local | Names only |
| Submit playing XI | Not working | Not working | API never called |
| Approve lineup | Not working | Not working | API never called |
| Record toss | Not working server-side | Not working server-side | Local save only |
| Enter backend Live state | Not working | Not working from mobile start | Backend state machine skipped |
| View public live match | Broken by null tournament | Partial | Tournament requires real match ID/public visibility |
| Innings-one live score | Incorrect | Incorrect | Mapper reads second innings only |
| Poll live state | Stops early | Stops early | Unchanged response interpreted as missing |
| Score locally | Visual only | Visual only | Independent Compose state |
| Persist delivery locally | Usually not working | Usually not working | Match context never set |
| Sync delivery to API | Not working | Not working reliably | Wrong match/player IDs |
| Start next innings | Not working | Not working | Local callback only |
| Submit/approve result | Not working | Not working | No wired workflow |
| Update standings | N/A | Not working via mobile | Requires backend result approval |

## 9. Prioritized defects

### P0 — Release blockers

1. Introduce explicit `localFixtureId`, `serverFixtureId`, and `serverMatchId`; never reuse `id` for all three.
2. Persist `serverMatchId` from every `create-match` response and navigate Match Center only with it.
3. Replace the local-only start flow with the backend state machine: create match → submit both XIs by server IDs → approve → toss → confirm returned status Live.
4. Fix custom match state authorization/null handling in backend `MatchController`.
5. Load server roster/match-player IDs; implement player-name-to-ID mapping or, preferably, make scorer selection ID-based.
6. Call `setMatchContext(serverMatchId, serverCurrentInnings)` and reset scorer state whenever that ID changes.
7. Wire declare innings to `/next-innings`, and completion to result submit/approve.

### P1 — Correctness and resilience

1. Replace `ScheduledFixture` as the match-state transport with a dedicated `MatchCenterState` containing fixture, match, innings, rule, roster, revision, and result identities.
2. Fix first-innings mapping and preserve home/away fixture identity independently of batting order.
3. Make polling return a sealed result such as `Changed`, `Unchanged`, `NotFound`, `Unauthorized`, and `NetworkError`.
4. Stop swallowing state API errors; surface offline/stale/error state in the UI.
5. Use rule-profile `balls_per_over`, wickets, format, and overs instead of hardcoded values.
6. Send true ISO-8601 UTC timestamps and validate them as dates on the backend.
7. Retry failed delivery rows with bounded backoff; validate acknowledgment UUIDs before marking synced.
8. Retain a bounded synced-delivery audit or server-confirmed cursor rather than immediately erasing all evidence.

### P2 — Contract and maintainability

1. Remove all hash-code ID fallbacks (`TournamentViewModel.kt:486-501`). Use nullable IDs and disable server actions until synchronization resolves them.
2. Remove heuristic tournament ID extraction; persist the relation explicitly.
3. Define no-draft tournament squad eligibility in `MatchService` instead of requiring a completed draft for every tournament.
4. Add batch-size limits and deterministic acknowledgment order to delivery sync.
5. Separate authenticated scorer/admin match state from public spectator visibility rules.

## 10. Recommended target data model

```kotlin
data class MatchReference(
    val localFixtureId: String,
    val serverFixtureId: Long?,
    val serverMatchId: Long?,
    val tournamentId: Long?,
)

data class MatchCenterState(
    val reference: MatchReference,
    val serverStatus: MatchStatus,
    val revision: Long,
    val fixtureHomeTeam: TeamRef,
    val fixtureAwayTeam: TeamRef,
    val battingTeam: TeamRef?,
    val bowlingTeam: TeamRef?,
    val currentInningsId: Long?,
    val currentInningsNumber: Int?,
    val striker: MatchPlayerRef?,
    val nonStriker: MatchPlayerRef?,
    val bowler: MatchPlayerRef?,
    val ruleProfile: MatchRuleSnapshot,
    val innings: List<InningsState>,
    val result: MatchResultState?,
)
```

Room should enforce uniqueness for non-null server fixture and match IDs. UI navigation should pass a typed/reference key, and repositories should refuse scoring unless `serverMatchId`, live status, current innings, and all three player IDs are present.

## 11. Recommended corrected sequences

### Custom

```text
Create/sync teams -> receive team IDs
Create/sync fixture -> persist serverFixtureId
Create operational match -> persist serverMatchId
Submit XI(team A IDs) -> submit XI(team B IDs)
Approve lineup -> record toss -> backend returns Live/current innings
Navigate using serverMatchId
Fetch authenticated state -> score with revision + match-player IDs
Next innings -> refresh authoritative state
Submit result -> approve result (authorized role)
```

### Tournament

```text
Load fixture -> require real fixture.id/match_id
If match_id null: create operational match and persist returned ID
Load eligible squad IDs (drafted squad or explicit no-draft roster)
Submit/approve XI -> toss -> Live
Navigate using match_id only
Use the same authoritative scoring/innings/result sequence as custom
```

## 12. Test coverage required before release

At minimum, automate:

- custom and tournament ID propagation from fixture creation through Match Center;
- away team wins toss and bats first without swapping home/away display;
- innings-one mapping shows non-zero live score;
- unchanged polling continues; 404, 403, offline, and stale-cache states differ visibly;
- six-ball and non-six-ball rules;
- player IDs always belong to approved XI and correct team;
- duplicate UUID delivery sync, partial/retry behavior, app restart recovery, and two-device revision conflicts;
- end of innings → next innings → chase completion;
- tie/no-result/win result submission and approval;
- private tournament organizer scoring and custom match state access;
- switching between two live matches clears scorer history, roster, and context.

## 13. Final assessment

The current Match Center is best understood as a local scoring prototype attached to a partial server reader, not as an integrated match lifecycle. The backend has most of the authoritative domain operations, but Android skips the state transitions and loses the identities required to invoke them. Fixing UI symptoms alone will not stabilize this flow; the first repair must be the identity model and an orchestrated backend lifecycle, followed by authoritative state mapping and delivery synchronization.

This report is based on source inspection. It does not claim successful runtime verification; the repository's Android build environment should be repaired and the above integration suite added before the flow is considered working.

## 14. Configuration inheritance audit

The required rule is:

```text
Custom match     -> use the configuration entered for that match
Tournament match -> snapshot the tournament configuration when the match is created
```

The current code does not enforce this rule.

### 14.1 Custom configuration is collected but lost

The custom Create Match screen collects overs, match type, ball type, and wickets, but starts with hardcoded date `19 Aug 2026`, time `16:37`, 6 overs, T20, and Tennis (`CreateMatchScreen.kt:55-67`). The choices and suggestions are also embedded directly in the Composable (`CreateMatchScreen.kt:360-516`).

These values reach `ScheduledFixture` and the local `fixtures` table (`ScheduledFixture.kt:7-18`; `FixtureEntity.kt:7-19`). They do not reach `AdminFixtureEntity`, which has no real scoring configuration fields (`AdminFixtureEntity.kt:11-54`).

More importantly, the custom fixture request sends only teams, schedule, venue, city, and timezone (`TournamentModels.kt:219-231`). The backend accepts that same limited set (`CustomFixtureController.php:51-89`). Overs, wickets, playing-XI size, format, ball type, and balls per over therefore stop at Room.

Backend operational-match creation assigns every custom match the shared `default-custom` T20 profile with 20 overs, 10 wickets, and 11 players (`MatchService.php:49-64`). A locally configured 6-over, 5-wicket tennis match can consequently be validated and completed by the server as a different match.

### 14.2 Backend fallback profile fields are inconsistent

`CricketRuleProfile` defines `maximum_wickets` and `max_overs_per_bowler` (`CricketRuleProfile.php:13-38`). `MatchService` fallback creation instead writes `wickets_per_innings` and `max_over_per_bowler` (`MatchService.php:33-44,52-60`). These names are not fillable model attributes. Values can be discarded or replaced by database defaults, making the fallback profile unreliable even before mobile consumes it.

### 14.3 Wickets, playing XI, and squad size are different concepts

Mobile derives lineup size as `wickets + 1` (`LineupViewModel.kt:71-83`) and tournament creation derives wickets as `squadSize - 1` (`CreateTournamentScreen.kt:68-76,87-95`). This is not a safe cricket rule:

- squad size is the pool available to the team;
- playing-XI size is the number selected for this match;
- maximum wickets controls innings termination;
- last-man-standing changes their relationship.

The backend already models `playing_xi_size`, `maximum_wickets`, and `last_man_standing` separately. Mobile must do the same.

### 14.4 Tournament settings are only partially propagated

Tournament creation captures ball type, squad size, and overs (`CreateTournamentScreen.kt:40-55`), but `CreateTournamentRequest` cannot send `cricket_rule_profile_id` (`TournamentModels.kt:149-169`) even though the backend accepts it (`AdminTournamentController.php:120-126`).

Mobile's `RuleProfileData` reads only name, format, overs, and legal balls per over (`TournamentModels.kt:49-54`). It drops playing-XI size, maximum wickets, innings per side, bowler limit, extra rules, last-man-standing, and over caps. Later fallback values in `TournamentViewModel.kt:451-474` hide this missing data.

`MatchService` does correctly associate a tournament match with the tournament profile, but generates a hardcoded profile if missing (`MatchService.php:27-48`). It also always requires a completed draft (`MatchService.php:71-98`), including tournaments whose `has_draft` setting is false.

### 14.5 Required immutable configuration snapshot

One validated object must be used by the form, Room, API, backend match, scorer, scorecard, and result engine:

```json
{
  "format": "custom",
  "innings_per_side": 1,
  "overs_per_innings": 6,
  "playing_xi_size": 6,
  "maximum_wickets": 5,
  "legal_balls_per_over": 6,
  "max_overs_per_bowler": 2,
  "ball_type": "tennis",
  "no_ball_runs": 1,
  "wide_runs": 1,
  "wide_runs_to_batsman": false,
  "noball_runs_to_batsman": false,
  "last_man_standing": false,
  "max_balls_per_over": null,
  "max_runs_per_over": null
}
```

Custom matches create this snapshot from their configuration screen. Tournament matches copy it from the tournament's active profile. It becomes immutable when the operational match starts, so editing a tournament later cannot change a live or historical match.

## 15. Match-affecting hardcoding inventory

| Location | Hardcoding/fallback | Effect |
|---|---|---|
| `CreateMatchScreen.kt:61-67` | Fixed date/time and rule defaults | Artificial/stale initial data |
| `CreateMatchScreen.kt:421-425` | Format labels only | Selecting T10/T20 does not create corresponding rules |
| `CreateMatchScreen.kt:462-464` | Tennis/Leather only | Does not match backend ball-type enum |
| `ScheduledFixture.kt:18` | Three capitalized statuses | Incompatible with backend lifecycle |
| `LineupViewModel.kt:49,68,81-83` | 11 and wickets + 1 | Wrong lineup requirement |
| `LiveScorerScreen.kt:45` | 6 balls per over default | Wrong unless a real snapshot always overrides it |
| `MatchStateResponse.kt:105-107` | Leather/T20/10 wickets | Replaces server truth with false data |
| `TournamentViewModel.kt:465-471` | Tennis/20/11 fallbacks | Hides missing configuration |
| `MatchService.php:31-64` | Generated fallback profiles | Server can use rules never selected by user |

Defaults are acceptable only for initializing a form. After confirmation, no scoring component should invent a fallback. A missing required match rule must become an explicit invalid/not-ready state.

## 16. Offline-first and slow-network audit

The requested behavior is achievable: a scoring tap commits locally and updates UI immediately; network synchronization is independent and never blocks scoring.

### 16.1 Existing foundations

- Room fixture and pending-delivery storage.
- Per-delivery UUID for idempotency (`PendingDeliveryEntity.kt:46-50`).
- Connectivity callback (`ConnectivityMonitor.kt:15-105`).
- WorkManager constrained to a connected network (`SyncWorker.kt:65-90`).
- General pending-change queue (`SyncManager.kt:102-159`).
- Backend transactional batch sync and UUID deduplication.

### 16.2 Current gaps and lag sources

- UI/ViewModel flows directly call `pushPendingChanges()` after local saves; slow HTTP can delay callbacks and navigation. Player registration is one example (`LineupViewModel.kt:138-163`).
- `preSyncBeforeLiveScoring()` pushes and pulls before entry (`SyncManager.kt:630-645` onward). An “online” but unusably slow connection can block until HTTP timeouts.
- The periodic worker runs every 15 minutes (`SyncWorker.kt:67-89`), which is only suitable as a recovery safety net, not live synchronization.
- Connectivity uses `NET_CAPABILITY_INTERNET`, not `NET_CAPABILITY_VALIDATED` (`ConnectivityMonitor.kt:49-68`), so captive portals and broken upstreams appear online.
- The general queue sends unrelated changes serially before deliveries (`SyncManager.kt:124-148`); a tournament/team backlog can delay live balls.
- General changes are deleted after five failures (`SyncManager.kt:150-151`) without a visible dead-letter recovery path.
- Scoring state is mutated in memory, then Room persistence is launched afterward. Insert failures are silently swallowed (`LiveScorerViewModel.kt:825-874`), so it is not crash-safe.

### 16.3 Required non-blocking write path

```text
Scoring tap
  -> validate with local immutable rule snapshot
  -> one Room transaction:
       append immutable MatchEvent/Delivery
       update local score projection
       append SyncOutbox row
  -> UI observes Room and updates immediately
  -> tap handler returns (no awaited HTTP)

Independent sync actor
  -> batch queued events for this operational match in sequence order
  -> send only when validated network exists
  -> verify every UUID acknowledgment and server revision
  -> mark acknowledged rows synced
  -> reconcile authoritative server snapshot into Room
```

Compose/ViewModel memory must be a projection of Room `Flow`, not the only authoritative local record.

### 16.4 Correct synchronization strategy

Use a unique one-time WorkManager chain per operational match, enqueued whenever an outbox event is committed. Retain the 15-minute periodic worker only for process-death/recovery sweeps.

While Match Center is open, use one foreground sync actor per match:

- debounce approximately 250–750 ms to batch rapid balls;
- cap batches, for example 20–50 deliveries;
- never run concurrent senders for the same match;
- order by a monotonic local sequence, not device timestamps;
- retry network, 5xx, and 429 failures with bounded exponential backoff;
- dead-letter validation/authorization failures and show operator action;
- expose `Saved locally`, `Syncing`, `Synced`, and `Needs attention` without disabling scoring.

WorkManager provides eventual delivery; the foreground actor provides near-live sync. Neither should block the UI.

### 16.5 Required atomic local event data

Each local transaction must retain:

- event UUID and device ID;
- monotonic local sequence;
- local match ID plus nullable server match ID;
- local/server innings identity;
- stable local/server player identities;
- configuration snapshot version;
- event payload and resulting local projection;
- base server revision;
- outbox status, attempts, next-attempt time, and classified error.

## 17. Conflict and authority model

Offline scoring needs an explicit multi-device policy:

1. Prefer one active scorer lease/device per match while online.
2. Send `baseServerRevision` with each event batch.
3. Backend accepts UUID-idempotent events only at the expected revision/sequence.
4. A conflict pauses automatic upload and fetches authoritative state.
5. Never silently overwrite or reorder accepted deliveries.
6. Genuine multi-device divergence requires an operator reconciliation screen.

Device clocks are not reliable ordering keys. Use `(device_id, local_sequence)` locally and server revision globally.

## 18. Required schema and API redesign

### Android Room

- `MatchEntity`: local ID, server fixture ID, server match ID, tournament ID, source, local/server status, revision.
- `MatchRuleSnapshotEntity`: every rule, version, and `CUSTOM`/`TOURNAMENT` origin.
- `MatchTeamEntity`: explicit home/away identity, separate from current batting/bowling roles.
- `MatchPlayerEntity`: local player, server player, match-player, team, and selection IDs.
- `MatchInningsEntity`: identities, teams, target, totals, and completion reason.
- `MatchEventEntity`: immutable local event log.
- `SyncOutboxEntity`: aggregate, UUID, sequence, payload, state, attempts, retry time, and error class.

Player/stat lists that require identity, querying, and synchronization should not remain mutable JSON strings in one fixture row.

### Backend/API

1. Custom match creation must accept and validate a complete rule snapshot.
2. Creation responses must include client correlation ID, server fixture/match IDs, revision, status, rules, teams, and match players.
3. Authenticated Match Center state must return the complete immutable rule snapshot.
4. Custom/private match state needs policy-aware authenticated access.
5. Batch sync must return acknowledgment/rejection for every UUID plus authoritative revision/state.
6. Creation commands need client-generated idempotency keys.
7. Normalize enum wire values such as `tennis`; do not mix `Tennis`, `Tennis Ball`, and `tennis`.
8. Fix backend fallback-profile attribute names and remove silent fallback creation once configuration becomes mandatory.

## 19. Revised implementation order

1. Define normalized enums, `MatchConfiguration`, typed identities, and lifecycle states.
2. Fix backend rule fields and implement complete custom/tournament snapshot contracts.
3. Migrate Room to separate identities, rules, players, innings, immutable events, and outbox.
4. Make creation local-first and backend creation asynchronous/idempotent; persist returned IDs.
5. Make tournament matches inherit and freeze tournament rules; disallow hidden overrides.
6. Wire XI, approval, and toss using stable server IDs and real backend transitions.
7. Replace in-memory scorer authority with atomic repository writes observed through Room Flow.
8. Add foreground per-match sync and one-time WorkManager recovery with verified acknowledgments.
9. Replace Match Center's lossy mapper and broken polling with explicit reconciliation states.
10. Wire next innings and results to backend commands while preserving local offline intent.
11. Add migration, contract, process-death, no/slow-network, retry, and multi-device conflict tests.

This design gives the intended result: custom matches obey their own saved configuration, tournament matches obey a frozen tournament configuration, scoring remains fully usable without internet, slow internet never causes input lag, and queued data synchronizes safely in the background.
