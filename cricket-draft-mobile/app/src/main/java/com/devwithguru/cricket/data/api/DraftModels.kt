package com.devwithguru.cricket.data.api

// ─── Draft State Response ──────────────────────────────────

data class DraftStateResponse(
    val data: DraftStateData
)

data class DraftStateData(
    val id: Int,
    val status: String,          // setup, live, paused, expired, completed
    val revision: Int,
    val current_pick_number: Int?,
    val current_round: Int?,
    val current_team: DraftTeamInfo?,
    val captain_team: DraftTeamInfo?,
    val can_start_next_pick: Boolean,
    val next_pick: DraftNextPick?,
    val can_start_next_round: Boolean,
    val next_round: DraftNextRound?,
    val rounds: List<DraftRoundData>,
    val team_squads: List<DraftTeamSquad>,
    val pending_picks: List<DraftPendingPick>,
    val remaining_players: List<DraftAvailablePlayer>,
    val timer: DraftTimerData,
    val captain_can_pick: Boolean,
    val summary: DraftSummary,
    val picks: List<DraftPickData>,
    val available_players: List<DraftAvailablePlayer>
)

data class DraftTeamInfo(
    val id: Int,
    val name: String?,
    val short_name: String?
)

data class DraftNextPick(
    val pick_number: Int,
    val round_number: Int?,
    val team: DraftTeamInfo?
)

data class DraftNextRound(
    val round_number: Int,
    val name: String?
)

data class DraftRoundData(
    val round_number: Int,
    val name: String?,
    val status: String,          // pending, active, completed
    val total: Int,
    val selected: Int,
    val skipped: Int,
    val pending: Int
)

data class DraftTeamSquad(
    val id: Int,
    val name: String?,
    val short_name: String?,
    val selected_count: Int,
    val selected_players: List<DraftSquadPlayer>
)

data class DraftSquadPlayer(
    val pick_number: Int,
    val full_name: String?,
    val playing_role: String?,
    val city: String?,
    val selected_at: String?
)

data class DraftPendingPick(
    val pick_number: Int,
    val round: Int?,
    val team: DraftTeamInfo?
)

data class DraftAvailablePlayer(
    val id: Int,
    val full_name: String?,
    val playing_role: String?,
    val city: String?
)

data class DraftTimerData(
    val remaining_seconds: Int?,
    val started_at: String?,
    val expires_at: String?,
    val server_now: String?,
    val duration: Int?,
    val expired: Boolean
)

data class DraftSummary(
    val total: Int,
    val selected: Int,
    val active: Int,
    val expired: Int,
    val skipped: Int,
    val pending: Int
)

data class DraftPickData(
    val pick_number: Int,
    val round: Int?,
    val status: String,          // pending, active, selected, expired, skipped
    val team: DraftTeamInfo?,
    val player: DraftPlayerInfo?
)

data class DraftPlayerInfo(
    val id: Int?,
    val full_name: String?,
    val playing_role: String?,
    val city: String?
)

// ─── Admin Tournament Team Models ───────────────────────────

data class AdminTeamListResponse(
    val data: List<AdminTeamData>
)

data class AdminTeamData(
    val id: Int,
    val name: String?,
    val short_name: String?,
    val unique_code: String?,
    val is_active: Boolean?,
    val display_order: Int?,
    val active_captain: AdminCaptainData?,
    val draft_picks_count: Int? = null
)

data class AdminCaptainData(
    val user_id: Int?,
    val assigned_at: String?,
    val user: AdminCaptainUser?
)

data class AdminCaptainUser(
    val id: Int?,
    val name: String?,
    val email: String?
)

data class CreateTeamRequest(
    val name: String,
    val short_name: String? = null
)

data class AssignCaptainRequest(
    val user_id: Int
)

// ─── Admin Player Models ───────────────────────────────────

data class AdminPlayerListResponse(
    val data: List<AdminPlayerData>
)

data class AdminPlayerData(
    val id: Int,
    val status: String,          // pending, approved, rejected
    val player_profile: AdminPlayerProfile?
)

data class AdminPlayerProfile(
    val id: Int?,
    val full_name: String?,
    val playing_role: String?,
    val city: String?,
    val user: AdminPlayerUser?
)

data class AdminPlayerUser(
    val id: Int?,
    val name: String?,
    val email: String?
)

// ─── Registration Models ───────────────────────────────────

data class RegistrationStatusResponse(
    val data: RegistrationStatusData?
)

data class RegistrationStatusData(
    val id: Int,
    val status: String,
    val submitted_at: String?,
    val reviewed_at: String?
)

data class RegistrationStoreResponse(
    val data: RegistrationStoreData,
    val message: String?
)

data class RegistrationStoreData(
    val id: Int,
    val status: String
)
