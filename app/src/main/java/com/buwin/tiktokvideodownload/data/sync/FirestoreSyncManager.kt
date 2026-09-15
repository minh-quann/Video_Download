package com.buwin.tiktokvideodownload.data.sync

import android.util.Log
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Syncs download history with Cloud Firestore under users/{userId}/history/{downloadId}.
 */
class FirestoreSyncManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "FirestoreSyncManager"
    }

    /**
     * Uploads or updates a download record to Firestore.
     */
    suspend fun syncRecordToCloud(record: DownloadRecord) {
        val uid = auth.currentUser?.uid ?: run {
            Log.d(TAG, "No logged-in user, skipping syncRecordToCloud")
            return
        }
        try {
            val data = hashMapOf(
                "id" to record.id,
                "title" to record.title,
                "author" to record.author,
                "coverUrl" to record.coverUrl,
                "formatTitle" to record.formatTitle,
                "fileExtension" to record.fileExtension,
                "downloadId" to record.downloadId,
                "timestamp" to record.timestamp,
                "filePath" to record.filePath,
                "originalUrl" to record.originalUrl
            )
            firestore.collection("users")
                .document(uid)
                .collection("history")
                .document(record.downloadId.toString())
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully synced record ${record.downloadId} to cloud for user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync record ${record.downloadId} to cloud: ${e.message}", e)
            throw e
        }
    }

    /**
     * Deletes a record from Cloud Firestore.
     */
    suspend fun deleteRecordFromCloud(downloadId: Long) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.collection("users")
                .document(uid)
                .collection("history")
                .document(downloadId.toString())
                .delete()
                .await()
            Log.d(TAG, "Deleted record $downloadId from cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete record $downloadId from cloud: ${e.message}", e)
            throw e
        }
    }

    /**
     * Clears all history records from Cloud Firestore for the logged-in user.
     */
    suspend fun clearCloudHistory() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("history")
                .get()
                .await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Log.d(TAG, "Successfully cleared all cloud history for user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cloud history: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fetches all cloud history records for the logged-in user.
     */
    suspend fun fetchCloudHistory(): List<DownloadRecord> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: ""
                val title = doc.getString("title") ?: ""
                val author = doc.getString("author") ?: ""
                val coverUrl = doc.getString("coverUrl") ?: ""
                val formatTitle = doc.getString("formatTitle") ?: ""
                val fileExtension = doc.getString("fileExtension") ?: ""
                val downloadId = doc.getLong("downloadId") ?: 0L
                val timestamp = doc.getLong("timestamp") ?: 0L
                val filePath = doc.getString("filePath") ?: ""
                val originalUrl = doc.getString("originalUrl") ?: ""

                if (id.isNotEmpty() || title.isNotEmpty()) {
                    DownloadRecord(
                        id = id,
                        title = title,
                        author = author,
                        coverUrl = coverUrl,
                        formatTitle = formatTitle,
                        fileExtension = fileExtension,
                        downloadId = downloadId,
                        timestamp = timestamp,
                        filePath = filePath,
                        originalUrl = originalUrl
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch cloud history: ${e.message}", e)
            throw e
        }
    }
}
