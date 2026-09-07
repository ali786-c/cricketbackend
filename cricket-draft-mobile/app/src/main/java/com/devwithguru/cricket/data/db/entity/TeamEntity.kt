package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey
    val id: String,
    val serverId: Int? = null,
    val name: String,
    val shortName: String = "",
    val tournamentId: String = "",
    val tournamentName: String = "",
    val playerCount: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0,
    val foundedYear: String = "",
    val captainName: String? = null,
    val viceCaptainName: String? = null,
    val wicketkeeperName: String? = null,
    val creatorId: Int? = null,

    // ── Sync ──
    val updatedAt: Long = System.currentTimeMillis()
)
