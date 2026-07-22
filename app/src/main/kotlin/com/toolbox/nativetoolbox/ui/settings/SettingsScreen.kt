package com.toolbox.nativetoolbox.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.BuildConfig
import com.toolbox.nativetoolbox.data.prefs.SettingsStore
import com.toolbox.nativetoolbox.util.UpdateChecker
import com.toolbox.nativetoolbox.ui.components.CheckRow
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.NavRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(settings: SettingsStore) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.System)
    var checking by remember { mutableStateOf(false) }

    fun checkUpdate() {
        if (checking) return
        checking = true
        scope.launch {
            val result = UpdateChecker.check()
            checking = false
            val info = result.getOrNull()
            when {
                result.isFailure -> Toast.makeText(context, "检查失败,稍后再试", Toast.LENGTH_SHORT).show()
                info == null -> Toast.makeText(context, "已是最新版本", Toast.LENGTH_SHORT).show()
                else -> {
                    Toast.makeText(context, "发现新版本 ${info.tag},正在打开下载页", Toast.LENGTH_SHORT).show()
                    val url = info.apkUrl.ifBlank { info.htmlUrl }
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.groupedBackground),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Text(
                "设置",
                Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                style = MaterialTheme.typography.displayLarge,
                color = palette.label
            )
        }

        item { SectionHeader("外观") }
        item {
            GroupedCard {
                CheckRow("跟随系统", themeMode == ThemeMode.System) {
                    scope.launch { settings.setThemeMode(ThemeMode.System) }
                }
                RowDivider()
                CheckRow("浅色", themeMode == ThemeMode.Light) {
                    scope.launch { settings.setThemeMode(ThemeMode.Light) }
                }
                RowDivider()
                CheckRow("深色", themeMode == ThemeMode.Dark) {
                    scope.launch { settings.setThemeMode(ThemeMode.Dark) }
                }
            }
        }

        item { SectionHeader("关于") }
        item {
            GroupedCard {
                NavRow(
                    "版本",
                    icon = Icons.Rounded.Info,
                    iconTint = palette.accent,
                    value = BuildConfig.VERSION_NAME,
                    showChevron = false
                )
                RowDivider(startIndent = 58.dp)
                NavRow(
                    if (checking) "检查中…" else "检查更新",
                    icon = Icons.Rounded.SystemUpdate,
                    iconTint = palette.green,
                    onClick = { checkUpdate() }
                )
                RowDivider(startIndent = 58.dp)
                NavRow(
                    "开源仓库",
                    icon = Icons.Rounded.Code,
                    iconTint = palette.gray,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Beicho/native-toolbox"))
                        )
                    }
                )
                RowDivider(startIndent = 58.dp)
                NavRow(
                    "Liquid Glass by Kyant0/AndroidLiquidGlass",
                    icon = Icons.Rounded.Star,
                    iconTint = palette.yellow,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kyant0/AndroidLiquidGlass"))
                        )
                    }
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 32.dp, vertical = 16.dp)) {
                Text(
                    "Astro Kit 星辰之匣 · Jetpack Compose 原生打造",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.tertiaryLabel
                )
            }
        }
    }
}
