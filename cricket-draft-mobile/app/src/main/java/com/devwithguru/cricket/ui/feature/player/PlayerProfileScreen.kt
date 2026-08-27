package com.devwithguru.cricket.ui.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch
import com.devwithguru.cricket.ui.feature.player.PlayerProfileViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.devwithguru.cricket.domain.model.ScheduledFixture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    playerId: String,
    onNavigateBack: () -> Unit,
    onStartScheduledMatch: ((ScheduledFixture) -> Unit)? = null,
    viewModel: PlayerProfileViewModel = hiltViewModel()
) {
    val tabTitles = listOf("Overview", "Stats", "Matches", "Teams", "Tournaments")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val scope = rememberCoroutineScope()

    // Look up player from Room DB via ViewModel
    LaunchedEffect(playerId) { viewModel.loadPlayer(playerId) }
    val registeredPlayer by viewModel.player.collectAsState()
    val playerName = registeredPlayer?.name ?: "Unknown Player"
    val playerStats by viewModel.stats.collectAsState()
    val playerInsights by viewModel.insights.collectAsState()
    val playerMatches by viewModel.matches.collectAsState()
    val playerTeams by viewModel.teams.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = playerName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Career Profile",
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

            Column(modifier = Modifier.fillMaxSize()) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)) }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Tab Content Rendering Area using HorizontalPager for swipe gestures
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> PlayerOverviewTab(
                            playerId = playerId,
                            playerName = playerName,
                            stats = playerStats,
                            insights = playerInsights
                        )
                        1 -> PlayerStatsTab(stats = playerStats)
                        2 -> PlayerMatchesTab(
                            onStartScheduledMatch = onStartScheduledMatch,
                            matchData = playerMatches
                        )
                        3 -> PlayerTeamsTab(teams = playerTeams)
                        4 -> PlayerTournamentsTab(teams = playerTeams)
                    }
                }
            }
        }
    }
}
