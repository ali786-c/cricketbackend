# Cricket Draft Backend API System Guide

## 1. Purpose and source of truth

This document explains the API that is actually registered in `routes/api.php` and implemented by the current Laravel code. It is intended for backend developers, Android developers, testers, and future maintainers.

The API base path is:

```text
/api/v1
```

The backend is server-authoritative. Controllers validate and scope requests, while domain services perform important transactional work such as draft mutations, fixture transitions, match setup, scoring, result approval, and standings rebuilds. Android's Room database is an offline cache and outbound queue; it is not the final authority when connectivity is available.

## 2. Request lifecycle

```text
Android/web client
    -> global API throttle
    -> optional Sanctum authentication
    -> optional role/permission middleware
    -> Laravel route-model binding
    -> controller validation and ownership checks
    -> transactional domain service
    -> Eloquent/MySQL persistence and audit records
    -> JSON response
    -> mobile Room reconciliation
```

Important layers:

1. `throttle:api` applies to every v1 endpoint.
2. Login and registration have the additional `throttle:api-auth` limiter.
3. Protected endpoints require `Authorization: Bearer <token>` through `auth:sanctum`.
4. Administrative routes normally require a Spatie permission such as `manage tournaments` or `control draft`.
5. Most tournament mutations additionally verify that `tournament.creator_id` equals the authenticated user ID.
6. Services use database transactions and, for sensitive state, `lockForUpdate()`.
7. Laravel validation failures return HTTP 422. Authentication normally returns 401, authorization 403, missing or deliberately hidden resources 404, and invalid lifecycle state may return 409 or 422.

## 3. Common conventions

### Authentication

Login and registration issue Laravel Sanctum personal access tokens. A token receives abilities derived from the user's roles, `profile:read`, and optionally `client:<slug>`. Current authorization is primarily enforced through route middleware and Spatie permissions, rather than token-ability middleware.

An optional `client_slug` identifies the consuming application. If supplied, it must reference an active `api_clients` record. If omitted, the current code still issues a token.

### JSON envelopes

Most single-resource responses use:

```json
{
  "data": {},
  "message": "Optional human-readable result"
}
```

Most collections use `data` as an array. Paginated Eloquent results are sometimes nested under `data`, so clients may receive `data.data`, `data.current_page`, and related pagination fields. `GET /news` is an exception: it returns Laravel's paginator object at the top level.

### Route identifiers

Parameters such as `{tournament}`, `{fixture}`, `{match}`, `{team}`, and `{playerProfile}` use implicit Eloquent route-model binding. They are database IDs unless a model explicitly changes its route key. News detail is implemented as a slug lookup even though the route placeholder is named `{newsArticle}`.

### Revisions and concurrency

Drafts and matches carry integer revisions. A successful state mutation increments the corresponding revision. Live scoring accepts `expected_revision`; when it does not match the database revision, the scoring service rejects the stale request with a 422 validation error. Offline delivery batches are deduplicated using `local_uuid`.

### Visibility

Tournament public endpoints call `publiclyVisibleNow()`. Non-public or unavailable tournaments deliberately return 404. The public list additionally limits status to `registration`, `ready`, `live`, or `completed`.

## 4. Authentication endpoints

| Method and path | Access | Function |
|---|---|---|
| `POST /auth/register` | Public, auth throttle | Creates a user, assigns both `player` and `admin` roles, optionally validates an API client, and issues a Sanctum token. Requires `name`, unique `email`, password of at least 8 characters, and `device_name`. Returns 201. |
| `POST /auth/login` | Public, auth throttle | Verifies email/password, optionally validates active `client_slug`, updates the client's last-seen time, and issues a new device token. |
| `GET /auth/me` | Sanctum | Returns user ID, name, email, role names, permission names, and a compact player profile. |
| `POST /auth/logout` | Sanctum | Revokes the current personal access token. |
| `POST /auth/logout-all` | Sanctum | Deletes every API token owned by the authenticated user. |

Important implementation detail: public self-registration currently grants the new account both `player` and `admin`. This is security-sensitive and should be confirmed as an intentional product rule.

## 5. Public tournament, team, and match discovery

| Method and path | Function |
|---|---|
| `GET /tournaments` | Lists publicly visible tournaments in allowed statuses with their format/rule summary. |
| `GET /tournaments/{tournament}` | Returns a public tournament summary and fixture count. Hidden tournaments return 404. |
| `GET /tournaments/{tournament}/teams` | Returns active tournament teams, codes, logos, creator IDs, and match-squad counts. |
| `GET /tournaments/{tournament}/players` | Returns approved tournament registrations with redacted profile fields. |
| `GET /tournaments/{tournament}/fixtures` | Returns fixtures, teams, schedule, venue, fixture status, operational match ID, and match status. |
| `GET /tournaments/{tournament}/standings` | Returns wins, losses, ties, no-results, points, and NRR ordered by points then NRR. The current `position` field is always `null`. |
| `GET /matches/{match}/state` | Returns the public live match-center state assembled from the match, current innings, playing XI, deliveries, batting/bowling tables, toss, and revision. |
| `GET /tournaments/{tournament}/sync?revision=N` | Returns one tournament synchronization snapshot: server time, tournament, draft, stages, fixtures, live/recent matches, and standings. `changed` compares the supplied revision with the maximum draft/match revision. |
| `GET /teams/{team}/squad` | Returns the drafted squad grouped by playing role and includes captain, vice-captain, and wicketkeeper designations. |

The tournament sync endpoint does not emit a delta. Even when `changed` is false, the current controller still builds and returns the full payload; the flag only tells the client whether reconciliation is necessary.

## 6. Public analytics, player history, search, news, and organizations

| Method and path | Inputs and function |
|---|---|
| `GET /players/{playerProfile}` | Returns the public profile payload, including contact phone and bio in the current implementation. |
| `GET /players/{playerProfile}/stats` | Optional `year`, `format`, `ball_type`, and `data_source` filters; returns derived career batting, bowling, and fielding statistics. |
| `GET /players/{playerProfile}/insights` | Returns higher-level player performance insights calculated by `PlayerProfileStatsService`. |
| `GET /players/{playerProfile}/matches` | Traverses tournament registrations to match-player snapshots and returns opponent, venue, runs, wickets, overs, result, and format. |
| `GET /players/{playerProfile}/teams` | Returns approved team/tournament history and match counts. |
| `GET /tournaments/{tournament}/players/compare` | Requires `player1_id` and `player2_id`; compares tournament performance. |
| `GET /teams/compare` | Requires distinct `team1_id` and `team2_id`; returns head-to-head aggregates and history. |
| `GET /tournaments/{tournament}/standings/simulate` | Estimates qualification status from standings and remaining scheduled fixtures. |
| `POST /teams/{team}/designations` | Sanctum only. Sets optional captain, vice-captain, and wicketkeeper tournament-player IDs by resetting and updating draft-pick flags. |
| `GET /search` | Requires `q` of 2-100 characters. Supports comma-separated `type`, `limit`, `city`, `playing_role`, `tournament_id`, and `status`. Searches players, teams, tournaments, and matches. |
| `GET /search/lookup/{code}` | Resolves team/player unique codes or tournament identifiers; returns a custom 404 body if not found. |
| `GET /news` | Returns paginated published articles, 15 per page, with creator name. |
| `GET /news/{newsArticle}` | Returns one published article by slug; despite the placeholder name, the controller receives the string and performs its own slug query. |
| `GET /organizations` | Lists active organizations with tournament counts. |
| `GET /organizations/{organization}` | Returns an organization, its seasons, and only its public tournaments. |

## 7. Authenticated player profile and registration

| Method and path | Function |
|---|---|
| `GET /profile` | Returns the authenticated user's complete player profile or `null`. |
| `POST|PATCH /profile` | Creates or updates the profile. `full_name` is required even for PATCH. Supports phone, city, role, batting/bowling styles, bio, and a maximum 5 MB image. Images are stored on the public disk. |
| `GET /tournaments/{tournament}/registration` | Returns the authenticated player's registration status for the tournament, or `null`. |
| `POST /tournaments/{tournament}/registration` | Requires an existing player profile and a visible tournament in `registration` or `ready`. Creates or resets the registration to `pending`. Limited to 20 requests/minute. |

## 8. Standalone/custom match endpoints

These routes support matches with `tournament_id = null`. They are authenticated but do not currently have role/permission or creator-ownership middleware.

| Method and path | Function |
|---|---|
| `POST /custom/teams` | Creates or reuses a global team by exact name. Accepts `name` and optional `short_name`; records the current user as creator only on creation. |
| `POST /custom/players` | Creates an active guest `PlayerProfile` without a user account. Accepts name, role, and city. |
| `POST /custom/fixtures` | Creates a fixture with no tournament through `FixtureService`. Team ID `0` plus a supplied team name auto-creates a global team. |
| `PUT /custom/fixtures/{fixture}` | Updates a standalone fixture through the same fixture service and conflict rules. |
| `POST /custom/fixtures/{fixture}/status` | Applies a validated fixture state transition. |
| `POST /custom/fixtures/{fixture}/create-match` | Converts the fixture into an operational match and moves the fixture to `in_progress`. |
| `DELETE /custom/fixtures/{fixture}` | Deletes a fixture only if it has no operational match. |

The shared fixture validator requires different valid home/away teams, `scheduled_at`, and optional venue, city, and valid timezone. `FixtureService` rejects schedule collisions and protects fixtures that are already operational or terminal.

## 9. Tournament administration

All routes below require Sanctum and `manage tournaments`. Most write operations also require the authenticated user to be the tournament creator.

| Method and path | Function |
|---|---|
| `GET /admin/tournaments` | Returns only tournaments created by the current user, paginated 20, with team/player/fixture/match counts. |
| `POST /admin/tournaments` | Creates a tournament, uploads optional logo/banner, sets creator, configuration, visibility, draft mode, ball/data source, structure, and code. Returns 201 and writes an audit record. |
| `GET /admin/tournaments/{tournament}` | Returns tournament configuration and related counts/details. |
| `PATCH /admin/tournaments/{tournament}` | Updates validated mutable configuration and optional branding. Creator-scoped and audited. |
| `POST /admin/tournaments/{tournament}/status` | Transitions tournament status to a validated supported value and records before/after state in the audit log. |

Tournament overs precedence is match override, then tournament default, then cricket rule-profile default.

## 10. Team administration

| Method and path | Function |
|---|---|
| `GET /admin/tournaments/{tournament}/teams` | Lists teams attached to the tournament with captain information and relevant counts. |
| `POST /admin/tournaments/{tournament}/teams` | Creates or finds a global team, attaches it to the tournament pivot, and returns the team. Supports identifying information such as name/short name and current creator. |
| `DELETE /admin/tournaments/{tournament}/teams/{team}` | Intended to remove a team from the tournament, subject to draft/match usage constraints. |
| `POST /admin/tournaments/{tournament}/teams/{team}/captain` | Assigns a user as this tournament team's captain and grants/maintains the captain role. |
| `DELETE /admin/tournaments/{tournament}/teams/{team}/captain` | Revokes the tournament-specific captain assignment. |

Teams are global and tournament membership lives in `tournament_teams`. Any controller still checking a direct `teams.tournament_id` reflects the pre-pivot architecture and requires review.

## 11. Player administration

| Method and path | Function |
|---|---|
| `GET /admin/tournaments/{tournament}/players` | Returns tournament registrations with profile/user data, optionally filtered, paginated 30. |
| `POST /admin/tournaments/{tournament}/players/manual` | Creates a guest profile and an already-approved tournament registration. Intended for players added by an admin without an app account. Returns 201. |
| `POST /admin/tournaments/{tournament}/players/{registration}/approve` | Creator-scoped; marks a pending/rejected registration approved and records reviewer/time. |
| `POST /admin/tournaments/{tournament}/players/{registration}/reject` | Creator-scoped; marks registration rejected and accepts optional review notes up to 2,000 characters. |

Guest profiles receive their permanent identity independently of an app user so historical statistics can later be claimed or linked.

## 12. Draft system

### Captain endpoints

Captain routes require Sanctum, role `captain`, permission `make draft pick`, and the `draft.enabled` middleware.

| Method and path | Function |
|---|---|
| `GET /tournaments/{tournament}/draft/state` | Returns draft state scoped to the captain's active team assignment. |
| `POST /tournaments/{tournament}/draft/pick` | Selects `tournament_player_id` for the active captain turn through the transactional draft engine. |

### Administrator endpoints

| Method and path | Function |
|---|---|
| `GET /admin/tournaments/{tournament}/draft/state` | Returns the full operational draft state. |
| `PUT /admin/tournaments/{tournament}/draft/setup` | Defines rounds, pick numbers/team assignments, and durations. Configuration is rejected after draft start. |
| `POST /admin/tournaments/{tournament}/draft/start` | Starts the configured draft and activates the first pending pick. |
| `POST /admin/tournaments/{tournament}/draft/pause` | Freezes the active countdown. |
| `POST /admin/tournaments/{tournament}/draft/resume` | Resumes a paused draft with its remaining duration. |
| `POST /admin/tournaments/{tournament}/draft/extend` | Adds 1-3,600 seconds and resets timing metadata for the active pick. |
| `POST /admin/tournaments/{tournament}/draft/skip` | Skips an eligible expired pick and advances the sequence. |
| `POST /admin/tournaments/{tournament}/draft/undo` | Requires `undo latest pick`; reverses the most recent selection. |
| `POST /admin/tournaments/{tournament}/draft/select-player` | Admin override using `pick_number` and `tournament_player_id`. |
| `POST /admin/tournaments/{tournament}/draft/remove-player` | Removes the selection at `pick_number`. |
| `POST /admin/tournaments/{tournament}/draft/reassign-player` | Moves/swaps assignment using distinct source and destination pick numbers. |

`DraftService` is the source of truth. It locks draft/pick rows, validates lifecycle, team turn, player approval and uniqueness, manages timing, increments revision, advances picks, completes the draft, and creates audit records. Expiry pauses the workflow; it does not silently make a pick.

## 13. Stages and fixtures

### Stages

| Method and path | Function |
|---|---|
| `GET /admin/tournaments/{tournament}/stages` | Lists stages ordered for the tournament. |
| `POST /admin/tournaments/{tournament}/stages` | Creates a stage with type, order, points rules, qualification rules, and status. Returns 201. |
| `GET /admin/tournaments/{tournament}/stages/{stage}` | Returns one stage and its fixtures. |
| `PATCH /admin/tournaments/{tournament}/stages/{stage}` | Updates stage configuration; status is `draft`, `active`, or `completed`. |
| `DELETE /admin/tournaments/{tournament}/stages/{stage}` | Deletes only a stage with no fixtures. |

### Fixtures

| Method and path | Function |
|---|---|
| `GET /admin/tournaments/{tournament}/fixtures` | Lists tournament fixtures with teams and operational match. |
| `POST /admin/tournaments/{tournament}/fixtures` | Creates a fixture, optionally auto-creating teams when IDs are zero and names are supplied. Returns 201. |
| `PUT /admin/tournaments/{tournament}/fixtures/{fixture}` | Updates a non-terminal fixture after ownership, membership, time-window, and collision checks. |
| `DELETE /admin/tournaments/{tournament}/fixtures/{fixture}` | Deletes only when no operational match exists. |
| `POST /admin/tournaments/{tournament}/fixtures/{fixture}/status` | Requests one of `scheduled`, `in_progress`, `postponed`, `completed`, or `cancelled`; service rules determine if the transition is legal. |
| `POST /admin/tournaments/{tournament}/fixtures/{fixture}/create-match` | Creates one operational match from a scheduled/postponed fixture and changes fixture status to `in_progress`. |

Fixture transitions implemented by the service are state-dependent. Completion is allowed only after the related match is completed or approved. Updates are blocked after an operational match exists or the fixture becomes in-progress/completed/cancelled.

## 14. Operational match setup

| Method and path | Function |
|---|---|
| `GET /admin/tournaments/{tournament}/matches` | Returns paginated matches with fixtures, rule profiles, toss, and players. |
| `POST /admin/tournaments/{tournament}/matches` | Creates a match directly from two tournament teams, optional fixture, and optional over limit. Returns 201. |
| `GET /admin/tournaments/{tournament}/matches/{match}` | Returns setup state, players, innings, fixture, and rule profile. |
| `PATCH /admin/tournaments/{tournament}/matches/{match}/overs` | Changes over limit only before live/terminal states. |
| `POST /admin/tournaments/{tournament}/matches/{match}/teams/{team}/playing-xi` | Submits player IDs and optional inline guest players for one team. |
| `POST /admin/tournaments/{tournament}/matches/{match}/approve-lineup` | Validates both teams' required XI counts and locks the lineup. |
| `POST /admin/tournaments/{tournament}/matches/{match}/toss` | Records team and `bat|field`, creates the first innings, and moves match to live. |

Standalone equivalents use `/admin/tournaments/0/matches/...` for playing XI, approval, and toss. These three routes are Sanctum-protected but omit the normal permission, throttle, and creator checks.

Their exact paths are:

```text
POST /admin/tournaments/0/matches/{match}/teams/{team}/playing-xi
POST /admin/tournaments/0/matches/{match}/approve-lineup
POST /admin/tournaments/0/matches/{match}/toss
```

The main lifecycle is:

```text
squad_selection -> lineup_pending -> toss_pending -> live
    -> innings_break/live -> completed -> result_pending -> approved
```

`MatchService` snapshots squad/profile information into match players. Tournament matches normally depend on valid tournament membership and completed draft selections. Custom matches can be created without a tournament.

## 15. Live scoring endpoints

Scoring writes require Sanctum plus `control draft`. Although the permission name is draft-oriented, it currently acts as the scorer permission.

| Method and path | Function |
|---|---|
| `POST /matches/{match}/deliveries` | Records one delivery. Requires striker, non-striker, and bowler match-player IDs. Accepts bat runs, five extra categories, wicket metadata, commentary, wagon coordinates, and optional `expected_revision`. |
| `POST /matches/{match}/deliveries/sync` | Accepts an ordered offline batch. Every item requires UUID and device timestamp. Existing UUIDs are acknowledged as `already_sync`; new deliveries are sorted and applied sequentially as `synced`. |
| `PATCH /deliveries/{matchDelivery}` | Corrects selected delivery fields and recalculates the entire match scorecard. |
| `POST /matches/{match}/undo` | Requires a 5-500 character reason. Voids the latest delivery and rebuilds derived statistics. |
| `POST /matches/{match}/next-innings` | Starts the next innings only after the current innings is complete. |
| `GET /matches/{match}/mvp` | Returns calculated MVP rankings. Authentication is required, but there is no additional permission on this route. |

For each accepted ball, `MatchScoringService`:

1. locks the match and active innings;
2. verifies live state and expected revision;
3. checks playing-XI participants and cricket constraints;
4. separates bat runs, wides, no-balls, byes, leg-byes, and penalties;
5. marks wides/no-balls illegal for legal-ball counting;
6. creates delivery and optional wicket records;
7. rebuilds cached batting and bowling figures from non-voided deliveries;
8. detects over, all-out, or chase completion;
9. updates match/innings state and revisions;
10. records audit/event information.

The delivery ledger is authoritative; cached totals and scorecard tables must remain rebuildable.

## 16. Result and standings endpoints

| Method and path | Access and function |
|---|---|
| `POST /admin/matches/{match}/result/submit` | `control draft`; creator-scoped through the match tournament. Derives winner/result from completed innings and moves `completed -> result_pending`. |
| `POST /admin/matches/{match}/result/approve` | `manage tournaments`; creator-scoped. Moves `result_pending -> approved`, updates the related fixture, and rebuilds standings transactionally. |

Only approved results should affect standings. `StandingsService` rebuilds aggregates rather than incrementing blindly, which makes approval idempotence and recovery safer.

## 17. Super Admin governance

Every governance method performs an explicit persisted `super_admin` role check in addition to route permissions.

| Method and path | Function |
|---|---|
| `GET /super-admin/dashboard` | Returns platform-level entity/session/queue metrics. |
| `GET /super-admin/api-clients` | Lists registered consuming applications. |
| `POST /super-admin/api-clients` | Creates API client metadata/credentials and audits the action. |
| `POST /super-admin/api-clients/{apiClient}/toggle` | Enables or disables a client and audits the change. |
| `GET /super-admin/api-sessions` | Lists/searches Sanctum sessions with active/expired filtering. |
| `POST /super-admin/api-sessions/{token}/revoke` | Revokes one API session and audits it. |
| `GET /super-admin/audit-logs` | Paginated audit search by text, action, user, and date range. |
| `GET /super-admin/health` | Checks database reachability/latency, queue counts, maintenance state, storage writability, route registration, debug/HTTPS posture, environment, PHP, and Laravel versions. Scheduler health remains manual. |
| `GET /super-admin/users` | Searches and filters users by role, with token/audit counts. |
| `GET /super-admin/users/{user}` | Returns roles, profile, session history, audit history, and activity counts. |
| `POST /super-admin/users/{user}/role` | Replaces the user's role. Prevents self-demotion and removal of the last Super Admin. |
| `POST /super-admin/users/{user}/revoke-sessions` | Revokes all API sessions for one user and audits the containment action. |
| `GET /super-admin/tournaments` | Global tournament search/status filter with operational counts. |
| `GET /super-admin/tournaments/{tournament}` | Full tournament operational view. |
| `GET /super-admin/matches` | Global paginated match list with optional status filter. Includes standalone matches. |
| `GET /super-admin/teams` | Global paginated/searchable team list. |
| `GET /super-admin/players` | Global paginated/searchable player profile list. |

## 18. Mobile offline synchronization model

The Android client writes local changes to Room and inserts a `PendingChangeEntity`. `SyncManager.pushPendingChanges()` sends changes by entity type, then marks or removes acknowledged queue items. WorkManager and connectivity callbacks retry pending changes when the network returns.

Key routing rule:

- tournament-backed teams, players, and fixtures use `/admin/tournaments/{id}/...`;
- standalone entities use `/custom/...`;
- offline balls use `/matches/{match}/deliveries/sync` with stable local UUIDs;
- a pre-live sync pushes local entities, pulls current server state, then uploads pending deliveries.

The server IDs returned after creation must replace or map the client's temporary IDs before dependent objects such as fixtures, lineups, and deliveries are pushed.

## 19. Important implementation risks and inconsistencies

These are code observations, not merely documentation gaps:

1. Public registration grants `admin` as well as `player`, which may allow any new account to acquire broad permissions depending on the seeded permission map.
2. The static `GET /teams/compare` route is declared after `GET /teams/{team}/squad`. Depending on Laravel route matching/binding behavior, `compare` may be consumed as a team identifier. Static routes should precede dynamic sibling routes.
3. `POST /teams/{team}/designations` requires authentication but no explicit role, permission, tournament ownership, or proof that submitted players belong to the team.
4. Custom fixture/team/player and custom match-setup routes require authentication but lack normal administrative permissions and creator ownership checks. A logged-in user may be able to mutate another user's standalone resources if IDs are known.
5. Some team controller logic and Super Admin eager-loading appear to retain pre-pivot assumptions such as a direct tournament relationship, while teams are now global through `tournament_teams`.
6. Public player detail currently exposes `phone`; this conflicts with the broader privacy goal of redacting contact information.
7. `GET /news` does not follow the otherwise common `{ "data": ... }` envelope convention in the same way as other endpoints.
8. `PATCH /deliveries/{delivery}` edits a ledger row in place before recalculation, whereas the architecture documents describe append-only corrections/voiding with audit history. It also has no match creator/assignment check beyond the broad scorer permission.
9. `GET /admin/tournaments/{tournament}/matches` does not perform the creator check used by the other match endpoints; permission holders may read another creator's matches.
10. Tournament sync's single revision is the maximum current draft/match revision. Independent resources can change without increasing that maximum, so this is not a collision-free global change token.
11. Validation and response schemas are hand-written in controllers rather than centralized API Resources/Form Requests, creating response-shape drift and making OpenAPI generation difficult.
12. Several endpoints use `control draft` as the scoring/result submission permission. A dedicated scorer permission would express intent and reduce privilege coupling.

## 20. Recommended client workflow

For a tournament match:

```text
register/login -> save token -> GET auth/me
-> create/update profile
-> create tournament -> attach teams -> add/approve players
-> configure/run draft when enabled
-> create stage and fixture
-> fixture/create-match
-> submit both playing XIs -> approve lineup -> record toss
-> GET match state
-> POST deliveries with expected_revision
-> next innings when required
-> submit result -> approve result
-> refresh standings/sync snapshot
```

For a standalone match:

```text
register/login
-> create/reuse custom teams
-> create guest players as needed
-> create custom fixture -> create operational match
-> use tournament/0 lineup, approval, and toss routes
-> score deliveries -> submit/approve result if supported by ownership rules
```

Clients should persist bearer tokens securely, treat 401 as session loss, treat 403 as insufficient authority, show 422 field errors, refresh on revision conflicts, retain offline UUIDs until acknowledged, and never assume a local write reached the server until the corresponding queue item is confirmed.

## 21. Primary implementation files

- Route registry: `routes/api.php`
- API controllers: `app/Http/Controllers/Api/V1/`
- Draft engine: `app/Modules/Draft/Services/DraftService.php`
- Fixture engine: `app/Modules/Tournament/Services/FixtureService.php`
- Match setup: `app/Modules/Scoring/Services/MatchService.php`
- Delivery scoring: `app/Modules/Scoring/Services/MatchScoringService.php`
- Recalculation: `app/Modules/Scoring/Services/MatchRecalculationService.php`
- Results: `app/Modules/Scoring/Services/MatchResultService.php`
- Standings: `app/Modules/Tournament/Services/StandingsService.php`
- Analytics: `app/Modules/Analytics/Services/`
- Android API declarations: `cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/api/ApiService.kt`
- Android outbound queue: `cricket-draft-mobile/app/src/main/java/com/devwithguru/cricket/data/sync/SyncManager.kt`

This guide describes current behavior. `routes/api.php` and its called services remain the final executable source of truth.
