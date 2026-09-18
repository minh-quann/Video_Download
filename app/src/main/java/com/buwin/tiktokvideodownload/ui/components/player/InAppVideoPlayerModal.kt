package com.buwin.tiktokvideodownload.ui.components.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.FrameLayout
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
import com.buwin.tiktokvideodownload.ui.components.editor.ImageEditorModal
import com.buwin.tiktokvideodownload.ui.components.editor.VideoEditorModal
import com.buwin.tiktokvideodownload.ui.components.dialog.AppConfirmationModal
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
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
    var showVideoEditorModal by remember { mutableStateOf(false) }
    var showImageEditorModal by remember { mutableStateOf(false) }
    var showDetailsModal by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer() }
    var textureSurface by remember { mutableStateOf<Surface?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var videoDimensions by remember { mutableStateOf<Pair<Int, Int>>(0 to 0) }
    var isCompleted by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }

    // Initialize & prepare MediaPlayer for hardware-accelerated TextureView rendering
    LaunchedEffect(videoUri) {
        if (videoUri != null && !isImage && !isAudio) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, videoUri)
                mediaPlayer.setOnPreparedListener { mp ->
                    isPrepared = true
                    totalDurationMs = mp.duration
                    videoDimensions = mp.videoWidth to mp.videoHeight
                    mp.isLooping = false
                    mp.setOnSeekCompleteListener { seekMp ->
                        isSeeking = false
                        try {
                            currentPositionMs = seekMp.currentPosition
                        } catch (_: Exception) {}
                    }
                    if (isMuted) {
                        mp.setVolume(0f, 0f)
                    } else {
                        mp.setVolume(1f, 1f)
                    }
                    textureViewRef?.let { tv ->
                        updateTextureAspect(tv, mp.videoWidth, mp.videoHeight)
                    }
                    mp.start()
                    isPlaying = true
                }
                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    isCompleted = true
                    showControls = true
                }
                mediaPlayer.setOnErrorListener { _, _, _ -> true }
                mediaPlayer.prepareAsync()
            } catch (_: Exception) {}
        }
    }

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

    // High-precision seek using SEEK_CLOSEST and async guard to prevent snap-back glitch
    val performSeek: (Int) -> Unit = { targetMs ->
        val clamped = targetMs.coerceIn(0, totalDurationMs)
        currentPositionMs = clamped
        isSeeking = true
        isCompleted = false
        try {
            mediaPlayer.seekTo(clamped.toLong(), MediaPlayer.SEEK_CLOSEST)
        } catch (_: Exception) {
            try {
                mediaPlayer.seekTo(clamped)
            } catch (_: Exception) {}
        }
    }

    // Auto-timeout guard for isSeeking in case onSeekComplete does not fire
    LaunchedEffect(isSeeking) {
        if (isSeeking) {
            delay(1000)
            isSeeking = false
        }
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

    // Sync playback position to update scrubber (paused while seeking to prevent snap-back)
    LaunchedEffect(isPrepared, isPlaying, isSeeking) {
        while (isPrepared && isPlaying) {
            if (!isSeeking) {
                try {
                    currentPositionMs = mediaPlayer.currentPosition
                } catch (_: Exception) {}
            }
            delay(150)
        }
    }

    // Sync Mute state to MediaPlayer
    LaunchedEffect(isMuted, isPrepared) {
        try {
            val vol = if (isMuted) 0f else 1f
            mediaPlayer.setVolume(vol, vol)
        } catch (_: Exception) {}
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
                mediaPlayer.stop()
                mediaPlayer.reset()
                mediaPlayer.release()
            } catch (_: Exception) {}
            textureSurface?.release()
            textureSurface = null
        }
    }

    fun togglePlayPause() {
        if (isCompleted) {
            performSeek(0)
            try { mediaPlayer.start() } catch (_: Exception) {}
            isPlaying = true
            isCompleted = false
        } else if (isPlaying) {
            try { mediaPlayer.pause() } catch (_: Exception) {}
            isPlaying = false
            showControls = true
        } else {
            try { mediaPlayer.start() } catch (_: Exception) {}
            isPlaying = true
        }
    }

    val handleEdit: () -> Unit = {
        if (isImage) {
            showImageEditorModal = true
        } else {
            try { mediaPlayer.pause() } catch (_: Exception) {}
            isPlaying = false
            showVideoEditorModal = true
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
                            TextureView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                        val surface = Surface(st)
                                        textureSurface = surface
                                        try {
                                            mediaPlayer.setSurface(surface)
                                        } catch (_: Exception) {}
                                        updateTextureAspect(this@apply, videoDimensions.first, videoDimensions.second)
                                    }

                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                        updateTextureAspect(this@apply, videoDimensions.first, videoDimensions.second)
                                    }

                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                        textureSurface?.release()
                                        textureSurface = null
                                        try {
                                            mediaPlayer.setSurface(null)
                                        } catch (_: Exception) {}
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                }
                                textureViewRef = this
                            }
                        },
                        update = { tv ->
                            updateTextureAspect(tv, videoDimensions.first, videoDimensions.second)
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
                        performSeek(targetSeekMs)
                        coroutineScope.launch {
                            delay(600)
                            showSeekHud = false
                        }
                    },
                    onDoubleTapRewind = {
                        val newPos = (currentPositionMs - 5000).coerceAtLeast(0)
                        performSeek(newPos)
                        rewindKey++
                    },
                    onDoubleTapForward = {
                        val newPos = (currentPositionMs + 5000).coerceAtMost(totalDurationMs)
                        performSeek(newPos)
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

            }

            // Double Tap ±5s Indicators with Apple Liquid Glass
            DoubleTapSeekIndicator(
                visible = showRewindIndicator && dismissProgress < 0.05f,
                isForward = false,
                isLandscape = isLandscape,
                backdrop = mediaBackdrop,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            DoubleTapSeekIndicator(
                visible = showForwardIndicator && dismissProgress < 0.05f,
                isForward = true,
                isLandscape = isLandscape,
                backdrop = mediaBackdrop,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            // Apple Liquid Glass Vertical Sliders (Brightness on left, Volume on right) & Seek Scrub HUD
            BrightnessHud(
                visible = showBrightnessHud && dismissProgress < 0.05f,
                brightness = currentBrightness,
                backdrop = mediaBackdrop,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = if (isLandscape) 48.dp else 24.dp)
            )

            VolumeHud(
                visible = showVolumeHud && dismissProgress < 0.05f,
                volumeFraction = currentVolumeFraction,
                backdrop = mediaBackdrop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = if (isLandscape) 48.dp else 24.dp)
            )

            SeekScrubHud(
                visible = showSeekHud && dismissProgress < 0.05f,
                targetSeekMs = targetSeekMs,
                initialSeekMs = seekInitialMs,
                totalDurationMs = totalDurationMs,
                backdrop = mediaBackdrop,
                modifier = Modifier.align(Alignment.Center)
            )

            // Center Play/Pause Floating Action with Apple Liquid Glass (Video only, outside layerBackdrop)
            AnimatedVisibility(
                visible = !isAudio && !isImage && (showControls || !isPlaying || isCompleted) && dismissProgress < 0.05f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = videoScale
                        scaleY = videoScale
                        translationX = videoOffsetX
                        translationY = videoOffsetY
                        alpha = controlsAlpha
                    }
            ) {
                val isPlayIcon = !isPlaying && !isCompleted
                LiquidRoundButton(
                    onClick = { togglePlayPause() },
                    backdrop = mediaBackdrop,
                    size = 72.dp,
                    showBorder = false
                ) {
                    Icon(
                        imageVector = when {
                            isCompleted -> CupertinoIcons.Outlined.ClockArrowCirclepath
                            isPlaying -> CupertinoIcons.Filled.Pause
                            else -> CupertinoIcons.Filled.Play
                        },
                        contentDescription = "Play/Pause",
                        modifier = Modifier
                            .size(36.dp)
                            .then(if (isPlayIcon) Modifier.padding(start = 2.5.dp) else Modifier)
                    )
                }
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
                            try { mediaPlayer.pause() } catch (_: Exception) {}
                            isPlaying = false
                            showVideoEditorModal = true
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
                    backdrop = mediaBackdrop,
                    currentPositionMs = currentPositionMs,
                    totalDurationMs = totalDurationMs,
                    isAudio = isAudio,
                    isMuted = isMuted,
                    isLandscape = isLandscape,
                    onSeek = { targetMs ->
                        performSeek(targetMs)
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

            // Bottom Action Bar for Images: Photos style bar
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
            if (showVideoEditorModal) {
                VideoEditorModal(
                    record = record,
                    downloadHelper = downloadHelper,
                    onDismiss = {
                        showVideoEditorModal = false
                        try { mediaPlayer.start() } catch (_: Exception) {}
                        isPlaying = true
                    },
                    onExportSuccess = {
                        showVideoEditorModal = false
                    }
                )
            }

            // Image Editor Modal
            if (showImageEditorModal) {
                ImageEditorModal(
                    record = record,
                    downloadHelper = downloadHelper,
                    onDismiss = {
                        showImageEditorModal = false
                    },
                    onExportSuccess = {
                        showImageEditorModal = false
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
                backdrop = mediaBackdrop,
                isDark = true,
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

/**
 * Scales TextureView transform matrix to maintain correct aspect ratio (fit center).
 */
private fun updateTextureAspect(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int
) {
    if (videoWidth <= 0 || videoHeight <= 0 || textureView.width <= 0 || textureView.height <= 0) return
    val viewAspect = textureView.width.toFloat() / textureView.height.toFloat()
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
    val matrix = Matrix()
    if (viewAspect > videoAspect) {
        val scale = videoAspect / viewAspect
        matrix.setScale(scale, 1f, textureView.width / 2f, textureView.height / 2f)
    } else {
        val scale = viewAspect / videoAspect
        matrix.setScale(1f, scale, textureView.width / 2f, textureView.height / 2f)
    }
    textureView.setTransform(matrix)
}

