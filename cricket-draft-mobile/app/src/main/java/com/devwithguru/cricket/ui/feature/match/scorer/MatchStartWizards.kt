package com.devwithguru.cricket.ui.feature.match.scorer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.ui.theme.screenConfig

@Composable
fun SelectOpeningStrikerDialog(
    visible: Boolean,
    battingSquadList: List<String>,
    onSelect: (String) -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = {}, // Force selection
            confirmButton = {},
            title = {
                Text(
                    text = "Select Opening Striker",
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    battingSquadList.forEach { name ->
                        Button(
                            onClick = { onSelect(name) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = name)
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SelectOpeningNonStrikerDialog(
    visible: Boolean,
    strikerName: String,
    battingSquadList: List<String>,
    onSelect: (String) -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = {}, // Force selection
            confirmButton = {},
            title = {
                Text(
                    text = "Select Opening Non-Striker",
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    battingSquadList.filter { it != strikerName }.forEach { name ->
                        Button(
                            onClick = { onSelect(name) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = name)
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SelectOpeningBatsmenDialog(
    visible: Boolean,
    battingSquadList: List<String>,
    onConfirm: (striker: String, nonStriker: String) -> Unit
) {
    if (visible) {
        var striker by remember { mutableStateOf<String?>(null) }
        var nonStriker by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {}, // Force selection
            confirmButton = {
                Button(
                    onClick = {
                        if (striker != null && nonStriker != null) {
                            onConfirm(striker!!, nonStriker!!)
                        }
                    },
                    enabled = striker != null && nonStriker != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirm Openers")
                }
            },
            title = {
                Text(
                    text = "Select Opening Batsmen",
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose one Striker (🏏) and one Non-Striker (🏃).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    battingSquadList.forEach { name ->
                        val isStriker = striker == name
                        val isNonStriker = nonStriker == name
                        val isSelected = isStriker || isNonStriker
                        
                        val borderColor = when {
                            isStriker -> MaterialTheme.colorScheme.primary
                            isNonStriker -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        }

                        Button(
                            onClick = {
                                when {
                                    isStriker -> {
                                        striker = null
                                    }
                                    isNonStriker -> {
                                        nonStriker = null
                                    }
                                    else -> {
                                        if (striker == null) {
                                            striker = name
                                        } else if (nonStriker == null) {
                                            nonStriker = name
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) borderColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isStriker) {
                                    Text("🏏 Striker", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else if (isNonStriker) {
                                    Text("🏃 Non-Striker", color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SelectOpeningBowlerDialog(
    visible: Boolean,
    bowlingSquadList: List<String>,
    onSelect: (String) -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = {}, // Force selection
            confirmButton = {},
            title = {
                Text(
                    text = "Select Opening Bowler",
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bowlingSquadList.forEach { name ->
                        Button(
                            onClick = { onSelect(name) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = name)
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
