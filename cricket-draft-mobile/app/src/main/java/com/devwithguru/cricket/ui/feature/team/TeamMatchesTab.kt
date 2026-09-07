package com.devwithguru.cricket.ui.feature.team

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.domain.model.Fixture

@Composable
fun TeamMatchesTab(fixtures: List<Fixture> = emptyList()) {
    if (fixtures.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No scheduled matches found for this team.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(fixtures) { fixture ->
                val homeName = fixture.homeTeamName
                val awayName = fixture.awayTeamName
                val matchId = fixture.id

                val statusLower = fixture.status.lowercase()
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
                    if (fixture.currentInnings == 2) {
                        fixture.firstInningsRuns?.let { r ->
                            val w = fixture.firstInningsWickets ?: 0
                            "$r-$w"
                        } ?: ""
                    } else if (fixture.currentInnings == 1) {
                        "${fixture.currentRuns ?: 0}-${fixture.currentWickets ?: 0} (${fixture.oversBowled ?: "0.0"})"
                    } else {
                        ""
                    }
                }

                val awayScoreText = remember(fixture) {
                    if (!isLive && !isCompleted) return@remember ""
                    if (fixture.currentInnings == 2) {
                        "${fixture.currentRuns ?: 0}-${fixture.currentWickets ?: 0} (${fixture.oversBowled ?: "0.0"})"
                    } else if (fixture.currentInnings == 1) {
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
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header Row: Round, Date + Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val roundText = if (fixture.roundName.isNotBlank()) fixture.roundName else "Match"
                            val dateText = fixture.scheduledDate?.take(15) ?: ""
                            Text(
                                text = "$roundText • Club Cricket • $dateText",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = statusLabel.uppercase(),
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = homeName.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = homeName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            if (homeScoreText.isNotEmpty()) {
                                Text(
                                    text = homeScoreText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = awayName.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = awayName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            if (awayScoreText.isNotEmpty()) {
                                Text(
                                    text = awayScoreText,
                                    color = if (awayScoreText == "Yet to bat") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Footer Divider + Actions
                        val venueText = fixture.venue ?: "TBD"
                        if (venueText.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = venueText,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
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
            }
        }
    }
}
