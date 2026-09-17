package com.buwin.tiktokvideodownload.ui.components.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Apple iOS AVPlayer & Photos style minimalist video scrubber.
 * Features a seamless 3.5dp pill track expanding smoothly to 6.5dp during scrubbing,
 * with a dynamic haptic thumb bloom that appears only when interacting.
 * Uses a unified awaitEachGesture handler to prevent tap/drag race conditions and thumb jumps.
 */
@Composable
fun SleekVideoScrubber(
    positionMs: Int,
    durationMs: Int,
    onSeek: (Int) -> Unit,
    onScrub: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.24f)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(0f) }

    val currentRatio = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val displayRatio = if (isDragging) dragRatio else currentRatio

    val trackHeightDp by animateDpAsState(
        targetValue = if (isDragging) 6.5.dp else 3.5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scrubberHeight"
    )

    // Apple-style thumb bloom: hidden during normal playback, reveals on touch
    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 6.5.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbRadius"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(durationMs) {
                if (durationMs <= 0) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isDragging = true
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    var currentProgress = (down.position.x / width).coerceIn(0f, 1f)
                    dragRatio = currentProgress
                    onScrub?.invoke((currentProgress * durationMs).toInt())

                    val pointerId = down.id
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                change.consume()
                                break
                            }
                            change.consume()
                            currentProgress = (change.position.x / width).coerceIn(0f, 1f)
                            dragRatio = currentProgress
                            onScrub?.invoke((currentProgress * durationMs).toInt())
                        }
                        val finalTargetMs = (dragRatio * durationMs).toInt()
                        onSeek(finalTargetMs)
                    } finally {
                        isDragging = false
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = trackHeightDp.toPx()
            val thumbRadius = thumbRadiusDp.toPx()
            val centerY = size.height / 2f
            val width = size.width

            val activeWidth = (width * displayRatio).coerceIn(0f, width)

            // Inactive Track (Translucent frosted glass white)
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - trackHeight / 2),
                size = Size(width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
            )

            // Active Track (Pure Apple White Progress)
            if (activeWidth > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2),
                    size = Size(activeWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
                )
            }

            // Apple Interactive Thumb Dot (Soft drop shadow + specular core)
            if (thumbRadius > 0.5f) {
                // Soft shadow for depth
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = thumbRadius + 1.5.dp.toPx(),
                    center = Offset(activeWidth, centerY)
                )
                // Crisp white thumb core
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = Offset(activeWidth, centerY)
                )
            }
        }
    }
}
