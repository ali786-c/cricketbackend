package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixtures")
data class FixtureEntity(
    @PrimaryKey
    val id: String,
    val homeTeam: String,
    val awayTeam: String,
    val overs: Int,
    val ballType: String,
    val matchType: String,
    val wickets: Int,
    val venue: String,
    val date: String,
    val time: String,
    val status: String = "Scheduled",
    val tossWinner: String = "",
    val tossDecision: String = "",
    val currentRuns: Int = 0,
    val currentWickets: Int = 0,
    val oversBowled: String = "0.0",
    val strikerName: String = "",
    val nonStrikerName: String = "",
    val bowlerName: String = "",
    val currentInnings: Int = 1,
    val homeSquad: String = "[]",        // JSON serialized List<String>
    val awaySquad: String = "[]",        // JSON serialized List<String>
    val firstInningsRuns: Int? = null,
    val firstInningsWickets: Int? = null,
    val firstInningsBatsmen: String = "[]",   // JSON serialized List<BatterState>
    val firstInningsBowlers: String = "[]",   // JSON serialized List<BowlerState>
    val secondInningsBatsmen: String = "[]",
    val secondInningsBowlers: String = "[]",
    val firstInningsFOW: String = "[]",       // JSON serialized List<WicketEvent>
    val firstInningsPartnerships: String = "[]",
    val secondInningsFOW: String = "[]",
    val secondInningsPartnerships: String = "[]",
    val activePartnershipRuns: Int = 0,
    val activePartnershipBalls: Int = 0,
    val firstInningsExtras: Int = 0,
    val secondInningsExtras: Int = 0,
    val firstInningsDotBalls: Int = 0,
    val secondInningsDotBalls: Int = 0
)
