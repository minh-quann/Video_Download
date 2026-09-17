package com.buwin.tiktokvideodownload.ui.components.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo

/**
 * Complete in-place section containing video preview card and organized download action options.
 */
@Composable
fun VideoDownloadContent(
    info: TikTokVideoInfo,
    cardBackground: Color,
    cardBorderColor: Color,
    onDownloadOption: (DownloadOption) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Primary Action: Highest quality no-watermark video (Fast Download CTA)
    val primaryOption = info.options.firstOrNull { it.type == DownloadFormatType.VIDEO_HD_NO_WATERMARK }
        ?: info.options.firstOrNull { it.type == DownloadFormatType.VIDEO_SD_NO_WATERMARK }
        ?: info.options.firstOrNull()

    // 2. Secondary Actions: Other formats (MP3 audio, SD, watermarked, photo slides)
    val secondaryOptions = info.options.filter { it != primaryOption }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Video Info Preview Hero Card ──
        VideoInfoCard(
            info = info,
            cardBackground = cardBackground,
            cardBorderColor = cardBorderColor,
            onDownloadOption = onDownloadOption
        )

        // ── 2. Primary Fast CTA Download Button ──
        if (primaryOption != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tải nhanh khuyên dùng",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )

                HeroDownloadButton(
                    option = primaryOption,
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    onClick = { onDownloadOption(primaryOption) }
                )
            }
        }

        // ── 3. Secondary Options Bento Grid (2 Columns) ──
        if (secondaryOptions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Định dạng khác",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )

                val chunked = secondaryOptions.chunked(2)
                chunked.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowOptions.forEach { option ->
                            SecondaryOptionTile(
                                option = option,
                                cardBackground = cardBackground,
                                cardBorderColor = cardBorderColor,
                                onClick = { onDownloadOption(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number of items
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

