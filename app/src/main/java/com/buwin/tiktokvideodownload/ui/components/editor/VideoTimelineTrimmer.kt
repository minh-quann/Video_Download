package com.buwin.tiktokvideodownload.ui.components.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Modern dual-handle video trimming timeline with active highlighted window,
 * drag handles, ruler markings, and synchronized playback playhead.
 */
@Composable
fun VideoTimelineTrimmer(
    totalDurationMs: Long,
    startMs: Long,
    endMs: Long,
    currentPositionMs: Long,
    onRangeChange: (newStartMs: Long, newEndMs: Long) -> Unit,
    onSeek: (seekMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    onDragStateChange: ((isDragging: Boolean) -> Unit)? = null,
    trimColor: Color = Color(0xFFFFD60A), // Vibrant Apple/CapCut editor yellow
    dimColor: Color = Color.Black.copy(alpha = 0.55f),
    handleWidthDp: Float = 18f,
    minDurationMs: Long = 1000L
) {
    if (totalDurationMs <= 0) return

    val currentStartMs by rememberUpdatedState(startMs)
    val currentEndMs by rememberUpdatedState(endMs)
    val currentTotalDurationMs by rememberUpdatedState(totalDurationMs)
    val onRangeChangeState by rememberUpdatedState(onRangeChange)
    val onSeekState by rememberUpdatedState(onSeek)
    val onDragStateChangeState by rememberUpdatedState(onDragStateChange)

    var dragTarget by remember { mutableStateOf<DragHandleType?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .systemGestureExclusion()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C1C1E))
            .pointerInput(Unit) {
                val touchTargetWidth = 48.dp.toPx()
                val handleWidthPx = handleWidthDp.dp.toPx()

                detectDragGestures(
                    onDragStart = { offset ->
                        val width = size.width
                        val dur = currentTotalDurationMs
                        if (width <= 0 || dur <= 0) return@detectDragGestures

                        val startRatio = (currentStartMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                        val endRatio = (currentEndMs.toFloat() / dur.toFloat()).coerceIn(startRatio, 1f)
                        val startX = width * startRatio
                        val endX = width * endRatio

                        val distToStart = abs(offset.x - startX)
                        val distToEnd = abs(offset.x - endX)

                        val target = when {
                            // Close to both handles -> pick closest
                            distToStart <= touchTargetWidth && distToEnd <= touchTargetWidth -> {
                                if (distToStart <= distToEnd) DragHandleType.START else DragHandleType.END
                            }
                            // Near left handle or to the left of it
                            offset.x <= startX + touchTargetWidth -> DragHandleType.START
                            // Near right handle or to the right of it
                            offset.x >= endX - touchTargetWidth -> DragHandleType.END
                            // In between handles -> scrub playhead
                            offset.x in startX..endX -> {
                                val ratio = (offset.x / width).coerceIn(startRatio, endRatio)
                                onSeekState((ratio * dur).toLong())
                                DragHandleType.PLAYHEAD
                            }
                            distToStart <= distToEnd -> DragHandleType.START
                            else -> DragHandleType.END
                        }

                        dragTarget = target
                        onDragStateChangeState?.invoke(true)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val width = size.width
                        val dur = currentTotalDurationMs
                        if (width <= 0 || dur <= 0) return@detectDragGestures

                        val deltaRatio = dragAmount.x / width
                        val startRatio = currentStartMs.toFloat() / dur.toFloat()
                        val endRatio = currentEndMs.toFloat() / dur.toFloat()
                        val minRatio = (minDurationMs.toFloat() / dur.toFloat()).coerceIn(0.005f, 0.95f)

                        when (dragTarget) {
                            DragHandleType.START -> {
                                val newStartRatio = (startRatio + deltaRatio).coerceIn(0f, endRatio - minRatio)
                                val newStartMs = (newStartRatio * dur).toLong()
                                onRangeChangeState(newStartMs, currentEndMs)
                                onSeekState(newStartMs)
                            }
                            DragHandleType.END -> {
                                val newEndRatio = (endRatio + deltaRatio).coerceIn(startRatio + minRatio, 1f)
                                val newEndMs = (newEndRatio * dur).toLong()
                                onRangeChangeState(currentStartMs, newEndMs)
                                onSeekState(newEndMs)
                            }
                            DragHandleType.PLAYHEAD -> {
                                val currentRatio = (change.position.x / width).coerceIn(startRatio, endRatio)
                                onSeekState((currentRatio * dur).toLong())
                            }
                            null -> {}
                        }
                    },
                    onDragEnd = {
                        dragTarget = null
                        onDragStateChangeState?.invoke(false)
                    },
                    onDragCancel = {
                        dragTarget = null
                        onDragStateChangeState?.invoke(false)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val dur = totalDurationMs
            if (width <= 0 || dur <= 0) return@Canvas

            val startRatio = (startMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            val endRatio = (endMs.toFloat() / dur.toFloat()).coerceIn(startRatio, 1f)
            val playheadRatio = (currentPositionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)

            val startX = width * startRatio
            val endX = width * endRatio
            val playheadX = width * playheadRatio
            val handleWidthPx = handleWidthDp.dp.toPx()

            // 1. Filmstrip background tick markings
            val tickCount = 20
            for (i in 0..tickCount) {
                val tx = (width / tickCount) * i
                val isMajor = i % 4 == 0
                val tickHeight = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = if (isMajor) 0.28f else 0.12f),
                    start = Offset(tx, 0f),
                    end = Offset(tx, tickHeight),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = if (isMajor) 0.28f else 0.12f),
                    start = Offset(tx, height - tickHeight),
                    end = Offset(tx, height),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. Dimmed outside regions
            if (startX > 0f) {
                drawRect(
                    color = dimColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(startX, height)
                )
            }
            if (endX < width) {
                drawRect(
                    color = dimColor,
                    topLeft = Offset(endX, 0f),
                    size = Size(width - endX, height)
                )
            }

            // 3. Highlighted active box border (top and bottom yellow bars)
            val borderThick = 3.dp.toPx()
            val innerStartX = (startX + handleWidthPx).coerceAtMost(endX)
            val innerEndX = (endX - handleWidthPx).coerceAtLeast(startX)
            if (innerEndX > innerStartX) {
                drawLine(
                    color = trimColor,
                    start = Offset(innerStartX, borderThick / 2),
                    end = Offset(innerEndX, borderThick / 2),
                    strokeWidth = borderThick
                )
                drawLine(
                    color = trimColor,
                    start = Offset(innerStartX, height - borderThick / 2),
                    end = Offset(innerEndX, height - borderThick / 2),
                    strokeWidth = borderThick
                )
            }

            // 4. Start Handle (Left) - perfectly flush inside [startX, startX + handleWidthPx]
            drawRoundRect(
                color = trimColor,
                topLeft = Offset(startX, 0f),
                size = Size(handleWidthPx, height),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            // Left grip dots (3 vertical black dots)
            val gripDotR = 1.3.dp.toPx()
            val midY = height / 2f
            val startHandleCenterX = startX + handleWidthPx / 2f
            drawCircle(Color.Black.copy(alpha = 0.65f), gripDotR, Offset(startHandleCenterX, midY - 7.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.65f), gripDotR, Offset(startHandleCenterX, midY))
            drawCircle(Color.Black.copy(alpha = 0.65f), gripDotR, Offset(startHandleCenterX, midY + 7.dp.toPx()))

            // 5. End Handle (Right) - perfectly flush inside [endX - handleWidthPx, endX]
            drawRoundRect(
                color = trimColor,
                topLeft = Offset(endX - handleWidthPx, 0f),
                size = Size(handleWidthPx, height),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            // Right grip dots (3 vertical black dots)
            val endHandleCenterX = endX - handleWidthPx / 2f
            drawCircle(Color.Black.copy(alpha = 0.65f), gripDotR, Offset(endHandleCenterX, midY - 7.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.65f), gripDotR, Offset(endHandleCenterX, midY))
            drawCircle(Color.Black.copy(alpha = 0.65f), gripDotR, Offset(endHandleCenterX, midY + 7.dp.toPx()))

            // 6. Playhead Line
            if (playheadX in startX..endX) {
                // Playhead shadow
                drawLine(
                    color = Color.Black.copy(alpha = 0.5f),
                    start = Offset(playheadX + 1.dp.toPx(), 0f),
                    end = Offset(playheadX + 1.dp.toPx(), height),
                    strokeWidth = 2.dp.toPx()
                )
                // Playhead white needle
                drawLine(
                    color = Color.White,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, height),
                    strokeWidth = 2.dp.toPx()
                )
                // Needle cap
                drawCircle(
                    color = Color.White,
                    radius = 3.5.dp.toPx(),
                    center = Offset(playheadX, 5.dp.toPx())
                )
            }
        }
    }
}

private enum class DragHandleType {
    START, END, PLAYHEAD
}
