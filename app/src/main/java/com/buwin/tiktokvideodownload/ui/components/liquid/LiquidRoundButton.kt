package com.buwin.tiktokvideodownload.ui.components.liquid

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Circular / Round button with Liquid Glass refraction, blur, specular highlight, and tactile deformation.
 * Supports dynamic light/dark mode identical to LiquidBottomTabs:
 * - Dark mode: rich smoky dark glass (0xFF18181B @ 45%)
 * - Light mode: pure frosted white glass (Color.White @ 28%)
 * Effects: 8dp blur, 24dp lens refraction, dynamic specular highlight, ambient drop shadow & inner shadow.
 */
@Composable
fun LiquidRoundButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    shape: Shape = CircleShape,
    isInteractive: Boolean = true,
    showBorder: Boolean = false,
    isDark: Boolean = LocalIsDark.current,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val isLightTheme = !isDark
    val containerColor = if (surfaceColor.isSpecified) {
        surfaceColor
    } else {
        if (isLightTheme) Color.White.copy(alpha = 0.28f)
        else Color(0xFF505056).copy(alpha = 0.55f)
    }

    val contentColor = if (isDark) Color.White else Color(0xFF1C1C1E)

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Box(
        modifier = modifier
            .size(size)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(24f.dp.toPx(), 24f.dp.toPx())
                },
                highlight = {
                    if (showBorder) {
                        Highlight.Plain
                    } else {
                        Highlight.Default.copy(alpha = if (isLightTheme) 0.55f else 0.35f)
                    }
                },
                shadow = {
                    Shadow(
                        radius = 8f.dp,
                        color = if (isLightTheme) Color.Black.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.35f)
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 6f.dp,
                        alpha = if (isLightTheme) 0.08f else 0.18f
                    )
                },
                layerBlock = if (isInteractive) {
                    {
                        val progress = interactiveHighlight.pressProgress
                        val offset = interactiveHighlight.offset
                        val dragDist = hypot(offset.x, offset.y)
                        val offsetAngle = atan2(offset.y, offset.x)

                        // Apple Fluid Interface: Damped rubber-band displacement
                        val maxDisplacement = (this.size.minDimension * 0.18f).coerceAtMost(8f.dp.toPx())
                        val dampingDistance = (this.size.minDimension * 1.5f).coerceAtLeast(30f.dp.toPx())
                        val dampedDistance = maxDisplacement * tanh(dragDist / dampingDistance)

                        translationX = dampedDistance * cos(offsetAngle)
                        translationY = dampedDistance * sin(offsetAngle)

                        // Apple Liquid Glass tactile deformation: Volume-preserving squash & stretch
                        val maxStretch = 0.16f
                        val stretchFactor = maxStretch * tanh(dragDist / dampingDistance)
                        val scaleAlong = 1f + stretchFactor
                        val scalePerp = 1f / sqrt(scaleAlong)

                        // Subtle tactile pop on press
                        val pressScale = lerp(1f, 1.04f, progress)

                        val cos2 = cos(offsetAngle) * cos(offsetAngle)
                        val sin2 = sin(offsetAngle) * sin(offsetAngle)
                        scaleX = pressScale * (scaleAlong * cos2 + scalePerp * sin2)
                        scaleY = pressScale * (scaleAlong * sin2 + scalePerp * cos2)
                    }
                } else null,
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    drawRect(containerColor)
                }
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
