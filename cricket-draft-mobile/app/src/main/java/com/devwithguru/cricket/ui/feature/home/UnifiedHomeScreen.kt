package com.devwithguru.cricket.ui.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.devwithguru.cricket.ui.components.PremiumMatchCard
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.devwithguru.cricket.ui.feature.player.PlayerMatchesViewModel
import com.devwithguru.cricket.ui.feature.player.PlayerProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedHomeScreen(
    userName: String,
    isDarkTheme: Boolean = false,
    onToggleTheme: (Boolean) -> Unit = {},
    viewModel: PlayerMatchesViewModel = hiltViewModel(),
    profileViewModel: PlayerProfileViewModel = hiltViewModel(),
    onNavigateToCreateMatch: () -> Unit,
    onNavigateToCreateTournament: () -> Unit,
    onNavigateToMyTournaments: () -> Unit,
    onNavigateToMyTeams: () -> Unit,
    onNavigateToPlayerProfile: () -> Unit,
    onNavigateToTournamentHub: (tournamentId: String) -> Unit,
    onNavigateToMatchCenter: (matchId: String) -> Unit,
    onNavigateToRecentMatches: () -> Unit,
    onSearchClick: () -> Unit,
    onLogout: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("Hub") }
    LaunchedEffect(Unit) { viewModel.loadAllFixtures() }
    // Load current user's profile stats for the profile card
    LaunchedEffect(Unit) { profileViewModel.loadPlayer("p1") }
    val profileStats by profileViewModel.stats.collectAsState()
    val liveFixtures by viewModel.fixtures.collectAsState()
    val pagerState = rememberPagerState(pageCount = { maxOf(liveFixtures.size, 1) })
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Drawer Header (STUMPS logo)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Icon(
                                Icons.Filled.SportsCricket,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "STUMPS",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Navigation Links
                        DrawerItem(
                            title = "Home",
                            icon = Icons.Filled.Home,
                            onClick = {
                                scope.launch { drawerState.close() }
                            }
                        )

                        DrawerItem(
                            title = "My Tournaments",
                            icon = Icons.Filled.Dashboard,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToMyTournaments()
                            }
                        )

                        DrawerItem(
                            title = "Create Match",
                            icon = Icons.Filled.Add,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToCreateMatch()
                            }
                        )

                        DrawerItem(
                            title = "New Tournament",
                            icon = Icons.Filled.EmojiEvents,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToCreateTournament()
                            }
                        )

                        DrawerItem(
                            title = "My Teams",
                            icon = Icons.Filled.Groups,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToMyTeams()
                            }
                        )

                        DrawerItem(
                            title = "Search Players/Teams",
                            icon = Icons.Filled.Search,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onSearchClick()
                            }
                        )

                        DrawerItem(
                            title = "My Profile",
                            icon = Icons.Filled.Person,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToPlayerProfile()
                            }
                        )
                    }

                    // Theme switcher toggle and divider before Logout
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                    contentDescription = "Theme Icon",
                                    tint = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Dark Mode",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = onToggleTheme,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            )
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                    }

                    // Logout Button at the bottom
                    DrawerItem(
                        title = "Logout",
                        icon = Icons.Filled.ExitToApp,
                        color = Color.Red,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.SportsCricket,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "STUMPS",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.8).sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

            // Section 1: Live & Upcoming Matches (Matches with redirect link)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Matches",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onNavigateToRecentMatches) {
                        Text(
                            text = "»",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    if (liveFixtures.isNotEmpty() && page < liveFixtures.size) {
                        val f = liveFixtures[page]
                        PremiumMatchCard(
                            tournamentName = "${f.homeTeam} vs ${f.awayTeam}",
                            stageInfo = "${f.matchType}, ${f.ballType}, ${f.date}",
                            teamA = f.homeTeam,
                            teamB = f.awayTeam,
                            scoreA = if (f.currentInnings == 1) "${f.currentRuns}-${f.currentWickets} (${f.oversBowled})" else "${f.firstInningsRuns ?: 0}",
                            scoreB = if (f.currentInnings == 2) "${f.currentRuns}-${f.currentWickets} (${f.oversBowled})" else "Yet to bat",
                            isLive = f.status == "Live",
                            modifier = Modifier.fillMaxWidth().height(170.dp),
                            onClick = { onNavigateToMatchCenter(f.id) },
                            onViewTournamentClick = {}
                        )
                    } else {
                        PremiumMatchCard(
                            tournamentName = null,
                            stageInfo = "No live matches",
                            teamA = "—",
                            teamB = "—",
                            scoreA = "",
                            scoreB = "",
                            isLive = false,
                            modifier = Modifier.fillMaxWidth().height(170.dp),
                            onClick = {},
                            onViewTournamentClick = {}
                        )
                    }
                }

                // Pager Indicators representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 3) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (i == pagerState.currentPage) 6.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        )
                    }
                }
            }

            // Section 2: Profile Header & Quick Access Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                MyProfileQuickAccessCard(
                    playerName = userName.split("@").first().capitalize(),
                    role = "Player",
                    matches = profileStats?.matches?.toString() ?: "--",
                    runs = profileStats?.runs?.toString() ?: "--",
                    wickets = profileStats?.wickets?.toString() ?: "--",
                    rating = 0f,
                    onClick = onNavigateToPlayerProfile
                )
            }

            // Section 3: Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateToCreateMatch,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Create Match",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToCreateTournament,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Tournament",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Section 4: Active Campaigns (Tournaments)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Active Campaigns",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToMyTournaments() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TournamentItemCard(
                        title = "Summer Smash '24",
                        subtitle = "Quarter Finals • 8 Teams",
                        icon = Icons.Default.EmojiEvents,
                        onClick = { onNavigateToTournamentHub("summer-smash-24") }
                    )

                    TournamentItemCard(
                        title = "Corporate Cup",
                        subtitle = "Registration Open",
                        icon = Icons.Filled.EditCalendar,
                        highlightPrimary = true,
                        onClick = { onNavigateToTournamentHub("corporate-cup") }
                    )
                }
            }

            // Section 5: Directory (Bento Bottom Grid)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DirectoryBentoCard(
                    title = "My Tournaments",
                    icon = Icons.Filled.Dashboard,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMyTournaments
                )

                DirectoryBentoCard(
                    title = "My Teams",
                    icon = Icons.Filled.Groups,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToMyTeams
                )
            }
        }
    }
}
}

// 🔴 Live Match Card Composable
@Composable
fun LiveMatchCard(
    tournamentName: String,
    teamA: String,
    teamB: String,
    scoreA: String,
    scoreB: String,
    overs: String,
    crr: String,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = pulseAlpha))
                    )
                    Text("Live", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Text(tournamentName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(scoreA, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(teamA, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("v", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(scoreB, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(teamB, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overs: $overs",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "CRR: $crr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

// Upcoming Match Card Composable
@Composable
fun UpcomingMatchCard(
    time: String,
    tournamentStage: String,
    teamA: String,
    teamB: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    Text(time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Text(tournamentStage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamAvatarCircle(teamA)
                Text("vs", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                TeamAvatarCircle(teamB)
            }
        }
    }
}

@Composable
fun TeamAvatarCircle(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// My Profile Quick Access Card Composable
@Composable
fun MyProfileQuickAccessCard(
    playerName: String,
    role: String,
    matches: String,
    runs: String,
    wickets: String,
    rating: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
        border = null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: "All" on left, "Following ♥" on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Following",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Statistics Row: Labels at top, values at bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Matches",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = matches,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = (-0.4).sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Runs",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = runs,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = (-0.4).sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Wickets",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = wickets,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = (-0.4).sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatsMiniBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightGreen: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlightGreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Tournament Item Card Composable
@Composable
fun TournamentItemCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlightPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (highlightPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = if (highlightPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Directory Bento Card Composable
@Composable
fun DirectoryBentoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

// Bottom Navigation Bar Custom Composable
@Composable
fun BottomNavigationBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                title = "Hub",
                icon = Icons.Filled.GridView,
                activeIcon = Icons.Filled.GridView,
                isActive = activeTab == "Hub",
                onClick = { onTabSelected("Hub") }
            )

            BottomNavItem(
                title = "Draft",
                icon = Icons.Filled.Groups,
                activeIcon = Icons.Filled.Groups,
                isActive = activeTab == "Draft",
                onClick = { onTabSelected("Draft") }
            )

            BottomNavItem(
                title = "Matches",
                icon = Icons.Filled.Scoreboard,
                activeIcon = Icons.Filled.Scoreboard,
                isActive = activeTab == "Matches",
                onClick = { onTabSelected("Matches") }
            )

            BottomNavItem(
                title = "Profile",
                icon = Icons.Filled.Person,
                activeIcon = Icons.Filled.Person,
                isActive = activeTab == "Profile",
                onClick = { onTabSelected("Profile") }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    if (isActive) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(activeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
fun DrawerItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
