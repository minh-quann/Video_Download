package com.buwin.tiktokvideodownload.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.data.auth.AuthManager
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.data.service.TikTokService
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidBottomProgressiveBlur
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidBottomTab
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidBottomTabs
import com.buwin.tiktokvideodownload.ui.screens.HistoryScreen
import com.buwin.tiktokvideodownload.ui.screens.HomeScreen
import com.buwin.tiktokvideodownload.ui.screens.SettingsScreen
import com.buwin.tiktokvideodownload.ui.screens.gallery.GalleryScreen
import com.buwin.tiktokvideodownload.ui.screens.home.viewmodel.HomeViewModel
import com.buwin.tiktokvideodownload.ui.theme.BackgroundDark
import com.buwin.tiktokvideodownload.ui.theme.BackgroundLight
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Gearshape
import io.github.alexzhirkevich.cupertino.icons.filled.House
import io.github.alexzhirkevich.cupertino.icons.filled.PhotoStack
import io.github.alexzhirkevich.cupertino.icons.outlined.ClockArrowCirclepath

/**
 * Tab Navigator container (Tier 1 Architecture).
 * Encapsulates the 4 primary tabs (Home, Gallery, History, Settings)
 * and owns the persistent Liquid Glass Bottom Bar and Progressive Blur.
 * All full-screen operations (Editor, Video Player) are hoisted to Root Navigator.
 */
@Composable
fun MainTabContainer(
    tiktokService: TikTokService,
    downloadHelper: DownloadManagerHelper,
    themePreferences: ThemePreferences,
    authManager: AuthManager,
    isDark: Boolean,
    sharedUrl: String?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onOpenPlayer: (DownloadRecord, Rect?) -> Unit,
    onOpenEditor: (DownloadRecord) -> Unit,
    onMissingFile: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenBackdrop = rememberLayerBackdrop()
    val localBackdrop = rememberLayerBackdrop()
    var isSettingsSubScreenOpen by remember { mutableStateOf(false) }

    val homeViewModel = remember {
        HomeViewModel(
            tiktokService = tiktokService,
            downloadHelper = downloadHelper,
            themePreferences = themePreferences
        )
    }

    val backgroundColor = if (isDark) BackgroundDark else BackgroundLight
    val isBottomBarVisible = !isSettingsSubScreenOpen

    Box(modifier = modifier.fillMaxSize()) {
        // Screen content with background captured into screenBackdrop for true Liquid Glass transparency
        Box(
            modifier = Modifier
                .layerBackdrop(screenBackdrop)
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    tiktokService = tiktokService,
                    downloadHelper = downloadHelper,
                    themePreferences = themePreferences,
                    sharedUrl = sharedUrl,
                    backdrop = localBackdrop,
                    viewModel = homeViewModel
                )
                1 -> GalleryScreen(
                    backdrop = localBackdrop,
                    downloadHelper = downloadHelper,
                    onPlayRecord = onOpenPlayer,
                    onEditRecord = onOpenEditor
                )
                2 -> HistoryScreen(
                    backdrop = localBackdrop,
                    downloadHelper = downloadHelper,
                    authManager = authManager,
                    onPlayRecord = { record -> onOpenPlayer(record, null) }
                )
                3 -> SettingsScreen(
                    backdrop = localBackdrop,
                    themePreferences = themePreferences,
                    downloadHelper = downloadHelper,
                    authManager = authManager,
                    onSubScreenChanged = { isSettingsSubScreenOpen = it }
                )
            }
        }

        // Progressive Blur Footer (Apple-style gradient blur dissolving scrolling content towards bottom edge)
        AnimatedVisibility(
            visible = isBottomBarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LiquidBottomProgressiveBlur(
                backdrop = screenBackdrop,
                isDark = isDark
            )
        }

        val contentColor = if (isDark) Color.White else Color.Black

        // Authentic Liquid Glass Bottom Tabs directly from Kyant0/AndroidLiquidGlass
        AnimatedVisibility(
            visible = isBottomBarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            LiquidBottomTabs(
                selectedTabIndex = { selectedTab },
                onTabSelected = onTabSelected,
                backdrop = screenBackdrop,
                tabsCount = 4,
                isDark = isDark
            ) {
                // Tab 1: Home with iOS SF Symbol House
                LiquidBottomTab({ onTabSelected(0) }) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.House,
                        contentDescription = "Tải video",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    BasicText(
                        text = "Tải video",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                // Tab 2: Gallery with iOS SF Symbol PhotoStack
                LiquidBottomTab({ onTabSelected(1) }) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.PhotoStack,
                        contentDescription = "Thư viện",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    BasicText(
                        text = "Thư viện",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                // Tab 3: History with iOS SF Symbol Clock/History
                LiquidBottomTab({ onTabSelected(2) }) {
                    Icon(
                        imageVector = CupertinoIcons.Default.ClockArrowCirclepath,
                        contentDescription = "Lịch sử",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    BasicText(
                        text = "Lịch sử",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                // Tab 4: Settings with iOS SF Symbol Gear
                LiquidBottomTab({ onTabSelected(3) }) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.Gearshape,
                        contentDescription = "Cài đặt",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    BasicText(
                        text = "Cài đặt",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}
