package com.devwithguru.cricket.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// ─── Tournament Response Models ─────────────────────────────

data class TournamentListResponse(
    val data: List<TournamentData>
)

data class TournamentDetailResponse(
    val data: TournamentData
)

data class TournamentData(
    val id: Int,
    val name: String?,
    val season_name: String?,
    val slug: String?,
    val status: String?,
    val venue: String?,
    val city: String?,
    val timezone: String?,
    val starts_on: String?,
    val ends_on: String?,
    val has_draft: Boolean? = null,
    val squad_size: Int? = null,
    val default_pick_duration: Int? = null,
    val ball_type: String? = null,
    val description: String? = null,
    val logo: String? = null,
    val cover_image: String? = null,
    val organizer_name: String? = null,
    val contact_info: String? = null,
    val competition_structure: String? = null,
    val tournament_code: String? = null,
    val is_public: Boolean? = null,
    val default_overs_per_innings: Int? = null,
    val rule_profile: RuleProfileData?,
    val fixtures_count: Int? = null,
    val teams_count: Int? = null,
    val tournament_players_count: Int? = null,
    val matches_count: Int? = null
)

data class RuleProfileData(
    val name: String?,
    val format: String?,
    val overs_per_innings: Int?,
    val legal_balls_per_over: Int?
)

data class TournamentTeamsResponse(
    val data: List<TournamentTeamData>
)

data class TournamentTeamData(
    val id: Int,
    val name: String?,
    val short_name: String?,
    val logo_path: String?,
    val squad_count: Int?,
    val creator_id: Int? = null
)

data class TournamentPlayersResponse(
    val data: List<TournamentPlayerData>
)

data class TournamentPlayerData(
    val id: Int,
    val full_name: String?,
    val playing_role: String?,
    val batting_style: String?,
    val bowling_style: String?,
    val city: String?,
    val photo_path: String?
)

data class TournamentStandingsResponse2(
    val data: List<TournamentStandingData>
)

data class TournamentStandingData(
    val position: Int?,
    val team: TeamData?,
    val played: Int,
    val wins: Int,
    val losses: Int,
    val ties: Int,
    val no_results: Int,
    val points: Int,
    val net_run_rate: Double?
)

data class TournamentFixturesResponse2(
    val data: List<TournamentFixtureData2>
)

data class TournamentFixtureData2(
    val id: Int,
    val round_number: Int?,
    val round_name: String?,
    val match_number: Int?,
    val scheduled_at: String?,
    val timezone: String?,
    val venue: String?,
    val city: String?,
    val status: String?,
    val home_team: TeamData?,
    val away_team: TeamData?,
    val match_id: Int?,
    val match_status: String?,
    val toss_winner: String? = null,
    val toss_decision: String? = null,
    val current_innings: Int? = null,
    val current_runs: Int? = null,
    val current_wickets: Int? = null,
    val overs_bowled: String? = null,
    val first_innings_runs: Int? = null,
    val first_innings_wickets: Int? = null
)

// ─── Team Squad Response ────────────────────────────────────

data class TeamSquadResponse(
    val data: TeamSquadData
)

data class TeamSquadData(
    val team_name: String?,
    val squad: List<SquadPlayerData>
)

data class SquadPlayerData(
    val tournament_player_id: Int,
    val player_name: String?,
    val playing_role: String?,
    val is_captain: Boolean?,
    val is_vice_captain: Boolean?,
    val is_wicketkeeper: Boolean?
)

// ─── Admin Tournament Request/Response Models ─────────────

data class CreateTournamentRequest(
    val name: String,
    val slug: String,
    val season_name: String? = null,
    val location: String? = null,
    val city: String? = null,
    val venue: String? = null,
    val starts_on: String? = null,
    val ends_on: String? = null,
    val ball_type: String = "Tennis Ball",
    val has_draft: Boolean = false,
    val squad_size: Int = 11,
    val default_pick_duration: Int = 60,
    val default_overs_per_innings: Int? = null,
    val is_public: Boolean = true,
    val description: String? = null,
    val organizer_name: String? = null,
    val contact_info: String? = null,
    val competition_structure: String? = null,
    val timezone: String = "Asia/Karachi"
)

data class CreateTournamentResponse(
    val data: TournamentData,
    val message: String?
)

data class UpdateTournamentRequest(
    val name: String? = null,
    val season_name: String? = null,
    val city: String? = null,
    val venue: String? = null,
    val starts_on: String? = null,
    val ends_on: String? = null,
    val ball_type: String? = null,
    val has_draft: Boolean? = null,
    val squad_size: Int? = null,
    val default_pick_duration: Int? = null,
    val default_overs_per_innings: Int? = null
)

data class UpdateStatusRequest(
    val status: String
)

data class TournamentStatusResponse(
    val data: TournamentData,
    val message: String?
)

data class AdminTeamDataResponse(
    val data: AdminTeamData,
    val message: String? = null
)

data class AdminSelectPlayerRequest(
    val pick_number: Int,
    val tournament_player_id: Int
)

data class ExtendTimerRequest(
    val seconds: Int
)

data class CaptainPickRequest(
    val tournament_player_id: Int
)

// ─── Fixture Creation Models ───────────────────────────────

data class CreateFixtureRequest(
    val home_team_id: Int,
    val away_team_id: Int,
    val round_number: Int? = null,
    val round_name: String? = null,
    val match_number: Int? = null,
    val scheduled_at: String,
    val venue: String? = null,
    val city: String? = null,
    val timezone: String = "Asia/Karachi",
    val notes: String? = null
)

data class AdminFixtureResponse(
    val data: TournamentFixtureData2,
    val message: String? = null
)

// ─── Draft Setup Models ────────────────────────────────────

data class DraftSetupRequest(
    val rounds: List<DraftSetupRound>
)

data class DraftSetupRound(
    val round_number: Int,
    val name: String? = null,
    val picks: List<DraftSetupPick>
)

data class DraftSetupPick(
    val team_id: Int,
    val pick_number: Int,
    val pick_duration: Int = 60
)

data class SimpleMessageResponse(
    val message: String? = null
)

// ─── Match Admin Models ──────────────────────────────────
data class AdminMatchListResponse(
    val data: List<AdminMatchData>
)

data class AdminMatchData(
    val id: Int,
    val tournament_id: Int,
    val status: String?,
    val overs_per_innings: Int?,
    val revision: Int?,
    val result_summary: String?,
    val toss_decision: String?,
    val started_at: String?,
    val completed_at: String?,
    val fixture: AdminMatchFixtureData?,
    val rule_profile: RuleProfileData?,
    val toss_winner: TeamData?,
    val players: List<Any>? = null,
    val innings: List<Any>? = null
)

data class AdminMatchFixtureData(
    val id: Int?,
    val title: String?,
    val round_name: String?,
    val scheduled_at: String?,
    val venue: String?,
    val home_team_id: Int?,
    val away_team_id: Int?,
    val homeTeam: TeamData?,
    val awayTeam: TeamData?
)

data class AdminMatchDetailResponse(
    val data: AdminMatchData,
    val message: String? = null
)

data class CreateMatchRequest(
    val home_team_id: Int,
    val away_team_id: Int,
    val fixture_id: Int? = null,
    val overs_per_innings: Int? = null
)

data class UpdateOversRequest(
    val overs_per_innings: Int
)

data class PlayingXiRequest(
    val player_ids: List<Int>
)

data class TossRequest(
    val toss_winner_team_id: Int,
    val toss_decision: String
)

// ─── Fixture Status Models ────────────────────────────────
data class UpdateFixtureStatusRequest(
    val status: String
)

data class CreateMatchFromFixtureResponse(
    val data: CreateMatchFromFixtureData,
    val message: String? = null
)

data class CreateMatchFromFixtureData(
    val match_id: Int,
    val status: String?
)

// ─── Offline Sync Models ──────────────────────────────────
data class SyncDeliveriesRequest(
    val deliveries: List<SyncDeliveryData>
)

data class SyncDeliveryData(
    val local_uuid: String,
    val device_timestamp: String,
    val striker_id: Int,
    val non_striker_id: Int,
    val bowler_id: Int,
    val runs_off_bat: Int? = null,
    val wides: Int? = null,
    val no_balls: Int? = null,
    val byes: Int? = null,
    val leg_byes: Int? = null,
    val penalty_runs: Int? = null,
    val commentary: String? = null,
    val wagon_x: Double? = null,
    val wagon_y: Double? = null,
    val wicket: SyncWicketData? = null
)

data class SyncWicketData(
    val dismissed_player_id: Int,
    val dismissal_type: String,
    val fielder_id: Int? = null,
    val runs_completed: Int? = null,
    val notes: String? = null
)

data class SyncDeliveriesResponse(
    val data: SyncDeliveriesResult
)

data class SyncDeliveriesResult(
    val deliveries: List<SyncDeliveryResult>,
    val match: SyncMatchData
)

data class SyncDeliveryResult(
    val local_uuid: String,
    val delivery_id: Int,
    val revision: Int,
    val notation: String,
    val status: String
)

data class SyncMatchData(
    val id: Int,
    val status: String?,
    val revision: Int?,
    val total_runs: Int?,
    val wickets: Int?,
    val legal_balls: Int?
)

// ─── Delivery Edit Models ─────────────────────────────────
data class EditDeliveryRequest(
    val striker_id: Int? = null,
    val non_striker_id: Int? = null,
    val bowler_id: Int? = null,
    val runs_off_bat: Int? = null,
    val wides: Int? = null,
    val no_balls: Int? = null,
    val byes: Int? = null,
    val leg_byes: Int? = null,
    val penalty_runs: Int? = null,
    val commentary: String? = null,
    val wagon_x: Double? = null,
    val wagon_y: Double? = null
)

data class EditDeliveryResponse(
    val message: String?,
    val data: Any? = null
)

// ─── Player Comparison Models ─────────────────────────────
data class PlayerComparisonResponse(
    val data: PlayerComparisonData
)

data class PlayerComparisonData(
    val player1: PlayerComparisonStats,
    val player2: PlayerComparisonStats
)

data class PlayerComparisonStats(
    val id: Int?,
    val full_name: String?,
    val matches_played: Int?,
    val batting: PlayerBattingComparison?,
    val bowling: PlayerBowlingComparison?
)

data class PlayerBattingComparison(
    val runs: Int?,
    val average: Double?,
    val strike_rate: Double?,
    val fours: Int?,
    val sixes: Int?
)

data class PlayerBowlingComparison(
    val wickets: Int?,
    val average: Double?,
    val economy: Double?,
    val maidens: Int?
)

// ─── Team Comparison Models ───────────────────────────────
data class TeamComparisonResponse(
    val data: TeamComparisonData
)

data class TeamComparisonData(
    val team1: TeamComparisonInfo,
    val team2: TeamComparisonInfo,
    val summary: TeamComparisonSummary,
    val encounters: List<TeamEncounterData>
)

data class TeamComparisonInfo(
    val id: Int,
    val name: String?,
    val short_name: String?,
    val logo_path: String?,
    val wins: Int
)

data class TeamComparisonSummary(
    val total_encounters: Int,
    val team1_wins: Int,
    val team2_wins: Int,
    val ties_no_results: Int
)

data class TeamEncounterData(
    val match_id: Int,
    val date: String?,
    val home_team: TeamEncounterTeam?,
    val away_team: TeamEncounterTeam?,
    val winner_id: Int?,
    val result_text: String?
)

data class TeamEncounterTeam(
    val id: Int?,
    val short_name: String?
)

// ─── Standings Simulation Models ──────────────────────────
data class StandingsSimulationResponse(
    val data: StandingsSimulationData
)

data class StandingsSimulationData(
    val simulations: List<Any>?,
    val qualification_scenarios: List<Any>?
)
