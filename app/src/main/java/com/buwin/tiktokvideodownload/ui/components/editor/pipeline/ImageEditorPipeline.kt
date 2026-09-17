package com.buwin.tiktokvideodownload.ui.components.editor.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.graphics.ColorMatrixColorFilter as AndroidColorMatrixColorFilter
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.graphics.ColorMatrix
import com.buwin.tiktokvideodownload.data.download.DownloadManagerHelper
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.components.editor.model.ImagePresetFilter
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a Compose ColorMatrix combining auto enhancement, manual adjustments,
 * and preset filter transforms.
 */
fun buildImageColorMatrix(
    isAuto: Boolean,
    autoVal: Float,
    exposure: Float,
    brilliance: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    tint: Float,
    filter: ImagePresetFilter,
    filterIntensity: Float
): ColorMatrix {
    val matrix = ColorMatrix()

    // Auto Enhance calculation
    val autoBright = if (isAuto) autoVal * 0.3f else 0f
    val autoCont = if (isAuto) autoVal * 0.2f else 0f
    val autoSat = if (isAuto) autoVal * 0.15f else 0f

    val bScale = (brilliance + autoBright) * 1.5f + exposure * 1.2f
    val cScale = 1f + (contrast + autoCont) / 100f
    val sScale = 1f + (saturation + autoSat) / 100f

    val cm = floatArrayOf(
        cScale, 0f, 0f, 0f, bScale + (warmth * 0.6f),
        0f, cScale, 0f, 0f, bScale + (tint * 0.5f),
        0f, 0f, cScale, 0f, bScale - (warmth * 0.6f),
        0f, 0f, 0f, 1f, 0f
    )
    matrix.set(ColorMatrix(cm))

    val satMat = ColorMatrix().apply { setToSaturation(sScale.coerceAtLeast(0f)) }
    matrix.timesAssign(satMat)

    // Preset filter weight
    val intensityWeight = filterIntensity / 100f
    when (filter) {
        ImagePresetFilter.VIVID -> {
            val vividSat = ColorMatrix().apply { setToSaturation(1f + 0.35f * intensityWeight) }
            matrix.timesAssign(vividSat)
        }
        ImagePresetFilter.VIVID_WARM -> {
            val warmMat = ColorMatrix(floatArrayOf(
                1f + 0.1f * intensityWeight, 0f, 0f, 0f, 15f * intensityWeight,
                0f, 1f + 0.05f * intensityWeight, 0f, 0f, 5f * intensityWeight,
                0f, 0f, 1f - 0.08f * intensityWeight, 0f, -10f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(warmMat)
        }
        ImagePresetFilter.VIVID_COOL -> {
            val coolMat = ColorMatrix(floatArrayOf(
                1f - 0.08f * intensityWeight, 0f, 0f, 0f, -8f * intensityWeight,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f + 0.15f * intensityWeight, 0f, 18f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(coolMat)
        }
        ImagePresetFilter.DRAMATIC -> {
            val dramMat = ColorMatrix(floatArrayOf(
                1f + 0.25f * intensityWeight, 0f, 0f, 0f, -10f * intensityWeight,
                0f, 1f + 0.25f * intensityWeight, 0f, 0f, -10f * intensityWeight,
                0f, 0f, 1f + 0.25f * intensityWeight, 0f, -10f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(dramMat)
        }
        ImagePresetFilter.DRAMATIC_WARM -> {
            val dramWarmMat = ColorMatrix(floatArrayOf(
                1f + 0.25f * intensityWeight, 0f, 0f, 0f, 10f * intensityWeight,
                0f, 1f + 0.25f * intensityWeight, 0f, 0f, 0f,
                0f, 0f, 1f + 0.20f * intensityWeight, 0f, -15f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(dramWarmMat)
        }
        ImagePresetFilter.DRAMATIC_COOL -> {
            val dramCoolMat = ColorMatrix(floatArrayOf(
                1f + 0.20f * intensityWeight, 0f, 0f, 0f, -15f * intensityWeight,
                0f, 1f + 0.25f * intensityWeight, 0f, 0f, 0f,
                0f, 0f, 1f + 0.28f * intensityWeight, 0f, 12f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(dramCoolMat)
        }
        ImagePresetFilter.MONO -> {
            val monoMat = ColorMatrix().apply { setToSaturation(1f - intensityWeight) }
            matrix.timesAssign(monoMat)
        }
        ImagePresetFilter.SILVERTONE -> {
            val silverMat = ColorMatrix(floatArrayOf(
                0.33f * intensityWeight + (1f - intensityWeight), 0.33f * intensityWeight, 0.33f * intensityWeight, 0f, 18f * intensityWeight,
                0.33f * intensityWeight, 0.33f * intensityWeight + (1f - intensityWeight), 0.33f * intensityWeight, 0f, 18f * intensityWeight,
                0.33f * intensityWeight, 0.33f * intensityWeight, 0.33f * intensityWeight + (1f - intensityWeight), 0f, 22f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(silverMat)
        }
        ImagePresetFilter.NOIR -> {
            val noirMat = ColorMatrix(floatArrayOf(
                0.40f * intensityWeight + (1f - intensityWeight), 0.40f * intensityWeight, 0.40f * intensityWeight, 0f, -15f * intensityWeight,
                0.40f * intensityWeight, 0.40f * intensityWeight + (1f - intensityWeight), 0.40f * intensityWeight, 0f, -15f * intensityWeight,
                0.40f * intensityWeight, 0.40f * intensityWeight, 0.40f * intensityWeight + (1f - intensityWeight), 0f, -15f * intensityWeight,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.timesAssign(noirMat)
        }
        ImagePresetFilter.ORIGINAL -> {}
    }

    return matrix
}

/**
 * Applies transformations (rotation, flip, color matrix adjustments) to the original image bitmap,
 * compresses and saves to public Pictures directory, registers via MediaScanner,
 * and adds an entry into the local download history.
 */
suspend fun exportEditedImage(
    context: Context,
    sourceUri: Uri?,
    record: DownloadRecord,
    rotationDegrees: Float,
    isFlippedHorizontal: Boolean,
    isAutoEnhanced: Boolean,
    autoEnhanceAmount: Float,
    exposure: Float,
    brilliance: Float,
    contrast: Float,
    saturation: Float,
    warmth: Float,
    tint: Float,
    filter: ImagePresetFilter,
    filterIntensity: Float,
    downloadHelper: DownloadManagerHelper
): DownloadRecord {
    val inputStream = if (sourceUri != null) {
        context.contentResolver.openInputStream(sourceUri)
    } else {
        File(record.filePath).inputStream()
    } ?: throw IllegalStateException("Không thể đọc tệp ảnh nguồn")

    val originalBitmap = BitmapFactory.decodeStream(inputStream)
        ?: throw IllegalStateException("Không thể giải mã hình ảnh")

    val matrix = AndroidMatrix().apply {
        if (rotationDegrees != 0f) postRotate(rotationDegrees)
        if (isFlippedHorizontal) postScale(-1f, 1f)
    }

    val transformedBitmap = Bitmap.createBitmap(
        originalBitmap,
        0, 0,
        originalBitmap.width, originalBitmap.height,
        matrix,
        true
    )

    val colorMat = buildImageColorMatrix(
        isAuto = isAutoEnhanced,
        autoVal = autoEnhanceAmount,
        exposure = exposure,
        brilliance = brilliance,
        contrast = contrast,
        saturation = saturation,
        warmth = warmth,
        tint = tint,
        filter = filter,
        filterIntensity = filterIntensity
    )
    val androidColorFilter = AndroidColorMatrixColorFilter(AndroidColorMatrix(colorMat.values))

    val resultBitmap = Bitmap.createBitmap(
        transformedBitmap.width,
        transformedBitmap.height,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(resultBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = androidColorFilter
    }
    canvas.drawBitmap(transformedBitmap, 0f, 0f, paint)

    val picturesDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "TikTokDownloads"
    ).apply { if (!exists()) mkdirs() }

    val timestamp = System.currentTimeMillis()
    val outputFile = File(picturesDir, "IMG_EDIT_${timestamp}.jpg")

    FileOutputStream(outputFile).use { out ->
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }

    MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf("image/jpeg"), null)

    val newRecord = DownloadRecord(
        id = "edit_img_$timestamp",
        title = "Đã chỉnh sửa - ${record.title}",
        author = record.author,
        coverUrl = Uri.fromFile(outputFile).toString(),
        formatTitle = "Hình ảnh đã chỉnh sửa",
        fileExtension = "jpg",
        downloadId = timestamp,
        timestamp = timestamp,
        filePath = outputFile.absolutePath,
        originalUrl = record.originalUrl
    )
    downloadHelper.addHistoryRecord(newRecord)

    return newRecord
}
