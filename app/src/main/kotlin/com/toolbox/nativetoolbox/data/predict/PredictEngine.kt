package com.toolbox.nativetoolbox.data.predict

import android.content.Context
import com.toolbox.nativetoolbox.data.store.AstroStore
import kotlin.math.exp
import kotlin.math.ln

/**
 * 预测引擎 —— 这个 App 的核心。
 *
 * 输入:使用历史 + 当下情境信号
 * 输出:此刻最可能想用的 2~3 个工具,以及「为什么推荐它」的人话理由
 *
 * 设计取舍:
 *  - **纯本地**:零网络、零 LLM。省电、离线可用,隐私本身是卖点。
 *  - **可解释**:每条推荐都带理由。用户看到理由才会惊讶,而不是觉得随机。
 *  - **可纠正**:长按拉黑,立刻降权 30 天。
 *  - **算得快**:全是查表和乘加,200 个工具一次算完 < 5ms,可以每次进主页都重算。
 */
object PredictEngine {

    /** 一条推荐 */
    data class Suggestion(
        val route: String,
        val score: Double,
        /** 给用户看的理由,如「刚分享进来的图」「你常在早上用」 */
        val reason: String,
    )

    private const val HALF_LIFE_DAYS = 14.0
    private const val COLD_START_DAYS = 30.0
    /** 事件太少时不显示推荐 —— 宁可不出现,也不要出现得很蠢 */
    private const val MIN_EVENTS_TO_SHOW = 8

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        EventStore.init(context)
        KnownNetworks.init(context)
    }

    fun pruneOldEvents() = EventStore.prune()

    // ---- 记录 ----

    private var openedAt = 0L
    private var openedRoute: String? = null
    private var openedFromRecommend = false
    private var producedOutput = false

    /** 工具打开时调用 */
    fun onToolOpen(route: String, fromRecommend: Boolean = false) {
        // 上一个工具还没结算就先结算(用户直接跳到了另一个工具)
        flushPending()
        openedAt = System.currentTimeMillis()
        openedRoute = route
        openedFromRecommend = fromRecommend
        producedOutput = false
    }

    /** 工具里产生了实际输出(存了文件/复制了结果)—— 强正信号 */
    fun onProduced() { producedOutput = true }

    /** 工具关闭 / 切到别处时结算这次使用 */
    fun flushPending() {
        val route = openedRoute ?: return
        val start = openedAt
        openedRoute = null
        if (start == 0L) return

        val dwell = System.currentTimeMillis() - start
        val signals = SignalCollector.collect(appContext)
        KnownNetworks.note(signals.wifiHash)

        EventStore.record(
            EventStore.Event(
                route = route,
                ts = start,
                quarterBucket = signals.quarterBucket,
                dayOfWeek = signals.dayOfWeek,
                charging = signals.charging,
                net = signals.net.name,
                wifiHash = signals.wifiHash,
                bt = signals.bt.name,
                clip = signals.clip.name,
                prevRoute = EventStore.lastRoute(),
                dwellMs = dwell,
                produced = producedOutput,
                fromRecommend = openedFromRecommend,
            )
        )
    }

    // ---- 打分 ----

    /**
     * 算出此刻最该推荐的工具。
     *
     * @param allRoutes 全部候选工具路由
     * @param limit 要几个
     * @param offlineRoutes 断网时仍可用的工具(联网工具会被降权)
     */
    fun suggest(
        allRoutes: List<String>,
        limit: Int = 3,
        onlineRoutes: Set<String> = emptySet(),
    ): List<Suggestion> {
        // 冷启动早期:数据太少,宁可不出现
        if (EventStore.size() < MIN_EVENTS_TO_SHOW) return emptyList()

        val signals = SignalCollector.collect(appContext)
        val events = EventStore.all().filter { it.isMeaningful }
        val now = System.currentTimeMillis()
        val modes = ContextMatcher.match(signals)
        val lastRoute = EventStore.lastRoute()

        // 冷启动融合系数:数据越久,先验权重越低
        val alpha = (1.0 - EventStore.ageDays() / COLD_START_DAYS).coerceIn(0.0, 1.0)

        // 预计算:各工具的近期热度 / 时段命中 / 转移次数
        val recency = HashMap<String, Double>()
        val timeAffinity = HashMap<String, Double>()
        val transition = HashMap<String, Int>()
        val totalUse = HashMap<String, Int>()
        val contextHit = HashMap<String, Double>()

        events.forEach { e ->
            val decay = exp(-ln(2.0) * (now - e.ts) / (HALF_LIFE_DAYS * 86_400_000.0))
            val weight = decay * (if (e.produced) 1.4 else 1.0)
            recency[e.route] = (recency[e.route] ?: 0.0) + weight
            totalUse[e.route] = (totalUse[e.route] ?: 0) + 1

            // 时段亲和:当前 15 分钟桶 ±1(即 ±15 分钟)
            val dq = kotlin.math.abs(e.quarterBucket - signals.quarterBucket)
            val circular = kotlin.math.min(dq, 96 - dq)
            if (circular <= 2) {
                val sameDayBonus = if (e.dayOfWeek == signals.dayOfWeek) 1.5 else 1.0
                timeAffinity[e.route] = (timeAffinity[e.route] ?: 0.0) +
                    decay * sameDayBonus * (1.0 - circular * 0.3)
            }

            // 连带:上一个用的是同一个工具
            if (lastRoute != null && e.prevRoute == lastRoute) {
                transition[e.route] = (transition[e.route] ?: 0) + 1
            }

            // 情境命中:同样的充电/网络/蓝牙组合下用过
            var ctxMatch = 0
            if (e.charging == signals.charging) ctxMatch++
            if (e.net == signals.net.name) ctxMatch++
            if (e.bt == signals.bt.name && e.bt != "NONE") ctxMatch += 2
            if (e.clip == signals.clip.name && e.clip != "NONE") ctxMatch += 3
            if (ctxMatch > 0) {
                contextHit[e.route] = (contextHit[e.route] ?: 0.0) + decay * ctxMatch / 7.0
            }
        }

        val maxRecency = recency.values.maxOrNull() ?: 1.0
        val maxTime = timeAffinity.values.maxOrNull() ?: 1.0
        val maxTrans = (transition.values.maxOrNull() ?: 1).toDouble()
        val maxCtx = contextHit.values.maxOrNull() ?: 1.0
        val totalEvents = events.size.coerceAtLeast(1)

        // 情境模式命中的工具集合(整组提权)
        val modeRoutes = HashMap<String, ContextMatcher.Mode>()
        modes.forEach { m -> m.routes.forEach { r -> modeRoutes.putIfAbsent(r, m) } }

        val scored = allRoutes.mapNotNull { route ->
            val personal =
                0.20 * ((recency[route] ?: 0.0) / maxRecency) +
                0.25 * ((timeAffinity[route] ?: 0.0) / maxTime) +
                0.20 * ((transition[route] ?: 0).toDouble() / maxTrans) +
                0.25 * ((contextHit[route] ?: 0.0) / maxCtx) +
                0.10 * ((totalUse[route] ?: 0).toDouble() / totalEvents)

            val prior = ColdStartPriors.scoreFor(route, signals)
            var score = alpha * prior + (1 - alpha) * personal

            // 情境模式命中:权重最高的一项,这是「读心感」的来源
            val mode = modeRoutes[route]
            if (mode != null) {
                // 模式在列表里越靠前加得越多
                val idx = mode.routes.indexOf(route)
                score += 0.30 * (1.0 - idx * 0.12).coerceAtLeast(0.4)
            }

            // 断网时联网工具几乎不推荐 —— 别让用户点开一个必然报错的东西
            if (signals.net == SignalCollector.NetKind.NONE && route in onlineRoutes) {
                score *= 0.1
            }

            // 用户拉黑
            score *= EventStore.muteFactor(route)

            // 连续出现在推荐位但没被点:自己认账降权
            score *= ignoredFactor(route)

            if (score <= 0.001) null else route to score
        }

        return scored
            .sortedByDescending { it.second }
            .take(limit)
            .map { (route, score) ->
                Suggestion(route, score, reasonFor(route, signals, modeRoutes[route], timeAffinity, transition, lastRoute))
            }
    }

    /**
     * 生成人话理由。这是「读心感」的另一半 ——
     * 用户看到「刚分享进来的图」才会惊讶,看到干巴巴的图标只会觉得随机。
     */
    private fun reasonFor(
        route: String,
        signals: SignalCollector.Signals,
        mode: ContextMatcher.Mode?,
        timeAffinity: Map<String, Double>,
        transition: Map<String, Int>,
        lastRoute: String?,
    ): String {
        // 情境模式的理由最具体,优先
        mode?.let { return it.reason }

        // 连带:刚用完 A 常接着用 B
        if ((transition[route] ?: 0) >= 2 && lastRoute != null) {
            return "你常接着用这个"
        }

        // 时段
        if ((timeAffinity[route] ?: 0.0) > 0.3) {
            val hour = signals.quarterBucket / 4
            val period = when {
                hour < 6 -> "深夜"
                hour < 11 -> "早上"
                hour < 14 -> "中午"
                hour < 18 -> "下午"
                hour < 22 -> "晚上"
                else -> "睡前"
            }
            return if (signals.isWeekend) "周末${period}常用" else "${period}这会儿常用"
        }

        ColdStartPriors.reasonFor(route, signals)?.let { return it }
        return "最近常用"
    }

    // ---- 推荐质量自省 ----

    private const val IGNORE_PREFS = "predict_ignore"

    /** 记录这批推荐被展示了 */
    fun onSuggestionsShown(routes: List<String>) {
        val prefs = appContext.getSharedPreferences(IGNORE_PREFS, Context.MODE_PRIVATE)
        val ed = prefs.edit()
        routes.forEach { r -> ed.putInt("shown_$r", prefs.getInt("shown_$r", 0) + 1) }
        ed.apply()
    }

    /** 推荐位被点了 —— 清空该工具的忽略计数 */
    fun onSuggestionClicked(route: String) {
        appContext.getSharedPreferences(IGNORE_PREFS, Context.MODE_PRIVATE)
            .edit().remove("shown_$route").apply()
    }

    /** 连续展示 4 次以上没被点,说明猜错了,降权 */
    private fun ignoredFactor(route: String): Double {
        val shown = appContext.getSharedPreferences(IGNORE_PREFS, Context.MODE_PRIVATE)
            .getInt("shown_$route", 0)
        return when {
            shown >= 8 -> 0.4
            shown >= 4 -> 0.7
            else -> 1.0
        }
    }

    // ---- 用户控制 ----

    fun mute(route: String) = EventStore.mute(route)
    fun unmute(route: String) = EventStore.unmute(route)
    fun mutedRoutes(): Set<String> = EventStore.mutedRoutes()

    /** 设置页「清空学习记录」 */
    fun clearLearningData() {
        EventStore.clearAll()
        appContext.getSharedPreferences(IGNORE_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        appContext.getSharedPreferences("predict_networks", Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** 设置页开关 */
    private const val SETTINGS_PREFS = "predict_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_CLIPBOARD = "clipboard_sniff"

    var enabled: Boolean
        get() = appContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
        set(v) = appContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, v).apply()

    /** 剪贴板嗅探开关。默认开,但用户可以关掉 */
    var clipboardSniffing: Boolean
        get() = appContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CLIPBOARD, true)
        set(v) = appContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CLIPBOARD, v).apply()

    /** 学习进度,给设置页展示:「已经了解你 N 次使用」 */
    fun learningProgress(): Pair<Int, Int> = EventStore.size() to EventStore.ageDays()
}
