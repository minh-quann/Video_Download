package com.buwin.tiktokvideodownload.data.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles file downloading using Android's system DownloadManager and persists download history.
 */
class DownloadManagerHelper(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("tiktok_downloads_prefs", Context.MODE_PRIVATE)

    /**
     * Enqueues a download with system DownloadManager and records it to history.
     */
    fun enqueueDownload(videoInfo: TikTokVideoInfo, option: DownloadOption): Long {
        val sanitizedTitle = videoInfo.title
            .take(30)
            .replace("[^a-zA-Z0-9_ -]".toRegex(), "")
            .trim()
            .ifEmpty { "TikTok_${videoInfo.id}" }

        val fileName = "${sanitizedTitle}_${option.type.name.lowercase()}_${System.currentTimeMillis()}.${option.fileExtension}"

        val request = DownloadManager.Request(Uri.parse(option.downloadUrl)).apply {
            setTitle(videoInfo.title.ifEmpty { "TikTok Video" })
            setDescription("Đang tải ${option.title}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TikTokDownloads/$fileName")
            setMimeType(option.mimeType)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadId = downloadManager.enqueue(request)

        val record = DownloadRecord(
            id = videoInfo.id,
            title = videoInfo.title.ifEmpty { "TikTok Video ${videoInfo.id}" },
            author = "@${videoInfo.authorUsername}",
            coverUrl = videoInfo.coverUrl,
            formatTitle = option.title,
            fileExtension = option.fileExtension,
            downloadId = downloadId
        )
        saveRecord(record)

        return downloadId
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
