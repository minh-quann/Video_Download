package com.buwin.tiktokvideodownload.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.Backdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.SpeakerSlash
import io.github.alexzhirkevich.cupertino.icons.filled.SpeakerWave2
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowDownRightAndArrowUpLeft
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowUpLeftAndArrowDownRight

import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark

/**
 * Bottom playback control bar designed to iOS AVPlayer / Photos standards.
 * Features a dynamic scrubber, tabular time indicator, and glass action buttons.
 */
@Composable
fun PlayerBottomBar(
    backdrop: Backdrop,
    currentPositionMs: Int,
    totalDurationMs: Int,
    isAudio: Boolean,
    isMuted: Boolean,
    isLandscape: Boolean,
    onSeek: (Int) -> Unit,
    onToggleMute: () -> Unit,
    onToggleOrientation: () -> Unit,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDark) Color.White else Color.Black
    var isDraggingSlider by remember { mutableStateOf(false) }
    var scrubbedPositionMs by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.20f),
                        Color.Black.copy(alpha = 0.60f),
                        Color.Black.copy(alpha = 0.88f)
                    )
                )
            )
            .navigationBarsPadding()
            .displayCutoutPadding()
            .padding(
                horizontal = if (isLandscape) 32.dp else 20.dp,
                vertical = 12.dp
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Apple AVPlayer Scrubber (Pure White active, frosted white inactive)
            SleekVideoScrubber(
                positionMs = if (isDraggingSlider) scrubbedPositionMs else currentPositionMs,
                durationMs = totalDurationMs,
                onScrub = { scrubPos ->
                    isDraggingSlider = true
                    scrubbedPositionMs = scrubPos
                },
                onSeek = { targetMs ->
                    scrubbedPositionMs = targetMs
                    isDraggingSlider = false
                    onSeek(targetMs)
                },
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.24f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Duration and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time position / total duration (Apple SF Pro typography)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatDuration(if (isDraggingSlider) scrubbedPositionMs else currentPositionMs),
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = "/",
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatDuration(totalDurationMs),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.2.sp
                    )
                }

                // Apple SF Symbol Action Icons: Mute & Orientation Fullscreen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mute / Speaker Toggle Disc (Shared LiquidRoundButton with Kyant backdrop blur and no border)
                    LiquidRoundButton(
                        onClick = onToggleMute,
                        backdrop = backdrop,
                        size = 36.dp,
                        showBorder = false,
                        isDark = isDark
                    ) {
                        Icon(
                            imageVector = if (isMuted) CupertinoIcons.Filled.SpeakerSlash else CupertinoIcons.Filled.SpeakerWave2,
                            contentDescription = if (isMuted) "Bật âm thanh" else "Tắt tiếng",
                            tint = if (isMuted) Color(0xFFFF453A) else contentColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Rotate / Fullscreen Orientation Toggle Disc (Video only)
                    if (!isAudio) {
                        LiquidRoundButton(
                            onClick = onToggleOrientation,
                            backdrop = backdrop,
                            size = 36.dp,
                            showBorder = false,
                            isDark = isDark
                        ) {
                            Icon(
                                imageVector = if (isLandscape) CupertinoIcons.Outlined.ArrowDownRightAndArrowUpLeft
                                       else CupertinoIcons.Outlined.ArrowUpLeftAndArrowDownRight,
                                contentDescription = if (isLandscape) "Thu nhỏ" else "Toàn màn hình",
                                tint = contentColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
