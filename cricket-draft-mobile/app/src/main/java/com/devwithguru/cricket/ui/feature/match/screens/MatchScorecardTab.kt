package com.devwithguru.cricket.ui.feature.match.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import com.devwithguru.cricket.ui.feature.match.scorer.LiveScorerViewModel
import com.devwithguru.cricket.domain.model.BatterState
import com.devwithguru.cricket.domain.model.WicketEvent
import com.devwithguru.cricket.domain.model.PartnershipEvent
import com.devwithguru.cricket.domain.model.ScheduledFixture

fun getDismissalText(b: BatterState): String {
    if (!b.isDismissed) return "not out"
    val type = b.dismissalType ?: "dismissed"
    val bowler = b.bowlerName?.split(" ")?.lastOrNull() ?: b.bowlerName ?: ""
    val fielder = b.fielderName?.split(" ")?.lastOrNull() ?: b.fielderName ?: ""
    return when (type) {
        "Bowled" -> "b $bowler"
        "Caught" -> "c $fielder b $bowler"
        "LBW" -> "lbw b $bowler"
        "Stumped" -> "st $fielder b $bowler"
        "Run Out" -> "run out ($fielder)"
        "Hit Wicket" -> "hit wicket b $bowler"
        "Retired" -> "retired"
        "Mankad" -> "run out (Mankad)"
        "Over The Fence" -> "over the fence"
        "One Hand One Bounce" -> "one hand one bounce"
        else -> "dismissed"
    }
}

@Composable
fun MatchScorecardTab(
    matchId: String,
    homeTeam: String,
    awayTeam: String,
    viewModel: LiveScorerViewModel,
    activeFixture: ScheduledFixture? = null
) {
    val isCompleted = activeFixture?.status?.equals("completed", ignoreCase = true) == true
    val (innings1Team, innings2Team) = remember(activeFixture, homeTeam, awayTeam) {
        val tossWinner = activeFixture?.tossWinner ?: ""
        val tossDecision = activeFixture?.tossDecision ?: ""
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
    val inningsList = listOf("$innings1Team Innings", "$innings2Team Innings")

    val fixtureData = activeFixture
    val currentInnings = fixtureData?.currentInnings ?: 1
    var selectedInningsTab by remember { mutableIntStateOf(if (currentInnings == 2) 1 else 0) }

    val battingList = remember(selectedInningsTab, currentInnings, isCompleted, viewModel.state, activeFixture) {
        if (selectedInningsTab == 0) {
            // Innings 1 Batting
            if (isCompleted) {
                fixtureData?.firstInningsBatsmen?.map { b ->
                    val status = getDismissalText(b)
                    val sr = if (b.balls > 0) String.format("%.1f", (b.runs * 100f / b.balls)) else "0.0"
                    BattingCardRow(b.name, status, b.runs, b.balls, b.fours, b.sixes, sr)
                } ?: emptyList()
            } else {
                if (currentInnings == 1) {
                    viewModel.state.batsmenStats.values.map { b ->
                        val status = getDismissalText(b)
                        val sr = if (b.balls > 0) String.format("%.1f", (b.runs * 100f / b.balls)) else "0.0"
                        BattingCardRow(b.name, status, b.runs, b.balls, b.fours, b.sixes, sr)
                    }
                } else {
                    fixtureData?.firstInningsBatsmen?.map { b ->
                        val status = getDismissalText(b)
                        val sr = if (b.balls > 0) String.format("%.1f", (b.runs * 100f / b.balls)) else "0.0"
                        BattingCardRow(b.name, status, b.runs, b.balls, b.fours, b.sixes, sr)
                    } ?: emptyList()
                }
            }
        } else {
            // Innings 2 Batting
            if (isCompleted) {
                fixtureData?.secondInningsBatsmen?.map { b ->
                    val status = getDismissalText(b)
                    val sr = if (b.balls > 0) String.format("%.1f", (b.runs * 100f / b.balls)) else "0.0"
                    BattingCardRow(b.name, status, b.runs, b.balls, b.fours, b.sixes, sr)
                } ?: emptyList()
            } else {
                if (currentInnings == 2) {
                    viewModel.state.batsmenStats.values.map { b ->
                        val status = getDismissalText(b)
                        val sr = if (b.balls > 0) String.format("%.1f", (b.runs * 100f / b.balls)) else "0.0"
                        BattingCardRow(b.name, status, b.runs, b.balls, b.fours, b.sixes, sr)
                    }
                } else {
                    emptyList()
                }
            }
        }
    }

    val bowlingList = remember(selectedInningsTab, currentInnings, isCompleted, viewModel.state, activeFixture) {
        if (selectedInningsTab == 0) {
            // Innings 1 Bowling (Bowled by Innings 2 Team)
            if (isCompleted) {
                fixtureData?.firstInningsBowlers?.map { b ->
                    val overs = "${b.balls / 6}.${b.balls % 6}"
                    val econ = if (b.balls > 0) String.format("%.2f", (b.runsConceded.toFloat() / (b.balls.toFloat() / 6f))) else "0.00"
                    BowlingCardRow(b.name, overs, 0, b.runsConceded, b.wickets, econ)
                } ?: emptyList()
            } else {
                if (currentInnings == 1) {
                    viewModel.state.bowlersStats.values.map { b ->
                        val overs = "${b.balls / 6}.${b.balls % 6}"
                        val econ = if (b.balls > 0) String.format("%.2f", (b.runsConceded.toFloat() / (b.balls.toFloat() / 6f))) else "0.00"
                        BowlingCardRow(b.name, overs, 0, b.runsConceded, b.wickets, econ)
                      }
                } else {
                    fixtureData?.firstInningsBowlers?.map { b ->
                        val overs = "${b.balls / 6}.${b.balls % 6}"
                        val econ = if (b.balls > 0) String.format("%.2f", (b.runsConceded.toFloat() / (b.balls.toFloat() / 6f))) else "0.00"
                        BowlingCardRow(b.name, overs, 0, b.runsConceded, b.wickets, econ)
                    } ?: emptyList()
                }
            }
        } else {
            // Innings 2 Bowling (Bowled by Innings 1 Team)
            if (isCompleted) {
                fixtureData?.secondInningsBowlers?.map { b ->
                    val overs = "${b.balls / 6}.${b.balls % 6}"
                    val econ = if (b.balls > 0) String.format("%.2f", (b.runsConceded.toFloat() / (b.balls.toFloat() / 6f))) else "0.00"
                    BowlingCardRow(b.name, overs, 0, b.runsConceded, b.wickets, econ)
                } ?: emptyList()
            } else {
                if (currentInnings == 2) {
                    viewModel.state.bowlersStats.values.map { b ->
                        val overs = "${b.balls / 6}.${b.balls % 6}"
                        val econ = if (b.balls > 0) String.format("%.2f", (b.runsConceded.toFloat() / (b.balls.toFloat() / 6f))) else "0.00"
                        BowlingCardRow(b.name, overs, 0, b.runsConceded, b.wickets, econ)
                    }
                } else {
                    emptyList()
                }
            }
        }
    }

    val fowList = remember(selectedInningsTab, currentInnings, isCompleted, viewModel.state, activeFixture) {
        if (selectedInningsTab == 0) {
            if (isCompleted) fixtureData?.firstInningsFOW ?: emptyList()
            else {
                if (currentInnings == 1) viewModel.state.fallOfWickets
                else fixtureData?.firstInningsFOW ?: emptyList()
            }
        } else {
            if (isCompleted) fixtureData?.secondInningsFOW ?: emptyList()
            else {
                if (currentInnings == 2) viewModel.state.fallOfWickets
                else fixtureData?.secondInningsFOW ?: emptyList()
            }
        }
    }

    val partnershipsList = remember(selectedInningsTab, currentInnings, isCompleted, viewModel.state, activeFixture) {
        if (selectedInningsTab == 0) {
            if (isCompleted) fixtureData?.firstInningsPartnerships ?: emptyList()
            else {
                if (currentInnings == 1) viewModel.state.partnerships
                else fixtureData?.firstInningsPartnerships ?: emptyList()
            }
        } else {
            if (isCompleted) fixtureData?.secondInningsPartnerships ?: emptyList()
            else {
                if (currentInnings == 2) viewModel.state.partnerships
                else fixtureData?.secondInningsPartnerships ?: emptyList()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Innings switcher
        item {
            TabRow(
                selectedTabIndex = selectedInningsTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) }
            ) {
                inningsList.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedInningsTab == index,
                        onClick = { selectedInningsTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedInningsTab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }

        // Active Innings Batting Scorecard
        item {
            Text(
                text = "Batting",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Batting table headers (Responsive column weights)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Batter", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(3f))
                Text(text = "R", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                Text(text = "B", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                Text(text = "4s", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                Text(text = "6s", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                Text(text = "SR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        }

        if (battingList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Innings not started yet.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(battingList) { batter ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(3f)) {
                            Text(text = batter.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = batter.status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(text = "${batter.r}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        Text(text = "${batter.b}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        Text(text = "${batter.fours}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        Text(text = "${batter.sixes}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        Text(text = batter.sr, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }
        }

        // Active Innings Bowling Scorecard
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bowling",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bowling table headers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Bowler", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(3f))
                Text(text = "O", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                Text(text = "M", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                Text(text = "R", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                Text(text = "W", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                Text(text = "Econ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        }

        if (bowlingList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Innings not started yet.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(bowlingList) { bowler ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = bowler.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(3f))
                        Text(text = bowler.o, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                        Text(text = "${bowler.m}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                        Text(text = "${bowler.r}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                        Text(text = "${bowler.w}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                        Text(text = bowler.econ, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }
        }

        // --- FALL OF WICKETS SECTION ---
        if (fowList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Fall of Wickets",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fowList.forEach { wicket ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${wicket.wicketNumber} - ${wicket.teamRuns}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${wicket.batsmanName} (${wicket.overs} Ov)",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- PARTNERSHIPS SECTION ---
        if (partnershipsList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Partnerships",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        partnershipsList.forEach { p ->
                            val ordinal = when (p.wicketNumber) {
                                1 -> "1st"
                                2 -> "2nd"
                                3 -> "3rd"
                                else -> "${p.wicketNumber}th"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Batsman name & contribution
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = p.batter1Name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${p.batter1ContributionRuns} (${p.batter1ContributionBalls})",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }

                                // Middle total runs / balls
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "${p.runs} Runs",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${p.balls} balls",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "$ordinal Wkt",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Right Batsman name & contribution
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = p.batter2Name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${p.batter2ContributionRuns} (${p.batter2ContributionBalls})",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
