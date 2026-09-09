package com.devwithguru.cricket.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A stable, copyable identifier pill shared by Players, Teams and Tournaments.
 *
 * Shows the entity's REAL backend identifier (Team = TEAM-XXXXX,
 * Player = 6-digit profile code, Tournament = TRN-XXXXX) with a copy icon —
 * one tap copies the exact ID used by the backend to the clipboard.
 */
@Composable
fun CopyableId(
    id: String?,
    label: String = "ID",
    modifier: Modifier = Modifier,
    fontSize: Int = 11
) {
    val context = LocalContext.current
    val display = id?.takeIf { it.isNotBlank() } ?: return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), RoundedCornerShape(99.dp))
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, display))
                    Toast.makeText(context, "$label copied: $display", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: $display",
            fontSize = fontSize.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        )
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy $label",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(13.dp)
        )
    }
}
