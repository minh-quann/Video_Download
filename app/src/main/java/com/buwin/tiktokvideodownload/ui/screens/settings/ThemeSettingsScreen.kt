package com.buwin.tiktokvideodownload.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidTopBar
import com.buwin.tiktokvideodownload.ui.screens.settings.components.SettingsSectionTitle
import com.buwin.tiktokvideodownload.ui.theme.AppThemeMode
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.buwin.tiktokvideodownload.ui.theme.ThemePreferences
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.CheckmarkCircle
import io.github.alexzhirkevich.cupertino.icons.outlined.Iphone
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles

/**
 * Dedicated Apple-style Theme & Appearance settings screen.
 * Replaces the popup modal with a full page featuring interactive theme previews and options.
 */
@Composable
fun ThemeSettingsScreen(
    themePreferences: ThemePreferences,
    onBack: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    // Intercept hardware and gesture back
    BackHandler {
        onBack()
    }

    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()

    val currentMode = themePreferences.currentThemeMode

    val cardBackground = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF0F0F2)
    val dividerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .layerBackdrop(contentBackdrop)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // Spacer to clear the progressive blur header
            Spacer(modifier = Modifier.height(116.dp))

            // ── Section 1: Visual Theme Mockup Previews (iOS Display & Brightness style) ──
            SettingsSectionTitle(text = "Xem trước giao diện")
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Light Mode Preview Card
                ThemeMockupCard(
                    title = "Sáng",
                    isMockupDark = false,
                    isSelected = currentMode == AppThemeMode.LIGHT,
                    onClick = { themePreferences.setThemeMode(AppThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                    isCurrentAppDark = isDark
                )

                // Dark Mode Preview Card
                ThemeMockupCard(
                    title = "Tối",
                    isMockupDark = true,
                    isSelected = currentMode == AppThemeMode.DARK,
                    onClick = { themePreferences.setThemeMode(AppThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                    isCurrentAppDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ── Section 2: Detailed Selection List ──
            SettingsSectionTitle(text = "Chế độ hiển thị")
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                border = BorderStroke(1.dp, cardBorderColor)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Option 1: System Default
                    ThemeOptionRow(
                        icon = CupertinoIcons.Outlined.Iphone,
                        title = "Theo hệ thống",
                        subtitle = "Tự động đổi sáng/tối theo máy",
                        isSelected = currentMode == AppThemeMode.SYSTEM,
                        onClick = { themePreferences.setThemeMode(AppThemeMode.SYSTEM) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = dividerColor
                    )

                    // Option 2: Light Mode
                    ThemeOptionRow(
                        icon = CupertinoIcons.Outlined.Sparkles,
                        title = "Giao diện Sáng",
                        subtitle = "Nền sáng ấm (#FAFAF9), thanh lịch",
                        isSelected = currentMode == AppThemeMode.LIGHT,
                        onClick = { themePreferences.setThemeMode(AppThemeMode.LIGHT) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = dividerColor
                    )

                    // Option 3: Dark Mode
                    ThemeOptionRow(
                        icon = CupertinoIcons.Outlined.Paintpalette,
                        title = "Giao diện Tối",
                        subtitle = "Đen OLED thuần khiết (#000000), tiết kiệm pin",
                        isSelected = currentMode == AppThemeMode.DARK,
                        onClick = { themePreferences.setThemeMode(AppThemeMode.DARK) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Khi chọn 'Theo hệ thống', ứng dụng sẽ tự động điều chỉnh màu sắc giao diện khớp với chế độ của thiết bị của bạn.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            // Clearance for bottom floating navigation tabs
            Spacer(modifier = Modifier.height(130.dp))
        }

        // Shared Progressive Blur Header with Back Button
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Giao diện",
            subtitle = "Tùy chỉnh chế độ hiển thị sáng / tối",
            isDark = isDark,
            navigationIcon = {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.70f),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )
    }
}

/**
 * Visual Mockup Card showcasing what the light/dark interface looks like.
 */
@Composable
private fun ThemeMockupCard(
    title: String,
    isMockupDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentAppDark: Boolean
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isCurrentAppDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMockupDark) Color(0xFF0F0F11) else Color(0xFFF3F4F6)
            ),
            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                // Mockup TopBar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isMockupDark) Color(0xFF1C1C1E) else Color.White),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mockup Card Content 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isMockupDark) Color(0xFF1C1C1E) else Color.White)
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isMockupDark) Color(0xFF3A3A3C) else Color(0xFFE5E7EB))
                        )
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isMockupDark) Color(0xFF2C2C2E) else Color(0xFFF3F4F6))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mockup Card Content 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isMockupDark) Color(0xFF1C1C1E) else Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * Single theme option row with icon, title, subtitle, and selection tick.
 */
@Composable
private fun ThemeOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
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
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = CupertinoIcons.Filled.CheckmarkCircle,
                contentDescription = "Đã chọn",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
