package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidButton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidButtonVariant
import com.kyant.backdrop.Backdrop

/**
 * Liquid Glass pill-shaped chip for the editor toolbar using the app's standard LiquidButton.
 * Uses the exact same InteractiveHighlight spring physics, refraction, and liquid glass styling as the rest of the app.
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

    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        variant = if (isActive) LiquidButtonVariant.Tinted else LiquidButtonVariant.Surface,
        tint = if (isActive) accentColor else if (hasActiveOp) accentColor.copy(alpha = 0.40f) else Color.Unspecified,
        surfaceColor = if (hasActiveOp && !isActive) accentColor.copy(alpha = 0.25f) else Color.Unspecified,
        contentColor = Color.White,
        isDark = true,
        isInteractive = true,
        modifier = modifier.height(42.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (isActive || hasActiveOp) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}
