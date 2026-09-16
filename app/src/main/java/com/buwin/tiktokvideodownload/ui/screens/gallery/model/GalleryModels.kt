package com.buwin.tiktokvideodownload.ui.screens.gallery.model

import android.net.Uri
import com.buwin.tiktokvideodownload.data.model.DownloadRecord

/**
 * Filter category tabs for Gallery / Media Collection.
 */
enum class GalleryFilterCategory(val key: String, val label: String) {
    ALL("all", "Tất cả"),
    VIDEOS("videos", "Video"),
    PHOTOS("photos", "Ảnh"),
    DOWNLOADED("downloaded", "Đã tải về"),
    EDITED("edited", "Đã chỉnh sửa")
}

/**
 * Represents a playable or editable media item in the Gallery.
 */
data class GalleryMediaItem(
    val id: String,
    val title: String,
    val uri: Uri,
    val isVideo: Boolean = true,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceRecord: DownloadRecord? = null,
    val isFromDevice: Boolean = false,
    val isDownloaded: Boolean = false,
    val isEdited: Boolean = false
) {
    val durationText: String
        get() {
            if (!isVideo || durationMs <= 0) return ""
            val totalSec = (durationMs / 1000).toInt()
            val m = totalSec / 60
            val s = totalSec % 60
            return String.format("%02d:%02d", m, s)
        }

    fun toDownloadRecord(): DownloadRecord {
        return sourceRecord ?: DownloadRecord(
            id = id,
            title = title,
            author = if (isDownloaded) "TikTok Download" else "Bộ sưu tập máy",
            coverUrl = uri.toString(),
            formatTitle = if (isVideo) "MP4" else "Ảnh",
            fileExtension = if (isVideo) "mp4" else "jpg",
            downloadId = 0L,
            timestamp = timestamp,
            filePath = uri.toString(),
            originalUrl = ""
        )
    }
}
