package com.devwithguru.cricket.ui.feature.match.scorer

import com.devwithguru.cricket.domain.model.BatterState
import com.devwithguru.cricket.domain.model.BowlerState
import com.devwithguru.cricket.domain.model.PartnershipEvent
import com.devwithguru.cricket.domain.model.WicketEvent

data class MatchScoringState(
    val runs: Int = 0,
    val wickets: Int = 0,
    val totalBalls: Int = 0,
    val extras: Int = 0,
    val dotBalls: Int = 0,
    val strikerName: String = "Ahmed Ali",
    val batter1: BatterState = BatterState("Ahmed Ali"),
    val batter2: BatterState = BatterState("Bilal Butt"),
    val bowler: BowlerState = BowlerState("Yasir Khan"),
    val thisOver: List<String> = emptyList(),
    val nextBatsmenQueue: List<String> = emptyList(),
    val dismissedBatsmen: List<String> = emptyList(),

    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val battingTeamName: String = "",
    val bowlingTeamName: String = "",
    val matchTotalOvers: Int = 6,
    val matchTotalWickets: Int = 10,
    val isInnings2: Boolean = false,
    val firstInningsTarget: Int? = null,
    val ballsPerOver: Int = 6,
    val bowlersStats: Map<String, BowlerState> = emptyMap(),
    val batsmenStats: Map<String, BatterState> = emptyMap(),
    
    val fallOfWickets: List<WicketEvent> = emptyList(),
    val partnerships: List<PartnershipEvent> = emptyList(),
    val activePartnershipRuns: Int = 0,
    val activePartnershipBalls: Int = 0,
    val activePartnershipBatter1Runs: Int = 0,
    val activePartnershipBatter1Balls: Int = 0,
    val activePartnershipBatter2Runs: Int = 0,
    val activePartnershipBatter2Balls: Int = 0
) {
    // Computed Strike properties
    val batterStriker: BatterState
        get() = if (batter1.name == strikerName) batter1 else batter2
    val batterNonStriker: BatterState
        get() = if (batter1.name == strikerName) batter2 else batter1
    val nonStrikerName: String
        get() = batterNonStriker.name
    // Computed Properties for Business Logic and UI Binding
    val isMatchCompleted: Boolean
        get() {
            if (!isInnings2 || firstInningsTarget == null) return false
            val needed = firstInningsTarget - runs
            val totalMatchBalls = matchTotalOvers * ballsPerOver
            val remainingBalls = totalMatchBalls - totalBalls
            return needed <= 0 || remainingBalls <= 0 || wickets >= matchTotalWickets
        }

    val isInnings1Completed: Boolean
        get() {
            if (isInnings2) return false
            val totalMatchBalls = matchTotalOvers * ballsPerOver
            return totalBalls >= totalMatchBalls || wickets >= matchTotalWickets
        }

    val matchResultStatus: String
        get() {
            if (!isInnings2 || firstInningsTarget == null) return ""
            val needed = firstInningsTarget - runs
            return if (needed <= 0) {
                "$battingTeamName Won by ${matchTotalWickets - wickets} Wickets! 🎉"
            } else if (totalBalls >= matchTotalOvers * ballsPerOver || wickets >= matchTotalWickets) {
                if (needed == 1) {
                    "Match Tied! 🤝"
                } else {
                    "$bowlingTeamName Won by ${needed - 1} runs! 🏆"
                }
            } else {
                ""
            }
        }

    val formattedOvers: String
        get() {
            val overs = totalBalls / ballsPerOver
            val balls = totalBalls % ballsPerOver
            return if (balls == 0 && overs > 0) "${overs}.0" else "$overs.$balls"
        }

    val formattedBowlerOvers: String
        get() {
            val overs = bowler.balls / ballsPerOver
            val balls = bowler.balls % ballsPerOver
            return if (balls == 0 && overs > 0) "${overs}.0" else "$overs.$balls"
        }

    val runRateStr: String
        get() {
            if (totalBalls <= 0) return "0.00"
            return String.format("%.2f", (runs.toFloat() / (totalBalls.toFloat() / ballsPerOver.toFloat())))
        }

    val requiredRunRateStr: String
        get() {
            if (!isInnings2 || firstInningsTarget == null) return "0.00"
            val needed = firstInningsTarget - runs
            val totalMatchBalls = matchTotalOvers * ballsPerOver
            val remainingBalls = maxOf(0, totalMatchBalls - totalBalls)
            if (remainingBalls <= 0) {
                return if (needed <= 0) "0.00" else "∞"
            }
            return String.format("%.2f", (needed.toFloat() / (remainingBalls.toFloat() / ballsPerOver.toFloat())))
        }

    val remainingBalls: Int
        get() {
            val totalMatchBalls = matchTotalOvers * ballsPerOver
            return maxOf(0, totalMatchBalls - totalBalls)
        }

    val neededRuns: Int
        get() {
            if (firstInningsTarget == null) return 0
            return maxOf(0, firstInningsTarget - runs)
        }
}
