package com.buwin.tiktokvideodownload.ui.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidToggle
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.kyant.backdrop.Backdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Folder
import io.github.alexzhirkevich.cupertino.icons.outlined.InfoCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles
import io.github.alexzhirkevich.cupertino.icons.outlined.Trash

/**
 * Utility card containing the auto-paste link toggle.
 */
@Composable
fun SettingsUtilityCard(
    autoPasteEnabled: Boolean,
    onAutoPasteChange: (Boolean) -> Unit,
    cardBackground: Color,
    cardBorderColor: Color,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                        onAutoPasteChange(!autoPasteEnabled)
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
                onCheckedChange = onAutoPasteChange,
                backdrop = backdrop
            )
        }
    }
}

/**
 * Appearance and Data management card (Theme, Downloads Folder, Clear History).
 */
@Composable
fun SettingsAppearanceCard(
    currentThemeMode: AppThemeMode,
    onThemeClick: () -> Unit,
    onOpenDownloadsClick: () -> Unit,
    onClearHistoryClick: () -> Unit,
    cardBackground: Color,
    cardBorderColor: Color,
    dividerColor: Color,
    chevronColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val currentThemeTitle = when (currentThemeMode) {
                AppThemeMode.SYSTEM -> "Theo hệ thống"
                AppThemeMode.LIGHT -> "Sáng"
                AppThemeMode.DARK -> "Tối"
            }

            SettingsRowItem(
                icon = CupertinoIcons.Outlined.Paintpalette,
                title = "Giao diện ứng dụng",
                trailingText = currentThemeTitle,
                chevronColor = chevronColor,
                onClick = onThemeClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                thickness = 0.6.dp,
                color = dividerColor
            )

            SettingsRowItem(
                icon = CupertinoIcons.Outlined.Folder,
                title = "Thư mục tải xuống",
                trailingText = "Download/",
                chevronColor = chevronColor,
                onClick = onOpenDownloadsClick
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                thickness = 0.6.dp,
                color = dividerColor
            )

            SettingsRowItem(
                icon = CupertinoIcons.Outlined.Trash,
                title = "Xóa lịch sử tải về",
                chevronColor = chevronColor,
                onClick = onClearHistoryClick
            )
        }
    }
}

/**
 * App information card showing version number.
 */
@Composable
fun SettingsAboutCard(
    cardBackground: Color,
    cardBorderColor: Color,
    chevronColor: Color,
    modifier: Modifier = Modifier,
    appVersion: String = com.buwin.tiktokvideodownload.data.config.AppConfig.APP_VERSION_DISPLAY
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        SettingsRowItem(
            icon = CupertinoIcons.Outlined.InfoCircle,
            title = "Phiên bản",
            trailingText = appVersion,
            chevronColor = chevronColor,
            showChevron = false,
            onClick = {}
        )
    }
}
