package com.toolbox.nativetoolbox.util

import java.nio.charset.Charset

/**
 * 拼音首字母检索:让「jsq」能搜到「计算器」。
 *
 * 原理:GB2312 一级字库(3755 个常用字)按拼音排序,比较每个字的
 * GB2312 区位码就能定位它的拼音首字母 —— 23 个分界点,零字库依赖。
 *
 * 【坑】Kotlin 的 Char 比较是 Unicode 码点序,而分界表只在 GB2312
 * 字节序下成立(「蛾/哦/压」的 Unicode 顺序和拼音顺序完全不同),
 * 所以必须先转 GB2312 再比字节。转不过去的字(GBK 扩展/生僻字)返回 null。
 *
 * 多音字按 GB2312 排序位置(即最常用读法),检索场景足够。
 */
object PinyinInitials {

    private val gb2312: Charset? = runCatching { Charset.forName("GB2312") }.getOrNull()

    private val letters = "abcdefghjklmnopqrstwxyz".toCharArray()

    /** 各首字母区第一个字的 GB2312 码(区位序升序,已验证) */
    private val boundaryCodes: IntArray? = run {
        val cs = gb2312 ?: return@run null
        val chars = "啊芭擦搭蛾发噶哈击喀垃妈拿哦啪期然撒塌挖昔压匝"
        val codes = IntArray(chars.length)
        for (i in chars.indices) {
            val b = chars[i].toString().toByteArray(cs)
            if (b.size != 2) return@run null
            codes[i] = ((b[0].toInt() and 0xFF) shl 8) or (b[1].toInt() and 0xFF)
        }
        codes
    }

    private fun gbCode(c: Char): Int? {
        val cs = gb2312 ?: return null
        val b = runCatching { c.toString().toByteArray(cs) }.getOrNull() ?: return null
        if (b.size != 2) return null
        val code = ((b[0].toInt() and 0xFF) shl 8) or (b[1].toInt() and 0xFF)
        // GB2312 汉字区从 0xB0A1 开始;之前是符号区
        return if (code >= 0xB0A1) code else null
    }

    /** 单个汉字的拼音首字母;生僻字或非汉字返回 null */
    fun initialOf(c: Char): Char? {
        val bounds = boundaryCodes ?: return null
        val code = gbCode(c) ?: return null
        var idx = -1
        for (i in bounds.indices) {
            if (code >= bounds[i]) idx = i else break
        }
        return if (idx >= 0) letters[idx] else null
    }

    /** 「计算器」→ "jsq";混排时字母数字原样小写保留("WiFi码" → "wifim") */
    fun of(text: String): String = buildString {
        text.forEach { c ->
            val init = initialOf(c)
            when {
                init != null -> append(init)
                c.isLetterOrDigit() -> append(c.lowercaseChar())
            }
        }
    }
}
