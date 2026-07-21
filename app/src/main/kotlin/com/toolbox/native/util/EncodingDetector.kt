package com.toolbox.native.util

import java.io.InputStream
import java.nio.charset.Charset

object EncodingDetector {

    /**
     * 检测文件编码
     * 通过 BOM 标识和字节序列特征判断
     */
    fun detectEncoding(inputStream: InputStream): String {
        val bytes = ByteArray(4096)
        val read = inputStream.read(bytes)

        if (read < 2) return "UTF-8"

        // 检测 BOM
        val bomResult = detectBOM(bytes, read)
        if (bomResult != null) return bomResult

        // 检测字节特征
        return detectByBytePattern(bytes, read)
    }

    private fun detectBOM(bytes: ByteArray, length: Int): String? {
        if (length < 2) return null

        // UTF-8 BOM: EF BB BF
        if (length >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()) {
            return "UTF-8 BOM"
        }

        // UTF-16 LE BOM: FF FE
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return "UTF-16LE"
        }

        // UTF-16 BE BOM: FE FF
        if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return "UTF-16BE"
        }

        // UTF-32 LE BOM: FF FE 00 00
        if (length >= 4 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xFE.toByte() &&
            bytes[2] == 0x00.toByte() &&
            bytes[3] == 0x00.toByte()) {
            return "UTF-32LE"
        }

        // UTF-32 BE BOM: 00 00 FE FF
        if (length >= 4 &&
            bytes[0] == 0x00.toByte() &&
            bytes[1] == 0x00.toByte() &&
            bytes[2] == 0xFE.toByte() &&
            bytes[3] == 0xFF.toByte()) {
            return "UTF-32BE"
        }

        return null
    }

    private fun detectByBytePattern(bytes: ByteArray, length: Int): String {
        // 统计各种编码特征
        var utf8Count = 0
        var gbkCount = 0
        var big5Count = 0
        var asciiCount = 0

        var i = 0
        while (i < length) {
            val byte = bytes[i].toInt() and 0xFF

            // ASCII 字符
            if (byte < 0x80) {
                asciiCount++
                i++
                continue
            }

            // UTF-8 多字节序列检测
            if (byte >= 0xC0 && byte <= 0xDF && i + 1 < length) {
                val next = bytes[i + 1].toInt() and 0xFF
                if (next >= 0x80 && next <= 0xBF) {
                    utf8Count++
                    i += 2
                    continue
                }
            }

            if (byte >= 0xE0 && byte <= 0xEF && i + 2 < length) {
                val next1 = bytes[i + 1].toInt() and 0xFF
                val next2 = bytes[i + 2].toInt() and 0xFF
                if (next1 >= 0x80 && next1 <= 0xBF &&
                    next2 >= 0x80 && next2 <= 0xBF) {
                    utf8Count++
                    i += 3
                    continue
                }
            }

            // GBK 双字节检测
            if (byte >= 0x81 && byte <= 0xFE && i + 1 < length) {
                val next = bytes[i + 1].toInt() and 0xFF
                if ((next >= 0x40 && next <= 0x7E) || (next >= 0x80 && next <= 0xFE)) {
                    gbkCount++

                    // BIG5 特征检测（与 GBK 重叠部分）
                    if (byte >= 0xA1 && byte <= 0xF9 &&
                        ((next >= 0x40 && next <= 0x7E) || (next >= 0xA1 && next <= 0xFE))) {
                        big5Count++
                    }

                    i += 2
                    continue
                }
            }

            i++
        }

        // 如果大部分是 ASCII，再看多字节特征
        val totalMultiByte = utf8Count + gbkCount
        if (totalMultiByte == 0) return "UTF-8"

        // UTF-8 权重更高（优先返回）
        if (utf8Count > gbkCount) return "UTF-8"
        if (gbkCount > utf8Count && gbkCount > big5Count) return "GBK"
        if (big5Count > gbkCount) return "Big5"

        return "UTF-8" // 默认
    }

    /**
     * 转换文件编码
     */
    fun convertEncoding(
        inputStream: InputStream,
        sourceCharset: String,
        targetCharset: String
    ): ByteArray {
        // 移除 BOM 标识（如果是 UTF-8 BOM）
        val sourceCharsetClean = if (sourceCharset == "UTF-8 BOM") "UTF-8" else sourceCharset

        // 读取并转换
        val text = inputStream.bufferedReader(Charset.forName(sourceCharsetClean)).use { it.readText() }

        // 如果目标是 UTF-8 BOM，添加 BOM
        return if (targetCharset == "UTF-8 BOM") {
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            val content = text.toByteArray(Charsets.UTF_8)
            bom + content
        } else {
            text.toByteArray(Charset.forName(targetCharset))
        }
    }
}
