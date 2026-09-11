package com.buwin.tiktokvideodownload.ui.components.liquid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.DocOnDoc
import io.github.alexzhirkevich.cupertino.icons.filled.XmarkCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowDownToLine
import io.github.alexzhirkevich.cupertino.icons.outlined.MagnifyingGlass

/**
 * Authentic iOS 26 Liquid Glass Search Bar.
 *
 * Implements the official Apple SwiftUI & Flutter liquid_glass_widgets pattern:
 * - Fluid spring transition: Search bar stretches across full width and smoothly resizes to accommodate
 *   the detached glass "Hủy" (Cancel) button sliding in from the trailing edge.
 * - Synchronized expandHorizontally + slideInHorizontally: Guarantees zero-jank, continuous 120 FPS interpolation.
 * - Deep real-time optical refraction, vibrancy, and specular highlights.
 */
@Composable
fun LiquidSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    placeholder: String = "Dán link TikTok tại đây...",
    isLoading: Boolean = false,
    onSearch: () -> Unit = {},
    onPaste: (() -> Unit)? = null,
    isDark: Boolean = LocalIsDark.current
) {
    val isLightTheme = !isDark
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Active editing state (true when focused or containing text)
    var isEditing by remember { mutableStateOf(value.isNotEmpty()) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value.isNotEmpty() && !isEditing) {
            isEditing = true
        }
    }

    // Interaction sources for physical touch feedback
    val searchBarInteractionSource = remember { MutableInteractionSource() }
    val isPressed by searchBarInteractionSource.collectIsPressedAsState()

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()

    val pasteInteractionSource = remember { MutableInteractionSource() }
    val isPastePressed by pasteInteractionSource.collectIsPressedAsState()

    val downloadInteractionSource = remember { MutableInteractionSource() }
    val isDownloadPressed by downloadInteractionSource.collectIsPressedAsState()

    val accentColor = if (isLightTheme) Color(0xFF007AFF) else Color(0xFF0A84FF)
    val contentColor = if (isLightTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val containerColor = if (isLightTheme) {
        Color.White.copy(0.68f)
    } else {
        Color(0xFF18181B).copy(0.62f)
    }

    // Apple iOS 26 fluid spring physics (mass: 1.0, stiffness: 350.0, damping: 30.0 -> dampingRatio: 0.80f)
    val springFloatSpec = spring<Float>(dampingRatio = 0.80f, stiffness = 350f)
    val springIntSizeSpec = spring<IntSize>(dampingRatio = 0.80f, stiffness = 350f)
    val springIntOffsetSpec = spring<IntOffset>(dampingRatio = 0.80f, stiffness = 350f)

    // Tactile press scales
    val searchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = springFloatSpec,
        label = "searchBarScale"
    )

    val cancelScale by animateFloatAsState(
        targetValue = if (isCancelPressed) 0.94f else 1f,
        animationSpec = springFloatSpec,
        label = "cancelScale"
    )

    val pasteScale by animateFloatAsState(
        targetValue = if (isPastePressed) 0.93f else 1f,
        animationSpec = springFloatSpec,
        label = "pasteScale"
    )

    val downloadScale by animateFloatAsState(
        targetValue = if (isDownloadPressed) 0.92f else 1f,
        animationSpec = springFloatSpec,
        label = "downloadScale"
    )

    fun handleCancel() {
        isEditing = false
        focusManager.clearFocus()
        if (value.isNotEmpty()) {
            onValueChange("")
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 1. Main Liquid Glass Search Field (Smoothly animates width with zero relayout overhead) ──
        Row(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .graphicsLayer {
                    scaleX = searchScale
                    scaleY = searchScale
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(if (isLightTheme) 10f.dp.toPx() else 8f.dp.toPx())
                        lens(
                            refractionHeight = 12f.dp.toPx(),
                            refractionAmount = 18f.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Default },
                    shadow = {
                        Shadow(
                            radius = if (isEditing) 12f.dp else 8f.dp,
                            color = if (isLightTheme) Color.Black.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.35f)
                        )
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 6f.dp,
                            alpha = if (isLightTheme) 0.07f else 0.16f
                        )
                    },
                    onDrawSurface = {
                        drawRect(containerColor)
                    }
                )
                .clickable(
                    interactionSource = searchBarInteractionSource,
                    indication = null
                ) {
                    isEditing = true
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
                .padding(start = 14.dp, end = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Magnifying Glass Glyph
            Icon(
                imageVector = CupertinoIcons.Default.MagnifyingGlass,
                contentDescription = "Tìm kiếm",
                tint = contentColor.copy(alpha = 0.50f),
                modifier = Modifier.size(20.dp)
            )

            // Interactive Text Field (always present, never destroyed/recreated)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            isEditing = true
                        } else if (value.isEmpty()) {
                            isEditing = false
                        }
                    },
                singleLine = true,
                textStyle = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    onSearch()
                }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = placeholder,
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.45f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Trailing Quick Actions
            // Quick Paste Chip (shown when field is empty)
            AnimatedVisibility(
                visible = value.isEmpty() && onPaste != null,
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.85f),
                exit = fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.85f)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pasteScale
                            scaleY = pasteScale
                        }
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = if (isLightTheme) 0.12f else 0.22f))
                        .clickable(
                            interactionSource = pasteInteractionSource,
                            indication = null,
                            role = Role.Button
                        ) {
                            onPaste?.invoke()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Filled.DocOnDoc,
                            contentDescription = "Dán",
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        BasicText(
                            text = "Dán",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }

            // Quick Clear Button (shown when field has text)
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.85f),
                exit = fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.85f)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.XmarkCircle,
                        contentDescription = "Xóa nội dung",
                        tint = contentColor.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Primary Download Action Trigger Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .graphicsLayer {
                        scaleX = downloadScale
                        scaleY = downloadScale
                    }
                    .clip(CircleShape)
                    .background(
                        if (value.isNotEmpty() || isLoading) accentColor
                        else accentColor.copy(alpha = if (isLightTheme) 0.16f else 0.26f)
                    )
                    .clickable(
                        interactionSource = downloadInteractionSource,
                        indication = null,
                        enabled = !isLoading,
                        role = Role.Button
                    ) {
                        focusManager.clearFocus()
                        onSearch()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.2.dp
                    )
                } else {
                    Icon(
                        imageVector = CupertinoIcons.Outlined.ArrowDownToLine,
                        contentDescription = "Bắt đầu tải",
                        tint = if (value.isNotEmpty()) Color.White else accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── 2. Detached Liquid Glass Cancel Pill (Smooth continuous width expansion + slide) ──
        AnimatedVisibility(
            visible = isEditing,
            enter = expandHorizontally(
                animationSpec = springIntSizeSpec,
                expandFrom = Alignment.End
            ) + slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = springIntOffsetSpec
            ) + fadeIn(animationSpec = tween(180)),
            exit = shrinkHorizontally(
                animationSpec = springIntSizeSpec,
                shrinkTowards = Alignment.End
            ) + slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = springIntOffsetSpec
            ) + fadeOut(animationSpec = tween(140))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .graphicsLayer {
                            scaleX = cancelScale
                            scaleY = cancelScale
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(if (isLightTheme) 8f.dp.toPx() else 6f.dp.toPx())
                                lens(
                                    refractionHeight = 12f.dp.toPx(),
                                    refractionAmount = 16f.dp.toPx(),
                                    depthEffect = true
                                )
                            },
                            highlight = { Highlight.Default },
                            shadow = {
                                Shadow(
                                    radius = 8f.dp,
                                    color = if (isLightTheme) Color.Black.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.3f)
                                )
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .clickable(
                            interactionSource = cancelInteractionSource,
                            indication = null,
                            role = Role.Button
                        ) {
                            handleCancel()
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "Hủy",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}
