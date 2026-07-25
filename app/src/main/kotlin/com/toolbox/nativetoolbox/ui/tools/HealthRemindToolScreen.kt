package com.toolbox.nativetoolbox.ui.tools

import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PermissionGate
import com.toolbox.nativetoolbox.util.ReminderReceiver

private val WATER_OPTIONS = listOf(60L, 90L, 120L, 180L)   // 分钟
private val SIT_OPTIONS = listOf(45L, 60L, 90L)
private val MED_OPTIONS = listOf(360L, 480L, 720L, 1440L)  // 6h/8h/12h/24h

@Composable
private fun RemindContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("health_remind", android.content.Context.MODE_PRIVATE) }

    var waterOn by remember { mutableStateOf(prefs.getBoolean("water_on", false)) }
    var waterIdx by remember { mutableStateOf(prefs.getInt("water_idx", 1)) }
    var sitOn by remember { mutableStateOf(prefs.getBoolean("sit_on", false)) }
    var sitIdx by remember { mutableStateOf(prefs.getInt("sit_idx", 1)) }
    var medOn by remember { mutableStateOf(prefs.getBoolean("med_on", false)) }
    var medIdx by remember { mutableStateOf(prefs.getInt("med_idx", 1)) }

    fun apply(kind: Int, on: Boolean, intervalMin: Long) {
        if (on) ReminderReceiver.schedule(context, kind, intervalMin)
        else ReminderReceiver.cancel(context, kind)
    }

    GroupedCard {
        CardPadding {
            Text("喝水提醒", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = palette.label)
            Spacer(Modifier.height(8.dp))
            SegmentedPicker(listOf("1 小时", "1.5 小时", "2 小时", "3 小时"), waterIdx, {
                waterIdx = it
                prefs.edit().putInt("water_idx", it).apply()
                if (waterOn) apply(0, true, WATER_OPTIONS[it])
            }, Modifier.fillMaxWidth())
        }
        ToggleRow("开启", waterOn, onCheckedChange = {
            waterOn = it
            prefs.edit().putBoolean("water_on", it).apply()
            apply(0, it, WATER_OPTIONS[waterIdx])
        })
    }
    Spacer(Modifier.height(20.dp))
    GroupedCard {
        CardPadding {
            Text("久坐提醒", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = palette.label)
            Spacer(Modifier.height(8.dp))
            SegmentedPicker(listOf("45 分钟", "1 小时", "1.5 小时"), sitIdx, {
                sitIdx = it
                prefs.edit().putInt("sit_idx", it).apply()
                if (sitOn) apply(2, true, SIT_OPTIONS[it])
            }, Modifier.fillMaxWidth())
        }
        ToggleRow("开启", sitOn, onCheckedChange = {
            sitOn = it
            prefs.edit().putBoolean("sit_on", it).apply()
            apply(2, it, SIT_OPTIONS[sitIdx])
        })
    }
    Spacer(Modifier.height(20.dp))
    GroupedCard {
        CardPadding {
            Text("吃药提醒", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = palette.label)
            Spacer(Modifier.height(8.dp))
            SegmentedPicker(listOf("每 6 时", "每 8 时", "每 12 时", "每天"), medIdx, {
                medIdx = it
                prefs.edit().putInt("med_idx", it).apply()
                if (medOn) apply(1, true, MED_OPTIONS[it])
            }, Modifier.fillMaxWidth())
        }
        ToggleRow("开启", medOn, onCheckedChange = {
            medOn = it
            prefs.edit().putBoolean("med_on", it).apply()
            apply(1, it, MED_OPTIONS[medIdx])
        })
    }
    Spacer(Modifier.height(12.dp))
    var tested by remember { mutableStateOf(false) }
    SolidButton(
        onClick = {
            ReminderReceiver.notify(context, 9, "🔔 测试提醒", "通知没问题,提醒都会像这样弹出来")
            tested = true
        },
        Modifier.fillMaxWidth().padding(horizontal = 16.dp), filled = false
    ) { Text(if (tested) "已发测试通知,看看通知栏" else "发一条测试通知") }
    Spacer(Modifier.height(8.dp))
    Text(
        "开着就一直提醒(间隔从开启时刻算)。国产手机记得把 Astro Kit 加入后台白名单,否则可能被杀掉不响",
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        style = MaterialTheme.typography.bodySmall,
        color = palette.tertiaryLabel,
        textAlign = TextAlign.Center
    )
}

@Composable
fun HealthRemindToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item {
            if (Build.VERSION.SDK_INT >= 33) {
                PermissionGate(android.Manifest.permission.POST_NOTIFICATIONS, "提醒要靠通知弹出来,需要通知权限") {
                    RemindContent()
                }
            } else {
                RemindContent()
            }
        }
    }
}
