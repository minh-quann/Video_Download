package com.buwin.tiktokvideodownload.ui.components.liquid

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect

/**
 * Shared iOS 26 Liquid Glass Progressive Blur Top Bar.
 * Produces an authentic frosted alpha-masked gradient blur (mờ dần) that seamlessly
 * dissolves into transparency without harsh dividing lines.
 */
@Composable
fun LiquidTopBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDark.current,
    headerHeight: Dp = 114.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val tintColor = if (isDark) Color(0xFF0C0C0E) else Color(0xFFFAFAF9)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .drawPlainBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        blur(if (isDark) 18f.dp.toPx() else 24f.dp.toPx())
                        runtimeShaderEffect(
                            "AlphaMask",
                            """
uniform shader content;

uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
    float blurAlpha = smoothstep(size.y, size.y * 0.35, coord.y);
    float tintAlpha = smoothstep(size.y, size.y * 0.35, coord.y);
    return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
}""",
                            "content"
                        ) {
                            setFloatUniform("size", size.width, size.height)
                            setColorUniform("tint", tintColor)
                            setFloatUniform("tintIntensity", if (isDark) 0.85f else 0.78f)
                        }
                    } else {
                        blur(if (isDark) 16f.dp.toPx() else 22f.dp.toPx())
                    }
                }
            )
    ) {
        // Ultra-smooth progressive tint overlay fading out seamlessly towards the bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tintColor.copy(alpha = if (isDark) 0.88f else 0.82f),
                            tintColor.copy(alpha = if (isDark) 0.65f else 0.55f),
                            tintColor.copy(alpha = if (isDark) 0.25f else 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Interactive Header Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    navigationIcon?.invoke()
                    title()
                }

                if (actions != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        content = actions
                    )
                }
            }
        }
    }
}

/**
 * Convenient shared overload with structured text title, optional subtitle and badge.
 */
@Composable
fun LiquidTopBar(
    backdrop: Backdrop,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isDark: Boolean = LocalIsDark.current,
    headerHeight: Dp = 114.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    titleBadge: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    LiquidTopBar(
        backdrop = backdrop,
        modifier = modifier,
        isDark = isDark,
        headerHeight = headerHeight,
        navigationIcon = navigationIcon,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.2.sp
                    )
                    titleBadge?.invoke()
                }
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = actions
    )
}
