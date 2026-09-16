package com.buwin.tiktokvideodownload.ui.components.editor

import android.net.Uri
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.editor.model.EditorPipelineState
import com.buwin.tiktokvideodownload.ui.components.editor.model.EditorToolCategory
import com.buwin.tiktokvideodownload.ui.components.editor.pipeline.EditorPipelineExecutor
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.AudioReplacementCard
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.EditorPlayerController
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.EditorToolBar
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.EditorTopBar
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.EditorVideoPreview
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.ExportErrorDialog
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.ExportProgressDialog
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.TrimRangeInfo
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen Video Editor with Liquid Glass.
 *
 * Architecture (same as Kyant0/AndroidLiquidGlass docs):
 *   Box {
 *     Column(.layerBackdrop)  ← SOURCE: video + timeline (captured for refraction)
 *     Column (overlay)        ← GLASS: TopBar + ToolBar with drawBackdrop (OUTSIDE layerBackdrop!)
 *   }
 *
 * drawBackdrop elements MUST be siblings (outside) of layerBackdrop, not children.
 * Otherwise circular render node reference → RenderThread stack overflow.
 */
@Composable
fun VideoEditorModal(
    record: DownloadRecord,
    downloadHelper: DownloadManagerHelper,
    onDismiss: () -> Unit,
    onExportSuccess: (DownloadRecord) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val videoUri = remember(record) { downloadHelper.getDownloadedUri(record) }

    // Backdrop source = video + timeline content
    val editorBackdrop = rememberLayerBackdrop()

    // ── Playback State ──
    var playerController by remember { mutableStateOf<EditorPlayerController?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isPrepared by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var isUserDraggingTimeline by remember { mutableStateOf(false) }

    // ── Timeline / Trim ──
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var startTrimMs by remember { mutableLongStateOf(0L) }
    var endTrimMs by remember { mutableLongStateOf(0L) }

    // ── Pipeline Operations ──
    var isTrimEnabled by remember { mutableStateOf(false) }
    var isMuteEnabled by remember { mutableStateOf(false) }
    var isReplaceAudioEnabled by remember { mutableStateOf(false) }
    var isExtractAudioOnly by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }

    // ── Audio Replacement ──
    var selectedCustomAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCustomAudioTitle by remember { mutableStateOf("") }
    var showAudioPickerSheet by remember { mutableStateOf(false) }

    // ── Export ──
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportError by remember { mutableStateOf<String?>(null) }

    // ── Active Tool Category ──
    var activeCategory by remember { mutableStateOf<EditorToolCategory?>(null) }

    val pipelineState = EditorPipelineState(
        startTrimMs = startTrimMs,
        endTrimMs = endTrimMs,
        totalDurationMs = totalDurationMs,
        isTrimEnabled = isTrimEnabled,
        isMuteEnabled = isMuteEnabled,
        isReplaceAudioEnabled = isReplaceAudioEnabled,
        isExtractAudioOnly = isExtractAudioOnly,
        speedMultiplier = speedMultiplier,
        isSpeedChanged = speedMultiplier != 1.0f,
        rotationDegrees = rotationDegrees,
        isRotated = rotationDegrees != 0f,
        brightness = brightness,
        contrast = contrast,
        saturation = saturation,
        isColorAdjusted = brightness != 0f || contrast != 0f || saturation != 0f,
        customAudioUri = selectedCustomAudioUri,
        customAudioTitle = selectedCustomAudioTitle,
        activeCategory = activeCategory
    )

    BackHandler(enabled = true) { }

    // Auto-loop within trim range
    LaunchedEffect(isPrepared, isPlaying, isUserDraggingTimeline, startTrimMs, endTrimMs) {
        while (isPrepared && isPlaying && !isUserDraggingTimeline) {
            playerController?.let { pc ->
                try {
                    val pos = pc.currentPosition
                    currentPositionMs = pos
                    if (pos >= endTrimMs && endTrimMs > startTrimMs) {
                        pc.seekTo(startTrimMs.toInt())
                        pc.start()
                    }
                } catch (_: Exception) {}
            }
            delay(150)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { playerController?.stopPlayback() } catch (_: Exception) {}
        }
    }

    fun executeExport() {
        if (videoUri == null || !pipelineState.hasActiveOperation) return
        isExporting = true
        exportProgress = 0f
        exportError = null

        coroutineScope.launch {
            val result = EditorPipelineExecutor.execute(
                context = context,
                inputUri = videoUri,
                record = record,
                state = pipelineState,
                downloadHelper = downloadHelper,
                onProgress = { p -> exportProgress = p }
            )
            result.fold(
                onSuccess = { exportResult ->
                    isExporting = false
                    EditorPipelineExecutor.saveToHistory(
                        context, exportResult, record, downloadHelper
                    ) { newRecord -> onExportSuccess(newRecord) }
                    onDismiss()
                },
                onFailure = { err ->
                    isExporting = false
                    exportError = err.localizedMessage ?: "Có lỗi xảy ra trong quá trình xử lý"
                }
            )
        }
    }

    // ── Layout ──
    // Same pattern as Kyant0 library + MainActivity:
    // layerBackdrop = captured source content
    // drawBackdrop elements = overlay siblings OUTSIDE layerBackdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ═══ LAYER 1: SOURCE (captured into editorBackdrop for glass refraction) ═══
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(editorBackdrop)
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top spacer for TopBar overlay
            Spacer(modifier = Modifier.height(58.dp))

            // ── Video Preview ──
            EditorVideoPreview(
                videoUri = videoUri,
                isPlaying = isPlaying,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                rotationDegrees = rotationDegrees,
                playbackSpeed = speedMultiplier,
                onPlayerReady = { pc -> playerController = pc },
                onPrepared = { durationMs ->
                    isPrepared = true
                    totalDurationMs = durationMs
                    startTrimMs = 0L
                    endTrimMs = durationMs
                    isPlaying = true
                },
                onTogglePlayPause = {
                    playerController?.let { pc ->
                        if (isPlaying) { pc.pause(); isPlaying = false }
                        else { pc.start(); isPlaying = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            // ── Active Operations Summary ──
            if (pipelineState.hasActiveOperation) {
                ActiveOpsSummaryBar(label = pipelineState.exportLabel)
            }

            // ── Time Range Info ──
            TrimRangeInfo(startTrimMs = startTrimMs, endTrimMs = endTrimMs)

            // ── Timeline Trimmer ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemGestureExclusion()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                VideoTimelineTrimmer(
                    totalDurationMs = totalDurationMs,
                    startMs = startTrimMs,
                    endMs = endTrimMs,
                    currentPositionMs = currentPositionMs,
                    onRangeChange = { newStart, newEnd ->
                        startTrimMs = newStart
                        endTrimMs = newEnd
                    },
                    onSeek = { seekMs ->
                        currentPositionMs = seekMs
                        playerController?.seekTo(seekMs.toInt())
                    },
                    onDragStateChange = { isDragging ->
                        isUserDraggingTimeline = isDragging
                        if (isDragging) {
                            playerController?.pause()
                        } else {
                            playerController?.seekTo(startTrimMs.toInt())
                            playerController?.start()
                            isPlaying = true
                        }
                    }
                )
            }

            // ── Audio Replacement Card ──
            if (isReplaceAudioEnabled) {
                AudioReplacementCard(
                    selectedAudioUri = selectedCustomAudioUri,
                    selectedAudioTitle = selectedCustomAudioTitle,
                    onPickAudio = { showAudioPickerSheet = true }
                )
            }

            // Bottom spacer for ToolBar overlay
            Spacer(modifier = Modifier.height(120.dp))
        }

        // ═══ LAYER 2: GLASS OVERLAY (drawBackdrop – OUTSIDE layerBackdrop!) ═══
        // Top bar pinned to top
        EditorTopBar(
            exportLabel = if (pipelineState.hasActiveOperation) "Xuất" else "Chọn thao tác",
            isExporting = isExporting,
            backdrop = editorBackdrop,
            onClose = onDismiss,
            onExport = { executeExport() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // Bottom tool bar pinned to bottom
        EditorToolBar(
            isTrimEnabled = isTrimEnabled,
            isMuteEnabled = isMuteEnabled,
            isReplaceAudioEnabled = isReplaceAudioEnabled,
            isExtractAudioOnly = isExtractAudioOnly,
            speedMultiplier = speedMultiplier,
            rotationDegrees = rotationDegrees,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            activeCategory = activeCategory,
            backdrop = editorBackdrop,
            onToggleTrim = {
                if (!isExtractAudioOnly) isTrimEnabled = !isTrimEnabled
            },
            onToggleMute = {
                if (!isReplaceAudioEnabled && !isExtractAudioOnly) {
                    isMuteEnabled = !isMuteEnabled
                }
            },
            onToggleReplaceAudio = {
                if (!isMuteEnabled && !isExtractAudioOnly) {
                    isReplaceAudioEnabled = !isReplaceAudioEnabled
                    if (isReplaceAudioEnabled && selectedCustomAudioUri == null) {
                        showAudioPickerSheet = true
                    }
                }
            },
            onToggleExtractAudio = {
                if (!isTrimEnabled && !isMuteEnabled && !isReplaceAudioEnabled) {
                    isExtractAudioOnly = !isExtractAudioOnly
                }
            },
            onSelectCategory = { cat -> activeCategory = cat },
            onSpeedChange = { speed -> speedMultiplier = speed },
            onRotate = { rotationDegrees = (rotationDegrees + 90f) % 360f },
            onBrightnessChange = { brightness = it },
            onContrastChange = { contrast = it },
            onSaturationChange = { saturation = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        // ── Audio Picker ──
        if (showAudioPickerSheet) {
            AudioPickerModal(
                downloadHelper = downloadHelper,
                selectedAudioUri = selectedCustomAudioUri,
                onSelectAudio = { uri, title ->
                    selectedCustomAudioUri = uri
                    selectedCustomAudioTitle = title
                },
                onDismiss = { showAudioPickerSheet = false }
            )
        }

        // ── Export Progress ──
        if (isExporting) {
            ExportProgressDialog(
                progressLabel = "Đang xử lý: ${pipelineState.exportLabel}...",
                progress = exportProgress
            )
        }

        // ── Export Error ──
        if (exportError != null) {
            ExportErrorDialog(
                error = exportError ?: "",
                onDismiss = { exportError = null }
            )
        }
    }
}

/**
 * Compact summary bar showing active operations.
 */
@Composable
private fun ActiveOpsSummaryBar(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF007AFF).copy(alpha = 0.15f),
                            Color(0xFF5856D6).copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "⚡ $label",
                color = Color(0xFF007AFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
