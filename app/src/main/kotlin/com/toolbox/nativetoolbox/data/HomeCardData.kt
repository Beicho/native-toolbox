package com.toolbox.nativetoolbox.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 主页动态卡片数据源 */
object HomeCardData {
    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    data class CountdownItem(val name: String, val dateIso: String, val daysLeft: Int)
    data class BirthdayItem(val name: String, val dateIso: String, val daysUntil: Int)
    data class TodoItem(val text: String, val done: Boolean)

    private fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun daysUntil(dateIso: String): Int? {
        val target = runCatching { iso.parse(dateIso) }.getOrNull() ?: return null
        return Math.round((target.time - todayStart()) / 86_400_000.0).toInt()
    }

    /** 获取最近的倒数日(未来 90 天内) */
    fun getUpcomingCountdowns(context: Context): List<CountdownItem> {
        val prefs = context.getSharedPreferences("countdown_day", Context.MODE_PRIVATE)
        val raw = prefs.getString("events", "") ?: ""
        return raw.lines().mapNotNull { line ->
            val parts = line.split(Regex("[\\s,，]+")).filter { it.isNotBlank() }
            if (parts.size < 2) return@mapNotNull null
            val dateText = parts.last().replace('/', '-').replace('.', '-')
            val dateIso = runCatching {
                val d = iso.parse(dateText) ?: return@runCatching null
                iso.format(d)
            }.getOrNull() ?: return@mapNotNull null
            val name = parts.dropLast(1).joinToString(" ")
            val days = daysUntil(dateIso) ?: return@mapNotNull null
            if (days in 0..90) CountdownItem(name.ifBlank { "倒数日" }, dateIso, days) else null
        }.sortedBy { it.daysLeft }.take(3)
    }

    /** 获取待办事项(只取前 5 条) */
    fun getTodos(context: Context): List<TodoItem> {
        val prefs = context.getSharedPreferences("todo", Context.MODE_PRIVATE)
        val raw = prefs.getString("items", "") ?: ""
        return raw.lines().take(5).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val done = line.startsWith("[x]") || line.startsWith("[X]")
            val text = line.removePrefix("[x]").removePrefix("[X]").removePrefix("[ ]").trim()
            if (text.isBlank()) null else TodoItem(text, done)
        }
    }

    /** 历史上的今天(从 prefs 读取) */
    fun getOnThisDay(context: Context): String? {
        val prefs = context.getSharedPreferences("onthisday", Context.MODE_PRIVATE)
        val raw = prefs.getString("events", "") ?: ""
        return raw.lines().firstOrNull()?.takeIf { it.isNotBlank() }
    }
}
