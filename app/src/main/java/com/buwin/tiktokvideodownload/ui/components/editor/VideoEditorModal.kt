package com.buwin.tiktokvideodownload.ui.components.editor

import android.net.Uri
import android.widget.VideoView
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.editor.model.EditorPipelineState
import com.buwin.tiktokvideodownload.ui.components.editor.pipeline.EditorPipelineExecutor
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.AudioReplacementCard
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.EditorToolBar
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.EditorTopBar
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.EditorVideoPreview
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.ExportErrorDialog
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.ExportProgressDialog
import com.buwin.tiktokvideodownload.ui.components.editor.widgets.TrimRangeInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern full-screen Video Editor modal with pipeline-based editing.
 * Multiple tools can be activated simultaneously (e.g., Trim + Mute).
 * Restructured: UI composed from dedicated widget files in widgets/ package.
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

    // Video playback state
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isPrepared by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var isUserDraggingTimeline by remember { mutableStateOf(false) }

    // Pipeline state (multi-select operations)
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var startTrimMs by remember { mutableLongStateOf(0L) }
    var endTrimMs by remember { mutableLongStateOf(0L) }
    var isTrimEnabled by remember { mutableStateOf(false) }
    var isMuteEnabled by remember { mutableStateOf(false) }
    var isReplaceAudioEnabled by remember { mutableStateOf(false) }
    var isExtractAudioOnly by remember { mutableStateOf(false) }

    // Audio replacement
    var selectedCustomAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCustomAudioTitle by remember { mutableStateOf("") }
    var showAudioPickerSheet by remember { mutableStateOf(false) }

    // Export state
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportError by remember { mutableStateOf<String?>(null) }

    // Build pipeline state snapshot for executor
    val pipelineState = EditorPipelineState(
        startTrimMs = startTrimMs,
        endTrimMs = endTrimMs,
        totalDurationMs = totalDurationMs,
        isTrimEnabled = isTrimEnabled,
        isMuteEnabled = isMuteEnabled,
        isReplaceAudioEnabled = isReplaceAudioEnabled,
        isExtractAudioOnly = isExtractAudioOnly,
        customAudioUri = selectedCustomAudioUri,
        customAudioTitle = selectedCustomAudioTitle
    )

    // Suppress back gesture during editing
    BackHandler(enabled = true) { }

    // Auto-loop playback within trim range
    LaunchedEffect(isPrepared, isPlaying, isUserDraggingTimeline, startTrimMs, endTrimMs) {
        while (isPrepared && isPlaying && !isUserDraggingTimeline) {
            videoViewRef?.let { vv ->
                try {
                    val pos = vv.currentPosition.toLong()
                    currentPositionMs = pos
                    if (pos >= endTrimMs && endTrimMs > startTrimMs) {
                        vv.seekTo(startTrimMs.toInt())
                        vv.start()
                    }
                } catch (_: Exception) {}
            }
            delay(150)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { videoViewRef?.stopPlayback() } catch (_: Exception) {}
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
                    ) { newRecord ->
                        onExportSuccess(newRecord)
                    }
                    onDismiss()
                },
                onFailure = { err ->
                    isExporting = false
                    exportError = err.localizedMessage ?: "Có lỗi xảy ra trong quá trình xử lý"
                }
            )
        }
    }

    // ── Main Layout ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top Bar
            EditorTopBar(
                exportLabel = if (pipelineState.hasActiveOperation) "Lưu & Xuất" else "Chọn thao tác",
                isExporting = isExporting,
                onClose = onDismiss,
                onExport = { executeExport() }
            )

            // 2. Video Preview
            EditorVideoPreview(
                videoUri = videoUri,
                isPlaying = isPlaying,
                onVideoViewReady = { vv -> videoViewRef = vv },
                onPrepared = { durationMs ->
                    isPrepared = true
                    totalDurationMs = durationMs
                    startTrimMs = 0L
                    endTrimMs = durationMs
                    isPlaying = true
                },
                onTogglePlayPause = {
                    videoViewRef?.let { vv ->
                        if (isPlaying) {
                            vv.pause()
                            isPlaying = false
                        } else {
                            vv.start()
                            isPlaying = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            // 3. Time Range Info (only when trim is active or as general info)
            TrimRangeInfo(startTrimMs = startTrimMs, endTrimMs = endTrimMs)

            // 4. Timeline Trimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemGestureExclusion()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                        videoViewRef?.seekTo(seekMs.toInt())
                    },
                    onDragStateChange = { isDragging ->
                        isUserDraggingTimeline = isDragging
                        if (isDragging) {
                            videoViewRef?.pause()
                        } else {
                            videoViewRef?.seekTo(startTrimMs.toInt())
                            videoViewRef?.start()
                            isPlaying = true
                        }
                    }
                )
            }

            // 5. Audio Replacement Card (visible when Replace Audio is active)
            if (isReplaceAudioEnabled) {
                AudioReplacementCard(
                    selectedAudioUri = selectedCustomAudioUri,
                    selectedAudioTitle = selectedCustomAudioTitle,
                    onPickAudio = { showAudioPickerSheet = true }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 6. Multi-Select Tool Bar
            EditorToolBar(
                isTrimEnabled = isTrimEnabled,
                isMuteEnabled = isMuteEnabled,
                isReplaceAudioEnabled = isReplaceAudioEnabled,
                isExtractAudioOnly = isExtractAudioOnly,
                onToggleTrim = { isTrimEnabled = !isTrimEnabled },
                onToggleMute = { isMuteEnabled = !isMuteEnabled },
                onToggleReplaceAudio = {
                    isReplaceAudioEnabled = !isReplaceAudioEnabled
                    if (isReplaceAudioEnabled && selectedCustomAudioUri == null) {
                        showAudioPickerSheet = true
                    }
                },
                onToggleExtractAudio = { isExtractAudioOnly = !isExtractAudioOnly }
            )
        }

        // ── Audio Picker Bottom Sheet ──
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

        // ── Export Progress Dialog ──
        if (isExporting) {
            ExportProgressDialog(
                progressLabel = "Đang xử lý: ${pipelineState.exportLabel}...",
                progress = exportProgress
            )
        }

        // ── Export Error Dialog ──
        if (exportError != null) {
            ExportErrorDialog(
                error = exportError ?: "",
                onDismiss = { exportError = null }
            )
        }
    }
}
