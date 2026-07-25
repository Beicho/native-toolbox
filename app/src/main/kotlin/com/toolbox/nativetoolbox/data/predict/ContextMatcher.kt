package com.toolbox.nativetoolbox.data.predict

/**
 * 情境模式识别。
 *
 * 单个信号意义有限,**组合才有意义**:
 * 「蜂窝网络」不说明什么,但「工作日 8 点 + 蜂窝网络」就是在通勤路上。
 * 匹配到模式后整组工具提权,这是「读心感」的主要来源。
 */
object ContextMatcher {

    data class Mode(
        val id: String,
        val label: String,
        /** 命中这个模式时该提权的工具路由 */
        val routes: List<String>,
        /** 给用户看的推荐理由 */
        val reason: String,
        val match: (SignalCollector.Signals) -> Boolean,
    )

    private val modes = listOf(
        Mode(
            id = "share_image",
            label = "处理图片",
            routes = listOf(
                "tool/image_compress", "tool/image_matting", "tool/watermark",
                "tool/ocr", "tool/image_convert", "tool/image_crop"
            ),
            reason = "刚分享进来的图",
            match = { it.shareMime == "image" }
        ),
        Mode(
            id = "share_text",
            label = "处理文字",
            routes = listOf("tool/translate", "tool/textprocess", "tool/qr", "tool/text_format"),
            reason = "刚分享进来的文字",
            match = { it.shareMime == "text" }
        ),
        Mode(
            id = "clip_english",
            label = "剪贴板有英文",
            routes = listOf("tool/translate", "tool/cn_convert"),
            reason = "剪贴板里是英文",
            match = { it.clip == SignalCollector.ClipKind.ENGLISH }
        ),
        Mode(
            id = "clip_url",
            label = "剪贴板有网址",
            routes = listOf("tool/qr", "tool/short_url", "tool/sitecheck", "tool/whois"),
            reason = "剪贴板里是网址",
            match = { it.clip == SignalCollector.ClipKind.URL }
        ),
        Mode(
            id = "clip_number",
            label = "剪贴板有数字",
            routes = listOf("tool/sci_calc", "tool/unit", "tool/radix", "tool/amount_upper"),
            reason = "剪贴板里是一串数字",
            match = { it.clip == SignalCollector.ClipKind.NUMBER }
        ),
        Mode(
            id = "clip_phone",
            label = "剪贴板有手机号",
            routes = listOf("tool/phone_location", "tool/mask_sensitive"),
            reason = "剪贴板里是手机号",
            match = { it.clip == SignalCollector.ClipKind.PHONE }
        ),
        Mode(
            id = "driving",
            label = "在车上",
            routes = listOf("tool/move_car", "tool/parking", "tool/fuel_calc", "tool/gps_speed"),
            reason = "连着车载蓝牙",
            match = { it.bt == SignalCollector.BtKind.CAR }
        ),
        Mode(
            id = "commuting",
            label = "通勤路上",
            routes = listOf("tool/countdown", "tool/white_noise", "tool/todo", "tool/ab_player"),
            reason = "这个点你常在路上",
            match = { it.likelyCommuting }
        ),
        Mode(
            id = "heavy_work",
            label = "适合干重活",
            routes = listOf("tool/video_compress", "tool/audio_convert", "tool/pdf_tools", "tool/similar_clean"),
            reason = "在充电又连着 WiFi",
            match = { it.goodForHeavyWork }
        ),
        Mode(
            id = "low_battery",
            label = "电量告急",
            routes = listOf("tool/flashlight", "tool/battery_info", "tool/screen_time"),
            reason = "电量不多了",
            match = { it.lowBattery }
        ),
        Mode(
            id = "night",
            label = "睡前",
            routes = listOf("tool/white_noise", "tool/screen_time", "tool/wooden_fish", "tool/breath"),
            reason = "夜深了",
            match = { it.quarterBucket >= 88 || it.quarterBucket < 24 }
        ),
        Mode(
            id = "office",
            label = "办公时间",
            routes = listOf("tool/ocr", "tool/pdf_tools", "tool/translate", "tool/file_transfer", "tool/qr"),
            reason = "工作时间常用",
            match = {
                !it.isWeekend && it.quarterBucket in 36..72 &&
                    it.net == SignalCollector.NetKind.WIFI
            }
        ),
        Mode(
            id = "unfamiliar_wifi",
            label = "换了网络",
            routes = listOf("tool/wifi_qr", "tool/speed_test", "tool/temp_mail", "tool/exchange", "tool/timezone"),
            reason = "连了个新网络",
            match = { s ->
                s.net == SignalCollector.NetKind.WIFI && s.wifiHash != 0 &&
                    !KnownNetworks.isKnown(s.wifiHash)
            }
        ),
        Mode(
            id = "offline",
            label = "没网",
            routes = emptyList(),
            reason = "现在没网,只推荐离线能用的",
            match = { it.net == SignalCollector.NetKind.NONE }
        ),
        Mode(
            id = "weekend_morning",
            label = "周末早上",
            routes = listOf("tool/astronomy", "tool/history_today", "tool/lunar", "tool/weather"),
            reason = "周末早上适合看看",
            match = { it.isWeekend && it.quarterBucket in 28..48 }
        ),
    )

    /** 当前命中的所有模式,按特异性排序(意图类信号优先) */
    fun match(signals: SignalCollector.Signals): List<Mode> {
        val hits = modes.filter { it.match(signals) }
        // 分享/剪贴板这类「当下意图」信号最强,排前面
        val priority = mapOf(
            "share_image" to 0, "share_text" to 0,
            "clip_english" to 1, "clip_url" to 1, "clip_number" to 1, "clip_phone" to 1,
            "driving" to 2, "unfamiliar_wifi" to 2,
            "low_battery" to 3, "offline" to 3,
        )
        return hits.sortedBy { priority[it.id] ?: 5 }
    }
}

/**
 * 见过的 WiFi 网络记录。用来区分「常连的网」和「刚连上的陌生网」——
 * 陌生网络通常意味着在外面(酒店、咖啡厅、别人家),推荐的东西该不一样。
 * 只存哈希,还原不出网络名字。
 */
object KnownNetworks {
    private const val PREFS = "predict_networks"
    private const val KEY = "hashes"
    private lateinit var appContext: android.content.Context
    private var cache: MutableSet<String>? = null

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private fun load(): MutableSet<String> {
        cache?.let { return it }
        val s = appContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        cache = s
        return s
    }

    fun isKnown(hash: Int): Boolean = load().contains(hash.toString())

    /** 在这个网络下用了 3 次以上才算「常连」,避免误判 */
    fun note(hash: Int) {
        if (hash == 0) return
        val prefs = appContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val countKey = "count_$hash"
        val n = prefs.getInt(countKey, 0) + 1
        prefs.edit().putInt(countKey, n).apply()
        if (n >= 3) {
            val set = load()
            if (set.add(hash.toString())) {
                prefs.edit().putStringSet(KEY, set).apply()
            }
        }
    }
}
