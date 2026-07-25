package com.toolbox.nativetoolbox.data.predict

/**
 * 冷启动先验表:新用户没有历史数据时,按时段给一组合理的默认推荐。
 * 随着个人数据积累,先验权重线性衰减到 0(30 天)。
 */
object ColdStartPriors {

    /** quarterBucket 区间 → 推荐工具(按优先级排) */
    private val byTime: List<Triple<IntRange, List<String>, String>> = listOf(
        Triple(24..35, listOf("tool/weather", "tool/countdown_day", "tool/history_today"), "早上常看"),
        Triple(36..51, listOf("tool/ocr", "tool/translate", "tool/qr", "tool/sci_calc"), "上午常用"),
        Triple(44..55, listOf("tool/bookkeeping", "tool/sci_calc", "tool/qr"), "午饭时段"),
        Triple(56..71, listOf("tool/translate", "tool/pdf_tools", "tool/file_transfer"), "下午常用"),
        Triple(72..83, listOf("tool/todo", "tool/bookkeeping", "tool/health_record"), "晚上常用"),
        Triple(84..95, listOf("tool/white_noise", "tool/screen_time", "tool/big_clock"), "睡前常用"),
        Triple(0..23, listOf("tool/white_noise", "tool/flashlight", "tool/big_clock"), "深夜常用"),
    )

    /** 万金油:任何时段都可能有人用 */
    private val evergreen = listOf(
        "tool/qr", "tool/sci_calc", "tool/translate", "tool/unit", "tool/countdown"
    )

    fun scoreFor(route: String, signals: SignalCollector.Signals): Double {
        var s = 0.0
        byTime.forEach { (range, routes, _) ->
            if (signals.quarterBucket in range) {
                val idx = routes.indexOf(route)
                if (idx >= 0) s += (routes.size - idx).toDouble() / routes.size
            }
        }
        if (route in evergreen) s += 0.25
        return s
    }

    fun reasonFor(route: String, signals: SignalCollector.Signals): String? {
        byTime.forEach { (range, routes, reason) ->
            if (signals.quarterBucket in range && routes.contains(route)) return reason
        }
        return null
    }
}
