package com.toolbox.nativetoolbox.ui.tools

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val durations = listOf(0, 5, 15, 30, 60)
private val durationLabels = listOf("一直亮", "5 分钟", "15 分钟", "30 分钟", "60 分钟")

@Composable
fun ScreenOnToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var enabled by rememberSaveable { mutableStateOf(false) }
    var durationIndex by rememberSaveable { mutableStateOf(0) }
    var maxBrightness by rememberSaveable { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(0) }

    val minutes = durations[durationIndex]

    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }

    // 亮度调节作用于当前窗口，退出页面自动还原成系统值
    DisposableEffect(maxBrightness, enabled) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null && enabled && maxBrightness) {
            window.attributes = window.attributes.apply {
                screenBrightness = 1f
            }
        }
        onDispose {
            if (window != null) {
                window.attributes = window.attributes.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }

    DisposableEffect(enabled, durationIndex) {
        if (!enabled || minutes == 0) {
            remaining = 0
            onDispose { }
        } else {
            remaining = minutes * 60
            val job = scope.launch {
                while (isActive && remaining > 0) {
                    delay(1000)
                    remaining -= 1
                }
                if (isActive) enabled = false
            }
            onDispose { job.cancel() }
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        if (enabled) "屏幕不会自动熄灭" else "已关闭",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        color = if (enabled) palette.green else palette.secondaryLabel
                    )
                    if (enabled && minutes > 0) {
                        Text(
                            "还剩 " + (remaining / 60) + " 分 " + (remaining % 60) + " 秒",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryLabel
                        )
                    }
                    SolidButton(onClick = { enabled = !enabled }) {
                        Text(if (enabled) "关掉" else "开启常亮")
                    }
                }
            }
        }
        item { SectionHeader("保持多久") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = durationLabels,
                        selectedIndex = durationIndex,
                        onSelected = { durationIndex = it }
                    )
                    Text(
                        if (minutes == 0) "只要停在这个页面就一直亮着。"
                        else "到时间自动关掉常亮，恢复系统的自动息屏。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
                ToggleRow(
                    "同时调到最亮",
                    maxBrightness,
                    onCheckedChange = { maxBrightness = it },
                    subtitle = "只影响这个页面，退出自动还原"
                )
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "长时间常亮加最高亮度会明显耗电和发热，用完记得关掉。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.orange
                    )
                }
            }
        }
    }
}
