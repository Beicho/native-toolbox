package com.toolbox.nativetoolbox.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.usageDataStore by preferencesDataStore(name = "usage")

/** 工具使用计数(常用置顶)+ 更新检查缓存 */
class UsageStore(private val context: Context) {

    /** route -> 使用次数 */
    val usageCounts: Flow<Map<String, Int>> = context.usageDataStore.data.map { prefs ->
        prefs.asMap().entries
            .filter { it.key.name.startsWith("use_") }
            .associate { it.key.name.removePrefix("use_") to (it.value as? Int ?: 0) }
    }

    suspend fun recordUse(route: String) {
        val key = intPreferencesKey("use_$route")
        context.usageDataStore.edit { it[key] = (it[key] ?: 0) + 1 }
    }

    private val lastCheckKey = longPreferencesKey("last_update_check")
    private val latestTagKey = stringPreferencesKey("latest_tag")

    val cachedLatestTag: Flow<String?> = context.usageDataStore.data.map { it[latestTagKey] }

    suspend fun lastCheckAt(): Long {
        return context.usageDataStore.data.first()[lastCheckKey] ?: 0L
    }

    suspend fun saveCheckResult(tag: String) {
        context.usageDataStore.edit {
            it[lastCheckKey] = System.currentTimeMillis()
            it[latestTagKey] = tag
        }
    }
}
