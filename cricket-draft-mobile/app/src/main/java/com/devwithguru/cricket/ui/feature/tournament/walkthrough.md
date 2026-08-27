# Walkthrough - Tournament System Bug Fixes

We have successfully resolved all major logical, data loading, navigation, and sync bugs within the Tournament module.

## Changes Made

### 1. Data Loading Gaps in Tournament Setup
- **File:** [TournamentSetupViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentSetupViewModel.kt)
  - Implemented team and player caching from the API `onSuccess` block. Synced data is now written directly to local Room entities so it is available offline immediately.

### 2. Team/Player Deletion Mismatch Fix
- **File:** [AdminLocalRepository.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/repository/AdminLocalRepository.kt)
  - Added methods `saveTeam(AdminTeamEntity)` and `savePlayer(AdminPlayerEntity)`.
  - Updated `deleteTeam`, `removePlayer`, `approvePlayer`, and `rejectPlayer` to resolve records using either local primary keys, server IDs, or hash code fallback.
- **File:** [AdminPlayerDao.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/db/dao/AdminPlayerDao.kt)
  - Added the query `findByIdOrServerId(idOrServerId, serverId)` to support looking up entities by both ID fields.

### 3. Synchronized Team Cache
- **File:** [TeamRepository.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/repository/TeamRepository.kt)
  - Updated `saveTeam` to automatically mirror standard `Team` entities into `admin_teams` in the local Room cache.

### 4. Unlocked Tournament Status Transition
- **File:** [TournamentSetupScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentSetupScreen.kt)
  - Modified the action buttons logic in `StatusTabContent` to support tournaments without a draft. Users can now mark setup ready once at least one fixture is scheduled.
- **File:** [TournamentSetupViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentSetupViewModel.kt)
  - Added updates to the local Room database's `TournamentEntity` when the user updates the status.

### 5. Stage Creation Route Mapping
- **File:** [StageViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/StageViewModel.kt)
  - Added a `setTournamentId(id)` setter to update the internal tournament state and refresh stages.
- **File:** [MainActivity.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/MainActivity.kt)
  - Bound the navigation `tournamentId` parameter inside a `LaunchedEffect` for the `CreateStage` screen destination.

### 6. Tournament Creation Double Submission/Navigation Reset
- **File:** [TournamentViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentViewModel.kt)
  - Updated `clearError()` to reset both `_createdTournamentId` and `_createError` to `null`. This prevents the creation form from auto-submitting/auto-navigating on screen relaunch.

### 8. Offline Scoring Cache Data Loss
- **File:** [FixtureMapper.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/mapper/FixtureMapper.kt)
  - Added `toScheduledDomain(gson)` and `toEntity(gson)` extension mappers for serialization of full innings, batsmanship, bowling, partnership, extras, and boundary tables into Room's `FixtureEntity`.
- **File:** [FixtureRepository.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/repository/FixtureRepository.kt)
  - Injected standard `FixtureDao` alongside the existing `AdminFixtureDao`.
  - Refactored `getScheduledFixtureById`, `saveFixture`, and `updateFixture` to write to the `fixtures` table using `FixtureDao` when scoring matches offline.

### 9. Operationalized 'Create Group' Sheet
- **File:** [TournamentHubScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentHubScreen.kt)
  - Wired the "Create Group" trigger inside the tournament hub home tab.
  - Linked the modal bottom sheet's submission button to create a `Stage` of type `POINTS_TABLE` for the group.

### 10. Team Detail Edit Roster Authorization
- **File:** [TeamViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/team/TeamViewModel.kt)
  - Updated the team detail permission check to authorize the tournament creator/organizer (matching `organizerName` against the logged-in user profile name) to edit rosters and add players.

### 11. Toss Lineup Player Registration Mismatch & Team Link
- **File:** [LineupViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/viewmodels/LineupViewModel.kt)
  - Stored `homeTeamId` and `awayTeamId` when loading squads for a match.
  - Updated `registerNewPlayer` to pass the correct target `teamId` when saving the new player to Room, and added an `onComplete(playerId)` callback.
- **File:** [TossLineupScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/match/toss/TossLineupScreen.kt)
  - Refactored the `AddPlayerDialog`'s new player registration callback block to use the ViewModel's asynchronous callback. This resolved race conditions where temporary `"p_..."` IDs were used, aligning the selection list immediately with the correct database-synced player record.

### 12. Toss Lineup Team ID & Match ID Hashcode Resolution
- **File:** [LineupViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/viewmodels/LineupViewModel.kt)
  - Injected `TeamRepository`.
  - Resolved `adminFixture.homeTeamId` and `adminFixture.awayTeamId` (which are saved in fixtures as Integer hashcodes/server IDs) back to their original local Room database String keys (e.g. `"at_t_123456"`) using `teamRepository.resolveOriginalTeamId(...)` before querying player rosters. This guarantees that existing players added in the Team Profile screen load perfectly on the Toss Lineup screen.
- **File:** [AdminFixtureDao.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/db/dao/AdminFixtureDao.kt)
  - Added the query `getAllAdminFixtures()` to support fetching all fixtures globally.
- **File:** [FixtureRepository.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/repository/FixtureRepository.kt)
  - Added `resolveOriginalFixtureId(fixtureIdStr)` to look up fixtures by both original String ID and hashcode.
  - Updated `getAdminFixtureById` and `getScheduledFixtureById` to resolve match IDs dynamically, solving cases where matches fail to load or show empty rosters.
- **File:** [MainViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/viewmodels/MainViewModel.kt)
  - Updated `getFixture(matchId)` to compare the cached fixture against both its original String ID and hashcode string.
- **File:** [TournamentViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentViewModel.kt)
  - Updated `AdminTeamEntity.toTournamentTeamData` mapping to parse the String ID directly to an Integer (`id.toIntOrNull()`) when created locally. This bypasses hashcode conversion completely for numeric IDs, ensuring clean, unified ID routing.

### 13. Dynamic Lineup Roster Size Validation
- **File:** [LineupViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/viewmodels/LineupViewModel.kt)
  - Injected `TournamentRepository`.
  - Exposed `squadSize` flow to load `tournament.squadSize` dynamically for the current match.
- **File:** [TossLineupScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/match/toss/TossLineupScreen.kt)
  - Read `squadSize` value from the ViewModel.
  - Adapted the auto-selection triggers to dynamically select `squadSize` players (e.g. 11, 6, etc.) instead of hardcoded 11.
  - Updated match start validations to check `homeCount == squadSize && awayCount == squadSize` and guide the user on the required player counts.

### 14. Universal Squad Size Input
- **File:** [CreateTournamentScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/CreateTournamentScreen.kt)
  - Moved the squadSize input field out of the conditional `hasDraft` block and placed it directly inside the "Game Format" card. This enables users to define squad sizes for all tournaments, regardless of whether a draft is enabled.

### 15. Keyboard Overlap & Layout Adjustment
- **Files:** [CreateTournamentScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/CreateTournamentScreen.kt), [CreateStageScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/CreateStageScreen.kt), [ScheduleMatchScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/ScheduleMatchScreen.kt), [AddTeamScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/AddTeamScreen.kt)
  - Added `.imePadding()` modifiers to the root scrollable Columns. This forces the layout height to automatically adjust based on the soft keyboard status, ensuring that all input fields remain accessible and scrollable.

### 16. Toss State Persistence, Bypass & Squad Limit Validation
- **Files:** [Fixture.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/domain/model/Fixture.kt), [AdminFixtureEntity.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/db/entity/AdminFixtureEntity.kt), [FixtureEntity.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/db/entity/FixtureEntity.kt), [ScheduledFixture.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/domain/model/ScheduledFixture.kt)
  - Added `tossWinner` and `tossDecision` fields to standard and admin fixture records.
- **File:** [FixtureMapper.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/mapper/FixtureMapper.kt)
  - Mapped the new toss fields across all entity/domain conversion methods.
- **File:** [CricketDatabase.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/db/CricketDatabase.kt)
  - Bumped database version to `11` to trigger Room's automatic destructive schema recreation.
- **File:** [FixtureRepository.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/repository/FixtureRepository.kt)
  - Added `saveTossDetails(fixtureId, tossWinner, tossDecision)` to update status to `"toss_completed"` and save toss properties in Room.
- **File:** [MainViewModel.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/viewmodels/MainViewModel.kt)
  - Exposed `saveTossDetails(...)` to database threads.
- **File:** [TournamentMatchesTab.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/TournamentMatchesTab.kt)
  - Dynamically labeled action buttons: show `"Lineup"` if status is `"toss_completed"` instead of `"Start"`.
  - Pass the status and toss properties in `onStartMatch` parameters.
- **File:** [MainActivity.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/MainActivity.kt)
  - Save toss details on `TossScreen`'s `onTossComplete` trigger.
  - In `onStartMatch` callback, check if the status is `"toss_completed"` and has valid toss values; if so, skip `Screen.Toss` and route the user directly to `Screen.TossLineup` destination.
- **File:** [CreateTournamentScreen.kt](file:///c:/Users/Muhammad%20Aliyan/Downloads/cricket-draft-source/cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/ui/feature/tournament/CreateTournamentScreen.kt)
  - Added a validation rule requiring "Players per Team" (squad size) to be at least 2 when creating a tournament.

---

## Verification & Testing

1. **Verification of Setup/Registration Transition:**
   - Create or open a tournament with draft disabled. Schedule a match.
   - Go to the "Status" tab and verify the "Mark Ready" button is clickable.
2. **Offline Mode Test:**
   - Put your device in Airplane mode. Re-open the tournament setup screen. Verify that the list of teams and players is fully loaded from the local database.
3. **Deletions Verification:**
   - Click the "Delete" button next to any synced team/player in the setup screen and confirm it is successfully removed from the local lists.
4. **Scoring Data Verification:**
   - Play a match offline. Close the match scorer. Tapping on the completed match in the tournament matches tab now correctly displays the historical innings scorecards and boundary stats compiled offline.
5. **Lineup Roster Registration Test:**
   - On the `TossLineupScreen`, register a new player on the fly for either team.
   - Proceed to start the match and verify that the newly added player appears in the active batsman/bowler selection options with correct statistics.
   - After completing the match, navigate to the Team detail/players profile and verify that the player is correctly linked to the team and saved in the permanent team roster in Room.
6. **Existing Players Lineup Verification:**
   - Add players to a team's profile via the Team Detail screen.
   - Go back, schedule a match for that team, and click start.
   - Verify that the players previously added to the team profile are automatically listed and checked on the Toss Lineup screen.
7. **Lineup Size Validation Test:**
   - Edit the tournament's `squadSize` setting (e.g., set to 6).
   - Go to the start match screen and verify that the lineup screen auto-selects 6 players and enforces exactly 6 selections to activate the "START MATCH" button.
8. **Keyboard Scroll Test:**
   - Relaunch the Create Tournament form, focus on any input text field, and verify that the page adjusts its layout size to scroll all sections above the keyboard.
9. **Toss Save & Bypass Verification:**
   - Start a scheduled match. Choose toss winner and decision.
   - On the lineup screen, click back.
   - Go to matches list: verify the button now displays `"Lineup"`.
   - Click `"Lineup"` and confirm it bypasses the toss screen and lands directly on the Toss Lineup screen with the previously chosen toss selections preserved.
10. **Create Tournament Squad Validation:**
    - Type `1` in "Players per Team". Click "CREATE TOURNAMENT".
    - Verify that the error message `"Players per Team must be at least 2"` is displayed and submission is blocked.
