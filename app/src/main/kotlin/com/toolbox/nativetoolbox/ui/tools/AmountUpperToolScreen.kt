package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.math.BigDecimal
import java.math.RoundingMode

private val digits = arrayOf("零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖")
private val sectionUnits = arrayOf("", "万", "亿", "万亿")
private val innerUnits = arrayOf("", "拾", "佰", "仟")

/** 四位一节转中文大写，节内连续零折叠为一个零 */
private fun sectionToChinese(section: Int): String {
    val sb = StringBuilder()
    var zeroPending = false
    var value = section
    var unitIndex = 0
    val stack = ArrayList<Pair<Int, Int>>()
    while (value > 0) {
        stack.add(value % 10 to unitIndex)
        value /= 10
        unitIndex++
    }
    stack.reversed().forEach { (digit, unit) ->
        if (digit == 0) {
            zeroPending = true
        } else {
            if (zeroPending && sb.isNotEmpty()) sb.append(digits[0])
            zeroPending = false
            sb.append(digits[digit]).append(innerUnits[unit])
        }
    }
    return sb.toString()
}

private fun amountToChinese(input: BigDecimal): String {
    if (input.compareTo(BigDecimal.ZERO) == 0) return "零元整"
    val negative = input.signum() < 0
    val value = input.abs().setScale(2, RoundingMode.HALF_UP)
    var integerPart = value.toBigInteger()
    val cents = value.subtract(BigDecimal(integerPart)).movePointRight(2).toInt()

    val sections = ArrayList<Int>()
    if (integerPart.signum() == 0) sections.add(0)
    val tenThousand = java.math.BigInteger.valueOf(10000)
    while (integerPart.signum() > 0) {
        sections.add(integerPart.mod(tenThousand).toInt())
        integerPart = integerPart.divide(tenThousand)
    }

    val yuan = StringBuilder()
    for (i in sections.indices.reversed()) {
        val section = sections[i]
        if (section == 0) {
            // 空节：若后面还有非零节且当前结果非空，补一个零
            if (yuan.isNotEmpty() && sections.take(i).any { it != 0 } && !yuan.endsWith(digits[0])) {
                yuan.append(digits[0])
            }
            continue
        }
        val chunk = sectionToChinese(section)
        // 非最高节且本节不足四位时，前面要补零（如 一亿零三万）
        if (yuan.isNotEmpty() && section < 1000 && !yuan.endsWith(digits[0])) yuan.append(digits[0])
        yuan.append(chunk).append(sectionUnits.getOrElse(i) { "" })
    }

    val sb = StringBuilder()
    if (negative) sb.append("负")
    if (yuan.isNotEmpty()) sb.append(yuan).append("元")
    when {
        cents == 0 -> sb.append(if (yuan.isEmpty()) "零元整" else "整")
        cents % 10 == 0 -> sb.append(digits[cents / 10]).append("角")
        cents < 10 -> {
            if (yuan.isNotEmpty()) sb.append(digits[0])
            sb.append(digits[cents]).append("分")
        }
        else -> sb.append(digits[cents / 10]).append("角").append(digits[cents % 10]).append("分")
    }
    return sb.toString()
}

@Composable
fun AmountUpperToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }

    val parsed = runCatching { if (input.isBlank()) null else BigDecimal(input.trim().replace(",", "")) }.getOrNull()
    val upper = parsed?.let { runCatching { amountToChinese(it) }.getOrNull() } ?: ""

    ToolScaffold {
        item { SectionHeader("金额") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "例如 12345.67",
                        mono = true
                    )
                    if (input.isNotBlank() && parsed == null) {
                        Text(
                            "这不是一个有效数字",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    } else {
                        Text(
                            "自动保留两位小数，四舍五入。可用于开票、合同、收据。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
        item { SectionHeader("中文大写") }
        item {
            GroupedCard { CardPadding { OutputCard(text = upper, label = "大写金额") } }
        }
    }
}
