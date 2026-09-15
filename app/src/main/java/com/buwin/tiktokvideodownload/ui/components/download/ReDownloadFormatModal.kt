package com.buwin.tiktokvideodownload.ui.components.download

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.buwin.tiktokvideodownload.data.config.AppConfig
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import com.buwin.tiktokvideodownload.ui.screens.history.components.DownloadPlatform
import com.buwin.tiktokvideodownload.ui.screens.history.components.PlatformFallbackThumbnail
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.buwin.tiktokvideodownload.ui.theme.OutlineDark
import com.buwin.tiktokvideodownload.ui.theme.OutlineLight
import com.buwin.tiktokvideodownload.ui.theme.SurfaceDark
import com.buwin.tiktokvideodownload.ui.theme.SurfaceLight
import com.buwin.tiktokvideodownload.ui.theme.cardBorderColor
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark
import kotlinx.coroutines.launch

/**
 * Bottom Sheet Modal styled after iOS 17/18 Presentation Controller.
 * Features fluid spring-based slide-in/out transitions, tactile drag-down dismiss gesture,
 * progressive backdrop blur, and branded thumbnail fallback.
 */
@Composable
fun ReDownloadFormatModal(
    info: TikTokVideoInfo,
    onDismiss: () -> Unit,
    onDownloadOption: (DownloadOption) -> Unit,
    backdrop: Backdrop? = null,
    isDark: Boolean = LocalIsDark.current
) {
    val coroutineScope = rememberCoroutineScope()
    var isClosing by remember { mutableStateOf(false) }

    // Spring physics spec matching iOS Sheet presentation
    val animProgress = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(1000f) }
    val velocityTracker = remember { VelocityTracker() }

    // Slide up with natural iOS spring physics on GPU layer
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.84f,
                stiffness = 450f
            )
        )
    }

    // Dismiss with smooth slide-down animation
    fun dismissWithAnimation(onFinished: () -> Unit = onDismiss) {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            launch {
                dragOffsetY.animateTo(
                    targetValue = sheetHeightPx,
                    animationSpec = spring(
                        dampingRatio = 0.88f,
                        stiffness = 480f
                    )
                )
            }
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            onFinished()
        }
    }

    BackHandler(enabled = !isClosing) {
        dismissWithAnimation()
    }

    val surfaceBg = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = cardBorderColor(isDark)

    val isFacebook = info.originalUrl.contains("facebook.com", ignoreCase = true) ||
            info.originalUrl.contains("fb.watch", ignoreCase = true) ||
            info.originalUrl.contains("fb.com", ignoreCase = true) ||
            info.authorUsername.contains("facebook", ignoreCase = true) ||
            info.authorNickname.contains("facebook", ignoreCase = true)
    val platform = if (isFacebook) DownloadPlatform.FACEBOOK else DownloadPlatform.TIKTOK

    val primaryOption = info.options.firstOrNull { it.type == DownloadFormatType.VIDEO_HD_NO_WATERMARK }
        ?: info.options.firstOrNull { it.type == DownloadFormatType.VIDEO_SD_NO_WATERMARK }
        ?: info.options.firstOrNull()

    val secondaryOptions = info.options.filter { it != primaryOption }

    // Interactive drag gesture modifier for dragging down to dismiss
    val dragGestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = {
                velocityTracker.resetTracking()
            },
            onDragEnd = {
                val velocity = velocityTracker.calculateVelocity().y
                if (dragOffsetY.value > 120f || velocity > 800f) {
                    dismissWithAnimation()
                } else {
                    coroutineScope.launch {
                        dragOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                        )
                    }
                }
            },
            onDragCancel = {
                coroutineScope.launch {
                    dragOffsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                    )
                }
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                val newOffset = if (dragOffsetY.value + dragAmount < 0) {
                    dragOffsetY.value + dragAmount * 0.18f // Rubber-band elasticity
                } else {
                    dragOffsetY.value + dragAmount
                }
                coroutineScope.launch {
                    dragOffsetY.snapTo(newOffset)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // GPU Drawing Phase: strictly runs on GPU, zero recomposition overhead
                val progress = animProgress.value
                val dragY = dragOffsetY.value
                val alpha = (progress * (1f - (dragY / sheetHeightPx).coerceIn(0f, 1f)) * 0.55f).coerceIn(0f, 0.55f)
                drawRect(Color.Black.copy(alpha = alpha))
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissWithAnimation() }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    if (coordinates.size.height > 0) {
                        sheetHeightPx = coordinates.size.height.toFloat()
                    }
                }
                .graphicsLayer {
                    // GPU RenderNode Phase: hardware-accelerated translation directly on display list
                    val progress = animProgress.value
                    val dragY = dragOffsetY.value
                    translationY = ((1f - progress) * sheetHeightPx) + maxOf(0f, dragY)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Prevent clicks inside modal from dismissing
                )
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp) },
                            effects = {
                                vibrancy()
                                blur(18f.dp.toPx())
                                lens(18f.dp.toPx(), 22f.dp.toPx())
                            },
                            highlight = { Highlight.Plain },
                            shadow = {
                                Shadow(
                                    radius = 28f.dp,
                                    color = if (isDark) Color.Black.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.22f)
                                )
                            },
                            onDrawSurface = {
                                drawRect(surfaceBg.copy(alpha = if (isDark) 0.88f else 0.94f))
                            }
                        )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(surfaceBg)
                    }
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag Handle Bar with vertical swipe gesture
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .then(dragGestureModifier),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.18f))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Header Title & Close Button (also swipe-enabled)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(dragGestureModifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tải lại video",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Chọn định dạng mong muốn để tải về máy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Surface(
                    onClick = { dismissWithAnimation() },
                    shape = CircleShape,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.Xmark,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video Summary Row with branded fallback thumbnail
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF5F5F4)),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail with branded fallback
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 0.8.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (info.coverUrl.isNotEmpty()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(info.coverUrl)
                                    .crossfade(true)
                                    .setHeader("Referer", if (info.coverUrl.contains("tikwm.com")) AppConfig.REFERER_TIKTOK else AppConfig.REFERER_FACEBOOK)
                                    .setHeader("User-Agent", AppConfig.USER_AGENT_MOBILE)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shimmerEffect(isDark)
                                    )
                                },
                                error = {
                                    PlatformFallbackThumbnail(
                                        platform = platform,
                                        isAudio = false,
                                        isDark = isDark
                                    )
                                }
                            )
                        } else {
                            PlatformFallbackThumbnail(
                                platform = platform,
                                isAudio = false,
                                isDark = isDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.title.ifEmpty { "Video tải về" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (info.authorUsername.isNotEmpty()) "@${info.authorUsername}" else info.authorNickname,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Format Options Section
            if (primaryOption != null) {
                Text(
                    text = "Khuyên dùng",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HeroDownloadButton(
                    option = primaryOption,
                    onClick = {
                        dismissWithAnimation {
                            onDownloadOption(primaryOption)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (secondaryOptions.isNotEmpty()) {
                Text(
                    text = "Định dạng khác",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    secondaryOptions.forEach { option ->
                        SecondaryOptionTile(
                            option = option,
                            cardBackground = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF),
                            cardBorderColor = borderColor,
                            onClick = {
                                dismissWithAnimation {
                                    onDownloadOption(option)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
