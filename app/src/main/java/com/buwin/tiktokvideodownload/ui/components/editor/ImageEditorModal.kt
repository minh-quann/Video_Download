package com.buwin.tiktokvideodownload.ui.components.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.liquid.InteractiveHighlight
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidButton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kyant.shapes.Capsule
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.CircleLefthalfed
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowTurnUpLeft
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowTurnUpRight
import io.github.alexzhirkevich.cupertino.icons.outlined.CameraFilters
import io.github.alexzhirkevich.cupertino.icons.outlined.Crop
import io.github.alexzhirkevich.cupertino.icons.outlined.CropRotate
import io.github.alexzhirkevich.cupertino.icons.outlined.Flame
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.RotateLeft
import io.github.alexzhirkevich.cupertino.icons.outlined.RotateRight
import io.github.alexzhirkevich.cupertino.icons.outlined.SliderHorizontal3
import io.github.alexzhirkevich.cupertino.icons.outlined.SunMax
import io.github.alexzhirkevich.cupertino.icons.outlined.WandAndStars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

// ── Apple Photos Tab Enum ──
enum class AppleEditorTab(val title: String) {
    ADJUST("ADJUST"),
    FILTERS("FILTERS"),
    CROP("CROP")
}

// ── Apple Photos Adjustment Tool Enum ──
enum class AppleAdjustTool(val displayName: String, val icon: ImageVector) {
    AUTO("TỰ ĐỘNG", CupertinoIcons.Outlined.WandAndStars),
    EXPOSURE("PHƠI SÁNG", CupertinoIcons.Outlined.SunMax),
    BRILLIANCE("ĐỘ SÁNG", CupertinoIcons.Outlined.SunMax),
    CONTRAST("TƯƠNG PHẢN", CupertinoIcons.Filled.CircleLefthalfed),
    SATURATION("BÃO HÒA", CupertinoIcons.Outlined.Paintpalette),
    WARMTH("ĐỘ ẤM", CupertinoIcons.Outlined.Flame),
    TINT("TÔNG MÀU", CupertinoIcons.Outlined.SliderHorizontal3)
}

// ── Apple Photos Preset Filters ──
enum class ApplePresetFilter(val displayName: String) {
    ORIGINAL("Gốc"),
    VIVID("Vivid"),
    VIVID_WARM("Vivid Warm"),
    VIVID_COOL("Vivid Cool"),
    DRAMATIC("Dramatic"),
    DRAMATIC_WARM("Dramatic Warm"),
    DRAMATIC_COOL("Dramatic Cool"),
    MONO("Mono"),
    SILVERTONE("Silvertone"),
    NOIR("Noir")
}

/**
 * Authentic 1:1 Apple Photos Editor Modal with genuine Liquid Glass on ALL buttons.
 * - Ambient backdrop layer captures full-bleed photo colors so glass elements refract rich hues.
 * - Header: Cancel & Done Liquid Glass buttons, Undo/Redo & Markup/More Liquid Glass capsules.
 * - Controls: Liquid Glass round buttons with specular glass gradient, chromatic aberration lens, and rubber-band squash & stretch.
 * - Bottom: Floating Liquid Glass capsule tab bar with specular sheen, inner shadow, and lens refraction.
 * - Dial: 3D Cylindrical tick ruler with magnetic snap to 0 and tactile haptic feedback.
 */
@Composable
fun ImageEditorModal(
    record: DownloadRecord,
    downloadHelper: DownloadManagerHelper,
    onDismiss: () -> Unit,
    onExportSuccess: (DownloadRecord) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val contentBackdrop = rememberLayerBackdrop()
    val imageUri = remember(record) { downloadHelper.getDownloadedUri(record) }

    // ── Main State ──
    var currentTab by remember { mutableStateOf(AppleEditorTab.ADJUST) }
    var currentAdjustTool by remember { mutableStateOf(AppleAdjustTool.AUTO) }

    // Adjust parameters (-100f .. 100f)
    var isAutoEnhanced by remember { mutableStateOf(false) }
    var autoEnhanceAmount by remember { mutableFloatStateOf(0f) }
    var exposureValue by remember { mutableFloatStateOf(0f) }
    var brillianceValue by remember { mutableFloatStateOf(0f) }
    var contrastValue by remember { mutableFloatStateOf(0f) }
    var saturationValue by remember { mutableFloatStateOf(0f) }
    var warmthValue by remember { mutableFloatStateOf(0f) }
    var tintValue by remember { mutableFloatStateOf(0f) }

    // Filter state
    var selectedFilter by remember { mutableStateOf(ApplePresetFilter.ORIGINAL) }
    var filterIntensity by remember { mutableFloatStateOf(100f) } // 0..100%

    // Crop / Orientation state
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var straightenAngle by remember { mutableFloatStateOf(0f) } // -45..+45 degrees

    var isSaving by remember { mutableStateOf(false) }

    val hasChanges = isAutoEnhanced || exposureValue != 0f || brillianceValue != 0f ||
            contrastValue != 0f || saturationValue != 0f || warmthValue != 0f || tintValue != 0f ||
            selectedFilter != ApplePresetFilter.ORIGINAL || rotationDegrees != 0f ||
            isFlippedHorizontal || straightenAngle != 0f

    // Compute live Compose ColorMatrix
    val composeColorMatrix = remember(
        isAutoEnhanced, autoEnhanceAmount, exposureValue, brillianceValue,
        contrastValue, saturationValue, warmthValue, tintValue, selectedFilter, filterIntensity
    ) {
        buildAppleColorMatrix(
            isAuto = isAutoEnhanced,
            autoVal = autoEnhanceAmount,
            exposure = exposureValue,
            brilliance = brillianceValue,
            contrast = contrastValue,
            saturation = saturationValue,
            warmth = warmthValue,
            tint = tintValue,
            filter = selectedFilter,
            filterIntensity = filterIntensity
        )
    }

    BackHandler(enabled = true) {
        if (!isSaving) onDismiss()
    }

    // Save edited image
    fun executeSave() {
        if (!hasChanges) {
            onDismiss()
            return
        }
        isSaving = true
        coroutineScope.launch {
            try {
                val newRecord = withContext(Dispatchers.IO) {
                    exportAppleEditedImage(
                        context = context,
                        sourceUri = imageUri,
                        record = record,
                        rotationDegrees = rotationDegrees + straightenAngle,
                        isFlippedHorizontal = isFlippedHorizontal,
                        isAutoEnhanced = isAutoEnhanced,
                        autoEnhanceAmount = autoEnhanceAmount,
                        exposure = exposureValue,
                        brilliance = brillianceValue,
                        contrast = contrastValue,
                        saturation = saturationValue,
                        warmth = warmthValue,
                        tint = tintValue,
                        filter = selectedFilter,
                        filterIntensity = filterIntensity,
                        downloadHelper = downloadHelper
                    )
                }
                isSaving = false
                AppToast.showSuccess("Đã lưu ảnh chỉnh sửa", newRecord.title)
                onExportSuccess(newRecord)
                onDismiss()
            } catch (e: Exception) {
                isSaving = false
                AppToast.showError("Không thể lưu ảnh", e.localizedMessage ?: "Lỗi xử lý hình ảnh")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 0. SOURCE CONTENT LAYER (Captured by contentBackdrop for glass refraction) ──
        Box(
            modifier = Modifier
                .layerBackdrop(contentBackdrop)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Center Sharp Image Canvas (with spacious padding to avoid touching controls)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 255.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri ?: record.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = record.title,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.colorMatrix(composeColorMatrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.rotationZ = rotationDegrees + straightenAngle
                            this.scaleX = if (isFlippedHorizontal) -1f else 1f
                        },
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFD60A),
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                )
            }
        }


        // ── 1. TOP BAR (Clean Apple Header: Cancel | Undo/Redo | Done) ──
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Cancel
            LiquidButton(
                onClick = onDismiss,
                backdrop = contentBackdrop,
                showBorder = false,
                surfaceColor = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }

            // Center: Undo / Redo Liquid Glass Capsule (bước trước / bước tiếp)
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .drawBackdrop(
                        backdrop = contentBackdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(3f.dp.toPx())
                            lens(12f.dp.toPx(), 24f.dp.toPx())
                        },
                        highlight = null,
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.20f))
                        }
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Outlined.ArrowTurnUpLeft,
                        contentDescription = "Undo",
                        tint = if (hasChanges) Color.White else Color(0xFF636366),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(
                                enabled = hasChanges,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    when (currentAdjustTool) {
                                        AppleAdjustTool.AUTO -> { isAutoEnhanced = false; autoEnhanceAmount = 0f }
                                        AppleAdjustTool.EXPOSURE -> exposureValue = 0f
                                        AppleAdjustTool.BRILLIANCE -> brillianceValue = 0f
                                        AppleAdjustTool.CONTRAST -> contrastValue = 0f
                                        AppleAdjustTool.SATURATION -> saturationValue = 0f
                                        AppleAdjustTool.WARMTH -> warmthValue = 0f
                                        AppleAdjustTool.TINT -> tintValue = 0f
                                    }
                                }
                            )
                    )
                    Icon(
                        imageVector = CupertinoIcons.Outlined.ArrowTurnUpRight,
                        contentDescription = "Redo",
                        tint = Color(0xFF636366),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Right: Done
            LiquidButton(
                onClick = { executeSave() },
                backdrop = contentBackdrop,
                isInteractive = hasChanges && !isSaving,
                showBorder = false,
                surfaceColor = if (hasChanges) Color(0xFFFFD60A).copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier.height(36.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFD60A),
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Done",
                        color = if (hasChanges) Color(0xFFFFD60A) else Color(0xFF636366),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        }

        // ── 3. BOTTOM CONTROLS & APPLE 3D SCRUBBER DIAL ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                label = "apple_editor_tab_content"
            ) { tab ->
                when (tab) {
                    AppleEditorTab.ADJUST -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Row of Circular Adjust Tools with genuine Liquid Glass
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppleAdjustTool.entries.forEach { tool ->
                                    val isSelected = tool == currentAdjustTool
                                    val toolVal = when (tool) {
                                        AppleAdjustTool.AUTO -> autoEnhanceAmount
                                        AppleAdjustTool.EXPOSURE -> exposureValue
                                        AppleAdjustTool.BRILLIANCE -> brillianceValue
                                        AppleAdjustTool.CONTRAST -> contrastValue
                                        AppleAdjustTool.SATURATION -> saturationValue
                                        AppleAdjustTool.WARMTH -> warmthValue
                                        AppleAdjustTool.TINT -> tintValue
                                    }
                                    val hasToolChange = toolVal != 0f || (tool == AppleAdjustTool.AUTO && isAutoEnhanced)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            currentAdjustTool = tool
                                            if (tool == AppleAdjustTool.AUTO && !isAutoEnhanced) {
                                                isAutoEnhanced = true
                                                autoEnhanceAmount = 50f
                                            }
                                        }
                                    ) {
                                        AppleCircleToolButton(
                                            icon = tool.icon,
                                            isSelected = isSelected,
                                            hasChange = hasToolChange,
                                            backdrop = contentBackdrop,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                currentAdjustTool = tool
                                                if (tool == AppleAdjustTool.AUTO && !isAutoEnhanced) {
                                                    isAutoEnhanced = true
                                                    autoEnhanceAmount = 50f
                                                }
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

                            // Current tool value text indicator with smooth pop
                            val currentVal = when (currentAdjustTool) {
                                AppleAdjustTool.AUTO -> autoEnhanceAmount
                                AppleAdjustTool.EXPOSURE -> exposureValue
                                AppleAdjustTool.BRILLIANCE -> brillianceValue
                                AppleAdjustTool.CONTRAST -> contrastValue
                                AppleAdjustTool.SATURATION -> saturationValue
                                AppleAdjustTool.WARMTH -> warmthValue
                                AppleAdjustTool.TINT -> tintValue
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

                            // Apple 3D Cylindrical Scrubber Dial
                            AppleScrubberDial(
                                value = currentVal,
                                onValueChange = { newVal ->
                                    when (currentAdjustTool) {
                                        AppleAdjustTool.AUTO -> {
                                            isAutoEnhanced = newVal != 0f
                                            autoEnhanceAmount = newVal
                                        }
                                        AppleAdjustTool.EXPOSURE -> exposureValue = newVal
                                        AppleAdjustTool.BRILLIANCE -> brillianceValue = newVal
                                        AppleAdjustTool.CONTRAST -> contrastValue = newVal
                                        AppleAdjustTool.SATURATION -> saturationValue = newVal
                                        AppleAdjustTool.WARMTH -> warmthValue = newVal
                                        AppleAdjustTool.TINT -> tintValue = newVal
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    AppleEditorTab.FILTERS -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Row of Circular Filters with genuine Liquid Glass
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ApplePresetFilter.entries.forEach { filter ->
                                    val isSelected = filter == selectedFilter
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedFilter = filter
                                        }
                                    ) {
                                        AppleCircleToolButton(
                                            icon = CupertinoIcons.Outlined.CameraFilters,
                                            isSelected = isSelected,
                                            hasChange = isSelected && filter != ApplePresetFilter.ORIGINAL,
                                            backdrop = contentBackdrop,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedFilter = filter
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
                                color = if (selectedFilter != ApplePresetFilter.ORIGINAL) Color(0xFFFFD60A) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            AppleScrubberDial(
                                value = filterIntensity,
                                onValueChange = { filterIntensity = it },
                                valueRange = 0f..100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    AppleEditorTab.CROP -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Rotate & Flip buttons with genuine Liquid Glass
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rotate 90 Liquid Glass Button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AppleCircleToolButton(
                                        icon = CupertinoIcons.Outlined.RotateRight,
                                        isSelected = false,
                                        hasChange = rotationDegrees != 0f,
                                        backdrop = contentBackdrop,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            rotationDegrees = (rotationDegrees + 90f) % 360f
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

                                // Flip Horizontal Liquid Glass Button
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AppleCircleToolButton(
                                        icon = CupertinoIcons.Outlined.RotateLeft,
                                        isSelected = isFlippedHorizontal,
                                        hasChange = isFlippedHorizontal,
                                        backdrop = contentBackdrop,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isFlippedHorizontal = !isFlippedHorizontal
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
                            AppleScrubberDial(
                                value = straightenAngle,
                                onValueChange = { straightenAngle = it },
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

            Spacer(modifier = Modifier.height(8.dp))

            // ── 4. BOTTOM FLOATING LIQUID GLASS CAPSULE TAB BAR ──
            val bottomTabInteractiveHighlight = remember(coroutineScope) {
                InteractiveHighlight(animationScope = coroutineScope)
            }

            Box(
                modifier = Modifier
                    .height(72.dp)
                    .width(248.dp)
                    .drawBackdrop(
                        backdrop = contentBackdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(3f.dp.toPx())
                            lens(12f.dp.toPx(), 24f.dp.toPx())
                        },
                        highlight = null,
                        layerBlock = {
                            val progress = bottomTabInteractiveHighlight.pressProgress
                            val offset = bottomTabInteractiveHighlight.offset
                            val dragDist = hypot(offset.x, offset.y)
                            val offsetAngle = atan2(offset.y, offset.x)

                            // Apple Fluid Interface: Damped rubber-band displacement
                            val maxDisplacement = (this.size.minDimension * 0.32f).coerceAtMost(14f.dp.toPx())
                            val dampingDistance = (this.size.minDimension * 1.5f).coerceAtLeast(36f.dp.toPx())
                            val dampedDistance = maxDisplacement * tanh(dragDist / dampingDistance)

                            translationX = dampedDistance * cos(offsetAngle)
                            translationY = dampedDistance * sin(offsetAngle)

                            // Apple Liquid Glass tactile deformation: Volume-preserving squash & stretch
                            val maxStretch = 0.20f
                            val stretchFactor = maxStretch * tanh(dragDist / dampingDistance)
                            val scaleAlong = 1f + stretchFactor
                            val scalePerp = 1f / sqrt(scaleAlong)

                            // Subtle tactile pop on press
                            val pressScale = lerp(1f, 1.035f, progress)

                            val cos2 = cos(offsetAngle) * cos(offsetAngle)
                            val sin2 = sin(offsetAngle) * sin(offsetAngle)
                            scaleX = pressScale * (scaleAlong * cos2 + scalePerp * sin2)
                            scaleY = pressScale * (scaleAlong * sin2 + scalePerp * cos2)
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.20f))
                        }
                    )
                    .then(bottomTabInteractiveHighlight.modifier)
                    .then(bottomTabInteractiveHighlight.gestureModifier)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleBottomPillTab(
                        label = "Adjust",
                        isSelected = currentTab == AppleEditorTab.ADJUST,
                        modifier = Modifier.weight(1f),
                        iconContent = { tint ->
                            AppleAdjustDialIcon(tint = tint)
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentTab = AppleEditorTab.ADJUST
                        }
                    )

                    AppleBottomPillTab(
                        label = "Filters",
                        isSelected = currentTab == AppleEditorTab.FILTERS,
                        modifier = Modifier.weight(1f),
                        iconContent = { tint ->
                            Icon(
                                imageVector = CupertinoIcons.Outlined.CameraFilters,
                                contentDescription = "Filters",
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentTab = AppleEditorTab.FILTERS
                        }
                    )

                    AppleBottomPillTab(
                        label = "Crop",
                        isSelected = currentTab == AppleEditorTab.CROP,
                        modifier = Modifier.weight(1f),
                        iconContent = { tint ->
                            Icon(
                                imageVector = CupertinoIcons.Outlined.CropRotate,
                                contentDescription = "Crop",
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentTab = AppleEditorTab.CROP
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ── Sub-components matching Apple Photos ──

/**
 * Authentic Apple Liquid Glass Circular Tool Button.
 * Uses LiquidRoundButton without border to match the player bottom bar style.
 */
@Composable
private fun AppleCircleToolButton(
    icon: ImageVector,
    isSelected: Boolean,
    hasChange: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    LiquidRoundButton(
        onClick = onClick,
        backdrop = backdrop,
        size = 46.dp,
        showBorder = false,
        surfaceColor = when {
            isSelected -> Color(0xFFFFD60A).copy(alpha = 0.35f)
            hasChange -> Color(0xFFFFD60A).copy(alpha = 0.20f)
            else -> Color.White.copy(alpha = 0.20f)
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected || hasChange) Color(0xFFFFD60A) else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Authentic Apple Photos 3D Cylindrical Scrubber Dial.
 * Features:
 * - 3D cylindrical perspective projection: tick marks curve around a virtual roller wheel.
 * - Magnetic snap detent at 0 with deep tactile feedback.
 * - Tactile tick haptic clicks on step crossings.
 * - Center gold needle with triangle pointer.
 */
@Composable
private fun AppleScrubberDial(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticStep by remember { mutableIntStateOf(value.roundToInt()) }
    var rawAccumulator by remember(value) { mutableFloatStateOf(value) }

    ComposeCanvas(
        modifier = modifier
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Sensitivity: 1px = 0.38 units for fine precision
                        val delta = -dragAmount.x * 0.38f
                        var candidate = (rawAccumulator + delta).coerceIn(valueRange)

                        // Apple Magnetic Snap around 0
                        if (abs(candidate) < 1.4f) {
                            candidate = 0f
                        }
                        rawAccumulator = candidate

                        val candidateInt = candidate.roundToInt()
                        if (candidateInt != lastHapticStep) {
                            if (candidateInt == 0) {
                                // Deeper haptic click at 0
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else if (candidateInt % 5 == 0) {
                                // Tick haptic every 5 units
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            lastHapticStep = candidateInt
                        }

                        onValueChange(candidate)
                    },
                    onDragEnd = {
                        // Settle snap if very close to integer
                        val rounded = (rawAccumulator).roundToInt().toFloat()
                        if (abs(rawAccumulator - rounded) < 0.35f) {
                            onValueChange(rounded)
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val tickSpacing = 11.dp.toPx()
        val yellowAccent = Color(0xFFFFD60A)

        // Virtual 3D Cylinder radius
        val radius = width * 0.52f

        // Draw tick lines around virtual cylinder
        for (i in -50..50) {
            val tickVal = i * 2.5f
            val deltaFromCurrent = tickVal - value
            val linearOffset = deltaFromCurrent * (tickSpacing / 2.5f)

            // Cylindrical angle theta
            val theta = linearOffset / radius
            if (theta in -1.52f..1.52f) {
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)

                // 3D projected screen X
                val xPos = centerX + radius * sinTheta

                // Depth scaling and perspective falloff
                val depthScale = (cosTheta * cosTheta).coerceIn(0f, 1f)
                val isZero = i == 0
                val isMajor = i % 10 == 0
                val isMedium = i % 5 == 0

                val baseHeight = when {
                    isMajor -> 22.dp.toPx()
                    isMedium -> 15.dp.toPx()
                    else -> 9.dp.toPx()
                }
                val tickHeight = baseHeight * (0.35f + 0.65f * depthScale)

                val baseAlpha = when {
                    isZero -> 0.95f
                    isMajor -> 0.70f
                    isMedium -> 0.45f
                    else -> 0.22f
                }
                val alpha = (baseAlpha * depthScale * depthScale).coerceIn(0f, 1f)

                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(xPos, centerY - tickHeight / 2f),
                    end = Offset(xPos, centerY + tickHeight / 2f),
                    strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.2.dp.toPx()
                )
            }
        }

        // Center stationary reference needle (Yellow when offset != 0, White when neutral)
        val centerLineHeight = 26.dp.toPx()
        val isNonZero = abs(value) > 0.05f
        drawLine(
            color = if (isNonZero) yellowAccent else Color.White,
            start = Offset(centerX, centerY - centerLineHeight / 2f),
            end = Offset(centerX, centerY + centerLineHeight / 2f),
            strokeWidth = 2.dp.toPx()
        )

        // Yellow triangle pointer on top center
        val trianglePath = Path().apply {
            moveTo(centerX - 4.dp.toPx(), centerY - centerLineHeight / 2f - 4.dp.toPx())
            lineTo(centerX + 4.dp.toPx(), centerY - centerLineHeight / 2f - 4.dp.toPx())
            lineTo(centerX, centerY - centerLineHeight / 2f)
            close()
        }
        drawPath(trianglePath, color = yellowAccent)
    }
}

/**
 * Downward pointing yellow triangle indicator matching Apple Photos app tab selector.
 */
@Composable
private fun ActiveIndicatorTriangle(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD60A)
) {
    ComposeCanvas(modifier = modifier.size(width = 6.5.dp, height = 4.5.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path, color = color)
    }
}

/**
 * Custom Apple Photos Adjust dial icon with circular gauge and 8 surrounding tick dots.
 * Matches iOS Photos app Adjust tab icon pixel-perfectly.
 */
@Composable
private fun AppleAdjustDialIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    ComposeCanvas(modifier = modifier.size(24.dp)) {
        val strokeW = 1.6.dp.toPx()
        val center = this.center
        val radius = size.minDimension * 0.27f

        // Center dial circle
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = strokeW)
        )

        // Center dial needle pointer (pointing at ~45 degrees up-right: -45 deg in screen coordinates)
        val pointerAngleRad = Math.toRadians(-45.0).toFloat()
        val pointerLength = radius * 0.82f
        val pointerEnd = Offset(
            x = center.x + pointerLength * cos(pointerAngleRad),
            y = center.y + pointerLength * sin(pointerAngleRad)
        )
        drawLine(
            color = tint,
            start = center,
            end = pointerEnd,
            strokeWidth = strokeW * 1.15f,
            cap = StrokeCap.Round
        )

        // 8 outer dots symmetrically spaced at 45-degree intervals
        val dotOrbitRadius = size.minDimension * 0.44f
        val dotRadius = 1.2.dp.toPx()
        for (i in 0 until 8) {
            val angleRad = Math.toRadians(i * 45.0).toFloat()
            val dotCenter = Offset(
                x = center.x + dotOrbitRadius * cos(angleRad),
                y = center.y + dotOrbitRadius * sin(angleRad)
            )
            drawCircle(
                color = tint,
                radius = dotRadius,
                center = dotCenter
            )
        }
    }
}

/**
 * Bottom Floating Pill Tab with yellow active indicator triangle & subtle spring response.
 * Exactly matches Apple Photos iOS 17/18 styling.
 */
@Composable
private fun AppleBottomPillTab(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    iconContent: @Composable (tint: Color) -> Unit,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tab_scale"
    )

    val triangleScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "triangle_scale"
    )

    val contentColor = if (isSelected) Color(0xFFFFD60A) else Color(0xFF9E9EA4)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Yellow active indicator triangle on top pointing downward (Apple Photos signature)
        Box(
            modifier = Modifier
                .height(5.dp)
                .graphicsLayer {
                    scaleX = triangleScale
                    scaleY = triangleScale
                    alpha = triangleScale
                },
            contentAlignment = Alignment.Center
        ) {
            ActiveIndicatorTriangle(
                color = Color(0xFFFFD60A)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Tab Icon (24.dp)
        iconContent(contentColor)

        Spacer(modifier = Modifier.height(3.dp))

        // Tab Label (11.sp)
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ── ColorMatrix & Bitmap Export Pipeline ──

private fun buildAppleColorMatrix(
    isAuto: Boolean,
    autoVal: Float,
    exposure: Float,
    brilliance: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    tint: Float,
    filter: ApplePresetFilter,
    filterIntensity: Float
): ColorMatrix {
    val matrix = ColorMatrix()

    // Auto Enhance calculation
    val autoBright = if (isAuto) autoVal * 0.3f else 0f
    val autoCont = if (isAuto) autoVal * 0.2f else 0f
    val autoSat = if (isAuto) autoVal * 0.15f else 0f

    val bScale = (brilliance + autoBright) * 1.5f + exposure * 1.2f
    val cScale = 1f + (contrast + autoCont) / 100f
    val sScale = 1f + (saturation + autoSat) / 100f

    val cm = floatArrayOf(
        cScale, 0f, 0f, 0f, bScale + (warmth * 0.6f),
        0f, cScale, 0f, 0f, bScale + (tint * 0.5f),
        0f, 0f, cScale, 0f, bScale - (warmth * 0.6f),
        0f, 0f, 0f, 1f, 0f
    )
    matrix.set(ColorMatrix(cm))

    val satMat = ColorMatrix().apply { setToSaturation(sScale.coerceAtLeast(0f)) }
    matrix.timesAssign(satMat)

    // Preset filter weight
    val intensityWeight = filterIntensity / 100f
    when (filter) {
        ApplePresetFilter.VIVID -> {
            val vividSat = ColorMatrix().apply { setToSaturation(1f + 0.35f * intensityWeight) }
            matrix.timesAssign(vividSat)
        }
        ApplePresetFilter.VIVID_WARM -> {
            val warmMat = ColorMatrix(floatArrayOf(
                1f + 0.1f * intensityWeight, 0f, 0f, 0f, 15f * intensityWeight,
                0f, 1f + 0.05f * intensityWeight, 0f, 0f, 5f * intensityWeight,
                0f, 0f, 1f - 0.08f * intensityWeight, 0f, -10f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(warmMat)
        }
        ApplePresetFilter.VIVID_COOL -> {
            val coolMat = ColorMatrix(floatArrayOf(
                1f - 0.08f * intensityWeight, 0f, 0f, 0f, -8f * intensityWeight,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f + 0.15f * intensityWeight, 0f, 18f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(coolMat)
        }
        ApplePresetFilter.DRAMATIC -> {
            val dramMat = ColorMatrix(floatArrayOf(
                1f + 0.25f * intensityWeight, 0f, 0f, 0f, -10f * intensityWeight,
                0f, 1f + 0.25f * intensityWeight, 0f, 0f, -10f * intensityWeight,
                0f, 0f, 1f + 0.25f * intensityWeight, 0f, -10f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(dramMat)
        }
        ApplePresetFilter.DRAMATIC_WARM -> {
            val dramWarmMat = ColorMatrix(floatArrayOf(
                1f + 0.25f * intensityWeight, 0f, 0f, 0f, 10f * intensityWeight,
                0f, 1f + 0.25f * intensityWeight, 0f, 0f, 0f,
                0f, 0f, 1f + 0.20f * intensityWeight, 0f, -15f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(dramWarmMat)
        }
        ApplePresetFilter.DRAMATIC_COOL -> {
            val dramCoolMat = ColorMatrix(floatArrayOf(
                1f + 0.20f * intensityWeight, 0f, 0f, 0f, -15f * intensityWeight,
                0f, 1f + 0.25f * intensityWeight, 0f, 0f, 0f,
                0f, 0f, 1f + 0.28f * intensityWeight, 0f, 12f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(dramCoolMat)
        }
        ApplePresetFilter.MONO -> {
            val monoMat = ColorMatrix().apply { setToSaturation(1f - intensityWeight) }
            matrix.timesAssign(monoMat)
        }
        ApplePresetFilter.SILVERTONE -> {
            val silverMat = ColorMatrix(floatArrayOf(
                0.33f * intensityWeight + (1f - intensityWeight), 0.33f * intensityWeight, 0.33f * intensityWeight, 0f, 18f * intensityWeight,
                0.33f * intensityWeight, 0.33f * intensityWeight + (1f - intensityWeight), 0.33f * intensityWeight, 0f, 18f * intensityWeight,
                0.33f * intensityWeight, 0.33f * intensityWeight, 0.33f * intensityWeight + (1f - intensityWeight), 0f, 22f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(silverMat)
        }
        ApplePresetFilter.NOIR -> {
            val noirMat = ColorMatrix(floatArrayOf(
                0.40f * intensityWeight + (1f - intensityWeight), 0.40f * intensityWeight, 0.40f * intensityWeight, 0f, -15f * intensityWeight,
                0.40f * intensityWeight, 0.40f * intensityWeight + (1f - intensityWeight), 0.40f * intensityWeight, 0f, -15f * intensityWeight,
                0.40f * intensityWeight, 0.40f * intensityWeight, 0.40f * intensityWeight + (1f - intensityWeight), 0f, -15f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(noirMat)
        }
        ApplePresetFilter.ORIGINAL -> {}
    }

    return matrix
}

private suspend fun exportAppleEditedImage(
    context: Context,
    sourceUri: Uri?,
    record: DownloadRecord,
    rotationDegrees: Float,
    isFlippedHorizontal: Boolean,
    isAutoEnhanced: Boolean,
    autoEnhanceAmount: Float,
    exposure: Float,
    brilliance: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    tint: Float,
    filter: ApplePresetFilter,
    filterIntensity: Float,
    downloadHelper: DownloadManagerHelper
): DownloadRecord {
    val inputStream = if (sourceUri != null) {
        context.contentResolver.openInputStream(sourceUri)
    } else {
        File(record.filePath).inputStream()
    } ?: throw IllegalStateException("Không thể đọc tệp ảnh nguồn")

    val originalBitmap = BitmapFactory.decodeStream(inputStream)
        ?: throw IllegalStateException("Không thể giải mã hình ảnh")

    val matrix = AndroidMatrix().apply {
        if (rotationDegrees != 0f) postRotate(rotationDegrees)
        if (isFlippedHorizontal) postScale(-1f, 1f)
    }

    val transformedBitmap = Bitmap.createBitmap(
        originalBitmap,
        0, 0,
        originalBitmap.width, originalBitmap.height,
        matrix,
        true
    )

    val colorMat = buildAppleColorMatrix(
        isAuto = isAutoEnhanced,
        autoVal = autoEnhanceAmount,
        exposure = exposure,
        brilliance = brilliance,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        tint = tint,
        filter = filter,
        filterIntensity = filterIntensity
    )
    val androidColorFilter = AndroidColorMatrixColorFilter(AndroidColorMatrix(colorMat.values))

    val resultBitmap = Bitmap.createBitmap(
        transformedBitmap.width,
        transformedBitmap.height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(resultBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = androidColorFilter
    }
    canvas.drawBitmap(transformedBitmap, 0f, 0f, paint)

    val picturesDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "TikTokDownloads"
    ).apply { if (!exists()) mkdirs() }

    val timestamp = System.currentTimeMillis()
    val outputFile = File(picturesDir, "IMG_EDIT_${timestamp}.jpg")

    FileOutputStream(outputFile).use { out ->
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }

    MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf("image/jpeg"), null)

    val newRecord = DownloadRecord(
        id = "edit_img_$timestamp",
        title = "Đã chỉnh sửa - ${record.title}",
        author = record.author,
        coverUrl = Uri.fromFile(outputFile).toString(),
        formatTitle = "Hình ảnh đã chỉnh sửa",
        fileExtension = "jpg",
        downloadId = timestamp,
        timestamp = timestamp,
        filePath = outputFile.absolutePath,
        originalUrl = record.originalUrl
    )
    downloadHelper.addHistoryRecord(newRecord)

    return newRecord
}
