package com.devwithguru.cricket.domain.model

/**
 * Fixture domain model — PRD §21-§25
 * Represents a scheduled match within a tournament stage.
 */
data class Fixture(
    // ── Identity ──
    val id: String,
    val tournamentId: String,

    // ── Stage Context ──
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
    val venue: String? = null,
    val city: String? = null,

    // ── Match Type (PRD §25) ──
    val matchType: String = "normal",

    // ── Officials ──
    val umpire1: String? = null,
    val umpire2: String? = null,

    // ── Status (PRD §23) ──
    val status: String = "scheduled",
    val tossWinner: String? = null,
    val tossDecision: String? = null,

    // ── Sync ──
    val syncStatus: String = "synced"
)
