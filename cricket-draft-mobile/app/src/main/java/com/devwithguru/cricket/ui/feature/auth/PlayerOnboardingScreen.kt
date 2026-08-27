package com.devwithguru.cricket.ui.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationCity
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
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOnboardingScreen(
    onSubmitRegistration: (name: String, role: String, batting: String, bowling: String, city: String, bio: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PlayerOnboardingViewModel = hiltViewModel()
) {
    val onboardingState by viewModel.uiState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var primaryRole by remember { mutableStateOf("") }
    var battingStyle by remember { mutableStateOf("Right-Hand") }
    var bowlingStyle by remember { mutableStateOf("None") }
    var biography by remember { mutableStateOf("") }

    var isRoleDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Navigate on successful profile update
    LaunchedEffect(onboardingState.isCompleted) {
        if (onboardingState.isCompleted) {
            onSubmitRegistration(fullName, primaryRole, battingStyle, bowlingStyle, city, biography)
        }
    }

    val rolesList = listOf("Batter", "Bowler", "All-Rounder", "Wicketkeeper")
    val bowlingStyles = listOf("Fast", "Medium", "Spin", "None")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYER PROFILE ONBOARDING",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                navigationIcon = {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onNavigateBack() }
                            .padding(16.dp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.border(0.dp, Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Complete your profile to enter the Stumps Cricket Draft pool. Coaches rely on this data.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            if (onboardingState.error != null) {
                Text(
                    text = onboardingState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // 1. Personal Details Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ),
                border = borderStrokeWhiteOpacity()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "PERSONAL DETAILS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Name Input
                    Text(text = "Full Name", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = { Text("e.g. Virat Kohli", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = outlinedTextFieldCustomColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact Input
                    Text(text = "Contact Number", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        placeholder = { Text("+1 234 567 8900", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = outlinedTextFieldCustomColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // City Input
                    Text(text = "City", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        placeholder = { Text("e.g. London", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = outlinedTextFieldCustomColors(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Athletic Profile Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                ),
                border = borderStrokeWhiteOpacity()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ATHLETIC PROFILE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Picker
                    Text(text = "Primary Playing Role", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = primaryRole,
                            onValueChange = {},
                            placeholder = { Text("Select Role", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { isRoleDropdownExpanded = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = outlinedTextFieldCustomColors(),
                            enabled = false // Disable normal interaction to allow clicks
                        )

                        // Transparent overlay box to capture clicks securely
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { isRoleDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = isRoleDropdownExpanded,
                            onDismissRequest = { isRoleDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            rolesList.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        primaryRole = role
                                        isRoleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Batting Style Radio Toggle Buttons
                    Text(text = "Batting Style", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        listOf("Right-Hand", "Left-Hand").forEach { style ->
                            val isSelected = battingStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { battingStyle = style }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bowling Style Chips Selectors
                    Text(text = "Bowling Style", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bowlingStyles.forEach { style ->
                            val isSelected = bowlingStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { bowlingStyle = style }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Biography Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ),
                border = borderStrokeWhiteOpacity()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "BIOGRAPHY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Experience & Highlights", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = biography,
                        onValueChange = { biography = it },
                        placeholder = { Text("Briefly describe your cricketing experience, previous teams, and key achievements...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = outlinedTextFieldCustomColors(),
                        maxLines = 5
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Button
            Button(
                onClick = {
                    if (fullName.isBlank() || contactNumber.isBlank() || city.isBlank() || primaryRole.isBlank()) {
                        errorMessage = "Please fill in all details (Name, Contact, City, Role)"
                        return@Button
                    }
                    errorMessage = null
                    viewModel.submitProfile(fullName, primaryRole, battingStyle, bowlingStyle, city, biography)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !onboardingState.isLoading
            ) {
                if (onboardingState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "SUBMIT REGISTRATION",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.SportsCricket, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

// Utility Theme border strokes
@Composable
private fun borderStrokeWhiteOpacity() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
)

// Outlined TextField Custom Colors to match Pitch Premium Design System
@Composable
private fun outlinedTextFieldCustomColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)
