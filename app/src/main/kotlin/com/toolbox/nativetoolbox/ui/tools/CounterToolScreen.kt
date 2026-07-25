package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private fun tick(context: android.content.Context) {
    runCatching {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as android.os.VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(20, 80))
    }
}

@Composable
fun CounterToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current

    var count by rememberSaveable { mutableStateOf(0) }
    var stepText by rememberSaveable { mutableStateOf("1") }
    var targetText by rememberSaveable { mutableStateOf("") }
    var hapticOn by rememberSaveable { mutableStateOf(true) }
    var history by rememberSaveable { mutableStateOf("") }

    val step = stepText.trim().toIntOrNull()?.takeIf { it != 0 } ?: 1
    val target = targetText.trim().toIntOrNull()
    val reached = target != null && count >= target

    fun bump(delta: Int) {
        count += delta
        if (hapticOn) tick(context)
    }

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        count.toString(),
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Light,
                        color = if (reached) palette.green else palette.label
                    )
                    if (target != null) {
                        Text(
                            if (reached) "已达成目标 " + target else "目标 " + target + "，还差 " + (target - count),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (reached) palette.green else palette.secondaryLabel
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SolidButton(
                            onClick = { bump(-step) },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            height = 56.dp
                        ) { Text("−" + step, fontSize = 20.sp) }
                        SolidButton(
                            onClick = { bump(step) },
                            modifier = Modifier.weight(2f),
                            height = 56.dp
                        ) { Text("+" + step, fontSize = 20.sp) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SolidButton(
                            onClick = {
                                if (count != 0) {
                                    history = (listOf(count.toString()) + history.split(",").filter { it.isNotBlank() })
                                        .take(10).joinToString(",")
                                }
                                count = 0
                            },
                            filled = false
                        ) { Text("归零并记录") }
                        SolidButton(onClick = { count = 0 }, filled = false) { Text("直接归零") }
                    }
                }
            }
        }
        item { SectionHeader("设置") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = stepText,
                            onValueChange = { stepText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "每次加减",
                            mono = true
                        )
                        IosTextField(
                            value = targetText,
                            onValueChange = { targetText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "目标（可选）",
                            mono = true
                        )
                    }
                }
                ToggleRow("按一下震一下", hapticOn, onCheckedChange = { hapticOn = it })
            }
        }
        if (history.isNotBlank()) {
            item { SectionHeader("最近记录") }
            item {
                GroupedCard {
                    val items = history.split(",").filter { it.isNotBlank() }
                    items.forEachIndexed { index, value ->
                        KeyValueRow("第 " + (items.size - index) + " 次", value, copyable = false)
                        if (index != items.lastIndex) RowDivider()
                    }
                    RowDivider()
                    KeyValueRow(
                        "合计",
                        items.sumOf { it.toIntOrNull() ?: 0 }.toString(),
                        copyable = false
                    )
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "适合数人数、数货、计圈数。归零并记录会把当前数字存进下面的列表，最多留 10 条。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
