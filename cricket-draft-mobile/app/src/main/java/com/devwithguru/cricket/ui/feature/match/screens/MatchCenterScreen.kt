package com.devwithguru.cricket.ui.feature.match.screens

import androidx.compose.ui.res.stringResource
import com.devwithguru.cricket.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.ui.feature.match.scorer.LiveScorerScreen
import com.devwithguru.cricket.ui.theme.screenConfig
import com.devwithguru.cricket.ui.feature.match.scorer.LiveScorerViewModel
import com.devwithguru.cricket.ui.feature.match.viewmodels.MatchCenterViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

// Shared Data Models for Scorecard and MVP listings
data class BattingCardRow(
    val name: String,
    val status: String,
    val r: Int,
    val b: Int,
    val fours: Int,
    val sixes: Int,
    val sr: String
)

data class BowlingCardRow(
    val name: String,
    val o: String,
    val m: Int,
    val r: Int,
    val w: Int,
    val econ: String
)

data class MVPStarRow(
    val rank: Int,
    val name: String,
    val stat: String,
    val pts: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCenterScreen(
    matchId: String,
    isScorer: Boolean = false,
    homeSquadList: List<String> = emptyList(),
    awaySquadList: List<String> = emptyList(),
    onNavigateBack: () -> Unit,
    onNavigateToMatchEditor: () -> Unit = {},
    onDeclareInnings: (runs: Int, wickets: Int, overs: String) -> Unit = { _, _, _ -> },
    matchCenterViewModel: MatchCenterViewModel = hiltViewModel(),
    scorerViewModel: LiveScorerViewModel = hiltViewModel()
) {
    LaunchedEffect(matchId) { matchCenterViewModel.loadFixture(matchId) }
    val activeFixture by matchCenterViewModel.fixture.collectAsState()
    var currentInnings by remember(activeFixture?.id, activeFixture?.currentInnings) {
        mutableStateOf(activeFixture?.currentInnings ?: 1)
    }
    var firstInningsTargetScore by remember(activeFixture?.id, activeFixture?.firstInningsRuns) {
        mutableStateOf(activeFixture?.firstInningsRuns?.let { if (it > 0) it + 1 else null })
    }
    var selectedTab by remember { mutableStateOf(0) }
    LaunchedEffect(isScorer) { selectedTab = 0 }

    val scoringTab = stringResource(R.string.tab_scoring)
    val scorecardTab = stringResource(R.string.tab_scorecard)
    val statsTab = stringResource(R.string.tab_stats)
    val superStarsTab = stringResource(R.string.tab_super_stars)
    val summaryTab = stringResource(R.string.tab_summary)

    val tabTitles = remember(isScorer, scoringTab, scorecardTab, statsTab, superStarsTab, summaryTab) {
        if (isScorer) {
            listOf(scoringTab, scorecardTab, statsTab, superStarsTab)
        } else {
            listOf(summaryTab, scorecardTab, statsTab, superStarsTab)
        }
    }

    val homeTeam = activeFixture?.homeTeam ?: "TBD"
    val awayTeam = activeFixture?.awayTeam ?: "TBD"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.match_center_title),
                            fontSize = screenConfig.titleTextSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$homeTeam vs $awayTeam",
                            fontSize = screenConfig.captionTextSize,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_desc),
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
            // Background radial gradient
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

            if (activeFixture == null) {
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
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = screenConfig.bodyTextSize,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Tab Contents
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> {
                            if (isScorer) {
                                val isFixtureInnings2 = currentInnings == 2

                                val battingTeam = if (isFixtureInnings2) activeFixture?.awayTeam ?: awayTeam else activeFixture?.homeTeam ?: homeTeam
                                val bowlingTeam = if (isFixtureInnings2) activeFixture?.homeTeam ?: homeTeam else activeFixture?.awayTeam ?: awayTeam

                                val fHomeSquad = activeFixture?.homeSquad?.takeIf { it.isNotEmpty() } ?: homeSquadList
                                val fAwaySquad = activeFixture?.awaySquad?.takeIf { it.isNotEmpty() } ?: awaySquadList

                                val dbInningsMatches = activeFixture?.currentInnings == currentInnings

                                LiveScorerScreen(
                                    homeTeamName = battingTeam,
                                    awayTeamName = bowlingTeam,
                                    homeSquadList = if (isFixtureInnings2) fAwaySquad else fHomeSquad,
                                    awaySquadList = if (isFixtureInnings2) fHomeSquad else fAwaySquad,
                                    ballsPerOver = 6,
                                    initialRuns = if (dbInningsMatches) activeFixture?.currentRuns ?: 0 else 0,
                                    initialWickets = if (dbInningsMatches) activeFixture?.currentWickets ?: 0 else 0,
                                    initialOversBowled = if (dbInningsMatches) activeFixture?.oversBowled ?: "0.0" else "0.0",
                                    initialStrikerName = if (dbInningsMatches) activeFixture?.strikerName ?: "" else "",
                                    initialNonStrikerName = if (dbInningsMatches) activeFixture?.nonStrikerName ?: "" else "",
                                    initialBowlerName = if (dbInningsMatches) activeFixture?.bowlerName ?: "" else "",
                                    initialBatsmenStats = if (dbInningsMatches) {
                                        if (isFixtureInnings2) activeFixture?.secondInningsBatsmen ?: emptyList() else activeFixture?.firstInningsBatsmen ?: emptyList()
                                    } else emptyList(),
                                    initialBowlersStats = if (dbInningsMatches) {
                                        if (isFixtureInnings2) activeFixture?.secondInningsBowlers ?: emptyList() else activeFixture?.firstInningsBowlers ?: emptyList()
                                    } else emptyList(),
                                    initialFOW = if (dbInningsMatches) {
                                        if (isFixtureInnings2) activeFixture?.secondInningsFOW ?: emptyList() else activeFixture?.firstInningsFOW ?: emptyList()
                                    } else emptyList(),
                                    initialPartnerships = if (dbInningsMatches) {
                                        if (isFixtureInnings2) activeFixture?.secondInningsPartnerships ?: emptyList() else activeFixture?.firstInningsPartnerships ?: emptyList()
                                    } else emptyList(),
                                    initialActivePartnershipRuns = if (dbInningsMatches) activeFixture?.activePartnershipRuns ?: 0 else 0,
                                    initialActivePartnershipBalls = if (dbInningsMatches) activeFixture?.activePartnershipBalls ?: 0 else 0,
                                    isInnings2 = isFixtureInnings2,
                                    firstInningsTarget = firstInningsTargetScore,
                                    matchTotalOvers = activeFixture?.overs ?: 6,
                                    matchTotalWickets = activeFixture?.wickets ?: 10,
                                    onDeclareInnings = { runs, wickets, overs ->
                                         activeFixture?.let { f ->
                                              val updated = if (f.currentInnings == 1) {
                                                  f.copy(
                                                      firstInningsRuns = runs,
                                                      firstInningsWickets = wickets,
                                                      firstInningsBatsmen = scorerViewModel.state.batsmenStats.values.toList(),
                                                      firstInningsBowlers = scorerViewModel.state.bowlersStats.values.toList(),
                                                      firstInningsExtras = scorerViewModel.state.extras,
                                                      firstInningsDotBalls = scorerViewModel.state.dotBalls,
                                                      firstInningsFOW = scorerViewModel.state.fallOfWickets,
                                                      firstInningsPartnerships = scorerViewModel.state.partnerships,
                                                      currentInnings = 2,
                                                      currentRuns = 0,
                                                      currentWickets = 0,
                                                      oversBowled = "0.0",
                                                      strikerName = "",
                                                      nonStrikerName = "",
                                                      bowlerName = ""
                                                  )
                                              } else {
                                                  f.copy(
                                                      secondInningsBatsmen = scorerViewModel.state.batsmenStats.values.toList(),
                                                      secondInningsBowlers = scorerViewModel.state.bowlersStats.values.toList(),
                                                      secondInningsExtras = scorerViewModel.state.extras,
                                                      secondInningsDotBalls = scorerViewModel.state.dotBalls,
                                                      secondInningsFOW = scorerViewModel.state.fallOfWickets,
                                                      secondInningsPartnerships = scorerViewModel.state.partnerships,
                                                      status = "Completed"
                                                  )
                                              }
                                              currentInnings = updated.currentInnings
                                              if (updated.currentInnings == 2) {
                                                  firstInningsTargetScore = runs + 1
                                              }
                                              matchCenterViewModel.updateFixture(updated)
                                         }
                                         onDeclareInnings(runs, wickets, overs)
                                    },
                                    onNavigateToMatchEditor = onNavigateToMatchEditor,
                                    onNavigateBack = onNavigateBack,
                                    viewModel = scorerViewModel,
                                    onScoreChanged = { runs, wickets, overs, striker, nonStriker ->
                                        activeFixture?.let { f ->
                                            val updated = if (f.currentInnings == 1) {
                                                f.copy(
                                                    currentRuns = runs,
                                                    currentWickets = wickets,
                                                    oversBowled = overs,
                                                    strikerName = striker,
                                                    nonStrikerName = nonStriker,
                                                    bowlerName = scorerViewModel.state.bowler.name,
                                                    activePartnershipRuns = scorerViewModel.state.activePartnershipRuns,
                                                    activePartnershipBalls = scorerViewModel.state.activePartnershipBalls,
                                                    firstInningsBatsmen = scorerViewModel.state.batsmenStats.values.toList(),
                                                    firstInningsBowlers = scorerViewModel.state.bowlersStats.values.toList(),
                                                    firstInningsFOW = scorerViewModel.state.fallOfWickets,
                                                    firstInningsPartnerships = scorerViewModel.state.partnerships
                                                )
                                            } else {
                                                f.copy(
                                                    currentRuns = runs,
                                                    currentWickets = wickets,
                                                    oversBowled = overs,
                                                    strikerName = striker,
                                                    nonStrikerName = nonStriker,
                                                    bowlerName = scorerViewModel.state.bowler.name,
                                                    activePartnershipRuns = scorerViewModel.state.activePartnershipRuns,
                                                    activePartnershipBalls = scorerViewModel.state.activePartnershipBalls,
                                                    secondInningsBatsmen = scorerViewModel.state.batsmenStats.values.toList(),
                                                    secondInningsBowlers = scorerViewModel.state.bowlersStats.values.toList(),
                                                    secondInningsFOW = scorerViewModel.state.fallOfWickets,
                                                    secondInningsPartnerships = scorerViewModel.state.partnerships
                                                )
                                            }
                                            matchCenterViewModel.updateFixture(updated)
                                        }
                                    })
                            } else {
                                MatchSummaryTab(homeTeam, awayTeam, activeFixture)
                            }
                        }
                        1 -> MatchScorecardTab(matchId = matchId, homeTeam = homeTeam, awayTeam = awayTeam, viewModel = scorerViewModel, activeFixture = activeFixture)
                        2 -> MatchStatsTab(matchId = matchId, homeTeam = homeTeam, awayTeam = awayTeam, viewModel = scorerViewModel, activeFixture = activeFixture)
                        3 -> MatchSuperStarsTab(matchId = matchId, viewModel = scorerViewModel, activeFixture = activeFixture)
                    }
                }
            }
            }
        }
    }
}

// Shared UI element for stats comparisons
@Composable
fun MatchStatCompareBar(
    label: String,
    homeVal: String,
    awayVal: String,
    homeRatio: Float,
    homeTeam: String,
    awayTeam: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = screenConfig.captionTextSize,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = homeVal, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = screenConfig.bodyTextSize)
            Text(text = awayVal, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = screenConfig.bodyTextSize)
        }

        // Custom comparison progress bar using dynamic weights
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(homeRatio.coerceAtLeast(0.01f))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((1f - homeRatio).coerceAtLeast(0.01f))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = homeTeam, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(text = awayTeam, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}
