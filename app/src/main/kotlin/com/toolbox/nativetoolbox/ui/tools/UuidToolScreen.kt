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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.util.UUID

@Composable
fun UuidToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var countIndex by rememberSaveable { mutableStateOf(0) }
    var uppercase by rememberSaveable { mutableStateOf(false) }
    var noHyphen by rememberSaveable { mutableStateOf(false) }
    var uuids by rememberSaveable { mutableStateOf(listOf(UUID.randomUUID().toString())) }

    val counts = listOf(1, 5, 10, 20)

    fun generate() {
        uuids = List(counts[countIndex]) { UUID.randomUUID().toString() }
    }

    val display = uuids.joinToString("\n") { raw ->
        var s = raw
        if (noHyphen) s = s.replace("-", "")
        if (uppercase) s = s.uppercase()
        s
    }

    ToolScaffold(title = "UUID 生成", onBack = onBack) {
        item { SectionHeader("选项") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = counts.map { "${it} 个" },
                        selectedIndex = countIndex,
                        onSelected = { countIndex = it; generate() }
                    )
                }
                ToggleRow("大写", uppercase, { uppercase = it })
                ToggleRow("去掉连字符", noHyphen, { noHyphen = it })
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(text = display, label = "UUID v4")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SolidButton(onClick = { generate() }, filled = true) {
                            Text("重新生成", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
