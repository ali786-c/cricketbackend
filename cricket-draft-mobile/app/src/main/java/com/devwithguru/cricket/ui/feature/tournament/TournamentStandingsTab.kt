package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.api.TournamentStandingData

@Composable
fun TournamentStandingsTab(
    standings: List<TournamentStandingData> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tournament Standings",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (standings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No standings available yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Pos", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(text = "Team", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(4f))
                        Text(text = "P", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(text = "W", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(text = "L", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(text = "Pts", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                        Text(text = "NRR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2f), textAlign = TextAlign.End)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))

                    // Table Rows
                    standings.forEach { standing ->
                        val pos = standing.position ?: 0
                        val teamName = standing.team?.name ?: "Unknown"
                        val isQualifying = pos <= 2

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isQualifying) MaterialTheme.colorScheme.primary.copy(alpha = 0.03f) else Color.Transparent)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "$pos", color = if (isQualifying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (isQualifying) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(4f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isQualifying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = teamName,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isQualifying) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                            Text(text = "${standing.played}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(text = "${standing.wins}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(text = "${standing.losses}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(text = "${standing.points}", color = if (isQualifying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                            Text(text = String.format("%.2f", standing.net_run_rate ?: 0.0), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(2f), textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
}
