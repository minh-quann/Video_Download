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

import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark

/**
 * Editor top bar using Liquid Glass components.
 * Close = LiquidRoundButton (non-interactive), Export = LiquidButton (non-interactive).
 * isInteractive=false avoids extra render layers from InteractiveHighlight.
 */
@Composable
fun EditorTopBar(
    exportLabel: String = "Xong",
    isExporting: Boolean = false,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onExport: () -> Unit,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isDark) Color.White else Color(0xFF1C1C1E)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Close button - Liquid Glass round button (Apple style)
        LiquidRoundButton(
            onClick = onClose,
            backdrop = backdrop,
            size = 38.dp,
            isInteractive = false,
            isDark = isDark
        ) {
            Icon(
                imageVector = CupertinoIcons.Outlined.Xmark,
                contentDescription = "Hủy",
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = "Chỉnh sửa video",
            color = contentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Export button - Apple Photos style Done / Action button
        LiquidButton(
            onClick = { if (!isExporting) onExport() },
            backdrop = backdrop,
            isInteractive = false,
            tint = Color(0xFF007AFF),
            surfaceColor = Color(0xFF007AFF).copy(alpha = 0.35f)
        ) {
            Text(
                text = if (exportLabel == "Chọn thao tác") "Xong" else exportLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
