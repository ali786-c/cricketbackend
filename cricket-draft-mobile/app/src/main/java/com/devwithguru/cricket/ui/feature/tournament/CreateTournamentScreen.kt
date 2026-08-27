package com.devwithguru.cricket.ui.feature.tournament

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentScreen(
    onCreateTournamentSuccess: (tournamentId: String, name: String, hasDraft: Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: TournamentViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var organizerName by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var season by remember { mutableStateOf("2026") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var ballType by remember { mutableStateOf("Tennis Ball") }
    var competitionStructure by remember { mutableStateOf("League") }
    var visibility by remember { mutableStateOf("public") }
    var hasDraft by remember { mutableStateOf(false) }
    var squadSize by remember { mutableStateOf("11") }
    var pickDuration by remember { mutableStateOf("60") }
    var oversPerInnings by remember { mutableStateOf("20") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val createdId by viewModel.createdTournamentId.collectAsState()
    val createError by viewModel.createError.collectAsState()

    // Navigate when tournament is created on server
    LaunchedEffect(createdId) {
        createdId?.let {
            isLoading = false
            val localTournament = com.devwithguru.cricket.domain.model.Tournament(
                id = it, name = name, description = description,
                organizerName = organizerName, contactInfo = contactInfo,
                city = city, venue = venue, season = season,
                startDate = startDate, endDate = endDate,
                ballType = ballType, oversPerInnings = oversPerInnings.toIntOrNull() ?: 20,
                competitionStructure = competitionStructure, visibility = visibility,
                hasDraft = hasDraft, squadSize = squadSize.toIntOrNull() ?: 11,
                pickDuration = pickDuration.toIntOrNull() ?: 60, status = "upcoming"
            )
            viewModel.saveTournament(localTournament)
            onCreateTournamentSuccess(it, name, hasDraft)
            viewModel.clearError()
        }
    }
    // Show error from API — fallback to local save
    LaunchedEffect(createError) {
        createError?.let {
            val localId = viewModel.generateUniqueLocalId()
            val localTournament = com.devwithguru.cricket.domain.model.Tournament(
                id = localId, name = name, description = description,
                organizerName = organizerName, contactInfo = contactInfo,
                city = city, venue = venue, season = season,
                startDate = startDate, endDate = endDate,
                ballType = ballType, oversPerInnings = oversPerInnings.toIntOrNull() ?: 20,
                competitionStructure = competitionStructure, visibility = visibility,
                hasDraft = hasDraft, squadSize = squadSize.toIntOrNull() ?: 11,
                pickDuration = pickDuration.toIntOrNull() ?: 60, status = "upcoming"
            )
            viewModel.saveTournament(localTournament)
            isLoading = false
            onCreateTournamentSuccess(localId, name, hasDraft)
            viewModel.clearError()
        }
    }
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create Tournament", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Description
            Text(
                text = "Set up your tournament with schedule, format, and optional draft.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // Error
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), textAlign = TextAlign.Center)
            }

            // ── Basic Information ──
            SectionCard(title = "Basic Information") {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; errorMessage = null },
                    placeholder = { Text("e.g., Premier Cricket Cup", fontSize = 13.sp) },
                    label = { Text("Tournament Name", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    colors = defaultFieldColors(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
                OutlinedTextField(
                    value = city, onValueChange = { city = it; errorMessage = null },
                    placeholder = { Text("e.g., Lahore", fontSize = 13.sp) },
                    label = { Text("City", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.LocationCity, null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    colors = defaultFieldColors(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
                OutlinedTextField(
                    value = venue, onValueChange = { venue = it },
                    placeholder = { Text("e.g., Gaddafi Stadium", fontSize = 13.sp) },
                    label = { Text("Venue (optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    colors = defaultFieldColors(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
                OutlinedTextField(
                    value = season, onValueChange = { season = it },
                    placeholder = { Text("e.g., 2026", fontSize = 13.sp) },
                    label = { Text("Season", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                    colors = defaultFieldColors(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Schedule (Date Picker Fix) ──
            SectionCard(title = "Schedule") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Start Date — Box wrapper with clickable overlay
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = if (startDate.isNotEmpty()) startDate else "",
                            onValueChange = {},
                            placeholder = { Text("Select date", fontSize = 12.sp) },
                            label = { Text("Start Date", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp)) },
                            readOnly = true,
                            enabled = false,
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
                        // Transparent clickable overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker { startDate = it } }
                        )
                    }

                    // End Date — Box wrapper with clickable overlay
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = if (endDate.isNotEmpty()) endDate else "",
                            onValueChange = {},
                            placeholder = { Text("Select date", fontSize = 12.sp) },
                            label = { Text("End Date", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp)) },
                            readOnly = true,
                            enabled = false,
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
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker { endDate = it } }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ── Game Format ──
            SectionCard(title = "Game Format") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oversPerInnings, onValueChange = { oversPerInnings = it.filter { c -> c.isDigit() } },
                        label = { Text("Overs per Innings", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                        colors = defaultFieldColors(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )
                    OutlinedTextField(
                        value = squadSize, onValueChange = { squadSize = it.filter { c -> c.isDigit() } },
                        label = { Text("Players per Team", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                        colors = defaultFieldColors(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Draft Settings ──
            SectionCard(title = "Draft Settings") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Draft", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Captains pick players in a snake draft", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Switch(checked = hasDraft, onCheckedChange = { hasDraft = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
                }

                if (hasDraft) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pickDuration, onValueChange = { pickDuration = it.filter { c -> c.isDigit() } },
                        label = { Text("Pick Duration (seconds)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), singleLine = true,
                        colors = defaultFieldColors(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Create Button ──
            Button(
                onClick = {
                    val pCount = squadSize.toIntOrNull() ?: 11
                    when {
                        name.isBlank() -> errorMessage = "Please enter tournament name"
                        city.isBlank() -> errorMessage = "Please enter city"
                        pCount < 2 -> errorMessage = "Players per Team must be at least 2"
                        else -> {
                            isLoading = true
                            errorMessage = null
                            viewModel.createTournamentViaApi(
                                name = name, description = description,
                                organizerName = organizerName, contactInfo = contactInfo,
                                city = city, venue = venue, season = season,
                                startDate = startDate, endDate = endDate,
                                ballType = ballType, competitionStructure = competitionStructure,
                                visibility = visibility,
                                hasDraft = hasDraft, squadSize = pCount,
                                pickDuration = pickDuration.toIntOrNull() ?: 60,
                                overs = oversPerInnings.toIntOrNull() ?: 20
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CREATE TOURNAMENT", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun defaultFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
    focusedContainerColor = MaterialTheme.colorScheme.background,
    unfocusedContainerColor = MaterialTheme.colorScheme.background
)
