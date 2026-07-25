package com.toolbox.nativetoolbox.util

import android.content.Context

/**
 * 拼音注音:数据表在 assets/pinyin.tsv(2.6 万字,含多音字全部读音),首次用到时加载。
 */
object PinyinUtil {

    private var table: HashMap<Char, List<String>>? = null

    // 常见姓氏的特殊读法(姓名模式优先生效)
    private val SURNAME = mapOf(
        '仇' to "qiú", '区' to "ōu", '单' to "shàn", '解' to "xiè", '查' to "zhā",
        '曾' to "zēng", '朴' to "piáo", '冼' to "xiǎn", '翟' to "zhái", '盖' to "gě",
        '华' to "huà", '任' to "rén", '燕' to "yān", '纪' to "jǐ", '过' to "guō",
        '缪' to "miào", '晟' to "chéng", '尉' to "yù", '乐' to "yuè", '员' to "yùn",
        '车' to "chē", '种' to "chóng", '重' to "chóng", '召' to "shào", '覃' to "qín",
    )

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (table != null) return
        val m = HashMap<Char, List<String>>(30000)
        context.assets.open("pinyin.tsv").bufferedReader().forEachLine { line ->
            val tab = line.indexOf('\t')
            if (tab > 0) {
                val ch = line[0]
                m[ch] = line.substring(tab + 1).split(',')
            }
        }
        table = m
    }

    fun lookup(c: Char): List<String>? = table?.get(c)

    /** 去掉声调:zhōng → zhong(两串严格等长一一对应,只含单码点字符) */
    private const val TONED = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜńňǹḿ"
    private const val PLAIN = "aaaaeeeeiiiioooouuuuüüüünnnm"

    fun stripTone(py: String): String = buildString(py.length) {
        for (ch in py) {
            val i = TONED.indexOf(ch)
            append(if (i >= 0) PLAIN[i] else ch)
        }
    }

    /** 每个字的注音结果 */
    data class Anno(val char: Char, val pinyins: List<String>?)

    fun annotate(text: String, nameMode: Boolean): List<Anno> {
        val t = table ?: return emptyList()
        val out = ArrayList<Anno>(text.length)
        text.forEachIndexed { idx, c ->
            var pys = t[c]
            if (nameMode && idx == 0 && SURNAME.containsKey(c)) {
                pys = listOf(SURNAME[c]!!) + (pys ?: emptyList()).filter { it != SURNAME[c] }
            }
            out.add(Anno(c, pys))
        }
        return out
    }
}
