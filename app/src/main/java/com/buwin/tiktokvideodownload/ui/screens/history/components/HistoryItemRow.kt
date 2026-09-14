package com.buwin.tiktokvideodownload.ui.screens.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.Backdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.filled.Video
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single item row displaying thumbnail, metadata, time, and playback button.
 */
@Composable
fun HistoryItemRow(
    record: DownloadRecord,
    backdrop: Backdrop,
    onOpen: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val timeText = remember(record.timestamp) {
        if (record.timestamp > 0) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(record.timestamp))
        } else {
            ""
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail preview
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center
        ) {
            if (record.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = record.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = if (record.fileExtension == "mp3") CupertinoIcons.Outlined.MusicNote else CupertinoIcons.Filled.Video,
                    contentDescription = null,
                    tint = if (isDark) Color.White else Color(0xFF4B5563),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${record.author} • ${record.formatTitle}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (timeText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeText,
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Liquid Play Button
        LiquidRoundButton(
            onClick = onOpen,
            backdrop = backdrop,
            size = 38.dp,
            surfaceColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.15f)
        ) {
            Icon(
                imageVector = CupertinoIcons.Filled.Play,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
