package com.devwithguru.cricket.domain.model

/**
 * Stage types — PRD §13
 * Each tournament can contain multiple stages of different types.
 */
enum class StageType(val displayName: String, val description: String) {
    POINTS_TABLE("Points Table / League", "Every team plays every other team"),
    KNOCKOUT("Knockout", "Single elimination — loser goes home"),
    MATCH("Single Match", "One-off match between two teams"),
    SERIES("Series", "Multiple matches between same teams"),
    QUALIFIER("Qualifier", "Special qualification match"),
    ELIMINATOR("Eliminator", "Loser is eliminated from tournament"),
    QUARTER_FINAL("Quarter Final", "Quarter-final stage"),
    SEMI_FINAL("Semi Final", "Semi-final stage"),
    FINAL("Final", "Championship match"),
    CUSTOM("Custom", "Configure your own stage rules")
}

/**
 * Competition structure — PRD §48
 */
enum class CompetitionStructure(val displayName: String, val description: String) {
    LEAGUE("League", "Every team plays every other team"),
    KNOCKOUT("Knockout", "Single elimination bracket"),
    GROUP_PLUS_PLAYOFFS("Group + Playoffs", "Groups then knockout stages"),
    CUSTOM("Custom", "I'll configure stages myself")
}
