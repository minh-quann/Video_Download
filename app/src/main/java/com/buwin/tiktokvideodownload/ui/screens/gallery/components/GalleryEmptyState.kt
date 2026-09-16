package com.buwin.tiktokvideodownload.ui.screens.gallery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Photo
import io.github.alexzhirkevich.cupertino.icons.outlined.Shield

/**
 * Empty state shown when media items are empty or when storage permissions are required.
 */
@Composable
fun GalleryEmptyState(
    isDark: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (!hasPermission) CupertinoIcons.Outlined.Shield else CupertinoIcons.Outlined.Photo,
                contentDescription = null,
                tint = if (isDark) Color(0xFF48484A) else Color(0xFF8E8E93),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (!hasPermission) "Cần quyền truy cập bộ sưu tập" else "Chưa có mục nào",
            color = if (isDark) Color.White else Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (!hasPermission) {
                "Cấp quyền để ứng dụng hiển thị toàn bộ ảnh và video có trong thiết bị của bạn"
            } else {
                "Các ảnh và video được lưu trong thiết bị sẽ tự động xuất hiện tại đây"
            },
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        if (!hasPermission) {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF007AFF))
                    .clickable { onRequestPermission() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Cho phép truy cập",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
