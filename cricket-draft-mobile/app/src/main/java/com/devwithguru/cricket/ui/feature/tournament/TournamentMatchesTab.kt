package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.api.TournamentFixtureData2

@Composable
fun TournamentMatchesTab(
    fixtures: List<TournamentFixtureData2> = emptyList(),
    teamCount: Int = 0,
    onNavigateToMatchCenter: (String, Boolean) -> Unit,
    onAddTeam: () -> Unit = {},
    onScheduleMatch: () -> Unit = {},
    onStartMatch: (matchId: String, homeTeam: String, awayTeam: String, status: String, tossWinner: String?, tossDecision: String?) -> Unit = { _, _, _, _, _, _ -> }
) {
    val allFixtures = fixtures

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

        items(allFixtures) { fixture ->
            val homeName = fixture.home_team?.name ?: "TBD"
            val awayName = fixture.away_team?.name ?: "TBD"
            val matchId = fixture.match_id?.toString() ?: fixture.id.toString()

            val statusLower = (fixture.match_status ?: fixture.status ?: "scheduled").lowercase()
            val isCompleted = statusLower == "completed"
            val isLive = statusLower == "live"
            val isTossCompleted = statusLower == "toss_completed"
            val isLineupPending = statusLower == "lineup_pending"

            val (statusLabel, statusColor) = when {
                isCompleted -> Pair("Completed", Color(0xFF4CAF50))
                isLive -> Pair("Live", Color(0xFFE53935))
                isTossCompleted -> Pair("Toss Completed", Color(0xFFFB8C00))
                isLineupPending -> Pair("Lineup Pending", Color(0xFFFB8C00))
                else -> Pair("Scheduled", MaterialTheme.colorScheme.primary)
            }

            val homeScoreText = remember(fixture) {
                if (!isLive && !isCompleted) return@remember ""
                if (fixture.current_innings == 2) {
                    fixture.first_innings_runs?.let { r ->
                        val w = fixture.first_innings_wickets ?: 0
                        "$r-$w"
                    } ?: ""
                } else if (fixture.current_innings == 1) {
                    "${fixture.current_runs ?: 0}-${fixture.current_wickets ?: 0} (${fixture.overs_bowled ?: "0.0"})"
                } else {
                    ""
                }
            }

            val awayScoreText = remember(fixture) {
                if (!isLive && !isCompleted) return@remember ""
                if (fixture.current_innings == 2) {
                    "${fixture.current_runs ?: 0}-${fixture.current_wickets ?: 0} (${fixture.overs_bowled ?: "0.0"})"
                } else if (fixture.current_innings == 1) {
                    "Yet to bat"
                } else {
                    ""
                }
            }

            val actionText = when {
                isCompleted -> "View Scorecard"
                isLive -> "Resume Scoring"
                isTossCompleted || isLineupPending -> "Select Lineup"
                else -> "Start Match"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .clickable {
                        if (isCompleted || isLive) {
                            onNavigateToMatchCenter(matchId, isLive)
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
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header Row: Round, Date + Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val roundText = if (!fixture.round_name.isNullOrBlank()) fixture.round_name else "Match"
                        val dateText = fixture.scheduled_at?.take(15) ?: ""
                        Text(
                            text = "$roundText • Club Cricket • $dateText",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = statusLabel.uppercase(),
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Home Team Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = homeName.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = homeName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (homeScoreText.isNotEmpty()) {
                            Text(
                                text = homeScoreText,
                                color = if (homeScoreText == "Yet to bat") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Away Team Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = awayName.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = awayName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (awayScoreText.isNotEmpty()) {
                            Text(
                                text = awayScoreText,
                                color = if (awayScoreText == "Yet to bat") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Footer Divider + Actions
                    val venueText = fixture.venue.takeIf { !it.isNullOrBlank() && it != "TBD" }?.let { "Venue: $it" } ?: "Venue: TBD"
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = venueText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = actionText,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
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
