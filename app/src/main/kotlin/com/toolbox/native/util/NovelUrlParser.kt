package com.toolbox.native.util

object NovelUrlParser {

    /**
     * 从番茄小说 URL 中提取 book_id
     * 支持多种 URL 格式
     */
    fun parseBookId(url: String): String? {
        // 格式1: https://fanqienovel.com/page/xxx?id=7123456789
        val pattern1 = Regex("""[?&]id=(\d+)""")
        pattern1.find(url)?.groupValues?.get(1)?.let { return it }

        // 格式2: https://fanqienovel.com/reader/7123456789
        val pattern2 = Regex("""/reader/(\d+)""")
        pattern2.find(url)?.groupValues?.get(1)?.let { return it }

        // 格式3: 直接是数字
        if (url.matches(Regex("""^\d+$"""))) {
            return url
        }

        return null
    }

    /**
     * 验证 URL 是否有效
     */
    fun isValidUrl(url: String): Boolean {
        return parseBookId(url) != null
    }
}
