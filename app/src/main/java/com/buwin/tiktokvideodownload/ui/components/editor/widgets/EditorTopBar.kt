package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidButton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.Backdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark

/**
 * Editor top bar using Liquid Glass components.
 * Close = LiquidRoundButton (non-interactive), Export = LiquidButton (non-interactive).
 * isInteractive=false avoids extra render layers from InteractiveHighlight.
 */
@Composable
fun EditorTopBar(
    exportLabel: String = "Lưu & Xuất",
    isExporting: Boolean = false,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Close button - Liquid Glass round button (no deformation)
        LiquidRoundButton(
            onClick = onClose,
            backdrop = backdrop,
            size = 38.dp,
            isInteractive = false,
            surfaceColor = Color.White.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = CupertinoIcons.Outlined.Xmark,
                contentDescription = "Cancel",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = "Chỉnh sửa video",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Export button - Liquid Glass pill button with accent tint (no deformation)
        LiquidButton(
            onClick = { if (!isExporting) onExport() },
            backdrop = backdrop,
            isInteractive = false,
            tint = Color(0xFF007AFF),
            surfaceColor = Color(0xFF007AFF).copy(alpha = 0.3f)
        ) {
            Text(
                text = exportLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
