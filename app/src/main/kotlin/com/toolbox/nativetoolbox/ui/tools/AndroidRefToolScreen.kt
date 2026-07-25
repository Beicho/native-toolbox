package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private class Ref(val name: String, val value: String, val note: String)

private val densityBuckets = listOf(
    Triple("ldpi", 120, 0.75),
    Triple("mdpi", 160, 1.0),
    Triple("hdpi", 240, 1.5),
    Triple("xhdpi", 320, 2.0),
    Triple("xxhdpi", 480, 3.0),
    Triple("xxxhdpi", 640, 4.0)
)

private val apiLevels = listOf(
    Ref("36", "Android 16", "2025 年发布"),
    Ref("35", "Android 15", "VanillaIceCream"),
    Ref("34", "Android 14", "UpsideDownCake，前台服务类型强制"),
    Ref("33", "Android 13", "Tiramisu，通知权限要动态申请"),
    Ref("32 / 31", "Android 12", "Snowcone，Material You、精确闹钟受限"),
    Ref("30", "Android 11", "R，分区存储强制"),
    Ref("29", "Android 10", "Q，深色模式、分区存储引入"),
    Ref("28", "Android 9", "Pie，默认禁止明文 HTTP"),
    Ref("26", "Android 8", "Oreo，后台服务限制、通知渠道"),
    Ref("23", "Android 6", "Marshmallow，运行时权限")
)

private val permissions = listOf(
    Ref("CAMERA", "危险权限", "相机取景与拍照，要运行时申请"),
    Ref("RECORD_AUDIO", "危险权限", "录音、语音识别"),
    Ref("ACCESS_FINE_LOCATION", "危险权限", "精确定位，同时要 COARSE"),
    Ref("POST_NOTIFICATIONS", "危险权限", "Android 13 起发通知必须申请"),
    Ref("READ_MEDIA_IMAGES", "危险权限", "Android 13 起替代 READ_EXTERNAL_STORAGE"),
    Ref("READ_MEDIA_VIDEO", "危险权限", "读视频，同上"),
    Ref("READ_MEDIA_AUDIO", "危险权限", "读音频，同上"),
    Ref("MANAGE_EXTERNAL_STORAGE", "特殊权限", "全盘访问，上架审核极严"),
    Ref("SCHEDULE_EXACT_ALARM", "特殊权限", "精确闹钟，Android 12 起受限"),
    Ref("INTERNET", "普通权限", "联网，声明即可无需申请"),
    Ref("VIBRATE", "普通权限", "震动，声明即可"),
    Ref("FOREGROUND_SERVICE", "普通权限", "前台服务，Android 14 起要细分类型")
)

private val adbTips = listOf(
    Ref("查看当前界面", "adb shell dumpsys window | grep mCurrentFocus", "定位 Activity 名"),
    Ref("抓崩溃日志", "adb logcat -b crash", "只看崩溃缓冲区"),
    Ref("清数据重启", "adb shell pm clear 包名", "等于「清除数据」"),
    Ref("模拟深链", "adb shell am start -a android.intent.action.VIEW -d \"scheme://path\"", "测试 deeplink"),
    Ref("查看权限状态", "adb shell dumpsys package 包名 | grep permission", "看授予情况"),
    Ref("方法数超限排查", "adb shell dumpsys meminfo 包名", "看内存与 dex"),
    Ref("强制深色模式", "adb shell cmd uimode night yes", "测暗色适配"),
    Ref("模拟慢网", "adb shell settings put global captive_portal_mode 0", "配合抓包工具")
)

@Composable
fun AndroidRefToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var tab by rememberSaveable { mutableStateOf(0) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var dpText by rememberSaveable { mutableStateOf("48") }
    var densityIndex by rememberSaveable { mutableStateOf(4) }

    val dp = dpText.trim().toDoubleOrNull() ?: 0.0
    val scale = densityBuckets[densityIndex].third
    val px = dp * scale

    val source = when (tab) {
        0 -> apiLevels
        1 -> permissions
        else -> adbTips
    }
    val filtered = if (keyword.isBlank()) source else source.filter {
        it.name.contains(keyword.trim(), true) || it.value.contains(keyword.trim(), true) ||
            it.note.contains(keyword.trim())
    }

    ToolScaffold {
        item { SectionHeader("dp 与 px 换算") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = dpText,
                            onValueChange = { dpText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "dp 值",
                            mono = true
                        )
                        StatCell("对应 px", if (dp > 0) Math.round(px).toString() else "—", Modifier.weight(1f))
                    }
                    SegmentedPicker(
                        options = densityBuckets.map { it.first },
                        selectedIndex = densityIndex,
                        onSelected = { densityIndex = it }
                    )
                    Text(
                        densityBuckets[densityIndex].first + " 的密度是 " +
                            densityBuckets[densityIndex].second + " dpi，缩放倍数 " +
                            densityBuckets[densityIndex].third,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("各密度切图尺寸") }
        item {
            GroupedCard {
                densityBuckets.forEachIndexed { index, (name, dpi, factor) ->
                    KeyValueRow(
                        name + "　" + dpi + " dpi",
                        if (dp > 0) Math.round(dp * factor).toString() + " px" else "—",
                        copyable = false
                    )
                    if (index != densityBuckets.lastIndex) RowDivider()
                }
            }
        }
        item { SectionHeader("速查") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("API 版本", "权限", "ADB 技巧"),
                        selectedIndex = tab,
                        onSelected = { tab = it }
                    )
                    IosTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = "搜索"
                    )
                }
            }
        }
        item { SectionHeader(if (filtered.isEmpty()) "没找到" else "共 " + filtered.size + " 条") }
        item {
            GroupedCard {
                if (filtered.isEmpty()) {
                    CardPadding {
                        Text(
                            "换个关键词试试",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                } else {
                    filtered.forEachIndexed { index, ref ->
                        KeyValueRow(ref.name + "　" + ref.value, ref.note)
                        if (index != filtered.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
