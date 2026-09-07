package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devwithguru.cricket.data.api.DraftAvailablePlayer
import com.devwithguru.cricket.data.api.DraftStateData
import com.devwithguru.cricket.data.api.DraftTeamSquad
import com.devwithguru.cricket.ui.theme.screenConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftRoomScreen(
    tournamentId: String,
    isAdmin: Boolean = true,
    onNavigateBack: () -> Unit,
    onDraftCompleted: (String) -> Unit,
    viewModel: DraftViewModel = hiltViewModel()
) {
    LaunchedEffect(tournamentId) { viewModel.loadDraft(tournamentId, isAdmin) }
    val draftState by viewModel.draftState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    var showPlayerPicker by remember { mutableStateOf(false) }

    // Navigate when draft completes
    LaunchedEffect(draftState?.status) {
        if (draftState?.status == "completed") onDraftCompleted(tournamentId)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(successMessage) { successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() } }
    LaunchedEffect(error) { error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() } }

    // Player picker dialog
    if (showPlayerPicker && draftState != null) {
        val activePick = draftState!!.picks.firstOrNull { it.status == "active" }
        val available = draftState!!.available_players

        AlertDialog(
            onDismissRequest = { showPlayerPicker = false },
            title = { Text("Select Player", fontWeight = FontWeight.Bold) },
            text = {
                if (available.isEmpty()) {
                    Text("No available players.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(available) { player ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (isAdmin && activePick != null) {
                                        viewModel.adminSelectPlayer(tournamentId, activePick.pick_number, player.id.toInt())
                                    } else {
                                        viewModel.captainPick(tournamentId, player.id.toInt())
                                    }
                                    showPlayerPicker = false
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(player.full_name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(player.playing_role ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPlayerPicker = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Draft Room", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isLoading && draftState == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (draftState == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ Server Not Available", fontSize = screenConfig.headingTextSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Draft Room requires the Laravel backend to be running.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Run: php artisan serve", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Setup steps:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("1. Install XAMPP/WAMP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text("2. Create database 'cricket_draft'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text("3. Run: php artisan migrate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text("4. Run: php artisan serve", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onNavigateBack, shape = RoundedCornerShape(10.dp)) { Text("Go Back") }
                }
            }
        } else {
            val state = draftState!!

            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // ── Timer & Status Bar ──
                DraftStatusBar(state, isAdmin, viewModel, tournamentId)

                // ── Round Progress ──
                DraftRoundProgress(state)

                // ── Team Squads (horizontal scroll) ──
                DraftTeamSquadsRow(state.team_squads)

                // ── Available Players + Pick Button ──
                DraftAvailablePlayersSection(
                    players = state.available_players,
                    canPick = state.captain_can_pick || (isAdmin && state.status in listOf("live", "paused")),
                    onPickClick = { showPlayerPicker = true }
                )

                // ── Admin Controls ──
                if (isAdmin) {
                    DraftAdminControls(state, viewModel, tournamentId)
                }
            }
        }
    }
}

@Composable
private fun DraftStatusBar(state: DraftStateData, isAdmin: Boolean, viewModel: DraftViewModel, tournamentId: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Status badge
                Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.2f)) {
                    Text(state.status.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }

                // Revision
                Text("v${state.revision}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current pick info
            val activePick = state.picks.firstOrNull { it.status == "active" }
            if (activePick != null) {
                Text("Round ${activePick.round ?: "?"} • Pick #${activePick.pick_number}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("${activePick.team?.name ?: "Unknown"}'s turn", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else if (state.status == "setup" || state.status == "paused") {
                state.next_pick?.let { next ->
                    Text("Next: Round ${next.round_number} • ${next.team?.name ?: "Unknown"}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            } else if (state.status == "completed") {
                Text("Draft Completed! 🎉", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Timer
            if (state.timer.remaining_seconds != null && state.status == "live") {
                Spacer(modifier = Modifier.height(6.dp))
                val remaining = state.timer.remaining_seconds!!
                val minutes = remaining / 60
                val seconds = remaining % 60
                val timerColor = if (remaining <= 10) Color(0xFFFF5252) else Color.White
                Text("⏱ ${minutes}:${String.format("%02d", seconds)}", color = timerColor, fontSize = screenConfig.scoreTextSize, fontWeight = FontWeight.ExtraBold)
            }

            // Summary
            Spacer(modifier = Modifier.height(6.dp))
            Text("${state.summary.selected}/${state.summary.total} picks done", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun DraftRoundProgress(state: DraftStateData) {
    if (state.rounds.isNotEmpty()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.rounds) { round ->
                val bgColor = when (round.status) {
                    "completed" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                    "active" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when (round.status) {
                    "completed" -> Color(0xFF4CAF50)
                    "active" -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Card(
                    modifier = Modifier.width(100.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(round.name ?: "R${round.round_number}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text("${round.selected}/${round.total}", fontSize = 10.sp, color = textColor.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftTeamSquadsRow(teams: List<DraftTeamSquad>) {
    if (teams.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("Team Squads", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(teams) { team ->
                    Card(
                        modifier = Modifier.width(140.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(team.name ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${team.selected_count} players", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            team.selected_players.forEach { player ->
                                Text("• ${player.full_name ?: "?"}", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftAvailablePlayersSection(players: List<DraftAvailablePlayer>, canPick: Boolean, onPickClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Available Players (${players.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (canPick) {
                Button(
                    onClick = onPickClick,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PICK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(players) { player ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(player.full_name ?: "Unknown", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(player.playing_role ?: "", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DraftAdminControls(state: DraftStateData, viewModel: DraftViewModel, tournamentId: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("Admin Controls", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (state.status) {
                    "setup", "paused" -> {
                        if (state.can_start_next_pick) {
                            AdminButton("▶ Start", Modifier.weight(1f)) { viewModel.startDraft(tournamentId) }
                        }
                        AdminButton("Resume", Modifier.weight(1f), enabled = state.status == "paused") { viewModel.resumeDraft(tournamentId) }
                    }
                    "live" -> {
                        AdminButton("⏸ Pause", Modifier.weight(1f)) { viewModel.pauseDraft(tournamentId) }
                        AdminButton("+60s", Modifier.weight(1f)) { viewModel.extendTimer(tournamentId, 60) }
                    }
                    "expired" -> {
                        AdminButton("⏭ Skip", Modifier.weight(1f)) { viewModel.skipPick(tournamentId) }
                        AdminButton("+60s", Modifier.weight(1f)) { viewModel.extendTimer(tournamentId, 60) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminButton("↩ Undo", Modifier.weight(1f), enabled = state.summary.selected > 0) { viewModel.undoPick(tournamentId) }
            }
        }
    }
}

@Composable
private fun AdminButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
