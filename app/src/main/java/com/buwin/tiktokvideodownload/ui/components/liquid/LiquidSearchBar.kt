package com.buwin.tiktokvideodownload.ui.components.liquid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
 * Authentic iOS 26 Liquid Morphing Search Bar.
 *
 * Implements Apple's fluid expanding / collapsing animation with spring physics:
 * - Collapsed state: Compact, elegant floating glass pill with gentle refraction in screen center.
 * - Expanded state: Morphs and blooms across full width with spring physics (stiffness 350, damping 0.75),
 *   automatically focusing the text field, showing quick actions (Paste / Clear / Download),
 *   and smoothly sliding in the glass Cancel pill ("Hủy").
 * - On Cancel / tap outside: Fluidly collapses back to its compact pill state with gentle overshoot bounce.
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
    isDark: Boolean = isSystemInDarkTheme()
) {
    val isLightTheme = !isDark
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // State of expansion: active if focused, manually opened, or contains text
    var isFocused by remember { mutableStateOf(false) }
    var isManuallyExpanded by remember { mutableStateOf(false) }
    val isExpanded = isManuallyExpanded || isFocused || value.isNotEmpty()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val accentColor = if (isLightTheme) Color(0xFF007AFF) else Color(0xFF0A84FF)
    val contentColor = if (isLightTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val containerColor = if (isLightTheme) {
        Color.White.copy(0.68f)
    } else {
        Color(0xFF18181B).copy(0.62f)
    }

    // iOS 26 fluid spring specifications (stiffness: 350, dampingRatio: 0.75 for organic overshoot)
    val floatSpringSpec = spring<Float>(dampingRatio = 0.75f, stiffness = 350f)
    val dpSpringSpec = spring<Dp>(dampingRatio = 0.75f, stiffness = 350f)
    val intOffsetSpringSpec = spring<IntOffset>(dampingRatio = 0.75f, stiffness = 350f)

    // Animated dimensions during expand / collapse
    val animatedHeight by animateDpAsState(
        targetValue = if (isExpanded) 54.dp else 48.dp,
        animationSpec = dpSpringSpec,
        label = "barHeight"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isFocused -> 1.01f
            else -> 1f
        },
        animationSpec = floatSpringSpec,
        label = "barScale"
    )

    // Lens refraction dynamically deepens when expanded
    val refractionAmount by animateDpAsState(
        targetValue = if (isExpanded) 22.dp else 16.dp,
        animationSpec = dpSpringSpec,
        label = "refractionAmount"
    )

    // Request focus when user taps to expand
    LaunchedEffect(isManuallyExpanded) {
        if (isManuallyExpanded) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val cancelButtonWidth = 58.dp
        val gap = 8.dp

        // Morphing geometry calculations
        val collapsedWidth = minOf(totalWidth, 230.dp)
        val collapsedOffset = maxOf(0.dp, (totalWidth - collapsedWidth) / 2)
        val expandedWidth = maxOf(0.dp, totalWidth - cancelButtonWidth - gap)

        // Blooming width animation (compact pill -> full width)
        val animatedWidth by animateDpAsState(
            targetValue = if (isExpanded) expandedWidth else collapsedWidth,
            animationSpec = dpSpringSpec,
            label = "searchBarWidth"
        )

        // Fluid offset animation (glides from screen center to left edge)
        val animatedOffset by animateDpAsState(
            targetValue = if (isExpanded) 0.dp else collapsedOffset,
            animationSpec = dpSpringSpec,
            label = "searchBarOffset"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (animatedOffset > 0.5.dp) {
                Spacer(modifier = Modifier.width(animatedOffset))
            }

            // Main Expanding Liquid Glass Search Pill
            Row(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(animatedHeight)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(if (isLightTheme) 10f.dp.toPx() else 8f.dp.toPx())
                            lens(
                                refractionHeight = 14f.dp.toPx(),
                                refractionAmount = refractionAmount.toPx(),
                                depthEffect = true,
                                chromaticAberration = true
                            )
                        },
                        highlight = { Highlight.Default },
                        shadow = {
                            Shadow(
                                radius = if (isExpanded) 16f.dp else 10f.dp,
                                color = if (isLightTheme) Color.Black.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.35f)
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 7f.dp,
                                alpha = if (isLightTheme) 0.07f else 0.16f
                            )
                        },
                        onDrawSurface = {
                            drawRect(containerColor)
                            if (isFocused) {
                                drawRect(accentColor.copy(alpha = 0.04f))
                            }
                        }
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button
                    ) {
                        if (!isExpanded) {
                            isManuallyExpanded = true
                        }
                    }
                    .padding(start = 14.dp, end = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Leading Magnifying Glass Icon
                Icon(
                    imageVector = CupertinoIcons.Default.MagnifyingGlass,
                    contentDescription = "Tìm kiếm",
                    tint = if (isFocused) accentColor else contentColor.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )

                // Middle area: Collapsed hint VS Expanded active text input
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (!isExpanded) {
                        // Collapsed state: Compact pill hint
                        BasicText(
                            text = "Dán link hoặc tìm kiếm...",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.60f),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1
                        )
                    } else {
                        // Expanded state: Full interactive text field
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    isFocused = it.isFocused
                                    if (!it.isFocused && value.isEmpty()) {
                                        isManuallyExpanded = false
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
                                                color = contentColor.copy(alpha = 0.40f),
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
                    }
                }

                // Expanded Action Controls (Paste chip / Clear button + Download Action)
                if (isExpanded) {
                    // Quick Paste Chip (when input is empty)
                    if (value.isEmpty() && onPaste != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = if (isLightTheme) 0.12f else 0.22f))
                                .clickable(role = Role.Button) { onPaste() }
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

                    // Clear button (when input has text)
                    if (value.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) {
                                    onValueChange("")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Filled.XmarkCircle,
                                contentDescription = "Xóa nội dung",
                                tint = contentColor.copy(alpha = 0.45f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Integrated Action Button (Download trigger)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (value.isNotEmpty() || isLoading) {
                                    accentColor
                                } else {
                                    accentColor.copy(alpha = if (isLightTheme) 0.18f else 0.28f)
                                }
                            )
                            .clickable(
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
            }

            // Morphing Detached Glass Cancel Pill (Slides in from right when expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInHorizontally(
                    initialOffsetX = { it + 30 },
                    animationSpec = intOffsetSpringSpec
                ) + fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.85f),
                exit = slideOutHorizontally(
                    targetOffsetX = { it + 30 },
                    animationSpec = intOffsetSpringSpec
                ) + fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.85f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(gap))

                    Box(
                        modifier = Modifier
                            .width(cancelButtonWidth)
                            .height(animatedHeight)
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
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button
                            ) {
                                focusManager.clearFocus()
                                isManuallyExpanded = false
                                if (value.isNotEmpty()) {
                                    onValueChange("")
                                }
                            },
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
}
