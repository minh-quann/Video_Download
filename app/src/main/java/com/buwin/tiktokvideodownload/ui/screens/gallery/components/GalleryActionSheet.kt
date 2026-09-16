package com.buwin.tiktokvideodownload.ui.screens.gallery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.buwin.tiktokvideodownload.ui.screens.gallery.model.GalleryMediaItem
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.filled.Trash
import io.github.alexzhirkevich.cupertino.icons.outlined.Photo
import io.github.alexzhirkevich.cupertino.icons.outlined.SliderHorizontal3

/**
 * Modal bottom sheet displaying quick actions for a selected gallery item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryActionSheet(
    item: GalleryMediaItem,
    isDark: Boolean,
    onDismissRequest: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Item Header Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val context = LocalContext.current
                val imageRequest = remember(item.uri, item.sourceRecord?.coverUrl) {
                    val cover = item.sourceRecord?.coverUrl
                    val targetData = if (!cover.isNullOrEmpty()) cover else item.uri
                    ImageRequest.Builder(context)
                        .data(targetData)
                        .apply {
                            if (item.isVideo) {
                                videoFrameMillis(1000L)
                            }
                        }
                        .crossfade(true)
                        .build()
                }

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.isVideo) {
                            if (item.durationText.isNotEmpty()) "Video • ${item.durationText}" else "Tệp Video"
                        } else "Hình ảnh",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                }
            }

            // 1. Play / View
            GalleryActionRow(
                icon = if (item.isVideo) CupertinoIcons.Filled.Play else CupertinoIcons.Outlined.Photo,
                title = if (item.isVideo) "Phát video" else "Xem ảnh",
                subtitle = if (item.isVideo) "Xem trực tiếp toàn màn hình" else "Xem ảnh kích thước đầy đủ",
                accentColor = Color(0xFF007AFF),
                isDark = isDark,
                onClick = onPlay
            )

            // 2. Edit Video / Photo
            GalleryActionRow(
                icon = CupertinoIcons.Outlined.SliderHorizontal3,
                title = if (item.isVideo) "Chỉnh sửa video" else "Chỉnh sửa ảnh",
                subtitle = if (item.isVideo) "Cắt, xoay, tốc độ, đổi màu sắc & bộ lọc" else "Chỉnh màu sắc, độ sáng & bộ lọc",
                accentColor = Color(0xFFFF9500),
                isDark = isDark,
                onClick = onEdit
            )

            // 3. Share
            GalleryActionRow(
                icon = Icons.Filled.Share,
                title = "Chia sẻ",
                subtitle = "Gửi qua Zalo, Messenger, TikTok",
                accentColor = Color(0xFF34C759),
                isDark = isDark,
                onClick = onShare
            )

            // 4. Delete
            GalleryActionRow(
                icon = CupertinoIcons.Filled.Trash,
                title = "Xóa tệp",
                subtitle = "Xóa vĩnh viễn khỏi thư viện",
                accentColor = Color(0xFFFF3B30),
                isDark = isDark,
                onClick = onDelete
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Single actionable row within the bottom sheet.
 */
@Composable
fun GalleryActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isDark) Color.White else Color.Black,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 11.5.sp
            )
        }
    }
}
