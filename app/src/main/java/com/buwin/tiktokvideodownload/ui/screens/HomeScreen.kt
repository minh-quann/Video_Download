package com.buwin.tiktokvideodownload.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import com.buwin.tiktokvideodownload.data.service.TikTokService
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidButton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidModal
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidSearchBar
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch

/**
 * Main Home screen for parsing TikTok links, displaying preview cards, and selecting download resolutions.
 */
@Composable
fun HomeScreen(
    backdrop: Backdrop,
    tiktokService: TikTokService,
    downloadHelper: DownloadManagerHelper,
    themePreferences: ThemePreferences,
    sharedUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var inputUrl by remember { mutableStateOf(sharedUrl ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var videoInfo by remember { mutableStateOf<TikTokVideoInfo?>(null) }
    var showDownloadModal by remember { mutableStateOf(false) }

    fun processUrl(urlToFetch: String) {
        val target = urlToFetch.trim()
        if (target.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập hoặc dán link TikTok", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        videoInfo = null
        scope.launch {
            val result = tiktokService.fetchVideoInfo(target)
            isLoading = false
            result.onSuccess { info ->
                videoInfo = info
                showDownloadModal = true // Auto-open liquid download modal
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Không thể bóc tách video. Vui lòng kiểm tra lại link.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TikTok Downloader",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tải video HD không logo & âm thanh MP3",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Theme Mode Toggle Button
                LiquidRoundButton(
                    onClick = {
                        val next = when (themePreferences.currentThemeMode) {
                            AppThemeMode.DARK -> AppThemeMode.LIGHT
                            AppThemeMode.LIGHT -> AppThemeMode.DARK
                            AppThemeMode.SYSTEM -> if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                        }
                        themePreferences.setThemeMode(next)
                    },
                    backdrop = backdrop,
                    size = 40.dp,
                    surfaceColor = if (isDark) Color.White.copy(0.15f) else Color.White.copy(0.7f)
                ) {
                    Icon(
                        imageVector = if (isDark) CupertinoIcons.Filled.SunMax else CupertinoIcons.Filled.Moon,
                        contentDescription = "Chuyển chế độ sáng/tối",
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Unified iOS 26 Liquid Glass Search Bar
            LiquidSearchBar(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                backdrop = backdrop,
                placeholder = "Dán link TikTok tại đây...",
                isLoading = isLoading,
                onSearch = { processUrl(inputUrl) },
                onPaste = {
                    val clip = clipboardManager.getText()?.text
                    if (!clip.isNullOrBlank()) {
                        val extracted = tiktokService.extractUrl(clip) ?: clip
                        inputUrl = extracted
                        processUrl(extracted)
                    } else {
                        Toast.makeText(context, "Bộ nhớ tạm rỗng", Toast.LENGTH_SHORT).show()
                    }
                },
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Standard Preview Card (NO liquid glass on cards as specified)
            AnimatedVisibility(
                visible = videoInfo != null,
                enter = fadeIn() + slideInVertically()
            ) {
                videoInfo?.let { info ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Video Thumbnail
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 10f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = info.coverUrl,
                                    contentDescription = "Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Duration Tag
                                if (info.durationSeconds > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.75f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${info.durationSeconds}s",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Author & Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = info.authorAvatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = info.authorNickname,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${info.authorUsername}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (info.title.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = info.title,
                                    fontSize = 14.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Center Action: Round Liquid Button to Open Download Modal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LiquidRoundButton(
                                    onClick = { showDownloadModal = true },
                                    backdrop = backdrop,
                                    size = 64.dp,
                                    tint = MaterialTheme.colorScheme.primary,
                                    surfaceColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                ) {
                                    Icon(
                                        imageVector = CupertinoIcons.Filled.ArrowDownCircle,
                                        contentDescription = "Open Download Options",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Bấm để chọn định dạng & tải về",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp)) // Leave space for bottom navigation bar
        }

        // Liquid Modal for Resolution and Format Selection
        LiquidModal(
            visible = showDownloadModal && videoInfo != null,
            onDismissRequest = { showDownloadModal = false },
            backdrop = backdrop
        ) {
            videoInfo?.let { info ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chọn định dạng tải về",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Circular Liquid Close Button
                    LiquidRoundButton(
                        onClick = { showDownloadModal = false },
                        backdrop = backdrop,
                        size = 36.dp,
                        surfaceColor = if (isDark) Color.White.copy(0.15f) else Color.Black.copy(0.1f)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.Xmark,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    info.options.forEach { option ->
                        DownloadOptionItem(
                            option = option,
                            backdrop = backdrop,
                            onDownloadClick = {
                                downloadHelper.enqueueDownload(info, option)
                                showDownloadModal = false
                                Toast.makeText(
                                    context,
                                    "Bắt đầu tải: ${option.title}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual format option row inside the Liquid Modal.
 */
@Composable
private fun DownloadOptionItem(
    option: DownloadOption,
    backdrop: Backdrop,
    onDownloadClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val (icon, badgeColor) = when (option.type) {
        DownloadFormatType.VIDEO_HD_NO_WATERMARK -> Pair(CupertinoIcons.Filled.Film, Color(0xFF10B981))
        DownloadFormatType.VIDEO_SD_NO_WATERMARK -> Pair(CupertinoIcons.Filled.Video, Color(0xFF3B82F6))
        DownloadFormatType.VIDEO_WATERMARK -> Pair(CupertinoIcons.Filled.Video, Color(0xFF6B7280))
        DownloadFormatType.AUDIO_MP3 -> Pair(CupertinoIcons.Default.MusicNote, Color(0xFFF59E0B))
        DownloadFormatType.PHOTO_SLIDE -> Pair(CupertinoIcons.Filled.PhotoStack, Color(0xFF8B5CF6))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            .clickable { onDownloadClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(badgeColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = option.title,
                tint = badgeColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = option.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Circular Liquid Button for Download Trigger
        LiquidRoundButton(
            onClick = onDownloadClick,
            backdrop = backdrop,
            size = 40.dp,
            tint = badgeColor,
            surfaceColor = badgeColor.copy(alpha = 0.85f)
        ) {
            Icon(
                imageVector = CupertinoIcons.Filled.ArrowDownCircle,
                contentDescription = "Download",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
