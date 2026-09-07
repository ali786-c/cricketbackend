package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.api.TournamentStandingData
import com.devwithguru.cricket.data.api.TournamentFixtureData2
import com.devwithguru.cricket.data.api.TournamentTeamData
import com.devwithguru.cricket.domain.model.Tournament

@Composable
fun TournamentHomeTab(
    tournament: Tournament? = null,
    fixtures: List<TournamentFixtureData2> = emptyList(),
    teams: List<TournamentTeamData> = emptyList(),
    standings: List<TournamentStandingData> = emptyList(),
    stages: List<com.devwithguru.cricket.domain.model.Stage> = emptyList(),
    onScheduleMatch: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onCreateStage: () -> Unit = {},
    onCreateTeam: () -> Unit = {},
    onStartMatch: (matchId: String, homeTeam: String, awayTeam: String, status: String, tossWinner: String?, tossDecision: String?) -> Unit = { _, _, _, _, _, _ -> },
    onNavigateToMatchCenter: (matchId: String, isScorer: Boolean) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ─── 1. Tournament Details Section ───
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = tournament?.name ?: "Tournament Details",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = tournament?.city ?: "Unknown", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = tournament?.startDate?.take(10) ?: "TBD", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                        Text(
                            text = tournament?.ballType ?: "Tennis Ball",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val sc = when (tournament?.status) {
                        "live" -> Color(0xFF4CAF50)
                        "completed" -> Color(0xFF2196F3)
                        else -> Color(0xFFFF9800)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = sc.copy(alpha = 0.15f)) {
                        Text(
                            text = tournament?.status?.replaceFirstChar { it.uppercase() } ?: "Upcoming",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = sc
                        )
                    }
                }
            }
        }

        // ─── 2. Create Teams Requirement Banner ───
        val hasEnoughTeams = teams.size >= 2
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (hasEnoughTeams) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color(0xFFFF9800).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (hasEnoughTeams) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color(0xFFFF9800).copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (hasEnoughTeams) Icons.Default.Info else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (hasEnoughTeams) MaterialTheme.colorScheme.primary else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (hasEnoughTeams) "Teams Setup Ready" else "Teams Setup Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (hasEnoughTeams) MaterialTheme.colorScheme.onSurface else Color(0xFFE65100)
                    )
                }

                Text(
                    text = if (hasEnoughTeams) {
                        "Minimum requirement met! You have registered ${teams.size} teams. You can now schedule match fixtures."
                    } else {
                        "At least 2 teams must be added to schedule matches in this tournament. Currently registered: ${teams.size}."
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onCreateTeam,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasEnoughTeams) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFF9800),
                        contentColor = if (hasEnoughTeams) MaterialTheme.colorScheme.primary else Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasEnoughTeams) "Add More Teams" else "Create Team",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ─── 3. Scrollable Team List Row ───
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Registered Teams (${teams.size})",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (teams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No teams added yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    teams.forEach { team ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(76.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (team.short_name ?: team.name?.take(3)?.uppercase() ?: "?").take(3).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = team.name ?: "Unknown",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // ─── 4. Top Players Section ───
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    text = "Top Players",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Player statistics will be populated once matches begin.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ─── 5. Recent Matches Section ───
        val completedFixtures = fixtures.filter { it.status?.lowercase() == "completed" || it.match_status?.lowercase() == "completed" }.takeLast(3)
        if (completedFixtures.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Recent Results",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                completedFixtures.forEach { fixture ->
                    val homeName = fixture.home_team?.name ?: "TBD"
                    val awayName = fixture.away_team?.name ?: "TBD"
                    val matchId = fixture.match_id?.toString() ?: fixture.id.toString()

                    val homeScoreText = remember(fixture) {
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
                        if (fixture.current_innings == 2) {
                            "${fixture.current_runs ?: 0}-${fixture.current_wickets ?: 0} (${fixture.overs_bowled ?: "0.0"})"
                        } else if (fixture.current_innings == 1) {
                            "Yet to bat"
                        } else {
                            ""
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable {
                                onNavigateToMatchCenter(matchId, false)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
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
                                    text = "COMPLETED",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            // Home Team
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
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Away Team
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
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Footer Divider + Venue
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
                                    text = "View Scorecard",
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

        // Upcoming Fixtures (next 3)
        val upcomingFixtures = fixtures.filter { it.status?.lowercase() != "completed" && it.match_status?.lowercase() != "completed" }.take(3)
        if (upcomingFixtures.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Upcoming Matches",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                upcomingFixtures.forEach { fixture ->
                    val homeName = fixture.home_team?.name ?: "TBD"
                    val awayName = fixture.away_team?.name ?: "TBD"
                    val matchId = fixture.match_id?.toString() ?: fixture.id.toString()

                    val statusLower = (fixture.match_status ?: fixture.status ?: "scheduled").lowercase()
                    val isLive = statusLower == "live"
                    val isTossCompleted = statusLower == "toss_completed"
                    val isLineupPending = statusLower == "lineup_pending"

                    val (statusLabel, statusColor) = when {
                        isLive -> Pair("Live", Color(0xFFE53935))
                        isTossCompleted -> Pair("Toss Completed", Color(0xFFFB8C00))
                        isLineupPending -> Pair("Lineup Pending", Color(0xFFFB8C00))
                        else -> Pair("Scheduled", MaterialTheme.colorScheme.primary)
                    }

                    val homeScoreText = remember(fixture) {
                        if (!isLive) return@remember ""
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
                        if (!isLive) return@remember ""
                        if (fixture.current_innings == 2) {
                            "${fixture.current_runs ?: 0}-${fixture.current_wickets ?: 0} (${fixture.overs_bowled ?: "0.0"})"
                        } else if (fixture.current_innings == 1) {
                            "Yet to bat"
                        } else {
                            ""
                        }
                    }

                    val actionText = when {
                        isLive -> "Resume Scoring"
                        isTossCompleted || isLineupPending -> "Select Lineup"
                        else -> "Start Match"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable {
                                if (isLive) {
                                    onNavigateToMatchCenter(matchId, true)
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
                                        color = MaterialTheme.colorScheme.onSurface,
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

                            // Footer Divider + Venue
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
            }
        }

        // Action Buttons Row (anchored shortcuts)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable { onScheduleMatch() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(text = "Schedule Match", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable { onCreateStage() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.SportsCricket, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(text = "Create Stage", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
