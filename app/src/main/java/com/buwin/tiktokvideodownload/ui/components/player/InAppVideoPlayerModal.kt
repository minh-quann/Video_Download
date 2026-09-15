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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.editor.VideoEditorModal
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
@Composable
fun InAppVideoPlayerModal(
    record: DownloadRecord,
    downloadHelper: DownloadManagerHelper,
    onDismiss: () -> Unit,
    onRedownloadClick: ((DownloadRecord) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val videoUri = remember(record) { downloadHelper.getDownloadedUri(record) }
    val isAudio = remember(record) { record.fileExtension.lowercase() in listOf("mp3", "m4a", "aac", "wav") }

    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    var isPlaying by remember { mutableStateOf(true) }
    var isPrepared by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var showEditorModal by remember { mutableStateOf(false) }

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

    // Back button handling: exit landscape mode first, otherwise dismiss modal
    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            onDismiss()
        }
    }

    // Auto-hide controls after 3.5 seconds of playing
    LaunchedEffect(showControls, isPlaying, isAudio, showSeekHud) {
        if (!isAudio && showControls && isPlaying && !isCompleted && !showSeekHud) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (videoUri == null) {
            // Error: File not found state
            FileNotFoundErrorView(
                isAudio = isAudio,
                record = record,
                downloadHelper = downloadHelper,
                onDismiss = onDismiss,
                onRedownloadClick = onRedownloadClick
            )
        } else {
            // Main Media Player Area
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Native Android VideoView
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
                    enabled = !isAudio,
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
                if (!isPrepared) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Center Play/Pause Floating Action (for video only)
                AnimatedVisibility(
                    visible = !isAudio && (showControls || !isPlaying || isCompleted),
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
                visible = if (isAudio) true else showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                PlayerTopBar(
                    title = record.title,
                    author = record.author,
                    formatTitle = record.formatTitle,
                    isMuted = isMuted,
                    isLandscape = isLandscape,
                    onToggleMute = { isMuted = !isMuted },
                    onDismiss = {
                        if (isLandscape) {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        onDismiss()
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (isAudio) "audio/*" else "video/*"
                            putExtra(Intent.EXTRA_STREAM, videoUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                if (isAudio) "Chia sẻ âm thanh" else "Chia sẻ video"
                            )
                        )
                    },
                    onOpenEditor = if (!isAudio) {
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
                visible = if (isAudio) true else showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
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
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "Đóng",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                onClick = { downloadHelper.openDownloadsFolder() },
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "Mở thư mục",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Medium
                )
            }
            if (onRedownloadClick != null) {
                Surface(
                    onClick = {
                        onDismiss()
                        onRedownloadClick(record)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
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
