package com.buwin.tiktokvideodownload.data.download

import android.app.DownloadManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.buwin.tiktokvideodownload.data.model.DownloadFormatType
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import com.buwin.tiktokvideodownload.data.notification.NotificationHelper
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.buwin.tiktokvideodownload.ui.components.toast.DownloadItemProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Handles concurrent parallel file downloading using OkHttpClient streaming and MediaStore.Downloads.
 * Tracks live download progress per task, persists download history, and broadcasts updates to both
 * the in-app multi-download WaterdropToast and system Local Notifications.
 */
class DownloadManagerHelper(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = context.getSharedPreferences("tiktok_downloads_prefs", Context.MODE_PRIVATE)
    private val notificationHelper = NotificationHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestoreSync = com.buwin.tiktokvideodownload.data.sync.FirestoreSyncManager()

    // High performance OkHttpClient supporting concurrent parallel connections
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val idGenerator = AtomicLong(System.currentTimeMillis())
    private val activeTasks = ConcurrentHashMap<Long, ActiveDownloadTask>()
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeCalls = ConcurrentHashMap<Long, Call>()
    private val cancelledDownloadIds = Collections.synchronizedSet(mutableSetOf<Long>())

    data class ActiveDownloadTask(
        val downloadId: Long,
        val optionTitle: String,
        val videoTitle: String,
        val isAudio: Boolean,
        var bytesDownloaded: Long = 0L,
        var totalBytes: Long = 0L,
        var speedBytesPerSec: Long = 0L,
        var progress: Int = -1
    )

    /**
     * Centralized aggregator for live download updates into the in-app Waterdrop dynamic toast.
     * Combines multiple simultaneous format downloads smoothly, showing individual progress bars
     * and aggregated speed/progress.
     */
    @Synchronized
    private fun dispatchToastUpdate() {
        val tasks = activeTasks.values.toList()
        if (tasks.isEmpty()) return

        if (tasks.size == 1) {
            val single = tasks.first()
            val progressLabel = if (single.progress >= 0) "${single.progress}%" else "Đang tải..."
            val sizeText = if (single.totalBytes > 0) {
                "${AppToast.formatBytes(single.bytesDownloaded)} / ${AppToast.formatBytes(single.totalBytes)}"
            } else {
                AppToast.formatBytes(single.bytesDownloaded)
            }
            val downloadingTitle = single.optionTitle.ifEmpty {
                if (single.isAudio) "Đang tải âm thanh..." else "Đang tải video..."
            }

            AppToast.showProgress(
                title = downloadingTitle,
                message = "$sizeText ($progressLabel)",
                progress = single.progress,
                downloadedBytes = single.bytesDownloaded,
                totalBytes = single.totalBytes,
                speedBytesPerSec = single.speedBytesPerSec,
                downloadId = single.downloadId
            )
        } else {
            val count = tasks.size
            val totalDownloaded = tasks.sumOf { it.bytesDownloaded }
            val allHaveTotal = tasks.all { it.totalBytes > 0 }
            val sumTotalBytes = tasks.sumOf { it.totalBytes }
            val combinedSpeed = tasks.sumOf { it.speedBytesPerSec }

            val overallProgress = if (allHaveTotal && sumTotalBytes > 0) {
                ((totalDownloaded * 100) / sumTotalBytes).toInt().coerceIn(0, 100)
            } else {
                val valid = tasks.filter { it.progress >= 0 }
                if (valid.isNotEmpty()) (valid.sumOf { it.progress } / valid.size).coerceIn(0, 100) else -1
            }

            val progressLabel = if (overallProgress >= 0) "$overallProgress%" else "Đang tải..."
            val sizeText = if (allHaveTotal && sumTotalBytes > 0) {
                "${AppToast.formatBytes(totalDownloaded)} / ${AppToast.formatBytes(sumTotalBytes)}"
            } else {
                "${AppToast.formatBytes(totalDownloaded)} ($count tệp)"
            }

            val itemList = tasks.map { task ->
                DownloadItemProgress(
                    downloadId = task.downloadId,
                    title = task.optionTitle,
                    message = if (task.totalBytes > 0) {
                        "${AppToast.formatBytes(task.bytesDownloaded)} / ${AppToast.formatBytes(task.totalBytes)}"
                    } else {
                        AppToast.formatBytes(task.bytesDownloaded)
                    },
                    progress = task.progress,
                    downloadedBytes = task.bytesDownloaded,
                    totalBytes = task.totalBytes,
                    speedBytesPerSec = task.speedBytesPerSec,
                    isAudio = task.isAudio
                )
            }

            AppToast.showProgress(
                title = "Đang tải $count tệp...",
                message = "$sizeText ($progressLabel)",
                progress = overallProgress,
                downloadedBytes = totalDownloaded,
                totalBytes = if (allHaveTotal) sumTotalBytes else 0L,
                speedBytesPerSec = combinedSpeed,
                downloadId = 0L,
                items = itemList
            )
        }
    }

    /**
     * Enqueues and initiates a concurrent download stream on Dispatchers.IO.
     * Multiple calls execute in parallel without sequential queue blocking.
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

        val downloadId = idGenerator.incrementAndGet()
        cancelledDownloadIds.remove(downloadId)

        val isAudio = option.type == DownloadFormatType.AUDIO_MP3
        val record = DownloadRecord(
            id = videoInfo.id,
            title = videoInfo.title.ifEmpty { "Video ${videoInfo.id}" },
            author = if (videoInfo.authorUsername.isNotEmpty()) "@${videoInfo.authorUsername}" else defaultPrefix,
            coverUrl = videoInfo.coverUrl,
            formatTitle = option.title,
            fileExtension = option.fileExtension,
            downloadId = downloadId,
            filePath = "",
            originalUrl = videoInfo.originalUrl
        )
        saveRecord(record)

        // Register in active multi-download registry and dispatch aggregated update
        val task = ActiveDownloadTask(
            downloadId = downloadId,
            optionTitle = option.title.ifEmpty { if (isAudio) "Âm thanh MP3" else "Video" },
            videoTitle = videoInfo.title.ifEmpty { "$defaultPrefix Video" },
            isAudio = isAudio,
            bytesDownloaded = 0L,
            totalBytes = 0L,
            speedBytesPerSec = 0L,
            progress = 0
        )
        activeTasks[downloadId] = task
        dispatchToastUpdate()

        notificationHelper.showProgress(
            notificationId = downloadId.toInt(),
            title = videoInfo.title.ifEmpty { "$defaultPrefix Video" },
            contentText = "${option.title} • Đang chuẩn bị tải...",
            progress = 0
        )

        // Launch concurrent parallel download coroutine
        val job = scope.launch {
            executeParallelDownload(
                downloadId = downloadId,
                videoInfo = videoInfo,
                option = option,
                fileName = fileName,
                subDir = subDir,
                defaultPrefix = defaultPrefix
            )
        }
        activeJobs[downloadId] = job

        return downloadId
    }

    /**
     * Executes the actual streaming download concurrently using OkHttpClient,
     * writes directly to MediaStore.Downloads (or public Downloads folder fallback),
     * and streams progress updates in real-time.
     */
    private suspend fun executeParallelDownload(
        downloadId: Long,
        videoInfo: TikTokVideoInfo,
        option: DownloadOption,
        fileName: String,
        subDir: String,
        defaultPrefix: String
    ) {
        val displayTitle = videoInfo.title.ifEmpty { "$defaultPrefix Video" }
        val isAudio = option.type == DownloadFormatType.AUDIO_MP3
        var savedContentUri: Uri? = null
        var fallbackFile: File? = null
        var outputStream: OutputStream? = null

        try {
            // 1. Prepare MediaStore entry in Downloads directory
            val mime = option.mimeType.ifBlank { if (isAudio) "audio/mpeg" else "video/mp4" }
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$subDir")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            savedContentUri = try {
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } catch (_: Exception) {
                null
            }

            outputStream = if (savedContentUri != null) {
                context.contentResolver.openOutputStream(savedContentUri)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), subDir)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                fallbackFile = file
                FileOutputStream(file)
            }

            if (outputStream == null) {
                throw IOException("Không thể mở luồng ghi tệp tin")
            }

            // 2. Build OkHttp request and start streaming
            val request = Request.Builder()
                .url(option.downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .build()

            val call = okHttpClient.newCall(request)
            activeCalls[downloadId] = call

            val response = call.execute()
            if (!response.isSuccessful) {
                throw IOException("Lỗi kết nối HTTP: ${response.code}")
            }

            val body = response.body ?: throw IOException("Phản hồi rỗng từ máy chủ")
            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdateMs = System.currentTimeMillis()
            var lastBytes = 0L
            var currentSpeed = 0L

            outputStream.use { out ->
                inputStream.use { inStream ->
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        if (cancelledDownloadIds.contains(downloadId) || !currentCoroutineContext().isActive) {
                            throw CancellationException("Tải xuống bị hủy bởi người dùng")
                        }

                        out.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        val timeDelta = now - lastUpdateMs
                        if (timeDelta >= 250) {
                            val bytesDelta = (totalRead - lastBytes).coerceAtLeast(0L)
                            val instantSpeed = (bytesDelta * 1000L) / timeDelta.coerceAtLeast(1L)
                            currentSpeed = if (currentSpeed == 0L) instantSpeed else ((currentSpeed * 0.4) + (instantSpeed * 0.6)).toLong()
                            lastBytes = totalRead
                            lastUpdateMs = now

                            val progress = if (totalBytes > 0) {
                                ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }

                            val task = activeTasks[downloadId]
                            if (task != null) {
                                task.bytesDownloaded = totalRead
                                task.totalBytes = totalBytes
                                task.speedBytesPerSec = currentSpeed
                                task.progress = progress
                            }
                            dispatchToastUpdate()

                            val progressLabel = if (progress >= 0) "$progress%" else "Đang tải..."
                            val sizeText = if (totalBytes > 0) {
                                "${AppToast.formatBytes(totalRead)} / ${AppToast.formatBytes(totalBytes)}"
                            } else {
                                AppToast.formatBytes(totalRead)
                            }
                            val speedText = if (currentSpeed > 0) " • ${AppToast.formatSpeed(currentSpeed)}" else ""

                            notificationHelper.showProgress(
                                notificationId = downloadId.toInt(),
                                title = displayTitle,
                                contentText = "$sizeText ($progressLabel)$speedText",
                                progress = progress
                            )
                        }
                    }
                    out.flush()
                }
            }

            // 3. Mark file as finalized in MediaStore
            if (savedContentUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val finalizeValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                context.contentResolver.update(savedContentUri, finalizeValues, null, null)
            }

            val finalFilePath = savedContentUri?.toString() ?: fallbackFile?.absolutePath ?: ""
            updateRecordCompleted(downloadId, finalFilePath)

            activeTasks.remove(downloadId)
            activeJobs.remove(downloadId)
            activeCalls.remove(downloadId)

            val finalSize = if (totalBytes > 0) AppToast.formatBytes(totalBytes) else if (totalRead > 0) AppToast.formatBytes(totalRead) else ""
            val sizeInfo = if (finalSize.isNotEmpty() && finalSize != "0 B") "$finalSize • " else ""
            val actionLabel = if (isAudio) "nghe âm thanh" else "xem video"

            notificationHelper.showCompleted(
                notificationId = downloadId.toInt(),
                title = "Tải hoàn tất!",
                contentText = "$displayTitle • ${sizeInfo}Đã lưu vào máy"
            )

            if (activeTasks.isNotEmpty()) {
                dispatchToastUpdate()
            } else {
                AppToast.showSuccess(
                    title = "Tải hoàn tất!",
                    message = "${sizeInfo}Chạm để $actionLabel",
                    downloadId = downloadId
                )
            }

        } catch (e: CancellationException) {
            cleanupDownloadResources(downloadId, savedContentUri, fallbackFile)
            if (activeTasks.isNotEmpty()) {
                dispatchToastUpdate()
            } else {
                AppToast.hide()
            }
        } catch (e: Exception) {
            cleanupDownloadResources(downloadId, savedContentUri, fallbackFile)
            if (activeTasks.isNotEmpty()) {
                dispatchToastUpdate()
            } else {
                val errorMsg = if (isAudio) "Không thể tải âm thanh. Vui lòng kiểm tra mạng và thử lại." else "Không thể tải video. Vui lòng kiểm tra mạng và thử lại."
                AppToast.showError(
                    title = "Tải thất bại",
                    message = errorMsg
                )
            }
            notificationHelper.showError(
                notificationId = downloadId.toInt(),
                title = "Tải thất bại",
                contentText = "Không thể tải $displayTitle"
            )
        }
    }

    /**
     * Cleans up allocated resources, deletes partial file / content URI,
     * and clears uncompleted record from history.
     */
    private fun cleanupDownloadResources(
        downloadId: Long,
        savedContentUri: Uri?,
        fallbackFile: File?
    ) {
        activeTasks.remove(downloadId)
        activeJobs.remove(downloadId)
        activeCalls.remove(downloadId)
        cancelledDownloadIds.remove(downloadId)
        notificationHelper.cancel(downloadId.toInt())

        try {
            if (savedContentUri != null) {
                context.contentResolver.delete(savedContentUri, null, null)
            }
            fallbackFile?.let {
                if (it.exists()) it.delete()
            }
        } catch (_: Exception) {
        }

        removeHistoryRecord(downloadId)
    }

    /**
     * Cancels an active download or all active downloads, dismisses system notification,
     * terminates running network requests, removes partial files, and cleans uncompleted records.
     */
    fun cancelDownload(downloadId: Long) {
        if (downloadId <= 0) {
            val allIds = activeTasks.keys.toList()
            allIds.forEach { cancelDownload(it) }
            AppToast.hide()
            return
        }
        cancelledDownloadIds.add(downloadId)
        activeTasks.remove(downloadId)
        activeCalls.remove(downloadId)?.cancel()
        activeJobs.remove(downloadId)?.cancel()
        notificationHelper.cancel(downloadId.toInt())
        removeHistoryRecord(downloadId)

        try {
            downloadManager.remove(downloadId)
        } catch (_: Exception) {
        }

        if (activeTasks.isNotEmpty()) {
            dispatchToastUpdate()
        } else {
            AppToast.hide()
        }
    }

    /**
     * Updates an existing record with its completed file path and latest timestamp,
     * and syncs it with Cloud Firestore if signed in.
     */
    private fun updateRecordCompleted(downloadId: Long, finalFilePath: String) {
        try {
            val records = getHistory().toMutableList()
            val index = records.indexOfFirst { it.downloadId == downloadId }
            if (index != -1) {
                val old = records[index]
                val updated = old.copy(filePath = finalFilePath, timestamp = System.currentTimeMillis())
                records[index] = updated
                saveAllRecords(records)
                scope.launch {
                    firestoreSync.syncRecordToCloud(updated)
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Removes an uncompleted or cancelled record from history, storage, and cloud.
     */
    fun removeHistoryRecord(downloadId: Long) {
        try {
            val records = getHistory().toMutableList()
            val target = records.firstOrNull { it.downloadId == downloadId }
            if (target != null) {
                records.removeAll { it.downloadId == downloadId }
                saveAllRecords(records)

                if (target.filePath.startsWith("content://")) {
                    try {
                        context.contentResolver.delete(Uri.parse(target.filePath), null, null)
                    } catch (_: Exception) {
                    }
                } else if (target.filePath.isNotEmpty()) {
                    val f = File(target.filePath)
                    if (f.exists()) f.delete()
                }

                scope.launch {
                    firestoreSync.deleteRecordFromCloud(downloadId)
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Deletes a downloaded record from filesystem, MediaStore content resolver, history, and cloud.
     */
    fun deleteRecord(record: DownloadRecord, uri: Uri? = null) {
        removeHistoryRecord(record.downloadId)
        try {
            val targetUri = uri ?: getDownloadedUri(record)
            if (targetUri != null) {
                if (targetUri.scheme == "file") {
                    targetUri.path?.let { File(it).delete() }
                } else {
                    context.contentResolver.delete(targetUri, null, null)
                }
            }
            if (record.filePath.isNotEmpty()) {
                val f = File(record.filePath)
                if (f.exists()) f.delete()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Saves all records into SharedPreferences.
     */
    private fun saveAllRecords(records: List<DownloadRecord>) {
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
                put("originalUrl", item.originalUrl)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("history_json", jsonArray.toString()).apply()
    }

    /**
     * Saves a new record to the top of SharedPreferences history.
     */
    private fun saveRecord(record: DownloadRecord) {
        val records = getHistory().toMutableList()
        records.add(0, record)
        saveAllRecords(records)
    }

    /**
     * Adds an edited or custom media record to local history and cloud sync.
     */
    fun addHistoryRecord(record: DownloadRecord) {
        saveRecord(record)
        scope.launch {
            try {
                firestoreSync.syncRecordToCloud(record)
            } catch (_: Exception) {
            }
        }
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
                        filePath = obj.optString("filePath"),
                        originalUrl = obj.optString("originalUrl", "")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return result
    }

    /**
     * Obtains a playable content/file Uri for the downloaded record.
     * Supports both modern MediaStore content URIs and legacy file paths.
     */
    fun getDownloadedUri(record: DownloadRecord): Uri? {
        // 1. Direct Content Uri (preferred for Android Scoped Storage & Sharing)
        if (record.filePath.startsWith("content://")) {
            try {
                val uri = Uri.parse(record.filePath)
                context.contentResolver.openFileDescriptor(uri, "r")?.close()
                return uri
            } catch (_: Exception) {
            }
        }

        // 2. Try recorded absolute file path
        if (record.filePath.isNotEmpty()) {
            try {
                val file = File(record.filePath)
                if (file.exists() && file.length() > 0) {
                    return Uri.fromFile(file)
                }
            } catch (_: Exception) {
            }
        }

        // 3. Try legacy DownloadManager content Uri (for items downloaded before update)
        try {
            if (record.downloadId > 0) {
                val dmUri = downloadManager.getUriForDownloadedFile(record.downloadId)
                if (dmUri != null) return dmUri
            }
        } catch (_: Exception) {
        }

        // 4. Query MediaStore.Downloads for matching item
        try {
            val fileName = if (record.filePath.isNotEmpty()) File(record.filePath).name else ""
            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME
            )
            val selection = if (fileName.isNotEmpty()) {
                "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            } else {
                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
            }
            val selectionArgs = if (fileName.isNotEmpty()) {
                arrayOf(fileName)
            } else {
                arrayOf("%${record.id}%")
            }
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Downloads._ID} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    return ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        id
                    )
                }
            }
        } catch (_: Exception) {
        }

        // 5. Fallback: Search in TikTokDownloads or FacebookDownloads directory
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subFolders = listOf("TikTokDownloads", "FacebookDownloads", "")
            for (sub in subFolders) {
                val dir = if (sub.isEmpty()) downloadDir else File(downloadDir, sub)
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
     * Clears all download history records locally and from Cloud Firestore.
     */
    fun clearHistory() {
        prefs.edit().remove("history_json").apply()
        scope.launch {
            firestoreSync.clearCloudHistory()
        }
    }

    /**
     * Synchronizes local download history with Cloud Firestore.
     * Merges records from both sources and backfills missing records to cloud.
     */
    suspend fun syncCloudHistory(): List<DownloadRecord> {
        val cloudRecords = firestoreSync.fetchCloudHistory()
        val localRecords = getHistory()

        val recordMap = mutableMapOf<Long, DownloadRecord>()
        cloudRecords.forEach { recordMap[it.downloadId] = it }
        localRecords.forEach { local ->
            val existing = recordMap[local.downloadId]
            if (existing == null) {
                recordMap[local.downloadId] = local
            } else {
                val bestFilePath = if (local.filePath.isNotEmpty()) local.filePath else existing.filePath
                val bestUrl = if (local.originalUrl.isNotEmpty()) local.originalUrl else existing.originalUrl
                recordMap[local.downloadId] = existing.copy(filePath = bestFilePath, originalUrl = bestUrl)
            }
        }

        val merged = recordMap.values.sortedByDescending { it.timestamp }
        saveAllRecords(merged)

        // Upload any local-only records to cloud
        merged.forEach { record ->
            firestoreSync.syncRecordToCloud(record)
        }

        return merged
    }

    /**
     * Resolves the original video URL for re-downloading.
     * Uses persisted originalUrl, or synthesizes a fallback for TikTok numeric IDs.
     */
    fun getResolvableVideoUrl(record: DownloadRecord): String {
        if (record.originalUrl.isNotEmpty()) return record.originalUrl
        if (record.author.startsWith("@") && record.id.isNotEmpty() && record.id.all { it.isDigit() }) {
            return "https://www.tiktok.com/${record.author}/video/${record.id}"
        }
        return ""
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
