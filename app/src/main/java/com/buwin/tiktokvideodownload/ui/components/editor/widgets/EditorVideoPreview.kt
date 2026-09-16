package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.RenderEffect
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Play

/**
 * Controller interface for playback operations during video editing.
 */
interface EditorPlayerController {
    val currentPosition: Long
    fun start()
    fun pause()
    fun seekTo(positionMs: Int)
    fun stopPlayback()
    fun setPlaybackSpeed(speed: Float)
}

/**
 * Creates a combined ColorMatrix for brightness, contrast, and saturation adjustments.
 */
internal fun createAdjustColorMatrix(
    brightness: Float,
    contrast: Float,
    saturation: Float
): ColorMatrix {
    val result = ColorMatrix()

    // 1. Saturation adjustment: saturation in [-1f, 1f] -> satFactor in [0f, 2f]
    val satFactor = (saturation + 1f).coerceAtLeast(0f)
    if (satFactor != 1f) {
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(satFactor)
        result.postConcat(satMatrix)
    }

    // 2. Contrast adjustment: matches Media3 Contrast effect formula
    if (contrast != 0f) {
        val f2 = (1.0f + contrast) / (1.0001f - contrast)
        val translate = (1.0f - f2) * 0.5f * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                f2, 0f, 0f, 0f, translate,
                0f, f2, 0f, 0f, translate,
                0f, 0f, f2, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.postConcat(contrastMatrix)
    }

    // 3. Brightness adjustment: brightness in [-1f, 1f] -> offset in [-255f, 255f]
    if (brightness != 0f) {
        val offset = brightness * 255f
        val brightnessMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, offset,
                0f, 1f, 0f, 0f, offset,
                0f, 0f, 1f, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.postConcat(brightnessMatrix)
    }

    return result
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

/**
 * Hardware-accelerated video preview canvas backed by TextureView.
 * Supports real-time color filter adjustments (brightness, contrast, saturation)
 * and transformations (rotation, scaling).
 */
@Composable
fun EditorVideoPreview(
    videoUri: Uri?,
    isPlaying: Boolean,
    brightness: Float = 0f,
    contrast: Float = 0f,
    saturation: Float = 0f,
    rotationDegrees: Float = 0f,
    playbackSpeed: Float = 1.0f,
    onPlayerReady: (EditorPlayerController) -> Unit,
    onPrepared: (durationMs: Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPrepared by remember { mutableStateOf(false) }
    var textureSurface by remember { mutableStateOf<Surface?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var videoDimensions by remember { mutableStateOf(Pair(0, 0)) }

    // Initialize & load media data
    LaunchedEffect(videoUri) {
        if (videoUri == null) return@LaunchedEffect
        try {
            isPrepared = false
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, videoUri)
            mediaPlayer.setOnPreparedListener { mp ->
                isPrepared = true
                onPrepared(mp.duration.toLong())
                if (isPlaying) {
                    mp.start()
                }
            }
            mediaPlayer.setOnVideoSizeChangedListener { _, w, h ->
                videoDimensions = Pair(w, h)
                textureViewRef?.let { tv -> updateTextureAspect(tv, w, h) }
            }
            mediaPlayer.isLooping = false
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Attach surface to MediaPlayer when available
    LaunchedEffect(textureSurface) {
        textureSurface?.let { surface ->
            try {
                mediaPlayer.setSurface(surface)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Sync play/pause state
    LaunchedEffect(isPlaying, isPrepared) {
        if (isPrepared) {
            try {
                if (isPlaying && !mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                } else if (!isPlaying && mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            } catch (_: Exception) {}
        }
    }

    // Sync playback speed
    LaunchedEffect(playbackSpeed, isPrepared) {
        if (isPrepared && playbackSpeed > 0f) {
            try {
                mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed)
            } catch (_: Exception) {}
        }
    }

    // Expose controller to parent
    LaunchedEffect(mediaPlayer) {
        val controller = object : EditorPlayerController {
            override val currentPosition: Long
                get() = try {
                    if (isPrepared) mediaPlayer.currentPosition.toLong() else 0L
                } catch (_: Exception) { 0L }

            override fun start() {
                try {
                    if (isPrepared && !mediaPlayer.isPlaying) mediaPlayer.start()
                } catch (_: Exception) {}
            }

            override fun pause() {
                try {
                    if (isPrepared && mediaPlayer.isPlaying) mediaPlayer.pause()
                } catch (_: Exception) {}
            }

            override fun seekTo(positionMs: Int) {
                try {
                    if (isPrepared) mediaPlayer.seekTo(positionMs)
                } catch (_: Exception) {}
            }

            override fun stopPlayback() {
                try {
                    if (isPrepared) mediaPlayer.stop()
                } catch (_: Exception) {}
            }

            override fun setPlaybackSpeed(speed: Float) {
                try {
                    if (isPrepared && speed > 0f) {
                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
                    }
                } catch (_: Exception) {}
            }
        }
        onPlayerReady(controller)
    }

    // Release MediaPlayer when disposed
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.stop()
                mediaPlayer.reset()
                mediaPlayer.release()
            } catch (_: Exception) {}
            textureSurface?.release()
            textureSurface = null
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (videoUri != null) {
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

                    // Real-time hardware-accelerated RenderEffect
                    val isAdjusted = brightness != 0f || contrast != 0f || saturation != 0f
                    val renderEffect = if (isAdjusted) {
                        val cm = createAdjustColorMatrix(brightness, contrast, saturation)
                        RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm))
                    } else null
                    tv.setRenderEffect(renderEffect)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.rotationZ = rotationDegrees
                        val isSideways = (rotationDegrees.toInt() % 180 != 0)
                        if (isSideways && size.width > 0 && size.height > 0) {
                            val scale = minOf(size.width / size.height, size.height / size.width)
                            scaleX = scale
                            scaleY = scale
                        } else {
                            scaleX = 1f
                            scaleY = 1f
                        }
                    }
            )

            // Play/Pause overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTogglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Filled.Play,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
