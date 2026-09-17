# Cricket Draft API v1

The mobile application communicates with the Laravel backend through the versioned `/api/v1` namespace. Web and mobile clients use the same database and domain services; the API is not a second business-logic implementation.

## Authentication

Create a Super Admin API client first from the Super Admin control plane. Mobile login accepts the registered `client_slug` and returns a revocable Sanctum bearer token.

```http
POST /api/v1/auth/login
Accept: application/json
Content-Type: application/json

{
  "email": "captain@example.com",
  "password": "password",
  "device_name": "captain-android",
  "client_slug": "cricket-draft-android"
}
```

Send the returned token on protected requests:

```http
Authorization: Bearer {token}
Accept: application/json
```

| Endpoint | Authentication | Purpose |
|---|---|---|
| `POST /api/v1/auth/login` | Public, rate limited | Create a mobile session |
| `GET /api/v1/auth/me` | Bearer token | Return user, roles, permissions, and player profile |
| `POST /api/v1/auth/logout` | Bearer token | Revoke the current device session |
| `POST /api/v1/auth/logout-all` | Bearer token | Revoke all sessions for the current user |

## Public tournament and scorecard data

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/tournaments` | List public registration, ready, live, and completed tournaments |
| `GET /api/v1/tournaments/{tournament}` | Tournament summary and cricket rule profile |
| `GET /api/v1/tournaments/{tournament}/fixtures` | Published schedule with teams, venue, time, and match status |
| `GET /api/v1/matches/{match}/state` | Live score, innings, batting, bowling, recent deliveries, and effective over limit |

The match-state response returns `overs_per_innings` at match level and `maximum_overs` for each innings so public and mobile scorecards can render the correct cap.

The live match state includes a `revision` integer. Clients should retain their last revision and refresh the state when the server revision changes. The current web scorecard polls every two seconds; a future mobile client can use the same strategy until push broadcasting is introduced.

For a single polling request that combines draft, fixtures, live matches, standings, and server time, use `GET /api/v1/tournaments/{tournament}/sync?revision={last_revision}`. The response includes `changed`, a server-side `revision`, and all current mobile synchronization data. A client can skip UI reconciliation when `changed` is false.

## Profile and registration APIs

Protected profile routes allow the mobile player to maintain the same fields available on the web profile form:

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/profile` | Return the authenticated player profile |
| `PATCH /api/v1/profile` | Update full name, phone, city, role, styles, and bio |
| `GET /api/v1/tournaments/{tournament}/registration` | Return the authenticated player’s registration status |
| `POST /api/v1/tournaments/{tournament}/registration` | Submit or resubmit a registration for approval |

Public tournament resources also expose approved team summaries, redacted approved player pools, fixtures, and official standings:

```text
GET /api/v1/tournaments/{tournament}/teams
GET /api/v1/tournaments/{tournament}/players
GET /api/v1/tournaments/{tournament}/fixtures
GET /api/v1/tournaments/{tournament}/standings
```

## Captain APIs

Captain routes require a bearer token, the `captain` role, the `make draft pick` permission, and an active captain assignment for the tournament.

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/tournaments/{tournament}/draft/state` | Return captain-scoped draft state and selected squad |
| `POST /api/v1/tournaments/{tournament}/draft/pick` | Submit a player pick through the transactional draft engine |

```http
POST /api/v1/tournaments/{tournament}/draft/pick
Authorization: Bearer {token}
Content-Type: application/json

{
  "tournament_player_id": 42
}
```

## Scorer APIs

Scorer routes require a bearer token and the `control draft` permission. All delivery writes are processed by the same server-authoritative scoring service used by the web scorer room.

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/matches/{match}/deliveries` | Record one ball with runs, extras, wicket, commentary, and expected revision |
| `POST /api/v1/matches/{match}/next-innings` | Start the next innings after completion |
| `POST /api/v1/matches/{match}/undo` | Void the latest delivery with a required reason |

The `expected_revision` field prevents stale mobile or tablet scorer screens from overwriting a newer event. Score totals and statistics remain derived from the delivery ledger.

## Administrator APIs

Administrator routes use Sanctum bearer tokens and the same permission boundaries as the web control plane. They reuse `TournamentController`, `DraftService`, `FixtureService`, `MatchService`, `MatchScoringService`, `MatchResultService`, and `StandingsService`; therefore, API writes remain transactional and server-authoritative.

| Endpoint group | Main operations |
|---|---|
| `/api/v1/admin/tournaments` | List, create, update, inspect, and transition tournament lifecycle |
| `/api/v1/admin/tournaments/{tournament}/players` | List registrations and approve or reject players |
| `/api/v1/admin/tournaments/{tournament}/draft` | Poll full admin state; start, pause, resume, extend, skip, undo, select, remove, or reassign picks |
| `/api/v1/admin/tournaments/{tournament}/fixtures` | Create, update, transition, and hand off scheduled fixtures to operational matches |
| `/api/v1/admin/tournaments/{tournament}/matches` | Create matches, submit Playing XIs, approve lineups, and record tosses |
| `PATCH /api/v1/admin/tournaments/{tournament}/matches/{match}/overs` | Change the match over limit before toss/scoring begins |
| `/api/v1/admin/matches/{match}/result` | Submit a completed result and approve it for standings |

Every draft mutation is performed by the administrator explicitly. A timer reaching zero produces an expired/paused state; the API never advances a pick automatically. `POST /draft/extend` adds seconds to the active remaining duration, while `POST /draft/skip` is accepted only after expiry.

The tournament status endpoint accepts any supported status (`draft`, `registration`, `ready`, `live`, `completed`, or `cancelled`) from any current status. Each change is audited with its previous and next value. Tournament configuration accepts optional `default_overs_per_innings` from 1 to 100; when omitted, the active cricket rule profile supplies the default. Match creation accepts optional `overs_per_innings` from 1 to 100, and the match-specific value is frozen into every innings and enforced by the scoring engine. The precedence is **match override**, then **tournament default**, then **rule-profile default**.

## Super Admin governance

The web control plane is available at `/super-admin` for users with the dedicated `super_admin` role. Its API counterpart is protected by both the governance permissions and an explicit persisted `super_admin` role check. It supports platform metrics, API client registration and activation, session listing and revocation, global audit logs, and health checks:

```text
GET|POST /api/v1/super-admin/api-clients
POST /api/v1/super-admin/api-clients/{apiClient}/toggle
GET /api/v1/super-admin/api-sessions
POST /api/v1/super-admin/api-sessions/{token}/revoke
GET /api/v1/super-admin/audit-logs
GET /api/v1/super-admin/health
```

A regular `admin` role is rejected from these routes, even though administrator permissions are broad elsewhere in the application.

The expanded governance endpoints are:

```text
GET  /api/v1/super-admin/users?search=&role=
GET  /api/v1/super-admin/users/{user}
POST /api/v1/super-admin/users/{user}/role
POST /api/v1/super-admin/users/{user}/revoke-sessions
GET  /api/v1/super-admin/tournaments?search=&status=
GET  /api/v1/super-admin/tournaments/{tournament}
GET  /api/v1/super-admin/api-sessions?search=&status=active|expired
GET  /api/v1/super-admin/audit-logs?search=&action=&user_id=&from=&to=
GET  /api/v1/super-admin/health
```

User role changes and session containment actions write audit events. The platform refuses to remove the `super_admin` role from the last remaining Super Admin, and a Super Admin cannot remove their own Super Admin access. Health responses include database response time, queue counts, storage state, API route registration, application state, environment details, HTTPS posture, and debug-mode status.

## Owner-scoped management collections

The following endpoints require a bearer token and return only resources managed by the authenticated creator:

```text
GET /api/v1/me/tournaments
GET /api/v1/me/fixtures
GET /api/v1/me/matches
```

Each item includes `owner_user_id` and a server-derived `viewer_permissions` object. Match state and public tournament/fixture projections use the same capability keys: `can_view`, `can_manage`, `can_edit_fixture`, `can_manage_lineup`, `can_score`, and `can_submit_result`. Public visibility does not set mutation capabilities. `GET /api/v1/players/{playerProfile}/matches` is participation history and never grants management authority.
