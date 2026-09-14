package com.buwin.tiktokvideodownload.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.data.auth.AuthManager
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.ui.components.dialog.AppConfirmationModal
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidMenuItem
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidOptionsMenu
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidTopBar
import com.buwin.tiktokvideodownload.ui.screens.settings.components.SettingsAboutCard
import com.buwin.tiktokvideodownload.ui.screens.settings.components.SettingsAccountCard
import com.buwin.tiktokvideodownload.ui.screens.settings.components.SettingsAppearanceCard
import com.buwin.tiktokvideodownload.ui.screens.settings.components.SettingsSectionTitle
import com.buwin.tiktokvideodownload.ui.screens.settings.components.SettingsUtilityCard
import com.buwin.tiktokvideodownload.ui.screens.settings.viewmodel.SettingsViewModel
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles

/**
 * Minimalist Apple-style Settings Screen featuring Google Sign-In & Cloud Sync,
 * app preferences, and progressive blur header.
 *
 * Fully modularized following Android Clean Architecture & MVVM.
 */
@Composable
fun SettingsScreen(
    backdrop: Backdrop,
    themePreferences: ThemePreferences,
    downloadHelper: DownloadManagerHelper,
    authManager: AuthManager,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = remember {
        SettingsViewModel(
            authManager = authManager,
            downloadHelper = downloadHelper,
            themePreferences = themePreferences
        )
    }
) {
    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    // Card & element styling matching iOS Inset Grouped / Liquid design
    val cardBackground = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF0F0F2)
    val dividerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val chevronColor = if (isDark) Color(0xFF636366) else Color(0xFFC7C7CC)

    // Items for Theme Selection Modal
    val themeMenuItems = remember(themePreferences.currentThemeMode) {
        listOf(
            LiquidMenuItem(
                id = "system",
                title = "Theo hệ thống",
                subtitle = "Tự động chuyển sáng/tối theo máy",
                icon = CupertinoIcons.Outlined.Iphone,
                isSelected = themePreferences.currentThemeMode == AppThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) }
            ),
            LiquidMenuItem(
                id = "light",
                title = "Giao diện Sáng",
                subtitle = "Nền sáng ấm (#FAFAF9)",
                icon = CupertinoIcons.Outlined.Sparkles,
                isSelected = themePreferences.currentThemeMode == AppThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) }
            ),
            LiquidMenuItem(
                id = "dark",
                title = "Giao diện Tối",
                subtitle = "Đen OLED tinh tế (#000000)",
                icon = CupertinoIcons.Outlined.Paintpalette,
                isSelected = themePreferences.currentThemeMode == AppThemeMode.DARK,
                onClick = { viewModel.setThemeMode(AppThemeMode.DARK) }
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .layerBackdrop(contentBackdrop)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // Clearance for the pinned progressive blur header
            Spacer(modifier = Modifier.height(116.dp))

            // ── Section 0: Tài khoản & Đám mây ──
            SettingsSectionTitle(text = "Tài khoản & Đám mây")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsAccountCard(
                currentUser = uiState.currentUser,
                isSigningIn = uiState.isSigningIn,
                isSyncing = uiState.isSyncing,
                onSignInClick = { viewModel.signInWithGoogle() },
                onSyncClick = { viewModel.syncCloudHistory() },
                onSignOutClick = { viewModel.setShowSignOutConfirm(true) },
                cardBackground = cardBackground,
                cardBorderColor = cardBorderColor,
                dividerColor = dividerColor,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 1: Tiện ích ──
            SettingsSectionTitle(text = "Tiện ích")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsUtilityCard(
                autoPasteEnabled = uiState.autoPasteEnabled,
                onAutoPasteChange = { viewModel.setAutoPasteEnabled(it) },
                cardBackground = cardBackground,
                cardBorderColor = cardBorderColor,
                backdrop = backdrop
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 2: Giao diện & Dữ liệu ──
            SettingsSectionTitle(text = "Giao diện & Dữ liệu")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsAppearanceCard(
                currentThemeMode = themePreferences.currentThemeMode,
                onThemeClick = { viewModel.setShowThemeOptionsMenu(true) },
                onOpenDownloadsClick = { viewModel.openDownloadsFolder() },
                onClearHistoryClick = { viewModel.setShowClearHistoryConfirm(true) },
                cardBackground = cardBackground,
                cardBorderColor = cardBorderColor,
                dividerColor = dividerColor,
                chevronColor = chevronColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Section 3: Thông tin ──
            SettingsSectionTitle(text = "Thông tin")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsAboutCard(
                cardBackground = cardBackground,
                cardBorderColor = cardBorderColor,
                chevronColor = chevronColor
            )

            // Bottom clearance for floating Liquid Bottom Tabs
            Spacer(modifier = Modifier.height(120.dp))
        }

        // Shared Progressive Blur Header
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Cài đặt",
            subtitle = "Tùy chọn ứng dụng & giao diện",
            isDark = isDark
        )

        // Options Menu Dialog overlay (Theme selector)
        LiquidOptionsMenu(
            visible = uiState.showThemeOptionsMenu,
            onDismissRequest = { viewModel.setShowThemeOptionsMenu(false) },
            title = "Chọn giao diện",
            items = themeMenuItems,
            backdrop = backdrop
        )

        // Confirmation Modal for Sign Out
        AppConfirmationModal(
            visible = uiState.showSignOutConfirm,
            onDismissRequest = { viewModel.setShowSignOutConfirm(false) },
            title = "Đăng xuất tài khoản?",
            message = "Bạn sẽ không thể tự động đồng bộ lịch sử tải về lên đám mây sau khi đăng xuất. Dữ liệu trong máy vẫn được giữ nguyên.",
            confirmText = "Đăng xuất",
            cancelText = "Hủy",
            isDestructive = true,
            backdrop = backdrop,
            isDark = isDark,
            onConfirm = { viewModel.signOut() }
        )

        // Confirmation Modal for Clearing Download History
        AppConfirmationModal(
            visible = uiState.showClearHistoryConfirm,
            onDismissRequest = { viewModel.setShowClearHistoryConfirm(false) },
            title = "Xóa toàn bộ lịch sử?",
            message = "Tất cả các bản ghi tải về trong danh sách lịch sử và trên đám mây sẽ bị xóa. Các tệp đã tải trong máy vẫn được giữ nguyên.",
            confirmText = "Xóa lịch sử",
            cancelText = "Hủy",
            isDestructive = true,
            backdrop = backdrop,
            isDark = isDark,
            onConfirm = { viewModel.clearHistory() }
        )
    }
}
