package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val speedLabels = listOf("很慢", "慢", "中", "快", "很快")
private val speedPixelsPerTick = listOf(1, 2, 3, 5, 8)
private val fontSizes = listOf(20, 26, 34, 44)

@Composable
fun TeleprompterToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val scrollState = rememberScrollState()

    var script by rememberSaveable { mutableStateOf("") }
    var running by rememberSaveable { mutableStateOf(false) }
    var speedIndex by rememberSaveable { mutableStateOf(2) }
    var fontIndex by rememberSaveable { mutableStateOf(2) }
    var mirrored by rememberSaveable { mutableStateOf(false) }
    var keepAwake by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(running, speedIndex) {
        if (!running) {
            onDispose { }
        } else {
            val job = scope.launch {
                while (isActive && running) {
                    delay(40)
                    val next = scrollState.value + speedPixelsPerTick[speedIndex]
                    if (next >= scrollState.maxValue) {
                        scrollState.scrollTo(scrollState.maxValue)
                        running = false
                    } else {
                        scrollState.scrollTo(next)
                    }
                }
            }
            onDispose { job.cancel() }
        }
    }

    DisposableEffect(keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    val charCount = script.length
    // 中文播报大约每分钟 220 字
    val estimateSeconds = if (charCount == 0) 0 else Math.round(charCount / 220.0 * 60).toInt()

    ToolScaffold {
        item { SectionHeader("提词区") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.sunkenBackground)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 16.dp, vertical = 140.dp)
                        ) {
                            Text(
                                script.ifBlank { "在下面粘贴稿子，然后点开始自动滚动。" },
                                fontSize = fontSizes[fontIndex].sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.label,
                                lineHeight = (fontSizes[fontIndex] * 1.6).sp,
                                modifier = if (mirrored) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { running = !running },
                            modifier = Modifier.weight(1f),
                            enabled = script.isNotBlank()
                        ) { Text(if (running) "暂停" else "开始滚动") }
                        SolidButton(
                            onClick = {
                                running = false
                                scope.launch { scrollState.scrollTo(0) }
                            },
                            modifier = Modifier.weight(1f),
                            filled = false
                        ) { Text("回到开头") }
                    }
                }
            }
        }
        item { SectionHeader("滚动速度") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = speedLabels,
                        selectedIndex = speedIndex,
                        onSelected = { speedIndex = it }
                    )
                    SegmentedPicker(
                        options = fontSizes.map { it.toString() },
                        selectedIndex = fontIndex,
                        onSelected = { fontIndex = it }
                    )
                }
                ToggleRow(
                    "左右镜像",
                    mirrored,
                    onCheckedChange = { mirrored = it },
                    subtitle = "配合提词器玻璃反射使用"
                )
                ToggleRow("屏幕常亮", keepAwake, onCheckedChange = { keepAwake = it })
            }
        }
        item { SectionHeader("稿子") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = script,
                        onValueChange = { script = it },
                        placeholder = "粘贴要念的稿子",
                        minHeight = 140.dp
                    )
                }
            }
        }
        item { SectionHeader("统计") }
        item {
            GroupedCard {
                KeyValueRow("字数", charCount.toString(), copyable = false)
                RowDivider()
                KeyValueRow("段落", if (script.isBlank()) "0" else script.lines().count { it.isNotBlank() }.toString(), copyable = false)
                RowDivider()
                KeyValueRow(
                    "预计时长",
                    if (estimateSeconds == 0) "—"
                    else (estimateSeconds / 60).toString() + " 分 " + (estimateSeconds % 60) + " 秒",
                    copyable = false
                )
            }
        }
    }
}
