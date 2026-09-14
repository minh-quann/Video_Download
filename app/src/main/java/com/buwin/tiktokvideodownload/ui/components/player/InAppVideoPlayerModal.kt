package com.buwin.tiktokvideodownload.ui.components.player

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Pause
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.ClockArrowCirclepath
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Premium in-app video and audio player modal supporting both direct Video playback
 * and Audio MP3 playback with Apple-style fluid controls and gestures.
 */
@Composable
fun InAppVideoPlayerModal(
    record: DownloadRecord,
    downloadHelper: DownloadManagerHelper,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val videoUri = remember(record) { downloadHelper.getDownloadedUri(record) }
    val isAudio = remember(record) { record.fileExtension.lowercase() in listOf("mp3", "m4a", "aac", "wav") }

    var isPlaying by remember { mutableStateOf(true) }
    var isPrepared by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isCompleted by remember { mutableStateOf(false) }

    // Handle back button dismiss
    BackHandler {
        onDismiss()
    }

    // Auto-hide controls after 3.5 seconds of playing (video only)
    LaunchedEffect(showControls, isPlaying, isAudio) {
        if (!isAudio && showControls && isPlaying && !isCompleted) {
            delay(3500)
            showControls = false
        }
    }

    // Polling current playback position to update scrubber
    LaunchedEffect(isPrepared, isPlaying, isDraggingSlider) {
        while (isPrepared && isPlaying && !isDraggingSlider) {
            videoViewRef?.let { vv ->
                try {
                    currentPositionMs = vv.currentPosition
                    if (totalDurationMs > 0) {
                        sliderPosition = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    }
                } catch (_: Exception) {
                }
            }
            delay(200)
        }
    }

    // Release video player when closed
    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Đóng",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Surface(
                        onClick = { downloadHelper.openDownloadsFolder() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Mở thư mục",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // Main Media Player Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControls = !showControls
                    },
                contentAlignment = Alignment.Center
            ) {
                // Native Android VideoView (handles decoding & audio playback)
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            setVideoURI(videoUri)
                            setOnPreparedListener { mp ->
                                isPrepared = true
                                totalDurationMs = mp.duration
                                mp.isLooping = false
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

                if (isAudio) {
                    // Audio MP3 Dedicated Player Visual
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1F1F23)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (record.coverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = record.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = CupertinoIcons.Outlined.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = record.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${record.author} • ${if (record.formatTitle.isNotEmpty()) record.formatTitle else "Âm thanh MP3"}",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        // Audio Dedicated Play/Pause Circle Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
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
                                },
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
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Buffering / Loading Spinner
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
                            ) {
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
                            },
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
            }

            // Top Header Bar
            AnimatedVisibility(
                visible = if (isAudio) true else showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dismiss / Close Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.20f))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.Xmark,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${record.author} • ${record.formatTitle}",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Share Intent Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.20f))
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (isAudio) "audio/*" else "video/*"
                                        putExtra(Intent.EXTRA_STREAM, videoUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, if (isAudio) "Chia sẻ âm thanh" else "Chia sẻ video"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Player Control Bar
            AnimatedVisibility(
                visible = if (isAudio) true else showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Time Scrubber Slider
                        Slider(
                            value = sliderPosition,
                            onValueChange = { newRatio ->
                                isDraggingSlider = true
                                sliderPosition = newRatio
                                currentPositionMs = (newRatio * totalDurationMs).toInt()
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                videoViewRef?.seekTo(currentPositionMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Duration and Position Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatDuration(totalDurationMs),
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats millisecond duration into mm:ss format.
 */
private fun formatDuration(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
