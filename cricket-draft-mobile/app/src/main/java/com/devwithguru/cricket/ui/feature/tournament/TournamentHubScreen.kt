package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devwithguru.cricket.domain.model.Stage
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentHubScreen(
    tournamentId: String,
    initialTab: Int = 0,
    onTabChanged: (Int) -> Unit = {},
    onNavigateToTeamDetail: (teamId: String) -> Unit,
    onNavigateToMatchCenter: (matchId: String, isScorer: Boolean) -> Unit,
    onStartMatch: (matchId: String, homeTeam: String, awayTeam: String, status: String, tossWinner: String?, tossDecision: String?) -> Unit = { _, _, _, _, _, _ -> },
    onScheduleMatch: (tournamentId: String) -> Unit = {},
    onCreateGroup: (tournamentId: String) -> Unit = {},
    onAddTeam: (tournamentId: String) -> Unit = {},
    onCreateStage: (tournamentId: String) -> Unit = {},
    onNavigateBack: () -> Unit,
    viewModel: TournamentViewModel = hiltViewModel()
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val context = LocalContext.current
    val tabTitles = listOf("Home", "Teams", "Matches", "Standings", "Statistics")
    LaunchedEffect(tournamentId) { viewModel.loadTournament(tournamentId) }
    val currentTournament by viewModel.currentTournament.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val standings by viewModel.standings.collectAsState()
    val fixtures by viewModel.fixtures.collectAsState()
    val stageVm: com.devwithguru.cricket.ui.feature.tournament.StageViewModel = hiltViewModel()
    val stages by stageVm.stages.collectAsState()

    var showCreateGroup by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var selectedTeamsForGroup by remember { mutableStateOf(setOf<Int>()) }

    if (showCreateGroup) {
        ModalBottomSheet(
            onDismissRequest = { showCreateGroup = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Create Group", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Group A") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Select Teams:", fontWeight = FontWeight.SemiBold)
                teams.forEach { team ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedTeamsForGroup = if (team.id in selectedTeamsForGroup) selectedTeamsForGroup - team.id else selectedTeamsForGroup + team.id
                        }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = team.id in selectedTeamsForGroup,
                            onCheckedChange = {
                                selectedTeamsForGroup = if (team.id in selectedTeamsForGroup) selectedTeamsForGroup - team.id else selectedTeamsForGroup + team.id
                            }
                        )
                        Text(team.name ?: "Unknown")
                    }
                }
                Button(
                    onClick = {
                        if (groupName.isNotBlank() && selectedTeamsForGroup.isNotEmpty()) {
                            stageVm.setTournamentId(tournamentId)
                            stageVm.createStage(
                                name = groupName,
                                type = com.devwithguru.cricket.domain.model.StageType.POINTS_TABLE,
                                numberOfTeams = selectedTeamsForGroup.size,
                                matchesPerTeam = 1,
                                pointsWin = 2,
                                pointsTie = 1,
                                qualificationRule = "top_2",
                                qualificationCount = 2
                            )
                            showCreateGroup = false
                            groupName = ""
                            selectedTeamsForGroup = setOf()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = groupName.isNotBlank() && selectedTeamsForGroup.isNotEmpty()
                ) {
                    Text("Create Group")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentTournament?.name ?: "Tournament",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Tournament Hub",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = 1000f
                        )
                    )
            )

            val isLoading by viewModel.isLoading.collectAsState()

            if (isLoading || currentTournament == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)) }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { 
                                selectedTab = index 
                                onTabChanged(index)
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> TournamentHomeTab(
                            tournament = currentTournament,
                            teams = teams,
                            fixtures = fixtures,
                            standings = standings,
                            stages = stages,
                            onScheduleMatch = {
                                if (teams.size < 2) {
                                    Toast.makeText(context, "Please add at least 2 teams to this tournament first!", Toast.LENGTH_LONG).show()
                                } else {
                                    onScheduleMatch(tournamentId)
                                }
                            },
                            onCreateGroup = { showCreateGroup = true },
                            onCreateStage = { onCreateStage(tournamentId) },
                            onCreateTeam = { onAddTeam(tournamentId) },
                            onStartMatch = onStartMatch,
                            onNavigateToMatchCenter = onNavigateToMatchCenter
                        )
                        1 -> TournamentTeamsTab(
                            teams = teams,
                            onAddTeam = { onAddTeam(tournamentId) },
                            onNavigateToTeamDetail = onNavigateToTeamDetail
                        )
                        2 -> TournamentMatchesTab(
                            fixtures = fixtures,
                            teamCount = teams.size,
                            onNavigateToMatchCenter = onNavigateToMatchCenter,
                            onAddTeam = { onAddTeam(tournamentId) },
                            onScheduleMatch = {
                                if (teams.size < 2) {
                                    Toast.makeText(context, "Please create at least 2 teams first!", Toast.LENGTH_SHORT).show()
                                } else {
                                    onScheduleMatch(tournamentId)
                                }
                            },
                            onStartMatch = onStartMatch
                        )
                        3 -> TournamentStandingsTab(standings = standings)
                        4 -> TournamentStatisticsTab(standings = standings)
                    }
                }
            }
            }
        }
    }
}
