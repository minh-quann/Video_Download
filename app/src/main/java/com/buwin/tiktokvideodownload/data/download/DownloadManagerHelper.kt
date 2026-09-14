package com.buwin.tiktokvideodownload.data.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import com.buwin.tiktokvideodownload.data.notification.NotificationHelper
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles file downloading using Android's system DownloadManager, persists download history,
 * tracks live download progress, and broadcasts updates to both in-app WaterdropToast and
 * system Local Notifications.
 */
class DownloadManagerHelper(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("tiktok_downloads_prefs", Context.MODE_PRIVATE)
    private val notificationHelper = NotificationHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cancelledDownloadIds = java.util.Collections.synchronizedSet(mutableSetOf<Long>())

    /**
     * Enqueues a download with system DownloadManager and records it to history.
     * Launches progress monitoring for both in-app Toast and system Local Notifications.
     */
    fun enqueueDownload(videoInfo: TikTokVideoInfo, option: DownloadOption): Long {
        if (option.downloadUrl.isBlank() || (!option.downloadUrl.startsWith("http://") && !option.downloadUrl.startsWith("https://"))) {
            AppToast.showError("Lỗi tải xuống", "Đường dẫn tải về không hợp lệ")
            return -1L
        }

        val isFacebook = videoInfo.originalUrl.contains("facebook.com") ||
                videoInfo.originalUrl.contains("fb.watch") ||
                videoInfo.originalUrl.contains("fb.com")
        val defaultPrefix = if (isFacebook) "Facebook" else "TikTok"
        val subDir = if (isFacebook) "FacebookDownloads" else "TikTokDownloads"

        val sanitizedTitle = videoInfo.title
            .take(30)
            .replace("[^a-zA-Z0-9_ -]".toRegex(), "")
            .trim()
            .ifEmpty { "${defaultPrefix}_${videoInfo.id}" }

        val fileName = "${sanitizedTitle}_${option.type.name.lowercase()}_${System.currentTimeMillis()}.${option.fileExtension}"

        val request = DownloadManager.Request(Uri.parse(option.downloadUrl)).apply {
            setTitle(videoInfo.title.ifEmpty { "$defaultPrefix Video" })
            setDescription("Đang tải ${option.title}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "$subDir/$fileName")
            setMimeType(option.mimeType)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadId = downloadManager.enqueue(request)
        cancelledDownloadIds.remove(downloadId)

        val filePath = java.io.File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$subDir/$fileName"
        ).absolutePath

        val record = DownloadRecord(
            id = videoInfo.id,
            title = videoInfo.title.ifEmpty { "Video ${videoInfo.id}" },
            author = if (videoInfo.authorUsername.isNotEmpty()) "@${videoInfo.authorUsername}" else defaultPrefix,
            coverUrl = videoInfo.coverUrl,
            formatTitle = option.title,
            fileExtension = option.fileExtension,
            downloadId = downloadId,
            filePath = filePath
        )
        saveRecord(record)

        // Show initial feedback in Waterdrop Toast and Local Notification
        val isAudio = option.type == DownloadFormatType.AUDIO_MP3
        AppToast.showProgress(
            title = option.title.ifEmpty { if (isAudio) "Bắt đầu tải âm thanh..." else "Bắt đầu tải video..." },
            message = "Đang kết nối...",
            progress = 0,
            downloadedBytes = 0L,
            totalBytes = 0L,
            speedBytesPerSec = 0L,
            downloadId = downloadId
        )
        notificationHelper.showProgress(
            notificationId = downloadId.toInt(),
            title = videoInfo.title.ifEmpty { "$defaultPrefix Video" },
            contentText = "${option.title} • Đang chuẩn bị tải...",
            progress = 0
        )

        // Launch live progress tracker
        scope.launch {
            monitorDownloadProgress(downloadId, videoInfo, option, defaultPrefix)
        }

        return downloadId
    }

    /**
     * Cancels an active download, dismisses system notification, removes partial files,
     * and deletes the uncompleted record from history.
     */
    fun cancelDownload(downloadId: Long) {
        if (downloadId <= 0) return
        cancelledDownloadIds.add(downloadId)

        // 1. Remove from Android DownloadManager (halts download and cleans temp file)
        try {
            downloadManager.remove(downloadId)
        } catch (_: Exception) {
        }

        // 2. Dismiss system status bar notification
        notificationHelper.cancel(downloadId.toInt())

        // 3. Remove uncompleted record from history and delete partial file if present
        try {
            val records = getHistory().toMutableList()
            val target = records.firstOrNull { it.downloadId == downloadId }
            if (target != null) {
                records.removeAll { it.downloadId == downloadId }
                val jsonArray = JSONArray()
                records.take(50).forEach { item ->
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("author", item.author)
                        put("coverUrl", item.coverUrl)
                        put("formatTitle", item.formatTitle)
                        put("fileExtension", item.fileExtension)
                        put("downloadId", item.downloadId)
                        put("timestamp", item.timestamp)
                        put("filePath", item.filePath)
                    }
                    jsonArray.put(obj)
                }
                prefs.edit().putString("history_json", jsonArray.toString()).apply()

                if (target.filePath.isNotEmpty()) {
                    val f = java.io.File(target.filePath)
                    if (f.exists()) {
                        f.delete()
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Periodically queries DownloadManager to update Toast and Local Notification progress.
     */
    private suspend fun monitorDownloadProgress(
        downloadId: Long,
        videoInfo: TikTokVideoInfo,
        option: DownloadOption,
        platformName: String
    ) {
        var isPolling = true
        val displayTitle = videoInfo.title.ifEmpty { "$platformName Video" }
        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()
        var currentSpeed = 0L

        while (isPolling) {
            if (cancelledDownloadIds.contains(downloadId)) {
                isPolling = false
                cancelledDownloadIds.remove(downloadId)
                return
            }
            delay(350)
            if (cancelledDownloadIds.contains(downloadId)) {
                isPolling = false
                cancelledDownloadIds.remove(downloadId)
                return
            }
            try {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cancelledDownloadIds.contains(downloadId)) {
                        isPolling = false
                        cancelledDownloadIds.remove(downloadId)
                        return
                    }
                    if (cursor.moveToFirst()) {
                        if (cancelledDownloadIds.contains(downloadId)) {
                            isPolling = false
                            cancelledDownloadIds.remove(downloadId)
                            return
                        }
                        val bytesDownloaded = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        )
                        val totalBytes = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        )
                        val status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                        )

                        // Calculate network transfer speed with smoothing
                        val now = System.currentTimeMillis()
                        val timeDeltaMs = now - lastTime
                        if (timeDeltaMs >= 450) {
                            val bytesDelta = (bytesDownloaded - lastBytes).coerceAtLeast(0L)
                            val instantSpeed = (bytesDelta * 1000L) / timeDeltaMs
                            currentSpeed = if (currentSpeed == 0L) instantSpeed else ((currentSpeed * 0.4) + (instantSpeed * 0.6)).toLong()
                            lastBytes = bytesDownloaded
                            lastTime = now
                        }

                        val progress = if (totalBytes > 0) {
                            ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }

                        when (status) {
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                if (cancelledDownloadIds.contains(downloadId)) {
                                    isPolling = false
                                    cancelledDownloadIds.remove(downloadId)
                                    return
                                }
                                val progressLabel = if (progress >= 0) "$progress%" else "Đang tải..."
                                val sizeText = if (totalBytes > 0) {
                                    "${AppToast.formatBytes(bytesDownloaded)} / ${AppToast.formatBytes(totalBytes)}"
                                } else {
                                    AppToast.formatBytes(bytesDownloaded)
                                }
                                val speedText = if (currentSpeed > 0) " • ${AppToast.formatSpeed(currentSpeed)}" else ""

                                val isAudioOption = option.type == DownloadFormatType.AUDIO_MP3
                                val downloadingTitle = option.title.ifEmpty { if (isAudioOption) "Đang tải âm thanh..." else "Đang tải video..." }

                                AppToast.showProgress(
                                    title = downloadingTitle,
                                    message = "$sizeText ($progressLabel)",
                                    progress = progress,
                                    downloadedBytes = bytesDownloaded,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = currentSpeed,
                                    downloadId = downloadId
                                )
                                notificationHelper.showProgress(
                                    notificationId = downloadId.toInt(),
                                    title = displayTitle,
                                    contentText = "$sizeText ($progressLabel)$speedText",
                                    progress = progress
                                )
                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                isPolling = false
                                if (cancelledDownloadIds.contains(downloadId)) {
                                    cancelledDownloadIds.remove(downloadId)
                                    return
                                }
                                val isAudioOption = option.type == DownloadFormatType.AUDIO_MP3
                                val finalSize = if (totalBytes > 0) AppToast.formatBytes(totalBytes) else if (bytesDownloaded > 0) AppToast.formatBytes(bytesDownloaded) else ""
                                val sizeInfo = if (finalSize.isNotEmpty() && finalSize != "0 B") "$finalSize • " else ""
                                val actionLabel = if (isAudioOption) "nghe âm thanh" else "xem video"
                                AppToast.showSuccess(
                                    title = "Tải hoàn tất!",
                                    message = "${sizeInfo}Chạm để $actionLabel",
                                    downloadId = downloadId
                                )
                                notificationHelper.showCompleted(
                                    notificationId = downloadId.toInt(),
                                    title = "Tải hoàn tất!",
                                    contentText = "$displayTitle • ${sizeInfo}Đã lưu vào máy"
                                )
                            }
                            DownloadManager.STATUS_FAILED -> {
                                isPolling = false
                                if (cancelledDownloadIds.contains(downloadId)) {
                                    cancelledDownloadIds.remove(downloadId)
                                    return
                                }
                                val isAudioOption = option.type == DownloadFormatType.AUDIO_MP3
                                val errorMsg = if (isAudioOption) "Không thể tải âm thanh. Vui lòng kiểm tra mạng và thử lại." else "Không thể tải video. Vui lòng kiểm tra mạng và thử lại."
                                AppToast.showError(
                                    title = "Tải thất bại",
                                    message = errorMsg
                                )
                                notificationHelper.showError(
                                    notificationId = downloadId.toInt(),
                                    title = "Tải thất bại",
                                    contentText = "Không thể tải $displayTitle"
                                )
                            }
                        }
                    } else {
                        isPolling = false
                    }
                } ?: run {
                    isPolling = false
                }
            } catch (_: Exception) {
                isPolling = false
            }
        }
        cancelledDownloadIds.remove(downloadId)
    }

    /**
     * Saves a record to SharedPreferences.
     */
    private fun saveRecord(record: DownloadRecord) {
        val records = getHistory().toMutableList()
        records.add(0, record)
        val jsonArray = JSONArray()
        records.take(50).forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("author", item.author)
                put("coverUrl", item.coverUrl)
                put("formatTitle", item.formatTitle)
                put("fileExtension", item.fileExtension)
                put("downloadId", item.downloadId)
                put("timestamp", item.timestamp)
                put("filePath", item.filePath)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("history_json", jsonArray.toString()).apply()
    }

    /**
     * Retrieves download history.
     */
    fun getHistory(): List<DownloadRecord> {
        val jsonString = prefs.getString("history_json", null) ?: return emptyList()
        val result = mutableListOf<DownloadRecord>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    DownloadRecord(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        author = obj.optString("author"),
                        coverUrl = obj.optString("coverUrl"),
                        formatTitle = obj.optString("formatTitle"),
                        fileExtension = obj.optString("fileExtension"),
                        downloadId = obj.optLong("downloadId"),
                        timestamp = obj.optLong("timestamp"),
                        filePath = obj.optString("filePath")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    /**
     * Obtains a playable content/file Uri for the downloaded record.
     */
    fun getDownloadedUri(record: DownloadRecord): Uri? {
        // 1. Try DownloadManager content Uri (preferred for Android Scoped Storage)
        try {
            if (record.downloadId > 0) {
                val dmUri = downloadManager.getUriForDownloadedFile(record.downloadId)
                if (dmUri != null) return dmUri
            }
        } catch (_: Exception) {
        }

        // 2. Try recorded absolute file path
        if (record.filePath.isNotEmpty()) {
            try {
                val file = java.io.File(record.filePath)
                if (file.exists() && file.length() > 0) {
                    return Uri.fromFile(file)
                }
            } catch (_: Exception) {
            }
        }

        // 3. Fallback: Search in TikTokDownloads or FacebookDownloads directory
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subFolders = listOf("TikTokDownloads", "FacebookDownloads", "")
            for (sub in subFolders) {
                val dir = if (sub.isEmpty()) downloadDir else java.io.File(downloadDir, sub)
                if (dir.exists() && dir.isDirectory) {
                    val match = dir.listFiles()?.firstOrNull { f ->
                        f.isFile && f.length() > 0 && (
                            f.name.contains(record.id) ||
                            (record.title.isNotEmpty() && f.name.contains(record.title.take(12)))
                        )
                    }
                    if (match != null) {
                        return Uri.fromFile(match)
                    }
                }
            }
        } catch (_: Exception) {
        }

        return null
    }

    /**
     * Clears all download history records.
     */
    fun clearHistory() {
        prefs.edit().remove("history_json").apply()
    }

    /**
     * Opens system Downloads folder or view intent.
     */
    fun openDownloadsFolder() {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
