package com.buwin.tiktokvideodownload.data.service

import android.text.Html
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Service responsible for parsing Facebook URLs (Videos, Reels, Watch)
 * and extracting direct download streams using a hybrid approach
 * (Direct HTML scraping + Resolver API fallback).
 */
class FacebookService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val urlPattern = Pattern.compile("https?://[^\\s<>\"'\\[\\]{}|\\\\^`]+")

    private val hdRegex = Pattern.compile("\"(?:browser_native_hd_url|playable_url_quality_hd)\":\"([^\"]+)\"")
    private val sdRegex = Pattern.compile("\"(?:browser_native_sd_url|playable_url)\":\"([^\"]+)\"")
    private val ogTitleRegex = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"")
    private val pageTitleRegex = Pattern.compile("<title>([^<]+)</title>")
    private val ogImageRegex = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"")

    /**
     * Checks if a URL belongs to Facebook domains.
     */
    fun isFacebookUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("facebook.com") ||
                lower.contains("fb.watch") ||
                lower.contains("fb.com") ||
                lower.contains("fb.me")
    }

    /**
     * Extracts a valid Facebook URL from freeform text.
     */
    fun extractUrl(text: String): String? {
        val matcher = urlPattern.matcher(text)
        while (matcher.find()) {
            val url = matcher.group().trimEnd('.', ',', ';', '!', '?', ')', ']', '"', '\'')
            if (isFacebookUrl(url)) {
                return url
            }
        }
        return null
    }

    /**
     * Fetches video information and download streams for a Facebook link.
     */
    suspend fun fetchVideoInfo(rawUrl: String): Result<TikTokVideoInfo> = withContext(Dispatchers.IO) {
        val cleanUrl = extractUrl(rawUrl) ?: rawUrl.trim()
        if (cleanUrl.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Facebook URL"))
        }

        // Strategy 1: Attempt direct Facebook HTML parsing
        val directResult = tryDirectExtraction(cleanUrl)
        if (directResult.isSuccess) {
            return@withContext directResult
        }

        // Strategy 2: Fallback to high-reliability BDBots Facebook resolver API
        val fallbackResult = tryApiFallback(cleanUrl)
        if (fallbackResult.isSuccess) {
            return@withContext fallbackResult
        }

        Result.failure(
            Exception(
                "Không thể bóc tách video Facebook. Vui lòng kiểm tra lại đường link hoặc đảm bảo video ở chế độ công khai (Public)."
            )
        )
    }

    /**
     * Scrapes Facebook page directly to extract direct MP4 CDN links.
     */
    private fun tryDirectExtraction(url: String): Result<TikTokVideoInfo> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-User", "?1")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP error: ${response.code}"))
                }

                val html = response.body?.string() ?: return Result.failure(Exception("Empty response body"))

                val hdMatch = hdRegex.matcher(html)
                val sdMatch = sdRegex.matcher(html)

                var hdUrl: String? = null
                var sdUrl: String? = null

                if (hdMatch.find()) {
                    hdMatch.group(1)?.let { hdUrl = unescapeJsonString(it) }
                }
                if (sdMatch.find()) {
                    sdMatch.group(1)?.let { sdUrl = unescapeJsonString(it) }
                }

                // If no video stream could be found in HTML, abort direct strategy
                if (hdUrl.isNullOrEmpty() && sdUrl.isNullOrEmpty()) {
                    return Result.failure(Exception("No video stream found in page HTML"))
                }

                // Extract video title
                var title = "Facebook Video"
                val ogTitleMatch = ogTitleRegex.matcher(html)
                val pageTitleMatch = pageTitleRegex.matcher(html)
                if (ogTitleMatch.find()) {
                    ogTitleMatch.group(1)?.let { title = unescapeHtml(it) }
                } else if (pageTitleMatch.find()) {
                    pageTitleMatch.group(1)?.let { title = unescapeHtml(it) }
                }

                // Extract cover image
                var coverUrl = ""
                val ogImageMatch = ogImageRegex.matcher(html)
                if (ogImageMatch.find()) {
                    ogImageMatch.group(1)?.let { coverUrl = unescapeHtml(it) }
                }

                val id = System.currentTimeMillis().toString()
                val options = mutableListOf<DownloadOption>()

                if (!hdUrl.isNullOrEmpty()) {
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.VIDEO_HD_NO_WATERMARK,
                            title = "Video Facebook HD",
                            description = "Chất lượng cao nhất (1080p / 720p HD)",
                            downloadUrl = hdUrl,
                            fileExtension = "mp4",
                            mimeType = "video/mp4"
                        )
                    )
                }

                if (!sdUrl.isNullOrEmpty()) {
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.VIDEO_SD_NO_WATERMARK,
                            title = "Video Facebook SD",
                            description = "Chất lượng chuẩn (360p / 480p SD)",
                            downloadUrl = sdUrl,
                            fileExtension = "mp4",
                            mimeType = "video/mp4"
                        )
                    )
                }

                // 3. Audio MP3 option extracted from available video streams
                // ponytail: Facebook serves AAC audio tracks in progressive MP4 streams; using SD stream saves bandwidth
                val audioUrl = sdUrl ?: hdUrl
                if (!audioUrl.isNullOrEmpty()) {
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.AUDIO_MP3,
                            title = "Âm Thanh MP3",
                            description = "Tách riêng âm thanh từ video",
                            downloadUrl = audioUrl,
                            fileExtension = "mp3",
                            mimeType = "audio/mpeg"
                        )
                    )
                }

                Result.success(
                    TikTokVideoInfo(
                        id = id,
                        title = title,
                        authorUsername = "facebook",
                        authorNickname = "Facebook",
                        authorAvatarUrl = "",
                        coverUrl = coverUrl,
                        durationSeconds = 0,
                        likesCount = 0L,
                        commentsCount = 0L,
                        sharesCount = 0L,
                        originalUrl = url,
                        options = options,
                        photoImages = emptyList()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queries secondary resolver API in case direct scraping is blocked or incomplete.
     */
    private fun tryApiFallback(url: String): Result<TikTokVideoInfo> {
        return try {
            val encodedUrl = URLEncoder.encode(url, "UTF-8")
            val apiUrl = "https://facebook.bdbots.org/dl?url=$encodedUrl"

            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("API HTTP error: ${response.code}"))
                }

                val bodyString = response.body?.string() ?: return Result.failure(Exception("Empty API body"))
                val json = JSONObject(bodyString)

                if (!json.optBoolean("success", false)) {
                    val msg = json.optString("message", "API resolution failed")
                    return Result.failure(Exception(msg))
                }

                val title = json.optString("title", "Facebook Video")
                val videosObj = json.optJSONObject("videos") ?: return Result.failure(Exception("No videos object in API response"))

                val options = mutableListOf<DownloadOption>()

                val hdObj = videosObj.optJSONObject("hd")
                val hdUrl = hdObj?.optString("url")?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                if (hdObj != null && hdUrl != null) {
                    val hdSize = hdObj.optString("size", "")
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.VIDEO_HD_NO_WATERMARK,
                            title = "Video Facebook HD",
                            description = if (hdSize.isNotEmpty()) "Chất lượng cao HD ($hdSize)" else "Chất lượng cao nhất (HD)",
                            downloadUrl = hdUrl,
                            fileExtension = "mp4",
                            mimeType = "video/mp4"
                        )
                    )
                }

                val sdObj = videosObj.optJSONObject("sd")
                val sdUrl = sdObj?.optString("url")?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                if (sdObj != null && sdUrl != null) {
                    val sdSize = sdObj.optString("size", "")
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.VIDEO_SD_NO_WATERMARK,
                            title = "Video Facebook SD",
                            description = if (sdSize.isNotEmpty()) "Chất lượng chuẩn SD ($sdSize)" else "Chất lượng chuẩn (SD)",
                            downloadUrl = sdUrl,
                            fileExtension = "mp4",
                            mimeType = "video/mp4"
                        )
                    )
                }

                // 3. Audio MP3 option extracted from available video streams
                val audioUrl = sdUrl ?: hdUrl
                if (!audioUrl.isNullOrEmpty()) {
                    options.add(
                        DownloadOption(
                            type = DownloadFormatType.AUDIO_MP3,
                            title = "Âm Thanh MP3",
                            description = "Tách riêng âm thanh từ video",
                            downloadUrl = audioUrl,
                            fileExtension = "mp3",
                            mimeType = "audio/mpeg"
                        )
                    )
                }

                if (options.isEmpty()) {
                    return Result.failure(Exception("No downloadable streams in API response"))
                }

                Result.success(
                    TikTokVideoInfo(
                        id = System.currentTimeMillis().toString(),
                        title = title,
                        authorUsername = "facebook",
                        authorNickname = "Facebook",
                        authorAvatarUrl = "",
                        coverUrl = "",
                        durationSeconds = 0,
                        likesCount = 0L,
                        commentsCount = 0L,
                        sharesCount = 0L,
                        originalUrl = url,
                        options = options,
                        photoImages = emptyList()
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unescapes JSON-encoded strings including unicode and slashes.
     */
    private fun unescapeJsonString(input: String): String {
        return try {
            val unescaped = JSONTokener("\"$input\"").nextValue().toString()
            unescaped.replace("\\/", "/")
        } catch (_: Exception) {
            input.replace("\\/", "/")
                .replace("\\u0025", "%")
                .replace("\\u0026", "&")
                .replace("\\u003D", "=")
                .replace("\\u003d", "=")
        }
    }

    /**
     * Converts HTML character entities into readable text.
     */
    private fun unescapeHtml(text: String): String {
        return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }
}
