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
import androidx.compose.ui.geometry.Rect
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
import io.github.alexzhirkevich.cupertino.icons.filled.ArrowDownCircle
import com.buwin.tiktokvideodownload.ui.navigation.MainTabContainer
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import com.buwin.tiktokvideodownload.data.service.FacebookService
import com.buwin.tiktokvideodownload.ui.components.download.ReDownloadFormatModal
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.BackgroundDark
import com.buwin.tiktokvideodownload.ui.theme.BackgroundLight
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.buwin.tiktokvideodownload.ui.theme.TiktokVideoDownloadTheme
import android.os.Build
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.buwin.tiktokvideodownload.ui.components.editor.VideoEditorModal
import com.buwin.tiktokvideodownload.ui.components.player.InAppVideoPlayerModal
import com.buwin.tiktokvideodownload.ui.components.dialog.AppConfirmationModal
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.buwin.tiktokvideodownload.ui.components.toast.WaterdropToast
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class MainActivity : ComponentActivity() {

    private val tiktokService by lazy { TikTokService() }
    private val downloadHelper by lazy { DownloadManagerHelper(this) }
    private val themePreferences by lazy { ThemePreferences(this) }
    private val authManager by lazy { com.buwin.tiktokvideodownload.data.auth.AuthManager(this) }
    private var sharedUrlState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        // Configure global Coil ImageLoader with VideoFrameDecoder for video thumbnails
        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
        coil.Coil.setImageLoader(imageLoader)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

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
                    authManager = authManager,
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
    authManager: com.buwin.tiktokvideodownload.data.auth.AuthManager,
    isDark: Boolean,
    sharedUrl: String?
) {
    val rootBackdrop = rememberLayerBackdrop()
    var selectedTab by remember { mutableIntStateOf(0) }
    var activePlayingRecord by remember { mutableStateOf<DownloadRecord?>(null) }
    var activeThumbnailBounds by remember { mutableStateOf<Rect?>(null) }
    var activeEditingRecord by remember { mutableStateOf<DownloadRecord?>(null) }
    var pendingCancelDownloadId by remember { mutableStateOf<Long?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val facebookService = remember { FacebookService() }
    var missingFileRecord by remember { mutableStateOf<DownloadRecord?>(null) }
    var reDownloadInfo by remember { mutableStateOf<TikTokVideoInfo?>(null) }

    fun startReDownloadFlow(record: DownloadRecord) {
        val targetUrl = downloadHelper.getResolvableVideoUrl(record)
        if (targetUrl.isEmpty()) {
            AppToast.showError(
                "Không tìm thấy liên kết",
                "Bản ghi này không có liên kết gốc để tải lại"
            )
            return
        }
        coroutineScope.launch {
            AppToast.showInfo(
                "Đang lấy dữ liệu video...",
                "Vui lòng chờ trong giây lát"
            )
            val result = tiktokService.fetchVideoInfo(targetUrl)
            result.fold(
                onSuccess = { info ->
                    reDownloadInfo = info
                },
                onFailure = { err ->
                    AppToast.showError(
                        "Lỗi kết nối",
                        err.localizedMessage ?: "Không thể lấy thông tin định dạng video"
                    )
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tier 1: Main Tab Navigator (Home, Gallery, History, Settings + Persistent Bottom Bar)
        MainTabContainer(
            tiktokService = tiktokService,
            downloadHelper = downloadHelper,
            themePreferences = themePreferences,
            authManager = authManager,
            isDark = isDark,
            sharedUrl = sharedUrl,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onOpenPlayer = { record, bounds ->
                val uri = downloadHelper.getDownloadedUri(record)
                if (uri == null) {
                    missingFileRecord = record
                } else {
                    activeThumbnailBounds = bounds
                    activePlayingRecord = record
                }
            },
            onOpenEditor = { record ->
                activeEditingRecord = record
            },
            onMissingFile = { record ->
                missingFileRecord = record
            },
            modifier = Modifier.layerBackdrop(rootBackdrop)
        )

        // Authentic iOS Waterdrop Dynamic Island Toast floating over entire app
        WaterdropToast(
            isDark = isDark,
            modifier = Modifier.align(Alignment.TopCenter),
            onClick = { state ->
                selectedTab = 2
                val records = downloadHelper.getHistory()
                val targetRecord = if (state.downloadId > 0) {
                    records.firstOrNull { it.downloadId == state.downloadId } ?: records.firstOrNull()
                } else {
                    records.firstOrNull()
                }
                if (targetRecord != null) {
                    activePlayingRecord = targetRecord
                }
            },
            onCancelDownload = { downloadId ->
                pendingCancelDownloadId = downloadId
            }
        )

        // Universal Confirmation Modal for Cancelling Download
        AppConfirmationModal(
            visible = pendingCancelDownloadId != null,
            onDismissRequest = { pendingCancelDownloadId = null },
            title = if (pendingCancelDownloadId == 0L) "Hủy tất cả tải xuống?" else "Hủy tải xuống?",
            message = if (pendingCancelDownloadId == 0L) {
                "Bạn có chắc chắn muốn hủy toàn bộ các tiến trình tải đang chạy không?"
            } else {
                "Bạn có chắc chắn muốn hủy quá trình tải tệp này không? Tiến trình hiện tại sẽ bị xóa."
            },
            confirmText = "Hủy tải",
            cancelText = "Tiếp tục tải",
            isDestructive = true,
            backdrop = rootBackdrop,
            isDark = isDark,
            onConfirm = {
                pendingCancelDownloadId?.let { id ->
                    downloadHelper.cancelDownload(id)
                }
                pendingCancelDownloadId = null
            }
        )

        // In-App Video & Audio Player Modal
        activePlayingRecord?.let { record ->
            InAppVideoPlayerModal(
                record = record,
                downloadHelper = downloadHelper,
                thumbnailBounds = activeThumbnailBounds,
                onDismiss = {
                    activePlayingRecord = null
                    activeThumbnailBounds = null
                },
                onRedownloadClick = { targetRecord ->
                    startReDownloadFlow(targetRecord)
                }
            )
        }

        // Real-Time Video & Photo Editor Modal
        activeEditingRecord?.let { record ->
            VideoEditorModal(
                record = record,
                downloadHelper = downloadHelper,
                onDismiss = { activeEditingRecord = null },
                onExportSuccess = {
                    activeEditingRecord = null
                    AppToast.showSuccess(
                        "Xuất tệp thành công",
                        "Tệp mới đã được lưu vào bộ sưu tập"
                    )
                }
            )
        }

        // Modal Confirmation for Re-downloading Missing File
        AppConfirmationModal(
            visible = missingFileRecord != null,
            onDismissRequest = { missingFileRecord = null },
            title = "Tệp không còn trên máy",
            message = "Tệp '${missingFileRecord?.title ?: ""}' đã bị xóa hoặc di chuyển khỏi bộ nhớ máy. Bạn có muốn tải lại video này không?",
            confirmText = "Tải lại video",
            cancelText = "Đóng",
            icon = CupertinoIcons.Filled.ArrowDownCircle,
            iconTint = MaterialTheme.colorScheme.primary,
            backdrop = rootBackdrop,
            isDark = isDark,
            onConfirm = {
                val target = missingFileRecord
                missingFileRecord = null
                if (target != null) {
                    startReDownloadFlow(target)
                }
            }
        )

        // Modal for Choosing Re-download Format
        reDownloadInfo?.let { info ->
            ReDownloadFormatModal(
                info = info,
                backdrop = rootBackdrop,
                isDark = isDark,
                onDismiss = { reDownloadInfo = null },
                onDownloadOption = { option ->
                    downloadHelper.enqueueDownload(info, option)
                    reDownloadInfo = null
                    AppToast.showSuccess(
                        "Bắt đầu tải lại",
                        "${info.title} (${option.title})"
                    )
                }
            )
        }
    }
}