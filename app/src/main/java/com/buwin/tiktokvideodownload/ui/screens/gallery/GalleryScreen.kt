package com.buwin.tiktokvideodownload.ui.screens.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.dialog.AppConfirmationModal
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidTopBar
import com.buwin.tiktokvideodownload.ui.components.toast.AppToast
import com.buwin.tiktokvideodownload.ui.screens.gallery.components.GalleryActionSheet
import com.buwin.tiktokvideodownload.ui.screens.gallery.components.GalleryEmptyState
import com.buwin.tiktokvideodownload.ui.screens.gallery.components.GalleryMediaCard
import com.buwin.tiktokvideodownload.ui.screens.gallery.model.GalleryMediaItem
import com.buwin.tiktokvideodownload.ui.screens.gallery.utils.GalleryMediaHelper
import com.buwin.tiktokvideodownload.ui.theme.LocalIsDark
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-featured Media Collection & Gallery screen.
 * Displays all device photos, videos, downloaded TikTok videos, and edited creations.
 */
@Composable
fun GalleryScreen(
    backdrop: Backdrop,
    downloadHelper: DownloadManagerHelper,
    onPlayRecord: (DownloadRecord) -> Unit,
    onEditRecord: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()
    val coroutineScope = rememberCoroutineScope()

    var mediaItems by remember { mutableStateOf<List<GalleryMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Active item action sheet & modals
    var selectedItemForActions by remember { mutableStateOf<GalleryMediaItem?>(null) }
    var deletingItem by remember { mutableStateOf<GalleryMediaItem?>(null) }

    // Storage permissions setup
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    var hasPermission by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Coroutine loader for all device and app media
    fun loadMedia() {
        coroutineScope.launch {
            isLoading = true
            val items = withContext(Dispatchers.IO) {
                GalleryMediaHelper.loadAllDeviceMedia(context, downloadHelper)
            }
            mediaItems = items
            isLoading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it }
        loadMedia()
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
        loadMedia()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── 2-Column Media Grid ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .layerBackdrop(contentBackdrop)
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 118.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Empty state placeholder
            if (mediaItems.isEmpty() && !isLoading) {
                item(span = { GridItemSpan(2) }) {
                    GalleryEmptyState(
                        isDark = isDark,
                        hasPermission = hasPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(permissions.toTypedArray())
                        }
                    )
                }
            }

            // Media items
            items(mediaItems, key = { it.id }) { item ->
                GalleryMediaCard(
                    item = item,
                    isDark = isDark,
                    onClick = { selectedItemForActions = item },
                    onQuickEdit = { onEditRecord(item.toDownloadRecord()) }
                )
            }
        }

        // ── Shared iOS Liquid Glass Top Bar ──
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Bộ sưu tập",
            subtitle = "${mediaItems.size} tệp phương tiện trong máy",
            isDark = isDark
        )

        // ── Item Action Bottom Sheet ──
        selectedItemForActions?.let { item ->
            GalleryActionSheet(
                item = item,
                isDark = isDark,
                onDismissRequest = { selectedItemForActions = null },
                onPlay = {
                    selectedItemForActions = null
                    onPlayRecord(item.toDownloadRecord())
                },
                onEdit = {
                    selectedItemForActions = null
                    onEditRecord(item.toDownloadRecord())
                },
                onShare = {
                    selectedItemForActions = null
                    GalleryMediaHelper.shareMedia(context, item.uri, item.isVideo)
                },
                onDelete = {
                    selectedItemForActions = null
                    deletingItem = item
                }
            )
        }

        // ── Confirm Delete Dialog ──
        AppConfirmationModal(
            visible = deletingItem != null,
            onDismissRequest = { deletingItem = null },
            title = "Xóa tệp này?",
            message = "Tệp \"${deletingItem?.title ?: ""}\" sẽ bị xóa khỏi bộ nhớ thiết bị.",
            confirmText = "Xóa tệp",
            cancelText = "Hủy",
            isDestructive = true,
            onConfirm = {
                val item = deletingItem ?: return@AppConfirmationModal
                try {
                    if (item.uri.scheme == "file") {
                        item.uri.path?.let { File(it).delete() }
                    } else {
                        context.contentResolver.delete(item.uri, null, null)
                    }
                    mediaItems = mediaItems.filter { it.id != item.id }
                    AppToast.showSuccess("Đã xóa tệp", item.title)
                } catch (e: Exception) {
                    AppToast.showError("Không thể xóa", e.localizedMessage ?: "Lỗi quyền truy cập")
                }
                deletingItem = null
            }
        )
    }
}
