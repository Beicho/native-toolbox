package com.toolbox.nativetoolbox.data.store

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 结构化数据仓库:替代之前「一坨空格分隔文本行塞 SharedPreferences」的做法。
 *
 * 【为什么不用 Room】
 * Room 需要引 KSP,而 KSP 版本与 Kotlin 版本严格绑定,一旦不匹配整个构建挂掉。
 * 本项目的数据量(单用户、每类几千条上限)用不到 SQL 索引和复杂查询,
 * Room 带来的构建风险大于收益。这里用「JSON 文件 + 内存缓存 + 原子写入」,
 * 零新依赖,而且导出功能天生就是现成的 —— 存的就是 JSON。
 *
 * 【统一 schema】
 * 每条记录都有 id / createdAt / updatedAt / deletedAt(软删除),
 * 这样改一条不用重写整个字符串,预测引擎也有干净稳定的输入。
 */
object AstroStore {

    /** 数据集合名。新增类型在这里登记,导出/导入/清空会自动覆盖 */
    enum class Collection(val key: String, val displayName: String, val limit: Int) {
        BOOKKEEPING("bookkeeping", "记账", 5000),
        COUNTDOWN("countdown", "倒数日", 200),
        TODO("todo", "待办", 1000),
        NOTES("notes", "笔记", 1000),
        HEALTH("health", "健康记录", 3000),
        PERIOD("period", "经期记录", 500),
        HABIT("habit", "习惯打卡", 3000),
        CLIP_SHELF("clip_shelf", "剪贴板暂存", 100),
        TEMPLATE("template", "文本模板", 100),
    }

    private const val DIR = "astro-store"
    private const val SCHEMA_VERSION = 1

    private lateinit var appContext: Context
    private val cache = HashMap<String, MutableList<Record>>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 一条记录。业务字段全放 [fields],不同集合自定义键名。
     * 例:记账 = {amount, category, note, dateIso}
     */
    data class Record(
        val id: String,
        val createdAt: Long,
        var updatedAt: Long,
        var deletedAt: Long = 0L,
        val fields: JSONObject,
    ) {
        val isDeleted: Boolean get() = deletedAt > 0

        fun str(key: String, default: String = ""): String = fields.optString(key, default)
        fun num(key: String, default: Double = 0.0): Double = fields.optDouble(key, default)
        fun int(key: String, default: Int = 0): Int = fields.optInt(key, default)
        fun bool(key: String, default: Boolean = false): Boolean = fields.optBoolean(key, default)

        fun toJson(): JSONObject = JSONObject()
            .put("id", id)
            .put("c", createdAt)
            .put("u", updatedAt)
            .apply { if (deletedAt > 0) put("d", deletedAt) }
            .put("f", fields)

        companion object {
            fun fromJson(o: JSONObject): Record? = runCatching {
                Record(
                    id = o.getString("id"),
                    createdAt = o.optLong("c"),
                    updatedAt = o.optLong("u"),
                    deletedAt = o.optLong("d", 0L),
                    fields = o.optJSONObject("f") ?: JSONObject(),
                )
            }.getOrNull()
        }
    }

    private fun file(c: Collection): File {
        val dir = File(appContext.filesDir, DIR).apply { if (!exists()) mkdirs() }
        return File(dir, "${c.key}.json")
    }

    @Synchronized
    private fun load(c: Collection): MutableList<Record> {
        cache[c.key]?.let { return it }
        val list = mutableListOf<Record>()
        val f = file(c)
        if (f.exists()) {
            runCatching {
                val root = JSONObject(f.readText())
                val arr = root.optJSONArray("records") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    Record.fromJson(arr.getJSONObject(i))?.let { list.add(it) }
                }
            }
        }
        cache[c.key] = list
        return list
    }

    /** 原子写入:先写临时文件再改名,避免写一半崩溃导致数据全丢 */
    @Synchronized
    private fun persist(c: Collection) {
        val list = cache[c.key] ?: return
        // 超过上限时,丢掉最老的已删除记录,再丢最老的正常记录
        if (list.size > c.limit) {
            val alive = list.filterNot { it.isDeleted }.sortedByDescending { it.createdAt }
            list.clear()
            list.addAll(alive.take(c.limit))
        }
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val root = JSONObject()
            .put("v", SCHEMA_VERSION)
            .put("collection", c.key)
            .put("records", arr)
        runCatching {
            val target = file(c)
            val tmp = File(target.parentFile, "${c.key}.tmp")
            tmp.writeText(root.toString())
            if (target.exists()) target.delete()
            tmp.renameTo(target)
        }
    }

    // ---- 公开 API ----

    /** 全部未删除记录,按创建时间倒序 */
    fun all(c: Collection): List<Record> =
        load(c).filterNot { it.isDeleted }.sortedByDescending { it.createdAt }

    fun count(c: Collection): Int = load(c).count { !it.isDeleted }

    fun get(c: Collection, id: String): Record? =
        load(c).firstOrNull { it.id == id && !it.isDeleted }

    fun add(c: Collection, build: JSONObject.() -> Unit): Record {
        val now = System.currentTimeMillis()
        val rec = Record(
            id = "${now}_${(now % 100000)}_${load(c).size}",
            createdAt = now,
            updatedAt = now,
            fields = JSONObject().apply(build),
        )
        load(c).add(rec)
        persist(c)
        return rec
    }

    fun update(c: Collection, id: String, edit: JSONObject.() -> Unit): Boolean {
        val rec = load(c).firstOrNull { it.id == id } ?: return false
        rec.fields.edit()
        rec.updatedAt = System.currentTimeMillis()
        persist(c)
        return true
    }

    /** 软删除:保留记录但标记,便于「撤销」和同步 */
    fun remove(c: Collection, id: String): Boolean {
        val rec = load(c).firstOrNull { it.id == id } ?: return false
        rec.deletedAt = System.currentTimeMillis()
        persist(c)
        return true
    }

    fun undoRemove(c: Collection, id: String): Boolean {
        val rec = load(c).firstOrNull { it.id == id } ?: return false
        rec.deletedAt = 0L
        rec.updatedAt = System.currentTimeMillis()
        persist(c)
        return true
    }

    fun clear(c: Collection) {
        load(c).clear()
        persist(c)
    }

    // ---- 导出 / 导入 ----

    /** 导出全部数据为一个 JSON(用户换手机、备份用) */
    fun exportAll(): String {
        val root = JSONObject()
            .put("app", "AstroKit")
            .put("v", SCHEMA_VERSION)
            .put("exportedAt", System.currentTimeMillis())
        val data = JSONObject()
        Collection.entries.forEach { c ->
            val arr = JSONArray()
            load(c).filterNot { it.isDeleted }.forEach { arr.put(it.toJson()) }
            if (arr.length() > 0) data.put(c.key, arr)
        }
        root.put("data", data)
        return root.toString(2)
    }

    /**
     * 导入。merge=true 时按 id 去重合并(保留 updatedAt 较新的),
     * false 时整体覆盖。
     */
    fun importAll(json: String, merge: Boolean = true): Result<Int> = runCatching {
        val root = JSONObject(json)
        if (root.optString("app") != "AstroKit") error("这不是 Astro Kit 的备份文件")
        val data = root.optJSONObject("data") ?: error("备份文件里没有数据")
        var imported = 0
        Collection.entries.forEach { c ->
            val arr = data.optJSONArray(c.key) ?: return@forEach
            val existing = load(c)
            if (!merge) existing.clear()
            val byId = existing.associateBy { it.id }.toMutableMap()
            for (i in 0 until arr.length()) {
                val rec = Record.fromJson(arr.getJSONObject(i)) ?: continue
                val old = byId[rec.id]
                if (old == null) {
                    existing.add(rec)
                    byId[rec.id] = rec
                    imported++
                } else if (rec.updatedAt > old.updatedAt) {
                    existing.remove(old)
                    existing.add(rec)
                    byId[rec.id] = rec
                    imported++
                }
            }
            persist(c)
        }
        imported
    }

    /** 供预测引擎用的轻量聚合(不暴露原始记录) */
    object Stats {
        /** 最近 n 天内该集合的记录条数 */
        fun recentCount(c: Collection, days: Int): Int {
            val since = System.currentTimeMillis() - days * 86_400_000L
            return load(c).count { !it.isDeleted && it.createdAt >= since }
        }

        /** 距今最近一条记录的天数,没有记录返回 null */
        fun daysSinceLast(c: Collection): Int? {
            val last = load(c).filterNot { it.isDeleted }.maxByOrNull { it.createdAt } ?: return null
            return ((System.currentTimeMillis() - last.createdAt) / 86_400_000L).toInt()
        }
    }
}
