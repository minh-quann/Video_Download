package com.buwin.tiktokvideodownload.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.Backdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import io.github.alexzhirkevich.cupertino.icons.outlined.InfoCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.SquareAndArrowUp

/**
 * Clean, translucent gallery top bar featuring Apple / Liquid Glass buttons.
 * Keeps media uncluttered without invasive titles (details reveal on swipe-up or info tap).
 */
@Composable
fun PlayerTopBar(
    backdrop: Backdrop,
    isMuted: Boolean,
    isLandscape: Boolean,
    onToggleMute: () -> Unit,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpenDetails: (() -> Unit)? = null,
    onOpenEditor: (() -> Unit)? = null,
    showMute: Boolean = true,
    isImage: Boolean = false,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDark) Color.White else Color(0xFF1C1C1E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.65f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(horizontal = if (isLandscape) 28.dp else 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button - Authentic Liquid Glass round button matching HistoryScreen
            LiquidRoundButton(
                onClick = onDismiss,
                backdrop = backdrop,
                size = 40.dp,
                isDark = isDark
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right action buttons (Mute, Edit, Info, Share) - Hidden when isImage as they are in the bottom bar
            if (!isImage) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mute toggle (videos only)
                    if (showMute) {
                        LiquidRoundButton(
                            onClick = onToggleMute,
                            backdrop = backdrop,
                            size = 40.dp,
                            isDark = isDark
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Bật âm" else "Tắt âm",
                                tint = if (isMuted) Color(0xFFEF4444) else contentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Video Editor (Cut/Trim/Filter)
                    if (onOpenEditor != null) {
                        LiquidRoundButton(
                            onClick = onOpenEditor,
                            backdrop = backdrop,
                            size = 40.dp,
                            isDark = isDark
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCut,
                                contentDescription = "Chỉnh sửa video",
                                tint = contentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Info / Metadata button (reveals file name and details like Apple/Samsung)
                    if (onOpenDetails != null) {
                        LiquidRoundButton(
                            onClick = onOpenDetails,
                            backdrop = backdrop,
                            size = 40.dp,
                            isDark = isDark
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.InfoCircle,
                                contentDescription = "Thông tin chi tiết",
                                tint = contentColor,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    // Share button - Apple SF Symbols SquareAndArrowUp
                    LiquidRoundButton(
                        onClick = onShare,
                        backdrop = backdrop,
                        size = 40.dp,
                        isDark = isDark
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.SquareAndArrowUp,
                            contentDescription = "Chia sẻ",
                            tint = contentColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
