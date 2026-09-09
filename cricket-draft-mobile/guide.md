# STUMPS Cricket Platform: Testing & Verification Guidelines

This document details the manual and automated testing guidelines for validating custom match rules and scoring configurations in the platform.

---

## 📋 Verification Guidelines for Phase 1: Custom Match Rules

Use these guidelines to write tests or perform manual QA sessions when validating custom over structures, batsman run credits, junior cricket limits, and Last-Man-Standing scoring configurations.

### 1. 🏏 Custom Over Size Verification
*   **Objective**: Confirm that overs increment only when the configured number of legal balls has been bowled.
*   **Test Steps**:
    1.  Create a tournament rule profile with `legal_balls_per_over` set to `7` or `8`.
    2.  Create a live match under this profile.
    3.  Bowl consecutive legal deliveries.
    4.  Verify that the `over_number` on the recorded deliveries remains `1` up to delivery 7 (for a 7-ball over), and the `ball_number` goes from `1` to `7`.
    5.  Record the next legal delivery and assert that it correctly starts over `2`, ball `1`.
*   **Expected Result**: Overs transition dynamically based on the rule profile's legal ball limit.

### 2. ➕ Batsman Runs Credits for Extras
*   **Objective**: Verify that batsman personal scores are credited with wide/no-ball extra runs when specific rule flags are active.
*   **Test Steps**:
    1.  Enable both `wide_runs_to_batsman` and `noball_runs_to_batsman` flags in the tournament rule profile.
    2.  Log a Wide ball (1 wide run) and verify that the striker's batting stats runs increment by `1`.
    3.  Log a No-Ball (1 no-ball run + 2 runs off the bat) and verify that the striker's runs increment by `3` (previous 1 + 1 penalty + 2 runs).
*   **Expected Result**: Striker's personal score increases directly by the wide/no-ball runs, instead of these runs counting solely as team extras.

### 3. 🚫 Junior Cricket Capping Limits
*   **Objective**: Assert that overs are capped when the maximum allowed deliveries (legal + illegal) or runs are reached.
*   **Test Steps**:
    1.  Set `max_balls_per_over` to `8` and `max_runs_per_over` to `10` in the rule profile.
    2.  Bowl 8 Wide balls consecutively.
    3.  Attempt to record a 9th delivery in that same over.
    4.  Verify that the system throws a validation error indicating the over limit has been reached, forcing the over to close.
*   **Expected Result**: Scorer is prevented from adding more deliveries than the defined over caps.

### 4. 👥 Last Man Standing (LMS) Rules
*   **Objective**: Confirm that the innings continues after the 9th wicket falls when LMS is enabled.
*   **Test Steps**:
    1.  Create a team with a `playing_xi_size` of `2` and rule profile `maximum_wickets` of `2`. Enable `last_man_standing`.
    2.  Log the first wicket. Verify that the match status remains `live` (in standard rules, the match would be marked completed at 1 wicket down out of 2 players).
    3.  Log the second wicket (striking out the last man). Verify that the match and innings status transition to `completed` and the result is marked `all_out`.
*   **Expected Result**: Innings only completes when all players (including the last man standing) are out.

---

## 🛠️ Phase 2: Live Scoring & Offline Sync Support Verification

Use these guidelines to test the `/api/v1/matches/{match}/deliveries/sync` batch sync API.

### 1. 📶 Offline Batch Scoring Sync Verification
*   **Objective**: Confirm that the backend can sync a batch of deliveries recorded offline, processing them chronologically.
*   **Test Steps**:
    1.  Construct a JSON payload containing an array of 3 deliveries, each with a unique `local_uuid` and sequential `device_timestamp` timestamps.
    2.  Submit the payload to `POST /api/v1/matches/{match}/deliveries/sync` using an authenticated Sanctum token.
    3.  Assert the response returns status `200` with each delivery marked as `"status": "synced"`.
    4.  Verify the database records contain the 3 deliveries with their respective `local_uuid` and `device_timestamp` values correctly stored.
*   **Expected Result**: All 3 deliveries are processed chronologically and successfully written to the database.

### 2. 🔂 Sync Idempotency (Duplicate Prevention)
*   **Objective**: Assert that if a sync request is retried (e.g. due to connection timeouts), duplicate deliveries are ignored.
*   **Test Steps**:
    1.  Submit the exact same batch of 3 deliveries (using the same `local_uuid` values) to the sync endpoint again.
    2.  Assert that the response returns status `200`, but each delivery is now returned with `"status": "already_sync"`.
    3.  Verify the database still contains exactly 3 deliveries for this match (no new deliveries were created).
*   **Expected Result**: Backend recognizes existing `local_uuid` records, skipping duplicates and returning success indicators to the client.

---

## 🛠️ Phase 3: Tournament Draft Room & Live Draft Settings Verification

Use these guidelines to test the tournament `has_draft` setting and route protection middleware.

### 1. 🎛️ has_draft Storage Verification
*   **Objective**: Verify that the `has_draft` boolean is correctly saved during tournament creation and update.
*   **Test Steps**:
    1.  Send a `POST /api/v1/admin/tournaments` request with payload `'has_draft' => true`.
    2.  Verify the returned tournament object has `has_draft: true` and is persisted in the database.
    3.  Send a `PUT /api/v1/admin/tournaments/{tournament}` request with payload `'has_draft' => false`.
    4.  Verify that `has_draft` is updated to `false` in the database.
*   **Expected Result**: `has_draft` setting is stored and updated correctly.

### 2. 🛡️ Draft Room Route Protection
*   **Objective**: Confirm that accessing draft screens or endpoints for a tournament without a draft throws a 403 Forbidden error.
*   **Test Steps**:
    1.  Identify/create a tournament where `has_draft` is `false`.
    2.  Attempt to request `GET /api/v1/tournaments/{tournament}/draft/state`.
    3.  Assert the response returns status `403 Forbidden` with the message `"Drafting is not enabled for this tournament."`.
    4.  Verify the same 403 response on web routes like `/admin/tournaments/{tournament}/draft/setup`.
*   **Expected Result**: Access is blocked and returns a clear 403 response, preventing any drafting operations.

---

## 🛠️ Phase 4: Live Broadcast Sync Channels Verification

Use these guidelines to verify real-time event broadcasting over Pusher.

### 1. 📢 Scoring Event Broadcast Verification
*   **Objective**: Verify that `DeliveryRecorded` is fired with the correct metrics payload whenever a delivery is scored.
*   **Test Steps**:
    1.  Subscribe a test WebSocket client to the channel `matches.{match_id}`.
    2.  Record a new delivery via the scoring endpoint.
    3.  Verify the client receives a broadcast event named `delivery.recorded`.
    4.  Assert that the event payload contains:
        *   `delivery` parameters (runs, extras, notation, etc.).
        *   `match` totals (overall runs, wickets, legal balls, and new revision number).
*   **Expected Result**: Real-time broadcast fires with complete delivery/match statistics.

### 2. ↩️ Undo Event Broadcast Verification
*   **Objective**: Confirm that `DeliveryUndone` is fired when the scorer voids the last delivery.
*   **Test Steps**:
    1.  Ensure the WebSocket client is subscribed to `matches.{match_id}`.
    2.  Submit an undo request to the match undo endpoint.
    3.  Verify the client receives a broadcast event named `delivery.undone`.
    4.  Assert that the event payload contains the updated match scorecard totals after rollback.
*   **Expected Result**: Real-time undo broadcast fires with updated match scores.

---

## 🛠️ Phase 5: Wagon Wheel Coordinate System Verification

Use these guidelines to verify spatial vector coordinate tracking for match deliveries.

### 1. 📍 Coordinate Storage & Payload Verification
*   **Objective**: Verify that `wagon_x` and `wagon_y` coordinates are correctly saved in the database and validated between `0` and `100`.
*   **Test Steps**:
    1.  Submit a score delivery request with `'wagon_x' => 45.5` and `'wagon_y' => 78.2`.
    2.  Verify the HTTP status is `200` and the database contains the recorded coordinates.
    3.  Submit a score delivery request with `'wagon_x' => 105.0` (out of bounds).
    4.  Verify that validation fails with a `422 Unprocessable Entity` status.
*   **Expected Result**: Valid coordinates are saved successfully, and out-of-bounds inputs are blocked.

### 2. 📡 Real-Time Coordinate Broadcasting
*   **Objective**: Ensure the `DeliveryRecorded` event includes the spatial coordinates in its payload.
*   **Test Steps**:
    1.  Subscribe a test WebSocket client to `matches.{match_id}`.
    2.  Submit a valid score delivery with coordinates.
    3.  Verify that the broadcast event payload contains the matching `wagon_x` and `wagon_y` attributes under the `delivery` details.
*   **Expected Result**: Coordinates are broadcasted in real-time to the listeners.

---

## 🛠️ Phase 6: MVP Point Calculation System Verification

Use these guidelines to verify player performance MVP points logic.

### 1. 🏆 MVP Points Calculations & Leaderboard Sorting
*   **Objective**: Confirm that player batting, bowling, and fielding points are calculated correctly and sorted by total points.
*   **Test Steps**:
    1.  Create a match and score multiple deliveries involving batters scoring runs (with boundary/six/run milestones), bowlers taking wickets and bowling maidens (with extras deductions), and fielders making catches/run-outs.
    2.  Submit a `GET /api/v1/matches/{match}/mvp` request with an authorized token.
    3.  Verify the HTTP status is `200` and the response payload contains player statistics along with their calculated points break-downs.
    4.  Assert that:
        *   Batting points reflect runs + boundary credits + milestone bonuses.
        *   Bowling points reflect wickets + maidens - extras.
        *   Fielding points reflect catches, stumpings, and run-outs.
        *   The array of players is sorted by total points in descending order.
*   **Expected Result**: Performance metrics maps to the correct point weightings, and the leaderboard is sorted by total points.

---

## 🛠️ Phase 7: Player Comparison API Verification

Use these guidelines to verify the player comparison service and endpoint.

### 1. 📊 Side-by-Side Statistics Verification
*   **Objective**: Verify the compared player stats return accurate tournament aggregates and ratios.
*   **Test Steps**:
    1.  Identify two approved players in a tournament who have matches played and deliveries scored.
    2.  Perform a `GET /api/v1/tournaments/{tournament}/players/compare` query passing `player1_id` and `player2_id` parameters.
    3.  Assert the response returns status `200` with both players compared side-by-side.
    4.  Verify that:
        *   Batting averages match total runs divided by total dismissals.
        *   Strike rates match runs divided by balls faced multiplied by 100.
        *   Bowling averages, economies, and strike rates correspond precisely to the cumulative tournament deliveries.
*   **Expected Result**: Comparison API successfully aggregates and displays side-by-side profiles of the two players.

---

## 🛠️ Phase 9: Player Profiles (Granular Query Filters & Insights) Verification

Use these guidelines to verify player career statistics filtering and graph insight API feeds.

### 1. 🔍 Granular Career Statistics Query Filters
*   **Objective**: Ensure the player statistics endpoint handles Year, Format, Ball Type, and Data Source filters correctly.
*   **Test Steps**:
    1.  Submit a `GET /api/v1/players/{playerProfile}/stats` request without parameters. Verify cumulative stats are returned.
    2.  Query the endpoint with `?year=2026`. Verify the calculations only include matches starting in 2026.
    3.  Query the endpoint with `?format=t20` or `?ball_type=tennis`. Verify statistics correctly isolate matching matches.
*   **Expected Result**: Career stats are dynamically recalculated and scoped to parameter filters.

### 2. 📈 Player Insight Trends & Splits
*   **Objective**: Confirm the player insights endpoint returns strike rate trends for the last 10 matches and wicket splits by bowler style.
*   **Test Steps**:
    1.  Submit a `GET /api/v1/players/{playerProfile}/insights` request.
    2.  Assert status is `200`.
    3.  Verify the `strike_rate_trend` array contains up to 10 entries with opponent short names and match strike rates.
    4.  Verify the `wicket_splits` contain fast, spin, and other/unknown keys with correct cumulative dismissal counts.
*   **Expected Result**: Insights payload contains valid trends and bowler style breakdowns.

---

## 🛠️ Phase 10: Team Officer Assignments, Squad Classifications & H2H Verification

Use these guidelines to verify officer assignments, squad role grouping, and team-to-team Head-to-Head stats.

### 1. 👮 Team Officer Assignments & classified squad
*   **Objective**: Confirm designations (Captain, Vice-Captain, Wicketkeeper) update safely and return grouped inside playing roles.
*   **Test Steps**:
    1.  Send a `POST /api/v1/teams/{team}/designations` request with captain, vice-captain, and stumper ID mappings.
    2.  Verify the return status is `200` and flags are persisted.
    3.  Send a `GET /api/v1/teams/{team}/squad` request.
    4.  Verify the JSON lists are grouped under `batters`, `bowlers`, `wicketkeepers`, and `all_rounders` keys, with correctly set designation flags.
*   **Expected Result**: Squad lists map to correct categories and officers are flagged.

### 2. ⚔️ Team-to-Team Head-to-Head Comparisons
*   **Objective**: Verify H2H comparison stats retrieve win/loss totals and logs of past mutual encounters.
*   **Test Steps**:
    1.  Query `GET /api/v1/teams/compare?team1_id=X&team2_id=Y`.
    2.  Verify the return status is `200`.
    3.  Assert the response totals (wins, losses, ties) match database match logs.
*   **Expected Result**: Comparative H2H metrics load and match previous fixture outcomes.

---

## 🛠️ Phase 11: Standings Simulator & TTS Commentary Verification

Use these guidelines to verify standings qualification simulations and text-to-speech commentary.

### 1. 📢 Voice-Friendly TTS Commentary
*   **Objective**: Verify that every scored delivery automatically returns a speech-friendly text description.
*   **Test Steps**:
    1.  Score a new delivery (e.g. four or dot ball) by calling the store delivery endpoint or running offline sync.
    2.  Examine the broadcast event payload or delivery API response.
    3.  Assert the `tts_commentary` key exists and contains descriptive text (e.g., "Over 1.1: Bowler to Striker, FOUR runs!...").
*   **Expected Result**: TTS string accurately represents delivery notation details.

### 2. 🧮 Standings Simulation (Possibilities Calculator)
*   **Objective**: Ensure the simulator accurately calculates max/min possible points and qualification flags.
*   **Test Steps**:
    1.  Add scheduled fixtures and current team points.
    2.  Query `GET /api/v1/tournaments/{tournament}/standings/simulate`.
    3.  Verify the returned list identifies each team's status (`qualified`, `eliminated`, or `in_contention`).
*   **Expected Result**: Team qualification status matches mathematical logic constraints.

---

## 🛠️ Phase 12: Club / Organization Multi-Tenant, News Feed & Unified Search Verification

Use these guidelines to verify organizations multi-tenancy, news feed retrieval, and unified global search.

### 1. 🏢 Multi-Tenant Clubs & Seasons
*   **Objective**: Ensure tournaments are child resources of parent organizations and seasons.
*   **Test Steps**:
    1.  Create an organization and season. Link a tournament to them.
    2.  Query `GET /api/v1/organizations` and verify the list contains the organization with its tournament count.
    3.  Query `GET /api/v1/organizations/{organization}` and verify the response includes its seasons and nested public tournaments.
*   **Expected Result**: Organizations API loads full parent-child hierarchy properties.

### 2. 🔍 Unified Global Search
*   **Objective**: Confirm single-query indexes retrieve matching multi-type records.
*   **Test Steps**:
    1.  Perform a `GET /api/v1/search?q={keyword}` query.
    2.  Verify the response structure groups matching profiles under `players`, `teams`, `tournaments`, and `matches` keys.
*   **Expected Result**: Unified search returns multi-type list arrays.

### 3. 📰 Cricket News Feed
*   **Objective**: Verify news lists are paginated and details return correctly.
*   **Test Steps**:
    1.  Query `GET /api/v1/news` and verify the paginated article lists.
    2.  Query `GET /api/v1/news/{slug}` and verify the published content details.
*   **Expected Result**: News lists and detail responses return status 200 with creator bindings.

---

## 🛠️ Phase 13: Post-Match Corrections (Match Editing) Verification

Use these guidelines to verify post-match delivery corrections and automated scorecard recalculations.

### 1. ⚙️ Delivery Corrections & Recalculation verity
*   **Objective**: Confirm editing a historic delivery updates batsman/bowler stats and innings totals correctly.
*   **Test Steps**:
    1.  Identify a match with multiple scored deliveries. Note the striker's batting runs and the innings total score.
    2.  Send a `PATCH /api/v1/deliveries/{matchDelivery}` request updating `runs_off_bat` (e.g. changing 1 run to 4 runs).
    3.  Verify return status is `200`.
    4.  Verify that the striker's batting stats (runs and strike rate) and the innings score totals are automatically updated.
*   **Expected Result**: Recalculated scorecard totals match updated delivery metrics.

---

## 🛠️ Phase 14: Instant Team & Match Sync (Create Match Flow) Verification

Use these steps to verify that standalone (custom) matches and their teams reach the backend instantly and appear in the SuperAdmin control plane — even when created offline.

### 1. 🏏 Create Team from "Create Match" → SuperAdmin Teams
*   **Objective**: A team created inside the Create Match flow must appear in SuperAdmin → Teams immediately.
*   **Test Steps**:
    1.  Open the app → Homepage → **Create Match**.
    2.  Tap Team A → **Create Team** → enter name (+ optional location) → tap **CREATE TEAM**.
    3.  With internet ON: within a few seconds, open the SuperAdmin web panel → **Teams** tab. The new team must be listed (refresh if needed).
    4.  With internet OFF (airplane mode): repeat with another team name. The app must work normally (team selected locally). Then turn internet back on and wait ~15 seconds (or reopen the app) and check the SuperAdmin Teams tab again.
*   **Expected Result**: Team exists in the backend in both cases (instant when online, auto-synced when reconnected). The team also appears in the in-app team list on next launch (pulled from Room, not a hardcoded list).

### 2. 📅 Save Fixture → SuperAdmin Fixtures
*   **Objective**: A standalone saved match must appear in SuperAdmin → **Fixtures** (new tab) even before it starts.
*   **Test Steps**:
    1.  Create Match → pick both teams, set venue/date/time → tap **SAVE FIXTURE**.
    2.  Open SuperAdmin → **Fixtures** tab.
    3.  The fixture must show with the **Custom Match** badge, both team names, and the scheduled date.
*   **Expected Result**: Fixture row visible with status `Scheduled`. The two teams (if newly named) were auto-created globally on the backend.

### 3. ▶️ Start Match → SuperAdmin Matches
*   **Objective**: Starting a match creates the operational match record server-side.
*   **Test Steps**:
    1.  In the Create Match flow tap **START MATCH** (or start a saved fixture from the tournament matches tab).
    2.  Complete toss/lineup and begin scoring.
    3.  Open SuperAdmin → **Matches** tab.
*   **Expected Result**: The match appears (status `Scheduled`/`Live`) linked to the custom fixture, with both team names shown.

### 4. 🔁 Offline Queue & Idempotency
*   **Objective**: Offline-created data must sync once (no duplicates) when the connection returns.
*   **Test Steps**:
    1.  Airplane mode ON → create a match with two new teams.
    2.  Airplane mode OFF → let the app auto-sync (or tap sync).
    3.  Check the SuperAdmin Teams tab: each team must appear exactly once. Check Fixtures: the fixture must appear exactly once.
    4.  Re-open the app while online — confirm no duplicate fixtures/teams are pulled (dedup by server ID).
*   **Expected Result**: Exactly one copy of each team and fixture on the backend; local and server IDs stay mapped (no re-creation on next sync).

### 5. 🧯 Retry Hygiene (no infinite background retry)
*   **Objective**: A permanently invalid queued change must not loop the background worker forever.
*   **Test Steps**:
    1.  (Dev) Inspect WorkManager logs while a failing change is in the queue.
    2.  Confirm the worker gives up after 3 consecutive no-progress attempts (`Result.failure`) instead of endless exponential retries.
*   **Expected Result**: `SyncWorker` logs `Sync gave up after N attempts` and stops; foreground/manual sync still retries the queue.

