package com.buwin.tiktokvideodownload.ui.components.download

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.github.alexzhirkevich.cupertino.icons.outlined.Waveform
import io.github.alexzhirkevich.cupertino.icons.outlined._4kTv

/**
 * Hero prominent download CTA button for the primary No-Watermark HD video.
 * Styled with Apple VisionOS Liquid Glass aesthetics (cardBackground surface, subtle border, luminescent disc).
 */
@Composable
fun HeroDownloadButton(
    option: DownloadOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardBackground: Color = if (LocalIsDark.current) Color(0xFF1C1C1E) else Color.White,
    cardBorderColor: Color = if (LocalIsDark.current) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
) {
    val isDark = LocalIsDark.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_btn_scale"
    )

    val title = when (option.type) {
        DownloadFormatType.PHOTO_SLIDE -> "TẢI BỘ ẢNH GỐC"
        DownloadFormatType.AUDIO_MP3 -> "TẢI NHẠC GỐC MP3"
        DownloadFormatType.VIDEO_SD_NO_WATERMARK -> "TẢI VIDEO KHÔNG LOGO"
        DownloadFormatType.VIDEO_HD_NO_WATERMARK -> "TẢI VIDEO KHÔNG LOGO"
        DownloadFormatType.VIDEO_WATERMARK -> "TẢI VIDEO BẢN GỐC"
    }

    val subtitle = when (option.type) {
        DownloadFormatType.PHOTO_SLIDE -> "Độ phân giải cao • Đầy đủ toàn bộ slide"
        DownloadFormatType.AUDIO_MP3 -> "Âm thanh gốc 320kbps • Tách nhạc TikTok"
        DownloadFormatType.VIDEO_SD_NO_WATERMARK -> "Độ phân giải 720p SD • Không watermark"
        DownloadFormatType.VIDEO_HD_NO_WATERMARK -> "Độ phân giải 1080p HD • Không watermark"
        DownloadFormatType.VIDEO_WATERMARK -> "Có watermark • Kèm ID tác giả"
    }

    val badgeText = when (option.type) {
        DownloadFormatType.PHOTO_SLIDE -> "Ảnh HD"
        DownloadFormatType.AUDIO_MP3 -> "MP3"
        DownloadFormatType.VIDEO_SD_NO_WATERMARK -> "720p SD"
        DownloadFormatType.VIDEO_HD_NO_WATERMARK -> "1080p HD"
        DownloadFormatType.VIDEO_WATERMARK -> "Có Logo"
    }

    val accentColor = Color(0xFF007AFF)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Liquid Glass Disc with Luminescent Accent (Apple VisionOS style)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                                if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .border(
                        width = 0.75.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f),
                                accentColor.copy(alpha = 0.40f),
                                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CupertinoIcons.Filled.ArrowDownCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Label & Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Frosted Capsule Quality Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        accentColor.copy(alpha = if (isDark) 0.14f else 0.10f)
                    )
                    .border(
                        width = 0.75.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                if (isDark) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.60f),
                                accentColor.copy(alpha = if (isDark) 0.20f else 0.15f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) accentColor else accentColor.copy(alpha = 0.95f)
                )
            }
        }
    }
}

/**
 * Modern compact Bento tile for secondary download actions (MP3 Audio, SD, Watermarked, Slides).
 * Synchronized with Apple VisionOS Liquid Glass design system.
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tile_press_scale"
    )

    val visual = when (option.type) {
        DownloadFormatType.AUDIO_MP3 -> SecondaryOptionVisual(
            icon = CupertinoIcons.Outlined.Waveform,
            accentGlow = Color(0xFFFF375F), // Apple Neon Rose
            badgeText = "MP3",
            title = "Nhạc Âm Thanh",
            subtitle = "Tách nhạc gốc"
        )
        DownloadFormatType.VIDEO_SD_NO_WATERMARK -> SecondaryOptionVisual(
            icon = CupertinoIcons.Filled.Video,
            accentGlow = Color(0xFF00D2FF), // Apple Cyan
            badgeText = "720p SD",
            title = "Video Chuẩn",
            subtitle = "Dung lượng nhẹ"
        )
        DownloadFormatType.VIDEO_WATERMARK -> SecondaryOptionVisual(
            icon = CupertinoIcons.Filled.Film,
            accentGlow = Color(0xFF98989D), // Apple Slate Gray
            badgeText = "Có Logo",
            title = "Bản Gốc Watermark",
            subtitle = "Kèm ID tác giả"
        )
        DownloadFormatType.PHOTO_SLIDE -> SecondaryOptionVisual(
            icon = CupertinoIcons.Filled.PhotoStack,
            accentGlow = Color(0xFFBF5AF2), // Apple Purple
            badgeText = "Ảnh HD",
            title = "Bộ Ảnh Slide",
            subtitle = "Tải toàn bộ ảnh"
        )
        DownloadFormatType.VIDEO_HD_NO_WATERMARK -> SecondaryOptionVisual(
            icon = CupertinoIcons.Outlined._4kTv,
            accentGlow = Color(0xFF30D158), // Apple Mint Green
            badgeText = "1080p HD",
            title = "Bản HD Không Logo",
            subtitle = "Chất lượng cao nhất"
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            // Top Row: Liquid Glass Disc + Frosted Capsule Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Liquid Glass Disc with Luminescent Accent Glow (Apple VisionOS style)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    visual.accentGlow.copy(alpha = if (isDark) 0.22f else 0.14f),
                                    if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
                                )
                            )
                        )
                        .border(
                            width = 0.75.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    if (isDark) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f),
                                    visual.accentGlow.copy(alpha = 0.40f),
                                    if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.accentGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Frosted Capsule Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            visual.accentGlow.copy(alpha = if (isDark) 0.14f else 0.10f)
                        )
                        .border(
                            width = 0.75.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    if (isDark) visual.accentGlow.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.60f),
                                    visual.accentGlow.copy(alpha = if (isDark) 0.20f else 0.15f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = visual.badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) visual.accentGlow else visual.accentGlow.copy(alpha = 0.95f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Content: Title and short description
            Text(
                text = visual.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = visual.subtitle,
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
 * Visual configuration data for secondary format tiles.
 */
private data class SecondaryOptionVisual(
    val icon: ImageVector,
    val accentGlow: Color,
    val badgeText: String,
    val title: String,
    val subtitle: String
)
