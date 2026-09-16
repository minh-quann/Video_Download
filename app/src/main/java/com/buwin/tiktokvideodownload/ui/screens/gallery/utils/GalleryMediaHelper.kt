package com.buwin.tiktokvideodownload.ui.screens.gallery.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.buwin.tiktokvideodownload.ui.screens.gallery.model.GalleryMediaItem
import java.io.File

object GalleryMediaHelper {

    /**
     * Extracts video duration in milliseconds via MediaMetadataRetriever.
     */
    fun extractVideoDuration(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    /**
     * Queries display file name from content resolver cursor.
     */
    fun queryFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Shares media URI via system ACTION_SEND chooser.
     */
    fun shareMedia(context: Context, uri: Uri, isVideo: Boolean) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Chia sẻ với"))
        } catch (e: Exception) {
            AppToast.showError("Không thể chia sẻ", e.localizedMessage ?: "Lỗi hệ thống")
        }
    }

    /**
     * Queries the system MediaStore and download cache to load the device's
     * complete photo and video library (Bộ sưu tập).
     */
    fun loadAllDeviceMedia(
        context: Context,
        downloadHelper: DownloadManagerHelper
    ): List<GalleryMediaItem> {
        val result = mutableListOf<GalleryMediaItem>()
        val seenUriStrings = mutableSetOf<String>()

        // 1. Load from app Download history
        val history = downloadHelper.getHistory()
        val historyMap = mutableMapOf<String, DownloadRecord>()
        for (record in history) {
            val isAudio = record.fileExtension.equals("m4a", true) ||
                    record.fileExtension.equals("mp3", true)
            if (isAudio) continue

            val uri = downloadHelper.getDownloadedUri(record) ?: continue
            val uriStr = uri.toString()
            seenUriStrings.add(uriStr)
            historyMap[uriStr] = record

            val isEdited = record.title.contains("edited", ignoreCase = true) ||
                    record.filePath.contains("edited", ignoreCase = true)

            val duration = extractVideoDuration(context, uri)

            result.add(
                GalleryMediaItem(
                    id = record.id.ifEmpty { "hist_${record.downloadId}" },
                    title = record.title.ifEmpty { "Video tải về" },
                    uri = uri,
                    isVideo = true,
                    durationMs = duration,
                    timestamp = record.timestamp,
                    sourceRecord = record,
                    isEdited = isEdited,
                    isDownloaded = true,
                    isFromDevice = false
                )
            )
        }

        // 2. Query MediaStore for all system Videos on the device
        try {
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA
            )
            val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                videoUri,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(videoUri, id)
                    val uriStr = contentUri.toString()
                    val rawName = cursor.getString(nameCol) ?: "Video"
                    val dateSec = cursor.getLong(dateCol)
                    val duration = cursor.getLong(durCol)
                    val size = cursor.getLong(sizeCol)
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""

                    if (seenUriStrings.add(uriStr)) {
                        val isEdited = rawName.contains("edited", true) || path.contains("edited", true)
                        val isDownloaded = path.contains("TikTok_Downloader", true) || historyMap.containsKey(uriStr)

                        result.add(
                            GalleryMediaItem(
                                id = "sys_vid_$id",
                                title = rawName.substringBeforeLast("."),
                                uri = contentUri,
                                isVideo = true,
                                durationMs = duration,
                                sizeBytes = size,
                                timestamp = if (dateSec > 0) dateSec * 1000L else System.currentTimeMillis(),
                                isEdited = isEdited,
                                isDownloaded = isDownloaded,
                                isFromDevice = !isDownloaded
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {}

        // 3. Query MediaStore for all system Images / Photos on the device
        try {
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATA
            )
            val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                imageUri,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(imageUri, id)
                    val uriStr = contentUri.toString()
                    val rawName = cursor.getString(nameCol) ?: "Ảnh"
                    val dateSec = cursor.getLong(dateCol)
                    val size = cursor.getLong(sizeCol)
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""

                    if (seenUriStrings.add(uriStr)) {
                        val isEdited = rawName.contains("edited", true) || path.contains("edited", true)
                        val isDownloaded = path.contains("TikTok_Downloader", true)

                        result.add(
                            GalleryMediaItem(
                                id = "sys_img_$id",
                                title = rawName.substringBeforeLast("."),
                                uri = contentUri,
                                isVideo = false,
                                durationMs = 0L,
                                sizeBytes = size,
                                timestamp = if (dateSec > 0) dateSec * 1000L else System.currentTimeMillis(),
                                isEdited = isEdited,
                                isDownloaded = isDownloaded,
                                isFromDevice = !isDownloaded
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {}

        // 4. Scan TikTok_Downloader public Downloads directory for any unindexed files
        try {
            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "TikTok_Downloader"
            )
            if (downloadDir.exists() && downloadDir.isDirectory) {
                val files = downloadDir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.isFile) {
                        val ext = file.extension.lowercase()
                        val isVid = ext in listOf("mp4", "mov", "mkv", "webm")
                        val isImg = ext in listOf("jpg", "jpeg", "png", "webp")
                        if (isVid || isImg) {
                            val fileUri = Uri.fromFile(file)
                            if (seenUriStrings.add(fileUri.toString())) {
                                val isEdited = file.name.contains("edited", ignoreCase = true)
                                val duration = if (isVid) extractVideoDuration(context, fileUri) else 0L
                                result.add(
                                    GalleryMediaItem(
                                        id = "file_${file.lastModified()}",
                                        title = file.nameWithoutExtension,
                                        uri = fileUri,
                                        isVideo = isVid,
                                        durationMs = duration,
                                        sizeBytes = file.length(),
                                        timestamp = file.lastModified(),
                                        isEdited = isEdited,
                                        isDownloaded = true,
                                        isFromDevice = false
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}

        return result.sortedByDescending { it.timestamp }
    }
}
