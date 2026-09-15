package com.buwin.tiktokvideodownload.ui.components.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.ui.theme.BackgroundDark
import com.buwin.tiktokvideodownload.ui.theme.BackgroundLight
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.Backdrop

/**
 * 7-Stop Progressive Fade Gradient Footer directly ported from MEBIECO (MainNavigator.android.tsx).
 *
 * MEBIECO design specs (mobile/src/styles/colors.ts & MainNavigator.android.tsx):
 * - colors: [fade[0], fade[8], fade[20], fade[40], fade[65], fade[85], fade[95]]
 * - locations: [0.0, 0.15, 0.3, 0.45, 0.6, 0.8, 1.0]
 * - height: 80dp + insets, positioned at bottom: 0 (pointerEvents: none)
 *
 * Visual behavior:
 * - Top (near Liquid Glass Tab): 0% opacity (completely sharp & unhindered scrolling content).
 * - Under Tab: Gentle non-linear easing curve (8% -> 20% -> 40%).
 * - Bottom (near System Navigation Bar): Deep fade (65% -> 85% -> 95%) dissolving seamlessly into background.
 */
@Composable
fun LiquidBottomProgressiveBlur(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isDark: Boolean = LocalIsDark.current,
    footerHeight: Dp = 100.dp
) {
    val fadeBaseColor = if (isDark) BackgroundDark else BackgroundLight
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val effectiveHeight = remember(footerHeight, navBarBottom) {
        maxOf(footerHeight, 80.dp + navBarBottom)
    }

    // Exact 7-stop non-linear easing curve identical to MEBIECO theme.fade
    val fadeBrush = remember(fadeBaseColor) {
        Brush.verticalGradient(
            0.00f to fadeBaseColor.copy(alpha = 0.00f), // MEBIECO fade[0]: rgba(..., 0)
            0.15f to fadeBaseColor.copy(alpha = 0.08f), // MEBIECO fade[8]: rgba(..., 0.08)
            0.30f to fadeBaseColor.copy(alpha = 0.20f), // MEBIECO fade[20]: rgba(..., 0.20)
            0.45f to fadeBaseColor.copy(alpha = 0.40f), // MEBIECO fade[40]: rgba(..., 0.40)
            0.60f to fadeBaseColor.copy(alpha = 0.65f), // MEBIECO fade[65]: rgba(..., 0.65)
            0.80f to fadeBaseColor.copy(alpha = 0.85f), // MEBIECO fade[85]: rgba(..., 0.85)
            1.00f to fadeBaseColor.copy(alpha = 0.95f)  // MEBIECO fade[95]: rgba(..., 0.95)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(effectiveHeight)
            .background(fadeBrush)
    )
}
