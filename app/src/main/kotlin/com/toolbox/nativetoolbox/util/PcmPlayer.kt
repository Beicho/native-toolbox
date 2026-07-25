package com.toolbox.nativetoolbox.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/** 简易 PCM 播放器 */
class PcmPlayer {
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var playing = false

    fun play(pcm: AudioKit.Pcm, onDone: () -> Unit) {
        stop()
        playing = true
        thread = Thread {
            val t = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(pcm.sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(if (pcm.channels >= 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(65536)
                .build()
            track = t
            t.play()
            var pos = 0
            val step = 8192
            while (playing && pos < pcm.samples.size) {
                val n = minOf(step, pcm.samples.size - pos)
                t.write(pcm.samples, pos, n)
                pos += n
            }
            runCatching { t.stop(); t.release() }
            if (playing) onDone()
            playing = false
        }.apply { isDaemon = true; start() }
    }

    fun stop() {
        playing = false
        runCatching { track?.pause(); track?.flush() }
        thread = null
    }
}

