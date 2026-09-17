package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.buwin.tiktokvideodownload.ui.components.editor.model.ImageEditorTab
import com.buwin.tiktokvideodownload.ui.components.liquid.InteractiveHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.CameraFilters
import io.github.alexzhirkevich.cupertino.icons.outlined.CropRotate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Floating Liquid Glass Capsule Tab Bar with rubber-band physics,
 * squash-and-stretch tactile response.
 */
@Composable
fun ImageEditorBottomBar(
    currentTab: ImageEditorTab,
    onTabSelected: (ImageEditorTab) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val bottomTabInteractiveHighlight = remember(coroutineScope) {
        InteractiveHighlight(animationScope = coroutineScope)
    }

    Box(
        modifier = modifier
            .height(72.dp)
            .width(248.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(3f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                highlight = null,
                layerBlock = {
                    val progress = bottomTabInteractiveHighlight.pressProgress
                    val offset = bottomTabInteractiveHighlight.offset
                    val dragDist = hypot(offset.x, offset.y)
                    val offsetAngle = atan2(offset.y, offset.x)

                    // Fluid interface: Damped rubber-band displacement
                    val maxDisplacement = (this.size.minDimension * 0.32f).coerceAtMost(14f.dp.toPx())
                    val dampingDistance = (this.size.minDimension * 1.5f).coerceAtLeast(36f.dp.toPx())
                    val dampedDistance = maxDisplacement * tanh(dragDist / dampingDistance)

                    translationX = dampedDistance * cos(offsetAngle)
                    translationY = dampedDistance * sin(offsetAngle)

                    // Liquid Glass tactile deformation: Volume-preserving squash & stretch
                    val maxStretch = 0.20f
                    val stretchFactor = maxStretch * tanh(dragDist / dampingDistance)
                    val scaleAlong = 1f + stretchFactor
                    val scalePerp = 1f / sqrt(scaleAlong)

                    // Subtle tactile pop on press
                    val pressScale = lerp(1f, 1.035f, progress)

                    val cos2 = cos(offsetAngle) * cos(offsetAngle)
                    val sin2 = sin(offsetAngle) * sin(offsetAngle)
                    scaleX = pressScale * (scaleAlong * cos2 + scalePerp * sin2)
                    scaleY = pressScale * (scaleAlong * sin2 + scalePerp * cos2)
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.20f))
                }
            )
            .then(bottomTabInteractiveHighlight.modifier)
            .then(bottomTabInteractiveHighlight.gestureModifier)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorBottomPillTab(
                label = "Adjust",
                isSelected = currentTab == ImageEditorTab.ADJUST,
                modifier = Modifier.weight(1f),
                iconContent = { tint ->
                    AdjustDialIcon(tint = tint)
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(ImageEditorTab.ADJUST)
                }
            )

            EditorBottomPillTab(
                label = "Filters",
                isSelected = currentTab == ImageEditorTab.FILTERS,
                modifier = Modifier.weight(1f),
                iconContent = { tint ->
                    Icon(
                        imageVector = CupertinoIcons.Outlined.CameraFilters,
                        contentDescription = "Filters",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(ImageEditorTab.FILTERS)
                }
            )

            EditorBottomPillTab(
                label = "Crop",
                isSelected = currentTab == ImageEditorTab.CROP,
                modifier = Modifier.weight(1f),
                iconContent = { tint ->
                    Icon(
                        imageVector = CupertinoIcons.Outlined.CropRotate,
                        contentDescription = "Crop",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(ImageEditorTab.CROP)
                }
            )
        }
    }
}

/**
 * Bottom Floating Pill Tab with active indicator triangle & spring response.
 */
@Composable
private fun EditorBottomPillTab(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    iconContent: @Composable (tint: Color) -> Unit,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tab_scale"
    )

    val triangleScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "triangle_scale"
    )

    val contentColor = if (isSelected) Color(0xFFFFD60A) else Color(0xFF9E9EA4)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Active indicator triangle on top pointing downward
        Box(
            modifier = Modifier
                .height(5.dp)
                .graphicsLayer {
                    scaleX = triangleScale
                    scaleY = triangleScale
                    alpha = triangleScale
                },
            contentAlignment = Alignment.Center
        ) {
            ActiveIndicatorTriangle(
                color = Color(0xFFFFD60A)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Tab Icon (24.dp)
        iconContent(contentColor)

        Spacer(modifier = Modifier.height(3.dp))

        // Tab Label (11.sp)
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Downward pointing yellow triangle indicator.
 */
@Composable
private fun ActiveIndicatorTriangle(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD60A)
) {
    ComposeCanvas(modifier = modifier.size(width = 6.5.dp, height = 4.5.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path, color = color)
    }
}

/**
 * Adjust dial icon with circular gauge and 8 surrounding tick dots.
 */
@Composable
private fun AdjustDialIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    ComposeCanvas(modifier = modifier.size(24.dp)) {
        val strokeW = 1.6.dp.toPx()
        val center = this.center
        val radius = size.minDimension * 0.27f

        // Center dial circle
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = strokeW)
        )

        // Center dial needle pointer (pointing at ~45 degrees up-right: -45 deg in screen coordinates)
        val pointerAngleRad = Math.toRadians(-45.0).toFloat()
        val pointerLength = radius * 0.82f
        val pointerEnd = Offset(
            x = center.x + pointerLength * cos(pointerAngleRad),
            y = center.y + pointerLength * sin(pointerAngleRad)
        )
        drawLine(
            color = tint,
            start = center,
            end = pointerEnd,
            strokeWidth = strokeW * 1.15f,
            cap = StrokeCap.Round
        )

        // 8 outer dots symmetrically spaced at 45-degree intervals
        val dotOrbitRadius = size.minDimension * 0.44f
        val dotRadius = 1.2.dp.toPx()
        for (i in 0 until 8) {
            val angleRad = Math.toRadians(i * 45.0).toFloat()
            val dotCenter = Offset(
                x = center.x + dotOrbitRadius * cos(angleRad),
                y = center.y + dotOrbitRadius * sin(angleRad)
            )
            drawCircle(
                color = tint,
                radius = dotRadius,
                center = dotCenter
            )
        }
    }
}
