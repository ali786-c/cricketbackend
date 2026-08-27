package com.devwithguru.cricket.domain.model

/**
 * Stage domain model — PRD §12-§18
 * A stage is a competition structure within a tournament.
 * A tournament can contain multiple stages.
 *
 * Example:
 * Tournament
 *   ├── Group A (StageType.POINTS_TABLE)
 *   ├── Group B (StageType.POINTS_TABLE)
 *   ├── Qualifier 1 (StageType.QUALIFIER)
 *   ├── Eliminator (StageType.ELIMINATOR)
 *   ├── Qualifier 2 (StageType.QUALIFIER)
 *   └── Final (StageType.FINAL)
 */
data class Stage(
    // ── Identity ──
    val id: String,
    val tournamentId: String,
    val name: String,

    // ── Type (PRD §13) ──
    val type: StageType = StageType.POINTS_TABLE,

    // ── Order in tournament ──
    val order: Int = 0,

    // ── Configuration (PRD §15-§16) ──
    val numberOfTeams: Int = 0,
    val matchesPerTeam: Int = 0,

    // ── Points System (PRD §15) ──
    val pointsForWin: Int = 2,
    val pointsForTie: Int = 1,
    val pointsForNoResult: Int = 1,
    val pointsForLoss: Int = 0,

    // ── Qualification Rules (PRD §16) ──
    // "top_1", "top_2", "top_4", "custom"
    val qualificationRule: String = "top_2",
    val qualificationCount: Int = 2,

    // ── Status ──
    // draft, active, completed
    val status: String = "draft",

    // ── Stats ──
    val teamsCount: Int = 0,
    val matchesCount: Int = 0,
    val completedMatches: Int = 0,

    // ── Sync ──
    val serverId: Int? = null,
    val syncStatus: String = "synced"
)
