package com.toolbox.nativetoolbox.ui.tools

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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val weekdayNames = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

@Composable
fun BigClockToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var now by androidx.compose.runtime.remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSeconds by rememberSaveable { mutableStateOf(true) }
    var keepAwake by rememberSaveable { mutableStateOf(true) }
    var sizeIndex by rememberSaveable { mutableStateOf(1) }

    // 每 200ms 刷新一次，秒针跳动看起来才准
    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                now = System.currentTimeMillis()
                delay(200)
            }
        }
        onDispose { job.cancel() }
    }

    // 大字时钟通常是放桌上看的，默认不让屏幕自动灭
    DisposableEffect(keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    val date = Date(now)
    val timeFormat = SimpleDateFormat(if (showSeconds) "HH:mm:ss" else "HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy 年 M 月 d 日", Locale.getDefault())
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
    val weekday = weekdayNames[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]

    val clockSize = when (sizeIndex) {
        0 -> 56.sp
        1 -> 76.sp
        else -> 96.sp
    }

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        timeFormat.format(date),
                        fontSize = clockSize,
                        fontWeight = FontWeight.Light,
                        color = palette.label,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        dateFormat.format(date) + "　" + weekday,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.secondaryLabel,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        item { SectionHeader("显示设置") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("小", "中", "大"),
                        selectedIndex = sizeIndex,
                        onSelected = { sizeIndex = it }
                    )
                }
                ToggleRow("显示秒", showSeconds, onCheckedChange = { showSeconds = it })
                ToggleRow(
                    "保持屏幕常亮",
                    keepAwake,
                    onCheckedChange = { keepAwake = it },
                    subtitle = "放桌上当台钟时建议打开"
                )
            }
        }
    }
}
