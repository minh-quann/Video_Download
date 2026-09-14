package com.buwin.tiktokvideodownload.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidTopBar
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Folder
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.filled.Trash
import io.github.alexzhirkevich.cupertino.icons.filled.Video
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying the history of downloaded TikTok and Facebook media files,
 * featuring the shared progressive blur header.
 */
@Composable
fun HistoryScreen(
    backdrop: Backdrop,
    downloadHelper: DownloadManagerHelper,
    modifier: Modifier = Modifier
) {
    var historyList by remember { mutableStateOf(downloadHelper.getHistory()) }
    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .layerBackdrop(contentBackdrop)
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Chưa có lượt tải nào",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hãy dán link video TikTok hoặc Facebook để bắt đầu tải nhé!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .layerBackdrop(contentBackdrop)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(116.dp))
                }
                items(historyList, key = { it.downloadId }) { item ->
                    HistoryItemCard(
                        record = item,
                        backdrop = backdrop,
                        onOpen = { downloadHelper.openDownloadsFolder() }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }

        // Shared Progressive Blur Header (Mờ dần, no icon)
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Lịch sử tải về",
            subtitle = "${historyList.size} tệp đã lưu",
            isDark = isDark,
            actions = {
                // Open Downloads Folder Button
                LiquidRoundButton(
                    onClick = { downloadHelper.openDownloadsFolder() },
                    backdrop = contentBackdrop,
                    size = 40.dp,
                    surfaceColor = if (isDark) Color.White.copy(0.18f) else Color.White.copy(0.75f)
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.Folder,
                        contentDescription = "Open Folder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear History Button
                if (historyList.isNotEmpty()) {
                    LiquidRoundButton(
                        onClick = {
                            downloadHelper.clearHistory()
                            historyList = emptyList()
                        },
                        backdrop = contentBackdrop,
                        size = 40.dp,
                        surfaceColor = Color(0xFFEF4444).copy(alpha = 0.2f)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Filled.Trash,
                            contentDescription = "Clear History",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )
    }
}

/**
 * Standard card for single history item.
 */
@Composable
private fun HistoryItemCard(
    record: DownloadRecord,
    backdrop: Backdrop,
    onOpen: () -> Unit
) {
    val dateText = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
            ) {
                if (record.coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = record.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (record.fileExtension == "mp3") CupertinoIcons.Outlined.MusicNote else CupertinoIcons.Filled.Video,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${record.author} • ${record.formatTitle}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Liquid Round Button to Play / Open
            LiquidRoundButton(
                onClick = onOpen,
                backdrop = backdrop,
                size = 40.dp,
                surfaceColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = CupertinoIcons.Filled.Play,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
