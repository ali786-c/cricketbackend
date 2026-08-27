package com.devwithguru.cricket.ui.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.api.PlayerStatsDetailData

@Composable
fun PlayerStatsTab(
    stats: PlayerStatsDetailData? = null
) {
    var activeSubTab by remember { mutableStateOf("BAT") }
    val subTabs = listOf("BAT", "BOWL", "SUMMARY")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Segmented Sub-Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            subTabs.forEach { tab ->
                val isActive = activeSubTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeSubTab = tab }
                        .background(if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Table Content
        when (activeSubTab) {
            "BAT" -> {
                if (stats != null) {
                    StatsTableHeader()
                    StatsTableRow("Matches", stats.matches.toString())
                    StatsTableRow("Runs", stats.runs.toString())
                    StatsTableRow("Average", String.format("%.1f", stats.batting_average ?: 0.0))
                    StatsTableRow("Strike Rate", String.format("%.1f", stats.strike_rate ?: 0.0))
                    StatsTableRow("50s", (stats.fifties ?: 0).toString())
                    StatsTableRow("100s", (stats.hundreds ?: 0).toString())
                } else {
                    EmptyStatsView("No batting data available")
                }
            }
            "BOWL" -> {
                if (stats != null) {
                    StatsTableHeader()
                    StatsTableRow("Wickets", stats.wickets.toString())
                    StatsTableRow("Average", String.format("%.1f", stats.bowling_average ?: 0.0))
                    StatsTableRow("Economy", String.format("%.2f", stats.economy ?: 0.0))
                    StatsTableRow("Best Bowling", stats.best_bowling ?: "-")
                } else {
                    EmptyStatsView("No bowling data available")
                }
            }
            "SUMMARY" -> {
                if (stats != null) {
                    StatsTableHeader()
                    StatsTableRow("Total Matches", stats.matches.toString())
                    StatsTableRow("Total Runs", stats.runs.toString())
                    StatsTableRow("Total Wickets", stats.wickets.toString())
                    StatsTableRow("Batting Avg", String.format("%.1f", stats.batting_average ?: 0.0))
                    StatsTableRow("Bowling Avg", String.format("%.1f", stats.bowling_average ?: 0.0))
                    StatsTableRow("Strike Rate", String.format("%.1f", stats.strike_rate ?: 0.0))
                    StatsTableRow("Economy", String.format("%.2f", stats.economy ?: 0.0))
                    StatsTableRow("50s", (stats.fifties ?: 0).toString())
                    StatsTableRow("100s", (stats.hundreds ?: 0).toString())
                    StatsTableRow("Catches", (stats.catches ?: 0).toString())
                    StatsTableRow("Stumpings", (stats.stumpings ?: 0).toString())
                } else {
                    EmptyStatsView("No stats available yet")
                }
            }
        }
    }
}

@Composable
private fun StatsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Metric",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Value",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
}

@Composable
private fun StatsTableRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}

@Composable
private fun EmptyStatsView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}
