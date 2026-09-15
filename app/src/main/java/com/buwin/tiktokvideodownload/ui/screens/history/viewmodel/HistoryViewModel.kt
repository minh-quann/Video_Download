package com.buwin.tiktokvideodownload.ui.screens.history.viewmodel

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buwin.tiktokvideodownload.data.auth.AuthManager
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.data.repository.HistoryRepository
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.buwin.tiktokvideodownload.ui.screens.history.model.HistoryDateGroup
import com.buwin.tiktokvideodownload.ui.screens.history.model.HistoryGroupUtils
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State representation for the History screen.
 */
data class HistoryUiState(
    val records: List<DownloadRecord> = emptyList(),
    val groups: List<HistoryDateGroup> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val currentUser: FirebaseUser? = null
)

/**
 * ViewModel managing download history, cloud synchronization, and authentication callbacks.
 */
class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // Listen to authentication changes and auto-sync
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(currentUser = user)
                if (user != null) {
                    syncWithCloud(showToast = false)
                } else {
                    loadLocalHistory()
                }
            }
        }
    }

    /**
     * Loads local records from storage and groups them by date.
     */
    fun loadLocalHistory() {
        val local = historyRepository.getLocalHistory()
        val groups = HistoryGroupUtils.groupByDate(local)
        _uiState.value = _uiState.value.copy(
            records = local,
            groups = groups,
            isLoading = false
        )
    }

    /**
     * Synchronizes records with Cloud Firestore.
     */
    fun syncWithCloud(showToast: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            try {
                val merged = historyRepository.syncWithCloud()
                val groups = HistoryGroupUtils.groupByDate(merged)
                _uiState.value = _uiState.value.copy(
                    records = merged,
                    groups = groups,
                    isSyncing = false,
                    isLoading = false
                )
                if (showToast) {
                    AppToast.showSuccess("Đã đồng bộ", "Lịch sử đã được cập nhật từ đám mây")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSyncing = false)
                if (showToast) {
                    val message = if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                        "Quyền Firestore bị chặn. Hãy cập nhật Rules trên Firebase Console."
                    } else {
                        e.localizedMessage ?: "Không thể đồng bộ dữ liệu"
                    }
                    AppToast.showError("Lỗi đồng bộ", message)
                }
            }
        }
    }

    /**
     * Triggers Google One Tap sign-in and syncs cloud history upon success.
     */
    fun signInWithGoogle() {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle()
            result.fold(
                onSuccess = { user ->
                    AppToast.showSuccess("Đăng nhập thành công", "Xin chào ${user.displayName ?: ""}")
                    syncWithCloud(showToast = true)
                },
                onFailure = { error ->
                    if (error !is GetCredentialCancellationException) {
                        AppToast.showError("Đăng nhập thất bại", error.localizedMessage ?: "Vui lòng thử lại")
                    }
                }
            )
        }
    }

    /**
     * Clears history records both locally and on Cloud Firestore.
     */
    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            _uiState.value = _uiState.value.copy(records = emptyList(), groups = emptyList())
            AppToast.showSuccess("Đã xóa lịch sử", "Toàn bộ lịch sử tải về đã được dọn sạch")
        }
    }

    /**
     * Opens system Downloads directory.
     */
    fun openDownloadsFolder() {
        historyRepository.openDownloadsFolder()
    }
}
