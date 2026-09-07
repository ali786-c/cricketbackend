package com.devwithguru.cricket.ui.feature.match.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.devwithguru.cricket.ui.theme.screenConfig
import com.devwithguru.cricket.ui.feature.match.screens.MatchStatCompareBar
import com.devwithguru.cricket.ui.feature.match.scorer.LiveScorerViewModel
import com.devwithguru.cricket.domain.model.ScheduledFixture

@Composable
fun MatchStatsTab(
    matchId: String,
    homeTeam: String,
    awayTeam: String,
    viewModel: LiveScorerViewModel,
    activeFixture: ScheduledFixture? = null
) {
    val fixtureData = activeFixture ?: remember(matchId) { null }
    val currentInnings = fixtureData?.currentInnings ?: 1
    val isCompleted = activeFixture?.status?.lowercase() == "completed"

    // 1. RUNS & BALLS CALCULATIONS
    val homeRuns = if (currentInnings == 1) {
        viewModel.state.runs
    } else {
        fixtureData?.firstInningsRuns ?: 0
    }
    val homeBalls = if (currentInnings == 1) {
        viewModel.state.totalBalls
    } else {
        fixtureData?.firstInningsBowlers?.sumOf { it.balls } ?: 0
    }

    val awayRuns = if (currentInnings == 2) {
        viewModel.state.runs
    } else if (isCompleted) {
        fixtureData?.currentRuns ?: 0
    } else {
        0
    }
    val awayBalls = if (currentInnings == 2) {
        viewModel.state.totalBalls
    } else if (isCompleted) {
        fixtureData?.secondInningsBowlers?.sumOf { it.balls } ?: 0
    } else {
        0
    }

    // 2. RUN RATES
    val homeRR = if (homeBalls > 0) (homeRuns.toFloat() / (homeBalls.toFloat() / 6f)) else 0f
    val awayRR = if (awayBalls > 0) (awayRuns.toFloat() / (awayBalls.toFloat() / 6f)) else 0f

    val homeRRStr = String.format("%.2f", homeRR)
    val awayRRStr = String.format("%.2f", awayRR)
    val rrHomeRatio = if (homeRR + awayRR > 0) homeRR / (homeRR + awayRR) else 0.5f

    // 3. BOUNDARIES (FOURS/SIXES)
    val homeBoundaries = if (currentInnings == 1) {
        viewModel.state.batsmenStats.values.sumOf { it.fours + it.sixes }
    } else {
        fixtureData?.firstInningsBatsmen?.sumOf { it.fours + it.sixes } ?: 0
    }
    val awayBoundaries = if (currentInnings == 2) {
        viewModel.state.batsmenStats.values.sumOf { it.fours + it.sixes }
    } else if (isCompleted) {
        fixtureData?.secondInningsBatsmen?.sumOf { it.fours + it.sixes } ?: 0
    } else {
        0
    }
    val boundaryHomeRatio = if (homeBoundaries + awayBoundaries > 0) {
        homeBoundaries.toFloat() / (homeBoundaries + awayBoundaries).toFloat()
    } else {
        0.5f
    }

    // 4. EXTRAS
    val homeExtras = if (currentInnings == 1) {
        viewModel.state.extras
    } else {
        activeFixture?.firstInningsExtras ?: 0
    }
    val awayExtras = if (currentInnings == 2) {
        viewModel.state.extras
    } else if (isCompleted) {
        activeFixture?.secondInningsExtras ?: 0
    } else {
        0
    }
    val extrasHomeRatio = if (homeExtras + awayExtras > 0) {
        homeExtras.toFloat() / (homeExtras + awayExtras).toFloat()
    } else {
        0.5f
    }

    // 5. DOT BALLS PERCENTAGE
    val homeDotBalls = if (currentInnings == 1) {
        viewModel.state.dotBalls
    } else {
        activeFixture?.firstInningsDotBalls ?: 0
    }
    val homeDotPct = if (homeBalls > 0) (homeDotBalls.toFloat() / homeBalls.toFloat() * 100f) else 0f

    val awayDotBalls = if (currentInnings == 2) {
        viewModel.state.dotBalls
    } else if (isCompleted) {
        activeFixture?.secondInningsDotBalls ?: 0
    } else {
        0
    }
    val awayDotPct = if (awayBalls > 0) (awayDotBalls.toFloat() / awayBalls.toFloat() * 100f) else 0f

    val dotHomeRatio = if (homeDotPct + awayDotPct > 0) homeDotPct / (homeDotPct + awayDotPct) else 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Team Comparison Statistics",
            color = MaterialTheme.colorScheme.primary,
            fontSize = screenConfig.bodyTextSize,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Run Rate Stat Comparison
                MatchStatCompareBar(
                    label = "Run Rate",
                    homeVal = homeRRStr,
                    awayVal = awayRRStr,
                    homeRatio = rrHomeRatio,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Boundaries Stat Comparison
                MatchStatCompareBar(
                    label = "Boundaries (Fours/Sixes)",
                    homeVal = "$homeBoundaries",
                    awayVal = "$awayBoundaries",
                    homeRatio = boundaryHomeRatio,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Extras Comparison
                MatchStatCompareBar(
                    label = "Extras",
                    homeVal = "$homeExtras",
                    awayVal = "$awayExtras",
                    homeRatio = extrasHomeRatio,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Dot Balls % Comparison
                MatchStatCompareBar(
                    label = "Dot Ball Percentage",
                    homeVal = String.format("%.0f%%", homeDotPct),
                    awayVal = String.format("%.0f%%", awayDotPct),
                    homeRatio = dotHomeRatio,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam
                )
            }
        }
    }
}
