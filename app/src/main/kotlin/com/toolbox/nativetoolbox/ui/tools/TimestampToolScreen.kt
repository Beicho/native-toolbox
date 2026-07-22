package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@Composable
fun TimestampToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var tsInput by rememberSaveable { mutableStateOf("") }
    var dateInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    // 时间戳 → 日期(自动识别秒/毫秒)
    val tsResult: Pair<String, String>? = tsInput.trim().toLongOrNull()?.let { raw ->
        val millis = if (raw < 1_000_000_000_000L) raw * 1000 else raw
        runCatching {
            val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            val utc = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
            local.format(fmt) to utc.format(fmt)
        }.getOrNull()
    }

    // 日期 → 时间戳
    val dateResult: Pair<String, String>? = runCatching {
        val local = LocalDateTime.parse(dateInput.trim(), fmt)
        val instant = local.atZone(ZoneId.systemDefault()).toInstant()
        instant.epochSecond.toString() to instant.toEpochMilli().toString()
    }.getOrNull()

    ToolScaffold(title = "时间戳", onBack = onBack) {
        item { SectionHeader("当前时间(每秒刷新,点击复制)") }
        item {
            GroupedCard {
                KeyValueRow("秒级", (now / 1000).toString())
                RowDivider()
                KeyValueRow("毫秒级", now.toString())
                RowDivider()
                KeyValueRow(
                    "本地时间",
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()).format(fmt)
                )
            }
        }

        item { SectionHeader("时间戳 → 日期") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = tsInput,
                        onValueChange = { tsInput = it },
                        placeholder = "输入 Unix 时间戳(秒或毫秒)",
                        mono = true
                    )
                }
                KeyValueRow("本地", tsResult?.first ?: "")
                RowDivider()
                KeyValueRow("UTC", tsResult?.second ?: "")
            }
        }

        item { SectionHeader("日期 → 时间戳") }
        item {
            GroupedCard {
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosTextField(
                            value = dateInput,
                            onValueChange = { dateInput = it },
                            placeholder = "yyyy-MM-dd HH:mm:ss",
                            mono = true,
                            modifier = Modifier.weight(1f)
                        )
                        SolidButton(
                            filled = false,
                            onClick = {
                                dateInput = LocalDateTime
                                    .ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault())
                                    .format(fmt)
                            },
                            height = 40.dp
                        ) {
                            Text("现在", color = palette.accent)
                        }
                    }
                }
                KeyValueRow("秒级", dateResult?.first ?: "")
                RowDivider()
                KeyValueRow("毫秒级", dateResult?.second ?: "")
            }
        }
    }
}
