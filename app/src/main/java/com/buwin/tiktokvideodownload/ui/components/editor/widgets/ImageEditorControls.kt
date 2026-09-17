package com.buwin.tiktokvideodownload.ui.components.editor.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.components.editor.model.ImageAdjustTool
import com.buwin.tiktokvideodownload.ui.components.editor.model.ImageEditorTab
import com.buwin.tiktokvideodownload.ui.components.editor.model.ImagePresetFilter
import com.kyant.backdrop.Backdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.CameraFilters
import io.github.alexzhirkevich.cupertino.icons.outlined.RotateLeft
import io.github.alexzhirkevich.cupertino.icons.outlined.RotateRight
import kotlin.math.roundToInt

/**
 * Animated controls for ADJUST, FILTERS, and CROP tabs.
 * Hosts tool selectors, indicator labels, and 3D scrubber dial.
 */
@Composable
fun ImageEditorControls(
    currentTab: ImageEditorTab,
    currentAdjustTool: ImageAdjustTool,
    isAutoEnhanced: Boolean,
    autoEnhanceAmount: Float,
    exposureValue: Float,
    brillianceValue: Float,
    contrastValue: Float,
    saturationValue: Float,
    warmthValue: Float,
    tintValue: Float,
    selectedFilter: ImagePresetFilter,
    filterIntensity: Float,
    rotationDegrees: Float,
    isFlippedHorizontal: Boolean,
    straightenAngle: Float,
    backdrop: Backdrop,
    onAdjustToolSelected: (ImageAdjustTool) -> Unit,
    onAdjustValueChange: (tool: ImageAdjustTool, value: Float) -> Unit,
    onFilterSelected: (ImagePresetFilter) -> Unit,
    onFilterIntensityChange: (Float) -> Unit,
    onRotate90: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onStraightenAngleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    AnimatedContent(
        targetState = currentTab,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally(
                initialOffsetX = { fullWidth -> direction * (fullWidth / 3) },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(180))) togetherWith
            (slideOutHorizontally(
                targetOffsetX = { fullWidth -> -direction * (fullWidth / 3) },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(tween(120)))
        },
        label = "editor_tab_content",
        modifier = modifier
    ) { tab ->
        when (tab) {
            ImageEditorTab.ADJUST -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row of Circular Adjust Tools
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImageAdjustTool.entries.forEach { tool ->
                            val isSelected = tool == currentAdjustTool
                            val toolVal = when (tool) {
                                ImageAdjustTool.AUTO -> autoEnhanceAmount
                                ImageAdjustTool.EXPOSURE -> exposureValue
                                ImageAdjustTool.BRILLIANCE -> brillianceValue
                                ImageAdjustTool.CONTRAST -> contrastValue
                                ImageAdjustTool.SATURATION -> saturationValue
                                ImageAdjustTool.WARMTH -> warmthValue
                                ImageAdjustTool.TINT -> tintValue
                            }
                            val hasToolChange = toolVal != 0f || (tool == ImageAdjustTool.AUTO && isAutoEnhanced)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAdjustToolSelected(tool)
                                }
                            ) {
                                EditorCircleToolButton(
                                    icon = tool.icon,
                                    isSelected = isSelected,
                                    hasChange = hasToolChange,
                                    backdrop = backdrop,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAdjustToolSelected(tool)
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tool.displayName,
                                    color = if (isSelected) Color(0xFFFFD60A) else Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Current tool value text indicator
                    val currentVal = when (currentAdjustTool) {
                        ImageAdjustTool.AUTO -> autoEnhanceAmount
                        ImageAdjustTool.EXPOSURE -> exposureValue
                        ImageAdjustTool.BRILLIANCE -> brillianceValue
                        ImageAdjustTool.CONTRAST -> contrastValue
                        ImageAdjustTool.SATURATION -> saturationValue
                        ImageAdjustTool.WARMTH -> warmthValue
                        ImageAdjustTool.TINT -> tintValue
                    }
                    val displayValStr = when {
                        currentVal > 0f -> "+${currentVal.roundToInt()}"
                        currentVal < 0f -> "${currentVal.roundToInt()}"
                        else -> "0"
                    }
                    Text(
                        text = displayValStr,
                        color = if (currentVal != 0f) Color(0xFFFFD60A) else Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // 3D Cylindrical Scrubber Dial
                    EditorScrubberDial(
                        value = currentVal,
                        onValueChange = { newVal ->
                            onAdjustValueChange(currentAdjustTool, newVal)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(vertical = 4.dp)
                    )
                }
            }

            ImageEditorTab.FILTERS -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row of Circular Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImagePresetFilter.entries.forEach { filter ->
                            val isSelected = filter == selectedFilter
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onFilterSelected(filter)
                                }
                            ) {
                                EditorCircleToolButton(
                                    icon = CupertinoIcons.Outlined.CameraFilters,
                                    isSelected = isSelected,
                                    hasChange = isSelected && filter != ImagePresetFilter.ORIGINAL,
                                    backdrop = backdrop,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onFilterSelected(filter)
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = filter.displayName,
                                    color = if (isSelected) Color(0xFFFFD60A) else Color(0xFF8E8E93),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Filter intensity scrubber
                    Text(
                        text = "${filterIntensity.roundToInt()}%",
                        color = if (selectedFilter != ImagePresetFilter.ORIGINAL) Color(0xFFFFD60A) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    EditorScrubberDial(
                        value = filterIntensity,
                        onValueChange = onFilterIntensityChange,
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(vertical = 4.dp)
                    )
                }
            }

            ImageEditorTab.CROP -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Rotate & Flip buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotate 90
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EditorCircleToolButton(
                                icon = CupertinoIcons.Outlined.RotateRight,
                                isSelected = false,
                                hasChange = rotationDegrees != 0f,
                                backdrop = backdrop,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRotate90()
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Xoay 90°",
                                color = if (rotationDegrees != 0f) Color(0xFFFFD60A) else Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                fontWeight = if (rotationDegrees != 0f) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Flip Horizontal
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EditorCircleToolButton(
                                icon = CupertinoIcons.Outlined.RotateLeft,
                                isSelected = isFlippedHorizontal,
                                hasChange = isFlippedHorizontal,
                                backdrop = backdrop,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onFlipHorizontal()
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lật ngang",
                                color = if (isFlippedHorizontal) Color(0xFFFFD60A) else Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                fontWeight = if (isFlippedHorizontal) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    // Angle Straighten Value
                    Text(
                        text = "${straightenAngle.roundToInt()}°",
                        color = if (straightenAngle != 0f) Color(0xFFFFD60A) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    // Straighten Angle Scrubber (-45..+45)
                    EditorScrubberDial(
                        value = straightenAngle,
                        onValueChange = onStraightenAngleChange,
                        valueRange = -45f..45f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
