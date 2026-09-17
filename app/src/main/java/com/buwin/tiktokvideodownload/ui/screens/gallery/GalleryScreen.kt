package com.buwin.tiktokvideodownload.ui.screens.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.dialog.AppConfirmationModal
import com.buwin.tiktokvideodownload.ui.components.liquid.LiquidRoundButton
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
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowClockwise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Native Gallery Screen.
 * Displays all device photos, videos, downloaded TikTok videos, and edited creations.
 * Features automatic real-time media observation (ContentObserver) and automatic reload on resume.
 */
@Composable
fun GalleryScreen(
    backdrop: Backdrop,
    downloadHelper: DownloadManagerHelper,
    onPlayRecord: (DownloadRecord, Rect?) -> Unit,
    onEditRecord: (DownloadRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = LocalIsDark.current
    val contentBackdrop = rememberLayerBackdrop()
    val coroutineScope = rememberCoroutineScope()

    var mediaItems by remember { mutableStateOf<List<GalleryMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

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

    // Coroutine loader for all device and app media with optional debounce
    fun loadMedia(debounceMs: Long = 0L) {
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
            }
            if (mediaItems.isEmpty()) {
                isLoading = true
            }
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

    // Automatically detect newly captured photos/videos via real-time MediaStore observer
    // and reload automatically whenever the user resumes the app from camera
    DisposableEffect(lifecycleOwner, hasPermission) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadMedia()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                loadMedia(debounceMs = 300L)
            }
        }

        val contentResolver = context.contentResolver
        if (hasPermission) {
            try {
                contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    contentObserver
                )
                contentResolver.registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    contentObserver
                )
            } catch (_: SecurityException) {}
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (hasPermission) {
                try {
                    contentResolver.unregisterContentObserver(contentObserver)
                } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── 3-Column Native Gallery Grid ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .layerBackdrop(contentBackdrop)
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            contentPadding = PaddingValues(top = 112.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Empty state placeholder
            if (mediaItems.isEmpty() && !isLoading) {
                item(span = { GridItemSpan(3) }) {
                    GalleryEmptyState(
                        isDark = isDark,
                        hasPermission = hasPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(permissions.toTypedArray())
                        }
                    )
                }
            }

            // Media items: direct click opens full-screen player, long click opens actions
            items(mediaItems, key = { it.id }) { item ->
                GalleryMediaCard(
                    item = item,
                    isDark = isDark,
                    onClick = { bounds -> onPlayRecord(item.toDownloadRecord(), bounds) },
                    onLongClick = { selectedItemForActions = item }
                )
            }
        }

        // ── Liquid Glass Top Bar with auto count and manual refresh action ──
        LiquidTopBar(
            backdrop = contentBackdrop,
            modifier = Modifier.align(Alignment.TopCenter),
            title = "Bộ sưu tập",
            subtitle = "${mediaItems.size} tệp phương tiện trong máy",
            isDark = isDark,
            actions = {
                LiquidRoundButton(
                    onClick = { loadMedia() },
                    backdrop = contentBackdrop,
                    size = 40.dp,
                    surfaceColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
                ) {
                    if (isLoading && mediaItems.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = CupertinoIcons.Outlined.ArrowClockwise,
                            contentDescription = "Làm mới",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )

        // ── Item Action Bottom Sheet ──
        selectedItemForActions?.let { item ->
            GalleryActionSheet(
                item = item,
                isDark = isDark,
                onDismissRequest = { selectedItemForActions = null },
                onPlay = {
                    selectedItemForActions = null
                    onPlayRecord(item.toDownloadRecord(), null)
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
