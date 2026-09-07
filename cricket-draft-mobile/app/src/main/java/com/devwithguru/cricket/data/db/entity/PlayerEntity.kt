package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String, // tournament_player_id
    val playerProfileId: Int?,
    val name: String,
    val role: String?,
    val battingStyle: String?,
    val bowlingStyle: String?,
    val city: String?,
    val photoPath: String?,
    val isRegistered: Boolean = false,
    val teamId: String? = null,
    val status: String? = "approved"
)
