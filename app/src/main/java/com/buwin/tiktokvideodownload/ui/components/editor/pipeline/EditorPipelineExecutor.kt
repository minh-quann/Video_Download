package com.buwin.tiktokvideodownload.ui.components.editor.pipeline

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.editor.VideoEditorEngine
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.editor.model.EditorOperation
import com.buwin.tiktokvideodownload.ui.components.editor.model.EditorPipelineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Executes a pipeline of editing operations sequentially.
 * Each step feeds its output as the next step's input, allowing combinations like Trim + Mute.
 */
object EditorPipelineExecutor {

    /**
     * Run the full export pipeline based on active operations.
     * Returns the exported file on success.
     */
    suspend fun execute(
        context: Context,
        inputUri: Uri,
        record: DownloadRecord,
        state: EditorPipelineState,
        downloadHelper: DownloadManagerHelper,
        onProgress: (Float) -> Unit
    ): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            val subDir = "TikTok_Downloader"
            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                subDir
            )
            if (!outputDir.exists()) outputDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val baseName = record.title.take(25)
                .replace(Regex("[^a-zA-Z0-9_\\-\\.]"), "_")
                .ifEmpty { "video" }

            val ops = state.activeOperations
            if (ops.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Chưa chọn thao tác nào"))
            }

            // Special case: extract audio only
            if (ops.size == 1 && ops.first() == EditorOperation.EXTRACT_AUDIO) {
                val file = File(outputDir, "${baseName}_audio_$timestamp.m4a")
                val result = VideoEditorEngine.extractAudio(
                    context, inputUri, file,
                    state.startTrimMs, state.endTrimMs
                ) { p -> onProgress(p) }
                return@withContext result.map {
                    ExportResult(
                        file = it,
                        title = "[Âm thanh] ${record.title}",
                        formatTitle = "Âm thanh M4A",
                        fileExtension = "m4a"
                    )
                }
            }

            // Pipeline: chain operations, each producing a temp file that feeds the next
            var currentInputUri = inputUri
            val tempFiles = mutableListOf<File>()
            val progressPerStep = 1f / ops.size
            val labelParts = mutableListOf<String>()

            for ((index, op) in ops.withIndex()) {
                val stepProgressBase = index * progressPerStep
                val isFinalStep = index == ops.lastIndex
                val outputFile = if (isFinalStep) {
                    val suffix = labelParts.joinToString("_").ifEmpty { "edited" }
                    File(outputDir, "${baseName}_${suffix}_$timestamp.mp4")
                } else {
                    File(context.cacheDir, "editor_step_${index}_$timestamp.mp4").also {
                        tempFiles.add(it)
                    }
                }

                val stepResult = when (op) {
                    EditorOperation.TRIM -> {
                        labelParts.add("trimmed")
                        VideoEditorEngine.trimVideo(
                            context, currentInputUri, outputFile,
                            state.startTrimMs, state.endTrimMs
                        ) { p -> onProgress(stepProgressBase + p * progressPerStep) }
                    }
                    EditorOperation.MUTE -> {
                        labelParts.add("muted")
                        // If trim was already applied, mute the full trimmed output
                        val start = if (labelParts.contains("trimmed")) 0L else state.startTrimMs
                        val end = if (labelParts.contains("trimmed")) Long.MAX_VALUE else state.endTrimMs
                        VideoEditorEngine.muteVideo(
                            context, currentInputUri, outputFile, start, end
                        ) { p -> onProgress(stepProgressBase + p * progressPerStep) }
                    }
                    EditorOperation.REPLACE_AUDIO -> {
                        labelParts.add("mux")
                        val audioUri = state.customAudioUri
                            ?: return@withContext Result.failure(
                                IllegalStateException("Vui lòng chọn tệp âm thanh cần ghép")
                            )
                        VideoEditorEngine.replaceAudio(
                            context, currentInputUri, audioUri, outputFile
                        ) { p -> onProgress(stepProgressBase + p * progressPerStep) }
                    }
                    EditorOperation.EXTRACT_AUDIO -> {
                        // Should not reach here in pipeline mode
                        Result.failure(IllegalStateException("Extract audio cannot be combined"))
                    }
                }

                stepResult.onFailure { err ->
                    // Cleanup temp files on failure
                    tempFiles.forEach { it.delete() }
                    return@withContext Result.failure(err)
                }

                // Use this step's output as the next step's input
                currentInputUri = Uri.fromFile(outputFile)
            }

            // Cleanup temp intermediate files
            tempFiles.forEach { it.delete() }

            // Final output file is the last pipeline output
            val finalFile = File(
                outputDir,
                "${baseName}_${labelParts.joinToString("_")}_$timestamp.mp4"
            )

            // Build display title
            val displayOps = ops.joinToString(" + ") { op ->
                when (op) {
                    EditorOperation.TRIM -> "Đã cắt"
                    EditorOperation.MUTE -> "Tắt tiếng"
                    EditorOperation.REPLACE_AUDIO -> "Ghép nhạc"
                    EditorOperation.EXTRACT_AUDIO -> "Âm thanh"
                }
            }

            // The final file was already written by the last step
            // We need to find it - it's the output of the final step
            val lastOutputName = "${baseName}_${labelParts.joinToString("_")}_$timestamp.mp4"
            val actualFinalFile = File(outputDir, lastOutputName)

            if (!actualFinalFile.exists()) {
                // The final step already wrote to the correct path
                return@withContext Result.failure(IllegalStateException("Export file not found"))
            }

            Result.success(
                ExportResult(
                    file = actualFinalFile,
                    title = "[$displayOps] ${record.title}",
                    formatTitle = "Video ${labelParts.joinToString(" + ").uppercase()}",
                    fileExtension = "mp4"
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * Create a DownloadRecord from the export result and save to history.
     */
    fun saveToHistory(
        context: Context,
        result: ExportResult,
        record: DownloadRecord,
        downloadHelper: DownloadManagerHelper,
        onSuccess: (DownloadRecord) -> Unit
    ) {
        MediaScannerConnection.scanFile(context, arrayOf(result.file.absolutePath), null, null)

        val timestamp = System.currentTimeMillis()
        val newRecord = DownloadRecord(
            id = "editor_$timestamp",
            title = result.title,
            author = record.author,
            coverUrl = record.coverUrl,
            formatTitle = result.formatTitle,
            fileExtension = result.fileExtension,
            downloadId = timestamp,
            timestamp = timestamp,
            filePath = result.file.absolutePath,
            originalUrl = record.originalUrl
        )
        downloadHelper.addHistoryRecord(newRecord)
        onSuccess(newRecord)
        Toast.makeText(context, "Xuất thành công: ${result.file.name}", Toast.LENGTH_LONG).show()
    }
}

/**
 * Result of a pipeline export.
 */
data class ExportResult(
    val file: File,
    val title: String,
    val formatTitle: String,
    val fileExtension: String
)
