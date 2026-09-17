package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 3D Cylindrical Scrubber Dial.
 * Features:
 * - 3D cylindrical perspective projection: tick marks curve around a virtual roller wheel.
 * - Magnetic snap detent at 0 with tactile feedback.
 * - Tactile tick haptic clicks on step crossings.
 * - Center needle with triangle pointer.
 */
@Composable
fun EditorScrubberDial(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticStep by remember { mutableIntStateOf(value.roundToInt()) }
    var rawAccumulator by remember(value) { mutableFloatStateOf(value) }

    ComposeCanvas(
        modifier = modifier
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Sensitivity: 1px = 0.38 units for fine precision
                        val delta = -dragAmount.x * 0.38f
                        var candidate = (rawAccumulator + delta).coerceIn(valueRange)

                        // Magnetic Snap around 0
                        if (abs(candidate) < 1.4f) {
                            candidate = 0f
                        }
                        rawAccumulator = candidate

                        val candidateInt = candidate.roundToInt()
                        if (candidateInt != lastHapticStep) {
                            if (candidateInt == 0) {
                                // Deeper haptic click at 0
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else if (candidateInt % 5 == 0) {
                                // Tick haptic every 5 units
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            lastHapticStep = candidateInt
                        }

                        onValueChange(candidate)
                    },
                    onDragEnd = {
                        // Settle snap if very close to integer
                        val rounded = (rawAccumulator).roundToInt().toFloat()
                        if (abs(rawAccumulator - rounded) < 0.35f) {
                            onValueChange(rounded)
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val tickSpacing = 11.dp.toPx()
        val yellowAccent = Color(0xFFFFD60A)

        // Virtual 3D Cylinder radius
        val radius = width * 0.52f

        // Draw tick lines around virtual cylinder
        for (i in -50..50) {
            val tickVal = i * 2.5f
            val deltaFromCurrent = tickVal - value
            val linearOffset = deltaFromCurrent * (tickSpacing / 2.5f)

            // Cylindrical angle theta
            val theta = linearOffset / radius
            if (theta in -1.52f..1.52f) {
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)

                // 3D projected screen X
                val xPos = centerX + radius * sinTheta

                // Depth scaling and perspective falloff
                val depthScale = (cosTheta * cosTheta).coerceIn(0f, 1f)
                val isZero = i == 0
                val isMajor = i % 10 == 0
                val isMedium = i % 5 == 0

                val baseHeight = when {
                    isMajor -> 22.dp.toPx()
                    isMedium -> 15.dp.toPx()
                    else -> 9.dp.toPx()
                }
                val tickHeight = baseHeight * (0.35f + 0.65f * depthScale)

                val baseAlpha = when {
                    isZero -> 0.95f
                    isMajor -> 0.70f
                    isMedium -> 0.45f
                    else -> 0.22f
                }
                val alpha = (baseAlpha * depthScale * depthScale).coerceIn(0f, 1f)

                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(xPos, centerY - tickHeight / 2f),
                    end = Offset(xPos, centerY + tickHeight / 2f),
                    strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.2.dp.toPx()
                )
            }
        }

        // Center stationary reference needle
        val centerLineHeight = 26.dp.toPx()
        val isNonZero = abs(value) > 0.05f
        drawLine(
            color = if (isNonZero) yellowAccent else Color.White,
            start = Offset(centerX, centerY - centerLineHeight / 2f),
            end = Offset(centerX, centerY + centerLineHeight / 2f),
            strokeWidth = 2.dp.toPx()
        )

        // Yellow triangle pointer on top center
        val trianglePath = Path().apply {
            moveTo(centerX - 4.dp.toPx(), centerY - centerLineHeight / 2f - 4.dp.toPx())
            lineTo(centerX + 4.dp.toPx(), centerY - centerLineHeight / 2f - 4.dp.toPx())
            lineTo(centerX, centerY - centerLineHeight / 2f)
            close()
        }
        drawPath(trianglePath, color = yellowAccent)
    }
}
