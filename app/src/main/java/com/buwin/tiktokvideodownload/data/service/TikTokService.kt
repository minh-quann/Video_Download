package com.buwin.tiktokvideodownload.data.service

import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Service responsible for parsing TikTok URLs and extracting direct download streams.
 */
class TikTokService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val urlPattern = Pattern.compile("https?://[^\\s<>\"'\\[\\]{}|\\\\^`]+")

    /**
     * Extracts a valid URL from freeform text (such as clipboard content or share intent).
     */
    fun extractUrl(text: String): String? {
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val url = matcher.group().trimEnd('.', ',', ';', '!', '?', ')', ']', '"', '\'')
            if (url.contains("tiktok.com") || url.contains("douyin.com")) {
                return url
            }
        }
        return null
    }

    /**
     * Fetches video information and available download streams for the given TikTok link.
     */
    suspend fun fetchVideoInfo(rawUrl: String): Result<TikTokVideoInfo> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = extractUrl(rawUrl) ?: rawUrl.trim()
            if (cleanUrl.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Invalid TikTok URL"))
            }

            val apiUrl = "https://www.tikwm.com/api/"
            val formBody = FormBody.Builder()
                .add("url", cleanUrl)
                .add("count", "12")
                .add("cursor", "0")
                .add("web", "1")
                .add("hd", "1")
                .build()

            val request = Request.Builder()
                .url(apiUrl)
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
                }

                val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val json = JSONObject(bodyString)

                val code = json.optInt("code", -1)
                if (code != 0) {
                    val msg = json.optString("msg", "Failed to parse video. Please check the URL.")
                    return@withContext Result.failure(Exception(msg))
                }

                val data = json.getJSONObject("data")
                val id = data.optString("id", System.currentTimeMillis().toString())
                val title = data.optString("title", "TikTok Video $id")
                val originCover = data.optString("origin_cover", "")
                val rawCover = data.optString("cover", "")
                val cover = normalizeMediaUrl(originCover.ifEmpty { rawCover })
                val duration = data.optInt("duration", 0)

                val authorObj = data.optJSONObject("author")
                val authorUsername = authorObj?.optString("unique_id", "TikTok User") ?: "TikTok User"
                val authorNickname = authorObj?.optString("nickname", authorUsername) ?: authorUsername
                val authorAvatar = normalizeMediaUrl(authorObj?.optString("avatar", "") ?: "")

                val diggCount = data.optLong("digg_count", 0L)
                val commentCount = data.optLong("comment_count", 0L)
                val shareCount = data.optLong("share_count", 0L)

                val options = mutableListOf<DownloadOption>()

                // 1. HD No Watermark (Highest resolution)
                val hdPlay = data.optString("hdplay", "")
                if (hdPlay.isNotEmpty()) {
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.VIDEO_HD_NO_WATERMARK,
                            title = "Video HD Không Logo",
                            description = "Độ phân giải cao nhất (1080p / 720p HD)",
                            downloadUrl = normalizeMediaUrl(hdPlay),
                            fileExtension = "mp4",
                            mimeType = "video/mp4"
                        )
                    )
                }

                // 2. Standard SD No Watermark
                val play = data.optString("play", "")
                if (play.isNotEmpty()) {
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.VIDEO_SD_NO_WATERMARK,
                            title = "Video Chuẩn Không Logo",
                            description = "Dung lượng nhẹ, tải nhanh hơn",
                            downloadUrl = normalizeMediaUrl(play),
                            fileExtension = "mp4",
                            mimeType = "video/mp4"
                        )
                    )
                }

                // 3. With Watermark
                val wmPlay = data.optString("wmplay", "")
                if (wmPlay.isNotEmpty()) {
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.VIDEO_WATERMARK,
                            title = "Video Kèm Watermark",
                            description = "Bản gốc có logo TikTok và ID tác giả",
                            downloadUrl = normalizeMediaUrl(wmPlay),
                            fileExtension = "mp4",
                            mimeType = "video/mp4"
                        )
                    )
                }

                // 4. Audio MP3
                val music = data.optString("music", "")
                if (music.isNotEmpty()) {
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.AUDIO_MP3,
                            title = "Nhạc Chuông / Âm Thanh MP3",
                            description = "Tách riêng file âm thanh gốc",
                            downloadUrl = normalizeMediaUrl(music),
                            fileExtension = "mp3",
                            mimeType = "audio/mpeg"
                        )
                    )
                }

                // 5. Image Slides (if photo post)
                val imagesJson = data.optJSONArray("images")
                val imagesList = mutableListOf<String>()
                if (imagesJson != null) {
                    for (i in 0 until imagesJson.length()) {
                        imagesList.add(normalizeMediaUrl(imagesJson.getString(i)))
                    }
                }

                Result.success(
                    TikTokVideoInfo(
                        id = id,
                        title = title,
                        authorUsername = authorUsername,
                        authorNickname = authorNickname,
                        authorAvatarUrl = authorAvatar,
                        coverUrl = cover,
                        durationSeconds = duration,
                        likesCount = diggCount,
                        commentsCount = commentCount,
                        sharesCount = shareCount,
                        originalUrl = cleanUrl,
                        options = options,
                        photoImages = imagesList
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Normalizes partial or relative URLs returned by the API into fully qualified HTTPS URLs.
     */
    private fun normalizeMediaUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "https://www.tikwm.com$trimmed"
            else -> "https://www.tikwm.com/$trimmed"
        }
    }
}
