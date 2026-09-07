package com.devwithguru.cricket.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithguru.cricket.data.sync.SyncStatus

/**
 * Compact sync status indicator for the app bar or match center.
 *
 * Shows:
 * - Green dot + "Synced" when idle
 * - Spinning blue icon + "Syncing..." when pushing/pulling
 * - Orange badge with count when there are pending changes
 * - Red cloud when offline
 * - Manual sync button
 */
@Composable
fun SyncStatusIndicator(
    isOnline: Boolean,
    isSyncing: Boolean,
    syncStatus: SyncStatus,
    pendingCount: Int,
    lastSyncTime: Long,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = when {
                    !isOnline -> Color(0xFFFFF3E0) // Orange tint
                    isSyncing -> Color(0xFFE3F2FD) // Blue tint
                    syncStatus == SyncStatus.PARTIAL -> Color(0xFFFFF3E0)
                    else -> Color(0xFFE8F5E9) // Green tint
                },
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status icon
        if (isSyncing) {
            val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = "Syncing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation)
            )
        } else if (!isOnline) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Offline",
                tint = Color(0xFFE65100),
                modifier = Modifier.size(16.dp)
            )
        } else if (pendingCount > 0) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Pending sync",
                    tint = Color(0xFFEF6C00),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = "Synced",
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(16.dp)
            )
        }

        // Status text
        Text(
            text = when {
                !isOnline -> "Offline"
                isSyncing -> "Syncing..."
                pendingCount > 0 -> "$pendingCount pending"
                syncStatus == SyncStatus.PARTIAL -> "Partial"
                else -> "Synced"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                !isOnline -> Color(0xFFE65100)
                isSyncing -> MaterialTheme.colorScheme.primary
                pendingCount > 0 -> Color(0xFFEF6C00)
                else -> Color(0xFF2E7D32)
            }
        )

        // Pending count badge (when there are pending changes)
        AnimatedVisibility(
            visible = pendingCount > 0 && !isSyncing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        color = Color(0xFFEF6C00),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (pendingCount > 99) "99+" else "$pendingCount",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Manual sync button (only when online and not currently syncing)
        AnimatedVisibility(
            visible = isOnline && !isSyncing && pendingCount > 0,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = onManualSync,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync now",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Full-width sync status banner for match center (shows at top of screen).
 */
@Composable
fun SyncStatusBanner(
    isOnline: Boolean,
    isSyncing: Boolean,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline || isSyncing || pendingCount > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val bgColor by animateColorAsState(
            targetValue = when {
                !isOnline -> Color(0xFFE65100)
                isSyncing -> MaterialTheme.colorScheme.primary
                pendingCount > 0 -> Color(0xFFEF6C00)
                else -> Color.Transparent
            },
            label = "banner_color"
        )

        Box(
            modifier = Modifier
                .background(bgColor)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    !isOnline -> "📡 You're offline — scoring is saved locally and will sync when connected"
                    isSyncing -> "🔄 Syncing data with server..."
                    pendingCount > 0 -> "⏳ $pendingCount change${if (pendingCount != 1) "s" else ""} pending sync"
                    else -> ""
                },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
