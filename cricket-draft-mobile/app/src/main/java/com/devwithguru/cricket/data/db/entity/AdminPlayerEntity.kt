package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_players")
data class AdminPlayerEntity(
    @PrimaryKey val id: Int,
    val serverId: Int? = null,
    val tournamentId: Int, // Also change tournamentId to Int if it's Int on backend? Wait, no, leave tournamentId as is for now if not strictly required, but backend uses Int. 
    val playerName: String,
    val role: String = "",
    val city: String = "",
    val status: String = "approved",
    val isManuallyAdded: Boolean = true,
    val syncStatus: String = "pending"
)
