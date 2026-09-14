package com.buwin.tiktokvideodownload.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import com.buwin.tiktokvideodownload.data.service.TikTokService
import com.buwin.tiktokvideodownload.ui.components.download.QuickGuideSection
import com.buwin.tiktokvideodownload.ui.components.download.VideoDownloadContent
import com.buwin.tiktokvideodownload.ui.components.download.VideoPreviewSkeleton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidSearchBar
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidTopBar
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Moon
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.filled.SunMax
import kotlinx.coroutines.launch

/**
 * Main Home screen featuring a pinned Liquid Glass Top Bar, real-time scroll backdrop refraction,
 * and intuitive modern TikTok downloader aesthetics.
 */
@Composable
fun HomeScreen(
    tiktokService: TikTokService,
    downloadHelper: DownloadManagerHelper,
    themePreferences: ThemePreferences,
    inputUrl: String,
    onInputUrlChange: (String) -> Unit,
    isLoading: Boolean,
    onIsLoadingChange: (Boolean) -> Unit,
    videoInfo: TikTokVideoInfo?,
    onVideoInfoChange: (TikTokVideoInfo?) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    sharedUrl: String? = null,
    backdrop: Backdrop = rememberLayerBackdrop(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val isDark = LocalIsDark.current

    // Backdrop layer capturing the scrollable content for the pinned LiquidTopBar
    val contentBackdrop = rememberLayerBackdrop()

    // Clipboard auto-detect state
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val clipText = clipboardManager.getText()?.text
            if (!clipText.isNullOrBlank()) {
                val extracted = tiktokService.extractUrl(clipText)
                if (!extracted.isNullOrBlank()) {
                    detectedClipboardUrl = extracted
                }
            }
        } catch (_: Throwable) {}
    }

    fun processUrl(urlToFetch: String) {
        val target = urlToFetch.trim()
        if (target.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập hoặc dán link TikTok", Toast.LENGTH_SHORT).show()
            return
        }
        onIsLoadingChange(true)
        onVideoInfoChange(null)
        scope.launch {
            val result = tiktokService.fetchVideoInfo(target)
            onIsLoadingChange(false)
            result.onSuccess { info ->
                onVideoInfoChange(info)
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Không thể bóc tách video. Vui lòng kiểm tra lại link.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank() && sharedUrl != inputUrl) {
            onInputUrlChange(sharedUrl)
            processUrl(sharedUrl)
        }
    }

    val cardBackground = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF0F0F2)

    Box(modifier = modifier.fillMaxSize()) {
        // ── 1. Scrollable Content Layer (Captured into contentBackdrop) ──
        Column(
            modifier = Modifier
                .layerBackdrop(contentBackdrop)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer allowing content to start cleanly below the pinned top bar
            Spacer(modifier = Modifier.height(128.dp))

            // Unified iOS 26 Liquid Glass Search Bar
            LiquidSearchBar(
                value = inputUrl,
                onValueChange = onInputUrlChange,
                backdrop = backdrop,
                placeholder = "Dán link video TikTok tại đây...",
                isLoading = isLoading,
                onSearch = { processUrl(inputUrl) },
                onPaste = {
                    val clip = clipboardManager.getText()?.text
                    if (!clip.isNullOrBlank()) {
                        val extracted = tiktokService.extractUrl(clip) ?: clip
                        onInputUrlChange(extracted)
                        processUrl(extracted)
                    } else {
                        Toast.makeText(context, "Bộ nhớ tạm rỗng", Toast.LENGTH_SHORT).show()
                    }
                },
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── 2. Content Sections: Loading Skeleton vs Video Card vs Welcome Hub ──
            if (isLoading) {
                VideoPreviewSkeleton(
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    isDark = isDark
                )
            } else if (videoInfo != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 30 })
                ) {
                    VideoDownloadContent(
                        info = videoInfo!!,
                        cardBackground = cardBackground,
                        cardBorderColor = cardBorderColor,
                        onDownloadOption = { option ->
                            downloadHelper.enqueueDownload(videoInfo!!, option)
                            Toast.makeText(
                                context,
                                "Bắt đầu tải: ${option.title}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            } else {
                // Modern Welcome & Feature Hub when no link is loaded yet
                QuickGuideSection(
                    clipboardUrl = detectedClipboardUrl,
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    onUseClipboardUrl = { url ->
                        onInputUrlChange(url)
                        processUrl(url)
                    }
                )
            }

            // Bottom clearance for the floating Liquid Bottom Tabs
            Spacer(modifier = Modifier.height(120.dp))
        }

        // ── 2. Pinned Sticky Liquid Glass Top Bar (Fixed on top, blurs scrolling content underneath) ──
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            isDark = isDark,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Modern TikTok Neon Brand Mark Box
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00F2FE),
                                        Color(0xFF4FACFE),
                                        Color(0xFFFE2C55)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Filled.Play,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TikTok Downloader",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 0.2.sp
                            )
                            // Glowing PRO Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF007AFF), Color(0xFF00C6FF))
                                        )
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "Tải video HD không logo & MP3",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                // Liquid Round Theme Toggle Button
                LiquidRoundButton(
                    onClick = {
                        val next = when (themePreferences.currentThemeMode) {
                            AppThemeMode.DARK -> AppThemeMode.LIGHT
                            AppThemeMode.LIGHT -> AppThemeMode.DARK
                            AppThemeMode.SYSTEM -> if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                        }
                        themePreferences.setThemeMode(next)
                    },
                    backdrop = contentBackdrop,
                    size = 40.dp,
                    surfaceColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.70f)
                ) {
                    Icon(
                        imageVector = if (isDark) CupertinoIcons.Filled.SunMax else CupertinoIcons.Filled.Moon,
                        contentDescription = "Theme",
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
}
