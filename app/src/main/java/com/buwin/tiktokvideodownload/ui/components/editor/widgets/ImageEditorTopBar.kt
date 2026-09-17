package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowTurnUpLeft
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowTurnUpRight

import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * Image editor top header bar featuring Cancel, Undo/Redo capsule, and Done button.
 * Uses Liquid Glass backdrops and haptic feedback.
 */
@Composable
fun ImageEditorTopBar(
    backdrop: Backdrop,
    hasChanges: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onSave: () -> Unit,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isLightTheme = !isDark
    val containerColor = if (isLightTheme) Color.White.copy(alpha = 0.28f)
    else Color(0xFF505056).copy(alpha = 0.55f)
    val contentColor = if (isDark) Color.White else Color(0xFF1C1C1E)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Cancel
        LiquidButton(
            onClick = onDismiss,
            backdrop = backdrop,
            showBorder = false,
            isDark = isDark,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = "Hủy",
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        // Center: Undo / Redo Liquid Glass Capsule
        Box(
            modifier = Modifier
                .height(36.dp)
                .drawBackdrop(
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
                        drawRect(containerColor)
                    }
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.ArrowTurnUpLeft,
                    contentDescription = "Undo",
                    tint = if (hasChanges) contentColor else Color(0xFF636366),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            enabled = hasChanges,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUndo()
                            }
                        )
                )
                Icon(
                    imageVector = CupertinoIcons.Outlined.ArrowTurnUpRight,
                    contentDescription = "Redo",
                    tint = Color(0xFF636366),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Right: Done
        LiquidButton(
            onClick = onSave,
            backdrop = backdrop,
            isInteractive = hasChanges && !isSaving,
            showBorder = false,
            isDark = isDark,
            surfaceColor = if (hasChanges) Color(0xFFFFD60A).copy(alpha = 0.35f) else Color.Unspecified,
            modifier = Modifier.height(36.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color(0xFFFFD60A),
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Xong",
                    color = if (hasChanges) Color(0xFFFFD60A) else if (isDark) Color(0xFF636366) else Color(0xFF8E8E93),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}
