package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey val playerProfileId: Int,
    val matchesPlayed: Int,
    val runsScored: Int,
    val wicketsTaken: Int,
    val battingAverage: Double?,
    val bowlingAverage: Double?,
    val strikeRate: Double?,
    val economyRate: Double?,
    val highestScore: Int?,
    val bestBowling: String?,
    val fifties: Int,
    val hundreds: Int,
    val catches: Int,
    val updatedAt: Long
)
