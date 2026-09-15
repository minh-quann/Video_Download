package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote

/**
 * Multi-select tool bar for the video editor.
 * Each tool can be toggled independently (pipeline approach), not mutually exclusive.
 * Active tools show a check indicator and highlighted border.
 */
@Composable
fun EditorToolBar(
    isTrimEnabled: Boolean,
    isMuteEnabled: Boolean,
    isReplaceAudioEnabled: Boolean,
    isExtractAudioOnly: Boolean,
    onToggleTrim: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleReplaceAudio: () -> Unit,
    onToggleExtractAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EditorToolChip(
            title = "Cắt Video",
            icon = Icons.Filled.ContentCut,
            isActive = isTrimEnabled,
            // Disable trim toggle when extract-audio-only mode is on
            isEnabled = !isExtractAudioOnly,
            onClick = onToggleTrim,
            modifier = Modifier.weight(1f)
        )
        EditorToolChip(
            title = "Tắt Tiếng",
            icon = Icons.Filled.VolumeOff,
            isActive = isMuteEnabled,
            // Mute conflicts with Replace Audio and Extract Audio
            isEnabled = !isReplaceAudioEnabled && !isExtractAudioOnly,
            onClick = onToggleMute,
            modifier = Modifier.weight(1f)
        )
        EditorToolChip(
            title = "Ghép Nhạc",
            icon = Icons.Filled.Audiotrack,
            isActive = isReplaceAudioEnabled,
            // Replace audio conflicts with Mute and Extract
            isEnabled = !isMuteEnabled && !isExtractAudioOnly,
            onClick = onToggleReplaceAudio,
            modifier = Modifier.weight(1f)
        )
        EditorToolChip(
            title = "Tách Nhạc",
            icon = CupertinoIcons.Outlined.MusicNote,
            isActive = isExtractAudioOnly,
            // Extract audio is standalone – disables all other ops
            isEnabled = !isTrimEnabled && !isMuteEnabled && !isReplaceAudioEnabled,
            onClick = onToggleExtractAudio,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual toggle chip for an editor tool.
 * Shows active state with accent color and a top indicator dot.
 */
@Composable
private fun EditorToolChip(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFF007AFF)
    val bgColor = when {
        isActive -> accentColor
        !isEnabled -> Color(0xFF151518)
        else -> Color(0xFF1E1E22)
    }
    val contentColor = when {
        isActive -> Color.White
        !isEnabled -> Color.Gray.copy(alpha = 0.5f)
        else -> Color.LightGray
    }
    val borderColor = if (isActive) accentColor.copy(alpha = 0.6f) else Color.Transparent

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = isEnabled || isActive) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Active indicator dot
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isActive) Color.White else Color.Transparent)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 11.5.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
