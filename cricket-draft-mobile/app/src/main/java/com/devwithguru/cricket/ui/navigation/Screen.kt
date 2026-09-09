package com.devwithguru.cricket.ui.navigation

sealed interface Screen {
    object Login : Screen
    object Register : Screen
    object Onboarding : Screen
    object Home : Screen
    data class CreateMatch(val tournamentId: String? = null, val defaultWickets: Int = 10) : Screen
    object CreateTournament : Screen
    object MyTournaments : Screen
    object MyTeams : Screen
    data class AddTeam(val tournamentId: String) : Screen
    data class CreateStage(val tournamentId: String, val stageNumber: Int = 1) : Screen
    data class ScheduleMatch(val tournamentId: String) : Screen
    data class TournamentHub(val tournamentId: String, val initialTab: Int = 0) : Screen
    data class TournamentSetup(val tournamentId: String, val hasDraft: Boolean = true) : Screen
    data class TournamentRegistration(val tournamentId: String, val tournamentName: String) : Screen
    data class DraftRoom(val tournamentId: String, val isAdmin: Boolean = true) : Screen
    data class TeamDetail(val teamId: String, val initialTab: Int = 0) : Screen
    data class Toss(val matchId: String, val homeTeam: String, val awayTeam: String) : Screen
    data class TossLineup(
        val matchId: String,
        val homeTeam: String,
        val awayTeam: String,
        val tossWinner: String,
        val tossDecision: String
    ) : Screen
    data class MatchCenter(
        val matchId: String,
        val isScorer: Boolean,
        val homeSquadList: List<String> = emptyList(),
        val awaySquadList: List<String> = emptyList()
    ) : Screen
    data class PlayerProfile(val playerId: String) : Screen
    object GlobalSearch : Screen
    data class MatchEditor(val matchId: String) : Screen
    object RecentMatches : Screen
}
