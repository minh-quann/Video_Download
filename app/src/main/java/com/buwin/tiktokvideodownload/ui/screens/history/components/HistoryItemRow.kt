package com.buwin.tiktokvideodownload.ui.screens.history.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buwin.tiktokvideodownload.R
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.download.shimmerEffect
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.Backdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Platform enumeration for download source identification.
 */
enum class DownloadPlatform {
    TIKTOK,
    FACEBOOK
}

/**
 * Determines whether the download record originates from TikTok or Facebook.
 */
fun DownloadRecord.detectPlatform(): DownloadPlatform {
    val orig = originalUrl.lowercase()
    if (orig.contains("facebook.com") || orig.contains("fb.watch") || orig.contains("fb.com")) {
        return DownloadPlatform.FACEBOOK
    }
    if (orig.contains("tiktok.com") || orig.contains("douyin.com")) {
        return DownloadPlatform.TIKTOK
    }

    val path = filePath.lowercase()
    if (path.contains("facebookdownloads")) return DownloadPlatform.FACEBOOK
    if (path.contains("tiktokdownloads")) return DownloadPlatform.TIKTOK

    val fmt = formatTitle.lowercase()
    if (fmt.contains("facebook")) return DownloadPlatform.FACEBOOK
    if (fmt.contains("tiktok")) return DownloadPlatform.TIKTOK

    val auth = author.lowercase()
    if (auth.contains("facebook")) return DownloadPlatform.FACEBOOK

    val t = title.lowercase()
    if (t.contains("facebook")) return DownloadPlatform.FACEBOOK

    // Default to TikTok as the primary platform
    return DownloadPlatform.TIKTOK
}

/**
 * Single item row displaying thumbnail, metadata, time, and playback button.
 * Includes graceful branded fallback when video thumbnails fail to load or expire.
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

    val platform = remember(record) { record.detectPlatform() }
    val isAudio = record.fileExtension.equals("mp3", ignoreCase = true)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail preview with branded fallback
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 0.8.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            var isError by remember(record.coverUrl) { mutableStateOf(false) }
            val context = LocalContext.current
            val imageRequest = remember(record.coverUrl) {
                ImageRequest.Builder(context)
                    .data(record.coverUrl)
                    .size(160, 160)
                    .crossfade(150)
                    .build()
            }

            if (record.coverUrl.isNotEmpty() && !isError) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = record.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isError = true }
                )
            } else {
                PlatformFallbackThumbnail(
                    platform = platform,
                    isAudio = isAudio,
                    isDark = isDark
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

/**
 * Premium branded thumbnail placeholder displayed when video cover fails or is unavailable.
 */
@Composable
fun PlatformFallbackThumbnail(
    platform: DownloadPlatform,
    isAudio: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = when (platform) {
        DownloadPlatform.FACEBOOK -> Brush.linearGradient(
            listOf(
                Color(0xFF1877F2),
                Color(0xFF0C63D4)
            )
        )
        DownloadPlatform.TIKTOK -> Brush.linearGradient(
            listOf(
                Color(0xFF1C1D26),
                Color(0xFF0B0B10)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        when (platform) {
            DownloadPlatform.FACEBOOK -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_facebook),
                    contentDescription = "Facebook",
                    modifier = Modifier.size(26.dp)
                )
            }
            DownloadPlatform.TIKTOK -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_tiktok),
                    contentDescription = "TikTok",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Audio badge overlay for MP3 music downloads
        if (isAudio) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.MusicNote,
                    contentDescription = "Audio",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
