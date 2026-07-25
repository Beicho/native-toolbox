package com.toolbox.nativetoolbox.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 音频处理地基:任意格式解码成 16bit PCM;PCM 写 WAV;PCM 编码 AAC 封装 m4a。
 * 全部走系统 MediaCodec,零第三方依赖。
 */
object AudioKit {

    data class Pcm(val samples: ShortArray, val sampleRate: Int, val channels: Int) {
        val durationMs: Long get() = samples.size.toLong() * 1000 / (sampleRate * channels)
    }

    /** 解码音频(或视频里的音轨)为 PCM。maxDurationMs 防止超长文件挤爆内存 */
    fun decode(context: Context, uri: Uri, maxDurationMs: Long = 10 * 60_000): Pcm? = runCatching {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                trackIndex = i; format = f; break
            }
        }
        if (trackIndex < 0 || format == null) { extractor.release(); return null }
        extractor.selectTrack(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val out = ArrayList<ShortArray>()
        var totalSamples = 0L
        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var sawInputEOS = false
        var sawOutputEOS = false
        val info = MediaCodec.BufferInfo()
        val maxTotal = maxDurationMs * sampleRate * channels / 1000

        while (!sawOutputEOS && totalSamples < maxTotal) {
            if (!sawInputEOS) {
                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val buf = codec.getInputBuffer(inIdx)!!
                    val n = extractor.readSampleData(buf, 0)
                    if (n < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, n, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIdx >= 0 -> {
                    val buf = codec.getOutputBuffer(outIdx)!!
                    if (info.size > 0) {
                        val shorts = ShortArray(info.size / 2)
                        buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                        out.add(shorts)
                        totalSamples += shorts.size
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) sawOutputEOS = true
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val nf = codec.outputFormat
                    sampleRate = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
        }
        codec.stop(); codec.release(); extractor.release()

        val total = out.sumOf { it.size }
        val merged = ShortArray(total)
        var off = 0
        for (chunk in out) { chunk.copyInto(merged, off); off += chunk.size }
        Pcm(merged, sampleRate, channels)
    }.getOrNull()

    /** PCM → WAV 字节流 */
    fun writeWav(pcm: Pcm, out: OutputStream) {
        val dataLen = pcm.samples.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataLen)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)                                   // PCM
        header.putShort(pcm.channels.toShort())
        header.putInt(pcm.sampleRate)
        header.putInt(pcm.sampleRate * pcm.channels * 2)     // byte rate
        header.putShort((pcm.channels * 2).toShort())        // block align
        header.putShort(16)                                  // bits
        header.put("data".toByteArray())
        header.putInt(dataLen)
        out.write(header.array())
        val bytes = ByteBuffer.allocate(dataLen).order(ByteOrder.LITTLE_ENDIAN)
        bytes.asShortBuffer().put(pcm.samples)
        out.write(bytes.array())
        out.flush()
    }

    /** PCM → AAC(m4a 文件),bitrate 默认 128k */
    fun encodeM4a(pcm: Pcm, outFile: File, bitrate: Int = 128_000): Boolean = runCatching {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, pcm.sampleRate, pcm.channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var track = -1
        var muxerStarted = false

        val info = MediaCodec.BufferInfo()
        var srcPos = 0
        var ptsUs = 0L
        var inputDone = false
        var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val inIdx = codec.dequeueInputBuffer(10_000)
                if (inIdx >= 0) {
                    val buf = codec.getInputBuffer(inIdx)!!
                    buf.clear()
                    val capShorts = buf.capacity() / 2
                    val n = minOf(capShorts, pcm.samples.size - srcPos)
                    if (n <= 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        buf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm.samples, srcPos, n)
                        codec.queueInputBuffer(inIdx, 0, n * 2, ptsUs, 0)
                        srcPos += n
                        ptsUs = srcPos.toLong() * 1_000_000 / (pcm.sampleRate * pcm.channels)
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, 10_000)
            when {
                outIdx >= 0 -> {
                    val buf = codec.getOutputBuffer(outIdx)!!
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        buf.position(info.offset); buf.limit(info.offset + info.size)
                        muxer.writeSampleData(track, buf, info)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
        }
        codec.stop(); codec.release()
        if (muxerStarted) { muxer.stop() }
        muxer.release()
        true
    }.getOrDefault(false)

    /** 简单线性重采样变速(变速变调联动,像磁带快放) */
    fun resampleSpeed(pcm: Pcm, speed: Float): Pcm {
        if (speed == 1f) return pcm
        val frames = pcm.samples.size / pcm.channels
        val newFrames = (frames / speed).toInt().coerceAtLeast(1)
        val out = ShortArray(newFrames * pcm.channels)
        for (f in 0 until newFrames) {
            val srcF = (f * speed)
            val i0 = srcF.toInt().coerceAtMost(frames - 1)
            val i1 = (i0 + 1).coerceAtMost(frames - 1)
            val t = srcF - i0
            for (c in 0 until pcm.channels) {
                val a = pcm.samples[i0 * pcm.channels + c].toFloat()
                val b = pcm.samples[i1 * pcm.channels + c].toFloat()
                out[f * pcm.channels + c] = (a + (b - a) * t).toInt().toShort()
            }
        }
        return Pcm(out, pcm.sampleRate, pcm.channels)
    }

    /** 倒放 */
    fun reverse(pcm: Pcm): Pcm {
        val frames = pcm.samples.size / pcm.channels
        val out = ShortArray(pcm.samples.size)
        for (f in 0 until frames) {
            val src = (frames - 1 - f) * pcm.channels
            for (c in 0 until pcm.channels) out[f * pcm.channels + c] = pcm.samples[src + c]
        }
        return Pcm(out, pcm.sampleRate, pcm.channels)
    }

    /** 裁剪 [startMs, endMs) */
    fun trim(pcm: Pcm, startMs: Long, endMs: Long): Pcm {
        val spms = pcm.sampleRate * pcm.channels / 1000.0
        val s = (startMs * spms).toInt().coerceIn(0, pcm.samples.size)
        val e = (endMs * spms).toInt().coerceIn(s, pcm.samples.size)
        // 对齐到帧边界
        val sAligned = s / pcm.channels * pcm.channels
        val eAligned = e / pcm.channels * pcm.channels
        return Pcm(pcm.samples.copyOfRange(sAligned, eAligned), pcm.sampleRate, pcm.channels)
    }

    /** 拼接(采样率声道需一致,不一致时重采样到第一段) */
    fun concat(list: List<Pcm>): Pcm? {
        if (list.isEmpty()) return null
        val base = list.first()
        val parts = list.map { p ->
            if (p.sampleRate == base.sampleRate && p.channels == base.channels) p
            else convert(p, base.sampleRate, base.channels)
        }
        val total = parts.sumOf { it.samples.size }
        val out = ShortArray(total)
        var off = 0
        for (p in parts) { p.samples.copyInto(out, off); off += p.samples.size }
        return Pcm(out, base.sampleRate, base.channels)
    }

    /** 采样率/声道转换 */
    fun convert(pcm: Pcm, targetRate: Int, targetChannels: Int): Pcm {
        var cur = pcm
        // 声道
        if (cur.channels != targetChannels) {
            val frames = cur.samples.size / cur.channels
            val out = ShortArray(frames * targetChannels)
            for (f in 0 until frames) {
                if (targetChannels == 1) {
                    var sum = 0
                    for (c in 0 until cur.channels) sum += cur.samples[f * cur.channels + c]
                    out[f] = (sum / cur.channels).toShort()
                } else {
                    val mono = cur.samples[f * cur.channels]
                    for (c in 0 until targetChannels) out[f * targetChannels + c] = mono
                }
            }
            cur = Pcm(out, cur.sampleRate, targetChannels)
        }
        // 采样率(线性)
        if (cur.sampleRate != targetRate) {
            val frames = cur.samples.size / cur.channels
            val newFrames = (frames.toLong() * targetRate / cur.sampleRate).toInt()
            val out = ShortArray(newFrames * cur.channels)
            for (f in 0 until newFrames) {
                val srcF = f.toDouble() * cur.sampleRate / targetRate
                val i0 = srcF.toInt().coerceAtMost(frames - 1)
                val i1 = (i0 + 1).coerceAtMost(frames - 1)
                val t = (srcF - i0).toFloat()
                for (c in 0 until cur.channels) {
                    val a = cur.samples[i0 * cur.channels + c].toFloat()
                    val b = cur.samples[i1 * cur.channels + c].toFloat()
                    out[f * cur.channels + c] = (a + (b - a) * t).toInt().toShort()
                }
            }
            cur = Pcm(out, targetRate, cur.channels)
        }
        return cur
    }
}
