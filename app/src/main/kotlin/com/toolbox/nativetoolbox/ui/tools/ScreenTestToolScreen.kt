package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val testColors = listOf(
    Color.Red to "红",
    Color.Green to "绿",
    Color.Blue to "蓝",
    Color.White to "白",
    Color.Black to "黑",
    Color(0xFF808080) to "灰"
)

@Composable
fun ScreenTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var testing by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }

    if (testing) {
        val (color, _) = testColors[index]
        Box(
            Modifier
                .fillMaxSize()
                .background(color)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (index < testColors.lastIndex) index++ else testing = false
                }
        )
        return
    }

    ToolScaffold {
        item { SectionHeader("屏幕坏点 / 漏光检测") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "进入后全屏轮播纯色(红→绿→蓝→白→黑→灰),仔细观察每种颜色下有没有异常亮点、暗点或色斑。点击屏幕切换下一色,最后一色点击后退出。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryLabel
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        SolidButton(onClick = { index = 0; testing = true }) {
                            Text("开始检测", color = Color.White)
                        }
                    }
                }
            }
        }
        item { SectionHeader("色块速览") }
        item {
            GroupedCard {
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        testColors.forEach { (c, name) ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                                    .background(c)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        index = testColors.indexOfFirst { it.second == name }
                                        testing = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (c == Color.White || c == Color.Green) Color.Black else Color.White,
                                    modifier = Modifier.padding(vertical = 14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
