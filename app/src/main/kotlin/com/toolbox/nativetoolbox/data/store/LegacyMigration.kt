package com.toolbox.nativetoolbox.data.store

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 旧数据迁移:把「空格分隔文本行塞 SharedPreferences」的老格式搬进 AstroStore。
 *
 * 老格式的问题:备注里打个空格就串行、没有 id 改一条要重写整串、9 个工具 9 套格式。
 * 迁移是一次性的,做完打标记。解析失败的行不丢弃 —— 原样存进 note 字段,
 * 用户至少还能看到自己写过什么,而不是静默消失。
 */
object LegacyMigration {

    private const val FLAG_PREFS = "astro_migration"
    private const val FLAG_KEY = "v1_done"
    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun runIfNeeded(context: Context) {
        val flags = context.getSharedPreferences(FLAG_PREFS, Context.MODE_PRIVATE)
        if (flags.getBoolean(FLAG_KEY, false)) return

        runCatching { migrateBookkeeping(context) }
        runCatching { migrateCountdown(context) }
        runCatching { migrateNotes(context) }
        runCatching { migrateClipShelf(context) }
        runCatching { migrateTemplates(context) }
        runCatching { migrateHealth(context) }
        runCatching { migratePeriod(context) }

        flags.edit().putBoolean(FLAG_KEY, true).apply()
    }

    /** 老格式:每行「日期 金额 分类 备注」,日期可省略 */
    private fun migrateBookkeeping(context: Context) {
        val raw = context.getSharedPreferences("tool_bookkeeping", Context.MODE_PRIVATE)
            .getString("raw", "") ?: return
        if (raw.isBlank()) return
        val categories = listOf("餐饮", "交通", "购物", "居住", "娱乐", "医疗", "学习", "其他", "收入")
        raw.lines().forEach { line ->
            val parts = line.trim().split(Regex("[\\s,，]+")).filter { it.isNotBlank() }
            if (parts.size < 2) return@forEach
            var idx = 0
            var dateIso = iso.format(Date())
            val maybeDate = parts[0].replace('/', '-').replace('.', '-')
            runCatching { iso.parse(maybeDate) }.getOrNull()?.let {
                dateIso = iso.format(it); idx = 1
            }
            val amount = parts.getOrNull(idx)?.toDoubleOrNull() ?: return@forEach
            val cat = parts.getOrNull(idx + 1)?.takeIf { categories.contains(it) }
            val note = parts.drop(idx + if (cat != null) 2 else 1).joinToString(" ")
            AstroStore.add(AstroStore.Collection.BOOKKEEPING) {
                put("amount", amount)
                put("category", cat ?: "其他")
                put("note", note)
                put("dateIso", dateIso)
            }
        }
    }

    /** 老格式:每行「名称 日期」 */
    private fun migrateCountdown(context: Context) {
        val raw = context.getSharedPreferences("tool_countdown_day", Context.MODE_PRIVATE)
            .getString("raw", "")
            ?: context.getSharedPreferences("countdown_day", Context.MODE_PRIVATE).getString("events", "")
            ?: return
        if (raw.isBlank()) return
        raw.lines().forEach { line ->
            val parts = line.split(Regex("[\\s,，]+")).filter { it.isNotBlank() }
            if (parts.size < 2) return@forEach
            val dateText = parts.last().replace('/', '-').replace('.', '-')
            val d = runCatching { iso.parse(dateText) }.getOrNull() ?: return@forEach
            AstroStore.add(AstroStore.Collection.COUNTDOWN) {
                put("title", parts.dropLast(1).joinToString(" ").ifBlank { "倒数日" })
                put("dateIso", iso.format(d))
            }
        }
    }

    /** 笔记:自由文本 + 待办清单(带 [ ] / [x] 前缀) */
    private fun migrateNotes(context: Context) {
        val prefs = context.getSharedPreferences("tool_notes", Context.MODE_PRIVATE)
        prefs.getString("note", "")?.takeIf { it.isNotBlank() }?.let { text ->
            AstroStore.add(AstroStore.Collection.NOTES) { put("text", text) }
        }
        prefs.getString("listRaw", "")?.takeIf { it.isNotBlank() }?.lines()?.forEach { line ->
            val t = line.trim()
            if (t.isBlank()) return@forEach
            val done = t.startsWith("[x]") || t.startsWith("[X]")
            val text = t.removePrefix("[x]").removePrefix("[X]").removePrefix("[ ]").trim()
            if (text.isNotBlank()) {
                AstroStore.add(AstroStore.Collection.TODO) {
                    put("text", text)
                    put("done", done)
                }
            }
        }
    }

    private fun migrateClipShelf(context: Context) {
        val sep = "\u0002"
        val raw = context.getSharedPreferences("tool_clipshelf", Context.MODE_PRIVATE)
            .getString("shelf", "") ?: return
        raw.split(sep).filter { it.isNotBlank() }.forEach { text ->
            AstroStore.add(AstroStore.Collection.CLIP_SHELF) { put("text", text) }
        }
    }

    /** 老格式:名称\n模板正文,条目间用 \u0002 分隔 */
    private fun migrateTemplates(context: Context) {
        val sep = "\u0002"
        val raw = context.getSharedPreferences("tool_texttemplate", Context.MODE_PRIVATE)
            .getString("savedTemplates", "") ?: return
        raw.split(sep).filter { it.isNotBlank() }.forEach { entry ->
            AstroStore.add(AstroStore.Collection.TEMPLATE) {
                put("name", entry.lineSequence().firstOrNull() ?: "未命名")
                put("body", entry.substringAfter('\n', ""))
            }
        }
    }

    /** 健康记录老格式是 JSON 数组,直接转 */
    private fun migrateHealth(context: Context) {
        val raw = context.getSharedPreferences("health_record", Context.MODE_PRIVATE)
            .getString("records", "[]") ?: return
        runCatching {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                AstroStore.add(AstroStore.Collection.HEALTH) {
                    put("kind", o.optInt("k"))
                    put("v1", o.optDouble("a"))
                    put("v2", o.optDouble("b", 0.0))
                    put("recordedAt", o.optLong("t"))
                }
            }
        }
    }

    private fun migratePeriod(context: Context) {
        val raw = context.getSharedPreferences("period", Context.MODE_PRIVATE)
            .getString("starts", "[]") ?: return
        runCatching {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                AstroStore.add(AstroStore.Collection.PERIOD) {
                    put("startAt", arr.getLong(i))
                }
            }
        }
    }
}
