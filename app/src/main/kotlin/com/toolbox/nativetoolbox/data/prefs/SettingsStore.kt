package com.toolbox.nativetoolbox.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.toolbox.nativetoolbox.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val themeModeKey = intPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.entries.getOrElse(prefs[themeModeKey] ?: 0) { ThemeMode.System }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.ordinal }
    }
}
