package com.devwithguru.cricket.ui.feature.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.devwithguru.cricket.domain.model.ScheduledFixture
import com.devwithguru.cricket.ui.theme.StatusLive
import com.devwithguru.cricket.ui.theme.screenConfig
import com.devwithguru.cricket.ui.feature.player.PlayerMatchesViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentMatchesScreen(
    onNavigateToMatchCenter: (matchId: String) -> Unit,
    onNavigateToTournamentHub: (tournamentId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PlayerMatchesViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadAllFixtures() }
    val fixtures by viewModel.fixtures.collectAsState()
    
    val matchesList = fixtures.map { f ->
        PremiumMatchData(
            id = f.id,
            tournamentName = "${f.homeTeam} vs ${f.awayTeam}",
            stageInfo = "${f.matchType}, ${f.ballType}, ${f.date}",
            teamA = f.homeTeam,
            teamB = f.awayTeam,
            scoreA = if (f.currentInnings == 1) "${f.currentRuns}-${f.currentWickets} (${f.oversBowled})" else "${f.firstInningsRuns ?: 0}",
            scoreB = if (f.currentInnings == 2) "${f.currentRuns}-${f.currentWickets} (${f.oversBowled})" else "Yet to bat",
            isLive = f.status == "Live"
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Recent Matches",
                        fontSize = screenConfig.headingTextSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(matchesList) { match ->
                PremiumMatchCard(
                    tournamentName = match.tournamentName,
                    stageInfo = match.stageInfo,
                    teamA = match.teamA,
                    teamB = match.teamB,
                    scoreA = match.scoreA,
                    scoreB = match.scoreB,
                    isLive = match.isLive,
                    onClick = { onNavigateToMatchCenter(match.id) },
                    onViewTournamentClick = { onNavigateToTournamentHub("corporate-cup") }
                )
            }
        }
    }
}

data class PremiumMatchData(
    val id: String,
    val tournamentName: String?,
    val stageInfo: String,
    val teamA: String,
    val teamB: String,
    val scoreA: String,
    val scoreB: String,
    val isLive: Boolean
)

@Composable
fun PremiumMatchCard(
    tournamentName: String?,
    stageInfo: String,
    teamA: String,
    teamB: String,
    scoreA: String,
    scoreB: String,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onViewTournamentClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stageInfo,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.1).sp
                )
                if (isLive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text(
                            text = "LIVE",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Team A Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = teamA,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = scoreA,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Team B Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = teamB,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = scoreB,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1
                )
            }

            if (!tournamentName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))

                // Tournament footer
                Text(
                    text = tournamentName.uppercase(),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // View Tournament Action Link
                Text(
                    text = "View Tournament",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    modifier = Modifier.clickable { onViewTournamentClick() }
                )
            }
        }
    }
}
