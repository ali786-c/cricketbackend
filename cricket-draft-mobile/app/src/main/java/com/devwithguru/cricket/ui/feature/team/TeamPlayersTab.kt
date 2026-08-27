package com.devwithguru.cricket.ui.feature.team

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.api.SquadPlayerData
import com.devwithguru.cricket.domain.model.RegisteredPlayer

data class PlayerRow(
    val id: String,
    val name: String,
    val role: String,
    val spec: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamPlayersTab(
    isCreator: Boolean,
    squad: List<SquadPlayerData> = emptyList(),
    allRegisteredPlayers: List<RegisteredPlayer> = emptyList(),
    onAddPlayerManually: (name: String, role: String) -> Unit = { _, _ -> },
    onAddExistingPlayer: (playerId: String) -> Unit = {},
    onAssignCaptain: (playerId: String) -> Unit = {},
    onAssignViceCaptain: (playerId: String) -> Unit = {},
    onAssignWicketkeeper: (playerId: String) -> Unit = {},
    onRemovePlayer: (playerId: String) -> Unit = {},
    onNavigateToPlayerDetail: (String) -> Unit
) {
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var expandedPlayerId by remember { mutableStateOf<String?>(null) }

    // Dialog state
    var dialogTabSelected by remember { mutableStateOf(0) } // 0 = Manually, 1 = Search
    var manualName by remember { mutableStateOf("") }
    var manualRole by remember { mutableStateOf("Batter") }
    var searchQuery by remember { mutableStateOf("") }

    val rolesList = listOf("Batter", "Bowler", "Wicketkeeper", "All-rounder")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Owner/Guest Mode Alert
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCreator) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isCreator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isCreator) "Owner Privileges Active (Manage Mode)" else "Read-Only View (Guest Mode)",
                        fontSize = 11.sp,
                        color = if (isCreator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Add Player Action Button (Strictly Creator Only)
        if (isCreator) {
            item {
                Button(
                    onClick = {
                        manualName = ""
                        manualRole = "Batter"
                        searchQuery = ""
                        dialogTabSelected = 0
                        showAddPlayerDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Player to Squad", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Empty state
        if (squad.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No players registered in this team squad yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Squad Groups
            val roles = listOf("Batter", "Wicketkeeper", "Bowler", "All-rounder")
            roles.forEach { role ->
                val rolePlayers = squad.filter { it.playing_role == role }
                if (rolePlayers.isNotEmpty()) {
                    item {
                        Text(
                            text = "${role}s",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(rolePlayers) { player ->
                        val playerIdStr = player.tournament_player_id.toString()
                        val isExpanded = isCreator && expandedPlayerId == playerIdStr
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isCreator) {
                                                expandedPlayerId = if (isExpanded) null else playerIdStr
                                            } else {
                                                onNavigateToPlayerDetail(playerIdStr)
                                            }
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text(
                                                text = player.player_name ?: "Unknown Player",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val designations = mutableListOf<String>()
                                            if (player.is_captain == true) designations.add("Captain")
                                            if (player.is_vice_captain == true) designations.add("Vice Captain")
                                            if (player.is_wicketkeeper == true) designations.add("Wicketkeeper")
                                            val specText = if (designations.isEmpty()) "Squad Member" else designations.joinToString(" • ")
                                            Text(
                                                text = specText,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (isCreator) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Edit,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand Options",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (isExpanded) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OptionChip(
                                            text = "C",
                                            isActive = player.is_captain == true,
                                            onClick = { onAssignCaptain(playerIdStr) }
                                        )
                                        OptionChip(
                                            text = "VC",
                                            isActive = player.is_vice_captain == true,
                                            onClick = { onAssignViceCaptain(playerIdStr) }
                                        )
                                        OptionChip(
                                            text = "WK",
                                            isActive = player.is_wicketkeeper == true,
                                            onClick = { onAssignWicketkeeper(playerIdStr) }
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = { onNavigateToPlayerDetail(playerIdStr) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Profile",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemovePlayer(playerIdStr) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove Player",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
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

    // Add Player Dialog
    if (showAddPlayerDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlayerDialog = false },
            title = { Text("Add Player to Squad", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabRow(
                        selectedTabIndex = dialogTabSelected,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = dialogTabSelected == 0,
                            onClick = { dialogTabSelected = 0 },
                            text = { Text("Manually", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = dialogTabSelected == 1,
                            onClick = { dialogTabSelected = 1 },
                            text = { Text("Search Registered", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (dialogTabSelected == 0) {
                        // Manually Form
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("Player Name") },
                            placeholder = { Text("e.g. Babar Azam") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Text("Playing Role", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rolesList.forEach { role ->
                                val isSel = manualRole == role
                                Card(
                                    onClick = { manualRole = role },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when(role) {
                                                "Wicketkeeper" -> "WK"
                                                "All-rounder" -> "All-R"
                                                else -> role
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Search Registered Form
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search Players") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        val filtered = allRegisteredPlayers.filter { player ->
                            player.name.contains(searchQuery, ignoreCase = true)
                        }

                        if (filtered.isEmpty()) {
                            Text(
                                text = "No registered players found.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filtered) { player ->
                                    Card(
                                        onClick = {
                                            onAddExistingPlayer(player.id)
                                            showAddPlayerDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Column {
                                                Text(player.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(player.role, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (dialogTabSelected == 0) {
                    Button(
                        onClick = {
                            if (manualName.isNotBlank()) {
                                onAddPlayerManually(manualName, manualRole)
                                showAddPlayerDialog = false
                            }
                        },
                        enabled = manualName.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlayerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OptionChip(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = { Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isActive,
            borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            selectedBorderColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    )
}
