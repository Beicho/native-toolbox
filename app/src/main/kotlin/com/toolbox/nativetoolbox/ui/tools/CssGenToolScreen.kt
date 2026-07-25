package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private const val KIND_SHADOW = 0
private const val KIND_GRADIENT = 1
private const val KIND_RADIUS = 2

private fun hexOf(color: Color): String = String.format(
    "#%02X%02X%02X",
    (color.red * 255).toInt(),
    (color.green * 255).toInt(),
    (color.blue * 255).toInt()
)

private fun parseHex(text: String, fallback: Color): Color {
    val clean = text.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return fallback
    return runCatching {
        val value = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or value)
        } else {
            Color(value)
        }
    }.getOrDefault(fallback)
}

@Composable
fun CssGenToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var kind by rememberSaveable { mutableStateOf(KIND_SHADOW) }

    // 阴影
    var offsetX by rememberSaveable { mutableStateOf("0") }
    var offsetY by rememberSaveable { mutableStateOf("8") }
    var blur by rememberSaveable { mutableStateOf("24") }
    var spread by rememberSaveable { mutableStateOf("0") }
    var shadowColor by rememberSaveable { mutableStateOf("#000000") }
    var shadowAlpha by rememberSaveable { mutableStateOf("18") }
    var inset by rememberSaveable { mutableStateOf(false) }

    // 渐变
    var colorA by rememberSaveable { mutableStateOf("#FF9500") }
    var colorB by rememberSaveable { mutableStateOf("#AF52DE") }
    var angleIndex by rememberSaveable { mutableStateOf(1) }

    // 圆角
    var radiusTL by rememberSaveable { mutableStateOf("16") }
    var radiusTR by rememberSaveable { mutableStateOf("16") }
    var radiusBR by rememberSaveable { mutableStateOf("16") }
    var radiusBL by rememberSaveable { mutableStateOf("16") }

    val angles = listOf(0, 90, 135, 180)
    val angleLabels = listOf("向上", "向右", "斜向", "向下")

    val css = when (kind) {
        KIND_SHADOW -> {
            val alpha = (shadowAlpha.trim().toIntOrNull() ?: 18).coerceIn(0, 100)
            val c = parseHex(shadowColor, Color.Black)
            val rgba = "rgba(" + (c.red * 255).toInt() + ", " + (c.green * 255).toInt() + ", " +
                (c.blue * 255).toInt() + ", " + String.format("%.2f", alpha / 100.0) + ")"
            "box-shadow: " + (if (inset) "inset " else "") +
                offsetX.trim() + "px " + offsetY.trim() + "px " + blur.trim() + "px " +
                spread.trim() + "px " + rgba + ";"
        }
        KIND_GRADIENT -> "background: linear-gradient(" + angles[angleIndex] + "deg, " +
            colorA.trim() + " 0%, " + colorB.trim() + " 100%);"
        else -> "border-radius: " + radiusTL.trim() + "px " + radiusTR.trim() + "px " +
            radiusBR.trim() + "px " + radiusBL.trim() + "px;"
    }

    val previewColorA = parseHex(colorA, palette.orange)
    val previewColorB = parseHex(colorB, palette.purple)

    ToolScaffold {
        item { SectionHeader("预览") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(palette.sunkenBackground, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (kind) {
                            KIND_GRADIENT -> Box(
                                Modifier
                                    .size(width = 200.dp, height = 100.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(listOf(previewColorA, previewColorB))
                                    )
                            )
                            KIND_RADIUS -> Box(
                                Modifier
                                    .size(width = 180.dp, height = 100.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = (radiusTL.trim().toIntOrNull() ?: 0).dp,
                                            topEnd = (radiusTR.trim().toIntOrNull() ?: 0).dp,
                                            bottomEnd = (radiusBR.trim().toIntOrNull() ?: 0).dp,
                                            bottomStart = (radiusBL.trim().toIntOrNull() ?: 0).dp
                                        )
                                    )
                                    .background(palette.accent)
                            )
                            else -> Box(
                                Modifier
                                    .size(width = 180.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.cardBackground)
                            )
                        }
                    }
                    if (kind == KIND_SHADOW) {
                        Text(
                            "阴影效果在这里没法精确还原，参考数值和生成的代码即可。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
        item { SectionHeader("生成什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("阴影", "渐变", "圆角"),
                        selectedIndex = kind,
                        onSelected = { kind = it }
                    )
                }
            }
        }
        item { SectionHeader("参数") }
        item {
            GroupedCard {
                CardPadding {
                    when (kind) {
                        KIND_SHADOW -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosTextField(offsetX, { offsetX = it }, Modifier.weight(1f), "横偏移", mono = true)
                                IosTextField(offsetY, { offsetY = it }, Modifier.weight(1f), "纵偏移", mono = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosTextField(blur, { blur = it }, Modifier.weight(1f), "模糊", mono = true)
                                IosTextField(spread, { spread = it }, Modifier.weight(1f), "扩散", mono = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosTextField(shadowColor, { shadowColor = it }, Modifier.weight(2f), "颜色 #RRGGBB", mono = true)
                                IosTextField(shadowAlpha, { shadowAlpha = it }, Modifier.weight(1f), "不透明 %", mono = true)
                            }
                            com.toolbox.nativetoolbox.ui.components.ToggleRow(
                                "内阴影", inset, onCheckedChange = { inset = it }
                            )
                        }
                        KIND_GRADIENT -> {
                            IosTextField(colorA, { colorA = it }, placeholder = "起始色 #RRGGBB", mono = true)
                            IosTextField(colorB, { colorB = it }, placeholder = "结束色 #RRGGBB", mono = true)
                            SegmentedPicker(
                                options = angleLabels,
                                selectedIndex = angleIndex,
                                onSelected = { angleIndex = it }
                            )
                        }
                        else -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosTextField(radiusTL, { radiusTL = it }, Modifier.weight(1f), "左上", mono = true)
                                IosTextField(radiusTR, { radiusTR = it }, Modifier.weight(1f), "右上", mono = true)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosTextField(radiusBL, { radiusBL = it }, Modifier.weight(1f), "左下", mono = true)
                                IosTextField(radiusBR, { radiusBR = it }, Modifier.weight(1f), "右下", mono = true)
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader("CSS") }
        item { GroupedCard { CardPadding { OutputCard(text = css, label = "复制到样式表") } } }
        item { SectionHeader("常用预设") }
        item {
            GroupedCard {
                CardPadding {
                    val presets = when (kind) {
                        KIND_SHADOW -> listOf(
                            "轻微浮起" to Triple("0", "2", "6"),
                            "卡片阴影" to Triple("0", "8", "24"),
                            "强烈立体" to Triple("0", "16", "40")
                        )
                        else -> emptyList()
                    }
                    if (presets.isEmpty()) {
                        Text(
                            "改上面的数值，下面的代码会实时更新。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presets.forEach { (name, values) ->
                                com.toolbox.nativetoolbox.ui.components.SolidButton(
                                    onClick = {
                                        offsetX = values.first
                                        offsetY = values.second
                                        blur = values.third
                                    },
                                    modifier = Modifier.weight(1f),
                                    filled = false,
                                    height = 38.dp
                                ) { Text(name, style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                }
            }
        }
    }
}
