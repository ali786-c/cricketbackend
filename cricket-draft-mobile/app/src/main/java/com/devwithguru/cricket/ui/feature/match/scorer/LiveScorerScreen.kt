package com.devwithguru.cricket.ui.feature.match.scorer

import com.devwithguru.cricket.domain.model.BatterState
import com.devwithguru.cricket.domain.model.BowlerState
import com.devwithguru.cricket.domain.model.WicketEvent
import com.devwithguru.cricket.domain.model.PartnershipEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.ui.theme.screenConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScorerScreen(
    homeTeamName: String = "Ali Panthers",
    awayTeamName: String = "Islamabad Blasters",
    homeSquadList: List<String> = emptyList(),
    awaySquadList: List<String> = emptyList(),
    ballsPerOver: Int = 6,
    onDeclareInnings: (runs: Int, wickets: Int, overs: String) -> Unit,
    onNavigateToMatchEditor: () -> Unit,
    onNavigateBack: () -> Unit,
    onScoreChanged: ((runs: Int, wickets: Int, oversBowled: String, striker: String, nonStriker: String) -> Unit)? = null,
    initialRuns: Int = 0,
    initialWickets: Int = 0,
    initialOversBowled: String = "0.0",
    initialStrikerName: String = "",
    initialNonStrikerName: String = "",
    isInnings2: Boolean = false,
    firstInningsTarget: Int? = null,
    matchTotalOvers: Int = 6,
    matchTotalWickets: Int = 10,
    initialBowlerName: String = "",
    initialBatsmenStats: List<BatterState> = emptyList(),
    initialBowlersStats: List<BowlerState> = emptyList(),
    initialFOW: List<WicketEvent> = emptyList(),
    initialPartnerships: List<PartnershipEvent> = emptyList(),
    initialActivePartnershipRuns: Int = 0,
    initialActivePartnershipBalls: Int = 0,
    viewModel: LiveScorerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {

    LaunchedEffect(homeTeamName, awayTeamName, homeSquadList, awaySquadList, isInnings2) {
        viewModel.initialize(
            runs = initialRuns,
            wickets = initialWickets,
            oversBowled = initialOversBowled,
            striker = initialStrikerName,
            nonStriker = initialNonStrikerName,
            initialBowlerName = initialBowlerName,
            ballsPerOver = ballsPerOver,
            battingSquad = homeSquadList,
            bowlingSquad = awaySquadList,
            homeTeamName = homeTeamName,
            awayTeamName = awayTeamName,
            matchTotalOvers = matchTotalOvers,
            matchTotalWickets = matchTotalWickets,
            isInnings2 = isInnings2,
            firstInningsTarget = firstInningsTarget,
            initialBatsmenStats = initialBatsmenStats,
            initialBowlersStats = initialBowlersStats,
            initialFOW = initialFOW,
            initialPartnerships = initialPartnerships,
            initialActivePartnershipRuns = initialActivePartnershipRuns,
            initialActivePartnershipBalls = initialActivePartnershipBalls
        )
    }

    val state = viewModel.state
    val historyStack = viewModel.historyStack

    var activeMetricIndex by remember { mutableStateOf(0) }
    LaunchedEffect(key1 = state.isInnings2) {
        while (true) {
            kotlinx.coroutines.delay(3000L)
            activeMetricIndex = (activeMetricIndex + 1) % if (state.isInnings2) 3 else 2
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Ambient background radial glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // --- SCORE BANNER PANEL ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onNavigateToMatchEditor,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Match",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Innings visual indicator
                            Text(
                                text = if (isInnings2) "2ND INNINGS - CHASE" else "1ST INNINGS",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )

                        // Live Score Numbers
                        val battingTeamAbbr = remember(state.battingTeamName) {
                            val parts = state.battingTeamName.split(" ")
                            if (parts.size >= 2) {
                                parts.mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                            } else {
                                state.battingTeamName.take(3).uppercase()
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$battingTeamAbbr  ",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = screenConfig.scoreTextSize,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "${state.runs}/${state.wickets}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = screenConfig.displayTextSize,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Overs Row
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Overs: ${state.formattedOvers}/$matchTotalOvers",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Single Row: CRR, Extras, Partnership, Target/Projected/RRR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. CRR
                            MetricItem(label = "CRR", value = state.runRateStr, modifier = Modifier.weight(1f))
                            
                            MetricDivider()

                            // 2. Extras
                            MetricItem(label = "EXTRAS", value = "${state.extras}", modifier = Modifier.weight(1f))

                            MetricDivider()

                            // 3. Partnership
                            MetricItem(label = "PARTN", value = "${state.activePartnershipRuns}(${state.activePartnershipBalls})", modifier = Modifier.weight(1f))

                            MetricDivider()

                            // 4. Target / Projected / RRR
                            if (isInnings2) {
                                MetricItem(label = "TARGET", value = "$firstInningsTarget", modifier = Modifier.weight(1f))
                                MetricDivider()
                                MetricItem(label = "RRR", value = state.requiredRunRateStr, modifier = Modifier.weight(1f))
                            } else {
                                val runRate = if (state.totalBalls > 0) (state.runs.toFloat() / (state.totalBalls.toFloat() / 6f)) else 0f
                                val projScore = (runRate * matchTotalOvers).toInt()
                                val proj = if (projScore > 0) "$projScore" else "-"
                                MetricItem(label = "PROJ", value = proj, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

                // --- ACTIVE BATSMEN & BOWLERS PANEL ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Batsmen Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .wrapContentHeight()
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Batter",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "R(B)  4s  6s",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Striker/Batter 1 Row
                            val isB1Striker = state.batter1.name == state.strikerName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleStrike { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isB1Striker) {
                                        Icon(
                                            imageVector = Icons.Default.SportsCricket,
                                            contentDescription = "Striker",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    Text(
                                        text = state.batter1.name,
                                        color = if (isB1Striker) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${state.batter1.runs}(${state.batter1.balls})  ${state.batter1.fours}  ${state.batter1.sixes}",
                                    color = if (isB1Striker) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Non-Striker/Batter 2 Row
                            val isB2Striker = state.batter2.name == state.strikerName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleStrike { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } }
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isB2Striker) {
                                        Icon(
                                            imageVector = Icons.Default.SportsCricket,
                                            contentDescription = "Striker",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    Text(
                                        text = state.batter2.name,
                                        color = if (isB2Striker) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${state.batter2.runs}(${state.batter2.balls})  ${state.batter2.fours}  ${state.batter2.sixes}",
                                    color = if (isB2Striker) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Bowler Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight()
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.showSelectBowlerDialog = true }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Bowler",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )

                            // Bowler Name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.bowler.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // O-M-R-W / Score Wickets below
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "O-M-R-W",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${state.formattedBowlerOvers}-0-${state.bowler.runsConceded}-${state.bowler.wickets}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Recent balls log
                val overScrollState = rememberScrollState()
                LaunchedEffect(state.thisOver.size) {
                    if (state.thisOver.isNotEmpty()) {
                        overScrollState.animateScrollTo(overScrollState.maxValue)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "THIS OVER",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(overScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.thisOver.isEmpty()) {
                            Text(
                                text = "No balls bowled yet in this over.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        } else {
                            state.thisOver.forEachIndexed { i, ballText ->
                                val isWicket = ballText == "W" || ballText.contains("+W") || (ballText.contains("W") && !ballText.startsWith("Wd"))
                                val isExtra = ballText.startsWith("Wd") || ballText.startsWith("Nb") || ballText.startsWith("By") || ballText.startsWith("Lb")
                                val isBoundary = ballText == "4" || ballText == "6"

                                val bubbleBg = when {
                                    isWicket -> MaterialTheme.colorScheme.error
                                    isBoundary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    isExtra -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                }
                                val bubbleBorder = when {
                                    isWicket -> Color.Transparent
                                    isBoundary -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(bubbleBg)
                                        .border(1.dp, bubbleBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ballText,
                                        color = if (isWicket) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // --- RUNS KEYBOARD AND CONTROLS ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.isMatchCompleted) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "MATCH COMPLETED! 🏏",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = state.matchResultStatus,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            onDeclareInnings(state.runs, state.wickets, state.formattedOvers)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Save & End Match 🏆", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else if (state.isInnings1Completed) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "INNINGS COMPLETED! 🏏",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "$homeTeamName Innings finished at ${state.runs}/${state.wickets} (${state.formattedOvers} Ov)",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            onDeclareInnings(state.runs, state.wickets, state.formattedOvers)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Start 2nd Innings 🏏", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            if (!viewModel.showAdvancedKeyboard) {
                                // Runs input row 1
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf(0, 1, 2, 3).forEach { run ->
                                        Button(
                                            onClick = { viewModel.recordRuns(run) { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            ),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(text = "$run", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Runs input row 2
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf(4, 6).forEach { boundary ->
                                        Button(
                                            onClick = { viewModel.recordRuns(boundary) { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text(
                                                text = if (boundary == 4) "4 (Boundary)" else "6 (Six)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Quick Extras & Wickets Input Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.recordExtra("Wd") { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(text = "+1 Wd", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.recordExtra("Nb") { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(text = "+1 Nb", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.showWicketDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(text = "Wicket", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                // Quick Byes/Leg-byes and Advanced Keyboard Toggle Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.recordExtra("By") { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(text = "+1 Bye", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.recordExtra("Lb") { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(text = "+1 Lb", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.showAdvancedKeyboard = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(text = "Custom", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // Render Advanced Custom Extras Input Keyboard Panel
                                AdvancedExtrasKeyboard(
                                    onConfirmCustomScore = { batRuns, extrasVal, extraCode ->
                                        viewModel.recordCustomScore(batRuns, extrasVal, extraCode) { r, w, o, s, n ->
                                            onScoreChanged?.invoke(r, w, o, s, n)
                                        }
                                    },
                                    onBackToStandard = { viewModel.showAdvancedKeyboard = false },
                                    onDeclareInnings = {
                                        onDeclareInnings(state.runs, state.wickets, state.formattedOvers)
                                    },
                                    isInnings2 = isInnings2
                                )
                            }
                        }

                        // --- UNDO HISTORY ACTION BUTTON ---
                        if (historyStack.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = { viewModel.performUndo { r, w, o, s, n -> onScoreChanged?.invoke(r, w, o, s, n) } },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = "Undo Last Score Input",
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Undo Last Delivery", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- WIZARDS AND SELECTORS MODAL DIALOGS ---

    // 1. SELECT OPENING BATSMEN DIALOG (UNIFIED)
    SelectOpeningBatsmenDialog(
        visible = viewModel.showSelectOpeningStrikerDialog,
        battingSquadList = viewModel.battingSquadList,
        onConfirm = { striker, nonStriker -> viewModel.selectOpeningBatsmen(striker, nonStriker) }
    )

    // 3. SELECT OPENING BOWLER DIALOG
    SelectOpeningBowlerDialog(
        visible = viewModel.showSelectOpeningBowlerDialog,
        bowlingSquadList = viewModel.bowlingSquadList,
        onSelect = { viewModel.selectOpeningBowler(it) }
    )

    // 4. IN-GAME WICKET DISMISSAL DETAILS DIALOG
    WicketRecordingDialog(
        visible = viewModel.showWicketDialog,
        strikerName = state.strikerName,
        nonStrikerName = state.nonStrikerName,
        bowlingSquadList = viewModel.bowlingSquadList,
        onDismiss = { viewModel.showWicketDialog = false },
        onRecordWicket = { type, name, fielder, runs, extra ->
            viewModel.recordWicket(type, name, fielder, runs, extra)
        }
    )

    // 5. IN-GAME INCOMING BATSMAN REPLACEMENT DIALOG
    SelectNextBatsmanDialog(
        visible = viewModel.showSelectBatsmanDialog,
        strikerName = state.strikerName,
        nonStrikerName = state.nonStrikerName,
        dismissedBatsmen = state.dismissedBatsmen,
        battingSquadList = viewModel.battingSquadList,
        onConfirm = { batsmanName ->
            viewModel.confirmIncomingBatsman(batsmanName) { r, w, o, s, n ->
                onScoreChanged?.invoke(r, w, o, s, n)
            }
        },
        onDismissRequest = { viewModel.showSelectBatsmanDialog = false }
    )

    // 6. IN-GAME NEXT BOWLER SELECTION DIALOG (Over end)
    SelectNextBowlerDialog(
        visible = viewModel.showSelectBowlerDialog,
        bowlerName = state.bowler.name,
        bowlingSquadList = viewModel.bowlingSquadList,
        onSelect = { viewModel.selectNextBowler(it) }
    )

    // 7. INTERACTIVE EXTRAS RUN-SELECTION DIALOG (Wide, No Ball, Bye, Leg Bye Option Toggles)
    if (viewModel.activeExtraDialogType.isNotEmpty()) {
        val extraLabel = when (viewModel.activeExtraDialogType) {
            "Wd" -> "Wide"
            "Nb" -> "No Ball"
            "By" -> "Bye"
            "Lb" -> "Leg Bye"
            else -> "Extra"
        }

        AlertDialog(
            onDismissRequest = { viewModel.activeExtraDialogType = "" },
            confirmButton = {},
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = extraLabel,
                        fontSize = screenConfig.headingTextSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { viewModel.activeExtraDialogType = "" }) {
                        Text("✕", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val (options, tag) = when (viewModel.activeExtraDialogType) {
                        "Wd" -> listOf(
                            Triple(0, 1, "Wd"),       // Wd (1 run extra)
                            Triple(0, 2, "Wd"),       // 1 Wd (2 runs extras)
                            Triple(0, 3, "Wd"),       // 2 Wd (3 runs extras)
                            Triple(0, 4, "Wd"),       // 3 Wd (4 runs extras)
                            Triple(0, 5, "Wd"),       // 4 Wd (5 runs extras)
                            Triple(0, 7, "Wd")        // 6 Wd (7 runs extras)
                        ) to "WD"
                        "Nb" -> listOf(
                            Triple(0, 1, "Nb"),       // Nb (1 run)
                            Triple(1, 1, "Nb"),       // 1 Nb (2 runs)
                            Triple(2, 1, "Nb"),       // 2 Nb (3 runs)
                            Triple(3, 1, "Nb"),       // 3 Nb (4 runs)
                            Triple(4, 1, "Nb"),       // 4 Nb (5 runs)
                            Triple(6, 1, "Nb")        // 6 Nb (7 runs)
                        ) to "NB"
                        "By" -> listOf(
                            Triple(0, 1, "By"),       // 1 Bye
                            Triple(0, 2, "By"),       // 2 Byes
                            Triple(0, 3, "By"),       // 3 Byes
                            Triple(0, 4, "By")        // 4 Byes
                        ) to "BY"
                        "Lb" -> listOf(
                            Triple(0, 1, "Lb"),       // 1 Leg Bye
                            Triple(0, 2, "Lb"),       // 2 Leg Byes
                            Triple(0, 3, "Lb"),       // 3 Leg Byes
                            Triple(0, 4, "Lb")        // 4 Leg Byes
                        ) to "LB"
                        else -> emptyList<Triple<Int, Int, String>>() to "EX"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { (batRuns, extrasVal, extraCode) ->
                            val totalRuns = batRuns + extrasVal
                            val label = when (viewModel.activeExtraDialogType) {
                                "Wd" -> if (extrasVal == 1) "Wide (1 run)" else "${extrasVal - 1} Wides (${extrasVal} runs)"
                                "Nb" -> if (batRuns == 0) "No Ball (1 run)" else "${batRuns} runs + No Ball (${totalRuns} runs)"
                                "By" -> "${extrasVal} Byes"
                                "Lb" -> "${extrasVal} Leg Byes"
                                else -> ""
                            }

                            Button(
                                onClick = {
                                    viewModel.recordCustomScore(batRuns, extrasVal, extraCode) { r, w, o, s, n ->
                                        onScoreChanged?.invoke(r, w, o, s, n)
                                    }
                                    viewModel.activeExtraDialogType = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(text = label)
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun AdvancedExtrasKeyboard(
    onConfirmCustomScore: (batRuns: Int, extrasVal: Int, extraCode: String) -> Unit,
    onBackToStandard: () -> Unit,
    onDeclareInnings: () -> Unit,
    isInnings2: Boolean
) {
    var selectedRunsOffBat by remember { mutableStateOf(0) }
    var selectedExtrasVal by remember { mutableStateOf(0) }
    var selectedExtraType by remember { mutableStateOf("Wd") } // "Wd", "Nb", "By", "Lb", "Pen"

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Selection indicator row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Score: ${selectedRunsOffBat} Runs + ${selectedExtrasVal} ${selectedExtraType} (${selectedRunsOffBat + selectedExtrasVal} Total)",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onBackToStandard, modifier = Modifier.size(24.dp)) {
                Text("✕", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Row 1: Runs off bat selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Runs:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.widthIn(min = 50.dp, max = 80.dp))
            listOf(0, 7, 8, 9).forEach { runs ->
                val isSelected = selectedRunsOffBat == runs
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .clickable { selectedRunsOffBat = runs },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$runs",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Row 2: Extras type selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Extra Type:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.widthIn(min = 50.dp, max = 80.dp))
            listOf("Wd", "Nb", "By", "Lb", "Pen").forEach { type ->
                val isSelected = selectedExtraType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { selectedExtraType = type }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Row 3: Extras runs selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Extra Runs:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.widthIn(min = 50.dp, max = 80.dp))
            listOf(0, 1, 2, 3, 4, 5).forEach { runs ->
                val isSelected = selectedExtrasVal == runs
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .clickable { selectedExtrasVal = runs },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$runs",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action Buttons Row (Declare Innings / End Match & Confirm Custom Score)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDeclareInnings,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = if (isInnings2) "End Match 🏆" else "Declare Innings 🏏", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onConfirmCustomScore(selectedRunsOffBat, selectedExtrasVal, selectedExtraType) },
                modifier = Modifier
                    .weight(1.2f)
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Confirm Score", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun MetricDivider() {
    Box(
        modifier = Modifier
            .height(18.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
    )
}
