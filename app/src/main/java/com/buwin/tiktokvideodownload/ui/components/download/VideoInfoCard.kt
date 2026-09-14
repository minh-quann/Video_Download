package com.buwin.tiktokvideodownload.ui.components.download

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.ArrowshapeTurnUpLeft
import io.github.alexzhirkevich.cupertino.icons.filled.BubbleLeft
import io.github.alexzhirkevich.cupertino.icons.filled.Eye
import io.github.alexzhirkevich.cupertino.icons.filled.Heart
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Modern compact TikTok hero card displaying a 3:4 video thumbnail alongside creator info and metrics.
 */
@Composable
fun VideoInfoCard(
    info: TikTokVideoInfo,
    cardBackground: Color,
    cardBorderColor: Color,
    onDownloadOption: ((DownloadOption) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showPreviewDialog by remember { mutableStateOf(false) }
    var videoFrameThumbnail by remember(info.id) { mutableStateOf<Bitmap?>(null) }

    val previewOption = info.options.firstOrNull { it.type == DownloadFormatType.VIDEO_HD_NO_WATERMARK }
        ?: info.options.firstOrNull { it.type == DownloadFormatType.VIDEO_SD_NO_WATERMARK }
        ?: info.options.firstOrNull { it.type != DownloadFormatType.AUDIO_MP3 }
    val previewVideoUrl = previewOption?.downloadUrl ?: ""

    // Extract real high-quality frame from video stream
    LaunchedEffect(info.id, previewVideoUrl) {
        if (previewVideoUrl.isNotEmpty() && videoFrameThumbnail == null) {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(previewVideoUrl, HashMap<String, String>())
                    val frame = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (frame != null) {
                        videoFrameThumbnail = frame
                    }
                    retriever.release()
                } catch (e: Throwable) {
                    // Fall back gracefully to AsyncImage cover
                }
            }
        }
    }

    if (showPreviewDialog && previewVideoUrl.isNotEmpty()) {
        VideoPreviewDialog(
            videoUrl = previewVideoUrl,
            info = info,
            onDismiss = { showPreviewDialog = false },
            onQuickDownload = {
                previewOption?.let { onDownloadOption?.invoke(it) }
                showPreviewDialog = false
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Left: 3:4 Vertical TikTok Thumbnail with Liquid Glass Play Overlay ──
            Box(
                modifier = Modifier
                    .width(108.dp)
                    .height(144.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E24))
                    .clickable(enabled = previewVideoUrl.isNotEmpty()) {
                        showPreviewDialog = true
                    }
            ) {
                if (videoFrameThumbnail != null) {
                    Image(
                        bitmap = videoFrameThumbnail!!.asImageBitmap(),
                        contentDescription = "Video Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(info.coverUrl)
                            .crossfade(true)
                            .setHeader("Referer", "https://www.tikwm.com/")
                            .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                            .build(),
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Liquid Glass Play Button in Center
                if (previewVideoUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.52f))
                            .border(1.2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Filled.Play,
                            contentDescription = "Xem trước",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(start = 2.dp)
                        )
                    }
                }

                // Duration Tag at Bottom Right
                if (info.durationSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${info.durationSeconds}s",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Right: Creator details, caption & engagement metrics ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(144.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Author profile row
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = info.authorAvatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.authorNickname,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${info.authorUsername}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Video caption
                    if (info.title.isNotEmpty()) {
                        Text(
                            text = info.title,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                        )
                    }
                }

                // Bottom: Metrics & tap hint
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Social counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompactStatItem(
                            icon = CupertinoIcons.Filled.Heart,
                            text = formatMetricCount(info.likesCount),
                            tint = Color(0xFFEF4444)
                        )
                        CompactStatItem(
                            icon = CupertinoIcons.Filled.BubbleLeft,
                            text = formatMetricCount(info.commentsCount),
                            tint = Color(0xFF3B82F6)
                        )
                        CompactStatItem(
                            icon = CupertinoIcons.Filled.ArrowshapeTurnUpLeft,
                            text = formatMetricCount(info.sharesCount),
                            tint = Color(0xFF10B981)
                        )
                    }

                    // Click to preview hint pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF007AFF).copy(alpha = 0.1f))
                            .clickable(enabled = previewVideoUrl.isNotEmpty()) {
                                showPreviewDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Filled.Eye,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Chạm xem trước video",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF007AFF)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact social count item with colorful icon.
 */
@Composable
private fun CompactStatItem(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Formats large metrics cleanly (e.g. 1.2M, 45.3K).
 */
private fun formatMetricCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        count > 0 -> count.toString()
        else -> "0"
    }
}
