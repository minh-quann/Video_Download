package com.buwin.tiktokvideodownload.ui.components.editor.model

import android.net.Uri

/**
 * Defines available editing operations.
 * Multiple operations can be active simultaneously in a pipeline fashion.
 */
enum class EditorOperation {
    TRIM,
    MUTE,
    REPLACE_AUDIO,
    EXTRACT_AUDIO
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

    // Audio replacement
    val customAudioUri: Uri? = null,
    val customAudioTitle: String = "",

    // Export
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportError: String? = null
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
        }

    /** True when any operation is selected */
    val hasActiveOperation: Boolean
        get() = isTrimEnabled || isMuteEnabled || isReplaceAudioEnabled || isExtractAudioOnly

    /** Summary text for export label */
    val exportLabel: String
        get() {
            val ops = activeOperations
            if (ops.isEmpty()) return "Chưa chọn thao tác"
            return ops.joinToString(" + ") { op ->
                when (op) {
                    EditorOperation.TRIM -> "Cắt"
                    EditorOperation.MUTE -> "Tắt tiếng"
                    EditorOperation.REPLACE_AUDIO -> "Ghép nhạc"
                    EditorOperation.EXTRACT_AUDIO -> "Tách nhạc"
                }
            }
        }
}
