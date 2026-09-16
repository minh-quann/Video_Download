package com.buwin.tiktokvideodownload.ui.screens.gallery.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.buwin.tiktokvideodownload.ui.screens.gallery.model.GalleryMediaItem
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.Photo

/**
 * Clean, modern media grid thumbnail styled after Apple Photos & Samsung Gallery.
 * Full-bleed 1:1 square, subtle corner rounding, video duration badge, and no cluttering titles.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryMediaCard(
    item: GalleryMediaItem,
    isDark: Boolean,
    onClick: (Rect?) -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val imageRequest = remember(item.uri, item.sourceRecord?.coverUrl) {
        val cover = item.sourceRecord?.coverUrl
        val targetData = if (!cover.isNullOrEmpty()) cover else item.uri
        ImageRequest.Builder(context)
            .data(targetData)
            .apply {
                if (item.isVideo) {
                    videoFrameMillis(1000L)
                }
            }
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onGloballyPositioned { cardCoordinates = it }
            .clip(RoundedCornerShape(3.dp))
            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
            .combinedClickable(
                onClick = { onClick(cardCoordinates?.boundsInRoot()) },
                onLongClick = onLongClick
            )
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                )
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isVideo) CupertinoIcons.Filled.Play else CupertinoIcons.Outlined.Photo,
                        contentDescription = null,
                        tint = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )

        // Video indicator & duration badge (Apple / Samsung style)
        if (item.isVideo) {
            // Subtle bottom gradient for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            // Duration text at bottom right
            if (item.durationText.isNotEmpty()) {
                Text(
                    text = item.durationText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 4.dp)
                )
            } else {
                Icon(
                    imageVector = CupertinoIcons.Filled.Play,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 4.dp)
                        .size(12.dp)
                )
            }
        }
    }
}
