package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for admin fixtures — PRD §21-§24
 * Stores match fixtures locally for offline access.
 */
@Entity(tableName = "admin_fixtures")
data class AdminFixtureEntity(
    @PrimaryKey val id: String,
    val serverId: Int? = null,
    val tournamentId: String,

    // ── Stage Context (PRD §24) ──
    val stageId: String? = null,
    val stageName: String? = null,

    // ── Match Info (PRD §24) ──
    val roundNumber: Int = 1,
    val roundName: String = "",
    val matchNumber: Int = 1,

    // ── Teams (PRD §24) ──
    val homeTeamId: String = "",
    val homeTeamName: String = "",
    val awayTeamId: String = "",
    val awayTeamName: String = "",

    // ── Schedule (PRD §21) ──
    val scheduledDate: String? = null,
    val scheduledTime: String? = null,
    val scheduledAt: String? = null,
    val venue: String? = null,
    val city: String? = null,

    // ── Match Type (PRD §25) ──
    // normal, series, knockout, qualifier, eliminator, quarter_final, semi_final, final
    val matchType: String = "normal",

    // ── Officials (PRD §24) ──
    val umpire1: String? = null,
    val umpire2: String? = null,

    // ── Status (PRD §23) ──
    // scheduled, upcoming, live, completed, postponed, cancelled, abandoned, no_result
    val status: String = "scheduled",
    val tossWinner: String? = null,
    val tossDecision: String? = null,

    // ── Sync ──
    val syncStatus: String = "pending",
    val updatedAt: Long = System.currentTimeMillis()
)
