package com.buwin.tiktokvideodownload.ui.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.buwin.tiktokvideodownload.R
import com.google.firebase.auth.FirebaseUser
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.CheckmarkCircle
import io.github.alexzhirkevich.cupertino.icons.filled.Person
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowClockwise

/**
 * Account & Cloud Sync card for the Settings screen.
 * Displays Google profile when logged in, or Google sign-in action when logged out.
 */
@Composable
fun SettingsAccountCard(
    currentUser: FirebaseUser?,
    isSigningIn: Boolean,
    isSyncing: Boolean,
    onSignInClick: () -> Unit,
    onSyncClick: () -> Unit,
    onSignOutClick: () -> Unit,
    cardBackground: Color,
    cardBorderColor: Color,
    dividerColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        if (currentUser != null) {
            // Logged in user profile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUser.photoUrl?.toString().isNullOrEmpty()) {
                            AsyncImage(
                                model = currentUser.photoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = CupertinoIcons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.displayName ?: "Người dùng Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentUser.email ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Filled.CheckmarkCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Đã đồng bộ lịch sử tải về",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.6.dp, color = dividerColor)
                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Sync Now & Sign Out
                val buttonSpring = spring<Float>(dampingRatio = 0.80f, stiffness = 450f)
                val haptic = LocalHapticFeedback.current

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Sync Button (Full Rounded Capsule)
                    val syncInteractionSource = remember { MutableInteractionSource() }
                    val isSyncPressed by syncInteractionSource.collectIsPressedAsState()
                    val syncScale by animateFloatAsState(
                        targetValue = if (isSyncPressed) 0.95f else 1f,
                        animationSpec = buttonSpring,
                        label = "syncScale"
                    )

                    val syncColor = MaterialTheme.colorScheme.primary
                    val syncBg = syncColor.copy(alpha = if (isDark) 0.18f else 0.10f)
                    val syncBorder = syncColor.copy(alpha = if (isDark) 0.30f else 0.20f)

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = syncScale
                                scaleY = syncScale
                            }
                            .clip(CircleShape)
                            .background(syncBg)
                            .border(BorderStroke(1.dp, syncBorder), CircleShape)
                            .clickable(
                                interactionSource = syncInteractionSource,
                                indication = null,
                                role = Role.Button,
                                enabled = !isSyncing
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSyncClick()
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = syncColor
                                )
                            } else {
                                Icon(
                                    imageVector = CupertinoIcons.Outlined.ArrowClockwise,
                                    contentDescription = null,
                                    tint = syncColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = if (isSyncing) "Đang đồng bộ..." else "Đồng bộ ngay",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = syncColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // 2. Sign Out Button (Full Rounded Capsule)
                    val signOutInteractionSource = remember { MutableInteractionSource() }
                    val isSignOutPressed by signOutInteractionSource.collectIsPressedAsState()
                    val signOutScale by animateFloatAsState(
                        targetValue = if (isSignOutPressed) 0.95f else 1f,
                        animationSpec = buttonSpring,
                        label = "signOutScale"
                    )

                    val signOutRed = Color(0xFFFF3B30)
                    val signOutBg = signOutRed.copy(alpha = if (isDark) 0.18f else 0.10f)
                    val signOutBorder = signOutRed.copy(alpha = if (isDark) 0.30f else 0.20f)

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = signOutScale
                                scaleY = signOutScale
                            }
                            .clip(CircleShape)
                            .background(signOutBg)
                            .border(BorderStroke(1.dp, signOutBorder), CircleShape)
                            .clickable(
                                interactionSource = signOutInteractionSource,
                                indication = null,
                                role = Role.Button
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSignOutClick()
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = signOutRed,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Đăng xuất",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = signOutRed
                            )
                        }
                    }
                }
            }
        } else {
            // Not logged in: Google Sign In banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F4F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Đăng nhập với Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lưu trữ và sao lưu lịch sử tải về trên đám mây",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    onClick = onSignInClick,
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF2C2C2E) else Color.White,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (isDark) Color.White else Color(0xFF1F2937)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Đang kết nối Google...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color(0xFF1F2937)
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_google_logo),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Tiếp tục với Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }
        }
    }
}
