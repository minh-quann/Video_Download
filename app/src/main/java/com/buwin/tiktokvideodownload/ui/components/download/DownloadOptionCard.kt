package com.buwin.tiktokvideodownload.ui.components.download

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.ArrowDownCircle
import io.github.alexzhirkevich.cupertino.icons.filled.Film
import io.github.alexzhirkevich.cupertino.icons.filled.PhotoStack
import io.github.alexzhirkevich.cupertino.icons.filled.Video
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowDownToLine
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote

/**
 * Hero prominent download CTA button for the primary No-Watermark HD video.
 */
@Composable
fun HeroDownloadButton(
    option: DownloadOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFF007AFF).copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF007AFF),
                        Color(0xFF00B4D8)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in Frosted Glass Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CupertinoIcons.Filled.ArrowDownCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Label & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TẢI VIDEO KHÔNG LOGO",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Độ phân giải 1080p HD • Không watermark",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }

            // Quality Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "1080p HD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Modern compact Bento tile for secondary download actions (MP3 Audio, SD, Watermarked, Slides).
 */
@Composable
fun SecondaryOptionTile(
    option: DownloadOption,
    cardBackground: Color,
    cardBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current

    val (icon, tintColor, bgTint, badgeText, shortTitle, shortDesc) = when (option.type) {
        DownloadFormatType.AUDIO_MP3 -> Hexuple(
            CupertinoIcons.Default.MusicNote,
            Color(0xFFF59E0B),
            if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7),
            "MP3",
            "Nhạc Âm Thanh",
            "Tách nhạc gốc"
        )
        DownloadFormatType.VIDEO_SD_NO_WATERMARK -> Hexuple(
            CupertinoIcons.Filled.Video,
            Color(0xFF3B82F6),
            if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
            "720p SD",
            "Video Chuẩn",
            "Dung lượng nhẹ"
        )
        DownloadFormatType.VIDEO_WATERMARK -> Hexuple(
            CupertinoIcons.Filled.Video,
            Color(0xFF64748B),
            if (isDark) Color(0xFF334155).copy(alpha = 0.35f) else Color(0xFFF1F5F9),
            "Có Logo",
            "Bản Gốc Watermark",
            "Kèm ID tác giả"
        )
        DownloadFormatType.PHOTO_SLIDE -> Hexuple(
            CupertinoIcons.Filled.PhotoStack,
            Color(0xFF8B5CF6),
            if (isDark) Color(0xFF4C1D95).copy(alpha = 0.35f) else Color(0xFFF5F3FF),
            "Ảnh HD",
            "Bộ Ảnh Slide",
            "Tải toàn bộ ảnh"
        )
        DownloadFormatType.VIDEO_HD_NO_WATERMARK -> Hexuple(
            CupertinoIcons.Filled.Film,
            Color(0xFF10B981),
            if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFD1FAE5),
            "1080p HD",
            "Bản HD Không Logo",
            "Chất lượng cao nhất"
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row: Category Icon + Format Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tintColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tintColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Content: Title and short description
            Text(
                text = shortTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = shortDesc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Legacy wrapper for individual option card if referenced elsewhere.
 */
@Composable
fun DownloadOptionCard(
    option: DownloadOption,
    cardBackground: Color,
    cardBorderColor: Color,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecondaryOptionTile(
        option = option,
        cardBackground = cardBackground,
        cardBorderColor = cardBorderColor,
        onClick = onDownloadClick,
        modifier = modifier
    )
}

/**
 * 6-element tuple helper for formatting parameters.
 */
private data class Hexuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)
