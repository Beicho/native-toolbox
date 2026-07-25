package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val IDLE = 0
private const val WAITING = 1
private const val READY = 2
private const val TOO_SOON = 3
private const val DONE = 4

private fun grade(ms: Long): String = when {
    ms < 180 -> "职业电竞级"
    ms < 230 -> "很快"
    ms < 280 -> "正常偏快"
    ms < 350 -> "普通人水平"
    ms < 450 -> "有点慢"
    else -> "该睡觉了"
}

@Composable
fun ReactionTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }

    var state by remember { mutableIntStateOf(IDLE) }
    var greenAt by remember { mutableLongStateOf(0L) }
    var lastMs by remember { mutableLongStateOf(0L) }
    var records by rememberSaveable { mutableStateOf("") }

    val list = records.split(",").mapNotNull { it.trim().toLongOrNull() }
    val best = list.minOrNull()
    val average = if (list.isEmpty()) null else list.average()

    DisposableEffect(state) {
        if (state != WAITING) {
            onDispose { }
        } else {
            val job = scope.launch {
                // 1.2 到 4 秒随机，防止靠节奏预判
                delay(Random.nextLong(1200, 4000))
                if (isActive && state == WAITING) {
                    greenAt = System.currentTimeMillis()
                    state = READY
                }
            }
            onDispose { job.cancel() }
        }
    }

    fun tap() {
        when (state) {
            IDLE, TOO_SOON, DONE -> state = WAITING
            WAITING -> state = TOO_SOON
            READY -> {
                lastMs = System.currentTimeMillis() - greenAt
                records = (listOf(lastMs.toString()) + list.map { it.toString() }).take(20).joinToString(",")
                state = DONE
            }
        }
    }

    val boxColor = when (state) {
        READY -> palette.green
        WAITING -> palette.red
        TOO_SOON -> palette.orange
        else -> palette.sunkenBackground
    }
    val boxText = when (state) {
        IDLE -> "点一下开始"
        WAITING -> "等变绿…"
        READY -> "现在点！"
        TOO_SOON -> "太早了，再来"
        else -> lastMs.toString() + " 毫秒"
    }

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(boxColor)
                            .clickable(interactionSource = interaction, indication = null) { tap() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            boxText,
                            fontSize = if (state == DONE) 40.sp else 24.sp,
                            fontWeight = FontWeight.Light,
                            color = if (state == IDLE) palette.secondaryLabel else palette.cardBackground
                        )
                    }
                    if (state == DONE) {
                        Text(
                            grade(lastMs),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accent
                        )
                    }
                    Text(
                        "红色时不要点，变绿立刻点。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("成绩") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("最快", best?.let { it.toString() + " ms" } ?: "—", Modifier.weight(1f))
                        StatCell(
                            "平均",
                            average?.let { Math.round(it).toString() + " ms" } ?: "—",
                            Modifier.weight(1f)
                        )
                        StatCell("次数", list.size.toString(), Modifier.weight(1f))
                    }
                    if (list.isNotEmpty()) {
                        SolidButton(onClick = { records = "" }, filled = false) { Text("清空成绩") }
                    }
                }
            }
        }
        if (list.isNotEmpty()) {
            item { SectionHeader("最近记录") }
            item {
                GroupedCard {
                    list.forEachIndexed { index, ms ->
                        KeyValueRow(
                            "第 " + (list.size - index) + " 次",
                            ms.toString() + " ms　" + grade(ms),
                            copyable = false
                        )
                        if (index != list.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { SectionHeader("参考") }
        item {
            GroupedCard {
                val refs = listOf(
                    "180 毫秒以内" to "职业电竞选手区间",
                    "200 到 250" to "反应很快",
                    "250 到 300" to "健康成年人常见",
                    "300 到 400" to "偏慢，可能累了",
                    "400 以上" to "疲劳或注意力分散"
                )
                refs.forEachIndexed { index, (range, desc) ->
                    KeyValueRow(range, desc, copyable = false)
                    if (index != refs.lastIndex) RowDivider()
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "屏幕本身有几十毫秒延迟，不同手机测出来会有差别，同一台机器上比较才有意义。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
