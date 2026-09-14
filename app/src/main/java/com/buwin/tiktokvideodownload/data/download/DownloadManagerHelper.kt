package com.buwin.tiktokvideodownload.data.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
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

    /**
     * Enqueues a download with system DownloadManager and records it to history.
     * Launches progress monitoring for both in-app Toast and system Local Notifications.
     */
    fun enqueueDownload(videoInfo: TikTokVideoInfo, option: DownloadOption): Long {
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

        val record = DownloadRecord(
            id = videoInfo.id,
            title = videoInfo.title.ifEmpty { "Video ${videoInfo.id}" },
            author = if (videoInfo.authorUsername.isNotEmpty()) "@${videoInfo.authorUsername}" else defaultPrefix,
            coverUrl = videoInfo.coverUrl,
            formatTitle = option.title,
            fileExtension = option.fileExtension,
            downloadId = downloadId
        )
        saveRecord(record)

        // Show initial feedback in Waterdrop Toast and Local Notification
        AppToast.showProgress(
            title = option.title.ifEmpty { "Bắt đầu tải video..." },
            message = "Đang kết nối...",
            progress = 0,
            downloadedBytes = 0L,
            totalBytes = 0L,
            speedBytesPerSec = 0L
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
            delay(350)
            try {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
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
                                val progressLabel = if (progress >= 0) "$progress%" else "Đang tải..."
                                val sizeText = if (totalBytes > 0) {
                                    "${AppToast.formatBytes(bytesDownloaded)} / ${AppToast.formatBytes(totalBytes)}"
                                } else {
                                    AppToast.formatBytes(bytesDownloaded)
                                }
                                val speedText = if (currentSpeed > 0) " • ${AppToast.formatSpeed(currentSpeed)}" else ""

                                AppToast.showProgress(
                                    title = option.title.ifEmpty { "Đang tải video..." },
                                    message = "$sizeText ($progressLabel)",
                                    progress = progress,
                                    downloadedBytes = bytesDownloaded,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = currentSpeed
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
                                val finalSize = if (totalBytes > 0) AppToast.formatBytes(totalBytes) else if (bytesDownloaded > 0) AppToast.formatBytes(bytesDownloaded) else ""
                                val sizeInfo = if (finalSize.isNotEmpty() && finalSize != "0 B") "$finalSize • " else ""
                                AppToast.showSuccess(
                                    title = "Tải hoàn tất!",
                                    message = "${sizeInfo}Đã lưu vào máy"
                                )
                                notificationHelper.showCompleted(
                                    notificationId = downloadId.toInt(),
                                    title = "Tải hoàn tất!",
                                    contentText = "$displayTitle • ${sizeInfo}Đã lưu vào máy"
                                )
                            }
                            DownloadManager.STATUS_FAILED -> {
                                isPolling = false
                                AppToast.showError(
                                    title = "Tải thất bại",
                                    message = "Không thể tải video. Vui lòng kiểm tra mạng và thử lại."
                                )
                                notificationHelper.showError(
                                    notificationId = downloadId.toInt(),
                                    title = "Tải thất bại",
                                    contentText = "Không thể tải video $displayTitle"
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
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
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
