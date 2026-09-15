package com.buwin.tiktokvideodownload.data.editor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * High-performance media editing engine using Android's native MediaExtractor and MediaMuxer.
 * Operates without re-encoding (lossless transmuxing) for sub-second, battery-efficient exports.
 */
object VideoEditorEngine {

    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer

    /**
     * Trims an MP4 video from [startMs] to [endMs] preserving video and audio quality.
     */
    suspend fun trimVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        endMs: Long,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply {
                setDataSource(context, inputUri, null)
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = mutableMapOf<Int, Int>()
            val trackCount = extractor.trackCount
            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            val durationUs = (endMs - startMs).coerceAtLeast(1L) * 1000L

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val muxerTrack = muxer.addTrack(format)
                    trackMap[i] = muxerTrack
                }
            }

            if (trackMap.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Không tìm thấy luồng video hoặc âm thanh hợp lệ"))
            }

            muxer.start()

            // Seek to start position using sync frame
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()
            val trackFirstPts = mutableMapOf<Int, Long>()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) {
                    extractor.unselectTrack(trackIndex)
                } else if (sampleTimeUs >= startUs) {
                    val muxerTrack = trackMap[trackIndex]
                    if (muxerTrack != null) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize > 0) {
                            val firstPts = trackFirstPts.getOrPut(trackIndex) { sampleTimeUs }
                            bufferInfo.offset = 0
                            bufferInfo.size = sampleSize
                            bufferInfo.presentationTimeUs = (sampleTimeUs - firstPts).coerceAtLeast(0L)
                            bufferInfo.flags = extractor.sampleFlags
                            muxer.writeSampleData(muxerTrack, buffer, bufferInfo)

                            val progress = ((sampleTimeUs - startUs).toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                }

                if (!extractor.advance()) break
            }

            muxer.stop()
            Result.success(outputFile)
        } catch (e: Throwable) {
            outputFile.delete()
            Result.failure(e)
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Extracts and trims the audio track from [inputUri] into an M4A/AAC file from [startMs] to [endMs].
     */
    suspend fun extractAudio(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long = 0L,
        endMs: Long = Long.MAX_VALUE,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply {
                setDataSource(context, inputUri, null)
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var audioTrackIndex = -1
            var muxerAudioTrack = -1
            val trackCount = extractor.trackCount
            val startUs = startMs * 1000L
            val endUs = if (endMs == Long.MAX_VALUE) Long.MAX_VALUE else endMs * 1000L

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = i
                    muxerAudioTrack = muxer.addTrack(format)
                    break
                }
            }

            if (audioTrackIndex < 0) {
                return@withContext Result.failure(IllegalStateException("Video này không có luồng âm thanh để trích xuất"))
            }

            muxer.start()

            if (startUs > 0) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()
            var firstPts = -1L
            val durationUs = (endUs - startUs).coerceAtLeast(1L)

            while (true) {
                val track = extractor.sampleTrackIndex
                if (track < 0) break
                if (track != audioTrackIndex) {
                    if (!extractor.advance()) break
                    continue
                }

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                if (sampleTimeUs >= startUs) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize > 0) {
                        if (firstPts < 0) firstPts = sampleTimeUs
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = (sampleTimeUs - firstPts).coerceAtLeast(0L)
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)

                        if (endUs < Long.MAX_VALUE) {
                            val progress = ((sampleTimeUs - startUs).toFloat() / durationUs.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                }

                if (!extractor.advance()) break
            }

            muxer.stop()
            Result.success(outputFile)
        } catch (e: Throwable) {
            outputFile.delete()
            Result.failure(e)
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Creates a muted copy of the video (strips audio tracks).
     */
    suspend fun muteVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long = 0L,
        endMs: Long = Long.MAX_VALUE,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply {
                setDataSource(context, inputUri, null)
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var videoTrackIndex = -1
            var muxerVideoTrack = -1
            val trackCount = extractor.trackCount
            val startUs = startMs * 1000L
            val endUs = if (endMs == Long.MAX_VALUE) Long.MAX_VALUE else endMs * 1000L

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    extractor.selectTrack(i)
                    videoTrackIndex = i
                    muxerVideoTrack = muxer.addTrack(format)
                    break
                }
            }

            if (videoTrackIndex < 0) {
                return@withContext Result.failure(IllegalStateException("Không tìm thấy luồng video"))
            }

            muxer.start()

            if (startUs > 0) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()
            var firstPts = -1L

            while (true) {
                val track = extractor.sampleTrackIndex
                if (track < 0) break
                if (track != videoTrackIndex) {
                    if (!extractor.advance()) break
                    continue
                }

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                if (sampleTimeUs >= startUs) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize > 0) {
                        if (firstPts < 0) firstPts = sampleTimeUs
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = (sampleTimeUs - firstPts).coerceAtLeast(0L)
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                    }
                }

                if (!extractor.advance()) break
            }

            muxer.stop()
            Result.success(outputFile)
        } catch (e: Throwable) {
            outputFile.delete()
            Result.failure(e)
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Muxes the video track from [videoUri] with the audio track from [audioUri] into [outputFile].
     * Replaces original video audio with the custom soundtrack seamlessly.
     */
    suspend fun replaceAudio(
        context: Context,
        videoUri: Uri,
        audioUri: Uri,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            videoExtractor = MediaExtractor().apply {
                setDataSource(context, videoUri, null)
            }
            audioExtractor = MediaExtractor().apply {
                setDataSource(context, audioUri, null)
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Select video track from video source
            var videoSourceTrack = -1
            var muxerVideoTrack = -1
            var videoDurationUs = 0L

            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoSourceTrack = i
                    muxerVideoTrack = muxer.addTrack(format)
                    videoDurationUs = try { format.getLong(MediaFormat.KEY_DURATION) } catch (_: Exception) { 0L }
                    break
                }
            }

            // Select audio track from audio source
            var audioSourceTrack = -1
            var muxerAudioTrack = -1

            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    audioSourceTrack = i
                    muxerAudioTrack = muxer.addTrack(format)
                    break
                }
            }

            if (videoSourceTrack < 0 || audioSourceTrack < 0) {
                return@withContext Result.failure(IllegalStateException("Cần cả luồng video và luồng âm thanh để ghép nhạc"))
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            // 1. Write Video Samples
            var firstVideoPts = -1L
            while (true) {
                val track = videoExtractor.sampleTrackIndex
                if (track < 0) break
                if (track != videoSourceTrack) {
                    if (!videoExtractor.advance()) break
                    continue
                }

                val sampleTimeUs = videoExtractor.sampleTime
                val sampleSize = videoExtractor.readSampleData(buffer, 0)
                if (sampleSize > 0) {
                    if (firstVideoPts < 0) firstVideoPts = sampleTimeUs
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = (sampleTimeUs - firstVideoPts).coerceAtLeast(0L)
                    bufferInfo.flags = videoExtractor.sampleFlags
                    muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)

                    if (videoDurationUs > 0) {
                        onProgress((sampleTimeUs.toFloat() / videoDurationUs.toFloat() * 0.6f).coerceIn(0f, 0.6f))
                    }
                }

                if (!videoExtractor.advance()) break
            }

            // 2. Write Audio Samples (bounded by video duration)
            var firstAudioPts = -1L
            while (true) {
                val track = audioExtractor.sampleTrackIndex
                if (track < 0) break
                if (track != audioSourceTrack) {
                    if (!audioExtractor.advance()) break
                    continue
                }

                val sampleTimeUs = audioExtractor.sampleTime
                if (videoDurationUs > 0 && sampleTimeUs > videoDurationUs) break

                val sampleSize = audioExtractor.readSampleData(buffer, 0)
                if (sampleSize > 0) {
                    if (firstAudioPts < 0) firstAudioPts = sampleTimeUs
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = (sampleTimeUs - firstAudioPts).coerceAtLeast(0L)
                    bufferInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)

                    if (videoDurationUs > 0) {
                        val p = 0.6f + (sampleTimeUs.toFloat() / videoDurationUs.toFloat() * 0.4f).coerceIn(0f, 0.4f)
                        onProgress(p.coerceIn(0f, 1f))
                    }
                }

                if (!audioExtractor.advance()) break
            }

            muxer.stop()
            Result.success(outputFile)
        } catch (e: Throwable) {
            outputFile.delete()
            Result.failure(e)
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { audioExtractor?.release() } catch (_: Exception) {}
            try { videoExtractor?.release() } catch (_: Exception) {}
        }
    }
}
