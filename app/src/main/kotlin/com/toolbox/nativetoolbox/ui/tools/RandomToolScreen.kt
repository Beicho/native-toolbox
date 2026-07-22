package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.liquid.LiquidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.security.SecureRandom
import kotlin.math.roundToInt

private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
private const val UPPER_ALL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val LOWER = "abcdefghijkmnpqrstuvwxyz"
private const val LOWER_ALL = "abcdefghijklmnopqrstuvwxyz"
private const val DIGIT = "23456789"
private const val DIGIT_ALL = "0123456789"
private const val SYMBOL = "!@#$%^&*()-_=+[]{}<>?"

@Composable
fun RandomToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var length by rememberSaveable { mutableStateOf(16f) }
    var useUpper by rememberSaveable { mutableStateOf(true) }
    var useLower by rememberSaveable { mutableStateOf(true) }
    var useDigit by rememberSaveable { mutableStateOf(true) }
    var useSymbol by rememberSaveable { mutableStateOf(false) }
    var excludeAmbiguous by rememberSaveable { mutableStateOf(true) }
    var results by rememberSaveable { mutableStateOf(listOf<String>()) }

    fun generate() {
        val pool = buildString {
            if (useUpper) append(if (excludeAmbiguous) UPPER else UPPER_ALL)
            if (useLower) append(if (excludeAmbiguous) LOWER else LOWER_ALL)
            if (useDigit) append(if (excludeAmbiguous) DIGIT else DIGIT_ALL)
            if (useSymbol) append(SYMBOL)
        }
        if (pool.isEmpty()) {
            results = listOf("请至少启用一类字符")
            return
        }
        val random = SecureRandom()
        results = List(5) {
            buildString {
                repeat(length.roundToInt()) { append(pool[random.nextInt(pool.length)]) }
            }
        }
    }

    ToolScaffold(title = "随机密码", onBack = onBack) {
        item { SectionHeader("长度 · ${length.roundToInt()} 位") }
        item {
            GroupedCard {
                CardPadding {
                    Slider(
                        value = length,
                        onValueChange = { length = it },
                        valueRange = 4f..64f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = palette.accent,
                            inactiveTrackColor = palette.fill
                        )
                    )
                }
            }
        }
        item { SectionHeader("字符集") }
        item {
            GroupedCard {
                ToggleRow("大写字母", useUpper, { useUpper = it })
                ToggleRow("小写字母", useLower, { useLower = it })
                ToggleRow("数字", useDigit, { useDigit = it })
                ToggleRow("符号", useSymbol, { useSymbol = it })
                ToggleRow("排除易混字符", excludeAmbiguous, { excludeAmbiguous = it }, subtitle = "去掉 0O1lI 等")
            }
        }
        item { SectionHeader("结果(一次 5 条)") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(text = results.joinToString("\n"), label = "密码")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        LiquidButton(onClick = { generate() }, tint = palette.accent) {
                            Text("生成", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
