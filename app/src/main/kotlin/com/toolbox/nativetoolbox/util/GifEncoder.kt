package com.toolbox.nativetoolbox.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * GIF89a 编码器(标准 LZW + NeuQuant 简化八叉树量化的轻量替代:均匀 216 色 web 安全色 + 抖动)。
 * 纯 Kotlin 零依赖,足够表情包和短动图使用。
 */
class GifEncoder(private val out: OutputStream) {

    private var width = 0
    private var height = 0
    private var started = false

    fun start(w: Int, h: Int) {
        width = w; height = h
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShort(w); writeShort(h)
        out.write(0xF7)          // 全局色表 256 色
        out.write(0)             // 背景色
        out.write(0)             // 像素比
        writePalette()
        // 循环扩展(永远循环)
        out.write(0x21); out.write(0xFF); out.write(11)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3); out.write(1); writeShort(0); out.write(0)
        started = true
    }

    /** delayMs 每帧时长 */
    fun addFrame(bmp: Bitmap, delayMs: Int) {
        require(started)
        val indexed = quantize(bmp)
        // 图形控制扩展
        out.write(0x21); out.write(0xF9); out.write(4)
        out.write(0)                    // 无透明
        writeShort(delayMs / 10)        // 单位 1/100 秒
        out.write(0); out.write(0)
        // 图像描述符
        out.write(0x2C)
        writeShort(0); writeShort(0); writeShort(width); writeShort(height)
        out.write(0)                    // 无局部色表
        lzwEncode(indexed)
    }

    fun finish() {
        out.write(0x3B)
        out.flush()
    }

    // 6×6×6 均匀色立方 216 色 + 40 灰阶 = 256
    private fun writePalette() {
        val pal = ByteArray(256 * 3)
        var i = 0
        for (r in 0..5) for (g in 0..5) for (b in 0..5) {
            pal[i * 3] = (r * 51).toByte(); pal[i * 3 + 1] = (g * 51).toByte(); pal[i * 3 + 2] = (b * 51).toByte()
            i++
        }
        var g = 0
        while (i < 256) {
            val v = (g * 255 / 39)
            pal[i * 3] = v.toByte(); pal[i * 3 + 1] = v.toByte(); pal[i * 3 + 2] = v.toByte()
            i++; g++
        }
        out.write(pal)
    }

    private fun colorIndex(r: Int, g: Int, b: Int): Int {
        // 灰色优先走灰阶(更细腻)
        val maxC = maxOf(r, g, b); val minC = minOf(r, g, b)
        if (maxC - minC < 12) {
            val v = (r + g + b) / 3
            return 216 + (v * 39 / 255)
        }
        return (r + 25) / 51 * 36 + (g + 25) / 51 * 6 + (b + 25) / 51
    }

    private fun quantize(bmp: Bitmap): ByteArray {
        val scaled = if (bmp.width != width || bmp.height != height)
            Bitmap.createScaledBitmap(bmp, width, height, true) else bmp
        val px = IntArray(width * height)
        scaled.getPixels(px, 0, width, 0, 0, width, height)
        if (scaled !== bmp) scaled.recycle()
        val idx = ByteArray(px.size)
        for (i in px.indices) {
            val p = px[i]
            idx[i] = colorIndex((p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF).toByte()
        }
        return idx
    }

    private fun writeShort(v: Int) {
        out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
    }

    /** 标准 GIF-LZW,码宽 8(256 色) */
    private fun lzwEncode(pixels: ByteArray) {
        val initCodeSize = 8
        out.write(initCodeSize)
        val clearCode = 1 shl initCodeSize          // 256
        val eofCode = clearCode + 1                 // 257
        var nextCode = eofCode + 1
        var codeSize = initCodeSize + 1

        val dict = HashMap<Long, Int>(8192)
        val block = ByteArrayOutputStream()
        var bitBuf = 0L
        var bitCnt = 0

        fun flushByteBlocks(force: Boolean) {
            while (bitCnt >= 8) {
                block.write((bitBuf and 0xFF).toInt())
                bitBuf = bitBuf shr 8
                bitCnt -= 8
                if (block.size() == 255) {
                    out.write(255); block.writeTo(out); block.reset()
                }
            }
            if (force && bitCnt > 0) {
                block.write((bitBuf and 0xFF).toInt())
                bitBuf = 0; bitCnt = 0
            }
            if (force && block.size() > 0) {
                out.write(block.size()); block.writeTo(out); block.reset()
            }
        }

        fun emit(code: Int) {
            bitBuf = bitBuf or (code.toLong() shl bitCnt)
            bitCnt += codeSize
            flushByteBlocks(false)
            // GIFLIB 语义:写完当前 code 后,下一个空位超出当前位宽才加宽(时机早一步解码就废)
            if (nextCode > (1 shl codeSize) - 1 && codeSize < 12) codeSize++
        }

        emit(clearCode)
        var prev = pixels[0].toInt() and 0xFF
        for (i in 1 until pixels.size) {
            val c = pixels[i].toInt() and 0xFF
            val key = (prev.toLong() shl 12) or c.toLong()
            val hit = dict[key]
            if (hit != null) {
                prev = hit
            } else {
                emit(prev)
                dict[key] = nextCode
                nextCode++
                if (nextCode >= 4096) {
                    emit(clearCode)
                    dict.clear()
                    nextCode = eofCode + 1
                    codeSize = initCodeSize + 1
                }
                prev = c
            }
        }
        emit(prev)
        emit(eofCode)
        flushByteBlocks(true)
        out.write(0) // 块结束
    }
}
