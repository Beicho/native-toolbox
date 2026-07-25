package com.toolbox.nativetoolbox.data.predict

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 使用事件仓库。纯本地 JSON,不上传。
 *
 * 一条事件 = 「什么时候、在什么情境下、用了哪个工具、用得怎么样」。
 * 打分引擎全靠这张表算亲和度。
 */
object EventStore {

    private const val FILE = "predict-events.json"
    private const val MAX_EVENTS = 3000       // 约合重度用户半年
    private const val KEEP_DAYS = 120

    private lateinit var appContext: Context
    private val events = mutableListOf<Event>()
    private var loaded = false

    data class Event(
        val route: String,
        val ts: Long,
        val quarterBucket: Int,
        val dayOfWeek: Int,
        val charging: Boolean,
        val net: String,
        val wifiHash: Int,
        val bt: String,
        val clip: String,
        val prevRoute: String?,
        /** 停留毫秒。< 3000 视为误触,打分时不给正权重 */
        val dwellMs: Long,
        /** 是否产生了实际输出(存文件/复制结果)—— 强正信号 */
        val produced: Boolean,
        /** 是否是从「此刻」推荐位点进来的 —— 用于评估推荐质量 */
        val fromRecommend: Boolean,
    ) {
        val isMeaningful: Boolean get() = dwellMs >= 3000 || produced

        fun toJson(): JSONObject = JSONObject()
            .put("r", route).put("t", ts).put("q", quarterBucket).put("w", dayOfWeek)
            .put("c", charging).put("n", net).put("h", wifiHash).put("b", bt)
            .put("cl", clip).put("p", prevRoute ?: "").put("d", dwellMs)
            .put("pr", produced).put("fr", fromRecommend)

        companion object {
            fun fromJson(o: JSONObject): Event? = runCatching {
                Event(
                    route = o.getString("r"),
                    ts = o.getLong("t"),
                    quarterBucket = o.optInt("q"),
                    dayOfWeek = o.optInt("w"),
                    charging = o.optBoolean("c"),
                    net = o.optString("n", "NONE"),
                    wifiHash = o.optInt("h"),
                    bt = o.optString("b", "NONE"),
                    clip = o.optString("cl", "NONE"),
                    prevRoute = o.optString("p").takeIf { it.isNotEmpty() },
                    dwellMs = o.optLong("d"),
                    produced = o.optBoolean("pr"),
                    fromRecommend = o.optBoolean("fr"),
                )
            }.getOrNull()
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun file(): File = File(appContext.filesDir, FILE)

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val f = file()
        if (!f.exists()) return
        runCatching {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                Event.fromJson(arr.getJSONObject(i))?.let { events.add(it) }
            }
        }
    }

    @Synchronized
    private fun persist() {
        runCatching {
            val arr = JSONArray()
            events.forEach { arr.put(it.toJson()) }
            val target = file()
            val tmp = File(target.parentFile, "$FILE.tmp")
            tmp.writeText(arr.toString())
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        }
    }

    @Synchronized
    fun record(e: Event) {
        ensureLoaded()
        events.add(e)
        if (events.size > MAX_EVENTS) {
            // 丢最老的 10%,一次性摊销,避免每条都做整表操作
            val drop = events.size - (MAX_EVENTS * 9 / 10)
            repeat(drop) { if (events.isNotEmpty()) events.removeAt(0) }
        }
        persist()
    }

    @Synchronized
    fun all(): List<Event> {
        ensureLoaded()
        return events.toList()
    }

    @Synchronized
    fun lastRoute(): String? {
        ensureLoaded()
        return events.lastOrNull()?.route
    }

    /** 总事件数,用于判断冷启动阶段 */
    @Synchronized
    fun size(): Int {
        ensureLoaded()
        return events.size
    }

    /** 首次记录距今天数,用于冷启动权重衰减 */
    @Synchronized
    fun ageDays(): Int {
        ensureLoaded()
        val first = events.firstOrNull()?.ts ?: return 0
        return ((System.currentTimeMillis() - first) / 86_400_000L).toInt()
    }

    @Synchronized
    fun prune() {
        ensureLoaded()
        val cutoff = System.currentTimeMillis() - KEEP_DAYS * 86_400_000L
        val before = events.size
        events.removeAll { it.ts < cutoff }
        if (events.size != before) persist()
    }

    @Synchronized
    fun clearAll() {
        ensureLoaded()
        events.clear()
        persist()
    }

    // ---- 用户显式反馈:不想看到某个工具 ----

    private const val MUTE_PREFS = "predict_mute"

    fun mute(route: String) {
        appContext.getSharedPreferences(MUTE_PREFS, Context.MODE_PRIVATE)
            .edit().putLong(route, System.currentTimeMillis()).apply()
    }

    fun unmute(route: String) {
        appContext.getSharedPreferences(MUTE_PREFS, Context.MODE_PRIVATE)
            .edit().remove(route).apply()
    }

    /** 静音倍率:被拉黑 30 天内大幅降权,之后自然恢复 */
    fun muteFactor(route: String): Double {
        val at = appContext.getSharedPreferences(MUTE_PREFS, Context.MODE_PRIVATE)
            .getLong(route, 0L)
        if (at == 0L) return 1.0
        val days = (System.currentTimeMillis() - at) / 86_400_000.0
        return if (days >= 30) 1.0 else 0.15
    }

    fun mutedRoutes(): Set<String> =
        appContext.getSharedPreferences(MUTE_PREFS, Context.MODE_PRIVATE).all.keys
}
