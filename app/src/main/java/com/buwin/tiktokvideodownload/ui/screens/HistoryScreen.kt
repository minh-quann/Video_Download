package com.buwin.tiktokvideodownload.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Represents a collection of downloaded records grouped by calendar date.
 */
private data class HistoryDateGroup(
    val dateKey: String,
    val dateTitle: String,
    val items: List<DownloadRecord>
)

/**
 * Screen displaying the history of downloaded TikTok and Facebook media files,
 * grouped by date with 26dp rounded corner cards and date headers outside the cards.
 */
@Composable
fun HistoryScreen(
    backdrop: Backdrop,
    downloadHelper: DownloadManagerHelper,
    onPlayRecord: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var historyList by remember { mutableStateOf(downloadHelper.getHistory()) }
    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()

    // Card styling matching iOS Inset Grouped / Liquid design
    val cardBackground = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF0F0F2)
    val dividerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)

    // Refresh history list whenever screen becomes active
    LaunchedEffect(Unit) {
        historyList = downloadHelper.getHistory()
    }

    // Group items chronologically by date
    val groupedHistory = remember(historyList) {
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val todayKey = dayFormat.format(todayCal.time)
        val yesterdayKey = dayFormat.format(yesterdayCal.time)

        historyList
            .groupBy { record ->
                if (record.timestamp > 0) {
                    dayFormat.format(Date(record.timestamp))
                } else {
                    todayKey
                }
            }
            .map { (dateKey, items) ->
                val firstTimestamp = items.firstOrNull { it.timestamp > 0 }?.timestamp ?: System.currentTimeMillis()
                val fullDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(firstTimestamp))
                val title = when (dateKey) {
                    todayKey -> "Hôm nay • $fullDateStr"
                    yesterdayKey -> "Hôm qua • $fullDateStr"
                    else -> fullDateStr
                }
                HistoryDateGroup(
                    dateKey = dateKey,
                    dateTitle = title,
                    items = items
                )
            }
    }

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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "header_clearance") {
                    Spacer(modifier = Modifier.height(116.dp))
                }

                items(groupedHistory, key = { it.dateKey }) { group ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Date header outside the card
                        Text(
                            text = group.dateTitle,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )

                        // Large grouped card with 26dp rounded corners
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            border = BorderStroke(1.dp, cardBorderColor)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                group.items.forEachIndexed { index, item ->
                                    HistoryItemRow(
                                        record = item,
                                        backdrop = backdrop,
                                        onOpen = { onPlayRecord(item) },
                                        isDark = isDark
                                    )
                                    if (index < group.items.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                                            thickness = 0.6.dp,
                                            color = dividerColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "footer_clearance") {
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
 * Standard item row inside the 26dp grouped card.
 */
@Composable
private fun HistoryItemRow(
    record: DownloadRecord,
    backdrop: Backdrop,
    onOpen: () -> Unit,
    isDark: Boolean
) {
    val timeText = remember(record.timestamp) {
        if (record.timestamp > 0) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(record.timestamp))
        } else {
            ""
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center
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
                    tint = if (isDark) Color.White else Color(0xFF4B5563),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${record.author} • ${record.formatTitle}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (timeText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeText,
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Liquid Round Button to Play / Open
        LiquidRoundButton(
            onClick = onOpen,
            backdrop = backdrop,
            size = 38.dp,
            surfaceColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.15f)
        ) {
            Icon(
                imageVector = CupertinoIcons.Filled.Play,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
