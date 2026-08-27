package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.api.TournamentFixtureData2

@Composable
fun TournamentMatchesTab(
    fixtures: List<TournamentFixtureData2> = emptyList(),
    teamCount: Int = 0,
    onNavigateToMatchCenter: (String) -> Unit,
    onAddTeam: () -> Unit = {},
    onScheduleMatch: () -> Unit = {},
    onStartMatch: (matchId: String, homeTeam: String, awayTeam: String, status: String, tossWinner: String?, tossDecision: String?) -> Unit = { _, _, _, _, _, _ -> }
) {
    val completedFixtures = fixtures.filter { it.status == "Completed" || it.match_status == "Completed" }
    val upcomingFixtures = fixtures.filter { it.status != "Completed" && it.match_status != "Completed" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (teamCount < 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4), // Light yellow warning background
                        contentColor = Color(0xFFF57F17) // Dark amber warning text
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Warning: Team count too low",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "At least 2 teams are required to schedule matches. Please register more teams in the 'Teams' tab first.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Quick Action Bar at the top (if fixtures exist)
        if (fixtures.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onScheduleMatch,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Match", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Upcoming matches
        if (upcomingFixtures.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming Matches (${upcomingFixtures.size})",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(upcomingFixtures) { fixture ->
                val homeName = fixture.home_team?.name ?: "TBD"
                val awayName = fixture.away_team?.name ?: "TBD"
                val matchId = fixture.match_id?.toString() ?: fixture.id.toString()
                val isLive = fixture.status?.lowercase() == "live" || fixture.match_status?.lowercase() == "live"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable {
                            if (isLive) {
                                onNavigateToMatchCenter(matchId)
                            } else {
                                onStartMatch(
                                    matchId,
                                    homeName,
                                    awayName,
                                    fixture.status ?: "scheduled",
                                    fixture.toss_winner,
                                    fixture.toss_decision
                                )
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "$homeName vs $awayName",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    if (isLive) {
                                        onNavigateToMatchCenter(matchId)
                                    } else {
                                        onStartMatch(
                                            matchId,
                                            homeName,
                                            awayName,
                                            fixture.status ?: "scheduled",
                                            fixture.toss_winner,
                                            fixture.toss_decision
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isLive) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                val btnText = when {
                                    isLive -> "Score"
                                    fixture.status?.lowercase() == "toss_completed" -> "Lineup"
                                    else -> "Start"
                                }
                                Text(btnText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            fixture.scheduled_at?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                        fixture.venue?.let { venue ->
                            Text(text = venue, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Completed matches
        if (completedFixtures.isNotEmpty()) {
            item {
                Text(
                    text = "Completed Matches (${completedFixtures.size})",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(completedFixtures) { fixture ->
                val homeName = fixture.home_team?.name ?: "TBD"
                val awayName = fixture.away_team?.name ?: "TBD"
                val matchId = fixture.match_id?.toString() ?: fixture.id.toString()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToMatchCenter(matchId) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "$homeName vs $awayName",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Text(text = "Completed", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            fixture.scheduled_at?.let {
                                Text(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Empty state
        if (fixtures.isEmpty()) {
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
                            text = "No matches scheduled yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onScheduleMatch,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Match", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
