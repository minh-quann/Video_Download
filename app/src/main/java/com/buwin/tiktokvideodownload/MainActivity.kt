package com.buwin.tiktokvideodownload

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.service.TikTokService
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Gearshape
import io.github.alexzhirkevich.cupertino.icons.filled.House
import io.github.alexzhirkevich.cupertino.icons.outlined.ClockArrowCirclepath
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidBottomTab
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidBottomTabs
import com.buwin.tiktokvideodownload.ui.screens.HistoryScreen
import com.buwin.tiktokvideodownload.ui.screens.HomeScreen
import com.buwin.tiktokvideodownload.ui.screens.SettingsScreen
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.BackgroundDark
import com.buwin.tiktokvideodownload.ui.theme.BackgroundLight
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.buwin.tiktokvideodownload.ui.theme.TiktokVideoDownloadTheme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class MainActivity : ComponentActivity() {

    private val tiktokService by lazy { TikTokService() }
    private val downloadHelper by lazy { DownloadManagerHelper(this) }
    private val themePreferences by lazy { ThemePreferences(this) }
    private var sharedUrlState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val isDark = when (themePreferences.currentThemeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            TiktokVideoDownloadTheme(darkTheme = isDark) {
                MainApp(
                    tiktokService = tiktokService,
                    downloadHelper = downloadHelper,
                    themePreferences = themePreferences,
                    isDark = isDark,
                    sharedUrl = sharedUrlState
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                sharedUrlState = tiktokService.extractUrl(sharedText) ?: sharedText
            }
        }
    }
}

@Composable
fun MainApp(
    tiktokService: TikTokService,
    downloadHelper: DownloadManagerHelper,
    themePreferences: ThemePreferences,
    isDark: Boolean,
    sharedUrl: String?
) {
    val screenBackdrop = rememberLayerBackdrop()
    val localBackdrop = rememberLayerBackdrop()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Preserve HomeScreen state when switching tabs
    var homeInputUrl by remember { mutableStateOf(sharedUrl ?: "") }
    var homeIsLoading by remember { mutableStateOf(false) }
    var homeVideoInfo by remember { mutableStateOf<com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo?>(null) }
    val homeScrollState = androidx.compose.foundation.rememberScrollState()

    androidx.compose.runtime.LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            homeInputUrl = sharedUrl
        }
    }

    val backgroundColor = if (isDark) BackgroundDark else BackgroundLight

    Box(modifier = Modifier.fillMaxSize()) {
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
                    inputUrl = homeInputUrl,
                    onInputUrlChange = { homeInputUrl = it },
                    isLoading = homeIsLoading,
                    onIsLoadingChange = { homeIsLoading = it },
                    videoInfo = homeVideoInfo,
                    onVideoInfoChange = { homeVideoInfo = it },
                    scrollState = homeScrollState,
                    sharedUrl = sharedUrl,
                    backdrop = localBackdrop
                )
                1 -> HistoryScreen(
                    backdrop = localBackdrop,
                    downloadHelper = downloadHelper
                )
                2 -> SettingsScreen(
                    backdrop = localBackdrop,
                    themePreferences = themePreferences,
                    downloadHelper = downloadHelper
                )
            }
        }

        val contentColor = if (isDark) Color.White else Color.Black

        // Authentic Liquid Glass Bottom Tabs directly from Kyant0/AndroidLiquidGlass
        LiquidBottomTabs(
            selectedTabIndex = { selectedTab },
            onTabSelected = { selectedTab = it },
            backdrop = screenBackdrop,
            tabsCount = 3,
            isDark = isDark,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            // Tab 1: Home with iOS SF Symbol House
            LiquidBottomTab({ selectedTab = 0 }) {
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

            // Tab 2: History with iOS SF Symbol Clock/History
            LiquidBottomTab({ selectedTab = 1 }) {
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

            // Tab 3: Settings with iOS SF Symbol Gear
            LiquidBottomTab({ selectedTab = 2 }) {
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