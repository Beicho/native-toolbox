package com.toolbox.nativetoolbox.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 自动落盘的页面状态:值一变就写进 SharedPreferences,重启不丢。
 * 用于笔记、记账这类用户数据;敏感信息(密码等)不要用。
 */
@Composable
fun rememberToolPrefs(name: String): SharedPreferences {
    val context = LocalContext.current
    return remember { context.getSharedPreferences("tool_$name", Context.MODE_PRIVATE) }
}

@Composable
fun rememberPrefString(prefs: SharedPreferences, key: String, default: String = ""): MutableState<String> {
    val state = remember { mutableStateOf(prefs.getString(key, default) ?: default) }
    LaunchedEffect(state.value) { prefs.edit().putString(key, state.value).apply() }
    return state
}

@Composable
fun rememberPrefInt(prefs: SharedPreferences, key: String, default: Int = 0): MutableState<Int> {
    val state = remember { mutableStateOf(prefs.getInt(key, default)) }
    LaunchedEffect(state.value) { prefs.edit().putInt(key, state.value).apply() }
    return state
}

@Composable
fun rememberPrefBool(prefs: SharedPreferences, key: String, default: Boolean = false): MutableState<Boolean> {
    val state = remember { mutableStateOf(prefs.getBoolean(key, default)) }
    LaunchedEffect(state.value) { prefs.edit().putBoolean(key, state.value).apply() }
    return state
}
