package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark

/**
 * Top header bar for the editor with close button, title, and export action.
 */
@Composable
fun EditorTopBar(
    title: String = "Chỉnh sửa video",
    exportLabel: String = "Lưu & Xuất",
    isExporting: Boolean = false,
    onClose: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Close button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .border(1.dp, Color(0xFFE6E8EC).copy(alpha = 0.35f), CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CupertinoIcons.Outlined.Xmark,
                contentDescription = "Cancel",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Export button
        Surface(
            onClick = { onExport() },
            shape = CircleShape,
            color = Color(0xFF007AFF),
            border = BorderStroke(1.dp, Color(0xFFE6E8EC).copy(alpha = 0.35f)),
            enabled = !isExporting
        ) {
            Text(
                text = exportLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
