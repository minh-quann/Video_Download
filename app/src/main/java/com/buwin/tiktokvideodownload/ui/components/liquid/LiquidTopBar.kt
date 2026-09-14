package com.buwin.tiktokvideodownload.ui.components.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

/**
 * Pinned Sticky Liquid Glass Top Bar.
 * Blurs and refracts scrollable content passing underneath with authentic frosted liquid glass.
 */
@Composable
fun LiquidTopBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDark.current,
    contentHeight: Dp = 60.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val containerGlassColor = if (isDark) {
        Color(0xFF101014).copy(alpha = 0.65f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.72f)
    }

    val hairlineBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { androidx.compose.foundation.shape.RoundedCornerShape(0.dp) },
                effects = {
                    vibrancy()
                    blur(if (isDark) 16f.dp.toPx() else 22f.dp.toPx())
                },
                highlight = {
                    Highlight.Default.copy(alpha = if (isDark) 0.15f else 0.40f)
                },
                shadow = {
                    Shadow(
                        radius = 10f.dp,
                        color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.06f)
                    )
                },
                onDrawSurface = {
                    drawRect(containerGlassColor)
                }
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        // Frosted hairline glass divider at the bottom edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(hairlineBorderColor)
        )
    }
}
