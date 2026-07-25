package com.toolbox.nativetoolbox.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

/**
 * 视频压缩转码:解码器直接渲染到编码器输入 Surface(系统自动缩放和颜色转换,不需要 GL),
 * H.264 重编码降码率;AAC 音轨原样拷贝不重编。
 */
object VideoTranscoder {

    data class Params(val width: Int, val height: Int, val bitrate: Int)

    /** 按目标短边计算输出尺寸(保持比例,偶数对齐) */
    fun fit(srcW: Int, srcH: Int, shortSide: Int): Pair<Int, Int> {
        if (minOf(srcW, srcH) <= shortSide) return srcW / 2 * 2 to srcH / 2 * 2
        val scale = shortSide.toFloat() / minOf(srcW, srcH)
        val w = (srcW * scale).toInt() / 2 * 2
        val h = (srcH * scale).toInt() / 2 * 2
        return w to h
    }

    fun transcode(
        context: Context,
        input: Uri,
        output: File,
        params: Params,
        onProgress: (Int) -> Unit,
    ): Boolean = runCatching {
        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(context, input, null)
        var vTrack = -1
        var vFormat: MediaFormat? = null
        for (i in 0 until videoExtractor.trackCount) {
            val f = videoExtractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) { vTrack = i; vFormat = f; break }
        }
        if (vTrack < 0 || vFormat == null) { videoExtractor.release(); return false }
        videoExtractor.selectTrack(vTrack)
        val durationUs = if (vFormat.containsKey(MediaFormat.KEY_DURATION)) vFormat.getLong(MediaFormat.KEY_DURATION) else 0L
        val rotation = if (vFormat.containsKey(MediaFormat.KEY_ROTATION)) vFormat.getInteger(MediaFormat.KEY_ROTATION) else 0

        // 编码器
        val outFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, params.width, params.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, params.bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        // 解码器渲染到编码器 surface
        val decoder = MediaCodec.createDecoderByType(vFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(vFormat, inputSurface, null, 0)
        decoder.start()

        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer.setOrientationHint(rotation)
        var muxVideoTrack = -1
        var muxerStarted = false

        // 音轨:AAC 直拷贝
        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(context, input, null)
        var aTrack = -1
        var aFormat: MediaFormat? = null
        for (i in 0 until audioExtractor.trackCount) {
            val f = audioExtractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_AAC) { aTrack = i; aFormat = f; break }
        }
        var muxAudioTrack = -1

        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var decodeDone = false
        var encodeDone = false

        while (!encodeDone) {
            // 喂解码器
            if (!inputDone) {
                val inIdx = decoder.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val buf = decoder.getInputBuffer(inIdx)!!
                    val n = videoExtractor.readSampleData(buf, 0)
                    if (n < 0) {
                        decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(inIdx, 0, n, videoExtractor.sampleTime, 0)
                        videoExtractor.advance()
                    }
                }
            }
            // 解码输出 → 渲染到编码器 surface
            if (!decodeDone) {
                val outIdx = decoder.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    decoder.releaseOutputBuffer(outIdx, info.size > 0 && !eos)
                    if (durationUs > 0 && info.presentationTimeUs > 0) {
                        onProgress((info.presentationTimeUs * 100 / durationUs).toInt().coerceIn(0, 99))
                    }
                    if (eos) {
                        decodeDone = true
                        encoder.signalEndOfInputStream()
                    }
                }
            }
            // 收编码输出
            val encIdx = encoder.dequeueOutputBuffer(info, 10_000)
            when {
                encIdx >= 0 -> {
                    val buf = encoder.getOutputBuffer(encIdx)!!
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        buf.position(info.offset); buf.limit(info.offset + info.size)
                        muxer.writeSampleData(muxVideoTrack, buf, info)
                    }
                    encoder.releaseOutputBuffer(encIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) encodeDone = true
                }
                encIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    muxVideoTrack = muxer.addTrack(encoder.outputFormat)
                    if (aTrack >= 0 && aFormat != null) muxAudioTrack = muxer.addTrack(aFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
        }

        // 拷贝音轨
        if (aTrack >= 0 && muxAudioTrack >= 0) {
            audioExtractor.selectTrack(aTrack)
            val abuf = ByteBuffer.allocate(1 shl 20)
            val ainfo = MediaCodec.BufferInfo()
            while (true) {
                val n = audioExtractor.readSampleData(abuf, 0)
                if (n < 0) break
                ainfo.offset = 0
                ainfo.size = n
                ainfo.presentationTimeUs = audioExtractor.sampleTime
                ainfo.flags = if (audioExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                muxer.writeSampleData(muxAudioTrack, abuf, ainfo)
                audioExtractor.advance()
            }
        }

        decoder.stop(); decoder.release()
        encoder.stop(); encoder.release()
        inputSurface.release()
        if (muxerStarted) muxer.stop()
        muxer.release()
        videoExtractor.release()
        audioExtractor.release()
        onProgress(100)
        true
    }.getOrDefault(false)
}
