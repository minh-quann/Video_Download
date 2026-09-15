package com.buwin.tiktokvideodownload.ui.components.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class PlayerTouchMode {
    NONE, BRIGHTNESS, VOLUME, SEEK
}

/**
 * Transparent touch surface handling swipe gestures for volume, brightness,
 * horizontal video scrubbing, and YouTube-style 5s double tap seeking.
 */
@Composable
fun PlayerGestureSurface(
    totalDurationMs: Int,
    currentPositionMs: Int,
    enabled: Boolean,
    onBrightnessDelta: (Float) -> Unit,
    onBrightnessEnd: () -> Unit,
    onVolumeDelta: (Float) -> Unit,
    onVolumeEnd: () -> Unit,
    onSeekScrubStart: (Int) -> Unit,
    onSeekScrubDelta: (Float, Int) -> Unit,
    onSeekScrubEnd: () -> Unit,
    onDoubleTapRewind: () -> Unit,
    onDoubleTapForward: () -> Unit,
    onSingleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val coroutineScope = rememberCoroutineScope()
    var singleTapJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(totalDurationMs, enabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    var hasMoved = false
                    var currentMode = PlayerTouchMode.NONE
                    var accX = 0f
                    var accY = 0f
                    val screenWidth = size.width.toFloat()
                    val screenHeight = size.height.toFloat()
                    val isRightHalf = downPos.x > (screenWidth / 2f)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            break
                        }

                        val dx = change.position.x - change.previousPosition.x
                        val dy = change.position.y - change.previousPosition.y
                        accX += dx
                        accY += dy

                        if (!hasMoved) {
                            val dist = (change.position - downPos).getDistance()
                            if (dist > viewConfiguration.touchSlop) {
                                hasMoved = true
                                singleTapJob?.cancel()
                                if (abs(accX) > abs(accY)) {
                                    currentMode = PlayerTouchMode.SEEK
                                    onSeekScrubStart(currentPositionMs)
                                } else {
                                    currentMode = if (isRightHalf) PlayerTouchMode.VOLUME else PlayerTouchMode.BRIGHTNESS
                                }
                            }
                        }

                        if (hasMoved) {
                            change.consume()
                            when (currentMode) {
                                PlayerTouchMode.BRIGHTNESS -> {
                                    val step = -dy / (screenHeight * 0.75f)
                                    onBrightnessDelta(step)
                                }
                                PlayerTouchMode.VOLUME -> {
                                    val step = -dy / (screenHeight * 0.75f)
                                    onVolumeDelta(step)
                                }
                                PlayerTouchMode.SEEK -> {
                                    onSeekScrubDelta(accX, screenWidth.toInt())
                                }
                                PlayerTouchMode.NONE -> {}
                            }
                        }
                    }

                    if (hasMoved) {
                        when (currentMode) {
                            PlayerTouchMode.SEEK -> onSeekScrubEnd()
                            PlayerTouchMode.BRIGHTNESS -> onBrightnessEnd()
                            PlayerTouchMode.VOLUME -> onVolumeEnd()
                            PlayerTouchMode.NONE -> {}
                        }
                    } else {
                        val upTime = System.currentTimeMillis()
                        val timeDiff = upTime - lastTapTimestamp
                        val distDiff = (downPos - lastTapPosition).getDistance()

                        if (timeDiff < 320 && distDiff < 120f) {
                            lastTapTimestamp = 0L
                            singleTapJob?.cancel()

                            if (downPos.x < screenWidth * 0.42f) {
                                onDoubleTapRewind()
                            } else if (downPos.x > screenWidth * 0.58f) {
                                onDoubleTapForward()
                            } else {
                                onSingleTap()
                            }
                        } else {
                            lastTapTimestamp = upTime
                            lastTapPosition = downPos
                            singleTapJob?.cancel()
                            singleTapJob = coroutineScope.launch {
                                delay(260)
                                onSingleTap()
                            }
                        }
                    }
                }
            }
    )
}
