package com.buwin.tiktokvideodownload.ui.components.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.editor.model.ImageAdjustTool
import com.buwin.tiktokvideodownload.ui.components.editor.model.ImageEditorTab
import com.buwin.tiktokvideodownload.ui.components.editor.model.ImagePresetFilter
import com.buwin.tiktokvideodownload.ui.components.editor.pipeline.buildImageColorMatrix
import com.buwin.tiktokvideodownload.ui.components.editor.pipeline.exportEditedImage
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.ImageEditorBottomBar
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.ImageEditorControls
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.ImageEditorTopBar
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Image Editor Modal with Liquid Glass.
 * Orchestrates:
 * - Content backdrop layer & SubcomposeAsyncImage preview
 * - TopBar: Cancel, Undo/Redo, Done
 * - Controls: Adjust, Filters, Crop animated panels
 * - BottomBar: Floating Liquid Glass capsule tab bar
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
    val contentBackdrop = rememberLayerBackdrop()
    val imageUri = remember(record) { downloadHelper.getDownloadedUri(record) }

    // ── Main State ──
    var currentTab by remember { mutableStateOf(ImageEditorTab.ADJUST) }
    var currentAdjustTool by remember { mutableStateOf(ImageAdjustTool.AUTO) }

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
    var selectedFilter by remember { mutableStateOf(ImagePresetFilter.ORIGINAL) }
    var filterIntensity by remember { mutableFloatStateOf(100f) } // 0..100%

    // Crop / Orientation state
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var straightenAngle by remember { mutableFloatStateOf(0f) } // -45..+45 degrees

    var isSaving by remember { mutableStateOf(false) }

    val hasChanges = isAutoEnhanced || exposureValue != 0f || brillianceValue != 0f ||
            contrastValue != 0f || saturationValue != 0f || warmthValue != 0f || tintValue != 0f ||
            selectedFilter != ImagePresetFilter.ORIGINAL || rotationDegrees != 0f ||
            isFlippedHorizontal || straightenAngle != 0f

    // Compute live Compose ColorMatrix
    val composeColorMatrix = remember(
        isAutoEnhanced, autoEnhanceAmount, exposureValue, brillianceValue,
        contrastValue, saturationValue, warmthValue, tintValue, selectedFilter, filterIntensity
    ) {
        buildImageColorMatrix(
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
                    exportEditedImage(
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

        // ── 1. TOP BAR (Cancel | Undo/Redo | Done) ──
        ImageEditorTopBar(
            backdrop = contentBackdrop,
            hasChanges = hasChanges,
            isSaving = isSaving,
            onDismiss = onDismiss,
            onUndo = {
                when (currentAdjustTool) {
                    ImageAdjustTool.AUTO -> {
                        isAutoEnhanced = false
                        autoEnhanceAmount = 0f
                    }
                    ImageAdjustTool.EXPOSURE -> exposureValue = 0f
                    ImageAdjustTool.BRILLIANCE -> brillianceValue = 0f
                    ImageAdjustTool.CONTRAST -> contrastValue = 0f
                    ImageAdjustTool.SATURATION -> saturationValue = 0f
                    ImageAdjustTool.WARMTH -> warmthValue = 0f
                    ImageAdjustTool.TINT -> tintValue = 0f
                }
            },
            onSave = { executeSave() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // ── 2. BOTTOM CONTROLS & TAB BAR ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ImageEditorControls(
                currentTab = currentTab,
                currentAdjustTool = currentAdjustTool,
                isAutoEnhanced = isAutoEnhanced,
                autoEnhanceAmount = autoEnhanceAmount,
                exposureValue = exposureValue,
                brillianceValue = brillianceValue,
                contrastValue = contrastValue,
                saturationValue = saturationValue,
                warmthValue = warmthValue,
                tintValue = tintValue,
                selectedFilter = selectedFilter,
                filterIntensity = filterIntensity,
                rotationDegrees = rotationDegrees,
                isFlippedHorizontal = isFlippedHorizontal,
                straightenAngle = straightenAngle,
                backdrop = contentBackdrop,
                onAdjustToolSelected = { tool ->
                    currentAdjustTool = tool
                    if (tool == ImageAdjustTool.AUTO && !isAutoEnhanced) {
                        isAutoEnhanced = true
                        autoEnhanceAmount = 50f
                    }
                },
                onAdjustValueChange = { tool, value ->
                    when (tool) {
                        ImageAdjustTool.AUTO -> {
                            isAutoEnhanced = value != 0f
                            autoEnhanceAmount = value
                        }
                        ImageAdjustTool.EXPOSURE -> exposureValue = value
                        ImageAdjustTool.BRILLIANCE -> brillianceValue = value
                        ImageAdjustTool.CONTRAST -> contrastValue = value
                        ImageAdjustTool.SATURATION -> saturationValue = value
                        ImageAdjustTool.WARMTH -> warmthValue = value
                        ImageAdjustTool.TINT -> tintValue = value
                    }
                },
                onFilterSelected = { filter ->
                    selectedFilter = filter
                },
                onFilterIntensityChange = { intensity ->
                    filterIntensity = intensity
                },
                onRotate90 = {
                    rotationDegrees = (rotationDegrees + 90f) % 360f
                },
                onFlipHorizontal = {
                    isFlippedHorizontal = !isFlippedHorizontal
                },
                onStraightenAngleChange = { angle ->
                    straightenAngle = angle
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Floating Liquid Glass Capsule Tab Bar
            ImageEditorBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                },
                backdrop = contentBackdrop
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
