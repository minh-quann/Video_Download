package com.buwin.tiktokvideodownload.data.model

/**
 * Supported download format types for TikTok content.
 */
enum class DownloadFormatType {
    VIDEO_HD_NO_WATERMARK,
    VIDEO_SD_NO_WATERMARK,
    VIDEO_WATERMARK,
    AUDIO_MP3,
    PHOTO_SLIDE
}

/**
 * Represents an individual downloadable option.
 */
data class DownloadOption(
    val type: DownloadFormatType,
    val title: String,
    val description: String,
    val downloadUrl: String,
    val fileExtension: String,
    val mimeType: String
)

/**
 * Metadata information of an extracted TikTok post/video.
 */
data class TikTokVideoInfo(
    val id: String,
    val title: String,
    val authorUsername: String,
    val authorNickname: String,
    val authorAvatarUrl: String,
    val coverUrl: String,
    val durationSeconds: Int,
    val likesCount: Long,
    val commentsCount: Long,
    val sharesCount: Long,
    val originalUrl: String,
    val options: List<DownloadOption>,
    val photoImages: List<String> = emptyList()
)

/**
 * Record of a completed or ongoing download.
 */
data class DownloadRecord(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val formatTitle: String,
    val fileExtension: String,
    val downloadId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String = ""
)
