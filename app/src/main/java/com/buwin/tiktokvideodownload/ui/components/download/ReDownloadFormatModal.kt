package com.buwin.tiktokvideodownload.ui.components.download

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buwin.tiktokvideodownload.data.config.AppConfig
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.buwin.tiktokvideodownload.ui.theme.OutlineDark
import com.buwin.tiktokvideodownload.ui.theme.OutlineLight
import com.buwin.tiktokvideodownload.ui.theme.SurfaceDark
import com.buwin.tiktokvideodownload.ui.theme.SurfaceLight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark

/**
 * Bottom Sheet Modal allowing users to choose a format (HD Video, SD, MP3)
 * when re-downloading a video whose local file was removed.
 */
@Composable
fun ReDownloadFormatModal(
    info: TikTokVideoInfo,
    onDismiss: () -> Unit,
    onDownloadOption: (DownloadOption) -> Unit,
    backdrop: Backdrop? = null,
    isDark: Boolean = LocalIsDark.current
) {
    BackHandler(onBack = onDismiss)

    val surfaceBg = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) OutlineDark else OutlineLight

    val primaryOption = info.options.firstOrNull { it.type == DownloadFormatType.VIDEO_HD_NO_WATERMARK }
        ?: info.options.firstOrNull { it.type == DownloadFormatType.VIDEO_SD_NO_WATERMARK }
        ?: info.options.firstOrNull()

    val secondaryOptions = info.options.filter { it != primaryOption }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) },
                            effects = {
                                vibrancy()
                                blur(16f.dp.toPx())
                                lens(16f.dp.toPx(), 20f.dp.toPx())
                            },
                            highlight = { Highlight.Plain },
                            shadow = {
                                Shadow(
                                    radius = 24f.dp,
                                    color = if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.2f)
                                )
                            },
                            onDrawSurface = {
                                drawRect(surfaceBg.copy(alpha = if (isDark) 0.88f else 0.94f))
                            }
                        )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                            .background(surfaceBg)
                    }
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag Handle Bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.15f))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Header Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tải lại video",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Chọn định dạng mong muốn để tải về máy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.Xmark,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video Summary Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF5F5F4)),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(info.coverUrl)
                                .crossfade(true)
                                .setHeader("Referer", if (info.coverUrl.contains("tikwm.com")) AppConfig.REFERER_TIKTOK else AppConfig.REFERER_FACEBOOK)
                                .setHeader("User-Agent", AppConfig.USER_AGENT_MOBILE)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.title.ifEmpty { "Video tải về" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (info.authorUsername.isNotEmpty()) "@${info.authorUsername}" else info.authorNickname,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Format Options Section
            if (primaryOption != null) {
                Text(
                    text = "Khuyên dùng",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HeroDownloadButton(
                    option = primaryOption,
                    onClick = {
                        onDownloadOption(primaryOption)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (secondaryOptions.isNotEmpty()) {
                Text(
                    text = "Định dạng khác",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    secondaryOptions.forEach { option ->
                        SecondaryOptionTile(
                            option = option,
                            cardBackground = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF),
                            cardBorderColor = borderColor,
                            onClick = {
                                onDownloadOption(option)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
