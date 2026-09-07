package com.devwithguru.cricket.ui.feature.tournament

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devwithguru.cricket.data.api.AdminPlayerData
import com.devwithguru.cricket.data.api.AdminTeamData
import com.devwithguru.cricket.data.api.CreateFixtureRequest
import com.devwithguru.cricket.data.api.DraftPickData
import com.devwithguru.cricket.data.api.TournamentFixtureData2
import com.devwithguru.cricket.data.db.entity.AdminFixtureEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.devwithguru.cricket.ui.theme.screenConfig

private fun <T> MutableList<T>.swap(i: Int, j: Int) { val tmp = this[i]; this[i] = this[j]; this[j] = tmp }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentSetupScreen(
    tournamentId: String,
    onNavigateToDraft: (String) -> Unit,
    onNavigateToTournamentHub: (String) -> Unit = {},
    onNavigateBack: () -> Unit,
    viewModel: TournamentSetupViewModel = hiltViewModel()
) {
    // Dynamic tabs based on tournament status
    val tournamentStatus by viewModel.tournamentStatus.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val hasDraft = viewModel.hasDraft
    val baseTabs = mutableListOf("Status", "Teams", "Players")
    if (hasDraft) baseTabs.add("Draft") else baseTabs.add("Fixtures")

    // Clamp selectedTab to valid range
    val clampedTab = selectedTab.coerceIn(0, baseTabs.size - 1)
    val tabTitles = baseTabs

    var showAddTeamSheet by remember { mutableStateOf(false) }
    var showAddPlayerSheet by remember { mutableStateOf(false) }
    var assignCaptainTeam by remember { mutableStateOf<AdminTeamData?>(null) }
    var showCreateFixtureSheet by remember { mutableStateOf(false) }

    LaunchedEffect(tournamentId) { viewModel.loadTournament(tournamentId) }
    val teams by viewModel.teams.collectAsState()
    val players by viewModel.players.collectAsState()
    val fixtures by viewModel.dbFixtures.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val draftPicks by viewModel.draftPicks.collectAsState()
    val draftRounds by viewModel.draftRounds.collectAsState()
    val draftStatus by viewModel.draftStatus.collectAsState()

    // ── Modals ──
    if (showAddTeamSheet) {
        AddTeamBottomSheet(
            onDismiss = { showAddTeamSheet = false },
            onConfirm = { name, shortName ->
                viewModel.createTeam(tournamentId, name, shortName)
                showAddTeamSheet = false
            }
        )
    }

    if (showAddPlayerSheet) {
        AddPlayerBottomSheet(
            onDismiss = { showAddPlayerSheet = false },
            onConfirm = { name, role, city ->
                viewModel.addPlayerManually(tournamentId, name, role, city)
                showAddPlayerSheet = false
            }
        )
    }

    assignCaptainTeam?.let { team ->
        AssignCaptainBottomSheet(
            team = team,
            registeredPlayers = players.filter { it.status == "approved" },
            onDismiss = { assignCaptainTeam = null },
            onConfirm = { selectedPlayerId ->
                // Find the player and get their user_id
                val selectedPlayer = players.find { it.id == selectedPlayerId }
                val userId = selectedPlayer?.player_profile?.user?.id
                val playerName = selectedPlayer?.player_profile?.full_name ?: "Captain"
                if (userId != null) {
                    viewModel.assignCaptain(tournamentId, team.id.toString(), userId, playerName)
                } else {
                    // Manually added player — assign locally with player profile ID as fallback
                    viewModel.assignCaptain(tournamentId, team.id.toString(), selectedPlayerId, playerName)
                }
                assignCaptainTeam = null
            }
        )
    }

    if (showCreateFixtureSheet) {
        CreateFixtureBottomSheet(
            teams = teams,
            onDismiss = { showCreateFixtureSheet = false },
            onConfirm = { request ->
                viewModel.createFixture(tournamentId, request)
                showCreateFixtureSheet = false
            }
        )
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (hasDraft) "Draft Setup" else "Tournament Setup", fontSize = screenConfig.titleTextSize, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (clampedTab) {
                1 -> FloatingActionButton(
                    onClick = { showAddTeamSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, "Add Team") }
                2 -> FloatingActionButton(
                    onClick = { showAddPlayerSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, "Add Player") }
                3 -> {
                    if (!hasDraft) {
                        FloatingActionButton(
                            onClick = { showCreateFixtureSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) { Icon(Icons.Default.Add, "Add Fixture") }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            // Demo mode banner
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFF9800).copy(alpha = 0.1f)) {
                Text("📋 Demo Mode — data saves locally. Run Laravel server for full API.", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 10.sp, color = Color(0xFFFF9800))
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = clampedTab,
                containerColor = MaterialTheme.colorScheme.background,
                edgePadding = 8.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = clampedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (clampedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (clampedTab) {
                0 -> StatusTabContent(
                    status = tournamentStatus,
                    teamCount = teams.size,
                    playerCount = players.size,
                    approvedCount = players.count { it.status == "approved" },
                    allTeamsHaveCaptains = teams.all { it.active_captain != null },
                    fixtureCount = fixtures.size,
                    hasDraft = hasDraft,
                    onStatusChange = { viewModel.updateStatus(tournamentId, it) },
                    onStartDraft = { onNavigateToDraft(tournamentId) },
                    onGoLive = {
                        viewModel.updateStatus(tournamentId, "live")
                        onNavigateToTournamentHub(tournamentId)
                    }
                )
                1 -> TeamsTabContent(
                    teams = teams,
                    onAssignCaptain = { assignCaptainTeam = it },
                    onDeleteTeam = { viewModel.deleteTeam(it.id.toString()) }
                )
                2 -> PlayersTabContent(
                    players = players,
                    onApprove = { viewModel.approvePlayer(tournamentId, it.toString()) },
                    onReject = { viewModel.rejectPlayer(tournamentId, it.toString()) },
                    onRemove = { viewModel.removePlayer(it.toString()) },
                    onAddManually = { showAddPlayerSheet = true }
                )
                3 -> {
                    if (hasDraft) {
                        LaunchedEffect(Unit) { viewModel.loadDraftState(tournamentId) }
                        DraftSetupTab(
                            teams = teams,
                            picks = draftPicks,
                            rounds = draftRounds,
                            draftStatus = draftStatus,
                            onStartDraft = { onNavigateToDraft(tournamentId) },
                            onSaveDraft = { request -> viewModel.saveDraftSetup(tournamentId, request) }
                        )
                    } else {
                        FixturesTabContent(fixtures = fixtures)
                    }
                }
            }
        }
    }
}

// ─── Status Tab ────────────────────────────────────────────

@Composable
private fun StatusTabContent(
    status: String,
    teamCount: Int,
    playerCount: Int,
    approvedCount: Int,
    allTeamsHaveCaptains: Boolean,
    fixtureCount: Int,
    hasDraft: Boolean,
    onStatusChange: (String) -> Unit,
    onStartDraft: () -> Unit,
    onGoLive: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Current Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tournament Status", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(status.replaceFirstChar { it.uppercase() }, fontSize = screenConfig.headingTextSize, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Summary Stats
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Teams", "$teamCount", Modifier.weight(1f))
                StatCard("Players", "$playerCount", Modifier.weight(1f))
                StatCard("Approved", "$approvedCount", Modifier.weight(1f))
            }
        }

        // Readiness Checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Readiness Checklist", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ChecklistItem("At least 2 teams", teamCount >= 2)
                    if (hasDraft) {
                        ChecklistItem("Players registered", playerCount > 0)
                        ChecklistItem("Players approved", approvedCount > 0)
                        ChecklistItem("All teams have captains", allTeamsHaveCaptains)
                        ChecklistItem("Ready for draft", approvedCount >= teamCount && allTeamsHaveCaptains)
                    } else {
                        ChecklistItem("Fixtures created", fixtureCount > 0)
                    }
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (status == "draft" && teamCount >= 2) {
                    if (hasDraft) {
                        ActionButton("Open Registration", Color(0xFFFF9800)) { onStatusChange("registration") }
                    } else if (fixtureCount > 0) {
                        ActionButton("Mark Ready", Color(0xFF4CAF50)) { onStatusChange("ready") }
                    }
                }
                if (status == "registration") {
                    if (hasDraft && approvedCount >= teamCount && allTeamsHaveCaptains) {
                        ActionButton("Mark Ready", Color(0xFF4CAF50)) { onStatusChange("ready") }
                    } else if (!hasDraft && fixtureCount > 0) {
                        ActionButton("Mark Ready", Color(0xFF4CAF50)) { onStatusChange("ready") }
                    }
                }
                if (hasDraft && status == "ready" && approvedCount >= teamCount && allTeamsHaveCaptains) {
                    ActionButton("Start Draft →", MaterialTheme.colorScheme.primary) { onStartDraft() }
                }
                if (teamCount >= 2) {
                    OutlinedButton(
                        onClick = onGoLive,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Go Live →", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = screenConfig.scoreTextSize, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChecklistItem(label: String, completed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (completed) Icons.Default.Check else Icons.Default.Close,
            null,
            tint = if (completed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        Text(label, fontSize = 13.sp, color = if (completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

// ─── Teams Tab ─────────────────────────────────────────────

@Composable
private fun TeamsTabContent(
    teams: List<AdminTeamData>,
    onAssignCaptain: (AdminTeamData) -> Unit,
    onDeleteTeam: (AdminTeamData) -> Unit
) {
    if (teams.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Groups, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No teams yet", fontSize = screenConfig.titleTextSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap + to add teams", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(teams) { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(team.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Code: ${team.unique_code ?: "-"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { onDeleteTeam(team) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val captain = team.active_captain
                        if (captain != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Person, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                    Text("Captain: ${captain.user?.name ?: "Unknown"}", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                                }
                                TextButton(onClick = { onAssignCaptain(team) }) { Text("Change", fontSize = 11.sp) }
                            }
                        } else {
                            Button(
                                onClick = { onAssignCaptain(team) },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                            ) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assign Captain", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Players Tab ───────────────────────────────────────────

@Composable
private fun PlayersTabContent(
    players: List<AdminPlayerData>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onAddManually: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with count
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${players.size} Players (${players.count { it.status == "approved" }} approved)",
                fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onAddManually) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Manually", fontSize = 11.sp)
            }
        }

        if (players.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No players yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Register players or add manually", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onAddManually, shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Player", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(players) { player ->
                    val profile = player.player_profile
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Avatar
                            Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Text(
                                    (profile?.full_name ?: "U").first().toString(),
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))

                            // Name + Role (responsive — takes available space)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    profile?.full_name ?: "Unknown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Text(
                                    buildString {
                                        append(profile?.playing_role ?: "")
                                        if (!profile?.city.isNullOrBlank()) append(" • ${profile?.city}")
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Status actions
                            when (player.status) {
                                "pending" -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { onApprove(player.id.toString()) }, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF4CAF50).copy(alpha = 0.1f))) {
                                        Icon(Icons.Default.Check, "Approve", tint = Color(0xFF4CAF50), modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(onClick = { onReject(player.id.toString()) }, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
                                        Icon(Icons.Default.Close, "Reject", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                    }
                                }
                                "approved" -> {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                                        Text("Approved", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                                "rejected" -> {
                                    IconButton(onClick = { onRemove(player.id) }, modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
                                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Fixtures Tab ──────────────────────────────────────────

@Composable
private fun FixturesTabContent(fixtures: List<AdminFixtureEntity>) {
    if (fixtures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SportsCricket, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No fixtures yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap + to add fixtures", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(fixtures) { fixture ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${fixture.homeTeamName ?: "?"} vs ${fixture.awayTeamName ?: "?"}",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                                Text("R${fixture.roundNumber ?: 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(fixture.scheduledAt?.take(10) ?: "TBD", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (fixture.venue != null) Text(fixture.venue ?: "", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// ─── Add Team Bottom Sheet ─────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTeamBottomSheet(onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var teamName by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))) }
            Text("Add Team", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = teamName, onValueChange = { teamName = it }, placeholder = { Text("e.g., Ali Panthers") }, label = { Text("Team Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = shortName, onValueChange = { shortName = it }, placeholder = { Text("e.g., AP") }, label = { Text("Short Name (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), shape = RoundedCornerShape(10.dp))
            Button(onClick = { if (teamName.isNotBlank()) onConfirm(teamName.trim(), shortName.trim().ifBlank { null }) }, enabled = teamName.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Add Team", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

// ─── Add Player Bottom Sheet ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlayerBottomSheet(onDismiss: () -> Unit, onConfirm: (name: String, role: String, city: String) -> Unit) {
    var playerName by remember { mutableStateOf("") }
    var playerRole by remember { mutableStateOf("Batsman") }
    var playerCity by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val roles = listOf("Batsman", "Bowler", "All-rounder", "Wicketkeeper")

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))) }
            Text("Add Player Manually", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = playerName, onValueChange = { playerName = it }, placeholder = { Text("e.g., Ahmed Ali") }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), shape = RoundedCornerShape(10.dp))

            Text("Playing Role", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                roles.forEach { role ->
                    val isSelected = playerRole == role
                    Box(
                        modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { playerRole = role },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(role, fontSize = 10.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    }
                }
            }

            OutlinedTextField(value = playerCity, onValueChange = { playerCity = it }, placeholder = { Text("e.g., Lahore") }, label = { Text("City (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))

            Button(
                onClick = { if (playerName.isNotBlank()) onConfirm(playerName.trim(), playerRole, playerCity.trim()) },
                enabled = playerName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Add Player", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

// ─── Assign Captain Bottom Sheet ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignCaptainBottomSheet(team: AdminTeamData, registeredPlayers: List<AdminPlayerData>, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    // Track selection by player.id (registration ID), NOT user.id
    var selectedPlayerId by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))) }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Assign Captain", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(team.name ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${registeredPlayers.size} approved players available", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(12.dp))

            if (registeredPlayers.isEmpty()) {
                Text("No approved players. Approve players first.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(registeredPlayers) { player ->
                        val isSelected = selectedPlayerId == player.id
                        Card(modifier = Modifier.fillMaxWidth().clickable { selectedPlayerId = player.id },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Selection indicator
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).border(
                                    2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape
                                ), contentAlignment = Alignment.Center) {
                                    if (isSelected) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(player.player_profile?.full_name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (!player.player_profile?.playing_role.isNullOrBlank()) {
                                            Text(player.player_profile?.playing_role ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (!player.player_profile?.city.isNullOrBlank()) {
                                            Text("• ${player.player_profile?.city}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
                Button(onClick = { selectedPlayerId?.let { onConfirm(it) } }, enabled = selectedPlayerId != null, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp)) { Text("Assign", fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Create Fixture Bottom Sheet ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFixtureBottomSheet(teams: List<AdminTeamData>, onDismiss: () -> Unit, onConfirm: (CreateFixtureRequest) -> Unit) {
    var homeTeamId by remember { mutableIntStateOf(0) }
    var awayTeamId by remember { mutableIntStateOf(0) }
    var homeTeamName by remember { mutableStateOf("") }
    var awayTeamName by remember { mutableStateOf("") }
    var scheduledDate by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var roundNumber by remember { mutableStateOf("1") }
    var showHomeTeamPicker by remember { mutableStateOf(false) }
    var showAwayTeamPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            val cal = Calendar.getInstance(); cal.set(year, month, day)
            onDateSelected(dateFormat.format(cal.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))) }
            Text("Add Fixture", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            // Home team
            Text("Home Team *", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(10.dp)).clickable { showHomeTeamPicker = true }.padding(12.dp)) {
                Text(if (homeTeamName.isNotEmpty()) homeTeamName else "Select home team", fontSize = 13.sp, color = if (homeTeamName.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showHomeTeamPicker) {
                AlertDialog(onDismissRequest = { showHomeTeamPicker = false }, title = { Text("Home Team") },
                    text = { Column { teams.forEach { t -> Text(t.name ?: "Unknown", modifier = Modifier.fillMaxWidth().clickable { homeTeamId = t.id; homeTeamName = t.name ?: ""; showHomeTeamPicker = false }.padding(12.dp)) } } },
                    confirmButton = {})
            }

            // Away team
            Text("Away Team *", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(10.dp)).clickable { showAwayTeamPicker = true }.padding(12.dp)) {
                Text(if (awayTeamName.isNotEmpty()) awayTeamName else "Select away team", fontSize = 13.sp, color = if (awayTeamName.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showAwayTeamPicker) {
                AlertDialog(onDismissRequest = { showAwayTeamPicker = false }, title = { Text("Away Team") },
                    text = { Column { teams.filter { it.id != homeTeamId }.forEach { t -> Text(t.name ?: "Unknown", modifier = Modifier.fillMaxWidth().clickable { awayTeamId = t.id; awayTeamName = t.name ?: ""; showAwayTeamPicker = false }.padding(12.dp)) } } },
                    confirmButton = {})
            }

            // Date
            Text("Match Date *", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(10.dp)).clickable { showDatePicker { scheduledDate = it } }.padding(12.dp)) {
                Text(if (scheduledDate.isNotEmpty()) scheduledDate else "Select date", fontSize = 13.sp, color = if (scheduledDate.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedTextField(value = roundNumber, onValueChange = { roundNumber = it.filter { c -> c.isDigit() } }, label = { Text("Round") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
                Button(
                    onClick = { if (homeTeamId != 0 && awayTeamId != 0 && scheduledDate.isNotEmpty()) onConfirm(CreateFixtureRequest(home_team_id = homeTeamId, away_team_id = awayTeamId, scheduled_at = scheduledDate, round_number = roundNumber.toIntOrNull(), venue = venue.ifBlank { null }, timezone = "Asia/Karachi")) },
                    enabled = homeTeamId != 0 && awayTeamId != 0 && scheduledDate.isNotEmpty() && homeTeamId != awayTeamId,
                    modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(10.dp)
                ) { Text("Create", fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
