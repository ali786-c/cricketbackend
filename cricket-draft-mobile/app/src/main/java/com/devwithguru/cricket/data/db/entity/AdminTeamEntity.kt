package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for admin teams — PRD §9-§11
 * Stores team data locally for offline access.
 */
@Entity(tableName = "admin_teams")
data class AdminTeamEntity(
    @PrimaryKey val id: String,
    val serverId: Int? = null,
    val tournamentId: String,
    val name: String,
    val shortName: String = "",

    // ── Team Info (PRD §10) ──
    val logo: String? = null,
    val captainUserId: Int? = null,
    val captainUserName: String? = null,
    val viceCaptainName: String? = null,
    val managerName: String? = null,
    val wicketkeeperName: String? = null,

    // ── Status (PRD §11) ──
    val status: String = "pending",
    // pending, approved, active, eliminated, champion, runner_up, withdrawn

    // ── Stats ──
    val playerCount: Int = 0,

    // ── Code for joining ──
    val teamCode: String? = null,

    // ── Sync ──
    val isActive: Boolean = true,
    val syncStatus: String = "pending",
    val creatorId: Int? = null
)
