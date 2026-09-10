package com.devwithguru.cricket.data.api

import com.devwithguru.cricket.domain.model.BatterState
import com.devwithguru.cricket.domain.model.BowlerState
import com.devwithguru.cricket.domain.model.ScheduledFixture

/**
 * API response from GET /api/v1/matches/{matchId}/state
 */
data class MatchStateResponse(
    val data: MatchStateData
)

data class MatchStateData(
    val id: Int,
    val revision: Int,
    val status: String,
    val result_type: String?,
    val result_summary: String?,
    val winner_team_id: Int?,
    val overs_per_innings: Int,
    val fixture: MatchFixtureData? = null,
    val rule_snapshot: RuleSnapshotData? = null,
    val players: List<MatchPlayerData> = emptyList(),
    val innings: List<InningsData>
)

data class MatchPlayerData(
    val match_player_id: Int,
    val player_profile_id: Int?,
    val team_id: Int,
    val name: String,
    val role: String?
)

data class MatchFixtureData(
    val id: Int?,
    val home_team: TeamData?,
    val away_team: TeamData?,
    val scheduled_at: String?,
    val venue: String?,
    val city: String?
)

data class RuleSnapshotData(
    val profile_id: Int? = null,
    val format: String?,
    val innings_per_side: Int? = null,
    val overs_per_innings: Int?,
    val squad_size: Int? = null,
    val playing_xi_size: Int?,
    val maximum_wickets: Int?,
    val legal_balls_per_over: Int?,
    val max_overs_per_bowler: Int? = null,
    val ball_type: String?,
    val no_ball_runs: Int? = null,
    val wide_runs: Int? = null,
    val wide_runs_to_batsman: Boolean? = null,
    val noball_runs_to_batsman: Boolean? = null,
    val last_man_standing: Boolean? = null,
    val max_balls_per_over: Int? = null,
    val max_runs_per_over: Int? = null,
    val origin: String? = null,
    val version: Int? = null,
    val locked_at: String? = null
)

data class InningsData(
    val id: Int,
    val number: Int,
    val batting_team: TeamData?,
    val bowling_team: TeamData?,
    val runs: Int,
    val wickets: Int,
    val legal_balls: Int,
    val maximum_overs: Int,
    val overs: String,
    val target: Int?,
    val status: String,
    val active_players: ActivePlayersData? = null,
    val batting: List<BattingStatData>,
    val bowling: List<BowlingStatData>,
    val recent_deliveries: List<RecentDeliveryData>
)

data class ActivePlayersData(
    val striker: ActivePlayerData?,
    val non_striker: ActivePlayerData?,
    val bowler: ActivePlayerData?
)

data class ActivePlayerData(
    val match_player_id: Int,
    val team_id: Int,
    val name: String?,
    val role: String?
)

data class TeamData(
    val id: Int?,
    val name: String?,
    val short_name: String?
)

data class BattingStatData(
    val player: String?,
    val dismissal: String?,
    val runs: Int,
    val balls: Int,
    val fours: Int,
    val sixes: Int,
    val strike_rate: Double
)

data class BowlingStatData(
    val player: String?,
    val overs: String,
    val runs: Int,
    val wickets: Int,
    val wides: Int,
    val no_balls: Int,
    val economy: Double
)

data class RecentDeliveryData(
    val over: String,
    val notation: String,
    val total_runs: Int
)

/**
 * API response from GET /api/v1/matches/{matchId}/mvp
 */
data class MvpResponse(
    val data: List<MvpPlayerData>
)

data class MvpPlayerData(
    val player_name: String,
    val team_name: String?,
    val total_points: Int,
    val batting_points: Int,
    val bowling_points: Int,
    val fielding_points: Int
)

/**
 * Convert API response to domain ScheduledFixture
 */
fun MatchStateData.toScheduledFixture(matchId: String): ScheduledFixture {
    val firstInnings = innings.find { it.number == 1 }
    val secondInnings = innings.find { it.number == 2 }
    val currentInningsData = innings.maxByOrNull { it.number }

    val homeTeamName = fixture?.home_team?.name.orEmpty()
    val awayTeamName = fixture?.away_team?.name.orEmpty()

    return ScheduledFixture(
        id = matchId,
        homeTeam = homeTeamName,
        awayTeam = awayTeamName,
        overs = overs_per_innings,
        ballType = rule_snapshot?.ball_type.orEmpty(),
        matchType = rule_snapshot?.format.orEmpty(),
        wickets = rule_snapshot?.maximum_wickets ?: 0,
        ballsPerOver = rule_snapshot?.legal_balls_per_over ?: 6,
        venue = fixture?.venue.orEmpty(),
        date = "",
        time = "",
        status = when (status) {
            "live" -> "Live"
            "completed", "result_pending", "approved" -> "Completed"
            else -> "Scheduled"
        },
        currentRuns = currentInningsData?.runs ?: 0,
        currentWickets = currentInningsData?.wickets ?: 0,
        oversBowled = currentInningsData?.overs ?: "0.0",
        strikerName = currentInningsData?.active_players?.striker?.name.orEmpty(),
        nonStrikerName = currentInningsData?.active_players?.non_striker?.name.orEmpty(),
        bowlerName = currentInningsData?.active_players?.bowler?.name.orEmpty(),
        currentInnings = currentInningsData?.number ?: 1,
        firstInningsRuns = firstInnings?.runs,
        firstInningsWickets = firstInnings?.wickets,
        firstInningsBatsmen = firstInnings?.batting?.map { it.toBatterState() } ?: emptyList(),
        firstInningsBowlers = firstInnings?.bowling?.map { it.toBowlerState() } ?: emptyList(),
        secondInningsBatsmen = secondInnings?.batting?.map { it.toBatterState() } ?: emptyList(),
        secondInningsBowlers = secondInnings?.bowling?.map { it.toBowlerState() } ?: emptyList(),
        playerServerIds = players.associate { it.name to it.match_player_id }
    )
}

fun BattingStatData.toBatterState() = BatterState(
    name = player ?: "Unknown",
    runs = runs,
    balls = balls,
    fours = fours,
    sixes = sixes,
    isDismissed = !dismissal.isNullOrEmpty() && dismissal != "not_out",
    dismissalType = dismissal
)

fun BowlingStatData.toBowlerState() = BowlerState(
    name = player ?: "Unknown",
    balls = runBallsFromOvers(overs),
    runsConceded = runs,
    wickets = wickets
)

private fun runBallsFromOvers(overs: String): Int {
    val parts = overs.split(".")
    val fullOvers = parts.getOrElse(0) { "0" }.toIntOrNull() ?: 0
    val balls = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
    return fullOvers * 6 + balls
}
