package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.api.AdminTeamData

/**
 * Add Team Screen — PRD §10
 * Two modes:
 * 1. Create New Team — enter name, short name, optional captain/vice-captain/manager
 * 2. Add by Code — enter team code to join existing team
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeamScreen(
    tournamentId: String,
    existingTeams: List<AdminTeamData> = emptyList(),
    onTeamCreated: (name: String, shortName: String, captain: String?, viceCaptain: String?, manager: String?) -> Unit,
    onTeamJoinedByCode: (code: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedMode by remember { mutableIntStateOf(0) } // 0 = Create New, 1 = Add by Code

    // ── Create New Team state ──
    var teamName by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var captainName by remember { mutableStateOf("") }
    var viceCaptainName by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Add by Code state ──
    var joinCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Add Team", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
            // ── Mode Toggle ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Create New" to 0, "Add by Code" to 1).forEach { (label, index) ->
                    val isSelected = selectedMode == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedMode = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (index == 0) Icons.Default.Add else Icons.Default.Link,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ── Error ──
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            when (selectedMode) {
                0 -> CreateNewTeamForm(
                    teamName = teamName,
                    onTeamNameChange = { teamName = it; errorMessage = null },
                    shortName = shortName,
                    onShortNameChange = { shortName = it },
                    onCreateClick = {
                        when {
                            teamName.isBlank() -> errorMessage = "Please enter team name"
                            teamName.length < 2 -> errorMessage = "Team name must be at least 2 characters"
                            existingTeams.any { (it.name ?: "").equals(teamName.trim(), ignoreCase = true) } ->
                                errorMessage = "A team with this name already exists"
                            else -> {
                                onTeamCreated(
                                    teamName.trim(),
                                    shortName.trim().ifBlank { teamName.trim().take(3).uppercase() },
                                    null,
                                    null,
                                    null
                                )
                            }
                        }
                    }
                )
                1 -> AddByCodeForm(
                    joinCode = joinCode,
                    onJoinCodeChange = { joinCode = it; errorMessage = null },
                    onJoinClick = {
                        when {
                            joinCode.isBlank() -> errorMessage = "Please enter a team code"
                            joinCode.length < 4 -> errorMessage = "Team code must be at least 4 characters"
                            else -> onTeamJoinedByCode(joinCode.trim())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateNewTeamForm(
    teamName: String,
    onTeamNameChange: (String) -> Unit,
    shortName: String,
    onShortNameChange: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Team Name
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Team Information", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = teamName,
                    onValueChange = onTeamNameChange,
                    label = { Text("Team Name *", fontSize = 12.sp) },
                    placeholder = { Text("e.g., Lahore Lions", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = teamFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )

                OutlinedTextField(
                    value = shortName,
                    onValueChange = onShortNameChange,
                    label = { Text("Short Name", fontSize = 12.sp) },
                    placeholder = { Text("e.g., LAH (auto-generated if empty)", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = teamFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Create Button
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Default.SportsCricket, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CREATE TEAM", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AddByCodeForm(
    joinCode: String,
    onJoinCodeChange: (String) -> Unit,
    onJoinClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    "Enter Team Code",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Ask your team manager for the 6-digit team code to join their team.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = joinCode,
                    onValueChange = onJoinCodeChange,
                    label = { Text("Team Code", fontSize = 12.sp) },
                    placeholder = { Text("e.g., LAH4821", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = teamFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                )
            }
        }

        Button(
            onClick = onJoinClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = joinCode.isNotBlank()
        ) {
            Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("JOIN TEAM", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun teamFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
    focusedContainerColor = MaterialTheme.colorScheme.background,
    unfocusedContainerColor = MaterialTheme.colorScheme.background
)
