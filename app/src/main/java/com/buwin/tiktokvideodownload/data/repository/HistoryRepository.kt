package com.buwin.tiktokvideodownload.data.repository

import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.data.sync.FirestoreSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository managing download history records across local storage and Cloud Firestore.
 * Decouples the UI layer from underlying database, networking, and preference implementations.
 */
class HistoryRepository(
    private val downloadHelper: DownloadManagerHelper,
    private val firestoreSync: FirestoreSyncManager = FirestoreSyncManager()
) {

    /**
     * Retrieves local cached history records.
     */
    fun getLocalHistory(): List<DownloadRecord> {
        return downloadHelper.getHistory()
    }

    /**
     * Synchronizes local records with Cloud Firestore two-way, resolving conflicts and updating storage.
     */
    suspend fun syncWithCloud(): List<DownloadRecord> = withContext(Dispatchers.IO) {
        downloadHelper.syncCloudHistory()
    }

    /**
     * Clears all history records both locally and in Cloud Firestore.
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        downloadHelper.clearHistory()
    }

    /**
     * Opens the system Downloads directory intent.
     */
    fun openDownloadsFolder() {
        downloadHelper.openDownloadsFolder()
    }
}
