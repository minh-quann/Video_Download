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

import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark

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
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isDark
    val accentColor = Color(0xFF007AFF)
    val tint = when {
        isActive -> accentColor.copy(alpha = 0.55f)
        hasActiveOp -> accentColor.copy(alpha = 0.2f)
        else -> Color.Unspecified
    }
    val surfaceColor = when {
        isActive -> if (isLightTheme) Color.White.copy(alpha = 0.35f) else Color(0xFF505056).copy(alpha = 0.70f)
        else -> if (isLightTheme) Color.White.copy(alpha = 0.28f) else Color(0xFF505056).copy(alpha = 0.55f)
    }
    val contentColor = when {
        isActive || hasActiveOp -> if (isLightTheme) Color(0xFF007AFF) else Color.White
        else -> if (isDark) Color.White else Color(0xFF1C1C1E)
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(24f.dp.toPx(), 24f.dp.toPx())
                },
                highlight = {
                    Highlight.Default.copy(
                        alpha = if (isLightTheme) 0.55f else 0.35f
                    )
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
