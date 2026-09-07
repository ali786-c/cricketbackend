package com.devwithguru.cricket

import android.os.Bundle
import android.widget.Toast
import com.devwithguru.cricket.R
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.devwithguru.cricket.ui.feature.auth.LoginScreen
import com.devwithguru.cricket.ui.feature.auth.PlayerOnboardingScreen
import com.devwithguru.cricket.ui.feature.home.UnifiedHomeScreen
import com.devwithguru.cricket.ui.feature.match.screens.CreateMatchScreen
import com.devwithguru.cricket.ui.feature.tournament.AddTeamScreen
import com.devwithguru.cricket.ui.feature.tournament.CreateStageScreen
import com.devwithguru.cricket.ui.feature.tournament.ScheduleMatchScreen
import com.devwithguru.cricket.ui.feature.tournament.CreateTournamentScreen
import com.devwithguru.cricket.ui.feature.tournament.MyTournamentsScreen
import com.devwithguru.cricket.ui.feature.tournament.TournamentHubScreen
import com.devwithguru.cricket.ui.feature.tournament.TournamentSetupScreen
import com.devwithguru.cricket.ui.feature.tournament.TournamentRegistrationScreen
import com.devwithguru.cricket.ui.feature.tournament.DraftRoomScreen
import com.devwithguru.cricket.ui.feature.team.TeamDetailScreen
import com.devwithguru.cricket.ui.feature.match.toss.TossLineupScreen
import com.devwithguru.cricket.ui.feature.match.toss.TossScreen
import com.devwithguru.cricket.ui.feature.match.scorer.LiveScorerScreen
import com.devwithguru.cricket.ui.feature.match.screens.MatchCenterScreen
import com.devwithguru.cricket.ui.feature.player.PlayerProfileScreen
import com.devwithguru.cricket.ui.feature.home.GlobalSearchScreen
import com.devwithguru.cricket.ui.feature.home.MyTeamsScreen
import com.devwithguru.cricket.ui.feature.match.screens.MatchEditorScreen
import com.devwithguru.cricket.ui.feature.match.RecentMatchesScreen
import com.devwithguru.cricket.domain.model.ScheduledFixture
import com.devwithguru.cricket.ui.viewmodels.MainViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.devwithguru.cricket.ui.feature.match.scorer.LiveScorerViewModel
import com.devwithguru.cricket.ui.components.SyncStatusIndicator
import com.devwithguru.cricket.ui.theme.CricketTheme
import com.devwithguru.cricket.ui.navigation.Screen
import com.devwithguru.cricket.ui.viewmodels.NavigationViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            CricketTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navigationViewModel = remember { NavigationViewModel() }
                    val currentScreen = navigationViewModel.currentScreen
                    val navigationStack = navigationViewModel.navigationStack
                    var loggedInEmail by remember { mutableStateOf("") }
                    val scorerViewModel: LiveScorerViewModel = hiltViewModel()
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val currentFixture by mainViewModel.currentFixture.collectAsState()
                    val context = LocalContext.current

                    val msgLoggedOut = stringResource(R.string.msg_logged_out)
                    val msgMatchSetupComplete = stringResource(R.string.msg_match_setup_complete)
                    val msgMatchCompletedResultsSaved = stringResource(R.string.msg_match_completed_results_saved)

                    // For formatted strings, we'll need to use context.getString in the lambda,
                    // but maybe the lint rule allows it if we don't have another choice,
                    // OR we can get the format string using stringResource and then format it.
                    val msgLoggedInFormat = stringResource(R.string.msg_logged_in)
                    val msgRegisteredFormat = stringResource(R.string.msg_registered)
                    val msgCreatedTournamentFormat = stringResource(R.string.msg_created_tournament)
                    val msgInningsCompleteFormat = stringResource(R.string.msg_innings_complete)

                    // Intercept system back button clicks dynamically
                    BackHandler(enabled = navigationStack.size > 1 && currentScreen != Screen.Home) {
                        navigationViewModel.navigateBack()
                    }

                    when (val screen = currentScreen) {
                        Screen.Login -> {
                            LoginScreen(
                                onLoginSuccess = { email ->
                                    loggedInEmail = email
                                    navigationViewModel.navigateBack()
                                    Toast.makeText(context, String.format(msgLoggedInFormat, email), Toast.LENGTH_SHORT).show()
                                },
                                onNavigateToRegister = {
                                    navigationViewModel.navigateTo(Screen.Onboarding)
                                }
                            )
                        }
                        Screen.Onboarding -> {
                            PlayerOnboardingScreen(
                                onSubmitRegistration = { name, role, batting, bowling, city, bio ->
                                    Toast.makeText(context, String.format(msgRegisteredFormat, name, role), Toast.LENGTH_LONG).show()
                                    navigationViewModel.navigateBack()
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen == Screen.Onboarding) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        Screen.Home -> {
                            UnifiedHomeScreen(
                                userName = loggedInEmail.ifBlank { "Ahmed Ali" },
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { isDarkTheme = it },
                                onNavigateToCreateMatch = {
                                    navigationViewModel.navigateTo(Screen.CreateMatch())
                                },
                                onNavigateToCreateTournament = {
                                    navigationViewModel.navigateTo(Screen.CreateTournament)
                                },
                                onNavigateToMyTournaments = {
                                    navigationViewModel.navigateTo(Screen.MyTournaments)
                                },
                                onNavigateToMyTeams = {
                                    navigationViewModel.navigateTo(Screen.MyTeams)
                                },
                                onNavigateToPlayerProfile = {
                                    navigationViewModel.navigateTo(Screen.PlayerProfile("p1"))
                                },
                                onNavigateToTournamentHub = { id ->
                                    navigationViewModel.navigateTo(Screen.TournamentHub(id))
                                },
                                onNavigateToMatchCenter = { id ->
                                    navigationViewModel.navigateTo(Screen.MatchCenter(matchId = id, isScorer = false))
                                },
                                onNavigateToRecentMatches = {
                                    navigationViewModel.navigateTo(Screen.RecentMatches)
                                },
                                onSearchClick = {
                                    navigationViewModel.navigateTo(Screen.GlobalSearch)
                                },
                                onLogout = {
                                    loggedInEmail = ""
                                    navigationViewModel.clearAndNavigateTo(Screen.Login)
                                    Toast.makeText(context, msgLoggedOut, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        is Screen.CreateMatch -> {
                            CreateMatchScreen(
                                tournamentId = screen.tournamentId,
                                defaultWickets = screen.defaultWickets,
                                onCreateMatchSuccess = { matchId, home, away, overs, ballType, date, time ->
                                    navigationViewModel.navigateTo(Screen.Toss(matchId, home, away))
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.CreateMatch) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        Screen.CreateTournament -> {
                            CreateTournamentScreen(
                                onCreateTournamentSuccess = { tournamentId, name, hasDraft ->
                                    Toast.makeText(context, "Tournament \"$name\" created!", Toast.LENGTH_SHORT).show()
                                    if (hasDraft) {
                                        navigationViewModel.updateCurrentScreen(Screen.CreateTournament, Screen.TournamentSetup(tournamentId, true))
                                    } else {
                                        navigationViewModel.updateCurrentScreen(Screen.CreateTournament, Screen.TournamentHub(tournamentId))
                                    }
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.CreateTournament) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        is Screen.TournamentSetup -> {
                            TournamentSetupScreen(
                                tournamentId = screen.tournamentId,
                                onNavigateToDraft = { tid ->
                                    navigationViewModel.navigateTo(Screen.DraftRoom(tid))
                                },
                                onNavigateToTournamentHub = { tid ->
                                    navigationViewModel.navigateTo(Screen.TournamentHub(tid))
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.TournamentSetup) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        is Screen.TournamentRegistration -> {
                            TournamentRegistrationScreen(
                                tournamentId = screen.tournamentId,
                                tournamentName = screen.tournamentName,
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.TournamentRegistration) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        is Screen.DraftRoom -> {
                            DraftRoomScreen(
                                tournamentId = screen.tournamentId,
                                isAdmin = screen.isAdmin,
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.DraftRoom) {
                                        navigationViewModel.navigateBack()
                                    }
                                },
                                onDraftCompleted = { tournamentId ->
                                    Toast.makeText(context, "Draft completed!", Toast.LENGTH_SHORT).show()
                                    navigationViewModel.navigateTo(Screen.TournamentHub(tournamentId))
                                }
                            )
                        }
                        Screen.MyTournaments -> {
                            MyTournamentsScreen(
                                onNavigateToCreateTournament = {
                                    navigationViewModel.navigateTo(Screen.CreateTournament)
                                },
                                onNavigateToTournament = { id, status, hasDraft ->
                                    if (hasDraft && (status == "draft" || status == "registration" || status == "ready")) {
                                        navigationViewModel.navigateTo(Screen.TournamentSetup(id, true))
                                    } else {
                                        navigationViewModel.navigateTo(Screen.TournamentHub(id))
                                    }
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen == Screen.MyTournaments) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        Screen.MyTeams -> {
                            MyTeamsScreen(
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen == Screen.MyTeams) {
                                        navigationViewModel.navigateBack()
                                    }
                                },
                                onNavigateToTeamDetail = { teamId ->
                                    navigationViewModel.navigateTo(Screen.TeamDetail(teamId))
                                },
                                onAddTeam = {
                                    // Navigate to AddTeam screen - for now, use a default tournament id or create new tournament flow
                                    Toast.makeText(context, "Create or add team", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        is Screen.TournamentHub -> {
                            TournamentHubScreen(
                                tournamentId = screen.tournamentId,
                                initialTab = screen.initialTab,
                                onTabChanged = { newTab ->
                                    navigationViewModel.updateCurrentScreen(
                                        oldScreen = screen,
                                        newScreen = screen.copy(initialTab = newTab)
                                    )
                                },
                                onNavigateToTeamDetail = { teamId ->
                                    navigationViewModel.navigateTo(Screen.TeamDetail(teamId))
                                },
                                 onNavigateToMatchCenter = { matchId, isScorer ->
                                     navigationViewModel.navigateTo(Screen.MatchCenter(matchId = matchId, isScorer = isScorer))
                                },
                                 onStartMatch = { matchId, home, away, status, tossWinner, tossDecision ->
                                     if (status.lowercase() == "toss_completed" && !tossWinner.isNullOrBlank() && !tossDecision.isNullOrBlank()) {
                                         navigationViewModel.navigateTo(
                                             Screen.TossLineup(
                                                 matchId = matchId,
                                                 homeTeam = home,
                                                 awayTeam = away,
                                                 tossWinner = tossWinner,
                                                 tossDecision = tossDecision
                                             )
                                         )
                                     } else {
                                         navigationViewModel.navigateTo(Screen.Toss(matchId, home, away))
                                     }
                                 },
                                onScheduleMatch = { tournamentId ->
                                    navigationViewModel.navigateTo(Screen.ScheduleMatch(tournamentId))
                                },
                                onCreateGroup = { tournamentId ->
                                    Toast.makeText(context, "Create Group coming soon", Toast.LENGTH_SHORT).show()
                                },
                                onAddTeam = { tournamentId ->
                                    navigationViewModel.navigateTo(Screen.AddTeam(tournamentId))
                                },
                                onCreateStage = { tournamentId ->
                                    navigationViewModel.navigateTo(Screen.CreateStage(tournamentId))
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.TournamentHub) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        is Screen.AddTeam -> {
                            // Get existing teams from ViewModel for duplicate check
                            val setupVm: com.devwithguru.cricket.ui.feature.tournament.TournamentSetupViewModel = hiltViewModel()
                            val existingTeams by setupVm.teams.collectAsState()

                            AddTeamScreen(
                                tournamentId = screen.tournamentId,
                                existingTeams = existingTeams,
                                onTeamCreated = { name, shortName, captain, viceCaptain, manager ->
                                    setupVm.createTeam(
                                        tournamentId = screen.tournamentId,
                                        name = name,
                                        shortName = shortName,
                                        captainName = captain,
                                        viceCaptainName = viceCaptain,
                                        managerName = manager
                                    )
                                    navigationViewModel.navigateBack()
                                },
                                onTeamJoinedByCode = { code ->
                                    Toast.makeText(context, "Team code '$code' — feature coming soon", Toast.LENGTH_SHORT).show()
                                },
                                onNavigateBack = {
                                    navigationViewModel.navigateBack()
                                }
                            )
                        }
                        is Screen.CreateStage -> {
                            val stageVm: com.devwithguru.cricket.ui.feature.tournament.StageViewModel = hiltViewModel()
                            LaunchedEffect(screen.tournamentId) {
                                stageVm.setTournamentId(screen.tournamentId)
                            }

                            CreateStageScreen(
                                tournamentId = screen.tournamentId,
                                stageNumber = screen.stageNumber,
                                onStageCreated = { name, type, numTeams, matchesPerTeam, pointsWin, pointsTie, qualRule, qualCount ->
                                    stageVm.createStage(
                                        name = name,
                                        type = type,
                                        numberOfTeams = numTeams,
                                        matchesPerTeam = matchesPerTeam,
                                        pointsWin = pointsWin,
                                        pointsTie = pointsTie,
                                        qualificationRule = qualRule,
                                        qualificationCount = qualCount
                                    )
                                    navigationViewModel.navigateBack()
                                },
                                onNavigateBack = {
                                    navigationViewModel.navigateBack()
                                }
                            )
                        }
                        is Screen.ScheduleMatch -> {
                            val fixtureVm: com.devwithguru.cricket.ui.feature.tournament.FixtureViewModel = hiltViewModel()
                            LaunchedEffect(screen.tournamentId) {
                                fixtureVm.setTournamentId(screen.tournamentId)
                            }
                            val scheduleStages by fixtureVm.stages.collectAsState()
                            val tournamentVm: com.devwithguru.cricket.ui.feature.tournament.TournamentViewModel = hiltViewModel()
                            LaunchedEffect(screen.tournamentId) { tournamentVm.loadTournament(screen.tournamentId) }
                            val scheduleTeams by tournamentVm.teams.collectAsState()

                            ScheduleMatchScreen(
                                tournamentId = screen.tournamentId,
                                stages = scheduleStages,
                                teams = scheduleTeams,
                                onMatchScheduled = { stageId, homeId, homeName, awayId, awayName, date, time, venue, matchType ->
                                    fixtureVm.scheduleMatch(
                                        stageId = stageId,
                                        stageName = scheduleStages.find { it.id == stageId }?.name,
                                        homeTeamId = homeId,
                                        homeTeamName = homeName,
                                        awayTeamId = awayId,
                                        awayTeamName = awayName,
                                        date = date,
                                        time = time,
                                        venue = venue,
                                        matchType = matchType
                                    )
                                    Toast.makeText(context, "$homeName vs $awayName scheduled!", Toast.LENGTH_SHORT).show()
                                    navigationViewModel.navigateBack()
                                },
                                onNavigateBack = {
                                    navigationViewModel.navigateBack()
                                }
                            )
                        }
                        is Screen.TeamDetail -> {
                            TeamDetailScreen(
                                teamId = screen.teamId,
                                initialTab = screen.initialTab,
                                onTabChanged = { newTab ->
                                    navigationViewModel.updateCurrentScreen(
                                        oldScreen = screen,
                                        newScreen = screen.copy(initialTab = newTab)
                                    )
                                },
                                onNavigateToPlayerDetail = { playerId ->
                                    navigationViewModel.navigateTo(Screen.PlayerProfile(playerId))
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.TeamDetail) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        is Screen.Toss -> {
                            TossScreen(
                                homeTeamName = screen.homeTeam,
                                awayTeamName = screen.awayTeam,
                                onTossComplete = { winner, decision ->
                                    mainViewModel.saveTossDetails(screen.matchId, winner, decision)
                                    navigationViewModel.navigateTo(
                                        Screen.TossLineup(
                                            matchId = screen.matchId,
                                            homeTeam = screen.homeTeam,
                                            awayTeam = screen.awayTeam,
                                            tossWinner = winner,
                                            tossDecision = decision
                                        )
                                    )
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.Toss) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        is Screen.TossLineup -> {
                            LaunchedEffect(screen.matchId) { mainViewModel.loadFixture(screen.matchId) }
                            TossLineupScreen(
                                matchId = screen.matchId,
                                homeTeamName = screen.homeTeam,
                                awayTeamName = screen.awayTeam,
                                tossWinner = screen.tossWinner,
                                tossDecision = screen.tossDecision,
                                onStartMatchSuccess = { winner, decision, homeLineup, awayLineup ->
                                     mainViewModel.startMatch(
                                         matchId = screen.matchId,
                                         tossWinner = winner,
                                         tossDecision = decision,
                                         homeSquad = homeLineup,
                                         awaySquad = awayLineup
                                     ) {
                                         Toast.makeText(context, msgMatchSetupComplete, Toast.LENGTH_SHORT).show()
                                         navigationViewModel.navigateTo(
                                             Screen.MatchCenter(
                                                 matchId = screen.matchId,
                                                 isScorer = true,
                                                 homeSquadList = homeLineup,
                                                 awaySquadList = awayLineup
                                             )
                                         )
                                         navigationViewModel.removeTossAndLineup()
                                     }
                                 },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.TossLineup) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        is Screen.MatchCenter -> {
                            LaunchedEffect(screen.matchId) { mainViewModel.loadFixture(screen.matchId) }
                            MatchCenterScreen(
                                matchId = screen.matchId,
                                isScorer = screen.isScorer,
                                homeSquadList = screen.homeSquadList,
                                awaySquadList = screen.awaySquadList,
                                scorerViewModel = scorerViewModel,
                                onNavigateBack = {
                                    val fixture = mainViewModel.getFixture(screen.matchId) ?: currentFixture?.takeIf { it.id == screen.matchId }
                                    fixture?.let { f ->
                                        if (f.status.lowercase() != "completed") {
                                            f.status = "Live"
                                            mainViewModel.updateFixture(f)
                                        }
                                    }
                                    if (navigationViewModel.currentScreen is Screen.MatchCenter) {
                                        navigationViewModel.navigateBack()
                                    }
                                },
                                onNavigateToMatchEditor = {
                                    navigationViewModel.navigateTo(Screen.MatchEditor(screen.matchId))
                                },
                                onDeclareInnings = { runs, wickets, overs ->
                                    val fixture = mainViewModel.getFixture(screen.matchId) ?: currentFixture?.takeIf { it.id == screen.matchId }
                                    fixture?.let { f ->
                                        if (f.currentInnings == 1) {
                                            // Transition to 2nd Innings
                                            f.currentInnings = 2
                                            f.firstInningsRuns = runs
                                            f.firstInningsWickets = wickets

                                            // Reset score trackers for the 2nd Innings chase
                                            f.currentRuns = 0
                                            f.currentWickets = 0
                                            f.oversBowled = "0.0"
                                            f.strikerName = ""
                                            f.nonStrikerName = ""

                                            Toast.makeText(context, String.format(msgInningsCompleteFormat, runs + 1), Toast.LENGTH_LONG).show()

                                            // Update the current screen state in place on the stack to reflect the new innings parameters
                                            navigationViewModel.updateCurrentScreen(screen, screen.copy(isScorer = true))
                                        } else {
                                            // Innings 2 declared - Match Completed
                                            f.status = "Completed"
                                            f.currentRuns = runs
                                            f.currentWickets = wickets
                                            f.oversBowled = overs
                                            Toast.makeText(context, msgMatchCompletedResultsSaved, Toast.LENGTH_LONG).show()
                                            // Go back to previous screen (Hub or Home)
                                            navigationViewModel.navigateBack()
                                        }
                                        // Persist fixture changes to Room
                                        mainViewModel.updateFixture(f)
                                    }
                                }
                            )
                        }
                        is Screen.PlayerProfile -> {
                            PlayerProfileScreen(
                                playerId = screen.playerId,
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.PlayerProfile) {
                                        navigationViewModel.navigateBack()
                                    }
                                },
                                onStartScheduledMatch = { fixture ->
                                    if (fixture.status.lowercase() == "live") {
                                        // Resume scoring directly
                                        navigationViewModel.navigateTo(
                                            Screen.MatchCenter(
                                                matchId = fixture.id,
                                                isScorer = true,
                                                homeSquadList = fixture.homeSquad,
                                                awaySquadList = fixture.awaySquad
                                            )
                                        )
                                    } else {
                                        fixture.status = "Live"
                                        navigationViewModel.navigateTo(
                                            Screen.Toss(
                                                matchId = fixture.id,
                                                homeTeam = fixture.homeTeam,
                                                awayTeam = fixture.awayTeam
                                            )
                                        )
                                    }
                                }
                            )
                        }
                        Screen.GlobalSearch -> {
                            GlobalSearchScreen(
                                onNavigateToPlayerProfile = { playerId ->
                                    navigationViewModel.navigateTo(Screen.PlayerProfile(playerId))
                                },
                                onNavigateToTeamDetail = { teamId ->
                                    navigationViewModel.navigateTo(Screen.TeamDetail(teamId))
                                },
                                onNavigateToTournamentHub = { tournamentId ->
                                    navigationViewModel.navigateTo(Screen.TournamentHub(tournamentId))
                                },
                                onNavigateToMatchCenter = { matchId ->
                                    navigationViewModel.navigateTo(Screen.MatchCenter(matchId = matchId, isScorer = false))
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen == Screen.GlobalSearch) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        is Screen.MatchEditor -> {
                            MatchEditorScreen(
                                matchId = screen.matchId,
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen is Screen.MatchEditor) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                        Screen.RecentMatches -> {
                            RecentMatchesScreen(
                                 onNavigateToMatchCenter = { matchId ->
                                     navigationViewModel.navigateTo(Screen.MatchCenter(matchId = matchId, isScorer = false))
                                 },
                                onNavigateToTournamentHub = { tournamentId ->
                                    navigationViewModel.navigateTo(Screen.TournamentHub(tournamentId))
                                },
                                onNavigateBack = {
                                    if (navigationViewModel.currentScreen == Screen.RecentMatches) {
                                        navigationViewModel.navigateBack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
