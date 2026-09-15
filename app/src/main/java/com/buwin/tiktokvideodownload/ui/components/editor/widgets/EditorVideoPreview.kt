package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Play

/**
 * Video preview canvas with overlay play/pause toggle.
 */
@Composable
fun EditorVideoPreview(
    videoUri: Uri?,
    isPlaying: Boolean,
    onVideoViewReady: (VideoView) -> Unit,
    onPrepared: (durationMs: Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (videoUri != null) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            onPrepared(mp.duration.toLong())
                            start()
                        }
                        setOnCompletionListener {
                            // handled externally via auto-loop
                        }
                        onVideoViewReady(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
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
