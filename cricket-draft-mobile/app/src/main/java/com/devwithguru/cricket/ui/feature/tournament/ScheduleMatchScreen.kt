package com.devwithguru.cricket.ui.feature.tournament

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.api.TournamentTeamData
import com.devwithguru.cricket.domain.model.Stage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Schedule Match Screen — PRD §21
 * Manual match scheduling with all options per PRD:
 * Team A, Team B, Date, Time, Venue, Officials, Match Type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleMatchScreen(
    tournamentId: String,
    stages: List<Stage> = emptyList(),
    teams: List<TournamentTeamData> = emptyList(),
    onMatchScheduled: (
        stageId: String?,
        homeTeamId: String,
        homeTeamName: String,
        awayTeamId: String,
        awayTeamName: String,
        date: String,
        time: String,
        venue: String,
        matchType: String
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    // ── State ──
    var selectedStageId by remember { mutableStateOf<String?>(null) }
    var selectedStageName by remember { mutableStateOf("") }
    var homeTeamIndex by remember { mutableIntStateOf(-1) }
    var awayTeamIndex by remember { mutableIntStateOf(-1) }
    var scheduledDate by remember { mutableStateOf("") }
    var scheduledTime by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf("normal") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTeamPicker by remember { mutableStateOf(false) }
    var pickingFor by remember { mutableStateOf("") } // "home" or "away"

    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                onDateSelected(dateFormat.format(cal.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onTimeSelected(String.format("%02d:%02d", hourOfDay, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    // ── Team Picker Dialog ──
    if (showTeamPicker) {
        AlertDialog(
            onDismissRequest = { showTeamPicker = false },
            title = { Text("Select ${if (pickingFor == "home") "Home" else "Away"} Team", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    teams.forEachIndexed { index, team ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (pickingFor == "home") homeTeamIndex = index else awayTeamIndex = index
                                    showTeamPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (team.short_name ?: team.name?.take(3) ?: "?").take(3).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(team.name ?: "Unknown", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTeamPicker = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Schedule Match", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground) } },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // ── Stage Selection (optional) ──
            if (stages.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Assign to Stage (Optional)", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            stages.take(4).forEach { stage ->
                                val isSelected = selectedStageId == stage.id
                                Box(
                                    modifier = Modifier.weight(1f).height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .clickable { selectedStageId = stage.id; selectedStageName = stage.name },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stage.name, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // ── Teams Selection (PRD §21) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Teams", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    // Home Team
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .clickable { pickingFor = "home"; showTeamPicker = true }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Home Team *", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text(
                                    if (homeTeamIndex >= 0) teams[homeTeamIndex].name ?: "Unknown" else "Tap to select",
                                    color = if (homeTeamIndex >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.SportsCricket, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Away Team
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .clickable { pickingFor = "away"; showTeamPicker = true }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Away Team *", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                Text(
                                    if (awayTeamIndex >= 0) teams[awayTeamIndex].name ?: "Unknown" else "Tap to select",
                                    color = if (awayTeamIndex >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.SportsCricket, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── Schedule (PRD §21) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Schedule", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Date
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = scheduledDate,
                                onValueChange = {},
                                placeholder = { Text("Select date", fontSize = 12.sp) },
                                label = { Text("Date *", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp)) },
                                readOnly = true, enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker { scheduledDate = it } })
                        }
                        // Time
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = scheduledTime,
                                onValueChange = {},
                                placeholder = { Text("Select time", fontSize = 12.sp) },
                                label = { Text("Time *", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp)) },
                                readOnly = true, enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showTimePicker { scheduledTime = it } })
                        }
                    }
                }
            }

            // ── Match Type ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Match Type", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    val matchTypes = listOf(
                        "normal" to "Normal Match",
                        "series" to "Series",
                        "knockout" to "Knockout Match",
                        "qualifier" to "Qualifier",
                        "eliminator" to "Eliminator",
                        "quarter_final" to "Quarter Final",
                        "semi_final" to "Semi Final",
                        "final" to "Final"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchTypes.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (value, label) ->
                                    val isSelected = matchType == value
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                            .clickable { matchType = value },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                if (rowItems.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Schedule Button ──
            Button(
                onClick = {
                    when {
                        homeTeamIndex < 0 -> errorMessage = "Please select a home team"
                        awayTeamIndex < 0 -> errorMessage = "Please select an away team"
                        homeTeamIndex == awayTeamIndex -> errorMessage = "Home and away teams must be different"
                        scheduledDate.isBlank() -> errorMessage = "Please select a date"
                        scheduledTime.isBlank() -> errorMessage = "Please select a time"
                        else -> {
                            val home = teams[homeTeamIndex]
                            val away = teams[awayTeamIndex]
                            onMatchScheduled(
                                selectedStageId,
                                home.id.toString(), home.name ?: "Unknown",
                                away.id.toString(), away.name ?: "Unknown",
                                scheduledDate, scheduledTime,
                                venue.ifBlank { "TBD" },
                                matchType
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = teams.size >= 2
            ) {
                Icon(Icons.Default.SportsCricket, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SCHEDULE MATCH", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
