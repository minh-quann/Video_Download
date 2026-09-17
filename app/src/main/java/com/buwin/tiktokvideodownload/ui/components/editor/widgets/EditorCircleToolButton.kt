package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.Backdrop

/**
 * Liquid Glass circular tool button with selected and modified change indicators.
 */
@Composable
fun EditorCircleToolButton(
    icon: ImageVector,
    isSelected: Boolean,
    hasChange: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidRoundButton(
        onClick = onClick,
        backdrop = backdrop,
        size = 46.dp,
        showBorder = false,
        surfaceColor = when {
            isSelected -> Color(0xFFFFD60A).copy(alpha = 0.35f)
            hasChange -> Color(0xFFFFD60A).copy(alpha = 0.20f)
            else -> Color.White.copy(alpha = 0.20f)
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected || hasChange) Color(0xFFFFD60A) else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}
