# STUMPS Mobile Application: Feature Architecture and Implementation Audit

## 1. Audit scope

This document evaluates the Android application as implemented in the current Kotlin source. It traces screens through ViewModels, repositories, Room, Retrofit, WorkManager, and the Laravel API. It does not treat the existence of a screen as proof that the feature works end-to-end.

Audit status meanings:

| Status | Meaning |
|---|---|
| **Implemented** | Real UI and data/business wiring exist and the code path is internally coherent. |
| **Partial** | A meaningful implementation exists, but part of the flow is missing, inconsistent, fragile, or locally simulated. |
| **UI-only** | The interface exists but uses hardcoded/sample state, toasts, or local-only mutations without the required backend operation. |
| **Broken/risk** | A concrete defect or contract mismatch is visible in code. |
| **Unverified** | Static code looks plausible, but no successful build/device/backend test proves it works. |

Overall conclusion: this is a large functional prototype with real offline persistence and significant backend integration. It is not yet production-ready. Several core flows are substantially implemented, but many presentation features remain sample-driven, important ID/synchronization paths are fragile, and the project has almost no automated Android test coverage.

## 2. Architecture

The app is a single Android application module using:

- Kotlin and Jetpack Compose Material 3;
- manually managed screen/navigation state in one `MainActivity`;
- Hilt dependency injection;
- Room with 19 entities and database version 15;
- Retrofit and OkHttp for Laravel `/api/v1` calls;
- WorkManager plus connectivity observation for background synchronization;
- encrypted preferences for tokens, with an insecure plain-preferences fallback;
- ViewModels and repositories for most active features.

The principal flow is:

```text
Compose screen
  -> ViewModel
  -> repository
  -> Room write/read
  -> PendingChange/PendingDelivery queue
  -> SyncManager or WorkManager
  -> Retrofit ApiService
  -> Laravel API
  -> returned server ID/state mapped back into Room
```

This is offline-first for tournaments, teams, fixtures, lineups, and scoring data. Global search and several analytics/profile reads are online-first or online-only.

### Architectural strengths

- UI, storage, and network concerns are separated better than in the original in-memory prototype.
- Room is observed with Flow in many list/detail features.
- Tokens are normally encrypted.
- Pending entity changes and pending deliveries are persisted, so process termination does not necessarily lose the outbound queue.
- Offline deliveries have UUID-based server deduplication.
- Draft state and match state have polling/revision concepts.
- Hilt provides repositories, DAOs, API service, workers, and ViewModels.

### Architectural weaknesses

- The whole application is one Gradle module and one very large `MainActivity` switch instead of Navigation Compose with typed destinations.
- Navigation arguments and back stack are only in ViewModel memory; they are not persisted in `SavedStateHandle` or represented by deep links.
- UI state is frequently stored with `remember`, so form contents can be lost on navigation/process recreation.
- Several screens calculate domain state locally while the backend is also server-authoritative, creating two scoring/statistics implementations.
- Multiple local representations exist for the same concepts: standard and admin teams/fixtures, String IDs, integer server IDs, hashes, names, and temporary IDs.
- Exceptions are silently swallowed in several synchronization and profile paths.
- API error bodies are often exposed as raw JSON strings or replaced with generic messages.
- No domain use-case layer coordinates complex workflows; ViewModels and repositories directly orchestrate many multi-step operations.

## 3. Build and test status

### Automated tests

Status: **Not meaningfully tested**.

Only the Android Studio template tests exist:

- `ExampleUnitTest.kt`
- `ExampleInstrumentedTest.kt`

There are no real tests for login, database migrations, mappings, repositories, sync retries, scoring rules, navigation, Compose screens, draft polling, or API contract parsing.

### Build verification

Status: **Unverified in this audit environment**.

`testDebugUnitTest lintDebug` could not run because Java was not available on PATH. Android Studio's bundled `java.exe` exists, but its bundled runtime is incomplete in this environment because `lib/jvm.cfg` is missing. This is an environment blocker, not proof of a source compilation failure.

### Historical runtime evidence

`crash.md` contains a real startup failure where Room detected a schema identity mismatch. Current code has since advanced the database to version 15 and uses `fallbackToDestructiveMigration()`. That likely avoids the same identity error, but it does so by deleting local data when a migration is missing.

## 4. Application startup, session, and navigation

### Session restoration

Status: **Partial**.

`NavigationViewModel` starts on Home whenever a cached token exists, otherwise Login. This permits offline reopening. However, it does not validate token expiry before showing Home. A revoked or expired token appears logged in until an API request fails.

The displayed Home username is held in `MainActivity` as `loggedInEmail`. On cold start with an existing session it is blank, causing the hardcoded fallback `Ahmed Ali` rather than the cached user name. This is a visible correctness bug.

### Navigation

Status: **Implemented but fragile**.

All declared destinations are routed in `MainActivity`, including authentication, Home, tournaments, teams, stages, fixtures, toss, lineup, scorer, match center, editor, profiles, search, and recent matches. Back navigation uses a custom mutable list.

Issues:

- no Navigation Compose graph, deep links, saved back stack, or process-death restoration;
- large destination objects carry complete squad lists, increasing state fragility;
- destination-specific `hiltViewModel()` calls inside branches can produce lifecycle/state surprises;
- screen changes are tightly coupled to one activity file;
- login/register clears the back stack correctly, but logout does not call the backend revoke endpoint.

## 5. Authentication and account features

### Login

Status: **Implemented, unverified end-to-end**.

The screen validates fields, invokes `AuthViewModel.login`, calls the Retrofit login endpoint, saves the token and user data, and supports cached-token offline fallback.

Issues:

- an incorrect password while a previous token is cached can still produce an offline-success state because all API failures fall back to the cached session;
- raw backend error JSON may reach the UI;
- there is no explicit token-expiry recovery/interceptor flow;
- the Home greeting uses email/fallback state rather than authoritative cached `userName`;
- device identity is a fixed request default rather than a managed session/device name.

### Registration

Status: **Implemented, backend security risk**.

Registration calls the real backend and stores the returned session. The backend currently assigns both `player` and `admin` roles to every registered account. Mobile behavior works with that contract, but the contract is unsafe unless open admin access is intentional.

### Logout

Status: **Partial**.

Logout clears local preferences and returns to Login. It does not call `/auth/logout`, so the bearer token remains valid on the server until revoked or expired.

### Player onboarding/profile editing

Status: **Partial**.

The onboarding screen has real form fields and a ViewModel/repository profile-update path. Text fields can be uploaded and successful profiles are cached in Room.

Issues:

- `AuthRepository.updateProfile` explicitly skips the photo part, so profile photo selection/upload is not implemented through this wrapper;
- phone exists in the backend contract but is not included in the shown repository request conversion;
- the callback in `MainActivity` only displays a toast; success/failure behavior must remain driven by the screen's own ViewModel;
- failures are often generic and several profile refresh exceptions are swallowed;
- there is no offline mutation queue for profile edits comparable to team/fixture changes.

## 6. Home dashboard

Status: **Partial/UI-heavy**.

The Home screen provides theme control, quick navigation, directory cards, drawer actions, recent/live match cards, profile entry, tournaments, teams, search, and logout.

The navigation callbacks are real. Much of the dashboard content and user presentation is still composed from supplied/default/sample values rather than one reactive Home repository aggregating authenticated user, tournaments, matches, and roles.

Issues:

- cached session greeting can show `Ahmed Ali`;
- role/permission-driven visibility is not consistently sourced from `auth/me`;
- bottom-navigation labels do not represent a persistent navigation graph;
- dashboard aggregates can disagree with Room/server state;
- some displayed match/tournament examples are presentation data rather than API results.

## 7. Global search

Status: **Implemented, online-only**.

The feature debounces input by 300 ms, waits for at least two characters, supports player/team/tournament/match filters, detects offline state, ignores stale responses, calls `/api/v1/search`, and maps results to the correct destination types.

Issues:

- no local fallback despite having Room data;
- no paging; results are capped by the requested limit;
- server error detail is discarded;
- tournament search uses slug while much of the local application uses String database IDs, so cross-feature ID assumptions must remain consistent;
- match search results can open Match Center without ensuring the required fixture/score cache is present.

## 8. Tournament directory and creation

### My Tournaments

Status: **Implemented with offline fallback**.

It loads the authenticated user's admin tournaments, maps them to domain/Room, and falls back to local data. Filtering and routing to setup or hub are implemented.

Risks:

- API and local records can coexist under different IDs if server-ID reconciliation fails;
- swallowed exceptions can make stale local data look current;
- “my tournaments” depends on the cached token and backend creator scoping.

### Create Tournament

Status: **Implemented but conflict-prone**.

The form covers identity, organizer/contact, city/venue, season/dates, ball type, competition structure, visibility, draft option, squad size, pick duration, and overs. It calls the backend first and writes the result locally. On API failure it creates a local tournament for offline operation.

Issues:

- any API failure is treated like offline creation, including authorization or validation errors; this can tell the user a tournament was created locally even though the server rejected it;
- reconciliation depends on temporary String IDs being replaced/mapped later;
- form state is screen-local `remember` state;
- image/logo/banner upload is not represented in the mobile creation flow;
- documentation and backend enum/value casing may differ (`Tennis Ball` vs backend values such as `tennis`).

### Tournament setup

Status: **Substantially implemented, needs integration testing**.

Setup loads teams, players, fixtures, draft configuration, and local cache; supports player approval/rejection, team creation/deletion/captain work, draft setup, and status changes. API results are cached for offline reuse.

Issues:

- multiple exceptions are silently ignored;
- a “Demo Mode” banner still contains an emoji, contrary to project UI rules;
- status readiness is inferred from local counts and may race pending sync;
- older and newer team relationships/IDs remain mixed;
- creator authorization can fail after local/offline creation until server reconciliation finishes.

## 9. Team features

### Add/create team

Status: **Create implemented; join-by-code not implemented**.

Creating a team saves locally, queues a change, attempts immediate sync, and makes it available offline. Tournament teams use the admin endpoint; standalone teams use `/custom/teams`.

The “Add by Code” UI exists, but `MainActivity` only displays “feature coming soon.” It performs no lookup or tournament attachment.

### My Teams

Status: **Partial**.

The screen can list locally available teams and navigate to details. Its “Add team” action only displays a toast and does not open a complete global-team workflow.

### Team detail

Status: **Partial**.

The five tabs exist: Home, Players, Matches, Tournaments, and Stats. Team and roster data are loaded from local repositories and some API squad data.

Working portions:

- team detail/navigation;
- local player roster display;
- manual local player creation/assignment;
- player profile navigation;
- owner/organizer-derived edit controls;
- local match/tournament summaries.

Issues:

- authorization is partly inferred by comparing organizer/profile names, which is not a secure or stable identity check;
- some statistics are calculated locally from available fixtures rather than backend analytics;
- hardcoded/sample fallbacks remain;
- player deletion/assignment synchronization needs real server tests;
- backend public squad is draft-pick based, while local team roster is `PlayerEntity.teamId` based, so they are not guaranteed to represent the same roster.

## 10. Player features

### Player profile

Status: **Partial but broadly connected**.

The profile has Overview, Stats, Matches, Teams, and Tournaments tabs. It can call the public player profile, stats, insights, match history, and team history endpoints and cache some data locally.

Issues:

- API failures are repeatedly swallowed, hiding whether shown data is stale or unavailable;
- profile source can be Room, API, or fallback model without a clear UI freshness indicator;
- Tournaments data is largely inferred through related local/team data;
- stats filters supported by the backend are not fully surfaced;
- the backend's public profile currently exposes phone information;
- several screens still contain sample/default cricket names and values.

### Manual/guest players

Status: **Implemented with ID-linking risk**.

Manual players are created in Room, immediately appended to the lineup/roster UI, queued, and sent to tournament or custom player endpoints. This supports offline addition.

Risk: the local player ID, server `PlayerProfile` ID, tournament registration ID, and match-player ID are different identifiers. Several flows pass a generic `player.id`, so correct translation must be verified for tournament lineups and scoring.

## 11. Stage and group features

### Create/manage stage

Status: **Implemented offline-first**.

The app has stage models, Room entity/DAO, mapper, repository, ViewModel, create screen, type selection, points rules, qualification settings, status update, counts, and delete operations. Changes can be queued for backend sync.

### Create Group shortcut

Status: **Not implemented from the main route**.

The Tournament Hub callback currently shows “Create Group coming soon.” Although stage creation exists and earlier walkthrough notes describe a group sheet, the active `MainActivity` callback does not execute it.

### Brackets and automatic progression

Status: **Not implemented**.

Stage types include knockout/qualifier/final concepts, but no complete bracket generation, qualification promotion, bye handling, or automatic stage progression engine exists in the mobile code.

## 12. Draft features

Status: **Substantially implemented online; limited offline behavior**.

Admin and captain draft screens call real endpoints. Implemented operations include state loading, polling, countdown display, start, pause, resume, extend, skip, undo, admin player selection, and captain pick.

Strengths:

- backend remains authoritative;
- state refreshes after mutations;
- polling and local timer jobs are separated;
- admin/captain paths use different endpoints.

Issues:

- draft operations require connectivity; there is no safe offline pick queue;
- UI uses emoji characters in admin buttons, contrary to the no-emoji rule;
- role selection is passed as `isAdmin` in navigation instead of always being derived from current permissions;
- polling lifecycle and network failure recovery need device tests;
- some IDs are converted with `.toInt()`, which can crash if a temporary/non-numeric local ID reaches the draft screen;
- no Android tests cover expiry, stale state, double-tap picks, or captain scope.

## 13. Fixtures and scheduling

### Tournament scheduling

Status: **Implemented offline-first, unverified**.

The schedule screen supports stage, home/away teams, date, time, venue, and match type. `FixtureRepository` saves an `AdminFixtureEntity`, queues synchronization, and can create the server fixture. It supports update, postpone, cancel, complete, and delete operations.

Issues:

- local validation cannot guarantee server collision/timezone/tournament lifecycle rules;
- date and time are assembled as strings and need timezone contract testing;
- duplicate local/server fixture reconciliation uses several heuristics;
- both `FixtureEntity` and `AdminFixtureEntity` represent overlapping concepts;
- IDs may be raw IDs, hashes, `srv_...` values, or local keys;
- status can be changed locally before the server accepts the transition.

### Standalone Create Match / Save Fixture

Status: **Partial; recently improved but still high risk**.

The form validates team, venue, date/time, overs, match type, ball type, and wickets. It loads known teams, can create global teams, saves a standalone fixture, queues it, and attempts immediate upload.

Concrete issues:

- when no teams exist it still falls back to hardcoded `BHH` and `NHH` names;
- `startFixture` saves and pushes the fixture but invokes `onReady()` without proving that the backend operational match was created or replacing the local fixture ID with the server match ID;
- navigation may pass a local fixture ID into toss/lineup/scoring endpoints that expect an operational match ID;
- network/validation failure is not surfaced strongly enough before proceeding;
- team location is collected but the custom-team backend accepts only name/short name, so location is not persisted remotely;
- immediate Super Admin visibility depends on successful queue execution and ID reconciliation, which remains untested.

## 14. Toss and lineup

### Toss

Status: **Local implementation complete; backend sync partial**.

The toss screen supports team selection, bat/bowl decision, animation, result, and local fixture persistence. Returning later can bypass toss when `toss_completed` data is stored.

Issues:

- local fixture status `toss_completed` is not one of the backend fixture statuses;
- backend toss applies to an operational match, not a fixture;
- the app must first resolve fixture ID to match ID; that connection is fragile in standalone flows;
- animation/result is local and should not be considered authoritative until the toss endpoint succeeds.

### Playing lineup

Status: **Partial with meaningful fixes**.

Squads load from Room, team IDs are resolved from IDs/names/hashes, the required count is derived from custom wickets or tournament squad size, existing players can be selected, and manual players are appended and queued immediately.

Issues:

- `squadsLoaded` prevents reload after IDs or server data change within the same ViewModel lifetime;
- fallback default squads are still present;
- player selection state is mainly UI state;
- lineup submission requires correct server match-player/profile identifiers, but local players may still have temporary IDs;
- exact required size is derived as `wickets + 1` for custom matches, which assumes maximum wickets always equals lineup size minus one;
- team-name resolution can select the wrong team when global names are duplicated;
- no automated test covers newly created offline player -> sync -> lineup submission.

## 15. Live scoring

Status: **Strong local scorer prototype; backend synchronization is incomplete/fragile**.

Implemented locally:

- runs 0-6;
- wides, no-balls, byes, leg-byes, and penalties;
- wickets and dismissal metadata;
- striker rotation and over completion;
- batter/bowler aggregates;
- fall of wickets and partnerships;
- innings/match completion;
- configurable balls per over, total overs, and wickets;
- local history/undo;
- Room persistence and pending-delivery queue;
- background batch upload.

Critical issues:

1. `setPlayerServerIds` is empty. The scorer exposes `strikerServerId`, `nonStrikerServerId`, and `bowlerServerId`, but the intended name-to-server-ID mapping is not implemented. Deliveries can therefore be queued with zero or incorrect player IDs and rejected by Laravel.
2. The scorer maintains its own cricket calculation engine while Laravel independently recalculates deliveries. Edge cases can diverge between local display and server truth.
3. When squads are absent, hardcoded player names are inserted.
4. Undo uses an in-memory state stack locally; server undo is a separate endpoint. The local action and remote ledger may diverge.
5. Local correction/editor behavior is not integrated with the actual server delivery IDs.
6. No conflict UI handles a stale server revision after another scorer writes.
7. Wicket legality, no-ball dismissal restrictions, bowler limits, and all unusual MCC cases are not proven equivalent to the backend.
8. `historyStack` is memory-only and lost on process death.
9. Offline batches are ordered by device timestamp, which can be unreliable if clock/timezone changes.
10. There are no rule-engine unit tests.

Until player server-ID mapping and end-to-end delivery tests pass, live server scoring should be classified as **not reliably working**, even though the local scorer UI works extensively.

## 16. Match Center and live viewing

Status: **Partial**.

Match Center provides Summary, Scorecard, Stats, and Super Stars tabs. `MatchCenterViewModel` can pre-sync, load Room state, request the public match state, poll server updates, and request MVP data.

Working/plausible portions:

- navigation and four-tab UI;
- local scorecard display;
- server state fetch and polling;
- Room cache update;
- MVP endpoint call;
- scorer controls conditioned by `isScorer`.

Issues:

- exceptions are swallowed, so stale data may be presented without explanation;
- `isScorer` comes from navigation rather than authoritative role/assignment verification;
- mixed local/server fixtures can make a match unresolvable;
- some statistics and fallback display values are locally calculated/sample-based;
- process recreation can lose squads carried in the destination;
- no polling/revision instrumentation tests exist.

## 17. Match editor and corrections

Status: **UI-only / not working against real matches**.

`MatchEditorScreen` initializes a hardcoded list of deliveries. Editing changes this in-memory list. “Rebuild” waits and shows a success toast. It does not load server deliveries, call `PATCH /api/v1/deliveries/{id}`, persist corrected entries to Room, or refresh authoritative match state.

This screen must not be presented as a functioning production correction tool.

## 18. Recent matches, scorecards, statistics, and Super Stars

Status: **Partial**.

Recent matches can be derived from local fixtures and open Match Center. Scorecard sections render innings, batters, bowlers, extras, wickets, and partnerships from available domain state. Super Stars can use the MVP API through Match Center.

Issues:

- completeness depends on successful mapping of the backend response into the large local `ScheduledFixture` model;
- some aggregate tabs compute values locally or show defaults;
- verified/manual data-source distinction is not consistently visible;
- no pagination strategy exists for long match history;
- there are no screenshot, accessibility, or scorecard mapping tests.

## 19. Standings and tournament statistics

Status: **Partial**.

Tournament standings call the real public standings endpoint and can fall back to locally calculated rows. Tournament statistics aggregate local fixture/player data for presentation.

Issues:

- local NRR calculation is simplified and may not match the backend's all-out/balls-faced rules;
- server standings currently return `position = null`, so ordering must be converted into position client-side;
- local standings shown before result approval can conflict with the official server table;
- advanced tournament leaderboards are not all backed by dedicated server endpoints.

## 20. Offline synchronization

Status: **Architecturally implemented; reliability unverified and error visibility weak**.

The app has:

- connectivity monitoring;
- persisted sync status and pending change entities;
- separate pending-delivery storage;
- manual/full/pre-live synchronization;
- background `SyncWorker`;
- entity dispatch for teams, players, fixtures, tournament status, and captains;
- custom routes when tournament ID is `0`;
- server pull for tournaments, teams, and fixtures;
- pending-count/status UI.

High-risk issues:

- several catch blocks discard exceptions completely;
- queue dependencies are implicit: a fixture can depend on two unsynced teams and a lineup can depend on unsynced players;
- retries do not expose structured permanent-versus-transient failure reasons;
- local/server ID replacement uses names, hashes, and composite keys;
- duplicate global names can merge unrelated teams because the backend uses `firstOrCreate(['name' => ...])`;
- destructive Room fallback can erase pending offline work on schema upgrades;
- no test verifies airplane mode -> create team/player/fixture -> reconnect -> exact once sync;
- HTTP success is sometimes treated as complete without validating the returned body/reconciled ID;
- WorkManager constraints, backoff, and app-start scheduling require device verification.

## 21. Room database and local data

Status: **Implemented with migration/data-loss risk**.

Room version 15 includes players, fixtures, innings, batting/bowling stats, wickets, partnerships, tournaments, teams, synchronization queues, admin resources, stages, user profiles, and player stats.

Issues:

- `fallbackToDestructiveMigration()` deletes all local data when a migration path is absent, including unsynced changes;
- seed players are inserted on a new database and can leak sample identities into real workflows;
- duplicated standard/admin entities require mirroring and can drift;
- exported schemas exist, but no migration tests are present;
- the historical Room identity crash proves schema evolution has already failed on a real device once.

## 22. Theme, responsive layout, accessibility, and UX

Status: **Partial**.

The app has a cohesive green theme, light/dark modes, semantic spacing helpers, screen-size configuration, adaptive font sizes, reusable cards, and sync-status presentation.

Issues:

- many hardcoded `sp` and `dp` values remain despite the font/responsiveness rules;
- several emoji characters remain in UI text;
- many icons have `contentDescription = null`, limiting accessibility;
- forms depend heavily on transient toast messages;
- strings remain hardcoded instead of localized resources;
- fixed dense layouts may clip on small devices and large accessibility font scales;
- no Compose UI/accessibility/screenshot tests exist.

## 23. Feature status matrix

| Feature | Status | Primary reason |
|---|---|---|
| Session-based startup | Partial | Cached token works; validity and display state can be stale. |
| Login | Implemented/unverified | Real API and secure token storage. |
| Registration | Implemented/risk | Real API; backend grants admin role. |
| Logout | Partial | Local token cleared; server token not revoked. |
| Profile onboarding/edit | Partial | Text fields sync; photo/phone/offline update gaps. |
| Home dashboard | Partial/UI-heavy | Navigation real; much content/default identity not authoritative. |
| Theme switching | Implemented | Material theme state works for current process. |
| Global search | Implemented/unverified | Real debounced API search; online only. |
| My tournaments | Implemented/unverified | API plus Room fallback. |
| Create tournament | Partial | Online and local fallback exist; failure classification/reconciliation weak. |
| Tournament setup | Partial | Broad real wiring; ID/status/error risks. |
| Add team | Implemented/unverified | Local queue and immediate push. |
| Join team by code | Not implemented | Toast only. |
| My teams | Partial | Listing/detail works; add action incomplete. |
| Team detail | Partial | Real local data; mixed roster/stat/authorization semantics. |
| Manual team player | Partial | Local save/sync exists; identifier contracts fragile. |
| Stage CRUD | Implemented/unverified | Room/repository/API queue path exists. |
| Create Group shortcut | Not implemented | Active callback says coming soon. |
| Knockout brackets/progression | Not implemented | Types only; no engine/UI flow. |
| Draft setup | Partial | Real setup path; needs contract tests. |
| Live draft admin | Implemented/unverified | Real endpoints and polling. |
| Live draft captain | Implemented/unverified | Real endpoint and captain state. |
| Tournament fixture scheduling | Implemented/unverified | Offline repository and API path. |
| Standalone Save Fixture | Partial | Persists/queues, but reconciliation unproven. |
| Standalone Start Match | Broken/risk | Proceeds without confirmed operational server match ID. |
| Toss | Partial | Local persistence works; server match mapping fragile. |
| Lineup selection | Partial | UI/local roster works; server identifiers uncertain. |
| Add player during lineup | Partial | Immediate local append and queue; server linkage unverified. |
| Local live scorer | Implemented prototype | Rich local rules and state. |
| Server live scoring | Broken/risk | Player server-ID mapping method is empty. |
| Offline delivery upload | Partial | UUID batch sync exists; real payload IDs may be invalid. |
| Undo scoring | Partial | Local stack and server void flow are not unified. |
| Match Center | Partial | Real API polling plus local fallback; silent failures/sample state. |
| Match Editor | UI-only | Hardcoded deliveries; no repository/API call. |
| Match MVP/Super Stars | Partial | API exists; fallback/data mapping unverified. |
| Player profile/history/stats | Partial | Real endpoints; stale/fallback/error visibility issues. |
| Standings | Partial | Real endpoint plus simplified local calculation. |
| Tournament statistics | Partial/UI-heavy | Mostly local aggregation, not full backend analytics. |
| Background sync | Partial/unverified | Architecture exists; no reliability tests and silent errors. |
| Responsive typography | Partial | Framework exists; hardcoded sizing remains. |
| Accessibility/localization | Weak | Missing descriptions, hardcoded strings, no tests. |

## 24. Priority repair plan

### P0: correctness and security blockers

1. Implement real local-player/name to server `match_players.id` mapping before any delivery upload.
2. Make standalone Start Match wait for fixture sync and operational match creation, then navigate using the returned match ID.
3. Preserve pending Room data with explicit migrations; remove destructive fallback for production.
4. Separate authentication failure from offline fallback so invalid credentials cannot silently reuse an old session.
5. Call server logout before clearing the local token, with an intentional offline revocation strategy.
6. Fix backend permissions for self-registration, custom resources, designations, and standalone match setup.

### P1: end-to-end functional completion

1. Replace Match Editor sample data with delivery loading, real IDs, correction API calls, audit reason, and refresh.
2. Create a deterministic dependency-aware sync graph: teams -> players -> fixtures -> operational match -> lineup/toss -> deliveries.
3. Consolidate IDs into explicit types/fields: local UUID, server team ID, profile ID, registration ID, match-player ID, fixture ID, and match ID.
4. Remove name/hash-based identity fallbacks.
5. Surface queue failures and distinguish retryable network errors from permanent 401/403/409/422 errors.
6. Derive scorer/admin capabilities from authenticated permissions, not navigation booleans or names.

### P2: product completeness

1. Implement join team by code and My Teams add flow.
2. Implement Create Group routing and real bracket/progression features, or remove unfinished controls.
3. Complete profile photo and phone editing.
4. Replace sample Home/statistics data with repository-backed aggregate state.
5. Standardize official versus local/unapproved standings presentation.
6. Remove sample players and fallback teams from production builds.

### P3: maintainability and quality

1. Adopt Navigation Compose and `SavedStateHandle`.
2. Introduce typed UI states and lifecycle-aware collection consistently.
3. Split the application into feature/core modules when stability permits.
4. Replace hardcoded strings/sizes/emojis and complete accessibility semantics.
5. Add structured logging and crash reporting without swallowing exceptions.

## 25. Required verification suite

Before calling the application production-ready, add and pass:

- Auth repository tests for success, 401, offline, expired token, and logout.
- Room migration tests for every released schema version.
- Mapper tests for every Laravel response.
- Sync tests for retries, ordering, duplicate UUIDs, dependent IDs, and permanent validation failures.
- Scoring unit tests for legal balls, extras, strike changes, dismissals, innings end, chase, undo, and reconciliation.
- ViewModel tests for every feature state.
- Compose navigation/form/accessibility tests.
- MockWebServer contract tests against representative Laravel payloads.
- Real-device airplane-mode recovery tests.
- End-to-end tournament flow and standalone match flow against a seeded Laravel test server.
- Two-device stale-revision/concurrent scorer tests.

## 26. Executable source-of-truth files

- Application/navigation: `MainActivity.kt`, `ui/navigation/Screen.kt`
- Authentication: `ui/feature/auth/`, `data/repository/AuthRepository.kt`
- Retrofit contract: `data/api/ApiService.kt`, `data/api/*Models.kt`
- Room: `data/db/CricketDatabase.kt`, `data/db/entity/`, `data/db/dao/`
- Entity sync: `data/sync/SyncManager.kt`, `data/sync/SyncWorker.kt`
- Delivery sync: `data/repository/DeliverySyncRepository.kt`
- Tournament features: `ui/feature/tournament/`
- Team features: `ui/feature/team/`
- Match features: `ui/feature/match/`
- Player features: `ui/feature/player/`
- Local scoring engine: `ui/feature/match/scorer/LiveScorerViewModel.kt`

This audit should be updated whenever a feature moves from visible UI to a verified end-to-end implementation. “Implemented” must mean the complete screen-to-server-to-local-reconciliation path works and is tested, not simply that a Composable exists.
