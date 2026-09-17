package com.buwin.tiktokvideodownload.ui.screens.home.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
import com.kyant.backdrop.backdrops.LayerBackdrop
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Moon
import io.github.alexzhirkevich.cupertino.icons.filled.SunMax

/**
 * Liquid glass round theme toggle button in the top bar.
 */
@Composable
fun HomeThemeButton(
    isDark: Boolean,
    onToggle: () -> Unit,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    LiquidRoundButton(
        onClick = onToggle,
        backdrop = backdrop,
        size = 40.dp,
        isDark = isDark,
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isDark) CupertinoIcons.Filled.SunMax else CupertinoIcons.Filled.Moon,
            contentDescription = "Theme Toggle",
            tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF0F172A),
            modifier = Modifier.size(20.dp)
        )
    }
}
