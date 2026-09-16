package com.buwin.tiktokvideodownload.ui.components.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.util.lerp
import com.buwin.tiktokvideodownload.ui.components.liquid.InteractiveHighlight
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Heart
import io.github.alexzhirkevich.cupertino.icons.outlined.Heart
import io.github.alexzhirkevich.cupertino.icons.outlined.InfoCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.SliderHorizontal3
import io.github.alexzhirkevich.cupertino.icons.outlined.SquareAndArrowUp
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Authentic Apple Photos style bottom action bar for image viewing.
 * Layout:
 * - Leftmost: Round Liquid Glass button for Share (SquareAndArrowUp).
 * - Center: Seamless Liquid Glass Capsule holding 3 monochrome actions (Favorite, Info, Edit)
 *   with fluid rubber-band spring deformation and tactile bounce-back when dragged.
 * - Rightmost: Round Liquid Glass button for Delete (Trash).
 * All icons are monochrome white, clean, borderless, text-free SF Symbols.
 */
@Composable
fun PlayerImageBottomBar(
    backdrop: Backdrop,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animationScope = rememberCoroutineScope()
    val centerInteractiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Leftmost: Round Share button
        LiquidRoundButton(
            onClick = onShare,
            backdrop = backdrop,
            size = 46.dp,
            surfaceColor = Color.White.copy(alpha = 0.20f)
        ) {
            Icon(
                imageVector = CupertinoIcons.Outlined.SquareAndArrowUp,
                contentDescription = "Chia sẻ",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Center: Liquid Glass Capsule with 3 actions (Favorite, Info, Edit) and tactile spring deformation
        Box(
            modifier = Modifier
                .height(46.dp)
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
                        val progress = centerInteractiveHighlight.pressProgress
                        val offset = centerInteractiveHighlight.offset
                        val dragDist = hypot(offset.x, offset.y)
                        val offsetAngle = atan2(offset.y, offset.x)

                        // Apple Fluid Interface: Damped rubber-band displacement
                        val maxDisplacement = (this.size.minDimension * 0.32f).coerceAtMost(14f.dp.toPx())
                        val dampingDistance = (this.size.minDimension * 1.5f).coerceAtLeast(36f.dp.toPx())
                        val dampedDistance = maxDisplacement * tanh(dragDist / dampingDistance)

                        translationX = dampedDistance * cos(offsetAngle)
                        translationY = dampedDistance * sin(offsetAngle)

                        // Apple Liquid Glass tactile deformation: Volume-preserving squash & stretch
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
                .then(centerInteractiveHighlight.modifier)
                .then(centerInteractiveHighlight.gestureModifier)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Favorite Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                            onClick = onToggleFavorite
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) CupertinoIcons.Filled.Heart else CupertinoIcons.Outlined.Heart,
                        contentDescription = "Yêu thích",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Info / Details Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                            onClick = onOpenDetails
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Outlined.InfoCircle,
                        contentDescription = "Chi tiết",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }

                // Edit Button (Apple Photos SliderHorizontal3)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                            onClick = onEdit
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Outlined.SliderHorizontal3,
                        contentDescription = "Chỉnh sửa",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. Rightmost: Round Delete button
        LiquidRoundButton(
            onClick = onDelete,
            backdrop = backdrop,
            size = 46.dp,
            surfaceColor = Color.White.copy(alpha = 0.20f)
        ) {
            Icon(
                imageVector = CupertinoIcons.Outlined.Trash,
                contentDescription = "Xóa",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
