package com.buwin.tiktokvideodownload.ui.components.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.editor.VideoEditorModal
import com.buwin.tiktokvideodownload.ui.components.dialog.AppConfirmationModal
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.ArrowDownCircle
import io.github.alexzhirkevich.cupertino.icons.filled.Pause
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.ClockArrowCirclepath
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Main in-app video and audio player modal coordinator.
 * Composes dedicated modular components: PlayerTopBar, PlayerBottomBar,
 * PlayerGestureSurface, PlayerHudOverlays, and AudioPlayerContent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppVideoPlayerModal(
    record: DownloadRecord,
    downloadHelper: DownloadManagerHelper,
    onDismiss: () -> Unit,
    thumbnailBounds: Rect? = null,
    onRedownloadClick: ((DownloadRecord) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val mediaBackdrop = rememberLayerBackdrop()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthPx = remember(configuration, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val videoUri = remember(record) { downloadHelper.getDownloadedUri(record) }
    val isAudio = remember(record) { record.fileExtension.lowercase() in listOf("mp3", "m4a", "aac", "wav") }
    val isImage = remember(record) {
        record.fileExtension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic") ||
        record.formatTitle.contains("ảnh", true) ||
        record.formatTitle.contains("photo", true) ||
        record.formatTitle.contains("image", true)
    }

    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    var isPlaying by remember { mutableStateOf(true) }
    var isPrepared by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var showEditorModal by remember { mutableStateOf(false) }
    var showDetailsModal by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isCompleted by remember { mutableStateOf(false) }

    // Screen brightness & system volume state
    val initialBrightness = remember {
        try {
            val cur = activity?.window?.attributes?.screenBrightness ?: -1f
            if (cur >= 0f) cur else {
                val sysVal = android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    128
                )
                (sysVal / 255f).coerceIn(0.05f, 1f)
            }
        } catch (_: Exception) {
            0.5f
        }
    }
    var currentBrightness by remember { mutableFloatStateOf(initialBrightness) }

    val initialVolumeFraction = remember(audioManager) {
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    }
    var currentVolumeFraction by remember { mutableFloatStateOf(initialVolumeFraction) }

    // HUD overlays state
    var showBrightnessHud by remember { mutableStateOf(false) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var showSeekHud by remember { mutableStateOf(false) }
    var targetSeekMs by remember { mutableIntStateOf(0) }
    var seekInitialMs by remember { mutableIntStateOf(0) }

    // Double tap ripple indicators state (YouTube style)
    var showRewindIndicator by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }
    var rewindKey by remember { mutableIntStateOf(0) }
    var forwardKey by remember { mutableIntStateOf(0) }

    var dismissProgress by remember { mutableFloatStateOf(0f) }
    var isBackDismissing by remember { mutableStateOf(false) }
    val backDismissAnim = remember { Animatable(0f) }
    val enterAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        enterAnim.animateTo(1f, spring(0.85f, 420f))
    }

    val handleDismiss: () -> Unit = {
        if (!isBackDismissing) {
            isBackDismissing = true
            coroutineScope.launch {
                if (isLandscape) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                if (!isImage) {
                    backDismissAnim.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                    onDismiss()
                } else {
                    // Image mode triggers Hero exit animation in ZoomableAsyncImage
                    delay(280)
                    onDismiss()
                }
            }
        }
    }

    // Back button handling: exit landscape mode first, otherwise dismiss modal
    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            handleDismiss()
        }
    }

    // Auto-hide controls after 3.5 seconds of playing
    LaunchedEffect(showControls, isPlaying, isAudio, isImage, showSeekHud) {
        if (!isAudio && !isImage && showControls && isPlaying && !isCompleted && !showSeekHud) {
            delay(3500)
            showControls = false
        }
    }

    // Sync playback position to update scrubber
    LaunchedEffect(isPrepared, isPlaying) {
        while (isPrepared && isPlaying) {
            videoViewRef?.let { vv ->
                try {
                    currentPositionMs = vv.currentPosition
                } catch (_: Exception) {
                }
            }
            delay(200)
        }
    }

    // Sync Mute state to MediaPlayer
    LaunchedEffect(isMuted, mediaPlayerRef) {
        mediaPlayerRef?.let { mp ->
            try {
                val vol = if (isMuted) 0f else 1f
                mp.setVolume(vol, vol)
            } catch (_: Exception) {
            }
        }
    }

    // Double tap rewind indicator auto-hide
    LaunchedEffect(rewindKey) {
        if (rewindKey > 0) {
            showRewindIndicator = true
            delay(650)
            showRewindIndicator = false
        }
    }

    // Double tap forward indicator auto-hide
    LaunchedEffect(forwardKey) {
        if (forwardKey > 0) {
            showForwardIndicator = true
            delay(650)
            showForwardIndicator = false
        }
    }

    // Restore screen orientation and clear brightness override when closing
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
            }
        }
    }

    fun togglePlayPause() {
        videoViewRef?.let { vv ->
            if (isCompleted) {
                vv.seekTo(0)
                vv.start()
                isPlaying = true
                isCompleted = false
            } else if (isPlaying) {
                vv.pause()
                isPlaying = false
            } else {
                vv.start()
                isPlaying = true
            }
        }
    }

    val handleEdit: () -> Unit = {
        if (isImage) {
            val editIntent = Intent(Intent.ACTION_EDIT).apply {
                setDataAndType(videoUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            try {
                context.startActivity(Intent.createChooser(editIntent, "Chỉnh sửa ảnh"))
            } catch (_: Exception) {
                showEditorModal = true
            }
        } else {
            videoViewRef?.pause()
            isPlaying = false
            showEditorModal = true
        }
    }

    var isFavorite by remember(record.id) { mutableStateOf(false) }

    val handleShare: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = when {
                isAudio -> "audio/*"
                isImage -> "image/*"
                else -> "video/*"
            }
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                when {
                    isAudio -> "Chia sẻ âm thanh"
                    isImage -> "Chia sẻ hình ảnh"
                    else -> "Chia sẻ video"
                }
            )
        )
    }

    val handleDelete: () -> Unit = {
        try {
            downloadHelper.deleteRecord(record, videoUri)
            AppToast.showSuccess("Đã xóa tệp", record.title)
            handleDismiss()
        } catch (e: Exception) {
            AppToast.showError("Không thể xóa", e.localizedMessage ?: "Lỗi quyền truy cập")
        }
    }

    val totalDismissFactor = (dismissProgress + backDismissAnim.value).coerceIn(0f, 1f)
    val bgAlpha = (enterAnim.value * (1f - totalDismissFactor)).coerceIn(0f, 1f)
    val controlsAlpha = (enterAnim.value * (1f - totalDismissFactor * 2.5f)).coerceIn(0f, 1f)

    // Dynamic Video geometry calculations if thumbnailBounds is present
    val videoScale = if (!isImage && thumbnailBounds != null && screenWidthPx > 0f) {
        val targetScale = (thumbnailBounds.width / screenWidthPx).coerceIn(0.12f, 0.95f)
        targetScale + (1f - targetScale) * enterAnim.value * (1f - backDismissAnim.value)
    } else {
        (1f - backDismissAnim.value * 0.35f).coerceAtLeast(0.4f)
    }

    val videoOffsetX = if (!isImage && thumbnailBounds != null) {
        val targetOffsetX = thumbnailBounds.center.x - screenWidthPx / 2f
        targetOffsetX * (backDismissAnim.value + (1f - enterAnim.value))
    } else 0f

    val videoOffsetY = if (!isImage && thumbnailBounds != null) {
        val targetOffsetY = thumbnailBounds.center.y - screenHeightPx / 2f
        targetOffsetY * (backDismissAnim.value + (1f - enterAnim.value))
    } else 0f

    val videoCornerRadius = if (!isImage && thumbnailBounds != null) {
        (3f * (backDismissAnim.value + (1f - enterAnim.value))).coerceAtLeast(0f).dp
    } else 0.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha))
    ) {
        if (videoUri == null) {
            // Error: File not found state
            FileNotFoundErrorView(
                isAudio = isAudio,
                record = record,
                downloadHelper = downloadHelper,
                onDismiss = handleDismiss,
                onRedownloadClick = onRedownloadClick
            )
        } else {
            // Main Media Player Area
            Box(
                modifier = Modifier
                    .layerBackdrop(mediaBackdrop)
                    .fillMaxSize()
                    .then(
                        if (!isImage) {
                            Modifier
                                .graphicsLayer {
                                    scaleX = videoScale
                                    scaleY = videoScale
                                    translationX = videoOffsetX
                                    translationY = videoOffsetY
                                    alpha = (enterAnim.value * (1f - backDismissAnim.value)).coerceIn(0f, 1f)
                                }
                                .clip(RoundedCornerShape(videoCornerRadius))
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Native Android VideoView or Zoomable Full Screen Image
                if (isImage) {
                    ZoomableAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(videoUri ?: record.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = record.title,
                        thumbnailBounds = thumbnailBounds,
                        isExiting = isBackDismissing,
                        onSingleTap = { showControls = !showControls },
                        onSwipeUp = { showDetailsModal = true },
                        onDismissProgress = { progress ->
                            dismissProgress = progress
                        },
                        onSwipeDown = onDismiss,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                setVideoURI(videoUri)
                                setOnPreparedListener { mp ->
                                    mediaPlayerRef = mp
                                    isPrepared = true
                                    totalDurationMs = mp.duration
                                    mp.isLooping = false
                                    if (isMuted) {
                                        mp.setVolume(0f, 0f)
                                    }
                                    start()
                                    isPlaying = true
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    isCompleted = true
                                    showControls = true
                                }
                                setOnErrorListener { _, _, _ ->
                                    true
                                }
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dedicated Audio MP3 visualizer content
                if (isAudio) {
                    AudioPlayerContent(
                        coverUrl = record.coverUrl,
                        title = record.title,
                        author = record.author,
                        formatTitle = record.formatTitle,
                        isPlaying = isPlaying,
                        isCompleted = isCompleted,
                        onTogglePlayPause = ::togglePlayPause
                    )
                }

                // Interactive Touch Gestures Surface (Volume, Brightness, Seek, Double Tap 5s)
                PlayerGestureSurface(
                    totalDurationMs = totalDurationMs,
                    currentPositionMs = currentPositionMs,
                    enabled = !isAudio && !isImage,
                    onBrightnessDelta = { step ->
                        val newBrightness = (currentBrightness + step).coerceIn(0.01f, 1f)
                        currentBrightness = newBrightness
                        activity?.window?.let { win ->
                            val lp = win.attributes
                            lp.screenBrightness = newBrightness
                            win.attributes = lp
                        }
                        showBrightnessHud = true
                    },
                    onBrightnessEnd = {
                        coroutineScope.launch {
                            delay(1000)
                            showBrightnessHud = false
                        }
                    },
                    onVolumeDelta = { step ->
                        val newVolFrac = (currentVolumeFraction + step).coerceIn(0f, 1f)
                        currentVolumeFraction = newVolFrac
                        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                        val targetVol = (newVolFrac * maxVol).roundToInt().coerceIn(0, maxVol)
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                        showVolumeHud = true
                    },
                    onVolumeEnd = {
                        coroutineScope.launch {
                            delay(1000)
                            showVolumeHud = false
                        }
                    },
                    onSeekScrubStart = { initMs ->
                        seekInitialMs = initMs
                        targetSeekMs = initMs
                        showSeekHud = true
                    },
                    onSeekScrubDelta = { accX, screenWidth ->
                        if (totalDurationMs > 0 && screenWidth > 0) {
                            val spanMs = 90_000f.coerceAtMost(totalDurationMs.toFloat())
                            val offsetMs = (accX / screenWidth * spanMs).toInt()
                            targetSeekMs = (seekInitialMs + offsetMs).coerceIn(0, totalDurationMs)
                            showSeekHud = true
                        }
                    },
                    onSeekScrubEnd = {
                        videoViewRef?.seekTo(targetSeekMs)
                        currentPositionMs = targetSeekMs
                        coroutineScope.launch {
                            delay(600)
                            showSeekHud = false
                        }
                    },
                    onDoubleTapRewind = {
                        val newPos = (currentPositionMs - 5000).coerceAtLeast(0)
                        videoViewRef?.seekTo(newPos)
                        currentPositionMs = newPos
                        rewindKey++
                    },
                    onDoubleTapForward = {
                        val newPos = (currentPositionMs + 5000).coerceAtMost(totalDurationMs)
                        videoViewRef?.seekTo(newPos)
                        currentPositionMs = newPos
                        forwardKey++
                    },
                    onSingleTap = {
                        showControls = !showControls
                    }
                )

                // Buffering Spinner
                if (!isPrepared && !isImage) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Center Play/Pause Floating Action (for video only)
                AnimatedVisibility(
                    visible = !isAudio && !isImage && (showControls || !isPlaying || isCompleted),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isCompleted -> CupertinoIcons.Outlined.ClockArrowCirclepath
                                isPlaying -> CupertinoIcons.Filled.Pause
                                else -> CupertinoIcons.Filled.Play
                            },
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Double Tap ±5s Indicators
                DoubleTapSeekIndicator(
                    visible = showRewindIndicator,
                    isForward = false,
                    isLandscape = isLandscape,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                DoubleTapSeekIndicator(
                    visible = showForwardIndicator,
                    isForward = true,
                    isLandscape = isLandscape,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )

                // HUD Overlays (Brightness, Volume, Seek)
                BrightnessHud(
                    visible = showBrightnessHud,
                    brightness = currentBrightness,
                    modifier = Modifier.align(Alignment.Center)
                )
                VolumeHud(
                    visible = showVolumeHud,
                    volumeFraction = currentVolumeFraction,
                    modifier = Modifier.align(Alignment.Center)
                )
                SeekScrubHud(
                    visible = showSeekHud,
                    targetSeekMs = targetSeekMs,
                    initialSeekMs = seekInitialMs,
                    totalDurationMs = totalDurationMs,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Top Header Bar
            AnimatedVisibility(
                visible = if (isAudio) true else (showControls && dismissProgress < 0.05f),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = controlsAlpha }
            ) {
                PlayerTopBar(
                    backdrop = mediaBackdrop,
                    isMuted = isMuted,
                    isLandscape = isLandscape,
                    showMute = !isImage && !isAudio,
                    isImage = isImage,
                    onToggleMute = { isMuted = !isMuted },
                    onDismiss = handleDismiss,
                    onShare = handleShare,
                    onOpenDetails = {
                        showDetailsModal = true
                    },
                    onOpenEditor = if (!isAudio && !isImage) {
                        {
                            videoViewRef?.pause()
                            isPlaying = false
                            showEditorModal = true
                        }
                    } else null
                )
            }

            // Bottom Player Control Bar
            AnimatedVisibility(
                visible = if (isAudio) true else (!isImage && showControls && dismissProgress < 0.05f),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = controlsAlpha }
            ) {
                PlayerBottomBar(
                    currentPositionMs = currentPositionMs,
                    totalDurationMs = totalDurationMs,
                    isAudio = isAudio,
                    isMuted = isMuted,
                    isLandscape = isLandscape,
                    onSeek = { targetMs ->
                        currentPositionMs = targetMs
                        videoViewRef?.seekTo(targetMs)
                    },
                    onToggleMute = { isMuted = !isMuted },
                    onToggleOrientation = {
                        val targetOrientation = if (isLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                        activity?.requestedOrientation = targetOrientation
                    }
                )
            }

            // Bottom Action Bar for Images: Authentic Apple Photos style bar
            AnimatedVisibility(
                visible = isImage && showControls && dismissProgress < 0.05f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = controlsAlpha }
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                PlayerImageBottomBar(
                    backdrop = mediaBackdrop,
                    isFavorite = isFavorite,
                    onToggleFavorite = {
                        isFavorite = !isFavorite
                        if (isFavorite) {
                            AppToast.showSuccess("Đã thêm vào mục yêu thích")
                        } else {
                            AppToast.showInfo("Đã xóa khỏi mục yêu thích")
                        }
                    },
                    onOpenDetails = { showDetailsModal = true },
                    onEdit = handleEdit,
                    onShare = handleShare,
                    onDelete = { showDeleteConfirmDialog = true }
                )
            }

            // Video Editor Modal
            if (showEditorModal) {
                VideoEditorModal(
                    record = record,
                    downloadHelper = downloadHelper,
                    onDismiss = {
                        showEditorModal = false
                        videoViewRef?.start()
                        isPlaying = true
                    },
                    onExportSuccess = {
                        showEditorModal = false
                    }
                )
            }

            // Media Details Bottom Sheet (revealed on swipe-up or info button click)
            if (showDetailsModal) {
                MediaDetailsBottomSheet(
                    record = record,
                    isImage = isImage,
                    onDismissRequest = { showDetailsModal = false },
                    onShare = handleShare
                )
            }

            // Confirm Delete Dialog
            AppConfirmationModal(
                visible = showDeleteConfirmDialog,
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = "Xóa tệp này?",
                message = "Tệp \"${record.title}\" sẽ bị xóa vĩnh viễn khỏi thiết bị.",
                confirmText = "Xóa tệp",
                cancelText = "Hủy",
                isDestructive = true,
                onConfirm = {
                    showDeleteConfirmDialog = false
                    handleDelete()
                }
            )
        }
    }
}

/**
 * File not found fallback view when the local downloaded file has been deleted or moved.
 */
@Composable
private fun FileNotFoundErrorView(
    isAudio: Boolean,
    record: DownloadRecord,
    downloadHelper: DownloadManagerHelper,
    onDismiss: () -> Unit,
    onRedownloadClick: ((DownloadRecord) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = CupertinoIcons.Outlined.Xmark,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAudio) "Không tìm thấy tệp âm thanh" else "Không tìm thấy tệp video",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tệp có thể đã bị di chuyển hoặc xóa khỏi bộ nhớ máy.",
            color = Color.LightGray,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "Đóng",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                onClick = { downloadHelper.openDownloadsFolder() },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "Mở thư mục",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Medium
                )
            }
            if (onRedownloadClick != null) {
                Surface(
                    onClick = {
                        onDismiss()
                        onRedownloadClick(record)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Filled.ArrowDownCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tải lại",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Traverses context wrappers to find the parent Activity.
 */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
