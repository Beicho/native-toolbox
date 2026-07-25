package com.toolbox.nativetoolbox.data

import android.content.Context
import com.toolbox.nativetoolbox.data.store.AstroStore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 主页活数据卡片的数据源。
 *
 * 全部走 AstroStore(结构化存储),不再解析「空格分隔文本行」。
 * 卡片支持就地操作 —— 打勾、记一笔、+1 都直接写回,不用跳进工具页。
 * 这是「每天开三次、每次十秒」的关键。
 */
object HomeCardData {

    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    data class CountdownItem(val id: String, val name: String, val dateIso: String, val daysLeft: Int)
    data class TodoItem(val id: String, val text: String, val done: Boolean)
    data class BookkeepSummary(val monthTotal: Double, val todayTotal: Double, val count: Int)

    private fun todayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun daysUntil(dateIso: String): Int? {
        val target = runCatching { iso.parse(dateIso) }.getOrNull() ?: return null
        return Math.round((target.time - todayStart()) / 86_400_000.0).toInt()
    }

    // ---- 倒数日 ----

    /** 未来 90 天内最近的几个倒数日 */
    fun getUpcomingCountdowns(context: Context): List<CountdownItem> =
        AstroStore.all(AstroStore.Collection.COUNTDOWN).mapNotNull { r ->
            val dateIso = r.str("dateIso").ifBlank { return@mapNotNull null }
            val days = daysUntil(dateIso) ?: return@mapNotNull null
            if (days in 0..90) {
                CountdownItem(r.id, r.str("title").ifBlank { "倒数日" }, dateIso, days)
            } else null
        }.sortedBy { it.daysLeft }.take(5)

    // ---- 待办 ----

    fun getTodos(context: Context): List<TodoItem> =
        AstroStore.all(AstroStore.Collection.TODO)
            .sortedWith(compareBy({ it.bool("done") }, { -it.createdAt }))
            .take(6)
            .map { TodoItem(it.id, it.str("text"), it.bool("done")) }

    /** 卡片上直接打勾 —— 不跳转 */
    fun toggleTodo(id: String, done: Boolean) {
        AstroStore.update(AstroStore.Collection.TODO, id) { put("done", done) }
    }

    fun addTodo(text: String) {
        if (text.isBlank()) return
        AstroStore.add(AstroStore.Collection.TODO) {
            put("text", text.trim())
            put("done", false)
        }
    }

    // ---- 记账 ----

    fun getBookkeepSummary(context: Context): BookkeepSummary? {
        val records = AstroStore.all(AstroStore.Collection.BOOKKEEPING)
        if (records.isEmpty()) return null
        val cal = Calendar.getInstance()
        val monthPrefix = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val todayIso = iso.format(cal.time)
        var month = 0.0
        var today = 0.0
        records.forEach { r ->
            // 收入不计入支出统计
            if (r.str("category") == "收入") return@forEach
            val d = r.str("dateIso")
            val amt = r.num("amount")
            if (d.startsWith(monthPrefix)) month += amt
            if (d == todayIso) today += amt
        }
        return BookkeepSummary(month, today, records.size)
    }

    /** 卡片上直接记一笔 */
    fun addExpense(amount: Double, category: String, note: String = "") {
        if (amount <= 0) return
        AstroStore.add(AstroStore.Collection.BOOKKEEPING) {
            put("amount", amount)
            put("category", category)
            put("note", note)
            put("dateIso", iso.format(Calendar.getInstance().time))
        }
    }

    // ---- 历史上的今天(仍走缓存,不是本地数据) ----

    fun getOnThisDay(context: Context): String? {
        val prefs = context.getSharedPreferences("onthisday", Context.MODE_PRIVATE)
        // 缓存要是今天写的才算数 —— 显示昨天的「历史上的今天」很滑稽
        val cachedDate = prefs.getString("date", "")
        val today = iso.format(Calendar.getInstance().time)
        if (cachedDate != today) return null
        return prefs.getString("events", "")?.lines()?.firstOrNull()?.takeIf { it.isNotBlank() }
    }
}
