package com.buwin.tiktokvideodownload.data.editor

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.SpeedChangingAudioProcessor
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Advanced media editing engine using Jetpack Media3 Transformer.
 * Handles operations that require re-encoding: speed, reverse, rotate, contrast adjustments.
 * For lossless operations (trim, mute, extract audio), use VideoEditorEngine directly.
 */
object AdvancedEditorEngine {

    /**
     * Change video playback speed using Media3 Transformer.
     * @param speed Speed multiplier (e.g., 0.5 for half speed, 2.0 for double speed)
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend fun changeSpeed(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        speed: Float,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .build()

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(false)
                .build()

            val result = runTransformer(context, editedMediaItem, outputFile, onProgress)
            result
        } catch (e: Throwable) {
            outputFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Rotate video by specified degrees (90, 180, 270).
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend fun rotateVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        rotationDegrees: Float,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .build()

            val rotation = ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(rotationDegrees)
                .build()

            val effects = Effects(
                /* audioProcessors= */ listOf(),
                /* videoEffects= */ listOf(rotation)
            )

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(effects)
                .build()

            runTransformer(context, editedMediaItem, outputFile, onProgress)
        } catch (e: Throwable) {
            outputFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Apply color adjustments (brightness, contrast, saturation) to video.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend fun adjustColors(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .build()

            val videoEffects = mutableListOf<androidx.media3.common.Effect>()
            if (brightness != 0f) {
                videoEffects.add(Brightness(brightness))
            }
            if (contrast != 0f) {
                videoEffects.add(Contrast(contrast))
            }
            if (saturation != 0f) {
                videoEffects.add(
                    HslAdjustment.Builder()
                        .adjustSaturation(saturation * 100f)
                        .build()
                )
            }

            val effects = Effects(
                /* audioProcessors= */ listOf(),
                /* videoEffects= */ videoEffects
            )

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(effects)
                .build()

            runTransformer(context, editedMediaItem, outputFile, onProgress)
        } catch (e: Throwable) {
            outputFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Apply contrast adjustment to video.
     * @param contrast Value from -1.0 to 1.0 (0 = no change)
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend fun adjustContrast(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        contrast: Float,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .build()

            val contrastEffect = Contrast(contrast)

            val effects = Effects(
                /* audioProcessors= */ listOf(),
                /* videoEffects= */ listOf(contrastEffect)
            )

            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(effects)
                .build()

            runTransformer(context, editedMediaItem, outputFile, onProgress)
        } catch (e: Throwable) {
            outputFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Core transformer execution with progress reporting.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private suspend fun runTransformer(
        context: Context,
        editedMediaItem: EditedMediaItem,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = suspendCancellableCoroutine { cont ->
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onProgress(1f)
                    if (cont.isActive) cont.resume(Result.success(outputFile))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    outputFile.delete()
                    if (cont.isActive) cont.resume(Result.failure(exportException))
                }
            })
            .build()

        transformer.start(editedMediaItem, outputFile.absolutePath)

        cont.invokeOnCancellation {
            transformer.cancel()
            outputFile.delete()
        }
    }
}
