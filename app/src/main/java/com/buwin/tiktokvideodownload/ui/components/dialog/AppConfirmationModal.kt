package com.buwin.tiktokvideodownload.ui.components.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ExclamationmarkTriangle

/**
 * Universal, reusable Confirmation Modal Dialog for the entire application.
 *
 * Implements Apple iOS Liquid Glass aesthetics:
 * - 28dp smooth rounded corner capsule with glass refraction and edge highlight.
 * - Backdrop blur & lens depth effects when backdrop is supplied, with seamless fallback.
 * - Tactile spring press feedback on both action buttons.
 * - Supports destructive (danger/red) and standard action styles.
 * - Android BackHandler integration for natural dismiss.
 */
@Composable
fun AppConfirmationModal(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Xác nhận",
    cancelText: String = "Hủy",
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)? = null,
    isDestructive: Boolean = false,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    backdrop: Backdrop? = null,
    isDark: Boolean = LocalIsDark.current,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    BackHandler(enabled = visible) {
        onDismissRequest()
    }

    val isLightTheme = !isDark
    val haptic = LocalHapticFeedback.current

    // Spring animation specs matching iOS 26 Liquid Glass
    val springSpec = spring<Float>(dampingRatio = 0.82f, stiffness = 380f)

    // Palette tokens
    val dimColor = if (isLightTheme) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.65f)
    val cardBgColor = if (isLightTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF1C1C1E).copy(alpha = 0.88f)
    val cardBorderColor = if (isLightTheme) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val titleColor = if (isLightTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val subtitleColor = if (isLightTheme) Color(0xFF64748B) else Color(0xFF94A3B8)

    val resolvedIcon = icon ?: if (isDestructive) CupertinoIcons.Outlined.ExclamationmarkTriangle else null
    val resolvedIconTint = iconTint ?: if (isDestructive) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.EnterTransition.None,
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            val cardModifier = if (backdrop != null) {
                Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(28.dp) },
                        effects = {
                            vibrancy()
                            blur(if (isLightTheme) 20f.dp.toPx() else 16f.dp.toPx())
                            lens(
                                refractionHeight = 18f.dp.toPx(),
                                refractionAmount = 24f.dp.toPx(),
                                depthEffect = true
                            )
                        },
                        highlight = { Highlight.Plain },
                        shadow = {
                            Shadow(
                                radius = 24f.dp,
                                color = if (isLightTheme) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.45f)
                            )
                        },
                        onDrawSurface = { drawRect(cardBgColor) }
                    )
            } else {
                Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color.Black.copy(alpha = if (isLightTheme) 0.15f else 0.5f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardBgColor)
                    .border(BorderStroke(1.dp, cardBorderColor), RoundedCornerShape(28.dp))
            }

            Column(
                modifier = Modifier
                    .animateEnterExit(
                        enter = fadeIn(animationSpec = springSpec) + scaleIn(initialScale = 0.88f, animationSpec = springSpec),
                        exit = fadeOut(animationSpec = springSpec) + scaleOut(targetScale = 0.88f, animationSpec = springSpec)
                    )
                    .then(cardModifier)
                    .then(modifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent outside dismiss when tapping inside the modal card
                    )
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Optional Header Icon Badge
                if (resolvedIcon != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(resolvedIconTint.copy(alpha = if (isLightTheme) 0.12f else 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolvedIcon,
                            contentDescription = null,
                            tint = resolvedIconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Modal Title
                BasicText(
                    text = title,
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Modal Descriptive Message
                BasicText(
                    text = message,
                    style = TextStyle(
                        color = subtitleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                // Optional custom content slot
                if (content != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons Row (Hủy / Tiếp tục & Xác nhận)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary Cancel Button
                    val cancelInteractionSource = remember { MutableInteractionSource() }
                    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
                    val cancelScale by animateFloatAsState(
                        targetValue = if (isCancelPressed) 0.96f else 1f,
                        animationSpec = springSpec,
                        label = "cancelButtonScale"
                    )

                    val cancelBgColor = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.10f)
                    val cancelTextColor = if (isLightTheme) Color(0xFF475569) else Color(0xFFCBD5E1)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .graphicsLayer {
                                scaleX = cancelScale
                                scaleY = cancelScale
                            }
                            .clip(CircleShape)
                            .background(cancelBgColor)
                            .clickable(
                                interactionSource = cancelInteractionSource,
                                indication = null,
                                role = Role.Button
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (onCancel != null) {
                                    onCancel()
                                } else {
                                    onDismissRequest()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = cancelText,
                            style = TextStyle(
                                color = cancelTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }

                    // Primary Confirm Button
                    val confirmInteractionSource = remember { MutableInteractionSource() }
                    val isConfirmPressed by confirmInteractionSource.collectIsPressedAsState()
                    val confirmScale by animateFloatAsState(
                        targetValue = if (isConfirmPressed) 0.96f else 1f,
                        animationSpec = springSpec,
                        label = "confirmButtonScale"
                    )

                    val confirmBgColor = if (isDestructive) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                    val confirmTextColor = Color.White

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .graphicsLayer {
                                scaleX = confirmScale
                                scaleY = confirmScale
                            }
                            .clip(CircleShape)
                            .background(confirmBgColor)
                            .clickable(
                                interactionSource = confirmInteractionSource,
                                indication = null,
                                role = Role.Button
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirm()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = confirmText,
                            style = TextStyle(
                                color = confirmTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }
            }
        }
    }
}
