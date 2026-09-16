package com.buwin.tiktokvideodownload.ui.components.editor.model

import android.net.Uri

/**
 * Defines available editing operations.
 * Operations can be combined in a pipeline (except EXTRACT_AUDIO which is standalone).
 */
enum class EditorOperation {
    TRIM,
    MUTE,
    REPLACE_AUDIO,
    EXTRACT_AUDIO,
    SPEED,
    ROTATE,
    ADJUST
}

/**
 * Category of tools shown in the bottom toolbar.
 * Each category reveals a different context panel.
 */
enum class EditorToolCategory {
    TRIM,
    AUDIO,
    SPEED,
    ROTATE,
    ADJUST
}

/**
 * Holds the complete editing pipeline state.
 * Operations are independent toggles rather than mutually exclusive tabs.
 */
data class EditorPipelineState(
    // Trim range
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = 0L,
    val totalDurationMs: Long = 0L,

    // Active operations - multiple can be true
    val isTrimEnabled: Boolean = false,
    val isMuteEnabled: Boolean = false,
    val isReplaceAudioEnabled: Boolean = false,
    val isExtractAudioOnly: Boolean = false,

    // Speed control
    val speedMultiplier: Float = 1.0f,
    val isSpeedChanged: Boolean = false,

    // Rotation
    val rotationDegrees: Float = 0f,
    val isRotated: Boolean = false,

    // Color adjustments
    val brightness: Float = 0f,    // -1.0 to 1.0
    val contrast: Float = 0f,      // -1.0 to 1.0
    val saturation: Float = 0f,    // -1.0 to 1.0
    val warmth: Float = 0f,        // -1.0 to 1.0 (Cold to Warm)
    val hue: Float = 0f,           // -180.0 to 180.0 degrees
    val blur: Float = 0f,          // 0.0 to 1.0
    val isColorAdjusted: Boolean = false,

    // Audio replacement
    val customAudioUri: Uri? = null,
    val customAudioTitle: String = "",

    // Export
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportError: String? = null,

    // Active tool category for context panel
    val activeCategory: EditorToolCategory? = null
) {
    /** Returns ordered list of operations that will be executed */
    val activeOperations: List<EditorOperation>
        get() = buildList {
            if (isExtractAudioOnly) {
                add(EditorOperation.EXTRACT_AUDIO)
                return@buildList
            }
            if (isTrimEnabled) add(EditorOperation.TRIM)
            if (isMuteEnabled) add(EditorOperation.MUTE)
            if (isReplaceAudioEnabled) add(EditorOperation.REPLACE_AUDIO)
            if (isSpeedChanged) add(EditorOperation.SPEED)
            if (isRotated) add(EditorOperation.ROTATE)
            if (isColorAdjusted) add(EditorOperation.ADJUST)
        }

    /** True when any operation is selected */
    val hasActiveOperation: Boolean
        get() = isTrimEnabled || isMuteEnabled || isReplaceAudioEnabled ||
                isExtractAudioOnly || isSpeedChanged || isRotated || isColorAdjusted

    /** Summary text for export label */
    val exportLabel: String
        get() {
            val ops = mutableListOf<String>()
            if (isTrimEnabled) ops.add("Cắt")
            if (isMuteEnabled) ops.add("Tắt tiếng")
            if (isReplaceAudioEnabled) ops.add("Ghép nhạc")
            if (isExtractAudioOnly) ops.add("Tách nhạc")
            if (isSpeedChanged) ops.add("Tốc độ ${speedMultiplier}x")
            if (isRotated) ops.add("Xoay ${rotationDegrees.toInt()}°")
            if (isColorAdjusted) ops.add("Hiệu chỉnh")
            if (ops.isEmpty()) return "Chưa chọn thao tác"
            return ops.joinToString(" + ")
        }
}
