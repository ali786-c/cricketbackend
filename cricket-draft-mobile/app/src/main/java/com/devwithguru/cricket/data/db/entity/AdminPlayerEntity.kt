package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_players")
data class AdminPlayerEntity(
    @PrimaryKey val id: String,
    val serverId: Int? = null,
    val tournamentId: String,
    val playerName: String,
    val role: String = "",
    val city: String = "",
    val status: String = "approved",
    val isManuallyAdded: Boolean = true,
    val syncStatus: String = "pending"
)
