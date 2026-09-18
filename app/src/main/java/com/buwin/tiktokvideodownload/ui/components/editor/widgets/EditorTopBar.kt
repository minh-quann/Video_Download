package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidButtonVariant
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.Backdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark

/**
 * Editor top bar using the app's standard LiquidRoundButton and LiquidButton.
 * Matches 100% of the liquid glass effects, colors, and spring physics throughout the app.
 */
@Composable
fun EditorTopBar(
    exportLabel: String = "Xong",
    isExporting: Boolean = false,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onExport: () -> Unit,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Close button: Standard LiquidRoundButton
        LiquidRoundButton(
            onClick = onClose,
            backdrop = backdrop,
            size = 38.dp,
            isInteractive = true,
            isDark = true
        ) {
            Icon(
                imageVector = CupertinoIcons.Outlined.Xmark,
                contentDescription = "Hủy",
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }

        // Title: Pure white text on dark background
        Text(
            text = "Chỉnh sửa video",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Export button: Standard LiquidButton with tinted variant
        LiquidButton(
            onClick = { if (!isExporting) onExport() },
            backdrop = backdrop,
            variant = LiquidButtonVariant.Tinted,
            tint = Color(0xFF007AFF),
            isInteractive = !isExporting,
            isDark = true,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (exportLabel == "Chọn thao tác") "Xong" else exportLabel,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
