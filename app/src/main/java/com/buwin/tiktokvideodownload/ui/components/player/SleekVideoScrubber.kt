package com.buwin.tiktokvideodownload.ui.components.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Ultra-sleek minimalist timeline scrubber inspired by YouTube, Samsung, and Apple AVPlayer.
 * Features a razor-thin 3dp track expanding smoothly to 5dp on interaction, with an animated thumb dot.
 */
@Composable
fun SleekVideoScrubber(
    positionMs: Int,
    durationMs: Int,
    onSeek: (Int) -> Unit,
    onScrub: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.28f)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }

    val currentRatio = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayRatio = if (isDragging) dragRatio else currentRatio

    val trackHeightDp by animateDpAsState(
        targetValue = if (isDragging) 5.dp else 3.dp,
        label = "scrubberHeight"
    )
    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 7.dp else 4.dp,
        label = "thumbRadius"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(durationMs) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val newRatio = (offset.x / size.width).coerceIn(0f, 1f)
                        dragRatio = newRatio
                        val targetMs = (newRatio * durationMs).toInt()
                        onScrub?.invoke(targetMs)
                        val success = tryAwaitRelease()
                        isDragging = false
                        if (success) {
                            onSeek(targetMs)
                        }
                    }
                )
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragRatio = (offset.x / size.width).coerceIn(0f, 1f)
                        onScrub?.invoke((dragRatio * durationMs).toInt())
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragRatio = newRatio
                        onScrub?.invoke((newRatio * durationMs).toInt())
                    },
                    onDragEnd = {
                        isDragging = false
                        val targetMs = (dragRatio * durationMs).toInt()
                        onSeek(targetMs)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = trackHeightDp.toPx()
            val thumbRadius = thumbRadiusDp.toPx()
            val centerY = size.height / 2f
            val width = size.width

            val activeWidth = (width * displayRatio).coerceIn(0f, width)

            // Inactive Track (Background)
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = Size(width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
            )

            // Active Track (Played Progress)
            if (activeWidth > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(activeWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
                )
            }

            // Sleek Apple / Samsung Thumb Dot
            if (thumbRadius > 0f) {
                // Drop shadow / glow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = thumbRadius + 1.5.dp.toPx(),
                    center = Offset(activeWidth, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = Offset(activeWidth, centerY)
                )
            }
        }
    }
}
