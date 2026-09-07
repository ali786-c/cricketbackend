package com.devwithguru.cricket.ui.feature.match.scorer

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.ui.theme.screenConfig

@Composable
fun SelectNextBowlerDialog(
    visible: Boolean,
    bowlerName: String,
    bowlingSquadList: List<String>,
    onSelect: (String) -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = {}, // Force selection
            confirmButton = {},
            title = {
                Text(
                    text = "Select Next Bowler",
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Over Completed! Choose bowler for the next over:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    val filteredBowlers = bowlingSquadList.filter { it != bowlerName }
                    val finalBowlersList = if (filteredBowlers.isEmpty()) bowlingSquadList else filteredBowlers

                    finalBowlersList.forEach { bowler ->
                        val isCurrent = false
                        Button(
                            onClick = { onSelect(bowler) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                contentColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = bowler)
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
fun SelectNextBatsmanDialog(
    visible: Boolean,
    strikerName: String,
    nonStrikerName: String,
    dismissedBatsmen: List<String>,
    battingSquadList: List<String>,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = {}, // Force selection
            confirmButton = {},
            title = {
                Text(
                    text = "Select Next Batsman",
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose incoming batsman to replace dismissed player:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    val selectableBatsmen = battingSquadList.filter { name ->
                        name != strikerName && 
                        name != nonStrikerName && 
                        !dismissedBatsmen.contains(name)
                    }
                    if (selectableBatsmen.isEmpty()) {
                        Text("No batsmen remaining in the squad roster.", color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss")
                        }
                    } else {
                        selectableBatsmen.forEach { batsmanName ->
                            Button(
                                onClick = { onConfirm(batsmanName) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(text = batsmanName)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WicketRecordingDialog(
    visible: Boolean,
    strikerName: String,
    nonStrikerName: String,
    bowlingSquadList: List<String>,
    onDismiss: () -> Unit,
    onRecordWicket: (type: String, dismissedName: String, fielderName: String?, completedRuns: Int, extraType: String?) -> Unit
) {
    if (visible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        // Navigation states: "Main", "CaughtBy", "StumpedBy", "RunOut", "RunOutFielder1", "RunOutFielder2"
        var currentView by remember { mutableStateOf("Main") }
        var selectedDismissedName by remember { mutableStateOf(strikerName) }
        var selectedFielder1 by remember { mutableStateOf<String?>(null) }
        var selectedFielder2 by remember { mutableStateOf<String?>(null) }
        
        // Run out configuration state
        var runOutRuns by remember { mutableStateOf(0) }
        var runOutExtraType by remember { mutableStateOf<String?>(null) }

        // Reset state when visible changes
        LaunchedEffect(visible) {
            if (visible) {
                currentView = "Main"
                selectedDismissedName = strikerName
                selectedFielder1 = null
                selectedFielder2 = null
                runOutRuns = 0
                runOutExtraType = null
            }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                when (currentView) {
                    "Main" -> {
                        DismissalPickerView(
                            strikerName = strikerName,
                            nonStrikerName = nonStrikerName,
                            selectedDismissedName = selectedDismissedName,
                            onDismissedNameChange = { selectedDismissedName = it },
                            onSelectType = { type ->
                                when (type) {
                                    "Caught" -> currentView = "CaughtBy"
                                    "Stumped" -> currentView = "StumpedBy"
                                    "Run Out" -> currentView = "RunOut"
                                    else -> {
                                        onRecordWicket(type, selectedDismissedName, null, 0, null)
                                    }
                                }
                            },
                            onDismiss = onDismiss
                        )
                    }
                    "CaughtBy" -> {
                        FielderSelectorView(
                            title = "Caught by 🤲",
                            bowlingSquadList = bowlingSquadList,
                            onBack = { currentView = "Main" },
                            onSelectFielder = { fielder ->
                                onRecordWicket("Caught", selectedDismissedName, fielder, 0, null)
                            }
                        )
                    }
                    "StumpedBy" -> {
                        FielderSelectorView(
                            title = "Stumped by ⚡",
                            bowlingSquadList = bowlingSquadList,
                            onBack = { currentView = "Main" },
                            onSelectFielder = { fielder ->
                                onRecordWicket("Stumped", selectedDismissedName, fielder, 0, null)
                            }
                        )
                    }
                    "RunOut" -> {
                        RunOutConfigView(
                            strikerName = strikerName,
                            nonStrikerName = nonStrikerName,
                            selectedDismissedName = selectedDismissedName,
                            onDismissedNameChange = { selectedDismissedName = it },
                            selectedFielder1 = selectedFielder1,
                            selectedFielder2 = selectedFielder2,
                            onSelectFielder1Click = { currentView = "RunOutFielder1" },
                            onSelectFielder2Click = { currentView = "RunOutFielder2" },
                            completedRuns = runOutRuns,
                            onRunsChange = { runOutRuns = it },
                            extraType = runOutExtraType,
                            onExtraTypeChange = { runOutExtraType = it },
                            onBack = { currentView = "Main" },
                            onConfirm = {
                                val combinedFielders = if (selectedFielder2 != null) {
                                    "${selectedFielder1}/${selectedFielder2}"
                                } else {
                                    selectedFielder1
                                }
                                onRecordWicket("Run Out", selectedDismissedName, combinedFielders, runOutRuns, runOutExtraType)
                            }
                        )
                    }
                    "RunOutFielder1" -> {
                        FielderSelectorView(
                            title = "Select Fielder 01 🏃",
                            bowlingSquadList = bowlingSquadList,
                            onBack = { currentView = "RunOut" },
                            onSelectFielder = { fielder ->
                                selectedFielder1 = fielder
                                currentView = "RunOut"
                            }
                        )
                    }
                    "RunOutFielder2" -> {
                        FielderSelectorView(
                            title = "Select Fielder 02 (Optional) 🏃",
                            bowlingSquadList = bowlingSquadList,
                            onBack = { currentView = "RunOut" },
                            onSelectFielder = { fielder ->
                                selectedFielder2 = fielder
                                currentView = "RunOut"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DismissalPickerView(
    strikerName: String,
    nonStrikerName: String,
    selectedDismissedName: String,
    onDismissedNameChange: (String) -> Unit,
    onSelectType: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Record Wicket 🏏",
                fontSize = screenConfig.headingTextSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Text("✕", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Select Dismissed Player
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Select Dismissed Batsman:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(strikerName, nonStrikerName).forEach { name ->
                    val isSel = name == selectedDismissedName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onDismissedNameChange(name) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Wicket Type Picker
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Select Dismissal Type:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            val rows = listOf(
                listOf("Bowled", "Caught", "LBW"),
                listOf("Run Out", "Stumped", "Hit Wicket"),
                listOf("Retired", "Mankad", "Over The Fence"),
                listOf("One Hand One Bounce")
            )

            rows.forEach { rowTypes ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    rowTypes.forEach { type ->
                        Box(
                            modifier = Modifier
                                .weight(if (rowTypes.size == 1) 3f else 1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .clickable { onSelectType(type) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (rowTypes.size == 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun FielderSelectorView(
    title: String,
    bowlingSquadList: List<String>,
    onBack: () -> Unit,
    onSelectFielder: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(max = 380.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = screenConfig.headingTextSize)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Text(
            text = "Select player from roster:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Grid list of fielders
        val chunks = bowlingSquadList.chunked(2)
        chunks.forEach { chunk ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                chunk.forEach { fielder ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .clickable { onSelectFielder(fielder) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = fielder,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (chunk.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun RunOutConfigView(
    strikerName: String,
    nonStrikerName: String,
    selectedDismissedName: String,
    onDismissedNameChange: (String) -> Unit,
    selectedFielder1: String?,
    selectedFielder2: String?,
    onSelectFielder1Click: () -> Unit,
    onSelectFielder2Click: () -> Unit,
    completedRuns: Int,
    onRunsChange: (Int) -> Unit,
    extraType: String?,
    onExtraTypeChange: (String?) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("←", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Run Out Details 🏃‍♂️",
                    fontSize = screenConfig.headingTextSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 1. Who was run out?
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Select batsman out *",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(strikerName, nonStrikerName).forEach { name ->
                    val isSel = name == selectedDismissedName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onDismissedNameChange(name) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Fielder Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Select fielder (Fielder 01 is mandatory)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Fielder 1 Selector Button
                val fielder1Label = selectedFielder1 ?: "Select Fielder 01*"
                val fielder1Bg = if (selectedFielder1 != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                val fielder1Border = if (selectedFielder1 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fielder1Bg)
                        .border(1.dp, fielder1Border, RoundedCornerShape(8.dp))
                        .clickable { onSelectFielder1Click() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fielder1Label, color = if (selectedFielder1 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Fielder 2 Selector Button
                val fielder2Label = selectedFielder2 ?: "Fielder 02 (Optional)"
                val fielder2Bg = if (selectedFielder2 != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                val fielder2Border = if (selectedFielder2 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fielder2Bg)
                        .border(1.dp, fielder2Border, RoundedCornerShape(8.dp))
                        .clickable { onSelectFielder2Click() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fielder2Label, color = if (selectedFielder2 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Runs completed
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Did batsman complete any runs? (optional)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(0, 1, 2, 3).forEach { runs ->
                    val isSel = completedRuns == runs
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .clickable { onRunsChange(runs) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$runs",
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Byes or leg byes
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Is that a byes or leg bye? (optional)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Byes", "Leg Byes").forEach { type ->
                    val isSel = extraType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .clickable {
                                onExtraTypeChange(if (isSel) null else type)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Noball or wide
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Is that a noball or wide? (optional)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("No Ball", "Wide").forEach { type ->
                    val isSel = extraType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .clickable {
                                onExtraTypeChange(if (isSel) null else type)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Submit Button
        Button(
            onClick = onConfirm,
            enabled = selectedFielder1 != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("DONE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
