package com.devwithguru.cricket.domain.model

/**
 * Tournament domain model — PRD §6-§8
 * Represents a complete cricket tournament with all configuration.
 */
data class Tournament(
    // ── Identity ──
    val id: String,
    val serverId: Int? = null,
    val name: String,

    // ── Basic Info (PRD §6.1) ──
    val description: String = "",
    val logo: String? = null,
    val coverImage: String? = null,
    val organizerName: String = "",
    val contactInfo: String = "",

    // ── Location (PRD §6.1) ──
    val city: String,
    val venue: String = "",

    // ── Schedule (PRD §6.1) ──
    val season: String,
    val startDate: String,
    val endDate: String,

    // ── Game Format (PRD §7 / §48) ──
    // T10, T20, ODI, Test, Tape Ball, Tennis Ball, Hard Ball, Indoor Cricket, Custom
    val ballType: String = "Tennis Ball",
    val oversPerInnings: Int = 20,
    val wicketsPerTeam: Int = 10,

    // ── Competition Structure (PRD §48) ──
    // League, Knockout, Group + Playoffs, Custom
    val competitionStructure: String = "League",

    // ── Visibility (PRD §8) ──
    // public, private, invite_only, code
    val visibility: String = "public",
    val tournamentCode: String? = null,

    // ── Draft Settings (existing) ──
    val hasDraft: Boolean = false,
    val squadSize: Int = 11,
    val pickDuration: Int = 60,

    // ── Status (PRD §5 lifecycle) ──
    val status: String = "upcoming",
    // draft → registration → setup → scheduled → live → completed → archived

    // ── Stats ──
    val teamCount: Int = 0
)
