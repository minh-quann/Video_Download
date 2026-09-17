package com.buwin.tiktokvideodownload.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Backward
import io.github.alexzhirkevich.cupertino.icons.filled.Forward
import io.github.alexzhirkevich.cupertino.icons.filled.Speaker
import io.github.alexzhirkevich.cupertino.icons.filled.SpeakerSlash
import io.github.alexzhirkevich.cupertino.icons.filled.SpeakerWave2
import io.github.alexzhirkevich.cupertino.icons.filled.SunMax
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Animated feedback for double tap forward/rewind 5s with Apple Liquid Glass.
 */
@Composable
fun DoubleTapSeekIndicator(
    visible: Boolean,
    isForward: Boolean,
    isLandscape: Boolean,
    backdrop: Backdrop? = null,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isDark
    val containerColor = if (isLightTheme) {
        Color.White.copy(alpha = 0.28f)
    } else {
        Color(0xFF505056).copy(alpha = 0.55f)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(
            initialScale = 0.75f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.85f,
            animationSpec = tween(200)
        ),
        modifier = modifier.padding(horizontal = if (isLandscape) 64.dp else 28.dp)
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(24f.dp.toPx(), 24f.dp.toPx())
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = if (isLightTheme) 0.55f else 0.35f)
                            },
                            shadow = {
                                Shadow(
                                    radius = 12f.dp,
                                    color = if (isLightTheme) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.40f)
                                )
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = 6f.dp,
                                    alpha = if (isLightTheme) 0.10f else 0.20f
                                )
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                            }
                        )
                    } else {
                        Modifier
                            .clip(CircleShape)
                            .background(containerColor)
                    }
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isForward) CupertinoIcons.Filled.Forward else CupertinoIcons.Filled.Backward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isForward) "+5s" else "-5s",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Apple iOS Control Center vertical slider with Liquid Glass refraction, blur,
 * specular highlight, and full capsule corner rounding.
 */
@Composable
fun AppleVerticalLiquidSlider(
    value: Float,
    icon: ImageVector,
    contentDescription: String,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDark.current,
    width: Dp = 46.dp,
    height: Dp = 158.dp
) {
    val isLightTheme = !isDark
    val containerColor = if (isLightTheme) {
        Color.White.copy(alpha = 0.28f)
    } else {
        Color(0xFF505056).copy(alpha = 0.55f)
    }

    val clampedFraction = value.coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = clampedFraction,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 800f),
        label = "LiquidSliderProgress"
    )

    // Dynamic contrast inversion: colors invert when covered by the bright progress fill
    val isIconCovered = animatedFraction >= 0.20f
    val iconColor by animateColorAsState(
        targetValue = if (isIconCovered) Color(0xFF1C1C1E) else Color.White,
        animationSpec = tween(120),
        label = "SliderIconColor"
    )

    val isTextCovered = animatedFraction >= 0.86f
    val textColor by animateColorAsState(
        targetValue = if (isTextCovered) Color(0xFF1C1C1E) else Color.White,
        animationSpec = tween(120),
        label = "SliderTextColor"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(24f.dp.toPx(), 24f.dp.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = if (isLightTheme) 0.55f else 0.35f)
                        },
                        shadow = {
                            Shadow(
                                radius = 12f.dp,
                                color = if (isLightTheme) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.45f)
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 6f.dp,
                                alpha = if (isLightTheme) 0.10f else 0.22f
                            )
                        },
                        onDrawSurface = {
                            drawRect(containerColor)
                        }
                    )
                } else {
                    Modifier
                        .clip(Capsule())
                        .background(containerColor)
                }
            )
            .clip(Capsule())
    ) {
        // Vertical progress fill rising from bottom to top
        if (animatedFraction > 0.001f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFraction)
                    .background(
                        if (isLightTheme) Color.White.copy(alpha = 0.94f)
                        else Color.White.copy(alpha = 0.90f)
                    )
            )
        }

        // Percentage text at top
        Text(
            text = "${(clampedFraction * 100).roundToInt()}%",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 11.dp)
        )

        // Apple SF symbol at bottom
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .size(22.dp)
        )
    }
}

/**
 * HUD indicator displaying current screen brightness percentage on vertical drag.
 * Designed in Apple iOS Control Center vertical capsule style with Liquid Glass effect.
 */
@Composable
fun BrightnessHud(
    visible: Boolean,
    brightness: Float,
    backdrop: Backdrop? = null,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.90f,
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        AppleVerticalLiquidSlider(
            value = brightness,
            icon = CupertinoIcons.Filled.SunMax,
            contentDescription = "Brightness",
            backdrop = backdrop,
            isDark = isDark
        )
    }
}

/**
 * HUD indicator displaying media volume level on vertical drag.
 * Designed in Apple iOS Control Center vertical capsule style with Liquid Glass effect.
 */
@Composable
fun VolumeHud(
    visible: Boolean,
    volumeFraction: Float,
    backdrop: Backdrop? = null,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    val volumeIcon = when {
        volumeFraction <= 0.001f -> CupertinoIcons.Filled.SpeakerSlash
        volumeFraction < 0.33f -> CupertinoIcons.Filled.Speaker
        else -> CupertinoIcons.Filled.SpeakerWave2
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.90f,
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        AppleVerticalLiquidSlider(
            value = volumeFraction,
            icon = volumeIcon,
            contentDescription = "Volume",
            backdrop = backdrop,
            isDark = isDark
        )
    }
}

/**
 * HUD indicator displaying seek position offset and target time on horizontal drag.
 * Wrapped in Apple Liquid Glass Capsule.
 */
@Composable
fun SeekScrubHud(
    visible: Boolean,
    targetSeekMs: Int,
    initialSeekMs: Int,
    totalDurationMs: Int,
    backdrop: Backdrop? = null,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isDark
    val containerColor = if (isLightTheme) {
        Color.White.copy(alpha = 0.28f)
    } else {
        Color(0xFF505056).copy(alpha = 0.55f)
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.90f,
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(24f.dp.toPx(), 24f.dp.toPx())
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = if (isLightTheme) 0.55f else 0.35f)
                            },
                            shadow = {
                                Shadow(
                                    radius = 12f.dp,
                                    color = if (isLightTheme) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.45f)
                                )
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = 6f.dp,
                                    alpha = if (isLightTheme) 0.10f else 0.22f
                                )
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                            }
                        )
                    } else {
                        Modifier
                            .clip(Capsule())
                            .background(containerColor)
                    }
                )
                .clip(Capsule())
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val diffSeconds = (targetSeekMs - initialSeekMs) / 1000
                val diffSign = if (diffSeconds >= 0) "+${diffSeconds}s" else "${diffSeconds}s"
                Icon(
                    imageVector = if (diffSeconds >= 0) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${formatDuration(targetSeekMs)} / ${formatDuration(totalDurationMs)}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "($diffSign)",
                        color = if (diffSeconds >= 0) Color(0xFF34D399) else Color(0xFFF87171),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Formats millisecond duration into mm:ss format.
 */
fun formatDuration(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
