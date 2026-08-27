package com.devwithguru.cricket.ui.feature.match.toss

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.devwithguru.cricket.ui.viewmodels.LineupViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.devwithguru.cricket.domain.model.RegisteredPlayer
import com.devwithguru.cricket.ui.theme.StatusLive

data class PlayerSelectable(
    val id: String,
    val name: String,
    val role: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TossLineupScreen(
    matchId: String,
    homeTeamName: String = "Ali Panthers",
    awayTeamName: String = "Islamabad Blasters",
    tossWinner: String = "",
    tossDecision: String = "",
    onStartMatchSuccess: (tossWinner: String, tossDecision: String, homeLineup: List<String>, awayLineup: List<String>) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LineupViewModel = hiltViewModel()
) {
    // Load squads for this match
    LaunchedEffect(matchId) {
        viewModel.loadSquadsForMatch(matchId)
    }

    // Lineup Tabs
    var selectedTeamTab by remember { mutableStateOf(0) }
    val teams = listOf(homeTeamName, awayTeamName)

    // Squad rosters (mutable so new players can be added)
    val homeSquad = remember { mutableStateListOf<PlayerSelectable>() }
    val awaySquad = remember { mutableStateListOf<PlayerSelectable>() }

    val homeSquadList by viewModel.homeSquad.collectAsState()
    val awaySquadList by viewModel.awaySquad.collectAsState()

    // Sync local state lists with ViewModel flows
    LaunchedEffect(homeSquadList) {
        homeSquad.clear()
        homeSquad.addAll(homeSquadList)
    }
    LaunchedEffect(awaySquadList) {
        awaySquad.clear()
        awaySquad.addAll(awaySquadList)
    }

    val squadSize by viewModel.squadSize.collectAsState()

    // Selected Player IDs
    var selectedHomePlayers by remember(matchId) { mutableStateOf(setOf<String>()) }
    var selectedAwayPlayers by remember(matchId) { mutableStateOf(setOf<String>()) }

    // Auto-select all initially
    LaunchedEffect(homeSquad.size, squadSize) {
        if (selectedHomePlayers.isEmpty() && homeSquad.isNotEmpty()) {
            selectedHomePlayers = homeSquad.take(squadSize).map { it.id }.toSet()
        }
    }
    LaunchedEffect(awaySquad.size, squadSize) {
        if (selectedAwayPlayers.isEmpty() && awaySquad.isNotEmpty()) {
            selectedAwayPlayers = awaySquad.take(squadSize).map { it.id }.toSet()
        }
    }

    // Add Player Dialog State
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var addPlayerForTeam by remember { mutableStateOf(0) } // 0 = home, 1 = away

    val currentSquad = if (selectedTeamTab == 0) homeSquad else awaySquad
    val currentSelected = if (selectedTeamTab == 0) selectedHomePlayers else selectedAwayPlayers

    val homeCount = homeSquad.count { it.id in selectedHomePlayers }
    val awayCount = awaySquad.count { it.id in selectedAwayPlayers }
    val currentCount = if (selectedTeamTab == 0) homeCount else awayCount
    val isFormValid = homeCount == squadSize && awayCount == squadSize

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LINEUP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$awayTeamName vs $homeTeamName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (isFormValid) {
                            onStartMatchSuccess(
                                tossWinner,
                                tossDecision,
                                homeSquad.filter { selectedHomePlayers.contains(it.id) }.map { it.name },
                                awaySquad.filter { selectedAwayPlayers.contains(it.id) }.map { it.name }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                    enabled = isFormValid
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFormValid) "START MATCH" else "Select $squadSize players for both teams",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Background ambient glow
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

            Column(modifier = Modifier.fillMaxSize()) {
                // Toss Result Banner (if toss was done)
                if (tossWinner.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$tossWinner won toss • Chose to ${tossDecision.lowercase()}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Team Tabs
                TabRow(
                    selectedTabIndex = selectedTeamTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)) }
                ) {
                    teams.forEachIndexed { index, teamName ->
                        val count = if (index == 0) homeCount else awayCount
                        val squad = if (index == 0) homeSquad else awaySquad
                        Tab(
                            selected = selectedTeamTab == index,
                            onClick = { selectedTeamTab = index },
                            text = {
                                Text(
                                    text = "$teamName ($count/${squad.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTeamTab == index) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                // Player List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add Player Button at top
                    item {
                        OutlinedButton(
                            onClick = {
                                addPlayerForTeam = selectedTeamTab
                                showAddPlayerDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                )
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ADD PLAYER",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    items(currentSquad) { player ->
                        val isSelected = player.id in currentSelected
                        PlayerLineupCard(
                            player = player,
                            isSelected = isSelected,
                            onToggle = {
                                if (selectedTeamTab == 0) {
                                    selectedHomePlayers = if (isSelected) {
                                        selectedHomePlayers - player.id
                                    } else {
                                        selectedHomePlayers + player.id
                                    }
                                } else {
                                    selectedAwayPlayers = if (isSelected) {
                                        selectedAwayPlayers - player.id
                                    } else {
                                        selectedAwayPlayers + player.id
                                    }
                                }
                            }
                        )
                    }

                    // Empty state
                    if (currentSquad.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No players yet",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Add players using the button above",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ===== ADD PLAYER DIALOG =====
        if (showAddPlayerDialog) {
            AddPlayerDialog(
                teamName = teams[addPlayerForTeam],
                onDismiss = { showAddPlayerDialog = false },
                onSearchPlayer = { id -> viewModel.searchPlayerById(id) },
                searchResult = viewModel.searchResult.value,
                onAddPlayer = { playerName, playerRole, existingId ->
                    val targetSquad = if (addPlayerForTeam == 0) homeSquad else awaySquad
                    if (existingId != null) {
                        // Player found by ID in registry — add to squad
                        val newPlayer = PlayerSelectable(existingId, playerName, playerRole)
                        targetSquad.add(newPlayer)
                        if (addPlayerForTeam == 0) {
                            selectedHomePlayers = selectedHomePlayers + existingId
                        } else {
                            selectedAwayPlayers = selectedAwayPlayers + existingId
                        }
                    } else {
                        // New player — register via ViewModel and auto-select on completion callback
                        viewModel.registerNewPlayer(playerName, playerRole, addPlayerForTeam == 0) { playerId ->
                            if (addPlayerForTeam == 0) {
                                selectedHomePlayers = selectedHomePlayers + playerId
                            } else {
                                selectedAwayPlayers = selectedAwayPlayers + playerId
                            }
                        }
                    }
                    showAddPlayerDialog = false
                }
            )
        }
    }
}

// ===== Player Lineup Card =====
@Composable
private fun PlayerLineupCard(
    player: PlayerSelectable,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .then(
                if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Name & Role
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = player.role,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ===== Add Player Dialog =====
@Composable
private fun AddPlayerDialog(
    teamName: String,
    onDismiss: () -> Unit,
    onAddPlayer: (name: String, role: String, existingId: String?) -> Unit,
    onSearchPlayer: (String) -> Unit,
    searchResult: RegisteredPlayer? = null
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Search by ID, 1 = New Player
    // Search by ID state
    var searchId by remember { mutableStateOf("") }
    var foundPlayer by remember { mutableStateOf<RegisteredPlayer?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // New player state
    var playerName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Batter") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
    val roles = listOf("Batter", "Bowler", "All-rounder", "Wicketkeeper")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Player",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Adding to $teamName",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                // Tab Switcher: Search by ID / New Player
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                "Search by ID",
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                "New Player",
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                when (activeTab) {
                    // ===== TAB 0: Search by Player ID =====
                    0 -> {
                        OutlinedTextField(
                            value = searchId,
                            onValueChange = {
                                searchId = it
                                foundPlayer = null
                                searchError = null
                            },
                            label = { Text("Player ID", fontSize = 12.sp) },
                            placeholder = { Text("e.g. h1, a2, p5", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Search Button
                        OutlinedButton(
                            onClick = {
                                onSearchPlayer(searchId.trim())
                                val result = searchResult
                                if (result != null) {
                                    foundPlayer = result
                                    searchError = null
                                } else {
                                    foundPlayer = null
                                    searchError = "No registered player found with ID \"${searchId.trim()}\""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            enabled = searchId.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SEARCH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Search Result
                        if (foundPlayer != null) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        val fp = foundPlayer
                                        if (fp != null) {
                                            Text(
                                                text = fp.name,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${fp.role} • ID: ${fp.id}",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Add found player button
                            Button(
                                onClick = {
                                    foundPlayer?.let {
                                        onAddPlayer(it.name, it.role, it.id)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ADD TO TEAM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        // Search error
                        if (searchError != null) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = StatusLive.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = searchError!!,
                                    color = StatusLive,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // ===== TAB 1: Add New Player Manually =====
                    1 -> {
                        OutlinedTextField(
                            value = playerName,
                            onValueChange = { playerName = it },
                            label = { Text("Player Name", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Imran Ali", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Role Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedRole,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Role", fontSize = 12.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { roleDropdownExpanded = true }) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { roleDropdownExpanded = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            DropdownMenu(
                                expanded = roleDropdownExpanded,
                                onDismissRequest = { roleDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                roles.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role) },
                                        onClick = {
                                            selectedRole = role
                                            roleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Add Button
                        Button(
                            onClick = {
                                if (playerName.isNotBlank()) {
                                    onAddPlayer(playerName.trim(), selectedRole, null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            enabled = playerName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ADD PLAYER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
