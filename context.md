# Cricket Draft OS - Development Context & Progress Summary

## User's Goal & Exact Requirement
The user wants seamless and immediate data persistence for both Teams and Players across all match types, with a strong offline-sync capability.
Specifically:
1. **Immediate Team Creation in "Create Match":** When creating a match, if the user selects the "Create Team" option, adds a name, and clicks the button, the team must immediately be sent to the backend and show up in the SuperAdmin "Teams" tab instantly—whether the match is part of a tournament or completely standalone, and whether the match has started yet or not.
2. **Immediate Player Registration in "Lineup":** When setting up a playing XI lineup, if the user adds a player manually (e.g., clicks "add player", types a name), that single player must immediately be added to the backend—not just saved locally and delayed until the entire lineup is submitted.
3. **Offline Queue:** Both of the above actions must check for internet availability. If the device is offline, the actions must be put in a queue. As soon as the internet returns, the queued teams and players must be synced to the backend automatically.

This document outlines the architectural changes, bug fixes, and feature additions implemented so far to resolve the "Custom Match / Global Teams" bugs and introduce instant offline-syncing capabilities.

## 1. Database Restructuring (Global Teams)
Previously, the database tightly coupled every Team to a specific Tournament via a \tournament_id column in the \teams table. This caused massive errors (HTTP 500s) when creating "Custom Matches" because Custom Matches have no associated tournament (\tournament_id = null).
- **Pivot Table Created:** We created a \tournament_teams pivot table to establish a Many-to-Many relationship.
- **Data Migration:** Safely migrated existing \tournament_ids to the new pivot table.
- **Column Dropped:** Removed \tournament_id from the \teams table. Teams are now **100% global entities** and can participate in any tournament or standalone match.

## 2. Backend (Laravel) Enhancements
- **Model Updates:** Updated relationships in Team.php and Tournament.php to use \belongsToMany.
- **MatchService:** Refactored the core match creation logic (MatchService.php) so it no longer crashes when processing a match that lacks a tournament.
- **Custom Endpoints Created:**
  - Added /api/v1/custom/teams and /api/v1/custom/players to strictly handle standalone creations without relying on route-model binding for Tournament.
- **AdminMatchController Workarounds:** 
  - The mobile app sends \tournament_id = 0 for custom matches. We added explicit custom routes (\dmin/tournaments/0/matches/...) to gracefully handle Playing XI, Lineup Approvals, and Tosses for custom matches.

## 3. Mobile App (Android/Kotlin) Offline Sync Fixes
The user requested that manually adding Teams or Players should hit the backend **immediately**, but queue gracefully if the internet is disconnected.
- **ApiService.kt:** Hooked up the new createCustomTeam and createCustomPlayer backend routes.
- **SyncManager.kt:** Updated the pushTeamChange and pushPlayerChange functions. If the app detects \tournamentId == ""0"", it correctly fires the custom endpoints instead of crashing on the admin endpoints.
- **LineupViewModel.kt:** Injected SyncManager. When a user manually registers a player in the lineup, it immediately saves locally **and** fires syncManager.pushPendingChanges(), ensuring instant backend delivery.
- **TournamentSetupViewModel.kt:** Injected SyncManager. Creating a team immediately queues the sync action and pushes to the backend.

## 4. Current Issues Pending Investigation
Despite the robust sync logic, the user reported two ongoing issues:
1. **Custom Teams not appearing immediately when created from "Create Match":**
   - *Hypothesis:* The mobile app likely uses a different ViewModel (e.g., CreateMatchViewModel or MatchCenterViewModel) to handle team creation during the standalone match flow, meaning our SyncManager injection in TournamentSetupViewModel is being bypassed.
2. **Created Custom Matches not showing in SuperAdmin:**
   - *Hypothesis:* The SyncManager's pushFixtureChange or createMatch network requests are likely failing silently or getting stuck in the PendingChangeEntity queue due to an unexpected payload format, or the SuperAdmin views are filtering out matches that lack a \tournament_id.

## 5. RESOLVED — Root Causes Found & Fixed (September 9, 2026)
Deep trace of both codebases confirmed the hypotheses above and more. Fixes applied:
1. **Create Match "Create Team" was pure UI state** — the dialog only did `existingTeams.add(...)` on a hardcoded demo list (`BHH`, `NHH`, `Ali Panthers`...) with no Room save, no API call, no sync queue. It now creates a real global team (`tournamentId "0"` → `POST /api/v1/custom/teams`) via `AdminLocalRepository.createTeam` + `SyncManager.pushPendingChanges()` inside `CreateMatchViewModel`. The team list on the screen is now loaded from Room (`getAllTeamNames()`) instead of hardcoded values.
2. **Create Match never synced the fixture** — `CreateMatchViewModel.saveFixture` wrote only to the local `fixtures` table and never queued a sync change. `FixtureRepository.saveFixtureWithSync()` now saves an `AdminFixtureEntity` with `tournamentId "0"` and queues an `admin_fixture` create payload carrying team NAMES (backend auto-creates missing global teams) and an ISO date (`toIsoDate()` converts `"19 Aug 2026"` → `"2026-08-19"`).
3. **Silent drops in SyncManager** — `pushSingleChange` matched only `"admin_team"` / `"admin_player"`; queued `"team"` / `"player"` / `"tournament"` / `"stage"` items fell into `else -> true` and were marked completed with NO API call (fake success, data loss). Dispatch now routes `"team"` and `"player"` to the real handlers; `"tournament"` creates are no longer queued at all (the tournament was already created by a direct API call, so queueing pushed duplicates).
4. **Fixture payload defects** — payloads carried local string team IDs (→ `toIntOrNull() ?: 0` with no name → backend `abort(422, 'Invalid home team')`) and human-format dates failing backend `date` validation. Now: team names in payload, ISO dates via `normalizeScheduledAt()` (SyncManager) / `toIsoDate()` (FixtureRepository), status updates use the server ID and map mobile `live` → backend `in_progress`, and standalone fixtures route to the new `custom/fixtures/{id}/status` and `DELETE custom/fixtures/{id}` endpoints.
5. **SuperAdmin had no Fixtures list** — scheduled custom fixtures were invisible by design (`super-admin.matches.index` shows only operational matches). Added `/super-admin/fixtures` (SuperAdmin\FixtureController + view + nav links) listing ALL fixtures including `tournament_id = null` ones with a "Custom Match" badge.
6. **SyncWorker infinite retry** — permanently-failing queued changes caused endless exponential retries (the logcat in `cricket-draft-mobile/crash.md` showing repeated `Worker result RETRY`). The worker now returns `Result.failure` after 3 consecutive no-progress attempts; partial progress still counts as success.
7. **Start Match creates the operational match server-side** — `MainViewModel.startMatch` pushes the pending fixture then calls `POST /api/v1/custom/fixtures/{id}/create-match` (`createOperationalMatchIfNeeded`), so started standalone matches appear in the SuperAdmin Matches tab.
8. **Feature tests** — `tests/Feature/Api/V1/CustomMatchSyncTest.php` covers custom team creation + SuperAdmin visibility, name idempotency, fixture auto-team-creation, fixtures-tab visibility, role rejection, operational-match creation + duplicate rejection, status transitions, and delete-before-match.
9. **Manual QA checklist** — `cricket-draft-mobile/guide.md` Phase 14 (offline airplane test, dedup check, retry-hygiene check).
10. **✅ CONFIRMED WORKING (user-verified): custom teams now appear in the SuperAdmin Teams tab.**
11. **Lineup "Add Player → New Player" fixed (September 9, 2026):** The dialog submitted correctly, but the new player vanished because `loadSquadsForMatch` collects Room's `players` table as a Flow — the `registerPlayer` insert RE-fired the collector, which REPLACED the squad list with the DB contents *before* the ViewModel appended the new player (a race). Repairs in `LineupViewModel`: (a) the new player is seeded into the squad list BEFORE the sync/push so any re-fired collect keeps it, (b) `loadSquadsForMatch` is guarded against double-launch (navigation fired two `LaunchedEffect` launches, the second resetting state and re-collecting mid-registration), and (c) the `"player"` sync payload now always carries the real resolved `teamId`, so the device-side roster link survives and `pushPlayerChange` forwards it (server currently ignores it; future-proof). Verified the dialog's role default is already `"Batter"` (never blank) and that `CustomPlayerController` accepts the payload (`validate()` ignores the extra `team_id` key).
12. **Lineup "Add Player" — REAL root cause found & fixed (September 9, 2026, second pass):** The race fix above was necessary but NOT sufficient. The squad query key itself was broken for custom matches: `saveFixtureWithSync` stored team NAMES on `AdminFixtureEntity` with BLANK `homeTeamId`/`awayTeamId`, so the lineup flow called `resolveOriginalTeamId("")` → `""` and saved new players with `teamId = null` while the squad collector queried `WHERE teamId = ''` (`null ≠ ''`). Every players-table re-emission — including the insert that registered the player — therefore wiped it from the UI, and `registerNewPlayer` stored `teamId = null` (via `takeIf { isNotBlank() }`). Fixes: (a) `LineupViewModel` rewritten — squads load ONCE per match via one-shot reads (`PlayerRepository.getPlayersByTeamOnce`), no live Room collectors, ViewModel is the only writer so the race is structurally impossible; team IDs resolve BY NAME via new `TeamRepository.resolveTeamIdByName`; (b) `FixtureRepository.saveFixtureWithSync` resolves and stores real local team IDs on the fixture entity (sync payload still sends names + id 0 because the backend rejects local numeric IDs with 422); (c) `SyncManager.pushSingleChange` gained a stale-queue guard — `create` changes for entities already synced by direct-push paths are marked complete instead of re-running (prevents duplicate backend fixtures/teams).
