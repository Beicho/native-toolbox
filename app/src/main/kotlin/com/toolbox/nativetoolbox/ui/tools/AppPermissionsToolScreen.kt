package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.NavRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** 用户最该关心的敏感权限，按危险程度排在前面 */
private val sensitivePermissions = listOf(
    "android.permission.CAMERA" to "相机",
    "android.permission.RECORD_AUDIO" to "麦克风",
    "android.permission.ACCESS_FINE_LOCATION" to "精确定位",
    "android.permission.ACCESS_COARSE_LOCATION" to "粗略定位",
    "android.permission.ACCESS_BACKGROUND_LOCATION" to "后台定位",
    "android.permission.READ_CONTACTS" to "读通讯录",
    "android.permission.WRITE_CONTACTS" to "改通讯录",
    "android.permission.READ_SMS" to "读短信",
    "android.permission.RECEIVE_SMS" to "收短信",
    "android.permission.READ_CALL_LOG" to "读通话记录",
    "android.permission.CALL_PHONE" to "直接打电话",
    "android.permission.READ_CALENDAR" to "读日历",
    "android.permission.BODY_SENSORS" to "身体传感器",
    "android.permission.ACTIVITY_RECOGNITION" to "运动识别",
    "android.permission.READ_MEDIA_IMAGES" to "读照片",
    "android.permission.READ_MEDIA_VIDEO" to "读视频",
    "android.permission.READ_EXTERNAL_STORAGE" to "读存储",
    "android.permission.MANAGE_EXTERNAL_STORAGE" to "全盘访问",
    "android.permission.SYSTEM_ALERT_WINDOW" to "悬浮窗",
    "android.permission.QUERY_ALL_PACKAGES" to "查看已装应用"
)

private class AppEntry(
    val label: String,
    val packageName: String,
    val system: Boolean,
    val granted: List<String>,
    val requested: Int
)

private fun loadApps(context: Context): List<AppEntry> = runCatching {
    val pm = context.packageManager
    @Suppress("DEPRECATION")
    val packages: List<PackageInfo> = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    packages.mapNotNull { info ->
        val app = info.applicationInfo ?: return@mapNotNull null
        val requested = info.requestedPermissions ?: emptyArray()
        val flags = info.requestedPermissionsFlags ?: IntArray(requested.size)
        val granted = requested.mapIndexedNotNull { index, permission ->
            val isGranted = index < flags.size &&
                (flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            val label = sensitivePermissions.firstOrNull { it.first == permission }?.second
            if (isGranted && label != null) label else null
        }
        AppEntry(
            label = pm.getApplicationLabel(app).toString(),
            packageName = info.packageName,
            system = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            granted = granted,
            requested = requested.size
        )
    }
}.getOrDefault(emptyList())

@Composable
fun AppPermissionsToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current

    val apps = remember { loadApps(context) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable { mutableStateOf(0) }
    var permissionFilter by rememberSaveable { mutableStateOf(0) }

    val visible = apps
        .filter { if (scope == 0) !it.system else true }
        .filter { keyword.isBlank() || it.label.contains(keyword.trim(), true) || it.packageName.contains(keyword.trim(), true) }
        .filter {
            permissionFilter == 0 || it.granted.contains(
                listOf("", "相机", "麦克风", "精确定位", "读通讯录")[permissionFilter]
            )
        }
        .sortedByDescending { it.granted.size }

    val stats = sensitivePermissions.take(6).map { (_, label) ->
        label to apps.count { it.granted.contains(label) }
    }

    ToolScaffold {
        item { SectionHeader("概览") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("已装应用", apps.size.toString(), Modifier.weight(1f))
                        StatCell("非系统", apps.count { !it.system }.toString(), Modifier.weight(1f))
                        StatCell(
                            "拿了敏感权限",
                            apps.count { it.granted.isNotEmpty() }.toString(),
                            Modifier.weight(1f)
                        )
                    }
                    if (apps.isEmpty()) {
                        Text(
                            "读不到已安装应用列表。Android 11 起系统限制了这个能力，部分机型会返回空。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.orange
                        )
                    }
                }
            }
        }
        if (apps.isNotEmpty()) {
            item { SectionHeader("哪些权限被拿得最多") }
            item {
                GroupedCard {
                    stats.forEachIndexed { index, (label, count) ->
                        KeyValueRow(label, count.toString() + " 个应用", copyable = false)
                        if (index != stats.lastIndex) RowDivider()
                    }
                }
            }
            item { SectionHeader("筛选") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = listOf("只看第三方", "包含系统应用"),
                            selectedIndex = scope,
                            onSelected = { scope = it }
                        )
                        SegmentedPicker(
                            options = listOf("全部", "相机", "麦克风", "定位", "通讯录"),
                            selectedIndex = permissionFilter,
                            onSelected = { permissionFilter = it }
                        )
                        IosTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            placeholder = "搜索应用名或包名"
                        )
                    }
                }
            }
            item { SectionHeader("应用列表（" + visible.size + "）") }
            item {
                GroupedCard {
                    visible.take(60).forEachIndexed { index, app ->
                        NavRow(
                            title = app.label,
                            value = if (app.granted.isEmpty()) "无敏感权限"
                            else app.granted.take(3).joinToString("、") +
                                (if (app.granted.size > 3) " 等 " + app.granted.size + " 项" else ""),
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", app.packageName, null)
                                        }
                                    )
                                }
                            }
                        )
                        if (index != visible.take(60).lastIndex) RowDivider()
                    }
                }
            }
            if (visible.size > 60) {
                item {
                    GroupedCard {
                        CardPadding {
                            Text(
                                "还有 " + (visible.size - 60) + " 个没显示，用搜索缩小范围。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.tertiaryLabel
                            )
                        }
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "点任意一个应用会跳到系统的应用详情页，在那里可以直接收回权限。\n\n" +
                            "这里只读取系统公开的权限授予状态，不修改任何设置，也不联网上报。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
