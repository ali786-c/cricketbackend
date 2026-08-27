package com.devwithguru.cricket.ui.feature.player

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.ui.feature.player.PlayerMatchesViewModel
import com.devwithguru.cricket.data.api.PlayerMatchData
import com.devwithguru.cricket.domain.model.ScheduledFixture
import com.devwithguru.cricket.ui.theme.StatusLive
import com.devwithguru.cricket.ui.theme.StatusUpcoming
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

@Composable
fun PlayerMatchesTab(
    onStartScheduledMatch: ((ScheduledFixture) -> Unit)? = null,
    matchData: List<PlayerMatchData> = emptyList(),
    viewModel: PlayerMatchesViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadAllFixtures() }
    val allFixtures by viewModel.fixtures.collectAsState()
    val liveMatches = allFixtures.filter { it.status == "Live" }
    val scheduledMatches = allFixtures.filter { it.status == "Scheduled" }
    val completedMatches = allFixtures.filter { it.status == "Completed" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. LIVE MATCHES (Ongoing) ---
        if (liveMatches.isNotEmpty()) {
            item {
                Text(
                    text = "Live Matches (Ongoing)",
                    color = StatusLive,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(liveMatches) { fixture ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StatusLive.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${fixture.homeTeam} vs ${fixture.awayTeam}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusLive.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = StatusLive,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        val inningsLabel = if (fixture.currentInnings == 2) " (2nd Innings)" else " (1st Innings)"
                        Text(
                            text = "Score: ${fixture.currentRuns}/${fixture.currentWickets} (${fixture.oversBowled} ov)$inningsLabel",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${fixture.overs} Overs • ${fixture.matchType} • ${fixture.ballType}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )

                        Button(
                            onClick = { onStartScheduledMatch?.invoke(fixture) },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusLive, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESUME SCORING", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // --- 2. SCHEDULED FIXTURES ---
        if (scheduledMatches.isNotEmpty()) {
            item {
                Text(
                    text = "Scheduled Fixtures",
                    color = StatusUpcoming,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(scheduledMatches) { fixture ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${fixture.homeTeam} vs ${fixture.awayTeam}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StatusUpcoming.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Scheduled",
                                    color = StatusUpcoming,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "${fixture.date} • ${fixture.time}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "${fixture.overs} Overs • ${fixture.matchType} • ${fixture.ballType}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )

                        if (fixture.venue.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = fixture.venue,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onStartScheduledMatch?.invoke(fixture) },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START MATCH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // --- 3. MATCH HISTORY (from API) ---
        if (matchData.isNotEmpty()) {
            item {
                Text(
                    text = "Match History",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(matchData) { match ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = buildString {
                                    match.tournament_name?.let { append(it) }
                                    match.opponent?.let {
                                        if (isNotEmpty()) append(" vs ")
                                        append(it)
                                    }
                                    if (isEmpty()) append("Match #${match.id}")
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = match.date ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        // Performance line
                        val perf = buildString {
                            match.runs?.let { append("$it runs") }
                            match.wickets?.let {
                                if (isNotEmpty()) append(" & ")
                                append("$it wickets")
                            }
                            match.overs_bowled?.let {
                                if (isNotEmpty()) append(" (${it} ov)")
                            }
                            if (isEmpty()) append("No performance data")
                        }
                        Text(
                            text = perf,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Match type + venue
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            match.match_type?.let { type ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = type,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            match.venue?.let { venue ->
                                Text(
                                    text = venue,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        match.result?.let { result ->
                            Text(
                                text = result,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 4. COMPLETED FIXTURES from Room ---
        if (completedMatches.isNotEmpty() && matchData.isEmpty()) {
            item {
                Text(
                    text = "Completed Fixtures",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(completedMatches) { fixture ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${fixture.homeTeam} vs ${fixture.awayTeam}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = fixture.date,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "Score: ${fixture.currentRuns}/${fixture.currentWickets} (${fixture.oversBowled} ov) - Completed",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 5. EMPTY STATE ---
        if (liveMatches.isEmpty() && scheduledMatches.isEmpty() && matchData.isEmpty() && completedMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matches yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Your matches will appear here",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
