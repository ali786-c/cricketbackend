package com.devwithguru.cricket.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for Tournament — PRD §6-§8
 * Stores all tournament configuration locally for offline access.
 */
@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey
    val id: String,
    val name: String,

    // ── Basic Info (PRD §6.1) ──
    val description: String = "",
    val logo: String? = null,
    val coverImage: String? = null,
    val organizerName: String = "",
    val contactInfo: String = "",

    // ── Location ──
    val city: String,
    val venue: String = "",

    // ── Schedule ──
    val season: String,
    val startDate: String,
    val endDate: String,

    // ── Game Format (PRD §7 / §48) ──
    val ballType: String = "Tennis Ball",
    val oversPerInnings: Int = 20,

    // ── Competition Structure (PRD §48) ──
    val competitionStructure: String = "League",

    // ── Visibility (PRD §8) ──
    val visibility: String = "public",
    val tournamentCode: String? = null,

    // ── Draft Settings ──
    val hasDraft: Boolean = false,
    val squadSize: Int = 11,
    val pickDuration: Int = 60,

    // ── Status (PRD §5) ──
    val status: String = "upcoming",

    // ── Stats ──
    val teamCount: Int = 0
)
