package com.devwithguru.cricket.domain.model

/**
 * Team domain model — PRD §9-§11
 * Represents a cricket team within a tournament.
 */
data class Team(
    // ── Identity ──
    val id: String,
    val serverId: Int? = null,
    val name: String,
    val shortName: String = "",

    // ── Tournament Context ──
    val tournamentId: String = "",
    val tournamentName: String = "",

    // ── Team Info (PRD §10) ──
    val logo: String? = null,
    val captainName: String? = null,
    val viceCaptainName: String? = null,
    val wicketkeeperName: String? = null,
    val managerName: String? = null,

    // ── Status (PRD §11) ──
    // pending, approved, active, eliminated, champion, runner_up, withdrawn
    val status: String = "pending",

    // ── Stats ──
    val playerCount: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val ties: Int = 0,
    val points: Int = 0,

    // ── Code for joining (PRD §10) ──
    val teamCode: String? = null,

    // ── Meta ──
    val foundedYear: String = "",
    val syncStatus: String = "synced",
    val creatorId: Int? = null
)
