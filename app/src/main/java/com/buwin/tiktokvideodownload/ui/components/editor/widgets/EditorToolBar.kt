package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.components.editor.model.EditorToolCategory
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.CircleLefthalfed
import io.github.alexzhirkevich.cupertino.icons.outlined.CameraFilters
import io.github.alexzhirkevich.cupertino.icons.outlined.Flame
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.SliderHorizontal3
import io.github.alexzhirkevich.cupertino.icons.outlined.SunMax
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Liquid Glass bottom toolbar with horizontally scrollable chips.
 * Context panels slide up with glass-styled backgrounds.
 */
@Composable
fun EditorToolBar(
    isTrimEnabled: Boolean,
    isMuteEnabled: Boolean,
    isReplaceAudioEnabled: Boolean,
    isExtractAudioOnly: Boolean,
    speedMultiplier: Float,
    rotationDegrees: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float = 0f,
    hue: Float = 0f,
    blur: Float = 0f,
    activeCategory: EditorToolCategory?,
    backdrop: Backdrop,
    onToggleTrim: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleReplaceAudio: () -> Unit,
    onToggleExtractAudio: () -> Unit,
    onSelectCategory: (EditorToolCategory?) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onRotate: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onWarmthChange: (Float) -> Unit = {},
    onHueChange: (Float) -> Unit = {},
    onBlurChange: (Float) -> Unit = {},
    onResetAdjust: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // ── Context Panel (Liquid Glass background) ──
        AnimatedContent(
            targetState = activeCategory,
            transitionSpec = {
                (slideInVertically(tween(250)) { it / 2 } + fadeIn(tween(200)))
                    .togetherWith(slideOutVertically(tween(150)) { it / 2 } + fadeOut(tween(100)))
            },
            label = "context_panel"
        ) { category ->
            when (category) {
                EditorToolCategory.TRIM -> {
                    LiquidContextPanel(backdrop = backdrop) {
                        ToggleRow(
                            items = listOf(
                                ToggleItem("Cắt Video", isTrimEnabled) { onToggleTrim() },
                                ToggleItem("Tách Nhạc", isExtractAudioOnly) { onToggleExtractAudio() }
                            )
                        )
                    }
                }
                EditorToolCategory.AUDIO -> {
                    LiquidContextPanel(backdrop = backdrop) {
                        ToggleRow(
                            items = listOf(
                                ToggleItem("Tắt Tiếng", isMuteEnabled) { onToggleMute() },
                                ToggleItem("Ghép Nhạc", isReplaceAudioEnabled) { onToggleReplaceAudio() }
                            )
                        )
                    }
                }
                EditorToolCategory.SPEED -> {
                    LiquidContextPanel(backdrop = backdrop) {
                        SpeedPanel(currentSpeed = speedMultiplier, onSpeedChange = onSpeedChange)
                    }
                }
                EditorToolCategory.ROTATE -> {
                    LiquidContextPanel(backdrop = backdrop) {
                        RotatePanel(currentDegrees = rotationDegrees, onRotate = onRotate)
                    }
                }
                EditorToolCategory.ADJUST -> {
                    LiquidContextPanel(backdrop = backdrop) {
                        AdjustPanel(
                            brightness = brightness,
                            contrast = contrast,
                            saturation = saturation,
                            warmth = warmth,
                            hue = hue,
                            blur = blur,
                            backdrop = backdrop,
                            onBrightnessChange = onBrightnessChange,
                            onContrastChange = onContrastChange,
                            onSaturationChange = onSaturationChange,
                            onWarmthChange = onWarmthChange,
                            onHueChange = onHueChange,
                            onBlurChange = onBlurChange,
                            onResetAdjust = onResetAdjust
                        )
                    }
                }
                null -> Spacer(modifier = Modifier.height(0.dp))
            }
        }

        // ── Horizontal Scrollable Liquid Glass Chips ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidEditorChip(
                label = "Cắt",
                icon = Icons.Filled.ContentCut,
                isActive = activeCategory == EditorToolCategory.TRIM,
                hasActiveOp = isTrimEnabled || isExtractAudioOnly,
                backdrop = backdrop,
                onClick = { onSelectCategory(if (activeCategory == EditorToolCategory.TRIM) null else EditorToolCategory.TRIM) }
            )
            LiquidEditorChip(
                label = "Âm thanh",
                icon = CupertinoIcons.Outlined.MusicNote,
                isActive = activeCategory == EditorToolCategory.AUDIO,
                hasActiveOp = isMuteEnabled || isReplaceAudioEnabled,
                backdrop = backdrop,
                onClick = { onSelectCategory(if (activeCategory == EditorToolCategory.AUDIO) null else EditorToolCategory.AUDIO) }
            )
            LiquidEditorChip(
                label = "Tốc độ",
                icon = Icons.Filled.Speed,
                isActive = activeCategory == EditorToolCategory.SPEED,
                hasActiveOp = speedMultiplier != 1.0f,
                backdrop = backdrop,
                onClick = { onSelectCategory(if (activeCategory == EditorToolCategory.SPEED) null else EditorToolCategory.SPEED) }
            )
            LiquidEditorChip(
                label = "Xoay",
                icon = Icons.Filled.RotateRight,
                isActive = activeCategory == EditorToolCategory.ROTATE,
                hasActiveOp = rotationDegrees != 0f,
                backdrop = backdrop,
                onClick = { onSelectCategory(if (activeCategory == EditorToolCategory.ROTATE) null else EditorToolCategory.ROTATE) }
            )
            LiquidEditorChip(
                label = "Điều chỉnh",
                icon = Icons.Filled.Tune,
                isActive = activeCategory == EditorToolCategory.ADJUST,
                hasActiveOp = brightness != 0f || contrast != 0f || saturation != 0f ||
                        warmth != 0f || hue != 0f || blur != 0f,
                backdrop = backdrop,
                onClick = { onSelectCategory(if (activeCategory == EditorToolCategory.ADJUST) null else EditorToolCategory.ADJUST) }
            )
        }
    }
}

// ── Liquid Glass Context Panel Container ──

@Composable
private fun LiquidContextPanel(
    backdrop: Backdrop,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20f.dp) },
                effects = {
                    vibrancy()
                    blur(6f.dp.toPx())
                    lens(12f.dp.toPx(), 20f.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = 0.25f) },
                shadow = { Shadow(radius = 10f.dp, color = Color.Black.copy(alpha = 0.2f)) },
                innerShadow = { InnerShadow(radius = 4f.dp, alpha = 0.08f) },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.04f))
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        content()
    }
}

// ── Sub-components ──

data class ToggleItem(
    val label: String,
    val isActive: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun ToggleRow(items: List<ToggleItem>) {
    val accentColor = Color(0xFF007AFF)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (item.isActive) accentColor else Color.White.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        if (item.isActive) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { item.onClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (item.isActive) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SpeedPanel(currentSpeed: Float, onSpeedChange: (Float) -> Unit) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f)
    val accentColor = Color(0xFF007AFF)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tốc độ phát: ${currentSpeed}x",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            speeds.forEach { speed ->
                val isSelected = currentSpeed == speed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.06f))
                        .border(
                            0.5.dp,
                            if (isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.06f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSpeedChange(speed) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${speed}x",
                        color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RotatePanel(currentDegrees: Float, onRotate: () -> Unit) {
    val accentColor = Color(0xFF007AFF)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .clickable { onRotate() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.RotateRight,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.height(22.dp)
                )
                Text(
                    text = "Xoay 90°",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${currentDegrees.toInt()}°",
                color = if (currentDegrees != 0f) accentColor else Color(0xFF9CA3AF),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdjustPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    hue: Float,
    blur: Float,
    backdrop: Backdrop,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onWarmthChange: (Float) -> Unit,
    onHueChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onResetAdjust: () -> Unit
) {
    val hasChanges = brightness != 0f || contrast != 0f || saturation != 0f ||
            warmth != 0f || hue != 0f || blur != 0f

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Top row with label & Reset button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "THÔNG SỐ MÀU SẮC",
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            if (hasChanges) {
                Text(
                    text = "Đặt lại",
                    color = Color(0xFF0088FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onResetAdjust() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Vertically scrollable adjustments list
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 195.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdjustSlider(
                label = "Độ sáng",
                value = brightness,
                onValueChange = onBrightnessChange,
                icon = CupertinoIcons.Outlined.SunMax,
                backdrop = backdrop
            )
            AdjustSlider(
                label = "Tương phản",
                value = contrast,
                onValueChange = onContrastChange,
                icon = CupertinoIcons.Filled.CircleLefthalfed,
                backdrop = backdrop
            )
            AdjustSlider(
                label = "Bão hòa",
                value = saturation,
                onValueChange = onSaturationChange,
                icon = CupertinoIcons.Outlined.Paintpalette,
                backdrop = backdrop
            )
            AdjustSlider(
                label = "Độ ấm",
                value = warmth,
                onValueChange = onWarmthChange,
                icon = CupertinoIcons.Outlined.Flame,
                backdrop = backdrop
            )
            AdjustSlider(
                label = "Tông màu",
                value = hue / 180f,
                onValueChange = { onHueChange(it * 180f) },
                icon = CupertinoIcons.Outlined.SliderHorizontal3,
                displayValue = "${hue.toInt()}°",
                backdrop = backdrop
            )
            AdjustSlider(
                label = "Làm mờ",
                value = blur,
                onValueChange = onBlurChange,
                icon = CupertinoIcons.Outlined.CameraFilters,
                valueRange = 0f..1f,
                backdrop = backdrop
            )
        }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: ImageVector,
    backdrop: Backdrop,
    valueRange: ClosedFloatingPointRange<Float> = -1f..1f,
    displayValue: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (value != 0f) Color.White else Color(0xFF9CA3AF),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (value != 0f) Color.White else Color(0xFF9CA3AF),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.width(74.dp)
        )
        com.buwin.tiktokvideodownload.ui.components.liquid.LiquidSlider(
            value = { value },
            onValueChange = onValueChange,
            valueRange = valueRange,
            visibilityThreshold = 0.001f,
            backdrop = backdrop,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = displayValue ?: "${(value * 100).toInt()}",
            color = if (value != 0f) Color.White else Color(0xFF666666),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}
