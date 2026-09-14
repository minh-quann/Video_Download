package com.buwin.tiktokvideodownload.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidMenuItem
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidOptionsMenu
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidToggle
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidTopBar
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronForward
import io.github.alexzhirkevich.cupertino.icons.outlined.Folder
import io.github.alexzhirkevich.cupertino.icons.outlined.InfoCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash

/**
 * Minimalist Apple-style Settings Screen featuring the shared progressive blur header.
 */
@Composable
fun SettingsScreen(
    backdrop: Backdrop,
    themePreferences: ThemePreferences,
    downloadHelper: DownloadManagerHelper,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()

    // State for Options Menu & Auto-paste toggle
    var showThemeOptionsMenu by remember { mutableStateOf(false) }
    var autoPasteEnabled by remember { mutableStateOf(true) }

    // Card styling
    val cardBackground = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF0F0F2)
    val dividerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val chevronColor = if (isDark) Color(0xFF636366) else Color(0xFFC7C7CC)

    // Items for Liquid Options Menu (Theme selection)
    val themeMenuItems = remember(themePreferences.currentThemeMode) {
        listOf(
            LiquidMenuItem(
                id = "system",
                title = "Theo hệ thống",
                subtitle = "Tự động chuyển sáng/tối theo máy",
                icon = CupertinoIcons.Outlined.Iphone,
                isSelected = themePreferences.currentThemeMode == AppThemeMode.SYSTEM,
                onClick = { themePreferences.setThemeMode(AppThemeMode.SYSTEM) }
            ),
            LiquidMenuItem(
                id = "light",
                title = "Giao diện Sáng",
                subtitle = "Nền sáng ấm (#FAFAF9)",
                icon = CupertinoIcons.Outlined.Sparkles,
                isSelected = themePreferences.currentThemeMode == AppThemeMode.LIGHT,
                onClick = { themePreferences.setThemeMode(AppThemeMode.LIGHT) }
            ),
            LiquidMenuItem(
                id = "dark",
                title = "Giao diện Tối",
                subtitle = "Đen OLED tinh tế (#000000)",
                icon = CupertinoIcons.Outlined.Paintpalette,
                isSelected = themePreferences.currentThemeMode == AppThemeMode.DARK,
                onClick = { themePreferences.setThemeMode(AppThemeMode.DARK) }
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

            // ------------------------------------------
            // SECTION 1: TIỆN ÍCH
            // ------------------------------------------
            SectionTitle(text = "Tiện ích")

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                autoPasteEnabled = !autoPasteEnabled
                            }
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.Sparkles,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Tự động nhận diện link",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LiquidToggle(
                        checked = autoPasteEnabled,
                        onCheckedChange = { autoPasteEnabled = it },
                        backdrop = backdrop
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ------------------------------------------
            // SECTION 2: GIAO DIỆN
            // ------------------------------------------
            SectionTitle(text = "Giao diện")

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                SimpleSettingsRow(
                    icon = CupertinoIcons.Outlined.Paintpalette,
                    title = "Chủ đề giao diện",
                    trailingText = when (themePreferences.currentThemeMode) {
                        AppThemeMode.SYSTEM -> "Theo hệ thống"
                        AppThemeMode.LIGHT -> "Chế độ Sáng"
                        AppThemeMode.DARK -> "Chế độ Tối"
                    },
                    chevronColor = chevronColor,
                    onClick = { showThemeOptionsMenu = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ------------------------------------------
            // SECTION 3: BỘ NHỚ & TỆP TIN
            // ------------------------------------------
            SectionTitle(text = "Bộ nhớ & Tệp tin")

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column {
                    SimpleSettingsRow(
                        icon = CupertinoIcons.Outlined.Folder,
                        title = "Thư mục lưu trữ",
                        trailingText = "Downloads",
                        chevronColor = chevronColor,
                        onClick = { downloadHelper.openDownloadsFolder() }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                        color = dividerColor,
                        thickness = 0.8.dp
                    )

                    SimpleSettingsRow(
                        icon = CupertinoIcons.Outlined.Trash,
                        title = "Xóa lịch sử tải về",
                        chevronColor = chevronColor,
                        onClick = {
                            downloadHelper.clearHistory()
                            AppToast.showSuccess("Đã xóa lịch sử", "Toàn bộ lịch sử tải về đã được xóa sạch")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ------------------------------------------
            // SECTION 4: THÔNG TIN ỨNG DỤNG
            // ------------------------------------------
            SectionTitle(text = "Thông tin")

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column {
                    SimpleSettingsRow(
                        icon = CupertinoIcons.Outlined.InfoCircle,
                        title = "Phiên bản",
                        trailingText = "1.0.0",
                        chevronColor = chevronColor,
                        showChevron = false,
                        onClick = {}
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                        color = dividerColor,
                        thickness = 0.8.dp
                    )

                    SimpleSettingsRow(
                        icon = CupertinoIcons.Outlined.Sparkles,
                        title = "Công nghệ giao diện",
                        trailingText = "Apple Liquid Glass",
                        chevronColor = chevronColor,
                        showChevron = false,
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }

        // Shared Progressive Blur Header (Mờ dần, no icon)
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Cài đặt",
            subtitle = "Tùy chọn ứng dụng & giao diện",
            isDark = isDark
        )

        // Options Menu Dialog overlay
        LiquidOptionsMenu(
            visible = showThemeOptionsMenu,
            onDismissRequest = { showThemeOptionsMenu = false },
            title = "Chọn giao diện",
            items = themeMenuItems,
            backdrop = backdrop
        )
    }
}

/**
 * Minimalist section title matching iOS settings style.
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 4.dp)
    )
}

/**
 * Clean, single settings row with monochrome icon and trailing chevron.
 */
@Composable
private fun SimpleSettingsRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    chevronColor: Color,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.ChevronForward,
                    contentDescription = null,
                    tint = chevronColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
