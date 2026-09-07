package com.devwithguru.cricket.ui.feature.match.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.ui.theme.screenConfig
import com.devwithguru.cricket.ui.feature.match.scorer.LiveScorerViewModel
import com.devwithguru.cricket.domain.model.ScheduledFixture

private data class TempPlayerMVP(
    val name: String,
    var runs: Int = 0,
    var ballsFaced: Int = 0,
    var fours: Int = 0,
    var sixes: Int = 0,
    var wickets: Int = 0,
    var ballsBowled: Int = 0,
    var runsConceded: Int = 0
)

@Composable
fun MatchSuperStarsTab(
    matchId: String,
    viewModel: LiveScorerViewModel,
    activeFixture: ScheduledFixture? = null
) {
    val fixtureData = activeFixture ?: remember(matchId) { null }
    val currentInnings = fixtureData?.currentInnings ?: 1
    val isCompleted = activeFixture?.status?.lowercase() == "completed"

    val mvpStars = remember(currentInnings, viewModel.state, isCompleted) {
        val playersMap = mutableMapOf<String, TempPlayerMVP>()

        fun getOrCreate(name: String) = playersMap.getOrPut(name) { TempPlayerMVP(name) }

        // --- INNINGS 1 DATA ---
        if (isCompleted) {
            fixtureData?.firstInningsBatsmen?.forEach { b ->
                val p = getOrCreate(b.name)
                p.runs += b.runs
                p.ballsFaced += b.balls
                p.fours += b.fours
                p.sixes += b.sixes
            }
            fixtureData?.firstInningsBowlers?.forEach { b ->
                val p = getOrCreate(b.name)
                p.wickets += b.wickets
                p.ballsBowled += b.balls
                p.runsConceded += b.runsConceded
            }
        } else {
            if (currentInnings == 1) {
                // Innings 1 is Live
                viewModel.state.batsmenStats.values.forEach { b ->
                    val p = getOrCreate(b.name)
                    p.runs += b.runs
                    p.ballsFaced += b.balls
                    p.fours += b.fours
                    p.sixes += b.sixes
                }
                viewModel.state.bowlersStats.values.forEach { b ->
                    val p = getOrCreate(b.name)
                    p.wickets += b.wickets
                    p.ballsBowled += b.balls
                    p.runsConceded += b.runsConceded
                }
            } else {
                // Innings 1 is completed (read from fixture)
                fixtureData?.firstInningsBatsmen?.forEach { b ->
                    val p = getOrCreate(b.name)
                    p.runs += b.runs
                    p.ballsFaced += b.balls
                    p.fours += b.fours
                    p.sixes += b.sixes
                }
                fixtureData?.firstInningsBowlers?.forEach { b ->
                    val p = getOrCreate(b.name)
                    p.wickets += b.wickets
                    p.ballsBowled += b.balls
                    p.runsConceded += b.runsConceded
                }
            }
        }

        // --- INNINGS 2 DATA ---
        if (isCompleted) {
            // Innings 2 is completed (read from fixture)
            fixtureData?.secondInningsBatsmen?.forEach { b ->
                val p = getOrCreate(b.name)
                p.runs += b.runs
                p.ballsFaced += b.balls
                p.fours += b.fours
                p.sixes += b.sixes
            }
            fixtureData?.secondInningsBowlers?.forEach { b ->
                val p = getOrCreate(b.name)
                p.wickets += b.wickets
                p.ballsBowled += b.balls
                p.runsConceded += b.runsConceded
            }
        } else {
            if (currentInnings == 2) {
                // Innings 2 is Live
                viewModel.state.batsmenStats.values.forEach { b ->
                    val p = getOrCreate(b.name)
                    p.runs += b.runs
                    p.ballsFaced += b.balls
                    p.fours += b.fours
                    p.sixes += b.sixes
                }
                viewModel.state.bowlersStats.values.forEach { b ->
                    val p = getOrCreate(b.name)
                    p.wickets += b.wickets
                    p.ballsBowled += b.balls
                    p.runsConceded += b.runsConceded
                }
            }
        }

        // --- CALCULATE MVP POINTS & MAP TO MVPStarRow ---
        playersMap.values
            .map { p ->
                val pts = p.runs + p.fours + (p.sixes * 2) + (p.wickets * 20)
                val statText = when {
                    p.ballsFaced > 0 && p.ballsBowled > 0 -> {
                        "${p.runs} runs & ${p.wickets} wickets"
                    }
                    p.ballsFaced > 0 -> {
                        "${p.runs} runs off ${p.ballsFaced} balls"
                    }
                    p.ballsBowled > 0 -> {
                        val overs = "${p.ballsBowled / 6}.${p.ballsBowled % 6}"
                        "$overs overs, ${p.wickets} wickets"
                    }
                    else -> "Did not bat or bowl"
                }
                MVPStarRow(rank = 0, name = p.name, stat = statText, pts = pts)
            }
            .filter { it.pts > 0 } // Only display active contributors
            .sortedByDescending { it.pts }
            .mapIndexed { index, row -> row.copy(rank = index + 1) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Match Super Stars (MVP Rankings)",
                color = MaterialTheme.colorScheme.primary,
                fontSize = screenConfig.bodyTextSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (mvpStars.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No super star performances recorded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(mvpStars) { star ->
                val isTopStar = star.rank == 1
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isTopStar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTopStar) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Rank Circle
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isTopStar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${star.rank}",
                                    color = if (isTopStar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = star.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = screenConfig.bodyTextSize,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isTopStar) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Match MVP",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = star.stat,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${star.pts}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = screenConfig.bodyTextSize,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "MVP Pts",
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
