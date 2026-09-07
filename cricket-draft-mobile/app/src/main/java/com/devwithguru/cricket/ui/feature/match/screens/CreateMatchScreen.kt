package com.devwithguru.cricket.ui.feature.match.screens

import androidx.compose.ui.res.stringResource
import com.devwithguru.cricket.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import com.devwithguru.cricket.ui.feature.match.viewmodels.CreateMatchViewModel
import com.devwithguru.cricket.domain.model.ScheduledFixture
import com.devwithguru.cricket.ui.theme.screenConfig


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMatchScreen(
    tournamentId: String? = null,
    onCreateMatchSuccess: (matchId: String, homeTeam: String, awayTeam: String, overs: Int, ballType: String, date: String, time: String) -> Unit,
    onNavigateBack: () -> Unit,
    defaultWickets: Int = 10,
    viewModel: CreateMatchViewModel = hiltViewModel()
) {
    var homeTeam by remember { mutableStateOf("") }
    var awayTeam by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var matchDate by remember { mutableStateOf("19 Aug 2026") }
    var matchTime by remember { mutableStateOf("16:37") }

    var selectedOvers by remember { mutableStateOf("6") }
    var selectedMatchType by remember { mutableStateOf("T20") }
    var selectedBallType by remember { mutableStateOf("Tennis") }
    var selectedWickets by remember { mutableStateOf(defaultWickets.toString()) }

    var oversDropdownExpanded by remember { mutableStateOf(false) }
    var matchTypeDropdownExpanded by remember { mutableStateOf(false) }
    var ballTypeDropdownExpanded by remember { mutableStateOf(false) }
    var wicketsDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // States to control Select Team Screen & Create New Team Dialog popup
    var activeSelectingTeamSide by remember { mutableStateOf<String?>(null) } // "A", "B", or null
    var isCreateTeamDialogOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val errSelectHomeTeam = stringResource(R.string.err_select_home_team)
    val errSelectAwayTeam = stringResource(R.string.err_select_away_team)
    val errTeamsSame = stringResource(R.string.err_teams_same)
    val errEnterVenue = stringResource(R.string.err_enter_venue)
    val errSelectDate = stringResource(R.string.err_select_date)
    val errSelectTime = stringResource(R.string.err_select_time)
    val errInvalidOvers = stringResource(R.string.err_invalid_overs)
    val errSelectMatchType = stringResource(R.string.err_select_match_type)
    val errSelectBallType = stringResource(R.string.err_select_ball_type)
    val errInvalidWickets = stringResource(R.string.err_invalid_wickets)
    val msgFixtureSaved = stringResource(R.string.msg_fixture_saved)

    val monthNames = listOf(
        stringResource(R.string.month_jan),
        stringResource(R.string.month_feb),
        stringResource(R.string.month_mar),
        stringResource(R.string.month_apr),
        stringResource(R.string.month_may),
        stringResource(R.string.month_jun),
        stringResource(R.string.month_jul),
        stringResource(R.string.month_aug),
        stringResource(R.string.month_sep),
        stringResource(R.string.month_oct),
        stringResource(R.string.month_nov),
        stringResource(R.string.month_dec)
    )

    val existingTeams = remember {
        mutableStateListOf("BHH", "NHH", "Ali Panthers", "Rawalpindi Kings", "Islamabad Blasters")
    }

    // Android Native DatePickerDialog configuration
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                matchDate = "$dayOfMonth ${monthNames[month]} $year"
                errorMessage = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Android Native TimePickerDialog configuration
    val timePickerDialog = remember {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                matchTime = String.format("%02d:%02d", hourOfDay, minute)
                errorMessage = null
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24 Hours format view
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.create_match_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_desc),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Ambient glow radial background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = 1000f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // --- Team Selector Circles ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team A Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.select_team_label),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                .border(
                                    2.dp,
                                    if (homeTeam.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                    CircleShape
                                  )
                                .clickable { activeSelectingTeamSide = "A" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (homeTeam.isNotBlank()) {
                                Text(
                                    text = homeTeam.take(2).uppercase(),
                                    fontSize = screenConfig.scoreTextSize,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_desc),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (homeTeam.isNotBlank()) homeTeam else stringResource(R.string.team_a_placeholder),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Team B Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.select_team_label),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                .border(
                                    2.dp,
                                    if (awayTeam.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .clickable { activeSelectingTeamSide = "B" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (awayTeam.isNotBlank()) {
                                Text(
                                    text = awayTeam.take(2).uppercase(),
                                    fontSize = screenConfig.scoreTextSize,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_desc),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (awayTeam.isNotBlank()) awayTeam else stringResource(R.string.team_b_placeholder),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Venue ---
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(stringResource(R.string.venue_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                    UnderlineTextField(
                        value = venue,
                        onValueChange = {
                            venue = it
                            errorMessage = null
                        },
                        placeholder = stringResource(R.string.venue_hint)
                    )
                }

                // --- Date & Time ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.date_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                        UnderlineTextField(
                            value = matchDate,
                            onValueChange = {},
                            placeholder = stringResource(R.string.date_hint),
                            onClick = { datePickerDialog.show() }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.time_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                        UnderlineTextField(
                            value = matchTime,
                            onValueChange = {},
                            placeholder = stringResource(R.string.time_hint),
                            onClick = { timePickerDialog.show() }
                        )
                    }
                }

                // --- Overs & Match Type ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Overs (Editable number + dropdown suggestions)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.overs_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                        Box {
                            UnderlineTextField(
                                value = selectedOvers,
                                onValueChange = { newVal ->
                                    selectedOvers = newVal.filter { it.isDigit() }
                                    errorMessage = null
                                },
                                placeholder = stringResource(R.string.overs_hint),
                                showDropdownArrow = true,
                                onArrowClick = { oversDropdownExpanded = true },
                                keyboardType = KeyboardType.Number
                            )
                            DropdownMenu(
                                expanded = oversDropdownExpanded,
                                onDismissRequest = { oversDropdownExpanded = false }
                            ) {
                                listOf(
                                    "5" to stringResource(R.string.overs_5),
                                    "6" to stringResource(R.string.overs_6),
                                    "10" to stringResource(R.string.overs_10),
                                    "20" to stringResource(R.string.overs_20),
                                    "50" to stringResource(R.string.overs_50)
                                ).forEach { (num, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedOvers = num
                                            oversDropdownExpanded = false
                                            errorMessage = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Match Type (Dropdown only)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.match_type_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                        Box {
                            UnderlineTextField(
                                value = selectedMatchType,
                                onValueChange = {},
                                placeholder = stringResource(R.string.match_type_hint),
                                onClick = { matchTypeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = matchTypeDropdownExpanded,
                                onDismissRequest = { matchTypeDropdownExpanded = false }
                            ) {
                                listOf(
                                    stringResource(R.string.match_type_t10),
                                    stringResource(R.string.match_type_t20),
                                    stringResource(R.string.match_type_one_day),
                                    stringResource(R.string.match_type_test)
                                ).forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedMatchType = option
                                            matchTypeDropdownExpanded = false
                                            errorMessage = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Ball Type & Wickets ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ball Type (Dropdown only)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ball_type_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                        Box {
                            UnderlineTextField(
                                value = selectedBallType,
                                onValueChange = {},
                                placeholder = stringResource(R.string.ball_type_hint),
                                onClick = { ballTypeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = ballTypeDropdownExpanded,
                                onDismissRequest = { ballTypeDropdownExpanded = false }
                            ) {
                                listOf(
                                    stringResource(R.string.ball_type_tennis),
                                    stringResource(R.string.ball_type_leather)
                                ).forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedBallType = option
                                            ballTypeDropdownExpanded = false
                                            errorMessage = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Wickets (Editable number + dropdown suggestions)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.wickets_label), fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
                        Box {
                            UnderlineTextField(
                                value = selectedWickets,
                                onValueChange = { newVal ->
                                    selectedWickets = newVal.filter { it.isDigit() }
                                    errorMessage = null
                                },
                                placeholder = stringResource(R.string.wickets_hint),
                                showDropdownArrow = true,
                                onArrowClick = { wicketsDropdownExpanded = true },
                                keyboardType = KeyboardType.Number
                            )
                            DropdownMenu(
                                expanded = wicketsDropdownExpanded,
                                onDismissRequest = { wicketsDropdownExpanded = false }
                            ) {
                                listOf(
                                    "3" to "3 Wickets",
                                    "5" to stringResource(R.string.wickets_5_label),
                                    "6" to "6 Wickets",
                                    "8" to stringResource(R.string.wickets_8_label),
                                    "10" to stringResource(R.string.wickets_10_label)
                                ).forEach { (num, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedWickets = num
                                            wicketsDropdownExpanded = false
                                            errorMessage = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Action Buttons: SAVE FIXTURE & START MATCH ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SAVE FIXTURE Button
                    Button(
                        onClick = {
                            when {
                                homeTeam.isBlank() -> errorMessage = errSelectHomeTeam
                                awayTeam.isBlank() -> errorMessage = errSelectAwayTeam
                                homeTeam.equals(awayTeam, ignoreCase = true) -> errorMessage = errTeamsSame
                                venue.isBlank() -> errorMessage = errEnterVenue
                                matchDate.isBlank() -> errorMessage = errSelectDate
                                matchTime.isBlank() -> errorMessage = errSelectTime
                                selectedOvers.isBlank() || selectedOvers.toIntOrNull() == null || selectedOvers.toInt() <= 0 -> errorMessage = errInvalidOvers
                                selectedMatchType.isBlank() -> errorMessage = errSelectMatchType
                                selectedBallType.isBlank() -> errorMessage = errSelectBallType
                                selectedWickets.isBlank() || selectedWickets.toIntOrNull() == null || selectedWickets.toInt() <= 0 -> errorMessage = errInvalidWickets
                                else -> {
                                    val matchId = "m_${System.currentTimeMillis()}"
                                    viewModel.saveFixture(
                                        ScheduledFixture(
                                            id = matchId,
                                            homeTeam = homeTeam,
                                            awayTeam = awayTeam,
                                            overs = selectedOvers.toIntOrNull() ?: 6,
                                            ballType = selectedBallType,
                                            matchType = selectedMatchType,
                                            wickets = selectedWickets.toIntOrNull() ?: 10,
                                            venue = venue,
                                            date = matchDate,
                                            time = matchTime
                                        )
                                    )
                                    Toast.makeText(context, msgFixtureSaved, Toast.LENGTH_LONG).show()
                                    onNavigateBack()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.save_fixture_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // START MATCH Button
                    Button(
                        onClick = {
                            when {
                                homeTeam.isBlank() -> errorMessage = errSelectHomeTeam
                                awayTeam.isBlank() -> errorMessage = errSelectAwayTeam
                                homeTeam.equals(awayTeam, ignoreCase = true) -> errorMessage = errTeamsSame
                                venue.isBlank() -> errorMessage = errEnterVenue
                                matchDate.isBlank() -> errorMessage = errSelectDate
                                matchTime.isBlank() -> errorMessage = errSelectTime
                                selectedOvers.isBlank() || selectedOvers.toIntOrNull() == null || selectedOvers.toInt() <= 0 -> errorMessage = errInvalidOvers
                                selectedMatchType.isBlank() -> errorMessage = errSelectMatchType
                                selectedBallType.isBlank() -> errorMessage = errSelectBallType
                                selectedWickets.isBlank() || selectedWickets.toIntOrNull() == null || selectedWickets.toInt() <= 0 -> errorMessage = errInvalidWickets
                                else -> {
                                    val parsedOvers = selectedOvers.toIntOrNull() ?: 6
                                    val matchId = "m_${System.currentTimeMillis()}"
                                    // Add it as live directly via ViewModel
                                    viewModel.saveFixture(
                                        ScheduledFixture(
                                            id = matchId,
                                            homeTeam = homeTeam,
                                            awayTeam = awayTeam,
                                            overs = parsedOvers,
                                            ballType = selectedBallType,
                                            matchType = selectedMatchType,
                                            wickets = selectedWickets.toIntOrNull() ?: 10,
                                            venue = venue,
                                            date = matchDate,
                                            time = matchTime,
                                            status = "Live"
                                        )
                                    )
                                    scope.launch {
                                        onCreateMatchSuccess(
                                            matchId,
                                            homeTeam,
                                            awayTeam,
                                            parsedOvers,
                                            selectedBallType,
                                            matchDate,
                                            matchTime
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.start_match_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // ==========================================
            // SCREEN OVERLAY: Select Team Panel (Pic 1)
            // ==========================================
            if (activeSelectingTeamSide != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Teal Top Bar matching Select Team picture header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { activeSelectingTeamSide = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close_desc),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                text = stringResource(R.string.select_team_label),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(48.dp)) // aligns title in center
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Option 1: Search by ID
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.option_1_label),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                var searchId by remember { mutableStateOf("") }
                                UnderlineTextField(
                                    value = searchId,
                                    onValueChange = { searchId = it },
                                    placeholder = stringResource(R.string.search_by_team_id_hint)
                                )
                            }

                            // Option 2: Create Team Button
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.option_2_label),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { isCreateTeamDialogOpen = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text(stringResource(R.string.create_new_team_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Option 3: Select from Existing List BHH, NHH
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.option_3_label),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.select_existing_team_hint),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )

                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    existingTeams.forEach { teamName ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (activeSelectingTeamSide == "A") {
                                                        homeTeam = teamName
                                                    } else {
                                                        awayTeam = teamName
                                                    }
                                                    activeSelectingTeamSide = null
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Circular avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                                                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Shield,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = teamName,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // POPUP MODAL DIALOG: Create Team Card (Pic 2)
            // ==========================================
            if (isCreateTeamDialogOpen) {
                Dialog(onDismissRequest = { isCreateTeamDialogOpen = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top left close cross button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                IconButton(
                                    onClick = { isCreateTeamDialogOpen = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = stringResource(R.string.close_desc),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            var newTeamName by remember { mutableStateOf("") }
                            var newTeamLocation by remember { mutableStateOf("") }

                            // Underline fields matching our dark theme style
                            UnderlineTextField(
                                value = newTeamName,
                                onValueChange = { newTeamName = it },
                                placeholder = stringResource(R.string.team_name_hint)
                            )

                            UnderlineTextField(
                                value = newTeamLocation,
                                onValueChange = { newTeamLocation = it },
                                placeholder = stringResource(R.string.location_hint)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action button matching app primary color
                            Button(
                                onClick = {
                                    if (newTeamName.isNotBlank()) {
                                        existingTeams.add(0, newTeamName)
                                        if (activeSelectingTeamSide == "A") {
                                            homeTeam = newTeamName
                                        } else {
                                            awayTeam = newTeamName
                                        }
                                        isCreateTeamDialogOpen = false
                                        activeSelectingTeamSide = null
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.create_team_btn_text),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🔴 Highly Refined Reusable Underline TextField without native padding clipping
@Composable
fun UnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    showDropdownArrow: Boolean = false,
    onArrowClick: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val lineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled && onClick == null,
                    readOnly = readOnly,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }
                    }
                )
            }

            if (showDropdownArrow) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.show_suggestions_desc),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onArrowClick?.invoke() }
                )
            }
        }
    }
}
