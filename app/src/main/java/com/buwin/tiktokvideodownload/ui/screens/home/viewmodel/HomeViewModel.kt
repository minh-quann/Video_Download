package com.buwin.tiktokvideodownload.ui.screens.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadOption
import com.buwin.tiktokvideodownload.data.model.TikTokVideoInfo
import com.buwin.tiktokvideodownload.data.service.TikTokService
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Home / Downloader screen.
 */
data class HomeUiState(
    val inputUrl: String = "",
    val isLoading: Boolean = false,
    val videoInfo: TikTokVideoInfo? = null,
    val detectedClipboardUrl: String? = null
)

/**
 * ViewModel managing link extraction, video info fetching,
 * clipboard auto-detection, and download enqueuing.
 */
class HomeViewModel(
    private val tiktokService: TikTokService,
    private val downloadHelper: DownloadManagerHelper,
    val themePreferences: ThemePreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Updates current input text in the search bar.
     */
    fun onInputUrlChange(newUrl: String) {
        _uiState.update { it.copy(inputUrl = newUrl) }
    }

    /**
     * Inspects clipboard content on startup and detects if it's a valid media URL.
     */
    fun checkClipboard(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) return
        try {
            val extracted = tiktokService.extractUrl(clipboardText)
            if (!extracted.isNullOrBlank()) {
                _uiState.update { it.copy(detectedClipboardUrl = extracted) }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Handles link pasted directly by user from clipboard.
     */
    fun onPasteClipboard(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) {
            AppToast.showInfo("Bộ nhớ tạm rỗng", "Vui lòng sao chép link trước khi dán")
            return
        }
        val extracted = tiktokService.extractUrl(clipboardText) ?: clipboardText
        _uiState.update { it.copy(inputUrl = extracted) }
        processUrl(extracted)
    }

    /**
     * Handles external shared URL from Android Intent.
     */
    fun handleSharedUrl(sharedUrl: String?) {
        if (!sharedUrl.isNullOrBlank() && sharedUrl != _uiState.value.inputUrl) {
            _uiState.update { it.copy(inputUrl = sharedUrl) }
            processUrl(sharedUrl)
        }
    }

    /**
     * Fetches video info for the given URL.
     */
    fun processUrl(urlToFetch: String = _uiState.value.inputUrl) {
        val target = urlToFetch.trim()
        if (target.isEmpty()) {
            AppToast.showError("Chưa nhập liên kết", "Vui lòng nhập hoặc dán link TikTok hoặc Facebook")
            return
        }

        _uiState.update { it.copy(isLoading = true, videoInfo = null) }

        viewModelScope.launch {
            val result = tiktokService.fetchVideoInfo(target)
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess { info ->
                _uiState.update { it.copy(videoInfo = info) }
            }.onFailure { error ->
                AppToast.showError("Không thể bóc tách", error.message ?: "Vui lòng kiểm tra lại đường link")
            }
        }
    }

    /**
     * Enqueues download for selected format/option.
     */
    fun enqueueDownload(option: DownloadOption) {
        val currentInfo = _uiState.value.videoInfo ?: return
        downloadHelper.enqueueDownload(currentInfo, option)
    }

    /**
     * Toggles light / dark theme.
     */
    fun toggleTheme(isDark: Boolean) {
        val next = when (themePreferences.currentThemeMode) {
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.SYSTEM -> if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        }
        themePreferences.setThemeMode(next)
    }
}
