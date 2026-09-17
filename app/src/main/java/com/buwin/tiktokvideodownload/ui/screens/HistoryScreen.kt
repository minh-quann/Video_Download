package com.buwin.tiktokvideodownload.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.data.auth.AuthManager
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.data.repository.HistoryRepository
import com.buwin.tiktokvideodownload.ui.components.dialog.AppConfirmationModal
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidTopBar
import com.buwin.tiktokvideodownload.ui.screens.history.components.HistoryCloudBanner
import com.buwin.tiktokvideodownload.ui.screens.history.components.HistoryDateGroupCard
import com.buwin.tiktokvideodownload.ui.screens.history.components.HistoryEmptyState
import com.buwin.tiktokvideodownload.ui.screens.history.components.HistorySkeleton
import com.buwin.tiktokvideodownload.ui.screens.history.viewmodel.HistoryViewModel
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.buwin.tiktokvideodownload.ui.theme.cardBorderColor
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.res.painterResource
import com.buwin.tiktokvideodownload.R
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Folder
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowClockwise

/**
 * Screen displaying the history of downloaded TikTok and Facebook media files,
 * refactored into Clean Architecture with separated components, repository, and ViewModel.
 */
@Composable
fun HistoryScreen(
    backdrop: Backdrop,
    downloadHelper: DownloadManagerHelper,
    authManager: AuthManager,
    onPlayRecord: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = remember {
        HistoryViewModel(
            historyRepository = HistoryRepository(downloadHelper),
            authManager = authManager
        )
    }
) {
    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    // Refresh history when screen becomes active
    LaunchedEffect(Unit) {
        viewModel.loadLocalHistory()
    }

    // Card styling matching iOS Inset Grouped / Liquid design
    val cardBackground = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorderColor = cardBorderColor(isDark)
    val dividerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            // Shimmer skeleton state while records are loading
            HistorySkeleton(
                contentBackdrop = contentBackdrop,
                cardBackground = cardBackground,
                cardBorderColor = cardBorderColor,
                dividerColor = dividerColor,
                isDark = isDark
            )
        } else if (uiState.records.isEmpty()) {
            HistoryEmptyState(
                contentBackdrop = contentBackdrop,
                showCloudPrompt = uiState.currentUser == null,
                onSignInClick = { viewModel.signInWithGoogle() },
                isDark = isDark
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .layerBackdrop(contentBackdrop)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "header_clearance") {
                    Spacer(modifier = Modifier.height(116.dp))
                }

                // Cloud backup banner when not signed in
                if (uiState.currentUser == null) {
                    item(key = "cloud_sync_banner") {
                        HistoryCloudBanner(
                            onClickSignIn = { viewModel.signInWithGoogle() },
                            isDark = isDark
                        )
                    }
                }

                // Grouped download cards by date
                items(uiState.groups, key = { it.dateKey }) { group ->
                    HistoryDateGroupCard(
                        group = group,
                        backdrop = backdrop,
                        onOpenRecord = onPlayRecord,
                        isDark = isDark,
                        cardBackground = cardBackground,
                        cardBorderColor = cardBorderColor,
                        dividerColor = dividerColor
                    )
                }

                item(key = "footer_clearance") {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }

        // Shared Progressive Blur Header
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Lịch sử tải về",
            subtitle = if (uiState.currentUser != null) {
                "${uiState.records.size} tệp • Đã đồng bộ"
            } else {
                "${uiState.records.size} tệp đã lưu"
            },
            isDark = isDark,
            actions = {
                // Cloud Sync Button when logged in
                if (uiState.currentUser != null) {
                    LiquidRoundButton(
                        onClick = { viewModel.syncWithCloud(showToast = true) },
                        backdrop = contentBackdrop,
                        size = 40.dp,
                        isDark = isDark
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.ArrowClockwise,
                                contentDescription = "Sync Cloud",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Open Downloads Folder Button
                LiquidRoundButton(
                    onClick = { viewModel.openDownloadsFolder() },
                    backdrop = contentBackdrop,
                    size = 40.dp,
                    isDark = isDark
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.Folder,
                        contentDescription = "Open Folder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear History Button with modern Solar Minimalistic Trash Icon
                if (uiState.records.isNotEmpty()) {
                    LiquidRoundButton(
                        onClick = { showClearConfirm = true },
                        backdrop = contentBackdrop,
                        size = 40.dp,
                        isDark = isDark
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash_minimal),
                            contentDescription = "Clear History",
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )

        // Confirmation Modal before clearing all download history
        AppConfirmationModal(
            visible = showClearConfirm,
            onDismissRequest = { showClearConfirm = false },
            title = "Xóa toàn bộ lịch sử?",
            message = "Tất cả các bản ghi tải về trong danh sách lịch sử và trên đám mây sẽ bị xóa. Các tệp đã tải trong máy vẫn được giữ nguyên.",
            confirmText = "Xóa lịch sử",
            cancelText = "Hủy",
            isDestructive = true,
            backdrop = backdrop,
            isDark = isDark,
            onConfirm = {
                showClearConfirm = false
                viewModel.clearHistory()
            }
        )
    }
}
