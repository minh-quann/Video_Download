package com.buwin.tiktokvideodownload.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.service.TikTokService
import com.buwin.tiktokvideodownload.ui.components.download.QuickGuideSection
import com.buwin.tiktokvideodownload.ui.components.download.VideoDownloadContent
import com.buwin.tiktokvideodownload.ui.components.download.VideoPreviewSkeleton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidSearchBar
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidTopBar
import com.buwin.tiktokvideodownload.ui.screens.home.components.HomeThemeButton
import com.buwin.tiktokvideodownload.ui.screens.home.viewmodel.HomeViewModel
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Main Home screen featuring Liquid Glass Search Bar, pinned Liquid Glass Top Bar,
 * progressive blur backdrop refraction, and intuitive TikTok/Facebook downloader UI.
 *
 * Refactored to follow Android Clean Architecture & MVVM.
 */
@Composable
fun HomeScreen(
    tiktokService: TikTokService,
    downloadHelper: DownloadManagerHelper,
    themePreferences: ThemePreferences,
    modifier: Modifier = Modifier,
    sharedUrl: String? = null,
    backdrop: Backdrop = rememberLayerBackdrop(),
    scrollState: ScrollState = rememberScrollState(),
    viewModel: HomeViewModel = remember {
        HomeViewModel(
            tiktokService = tiktokService,
            downloadHelper = downloadHelper,
            themePreferences = themePreferences
        )
    }
) {
    val clipboardManager = LocalClipboardManager.current
    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()
    val uiState by viewModel.uiState.collectAsState()

    // Inspect clipboard for supported media link on initial launch
    LaunchedEffect(Unit) {
        val clipText = clipboardManager.getText()?.text
        viewModel.checkClipboard(clipText)
    }

    // React to incoming shared URL from external apps
    LaunchedEffect(sharedUrl) {
        viewModel.handleSharedUrl(sharedUrl)
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
            // Clearance for pinned progressive blur top bar
            Spacer(modifier = Modifier.height(116.dp))

            // Unified iOS 26 Liquid Glass Search Bar
            LiquidSearchBar(
                value = uiState.inputUrl,
                onValueChange = { viewModel.onInputUrlChange(it) },
                backdrop = backdrop,
                placeholder = "Dán link video TikTok hoặc Facebook tại đây...",
                isLoading = uiState.isLoading,
                onSearch = { viewModel.processUrl() },
                onPaste = {
                    val clipText = clipboardManager.getText()?.text
                    viewModel.onPasteClipboard(clipText)
                },
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ── 2. Content Sections: Loading Skeleton vs Video Card vs Welcome Hub ──
            if (uiState.isLoading) {
                VideoPreviewSkeleton(
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    isDark = isDark
                )
            } else if (uiState.videoInfo != null) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 30 })
                ) {
                    VideoDownloadContent(
                        info = uiState.videoInfo!!,
                        cardBackground = cardBackground,
                        cardBorderColor = cardBorderColor,
                        onDownloadOption = { option ->
                            viewModel.enqueueDownload(option)
                        }
                    )
                }
            } else {
                // Modern Welcome & Feature Hub when no link is loaded yet
                QuickGuideSection(
                    clipboardUrl = uiState.detectedClipboardUrl,
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    onUseClipboardUrl = { url ->
                        viewModel.onInputUrlChange(url)
                        viewModel.processUrl(url)
                    }
                )
            }

            // Bottom clearance for floating Liquid Bottom Tabs
            Spacer(modifier = Modifier.height(120.dp))
        }

        // ── 3. Shared Progressive Blur Liquid Glass Top Bar ──
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Video Downloader",
            subtitle = "Tải video TikTok & Facebook HD không logo",
            isDark = isDark,
            actions = {
                HomeThemeButton(
                    isDark = isDark,
                    onToggle = { viewModel.toggleTheme(isDark) },
                    backdrop = contentBackdrop
                )
            }
        )
    }
}
