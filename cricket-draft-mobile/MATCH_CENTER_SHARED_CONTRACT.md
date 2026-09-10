# Match Center Shared Contract — API v1

This is the Phase 1 source of truth for names and wire values shared by Android and Laravel.

## Identity

| Concept | Android type | API representation | Rule |
|---|---|---|---|
| Local match | `LocalMatchId` | UUID string in `client_uuid` | Generated once; Room primary identity |
| Server fixture | `ServerFixtureId` | positive integer | Never inferred from a local ID |
| Operational match | `ServerMatchId` | positive integer | Required for scoring endpoints |
| Tournament/team/player/match-player/innings | Dedicated typed IDs | positive integer | Internal IDs are never interchangeable |
| Public player ID | `publicPlayerId` | unique six-digit string | Search/display only; database PK remains internal |

`hashCode()`, formatted route strings, team names and player names are prohibited as identity conversion mechanisms.

## Match configuration

The immutable snapshot contains `format`, `innings_per_side`, `overs_per_innings`, optional `squad_size`, `playing_xi_size`, `maximum_wickets`, `legal_balls_per_over`, optional bowler/ball/run caps, `ball_type`, extra penalties/attribution rules, `last_man_standing`, `origin`, `version`, and `locked_at`.

Key invariants:

- playing XI is 2–99; maximum wickets is positive and below playing-XI size;
- optional squad size cannot be below playing-XI size;
- innings are 1–4, overs 1–100, legal balls per over 1–12;
- bowler limit cannot exceed innings overs;
- configuration is locked when the operational match is created;
- `origin` is `custom`, `tournament`, or `legacy_review`.

## Canonical lifecycle

```text
local_draft -> pending_sync -> scheduled -> squad_selection
-> lineup_pending -> toss_pending -> live -> innings_break
-> completed -> result_pending -> approved
```

Terminal/exception states are `rejected`, `abandoned`, and `cancelled`. `toss_completed`, `ready`, and `in_progress` are legacy UI aliases, not new backend states; adapters must map them to the canonical lifecycle.

Offline queue policy:

- local draft, configuration, team selection, lineup selection, toss command, scoring events, innings-end and match-end commands may be queued;
- the UI may show locally accepted/pending state, but must not fabricate a backend revision;
- result approval/rejection and conflict resolution require confirmed authorization and server state.

## Normalized enums

- Format: `t10`, `t20`, `odi`, `test`, `limited_overs`, `custom`
- Ball: `leather`, `tennis`, `hard_ball`, `tape_ball`, `indoor`
- Toss: `bat`, `field`
- Extras: `wide`, `no_ball`, `bye`, `leg_bye`, `penalty`
- Sync: `pending`, `syncing`, `synced`, `failed`, `needs_attention`
- Dismissals: `bowled`, `caught`, `lbw`, `run_out`, `stumped`, `hit_wicket`, `retired_out`, `obstructing_field`

## API error envelope

```json
{
  "code": "revision_conflict",
  "message": "The match changed on the server.",
  "field_errors": {},
  "retryable": false,
  "current_revision": 17,
  "correlation_id": "01J..."
}
```

Stable codes are `unauthenticated`, `forbidden`, `validation_failed`, `revision_conflict`, `not_found`, `throttled`, `transient_server_error`, and `network_unavailable`. Network, 429 and transient 5xx failures are retryable. Authentication, authorization, validation, not-found and revision conflicts require explicit handling.

## JSON examples

Custom match configuration:

```json
{"format":"custom","innings_per_side":1,"overs_per_innings":10,"squad_size":15,"playing_xi_size":11,"maximum_wickets":10,"legal_balls_per_over":6,"max_overs_per_bowler":2,"ball_type":"tennis","no_ball_runs":1,"wide_runs":1,"wide_runs_to_batsman":false,"noball_runs_to_batsman":false,"last_man_standing":false,"max_balls_per_over":null,"max_runs_per_over":null,"origin":"custom","version":1,"locked_at":"2026-09-10T10:00:00Z"}
```

Tournament match uses the same shape with `origin: "tournament"`; its values are copied from the selected tournament profile and then locked. No-draft tournaments still require explicit approved player selection. Last-man-standing uses the configured playing XI as the all-out boundary; otherwise `maximum_wickets` applies.
