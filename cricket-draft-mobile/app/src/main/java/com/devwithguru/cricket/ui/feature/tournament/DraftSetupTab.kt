package com.devwithguru.cricket.ui.feature.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.devwithguru.cricket.data.api.AdminTeamData
import com.devwithguru.cricket.data.api.DraftPickData
import com.devwithguru.cricket.data.api.DraftRoundData
import com.devwithguru.cricket.data.api.DraftSetupPick
import com.devwithguru.cricket.data.api.DraftSetupRequest
import com.devwithguru.cricket.data.api.DraftSetupRound

/**
 * Draft setup tab — manual round-by-round pick configuration.
 *
 * Admin flow:
 * 1. Tap "Add Round" → creates empty round
 * 2. Tap "Add Pick" in a round → select a team
 * 3. Repeat until all picks for the round are added
 * 4. Add more rounds as needed
 * 5. Save → sends to backend
 * 6. Start Draft → navigates to draft room
 */
@Composable
fun DraftSetupTab(
    teams: List<AdminTeamData>,
    picks: List<DraftPickData>,
    rounds: List<DraftRoundData>,
    draftStatus: String?,
    onStartDraft: () -> Unit,
    onSaveDraft: (DraftSetupRequest) -> Unit = {}
) {
    // Local state: rounds with their picks
    data class LocalPick(val teamId: Int, val teamName: String, val shortName: String)
    data class LocalRound(var name: String, val picks: MutableList<LocalPick>)

    var localRounds by remember { mutableStateOf(mutableListOf<LocalRound>()) }
    var roundCounter by remember { mutableIntStateOf(0) }
    var showTeamPickerForRound by remember { mutableIntStateOf(-1) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Existing draft progress (if any) ──
            if (picks.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Draft In Progress", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (draftStatus != null) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = when (draftStatus) { "live" -> Color(0xFFEF4444).copy(alpha = 0.15f); "completed" -> Color(0xFF22C55E).copy(alpha = 0.15f); else -> MaterialTheme.colorScheme.surface }) {
                                        Text(draftStatus.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = when (draftStatus) { "live" -> Color(0xFFEF4444); "completed" -> Color(0xFF22C55E); else -> MaterialTheme.colorScheme.onSurfaceVariant }, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${picks.count { it.status == "selected" }}/${picks.size} picks completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)) }
            }

            // ── Teams Reference ──
            if (teams.isNotEmpty()) {
                item {
                    Text("Available Teams", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        teams.forEach { team ->
                            Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(team.short_name ?: "?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(team.name ?: "", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // ── Rounds ──
            if (localRounds.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Draft Rounds", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${localRounds.size} rounds • ${localRounds.sumOf { it.picks.size }} total picks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            itemsIndexed(localRounds) { roundIndex, round ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Round header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
                                    Text("R${roundIndex + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Text(round.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = {
                                localRounds = localRounds.toMutableList().also { it.removeAt(roundIndex) }
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, "Remove round", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Picks in this round
                        if (round.picks.isEmpty()) {
                            Text("No picks yet — tap \"Add Pick\" below", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        } else {
                            round.picks.forEachIndexed { pickIndex, pick ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                        Text("${roundIndex * 10 + pickIndex + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                                        Text(pick.shortName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(pick.teamName, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        localRounds = localRounds.toMutableList().also { roundsList ->
                                            roundsList[roundIndex] = roundsList[roundIndex].copy(
                                                picks = roundsList[roundIndex].picks.toMutableList().also { it.removeAt(pickIndex) }
                                            )
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, "Remove pick", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Add Pick button
                        OutlinedButton(
                            onClick = { showTeamPickerForRound = roundIndex },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Pick", fontSize = 11.sp)
                        }
                    }
                }
            }

            // ── Add Round Button ──
            item {
                OutlinedButton(
                    onClick = {
                        roundCounter++
                        localRounds = localRounds.toMutableList().also {
                            it.add(LocalRound(name = "Round $roundCounter", picks = mutableListOf()))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Round", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Not enough teams ──
            if (teams.size < 2) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)), shape = RoundedCornerShape(10.dp)) {
                        Text("⚠️ Add at least 2 teams before configuring draft", modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── Team Picker Dialog ──
        if (showTeamPickerForRound >= 0 && showTeamPickerForRound < localRounds.size) {
            val roundIndex = showTeamPickerForRound
            val round = localRounds[roundIndex]
            val usedTeamIds = round.picks.map { it.teamId }.toSet()

            AlertDialog(
                onDismissRequest = { showTeamPickerForRound = -1 },
                title = { Text("Select Team for R${roundIndex + 1}", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        teams.filter { it.id !in usedTeamIds }.forEach { team ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                                    localRounds = localRounds.toMutableList().also { roundsList ->
                                        roundsList[roundIndex] = roundsList[roundIndex].copy(
                                            picks = roundsList[roundIndex].picks.toMutableList().also {
                                                it.add(LocalPick(team.id, team.name ?: "Team", team.short_name ?: "?"))
                                            }
                                        )
                                    }
                                    showTeamPickerForRound = -1
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                                        Text(team.short_name ?: "?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(team.name ?: "Unknown", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        if (teams.all { it.id in usedTeamIds }) {
                            Text("All teams already picked in this round", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { showTeamPickerForRound = -1 }) { Text("Cancel") } }
            )
        }

        // ── Bottom: Save + Start Draft ──
        Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val hasRounds = localRounds.isNotEmpty() && localRounds.all { it.picks.isNotEmpty() }

                if (hasRounds) {
                    Button(
                        onClick = {
                            // Build and save draft setup
                            val setupRounds = localRounds.mapIndexed { index, round ->
                                DraftSetupRound(
                                    round_number = index + 1,
                                    name = round.name,
                                    picks = round.picks.mapIndexed { pickIndex, pick ->
                                        DraftSetupPick(
                                            team_id = pick.teamId,
                                            pick_number = index * 10 + pickIndex + 1,
                                            pick_duration = 60
                                        )
                                    }
                                )
                            }
                            onSaveDraft(DraftSetupRequest(rounds = setupRounds))
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Save Draft Setup", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onStartDraft,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = teams.size >= 2
                ) {
                    Icon(Icons.Default.SportsCricket, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Draft →", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
