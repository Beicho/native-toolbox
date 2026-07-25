package com.toolbox.nativetoolbox.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 自动落盘的页面状态:赋值即写盘,重启不丢。
 * 用于笔记、记账这类用户数据;敏感信息(密码等)不要用。
 *
 * 【为什么不用 LaunchedEffect 落盘】
 * 早先的实现是 `LaunchedEffect(state.value) { 写盘 }`。一次交互里连续改两个
 * 持久化状态(如剪贴板暂存架的 add() 同时改 shelf 和 input)会反复重启协程,
 * 在 LazyColumn 重组期写状态 → 无限重组 → ANR/闪退。
 * 现在写盘发生在 setValue 内部(事件回调里,不在组合期),彻底避开这个坑。
 */
private class PrefState<T>(
    initial: T,
    private val write: (T) -> Unit,
) : MutableState<T> {
    private val backing = mutableStateOf(initial)

    override var value: T
        get() = backing.value
        set(newValue) {
            if (backing.value != newValue) {
                backing.value = newValue
                write(newValue)
            }
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}

@Composable
fun rememberToolPrefs(name: String): SharedPreferences {
    val context = LocalContext.current
    return remember { context.getSharedPreferences("tool_$name", Context.MODE_PRIVATE) }
}

@Composable
fun rememberPrefString(prefs: SharedPreferences, key: String, default: String = ""): MutableState<String> =
    remember(prefs, key) {
        PrefState(prefs.getString(key, default) ?: default) { v ->
            prefs.edit().putString(key, v).apply()
        }
    }

@Composable
fun rememberPrefInt(prefs: SharedPreferences, key: String, default: Int = 0): MutableState<Int> =
    remember(prefs, key) {
        PrefState(prefs.getInt(key, default)) { v ->
            prefs.edit().putInt(key, v).apply()
        }
    }

@Composable
fun rememberPrefBool(prefs: SharedPreferences, key: String, default: Boolean = false): MutableState<Boolean> =
    remember(prefs, key) {
        PrefState(prefs.getBoolean(key, default)) { v ->
            prefs.edit().putBoolean(key, v).apply()
        }
    }
