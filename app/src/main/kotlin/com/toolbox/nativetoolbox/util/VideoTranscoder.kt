package com.toolbox.nativetoolbox.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer

/** 硬件加速视频转码:decoder → Surface → encoder → muxer */
object VideoTranscoder {
    data class Params(val width: Int, val height: Int, val videoBitrate: Int)

    /** 按短边等比缩放到目标 */
    fun fit(w: Int, h: Int, shortSide: Int): Pair<Int, Int> {
        if (w <= 0 || h <= 0) return shortSide to shortSide
        val (sw, sh) = if (w < h) w to h else h to w
        val scale = shortSide.toFloat() / sw
        val nw = (w * scale).toInt() and -2
        val nh = (h * scale).toInt() and -2
        return nw to nh
    }

    /** 转码,回调进度 0..100,失败返回 false */
    fun transcode(context: Context, srcUri: Uri, dst: File, params: Params, onProgress: (Int) -> Unit): Boolean {
        return runCatching {
            val extractor = MediaExtractor()
            context.contentResolver.openFileDescriptor(srcUri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: throw Exception("无法打开源视频")

            var videoTrack = -1
            var audioTrack = -1
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                when {
                    mime.startsWith("video/") && videoTrack < 0 -> { videoTrack = i; videoFormat = fmt }
                    mime.startsWith("audio/") && audioTrack < 0 -> { audioTrack = i; audioFormat = fmt }
                }
            }
            if (videoTrack < 0 || videoFormat == null) throw Exception("视频轨道缺失")

            val srcDurUs = videoFormat.getLong(MediaFormat.KEY_DURATION)

            // 解码器
            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME)!!
            val decoder = MediaCodec.createDecoderByType(videoMime)
            decoder.configure(videoFormat, null, null, 0)

            // 编码器
            val encoderFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, params.width, params.height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, params.videoBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }
            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            decoder.configure(videoFormat, inputSurface, null, 0)

            val muxer = MediaMuxer(dst.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var muxerStarted = false

            decoder.start()
            encoder.start()

            if (audioTrack >= 0 && audioFormat != null) {
                // 音频直接复制(不重编码)
                muxerAudioTrack = muxer.addTrack(audioFormat)
            }

            extractor.selectTrack(videoTrack)
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                // 喂解码器
                if (!inputDone) {
                    val inputId = decoder.dequeueInputBuffer(10_000)
                    if (inputId >= 0) {
                        val buf = decoder.getInputBuffer(inputId)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inputId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val pts = extractor.sampleTime
                            decoder.queueInputBuffer(inputId, 0, size, pts, 0)
                            extractor.advance()
                            val pct = if (srcDurUs > 0) (pts * 50 / srcDurUs).toInt().coerceIn(0, 50) else 0
                            onProgress(pct)
                        }
                    }
                }

                // 解码器输出 → Surface(自动到 encoder)
                val outId = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outId >= 0) {
                    decoder.releaseOutputBuffer(outId, true)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        encoder.signalEndOfInputStream()
                    }
                }

                // 取编码器输出
                val encOutId = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    encOutId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFmt = encoder.outputFormat
                        muxerVideoTrack = muxer.addTrack(newFmt)
                        if (muxerAudioTrack < 0 || audioTrack < 0) {
                            muxer.start(); muxerStarted = true
                        }
                    }
                    encOutId >= 0 -> {
                        val buf = encoder.getOutputBuffer(encOutId)!!
                        if (bufferInfo.size > 0) {
                            if (!muxerStarted) { muxer.start(); muxerStarted = true }
                            muxer.writeSampleData(muxerVideoTrack, buf, bufferInfo)
                            val pct = if (srcDurUs > 0) (50 + bufferInfo.presentationTimeUs * 50 / srcDurUs).toInt().coerceIn(50, 100) else 50
                            onProgress(pct)
                        }
                        encoder.releaseOutputBuffer(encOutId, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            // 音频复制(如果有)
            if (audioTrack >= 0 && muxerAudioTrack >= 0 && muxerStarted) {
                extractor.unselectTrack(videoTrack)
                extractor.selectTrack(audioTrack)
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                val maxSize = 1024 * 1024
                val audioBuf = ByteBuffer.allocate(maxSize)
                val audioInfo = MediaCodec.BufferInfo()
                while (true) {
                    audioBuf.clear()
                    val size = extractor.readSampleData(audioBuf, 0)
                    if (size < 0) break
                    audioInfo.set(0, size, extractor.sampleTime, extractor.sampleFlags)
                    muxer.writeSampleData(muxerAudioTrack, audioBuf, audioInfo)
                    extractor.advance()
                }
            }

            decoder.stop()
            decoder.release()
            encoder.stop()
            encoder.release()
            inputSurface.release()
            if (muxerStarted) muxer.stop()
            muxer.release()
            extractor.release()
            true
        }.getOrElse { false }
    }
}
