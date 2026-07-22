package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun parse(s: String): LocalDate? = runCatching { LocalDate.parse(s.trim(), dFmt) }.getOrNull()

private fun weekName(d: DayOfWeek): String =
    listOf("一", "二", "三", "四", "五", "六", "日")[d.value - 1]

@Composable
fun DateCalcToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val today = LocalDate.now()
    var dateA by rememberSaveable { mutableStateOf(today.format(dFmt)) }
    var dateB by rememberSaveable { mutableStateOf("") }
    var baseDate by rememberSaveable { mutableStateOf(today.format(dFmt)) }
    var offsetDays by rememberSaveable { mutableStateOf("100") }

    val a = parse(dateA)
    val b = parse(dateB)
    val diff = if (a != null && b != null) ChronoUnit.DAYS.between(a, b) else null

    val base = parse(baseDate)
    val offset = offsetDays.trim().toLongOrNull()
    val target = if (base != null && offset != null) base.plusDays(offset) else null

    ToolScaffold {
        item { SectionHeader("日期间隔") }
        item {
            GroupedCard {
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosTextField(
                            value = dateA,
                            onValueChange = { dateA = it },
                            placeholder = "yyyy-MM-dd",
                            mono = true,
                            modifier = Modifier.weight(1f)
                        )
                        Text("→", color = palette.secondaryLabel)
                        IosTextField(
                            value = dateB,
                            onValueChange = { dateB = it },
                            placeholder = "yyyy-MM-dd",
                            mono = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                KeyValueRow("相差天数", diff?.toString() ?: "", copyable = false)
                RowDivider()
                KeyValueRow(
                    "约合",
                    if (diff == null) "" else "${diff / 7} 周 ${diff % 7} 天",
                    copyable = false
                )
            }
        }

        item { SectionHeader("N 天之后/之前(负数为之前)") }
        item {
            GroupedCard {
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosTextField(
                            value = baseDate,
                            onValueChange = { baseDate = it },
                            placeholder = "yyyy-MM-dd",
                            mono = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        IosTextField(
                            value = offsetDays,
                            onValueChange = { offsetDays = it },
                            placeholder = "天数",
                            mono = true,
                            modifier = Modifier.weight(0.8f)
                        )
                        SolidButton(onClick = { baseDate = LocalDate.now().format(dFmt) }, filled = false, height = 40.dp) {
                            Text("今天", color = palette.accent)
                        }
                    }
                }
                KeyValueRow(
                    "结果",
                    target?.let { "${it.format(dFmt)} 周${weekName(it.dayOfWeek)}" } ?: "",
                    copyable = false
                )
            }
        }

        item { SectionHeader("今天") }
        item {
            GroupedCard {
                KeyValueRow("日期", "${today.format(dFmt)} 周${weekName(today.dayOfWeek)}", copyable = false)
                RowDivider()
                KeyValueRow("今年第", "${today.dayOfYear} 天", copyable = false)
                RowDivider()
                KeyValueRow("距明年元旦", "${ChronoUnit.DAYS.between(today, LocalDate.of(today.year + 1, 1, 1))} 天", copyable = false)
            }
        }
    }
}
