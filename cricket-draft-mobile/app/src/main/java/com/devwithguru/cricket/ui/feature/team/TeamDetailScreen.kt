package com.devwithguru.cricket.ui.feature.team

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamId: String,
    initialTab: Int = 0,
    onTabChanged: (Int) -> Unit = {},
    onNavigateToPlayerDetail: (playerId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TeamViewModel = hiltViewModel()
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val tabTitles = listOf("Home", "Players", "Matches", "Tournaments", "Stats")
    LaunchedEffect(teamId) { viewModel.loadTeam(teamId) }
    val currentTeam by viewModel.currentTeam.collectAsState()
    val squad by viewModel.squad.collectAsState()
    val allRegisteredPlayers by viewModel.allRegisteredPlayers.collectAsState()
    val fixtures by viewModel.fixtures.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()

    // Creator state collected from ViewModel
    val isCreator by viewModel.isCreator.collectAsState()
    
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(error) {
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentTeam?.name ?: "Team",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Team Profile",
                            fontSize = 11.sp,
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
            // Background ambient glow
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

            if (isLoading || currentTeam == null) {
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

                // Render Content Tabs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> TeamHomeTab(team = currentTeam, squad = squad)
                        1 -> TeamPlayersTab(
                            isCreator = isCreator,
                            squad = squad,
                            allRegisteredPlayers = allRegisteredPlayers,
                            onAddPlayerManually = { name, role ->
                                viewModel.addPlayerManually(name, role, teamId)
                            },
                            onAddExistingPlayer = { playerId ->
                                viewModel.addExistingPlayer(playerId, teamId)
                            },
                            onAssignCaptain = { playerId ->
                                viewModel.setTeamCaptain(playerId, teamId)
                            },
                            onAssignViceCaptain = { playerId ->
                                viewModel.setTeamViceCaptain(playerId, teamId)
                            },
                            onAssignWicketkeeper = { playerId ->
                                viewModel.setTeamWicketkeeper(playerId, teamId)
                            },
                            onRemovePlayer = { playerId ->
                                viewModel.removePlayerFromTeam(playerId)
                            },
                            onNavigateToPlayerDetail = onNavigateToPlayerDetail
                        )
                        2 -> TeamMatchesTab(fixtures = fixtures)
                        3 -> TeamTournamentsTab(tournaments = tournaments)
                        4 -> TeamStatsTab(team = currentTeam, squad = squad)
                    }
                }
            }
            }
        }
    }
}
