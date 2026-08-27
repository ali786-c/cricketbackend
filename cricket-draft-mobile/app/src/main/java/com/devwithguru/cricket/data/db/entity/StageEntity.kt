package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for Stage — PRD §12-§18
 * Stores stage data locally for offline access.
 */
@Entity(tableName = "stages")
data class StageEntity(
    @PrimaryKey
    val id: String,
    val tournamentId: String,
    val name: String,

    // ── Type (PRD §13) ──
    // POINTS_TABLE, KNOCKOUT, MATCH, SERIES, QUALIFIER, ELIMINATOR,
    // QUARTER_FINAL, SEMI_FINAL, FINAL, CUSTOM
    val type: String = "POINTS_TABLE",

    // ── Order ──
    val order: Int = 0,

    // ── Configuration (PRD §15) ──
    val numberOfTeams: Int = 0,
    val matchesPerTeam: Int = 0,

    // ── Points System (PRD §15) ──
    val pointsForWin: Int = 2,
    val pointsForTie: Int = 1,
    val pointsForNoResult: Int = 1,
    val pointsForLoss: Int = 0,

    // ── Qualification (PRD §16) ──
    val qualificationRule: String = "top_2",
    val qualificationCount: Int = 2,

    // ── Status ──
    val status: String = "draft",

    // ── Stats ──
    val teamsCount: Int = 0,
    val matchesCount: Int = 0,
    val completedMatches: Int = 0,

    // ── Sync ──
    val serverId: Int? = null,
    val syncStatus: String = "pending"
)
