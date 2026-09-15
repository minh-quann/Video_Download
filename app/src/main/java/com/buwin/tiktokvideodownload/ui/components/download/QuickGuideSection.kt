package com.buwin.tiktokvideodownload.ui.components.download

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Bolt
import io.github.alexzhirkevich.cupertino.icons.filled.CheckmarkCircle
import io.github.alexzhirkevich.cupertino.icons.filled.DocOnDoc
import io.github.alexzhirkevich.cupertino.icons.filled.Film
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles

/**
 * Modern welcome hub showing features, quick instructions, and clipboard detection when no video is loaded.
 */
@Composable
fun QuickGuideSection(
    clipboardUrl: String?,
    cardBackground: Color,
    cardBorderColor: Color,
    onUseClipboardUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── 1. Clipboard Quick Action Card (if media link detected) ──
        if (!clipboardUrl.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUseClipboardUrl(clipboardUrl) },
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF007AFF).copy(alpha = if (isDark) 0.16f else 0.08f)
                ),
                border = BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = if (isDark) 0.35f else 0.22f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF007AFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Filled.DocOnDoc,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        val isFb = clipboardUrl.contains("facebook.com") || clipboardUrl.contains("fb.watch") || clipboardUrl.contains("fb.com")
                        Column {
                            Text(
                                text = if (isFb) "Phát hiện link Facebook" else "Phát hiện link TikTok",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Chạm để dán và tải ngay",
                                fontSize = 11.5.sp,
                                color = Color(0xFF007AFF)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF007AFF))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Tải ngay",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ── 2. Bento Feature Highlights Grid ──
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tính năng vượt trội",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureTile(
                    title = "1080p Siêu nét",
                    description = "Chất lượng gốc cao nhất",
                    icon = CupertinoIcons.Filled.Film,
                    tint = Color(0xFF10B981),
                    bgTint = if (isDark) Color(0xFF064E3B).copy(0.35f) else Color(0xFFD1FAE5),
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    modifier = Modifier.weight(1f)
                )

                FeatureTile(
                    title = "Xóa Logo 100%",
                    description = "Không dính ID tác giả",
                    icon = CupertinoIcons.Filled.CheckmarkCircle,
                    tint = Color(0xFF007AFF),
                    bgTint = if (isDark) Color(0xFF1E3A8A).copy(0.35f) else Color(0xFFEFF6FF),
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureTile(
                    title = "Tách Nhạc MP3",
                    description = "Trích xuất âm thanh 320k",
                    icon = CupertinoIcons.Outlined.MusicNote,
                    tint = Color(0xFFF59E0B),
                    bgTint = if (isDark) Color(0xFF78350F).copy(0.35f) else Color(0xFFFEF3C7),
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    modifier = Modifier.weight(1f)
                )

                FeatureTile(
                    title = "Tốc độ Siêu tốc",
                    description = "Bóc tách tức thì trong 1s",
                    icon = CupertinoIcons.Filled.Bolt,
                    tint = Color(0xFF8B5CF6),
                    bgTint = if (isDark) Color(0xFF4C1D95).copy(0.35f) else Color(0xFFF5F3FF),
                    cardBackground = cardBackground,
                    cardBorderColor = cardBorderColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── 3. Quick 3-Step Guide Card (Standard 26.dp Squircle) ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            border = BorderStroke(1.dp, cardBorderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF007AFF).copy(alpha = if (isDark) 0.22f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.Sparkles,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Cách tải video trong 3 bước",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    GuideStepRow(
                        number = "1",
                        title = "Sao chép liên kết TikTok hoặc Facebook",
                        subtitle = "Mở video/Reels, bấm Chia sẻ -> Sao chép liên kết",
                        isLast = false
                    )
                    GuideStepRow(
                        number = "2",
                        title = "Dán vào ô tìm kiếm",
                        subtitle = "Chạm nút \"Dán\" phía trên hoặc nhập link trực tiếp",
                        isLast = false
                    )
                    GuideStepRow(
                        number = "3",
                        title = "Chọn chất lượng & Tải về",
                        subtitle = "Bấm nút Tải HD để lưu video không logo về máy",
                        isLast = true
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureTile(
    title: String,
    description: String,
    icon: ImageVector,
    tint: Color,
    bgTint: Color,
    cardBackground: Color,
    cardBorderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GuideStepRow(
    number: String,
    title: String,
    subtitle: String,
    isLast: Boolean = false
) {
    val isDark = LocalIsDark.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF).copy(alpha = if (isDark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(24.dp)
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE6E8EC))
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp, bottom = if (!isLast) 8.dp else 0.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
