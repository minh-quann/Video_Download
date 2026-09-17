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
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

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
            surfaceColor = Color.White.copy(alpha = 0.16f),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = "Cancel",
                color = Color.White,
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
                        blur(3f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = null,
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.20f))
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
                    tint = if (hasChanges) Color.White else Color(0xFF636366),
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
            surfaceColor = if (hasChanges) Color(0xFFFFD60A).copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f),
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
                    text = "Done",
                    color = if (hasChanges) Color(0xFFFFD60A) else Color(0xFF636366),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }
        }
    }
}
