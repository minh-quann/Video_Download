package com.buwin.tiktokvideodownload.ui.components.editor.model

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.CircleLefthalfed
import io.github.alexzhirkevich.cupertino.icons.outlined.Flame
import io.github.alexzhirkevich.cupertino.icons.outlined.Paintpalette
import io.github.alexzhirkevich.cupertino.icons.outlined.SliderHorizontal3
import io.github.alexzhirkevich.cupertino.icons.outlined.SunMax
import io.github.alexzhirkevich.cupertino.icons.outlined.WandAndStars

/**
 * Tabs available in the Image Editor.
 */
enum class ImageEditorTab(val title: String) {
    ADJUST("ADJUST"),
    FILTERS("FILTERS"),
    CROP("CROP")
}

/**
 * Adjustment tools under the ADJUST tab.
 */
enum class ImageAdjustTool(val displayName: String, val icon: ImageVector) {
    AUTO("TỰ ĐỘNG", CupertinoIcons.Outlined.WandAndStars),
    EXPOSURE("PHƠI SÁNG", CupertinoIcons.Outlined.SunMax),
    BRILLIANCE("ĐỘ SÁNG", CupertinoIcons.Outlined.SunMax),
    CONTRAST("TƯƠNG PHẢN", CupertinoIcons.Filled.CircleLefthalfed),
    SATURATION("BÃO HÒA", CupertinoIcons.Outlined.Paintpalette),
    WARMTH("ĐỘ ẤM", CupertinoIcons.Outlined.Flame),
    TINT("TÔNG MÀU", CupertinoIcons.Outlined.SliderHorizontal3)
}

/**
 * Preset photo filters for image editing.
 */
enum class ImagePresetFilter(val displayName: String) {
    ORIGINAL("Gốc"),
    VIVID("Vivid"),
    VIVID_WARM("Vivid Warm"),
    VIVID_COOL("Vivid Cool"),
    DRAMATIC("Dramatic"),
    DRAMATIC_WARM("Dramatic Warm"),
    DRAMATIC_COOL("Dramatic Cool"),
    MONO("Mono"),
    SILVERTONE("Silvertone"),
    NOIR("Noir")
}
