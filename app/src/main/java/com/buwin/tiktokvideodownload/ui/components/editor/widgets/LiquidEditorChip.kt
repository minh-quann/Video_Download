package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/**
 * Liquid Glass pill-shaped chip for the editor toolbar.
 * Uses drawBackdrop for glass refraction (blur + lens + vibrancy).
 *
 * ponytail: No InteractiveHighlight deformation to keep render node
 * count low. Glass refraction still works because backdrop is captured
 * on a sibling Box (not wrapping the VideoView SurfaceView).
 */
@Composable
fun LiquidEditorChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    hasActiveOp: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFF007AFF)
    val tint = when {
        isActive -> accentColor.copy(alpha = 0.55f)
        hasActiveOp -> accentColor.copy(alpha = 0.2f)
        else -> Color.Unspecified
    }
    val surfaceColor = when {
        isActive -> Color.White.copy(alpha = 0.12f)
        else -> Color.White.copy(alpha = 0.06f)
    }
    val contentColor = when {
        isActive || hasActiveOp -> Color.White
        else -> Color(0xFFAAAAAA)
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(3f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                highlight = {
                    Highlight.Plain.copy(
                        alpha = if (isActive) 0.6f else 0.3f
                    )
                },
                shadow = {
                    Shadow(
                        radius = 6f.dp,
                        color = Color.Black.copy(alpha = 0.15f)
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 3f.dp,
                        alpha = 0.1f
                    )
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.65f))
                    }
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .height(42f.dp)
            .padding(horizontal = 14f.dp),
        horizontalArrangement = Arrangement.spacedBy(6f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
