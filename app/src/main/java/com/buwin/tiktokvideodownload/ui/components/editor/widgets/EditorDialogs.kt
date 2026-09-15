package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.buwin.tiktokvideodownload.ui.components.player.formatDuration

/**
 * Trim range info chips showing start/end time and selected duration.
 */
@Composable
fun TrimRangeInfo(
    startTrimMs: Long,
    endTrimMs: Long,
    modifier: Modifier = Modifier
) {
    val durationSelectedMs = (endTrimMs - startTrimMs).coerceAtLeast(0L)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Bắt đầu: ${formatDuration(startTrimMs.toInt())}",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFD60A).copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "Thời lượng: ${String.format(java.util.Locale.US, "%.1fs", durationSelectedMs / 1000f)}",
                color = Color(0xFFFFD60A),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Kết thúc: ${formatDuration(endTrimMs.toInt())}",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Export progress dialog shown during processing.
 */
@Composable
fun ExportProgressDialog(
    progressLabel: String,
    progress: Float
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1E))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF007AFF),
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = progressLabel,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}% hoàn thành",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Error dialog shown when export fails.
 */
@Composable
fun ExportErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Lỗi xử lý",
                    color = Color(0xFFEF4444),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Đóng",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
