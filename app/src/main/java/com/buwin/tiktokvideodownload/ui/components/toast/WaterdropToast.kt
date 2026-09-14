package com.buwin.tiktokvideodownload.ui.components.toast

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.ArrowDownCircle
import io.github.alexzhirkevich.cupertino.icons.filled.CheckmarkCircle
import io.github.alexzhirkevich.cupertino.icons.filled.XmarkCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CIRCLE_SIZE = 48.dp

/**
 * 100% Faithful Replica of MEBIECO Mobile's WaterdropToast.
 *
 * Implements the signature 2-Phase Waterdrop Droplet & Morphing Swell animation:
 * - Phase 1: Pure 48x48 circular waterdrop droplet drops down from notch/status bar with spring physics.
 * - Phase 2: Overlapping liquid swell expanding horizontally outward from 48px circle to full width pill.
 * - Organic Exit: Width contracts horizontally back to 48px circle, then droplet lifts back up into notch.
 * - Swipe-Up Gesture: Natural drag momentum physics with swipe-up to dismiss.
 * - In-place updates: When already visible, smoothly updates type, texts, and live progress bar without re-dropping.
 */
@Composable
fun WaterdropToast(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val toastState by AppToast.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    val screenWidth = configuration.screenWidthDp.dp
    val fullWidth = screenWidth - 32.dp

    // Animation progress values matching MEBIECO Reanimated shared values
    val dropProgress = remember { Animatable(0f) }   // Phase 1: 0 (at -60dp) -> 1 (at 0dp)
    val expandProgress = remember { Animatable(0f) } // Phase 2: 0 (48dp circle) -> 1 (fullWidth pill)
    val dragY = remember { Animatable(0f) }          // Swipe-up drag offset

    var isMounted by remember { mutableStateOf(false) }
    var currentDisplayState by remember { mutableStateOf(toastState) }

    // Trigger haptic feedback for success or error
    fun triggerHaptics(type: ToastType) {
        when (type) {
            ToastType.SUCCESS, ToastType.ERROR -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            else -> {}
        }
    }

    // Function to execute the organic 2-phase liquid overlap exit
    fun startExitAnimation(onFinished: () -> Unit = {}) {
        coroutineScope.launch {
            // 1. Width contracts horizontally back to 48px circle (180ms)
            launch {
                expandProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                )
            }
            // 2. With 40ms organic delay, waterdrop lifts back UP into top notch (180ms)
            delay(40)
            dropProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
            )

            isMounted = false
            AppToast.hide()
            onFinished()
        }
    }

    // Main orchestration effect responding to state changes
    LaunchedEffect(toastState.isVisible, toastState.timestamp) {
        if (toastState.isVisible) {
            currentDisplayState = toastState

            if (isMounted && dropProgress.value > 0.5f) {
                // Toast is ALREADY visible: smoothly update content without re-running drop animation
                triggerHaptics(toastState.type)
            } else {
                // New Toast: Launch 2-Phase Waterdrop & Morphing Animation
                isMounted = true
                dropProgress.snapTo(0f)
                expandProgress.snapTo(0f)
                dragY.snapTo(0f)

                triggerHaptics(toastState.type)

                // Phase 1: Pure 48x48 round waterdrop circle drops down from top
                launch {
                    dropProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = 400f
                        )
                    )
                }

                // Phase 2: Overlapping Liquid Swell out to 2 sides (with 65ms organic delay)
                launch {
                    delay(65)
                    expandProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 320f
                        )
                    )
                }
            }

            // Auto-hide lifecycle timer if duration is specified
            if (toastState.durationMs > 0) {
                delay(toastState.durationMs)
                startExitAnimation()
            }
        } else if (isMounted) {
            // Explicit hide requested
            startExitAnimation()
        }
    }

    if (!isMounted) return

    // Calculate interpolated dimensions & layout
    val currentWidth = lerp(CIRCLE_SIZE, fullWidth, expandProgress.value)
    val translateY = lerp((-60).dp, 0.dp, dropProgress.value) + dragY.value.dp
    val containerAlpha = (dropProgress.value / 0.2f).coerceIn(0f, 1f)
    val contentAlpha = ((expandProgress.value - 0.22f) / 0.78f).coerceIn(0f, 1f)

    val backgroundColor = if (isDark) {
        Color(0xFF1E1E20).copy(alpha = 0.98f)
    } else {
        Color.White.copy(alpha = 0.98f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    // Top overlay wrapper with status bar padding
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp)
            .offset { IntOffset(0, translateY.roundToPx()) }
            .graphicsLayer { alpha = containerAlpha },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .width(currentWidth)
                .heightIn(min = CIRCLE_SIZE)
                .shadow(
                    elevation = if (isDark) 12.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.15f)
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragY.value < -15f) {
                                // Real swipe-up momentum dismiss
                                coroutineScope.launch {
                                    launch {
                                        dragY.animateTo(
                                            targetValue = -120f,
                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
                                        )
                                    }
                                    launch {
                                        expandProgress.animateTo(0f, tween(110))
                                    }
                                    delay(160)
                                    isMounted = false
                                    AppToast.hide()
                                    dragY.snapTo(0f)
                                }
                            } else {
                                coroutineScope.launch {
                                    dragY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount < 0 || dragY.value < 0) {
                                coroutineScope.launch {
                                    dragY.snapTo(dragY.value + dragAmount)
                                }
                            }
                        }
                    )
                },
            shape = CircleShape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed Icon Container (48x48dp, perfectly centered in Phase 1 droplet, stays left in Phase 2)
                Box(
                    modifier = Modifier
                        .size(CIRCLE_SIZE),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = currentDisplayState.type,
                        animationSpec = tween(280),
                        label = "ToastIconCrossfade"
                    ) { type ->
                        when (type) {
                            ToastType.SUCCESS -> {
                                Icon(
                                    imageVector = CupertinoIcons.Filled.CheckmarkCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            ToastType.ERROR -> {
                                Icon(
                                    imageVector = CupertinoIcons.Filled.XmarkCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            ToastType.INFO -> {
                                Icon(
                                    imageVector = CupertinoIcons.Filled.ArrowDownCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            ToastType.PROGRESS -> {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    if (currentDisplayState.progress >= 0) {
                                        val smoothIconProgress by animateFloatAsState(
                                            targetValue = (currentDisplayState.progress.coerceIn(0, 100)) / 100f,
                                            animationSpec = tween(400, easing = LinearOutSlowInEasing),
                                            label = "SmoothIconProgress"
                                        )
                                        CircularProgressIndicator(
                                            progress = { smoothIconProgress },
                                            modifier = Modifier.fillMaxSize(),
                                            strokeWidth = 2.5.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.fillMaxSize(),
                                            strokeWidth = 2.5.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Content Row (Title, SubText, Progress Bar, Close Button) fading in during Phase 2
                if (expandProgress.value > 0.15f) {
                    Row(
                        modifier = Modifier
                            .width(fullWidth - CIRCLE_SIZE)
                            .padding(end = 14.dp)
                            .graphicsLayer { alpha = contentAlpha },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = 380f
                                    )
                                )
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Crossfade(
                                targetState = currentDisplayState.type == ToastType.PROGRESS,
                                animationSpec = tween(280),
                                label = "ToastContentCrossfade"
                            ) { isProgress ->
                                if (isProgress) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Row 1: Title + Percentage
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = currentDisplayState.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDark) Color.White else Color(0xFF111827),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            if (currentDisplayState.progress >= 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${currentDisplayState.progress}%",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.5.dp))

                                        // Row 2: Downloaded/Total MB + Speed
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val sizeLabel = when {
                                                currentDisplayState.totalBytes > 0 ->
                                                    "${AppToast.formatBytes(currentDisplayState.downloadedBytes)} / ${AppToast.formatBytes(currentDisplayState.totalBytes)}"
                                                currentDisplayState.downloadedBytes > 0 ->
                                                    AppToast.formatBytes(currentDisplayState.downloadedBytes)
                                                else ->
                                                    currentDisplayState.message ?: "Đang kết nối..."
                                            }

                                            Text(
                                                text = sizeLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            if (currentDisplayState.speedBytesPerSec > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = AppToast.formatSpeed(currentDisplayState.speedBytesPerSec),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Modern Silky Smooth Progress Bar with Shimmer
                                        SmoothLiveProgressBar(
                                            progress = currentDisplayState.progress,
                                            isDark = isDark
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = currentDisplayState.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color.White else Color(0xFF111827),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (!currentDisplayState.message.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = currentDisplayState.message!!,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Close Button matching MEBIECO
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    startExitAnimation()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.Xmark,
                                contentDescription = "Close",
                                tint = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern iOS / Dynamic Island style animated progress bar with:
 * - Silky smooth tween progress interpolation (no rubber-band snapping)
 * - Vibrant gradient fill with glowing highlight
 * - Infinite continuous light streak / shimmer sweep animation
 * - Fluid indeterminate wave mode when file size is not yet known
 */
@Composable
private fun SmoothLiveProgressBar(
    progress: Int, // 0..100, or -1 for indeterminate
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackBgColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    // Smoothly interpolate progress value (0f..1f)
    val targetProgress = if (progress >= 0) (progress.coerceIn(0, 100) / 100f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "SmoothLiveProgress"
    )

    // Infinite transition for shimmer and wave animations
    val infiniteTransition = rememberInfiniteTransition(label = "ProgressShimmerTransition")

    // Continuous light sweep across the bar (1.6s cycle)
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerPhase"
    )

    // Indeterminate wave offset (0f..1f)
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveOffset"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(trackBgColor)
    ) {
        val totalWidth = maxWidth
        val totalWidthPx = constraints.maxWidth.toFloat()

        if (progress >= 0) {
            // Determinate Progress Bar with Gradient & Shimmer
            val fillWidth = totalWidth * animatedProgress.coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(fillWidth)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                primaryColor,
                                Color(0xFF00C6FF),
                                primaryColor
                            )
                        )
                    )
            ) {
                // Moving light streak / shimmer beam effect
                val widthPx = totalWidthPx * animatedProgress.coerceIn(0.01f, 1f)
                val startX = (shimmerPhase * widthPx) - (widthPx * 0.3f)
                val endX = (shimmerPhase * widthPx) + (widthPx * 0.3f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.55f),
                                    Color.Transparent
                                ),
                                startX = startX,
                                endX = endX
                            )
                        )
                )
            }
        } else {
            // Indeterminate Pulsing Wave traveling along the full track width
            val waveWidth = totalWidth * 0.42f
            val maxTravel = totalWidth - waveWidth
            val currentOffset = maxTravel * waveOffset

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(waveWidth)
                    .offset(x = currentOffset)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor,
                                Color(0xFF00C6FF),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
