package com.devwithguru.cricket.ui.feature.match.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.domain.model.ScheduledFixture
import com.devwithguru.cricket.domain.model.BatterState
import com.devwithguru.cricket.domain.model.BowlerState
import com.devwithguru.cricket.ui.theme.screenConfig

@Composable
fun MatchSummaryTab(homeTeam: String, awayTeam: String, fixture: ScheduledFixture? = null) {
    val isStarted = fixture != null && fixture.status != "Scheduled"
    val isCompleted = fixture != null && fixture.status.equals("Completed", ignoreCase = true)
    
    // Brand teal-green color matching the screenshot label
    val labelColor = Color(0xFF00897B)

    // ── Derive Innings Batting Order based on Toss ──
    val tossWinner = fixture?.tossWinner ?: ""
    val tossDecision = fixture?.tossDecision ?: ""

    val (innings1Team, innings2Team) = remember(tossWinner, tossDecision, homeTeam, awayTeam) {
        when {
            tossWinner.equals(homeTeam, ignoreCase = true) -> {
                if (tossDecision.equals("bat", ignoreCase = true)) Pair(homeTeam, awayTeam) else Pair(awayTeam, homeTeam)
            }
            tossWinner.equals(awayTeam, ignoreCase = true) -> {
                if (tossDecision.equals("bat", ignoreCase = true)) Pair(awayTeam, homeTeam) else Pair(homeTeam, awayTeam)
            }
            else -> Pair(homeTeam, awayTeam)
        }
    }

    val innings1TossWinner = tossWinner.equals(innings1Team, ignoreCase = true)
    val innings2TossWinner = tossWinner.equals(innings2Team, ignoreCase = true)

    // ── Calculate Innings 1 Details ──
    val innings1Score = remember(fixture) {
        if (fixture != null) {
            if (fixture.currentInnings >= 2) {
                val runs = fixture.firstInningsRuns ?: 0
                val wickets = fixture.firstInningsWickets ?: 0
                "$runs-$wickets"
            } else {
                "${fixture.currentRuns}-${fixture.currentWickets}"
            }
        } else "0-0"
    }

    val innings1OversText = remember(fixture) {
        if (fixture != null) {
            if (fixture.currentInnings >= 2) {
                val totalBalls = fixture.firstInningsBowlers.sumOf { it.balls }
                if (totalBalls > 0) "${totalBalls / 6}.${totalBalls % 6}" else fixture.overs.toString()
            } else {
                fixture.oversBowled
            }
        } else "0.0"
    }

    // ── Calculate Innings 2 Details ──
    val innings2Score = remember(fixture) {
        if (fixture != null) {
            if (fixture.currentInnings >= 2) {
                "${fixture.currentRuns}-${fixture.currentWickets}"
            } else {
                "0-0"
            }
        } else "0-0"
    }

    val innings2OversText = remember(fixture) {
        if (fixture != null) {
            if (fixture.status.equals("Completed", ignoreCase = true)) {
                val totalBalls = fixture.secondInningsBowlers.sumOf { it.balls }
                if (totalBalls > 0) "${totalBalls / 6}.${totalBalls % 6}" else fixture.oversBowled
            } else {
                fixture.oversBowled
            }
        } else "0.0"
    }

    // ── Top Performers Selection ──
    val topBatsmenInnings1 = remember(fixture) {
        fixture?.firstInningsBatsmen?.filter { it.runs > 0 || it.balls > 0 }
            ?.sortedWith(compareByDescending<BatterState> { it.runs }.thenBy { it.balls })
            ?.take(3) ?: emptyList()
    }

    val topBowlersInnings1 = remember(fixture) {
        fixture?.firstInningsBowlers?.filter { it.balls > 0 }
            ?.sortedWith(compareByDescending<BowlerState> { it.wickets }.thenBy { it.runsConceded })
            ?.take(3) ?: emptyList()
    }

    val topBatsmenInnings2 = remember(fixture) {
        fixture?.secondInningsBatsmen?.filter { it.runs > 0 || it.balls > 0 }
            ?.sortedWith(compareByDescending<BatterState> { it.runs }.thenBy { it.balls })
            ?.take(3) ?: emptyList()
    }

    val topBowlersInnings2 = remember(fixture) {
        fixture?.secondInningsBowlers?.filter { it.balls > 0 }
            ?.sortedWith(compareByDescending<BowlerState> { it.wickets }.thenBy { it.runsConceded })
            ?.take(3) ?: emptyList()
    }

    // ── Dynamic Player Of The Match Calculations (MVP leader) ──
    val potmName = remember(fixture) {
        if (fixture == null) return@remember "TBD"
        val playersMap = mutableMapOf<String, Int>()
        fixture.firstInningsBatsmen.forEach { b ->
            playersMap[b.name] = (playersMap[b.name] ?: 0) + b.runs + b.fours + (b.sixes * 2)
        }
        fixture.firstInningsBowlers.forEach { b ->
            playersMap[b.name] = (playersMap[b.name] ?: 0) + (b.wickets * 20)
        }
        fixture.secondInningsBatsmen.forEach { b ->
            playersMap[b.name] = (playersMap[b.name] ?: 0) + b.runs + b.fours + (b.sixes * 2)
        }
        fixture.secondInningsBowlers.forEach { b ->
            playersMap[b.name] = (playersMap[b.name] ?: 0) + (b.wickets * 20)
        }
        val topPlayer = playersMap.maxByOrNull { it.value }?.key
        topPlayer ?: "TBD"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isStarted) {
            // ── Match Scheduled Page (Clean design) ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = innings1Team.uppercase(),
                    color = labelColor,
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "YET TO BAT",
                    color = labelColor.copy(alpha = 0.6f),
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = innings2Team.uppercase(),
                    color = labelColor,
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "YET TO BAT",
                    color = labelColor.copy(alpha = 0.6f),
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Match hasn't started yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scoring summary will populate here once the match is Live.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // ─── INNINGS 1 SECTION ───
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = innings1Team.uppercase(),
                            color = labelColor,
                            fontSize = screenConfig.headingTextSize,
                            fontWeight = FontWeight.Black
                        )
                        if (innings1TossWinner) {
                            TossBadge()
                        }
                    }
                    Text(
                        text = "$innings1Score ($innings1OversText)",
                        color = labelColor,
                        fontSize = screenConfig.headingTextSize,
                        fontWeight = FontWeight.Black
                    )
                }

                // Innings 1 side-by-side Top Performers (Batsmen of Innings 1 Team vs Bowlers of Innings 2 Team)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Batsmen Column (50% weight)
                    Column(modifier = Modifier.weight(1f)) {
                        topBatsmenInnings1.forEach { batter ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = batter.name,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val batterScore = "${batter.runs}${if (!batter.isDismissed && batter.runs > 0) "*" else ""}(${batter.balls})"
                                Text(
                                    text = batterScore,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Bowlers Column (50% weight)
                    Column(modifier = Modifier.weight(1f)) {
                        topBowlersInnings1.forEach { bowler ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bowler.name,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${bowler.wickets}-${bowler.runsConceded}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── INNINGS 2 SECTION ───
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val isSecondInningsLive = fixture != null && fixture.currentInnings >= 2
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = innings2Team.uppercase(),
                            color = labelColor,
                            fontSize = screenConfig.headingTextSize,
                            fontWeight = FontWeight.Black
                        )
                        if (innings2TossWinner) {
                            TossBadge()
                        }
                    }
                    Text(
                        text = if (isSecondInningsLive) "$innings2Score ($innings2OversText)" else "YET TO BAT",
                        color = if (isSecondInningsLive) labelColor else labelColor.copy(alpha = 0.5f),
                        fontSize = screenConfig.headingTextSize,
                        fontWeight = FontWeight.Black
                    )
                }

                if (isSecondInningsLive) {
                    // Innings 2 side-by-side Top Performers (Batsmen of Innings 2 Team vs Bowlers of Innings 1 Team)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Batsmen Column (50% weight)
                        Column(modifier = Modifier.weight(1f)) {
                            topBatsmenInnings2.forEach { batter ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = batter.name,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val batterScore = "${batter.runs}${if (!batter.isDismissed && batter.runs > 0) "*" else ""}(${batter.balls})"
                                    Text(
                                        text = batterScore,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Bowlers Column (50% weight)
                        Column(modifier = Modifier.weight(1f)) {
                            topBowlersInnings2.forEach { bowler ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bowler.name,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${bowler.wickets}-${bowler.runsConceded}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── WINNER SECTION (Completed Only) ───
            if (isCompleted && fixture != null) {
                val fRuns = fixture.firstInningsRuns ?: 0
                val sRuns = fixture.currentRuns
                val sWickets = fixture.currentWickets
                val target = fRuns + 1
                
                val winnerTeam = if (sRuns >= target) innings2Team else innings1Team
                
                val winnerResultText = remember(fixture, innings1Team, innings2Team) {
                    if (sRuns >= target) {
                        val wicketsLeft = fixture.wickets - sWickets
                        "${winnerTeam.uppercase()} won by $wicketsLeft wickets"
                    } else {
                        val needed = target - sRuns
                        if (needed == 1) {
                            "Match Tied"
                        } else {
                            val runsMargin = needed - 1
                            "${winnerTeam.uppercase()} won by $runsMargin runs"
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Winner Team Circular Logo
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        labelColor,
                                        labelColor.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = winnerTeam.take(2).uppercase(),
                            color = Color.White,
                            fontSize = screenConfig.headingTextSize,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Winner",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = winnerResultText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = screenConfig.headingTextSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── PLAYER OF THE MATCH SECTION (Completed/Contributors Available) ───
            if (isCompleted && potmName != "TBD") {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Circular Player Avatar
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = potmName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2).uppercase()
                        Text(
                            text = if (initials.isNotEmpty()) initials else "P",
                            color = Color.White,
                            fontSize = screenConfig.headingTextSize,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Player Of The Match",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = potmName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = screenConfig.headingTextSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ─── SHARE MATCH ACTIONS BUTTONS ───
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Share Report */ },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Match Report",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { /* Share Summary */ },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Match Summary",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TossBadge() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "T",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
