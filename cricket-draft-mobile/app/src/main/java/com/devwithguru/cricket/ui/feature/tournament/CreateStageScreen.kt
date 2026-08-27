package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.domain.model.StageType

/**
 * Create Stage Screen — PRD §13, §15-§18, §46
 *
 * PRD §46 UX principle:
 * "Instead of 'Configure Stage Parameters', show:
 *  'How do you want this stage to work?'"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStageScreen(
    tournamentId: String,
    stageNumber: Int = 1,
    availableTeamCount: Int = 0,
    onStageCreated: (
        name: String,
        type: StageType,
        numberOfTeams: Int,
        matchesPerTeam: Int,
        pointsWin: Int,
        pointsTie: Int,
        qualificationRule: String,
        qualificationCount: Int
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf<StageType?>(null) }
    var stageName by remember { mutableStateOf("") }
    var numberOfTeams by remember { mutableStateOf("") }
    var matchesPerTeam by remember { mutableStateOf("1") }
    var pointsWin by remember { mutableStateOf("2") }
    var pointsTie by remember { mutableStateOf("1") }
    var qualificationRule by remember { mutableStateOf("top_2") }
    var qualificationCount by remember { mutableStateOf("2") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Create Stage", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Guided Question (PRD §46) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.SportsCricket,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "How do you want Stage $stageNumber to work?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Error ──
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // ── Stage Type Selection (PRD §46 friendly options) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Stage Type", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    val stageOptions = listOf(
                        StageType.POINTS_TABLE to "Every team plays every other team",
                        StageType.KNOCKOUT to "Single elimination — loser goes home",
                        StageType.QUALIFIER to "Qualification match for next stage",
                        StageType.SERIES to "Multiple matches between same teams",
                        StageType.SEMI_FINAL to "Semi-final stage",
                        StageType.FINAL to "Championship match"
                    )

                    stageOptions.forEach { (type, desc) ->
                        val isSelected = selectedType == type
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedType = type
                                    // Auto-fill stage name based on type
                                    if (stageName.isBlank()) {
                                        stageName = when (type) {
                                            StageType.POINTS_TABLE -> "Group ${('A' + stageNumber - 1)}"
                                            else -> type.displayName
                                        }
                                    }
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedType = type
                                    if (stageName.isBlank()) {
                                        stageName = when (type) {
                                            StageType.POINTS_TABLE -> "Group ${('A' + stageNumber - 1)}"
                                            else -> type.displayName
                                        }
                                    }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(type.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // ── Stage Configuration (shown after type selected) ──
            if (selectedType != null) {
                // Stage Name
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Stage Name", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = stageName,
                            onValueChange = { stageName = it },
                            placeholder = { Text("e.g., Group A, Semi Final 1", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )
                    }
                }

                // Points Table specific config
                if (selectedType == StageType.POINTS_TABLE) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("League Settings", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = numberOfTeams,
                                onValueChange = { numberOfTeams = it.filter { c -> c.isDigit() } },
                                label = { Text("Number of Teams", fontSize = 12.sp) },
                                placeholder = { Text("e.g., ${availableTeamCount.coerceAtLeast(2)}", fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )

                            OutlinedTextField(
                                value = matchesPerTeam,
                                onValueChange = { matchesPerTeam = it.filter { c -> c.isDigit() } },
                                label = { Text("Matches Per Team", fontSize = 12.sp) },
                                placeholder = { Text("1 = every team plays every other once", fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )

                            // Points System (PRD §15)
                            Text("Points System", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = pointsWin,
                                    onValueChange = { pointsWin = it.filter { c -> c.isDigit() } },
                                    label = { Text("Win", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                OutlinedTextField(
                                    value = pointsTie,
                                    onValueChange = { pointsTie = it.filter { c -> c.isDigit() } },
                                    label = { Text("Tie/NR", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                            }

                            // Qualification Rules (PRD §16)
                            Text("Qualification Rules", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("top_1" to "Top 1", "top_2" to "Top 2", "top_4" to "Top 4").forEach { (rule, label) ->
                                    val isSelected = qualificationRule == rule
                                    Box(
                                        modifier = Modifier.weight(1f).height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .clickable { qualificationRule = rule },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Create Button
                Button(
                    onClick = {
                        when {
                            stageName.isBlank() -> errorMessage = "Please enter a stage name"
                            selectedType == null -> errorMessage = "Please select a stage type"
                            else -> {
                                onStageCreated(
                                    stageName.trim(),
                                    selectedType!!,
                                    numberOfTeams.toIntOrNull() ?: availableTeamCount,
                                    matchesPerTeam.toIntOrNull() ?: 1,
                                    pointsWin.toIntOrNull() ?: 2,
                                    pointsTie.toIntOrNull() ?: 1,
                                    qualificationRule,
                                    qualificationCount.toIntOrNull() ?: 2
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = selectedType != null && stageName.isNotBlank()
                ) {
                    Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CREATE STAGE", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
