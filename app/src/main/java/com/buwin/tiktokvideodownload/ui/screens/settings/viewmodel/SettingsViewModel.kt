package com.buwin.tiktokvideodownload.ui.screens.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buwin.tiktokvideodownload.data.auth.AuthManager
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Settings screen.
 */
data class SettingsUiState(
    val currentUser: FirebaseUser? = null,
    val isSigningIn: Boolean = false,
    val isSyncing: Boolean = false,
    val showSignOutConfirm: Boolean = false,
    val showClearHistoryConfirm: Boolean = false,
    val showThemeOptionsMenu: Boolean = false,
    val isThemeScreenOpen: Boolean = false,
    val autoPasteEnabled: Boolean = true
)

/**
 * ViewModel managing settings state, authentication actions,
 * cloud sync trigger, and app preferences.
 */
class SettingsViewModel(
    private val authManager: AuthManager,
    private val downloadHelper: DownloadManagerHelper,
    val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Observe current Firebase user state changes
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    /**
     * Initiates Google Credential Manager sign-in flow.
     */
    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true) }
            val result = authManager.signInWithGoogle()
            _uiState.update { it.copy(isSigningIn = false) }

            result.fold(
                onSuccess = { user ->
                    AppToast.showSuccess(
                        "Đăng nhập thành công",
                        "Xin chào ${user.displayName ?: ""}"
                    )
                    // Trigger sync right after sign-in
                    syncCloudHistory(showToast = false)
                },
                onFailure = { error ->
                    if (error !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                        AppToast.showError(
                            "Đăng nhập thất bại",
                            error.localizedMessage ?: "Vui lòng thử lại"
                        )
                    }
                }
            )
        }
    }

    /**
     * Signs out the user and dismisses confirmation modal.
     */
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _uiState.update { it.copy(showSignOutConfirm = false) }
            AppToast.showSuccess("Đã đăng xuất", "Tài khoản Google đã đăng xuất")
        }
    }

    /**
     * Manually triggers two-way sync with Cloud Firestore.
     */
    fun syncCloudHistory(showToast: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                downloadHelper.syncCloudHistory()
                _uiState.update { it.copy(isSyncing = false) }
                if (showToast) {
                    AppToast.showSuccess("Đã đồng bộ", "Lịch sử đã được cập nhật từ đám mây")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false) }
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
     * Clears all local download records and cloud history.
     */
    fun clearHistory() {
        downloadHelper.clearHistory()
        _uiState.update { it.copy(showClearHistoryConfirm = false) }
        AppToast.showSuccess("Đã xóa lịch sử", "Toàn bộ lịch sử tải về đã được dọn sạch")
    }

    /**
     * Opens system Downloads directory.
     */
    fun openDownloadsFolder() {
        downloadHelper.openDownloadsFolder()
    }

    /**
     * Updates theme mode in ThemePreferences.
     */
    fun setThemeMode(mode: AppThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun setAutoPasteEnabled(enabled: Boolean) {
        _uiState.update { it.copy(autoPasteEnabled = enabled) }
    }

    fun setShowSignOutConfirm(show: Boolean) {
        _uiState.update { it.copy(showSignOutConfirm = show) }
    }

    fun setShowClearHistoryConfirm(show: Boolean) {
        _uiState.update { it.copy(showClearHistoryConfirm = show) }
    }

    fun setShowThemeOptionsMenu(show: Boolean) {
        _uiState.update { it.copy(showThemeOptionsMenu = show) }
    }

    fun openThemeScreen() {
        _uiState.update { it.copy(isThemeScreenOpen = true) }
    }

    fun closeThemeScreen() {
        _uiState.update { it.copy(isThemeScreenOpen = false) }
    }
}
