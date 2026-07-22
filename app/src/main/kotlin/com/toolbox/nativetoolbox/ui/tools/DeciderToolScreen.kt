package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom

private val presets = listOf(
    "吃什么" to "火锅\n烧烤\n麻辣烫\n炒饭\n面条\n饺子\n汉堡\n沙拉",
    "做不做" to "做!\n不做",
    "谁去" to "我去\n你去\n他去\n再抽一次"
)

@Composable
fun DeciderToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf(presets[0].second) }
    var result by remember { mutableStateOf("") }
    var rolling by remember { mutableStateOf(false) }
    val random = remember { SecureRandom() }

    fun roll() {
        val options = input.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (options.size < 2 || rolling) return
        rolling = true
        scope.launch {
            // 滚动动画:快速闪过随机项,逐渐减速
            var d = 40L
            repeat(18) {
                result = options[random.nextInt(options.size)]
                delay(d)
                d = (d * 1.18).toLong()
            }
            result = options[random.nextInt(options.size)]
            rolling = false
        }
    }

    ToolScaffold {
        item { SectionHeader("选项(每行一个)") }
        item {
            GroupedCard {
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        presets.forEach { (name, content) ->
                            SolidButton(
                                onClick = { input = content },
                                filled = false,
                                height = 34.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(name, color = palette.accent, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "每行一个选项…",
                        minHeight = 130.dp
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        SolidButton(onClick = { roll() }, enabled = !rolling) {
                            Text(if (rolling) "命运转动中…" else "帮我决定!", color = Color.White)
                        }
                    }
                }
            }
        }
        item { SectionHeader("命运的答案") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (result.isEmpty()) palette.sunkenBackground
                                else if (rolling) palette.orange.copy(alpha = 0.15f)
                                else palette.accent.copy(alpha = 0.12f)
                            )
                            .animateContentSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            result.ifEmpty { "点上面的按钮" },
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                result.isEmpty() -> palette.tertiaryLabel
                                rolling -> palette.orange
                                else -> palette.accent
                            }
                        )
                    }
                }
            }
        }
    }
}
