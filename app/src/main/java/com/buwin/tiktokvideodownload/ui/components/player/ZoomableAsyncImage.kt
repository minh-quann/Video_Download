package com.buwin.tiktokvideodownload.ui.components.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Zoomable Image Viewer styled after Apple Photos and Samsung Gallery.
 * Supports:
 * - Fluid Shared Element / Hero Transition expanding from & shrinking to thumbnail bounds.
 * - Interactive pull-down gesture to dismiss with real-time target tracking.
 * - Double tap to zoom in (2.5x) and zoom out (1x) with fluid spring animation.
 * - Pinch to zoom (1x to 5x) with bounce-back spring.
 * - Free panning when zoomed in with boundary constraints.
 * - Single tap to toggle UI controls.
 * - Swipe up when not zoomed to open media details bottom sheet.
 */
@Composable
fun ZoomableAsyncImage(
    model: Any?,
    contentDescription: String?,
    onSingleTap: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    thumbnailBounds: Rect? = null,
    isExiting: Boolean = false,
    onDismissProgress: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = remember(configuration, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }

    val screenCenterX = screenWidthPx / 2f
    val screenCenterY = screenHeightPx / 2f

    // Calculate exact target offset and scale to match thumbnail on grid
    val (targetOffset, targetScale) = remember(thumbnailBounds, screenWidthPx, screenHeightPx) {
        if (thumbnailBounds != null && screenWidthPx > 0f && screenHeightPx > 0f) {
            val off = Offset(
                x = thumbnailBounds.center.x - screenCenterX,
                y = thumbnailBounds.center.y - screenCenterY
            )
            val sc = (thumbnailBounds.width / screenWidthPx).coerceIn(0.12f, 0.95f)
            Pair(off, sc)
        } else {
            Pair(Offset(0f, screenHeightPx * 0.45f), 0.35f)
        }
    }

    val initialScale = if (thumbnailBounds != null) targetScale else 0.88f
    val initialOffset = if (thumbnailBounds != null) targetOffset else Offset.Zero
    val initialCorner = if (thumbnailBounds != null) 3f else 0f

    val scale = remember { Animatable(initialScale) }
    val offset = remember { Animatable(initialOffset, Offset.VectorConverter) }
    val cornerRadiusAnim = remember { Animatable(initialCorner) }
    val dismissAnimProgress = remember { Animatable(0f) }
    val imageAlpha = remember { Animatable(1f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var isDismissing by remember { mutableStateOf(false) }

    // Hero entrance transition: expand smoothly from thumbnail bounds to full screen
    LaunchedEffect(Unit) {
        launch { scale.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 380f)) }
        launch { offset.animateTo(Offset.Zero, spring(dampingRatio = 0.82f, stiffness = 380f)) }
        launch { cornerRadiusAnim.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 380f)) }
    }

    // Triggered dismiss transition when user presses Back button / TopBar close
    LaunchedEffect(isExiting) {
        if (isExiting && !isDismissing) {
            isDismissing = true
            launch {
                offset.animateTo(targetOffset, spring(dampingRatio = 0.82f, stiffness = 380f))
            }
            launch {
                scale.animateTo(targetScale, spring(dampingRatio = 0.82f, stiffness = 380f))
            }
            launch {
                cornerRadiusAnim.animateTo(3f, spring(dampingRatio = 0.82f, stiffness = 380f))
            }
            launch {
                dismissAnimProgress.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 380f)) {
                    onDismissProgress(value)
                }
            }
            launch {
                imageAlpha.animateTo(0f, tween(180, delayMillis = 60))
            }
            delay(220)
            onSwipeDown()
        }
    }

    fun clampOffset(proposedOffset: Offset, currentScale: Float): Offset {
        if (currentScale <= 1f || containerSize == IntSize.Zero) return Offset.Zero
        val maxBoundX = (containerSize.width * (currentScale - 1f)) / 2f
        val maxBoundY = (containerSize.height * (currentScale - 1f)) / 2f
        return Offset(
            proposedOffset.x.coerceIn(-maxBoundX, maxBoundX),
            proposedOffset.y.coerceIn(-maxBoundY, maxBoundY)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(containerSize) {
                var lastTapTime = 0L
                var lastTapPos = Offset.Zero

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val currentTime = System.currentTimeMillis()
                    var isDoubleTap = false

                    // Double Tap detection (within 320ms and within 90px)
                    if (currentTime - lastTapTime < 320L && (downPos - lastTapPos).getDistance() < 90f) {
                        isDoubleTap = true
                        lastTapTime = 0L
                    } else {
                        lastTapTime = currentTime
                        lastTapPos = downPos
                    }

                    if (isDoubleTap) {
                        down.consume()
                        coroutineScope.launch {
                            if (scale.value > 1.2f) {
                                // Zoom out to 1x
                                launch { scale.animateTo(1f, spring(0.78f, 380f)) }
                                launch { offset.animateTo(Offset.Zero, spring(0.78f, 380f)) }
                            } else {
                                // Zoom in 2.5x centered around tap point
                                val targetScaleMultiplier = 2.5f
                                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                val targetOff = clampOffset((center - downPos) * (targetScaleMultiplier - 1f), targetScaleMultiplier)
                                launch { scale.animateTo(targetScaleMultiplier, spring(0.78f, 380f)) }
                                launch { offset.animateTo(targetOff, spring(0.78f, 380f)) }
                            }
                        }
                        return@awaitEachGesture
                    }

                    // Multi-touch pinch & single-touch pan tracking
                    var hasMoved = false
                    var totalDragY = 0f
                    var totalDragX = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val activeChanges = event.changes.filter { it.pressed }
                        if (activeChanges.isEmpty()) break

                        val pointerCount = activeChanges.size

                        if (pointerCount >= 2) {
                            // Pinch to zoom and 2-finger pan
                            hasMoved = true
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            activeChanges.forEach { it.consume() }

                            coroutineScope.launch {
                                val newScale = (scale.value * zoomChange).coerceIn(0.75f, 5f)
                                scale.snapTo(newScale)
                                val newOffset = clampOffset(offset.value + panChange, newScale)
                                offset.snapTo(newOffset)
                            }
                        } else if (pointerCount == 1) {
                            val change = activeChanges.first()
                            val dragAmount = change.position - change.previousPosition
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (scale.value > 1.05f) {
                                // Pan image when zoomed in
                                if (dragAmount.getDistance() > 1.2f) {
                                    hasMoved = true
                                    change.consume()
                                    coroutineScope.launch {
                                        val newOffset = clampOffset(offset.value + dragAmount, scale.value)
                                        offset.snapTo(newOffset)
                                    }
                                }
                            } else {
                                // Image at 1x: track swipe distance
                                if (abs(totalDragY) > 10f || abs(totalDragX) > 10f) {
                                    hasMoved = true
                                }
                                // Pull down gesture on 1x: fluid interactive drag-to-dismiss tracking
                                if (totalDragY > 0f && !isDismissing) {
                                    change.consume()
                                    val progress = (totalDragY / (screenHeightPx * 0.45f)).coerceIn(0f, 1f)
                                    val dragScale = (1f - progress * 0.25f).coerceAtLeast(0.35f)
                                    val dragCorner = progress * 16f
                                    coroutineScope.launch {
                                        offset.snapTo(Offset(totalDragX * 0.5f, totalDragY))
                                        scale.snapTo(dragScale)
                                        cornerRadiusAnim.snapTo(dragCorner)
                                        dismissAnimProgress.snapTo(progress)
                                        onDismissProgress(progress)
                                    }
                                }
                            }
                        }
                    }

                    // Gesture completion handling
                    if (!hasMoved) {
                        // Single tap: toggle UI header controls
                        coroutineScope.launch {
                            delay(220)
                            if (System.currentTimeMillis() - lastTapTime >= 220L) {
                                onSingleTap()
                            }
                        }
                    } else if (isDismissing) {
                        // Dismiss already active, ignore
                    } else {
                        // Bounce back to 1x if pinched below 1x
                        if (scale.value < 1f && totalDragY <= 0f) {
                            coroutineScope.launch {
                                launch { scale.animateTo(1f, spring(0.75f, 400f)) }
                                launch { offset.animateTo(Offset.Zero, spring(0.75f, 400f)) }
                            }
                        } else if (scale.value <= 1.05f && totalDragY < -35f && abs(totalDragY) > abs(totalDragX) * 1.5f) {
                            // Upward swipe on 1x image: open details bottom sheet
                            coroutineScope.launch {
                                launch { offset.animateTo(Offset.Zero, spring(0.8f, 400f)) }
                                launch { scale.animateTo(1f, spring(0.8f, 400f)) }
                                launch { cornerRadiusAnim.animateTo(0f, spring(0.8f, 400f)) }
                                launch { dismissAnimProgress.animateTo(0f, spring(0.8f, 400f)) }
                                onDismissProgress(0f)
                            }
                            onSwipeUp()
                        } else if (scale.value <= 1.05f && (totalDragY > 45f || dismissAnimProgress.value > 0.12f) && abs(totalDragY) > abs(totalDragX) * 0.9f) {
                            // Downward swipe: fluid dismiss shrinking back into thumbnail bounds
                            isDismissing = true
                            coroutineScope.launch {
                                launch {
                                    offset.animateTo(
                                        targetOffset,
                                        spring(dampingRatio = 0.82f, stiffness = 380f)
                                    )
                                }
                                launch {
                                    scale.animateTo(
                                        targetScale,
                                        spring(dampingRatio = 0.82f, stiffness = 380f)
                                    )
                                }
                                launch {
                                    cornerRadiusAnim.animateTo(
                                        3f,
                                        spring(dampingRatio = 0.82f, stiffness = 380f)
                                    )
                                }
                                launch {
                                    dismissAnimProgress.animateTo(
                                        1f,
                                        spring(dampingRatio = 0.82f, stiffness = 380f)
                                    ) {
                                        onDismissProgress(value)
                                    }
                                }
                                launch {
                                    imageAlpha.animateTo(0f, tween(180, delayMillis = 60))
                                }
                                delay(220)
                                onSwipeDown()
                            }
                        } else {
                            // Reset position with smooth spring if drag threshold wasn't met
                            coroutineScope.launch {
                                launch { offset.animateTo(Offset.Zero, spring(0.8f, 380f)) }
                                launch { scale.animateTo(1f, spring(0.8f, 380f)) }
                                launch { cornerRadiusAnim.animateTo(0f, spring(0.8f, 380f)) }
                                launch {
                                    dismissAnimProgress.animateTo(0f, spring(0.8f, 380f)) {
                                        onDismissProgress(value)
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationX = offset.value.x
                    translationY = offset.value.y
                    alpha = imageAlpha.value
                }
                .clip(RoundedCornerShape(cornerRadiusAnim.value.coerceAtLeast(0f).dp)),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        )
    }
}
