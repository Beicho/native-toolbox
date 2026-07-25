package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.CnConvert

@Composable
fun CnConvertToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf(0) } // 0 繁→简 1 简→繁
    var output by remember { mutableStateOf("") }

    // 港台用语对照(双向)
    val terms = listOf(
        "软件 ↔ 軟體", "网络 ↔ 網路", "信息 ↔ 資訊", "打印 ↔ 列印", "视频 ↔ 影片",
        "服务器 ↔ 伺服器", "硬盘 ↔ 硬碟", "鼠标 ↔ 滑鼠", "内存 ↔ 記憶體", "数据库 ↔ 資料庫",
    )

    ToolScaffold {
        item {
            if (output.isNotEmpty()) OutputCard(output, Modifier, label = if (direction == 0) "简体结果" else "繁体结果")
        }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("繁体 → 简体", "简体 → 繁体"), direction, {
                        direction = it
                        if (input.isNotBlank()) output = if (it == 0) CnConvert.toSimplified(input) else CnConvert.toTraditional(input)
                    }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    IosTextArea(input, { input = it }, Modifier.fillMaxWidth(), placeholder = if (direction == 0) "貼上繁體字…" else "粘贴简体字…", minHeight = 140.dp)
                    Spacer(Modifier.height(12.dp))
                    SolidButton(
                        onClick = {
                            output = if (input.isBlank()) ""
                            else if (direction == 0) CnConvert.toSimplified(input)
                            else CnConvert.toTraditional(input)
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("转换") }
                    if (output.isEmpty() && input.isBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("支持整段文章,常用字全覆盖", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                    }
                }
            }
        }
        item { SectionHeader("两岸用语速查") }
        item {
            GroupedCard {
                terms.forEachIndexed { i, t ->
                    val parts = t.split(" ↔ ")
                    KeyValueRow(parts[0], parts[1])
                    if (i != terms.lastIndex) RowDivider()
                }
            }
        }
    }
}
