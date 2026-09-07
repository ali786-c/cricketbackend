package com.devwithguru.cricket.ui.feature.match.toss

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.devwithguru.cricket.ui.theme.screenConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TossScreen(
    homeTeamName: String,
    awayTeamName: String,
    onTossComplete: (tossWinner: String, tossDecision: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Step 1: Select toss winner, Step 2: Choose bat/bowl, Step 3: Coin flip animation, Step 4: Result
    var tossStep by remember { mutableStateOf(1) } // 1 = pick winner, 2 = pick decision, 3 = flipping, 4 = done
    var selectedWinner by remember { mutableStateOf("") }
    var selectedDecision by remember { mutableStateOf("") }

    // Coin flip animation state
    val scope = rememberCoroutineScope()
    var isFlipping by remember { mutableStateOf(false) }
    var flipComplete by remember { mutableStateOf(false) }

    // Infinite rotating animation while flipping
    val infiniteTransition = rememberInfiniteTransition(label = "coinFlip")
    val coinRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coinRotation"
    )

    // Scale pulse for result reveal
    val resultScale = remember { Animatable(0f) }
    val resultAlpha = remember { Animatable(0f) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TOSS",
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
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            radius = 1000f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (tossStep) {
                    // ===== STEP 1: SELECT WHO WON THE TOSS =====
                    1 -> {
                        Text(
                            text = "Who won the toss?",
                            fontSize = screenConfig.scoreTextSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Home Team Button
                        TeamTossCard(
                            teamName = homeTeamName,
                            isSelected = selectedWinner == homeTeamName,
                            onClick = { selectedWinner = homeTeamName }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "VS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Away Team Button
                        TeamTossCard(
                            teamName = awayTeamName,
                            isSelected = selectedWinner == awayTeamName,
                            onClick = { selectedWinner = awayTeamName }
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Next Button
                        Button(
                            onClick = { if (selectedWinner.isNotBlank()) tossStep = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedWinner.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.White.copy(alpha = 0.05f)
                            )
                        ) {
                            Text("NEXT", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                        }
                    }

                    // ===== STEP 2: BAT OR BOWL =====
                    2 -> {
                        Text(
                            text = "$selectedWinner elected to?",
                            fontSize = screenConfig.scoreTextSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // BAT option
                            DecisionCard(
                                label = "BAT",
                                emoji = "🏏",
                                isSelected = selectedDecision == "Batting",
                                onClick = { selectedDecision = "Batting" },
                                modifier = Modifier.weight(1f)
                            )

                            // BOWL option
                            DecisionCard(
                                label = "BOWL",
                                emoji = "⚾",
                                isSelected = selectedDecision == "Bowling",
                                onClick = { selectedDecision = "Bowling" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // FLIP COIN Button
                        Button(
                            onClick = {
                                if (selectedDecision.isNotBlank()) {
                                    tossStep = 3
                                    isFlipping = true
                                    scope.launch {
                                        delay(2500) // Flip for 2.5 seconds
                                        isFlipping = false
                                        flipComplete = true
                                        tossStep = 4
                                        // Animate result reveal
                                        launch { resultScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
                                        launch { resultAlpha.animateTo(1f, tween(400)) }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedDecision.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.White.copy(alpha = 0.05f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FLIP COIN", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp)
                        }
                    }

                    // ===== STEP 3: COIN FLIPPING ANIMATION =====
                    3 -> {
                        Spacer(modifier = Modifier.height(40.dp))

                        // Spinning coin
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .graphicsLayer {
                                    rotationX = coinRotation
                                    cameraDistance = 12f * density
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700), // Gold
                                            Color(0xFFFFA000), // Dark Gold
                                            Color(0xFFFFD700)  // Gold
                                        )
                                    )
                                )
                                .border(3.dp, Color(0xFFB8860B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF3E2723),
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Flipping...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }

                    // ===== STEP 4: RESULT REVEALED =====
                    4 -> {
                        Column(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = resultScale.value
                                    scaleY = resultScale.value
                                    alpha = resultAlpha.value
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Winner coin (static, landed)
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700),
                                                Color(0xFFFFA000),
                                                Color(0xFFFFD700)
                                            )
                                        )
                                    )
                                    .border(3.dp, Color(0xFFB8860B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedWinner.take(2).uppercase(),
                                    fontSize = screenConfig.displayTextSize,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF3E2723)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "$selectedWinner won the toss!",
                                fontSize = screenConfig.headingTextSize,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Elected to ${selectedDecision.uppercase()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Summary card
                            val loser = if (selectedWinner == homeTeamName) awayTeamName else homeTeamName
                            val loserAction = if (selectedDecision == "Batting") "Bowl" else "Bat"
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "🏏 $selectedWinner will ${selectedDecision.lowercase()} first",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "⚾ $loser will ${loserAction.lowercase()} first",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))

                            // Proceed to Lineup
                            Button(
                                onClick = { onTossComplete(selectedWinner, selectedDecision) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("PROCEED TO LINEUP →", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== Team Toss Selection Card =====
@Composable
private fun TeamTossCard(
    teamName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Team avatar circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = teamName.take(2).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Text(
                text = teamName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }
    }
}

// ===== Bat / Bowl Decision Card =====
@Composable
private fun DecisionCard(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = screenConfig.displayTextSize
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
    }
}
