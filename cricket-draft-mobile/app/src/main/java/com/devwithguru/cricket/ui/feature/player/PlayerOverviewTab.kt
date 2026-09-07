package com.devwithguru.cricket.ui.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.devwithguru.cricket.data.api.PlayerStatsDetailData
import com.devwithguru.cricket.data.api.PlayerInsightsDetailData
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.ui.theme.screenConfig

@Composable
fun PlayerOverviewTab(
    playerId: String = "",
    playerName: String = "Unknown Player",
    stats: PlayerStatsDetailData? = null,
    insights: PlayerInsightsDetailData? = null
) {
    val battingStats = if (stats != null) {
        listOf(
            Pair("Runs", stats.runs.toString()),
            Pair("Average", String.format("%.1f", stats.batting_average ?: 0.0)),
            Pair("Strike Rate", String.format("%.1f", stats.strike_rate ?: 0.0))
        )
    } else {
        listOf(
            Pair("Runs", "0"),
            Pair("Average", "0.0"),
            Pair("Strike Rate", "0.0")
        )
    }

    val bowlingStats = if (stats != null) {
        listOf(
            Pair("Wickets", stats.wickets.toString()),
            Pair("Average", String.format("%.1f", stats.bowling_average ?: 0.0)),
            Pair("Best Bowling", stats.best_bowling ?: "-")
        )
    } else {
        listOf(
            Pair("Wickets", "0"),
            Pair("Average", "0.0"),
            Pair("Best Bowling", "-")
        )
    }

    val fieldingStats = if (stats != null) {
        listOf(
            Pair("Catches", (stats.catches ?: 0).toString()),
            Pair("Stumpings", (stats.stumpings ?: 0).toString()),
            Pair("Matches", stats.matches.toString())
        )
    } else {
        listOf(
            Pair("Catches", "0"),
            Pair("Stumpings", "0"),
            Pair("Matches", "0")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player Avatar Circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(50.dp)
            )
        }

        // Player Name & Registration Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = playerName,
                fontSize = screenConfig.scoreTextSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Career Profile",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Profile ID pill + Follow + Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Profile ID: $playerId",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = { },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 32.dp)
            ) {
                Text(text = "Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summary Row: Total Matches, Fifties, Hundreds
        if (stats != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryPill("Matches", stats.matches.toString(), Modifier.weight(1f))
                SummaryPill("50s", (stats.fifties ?: 0).toString(), Modifier.weight(1f))
                SummaryPill("100s", (stats.hundreds ?: 0).toString(), Modifier.weight(1f))
                SummaryPill("Economy", String.format("%.2f", stats.economy ?: 0.0), Modifier.weight(1f))
            }
        }

        val isLightTheme = MaterialTheme.colorScheme.background != Color(0xFF121212)

        // Three Vertical Stat Columns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatColumn(
                    title = "Batting",
                    headerColor = if (isLightTheme) Color(0xFFC53030) else Color(0xFFFF8A80),
                    backgroundColor = if (isLightTheme) Color(0xFFFFF5F5) else Color(0xFFFF5252).copy(alpha = 0.05f),
                    borderColor = if (isLightTheme) Color(0xFFFEB2B2) else Color(0xFFFF5252).copy(alpha = 0.12f),
                    stats = battingStats
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatColumn(
                    title = "Bowling",
                    headerColor = if (isLightTheme) Color(0xFF22543D) else Color(0xFFB9F6CA),
                    backgroundColor = if (isLightTheme) Color(0xFFF0FFF4) else Color(0xFF4CAF50).copy(alpha = 0.05f),
                    borderColor = if (isLightTheme) Color(0xFF9AE6B4) else Color(0xFF4CAF50).copy(alpha = 0.12f),
                    stats = bowlingStats
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatColumn(
                    title = "Fielding",
                    headerColor = if (isLightTheme) Color(0xFF2B6CB0) else Color(0xFF80D8FF),
                    backgroundColor = if (isLightTheme) Color(0xFFEBF8FF) else Color(0xFF2196F3).copy(alpha = 0.05f),
                    borderColor = if (isLightTheme) Color(0xFF90CDF4) else Color(0xFF2196F3).copy(alpha = 0.12f),
                    stats = fieldingStats
                )
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun StatColumn(
    title: String,
    headerColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    stats: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = title,
                color = headerColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            stats.forEach { (label, value) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = value,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}
