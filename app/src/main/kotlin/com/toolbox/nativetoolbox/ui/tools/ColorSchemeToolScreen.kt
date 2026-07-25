package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.rememberCopy
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private fun hexOf(c: Color): String = String.format(
    "#%02X%02X%02X",
    (c.red * 255).toInt(),
    (c.green * 255).toInt(),
    (c.blue * 255).toInt()
)

private fun parseHexColor(text: String): Color? {
    val clean = text.trim().removePrefix("#")
    if (clean.length != 6) return null
    return runCatching { Color(0xFF000000 or clean.toLong(16)) }.getOrNull()
}

private fun rgbToHsl(c: Color): Triple<Float, Float, Float> {
    val r = c.red
    val g = c.green
    val b = c.blue
    val maxV = max(r, max(g, b))
    val minV = min(r, min(g, b))
    val l = (maxV + minV) / 2
    if (maxV == minV) return Triple(0f, 0f, l)
    val d = maxV - minV
    val s = if (l > 0.5f) d / (2 - maxV - minV) else d / (maxV + minV)
    val h = when (maxV) {
        r -> ((g - b) / d + (if (g < b) 6 else 0))
        g -> ((b - r) / d + 2)
        else -> ((r - g) / d + 4)
    } * 60
    return Triple(h, s, l)
}

private fun hslToRgb(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360) + 360) % 360 / 360f
    if (s == 0f) return Color(l, l, l)
    fun hue2rgb(p: Float, q: Float, tRaw: Float): Float {
        var t = tRaw
        if (t < 0) t += 1
        if (t > 1) t -= 1
        return when {
            t < 1f / 6 -> p + (q - p) * 6 * t
            t < 1f / 2 -> q
            t < 2f / 3 -> p + (q - p) * (2f / 3 - t) * 6
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
    val p = 2 * l - q
    return Color(
        hue2rgb(p, q, hue + 1f / 3).coerceIn(0f, 1f),
        hue2rgb(p, q, hue).coerceIn(0f, 1f),
        hue2rgb(p, q, hue - 1f / 3).coerceIn(0f, 1f)
    )
}

private fun rotate(c: Color, degrees: Float, satScale: Float = 1f, lightShift: Float = 0f): Color {
    val (h, s, l) = rgbToHsl(c)
    return hslToRgb(h + degrees, (s * satScale).coerceIn(0f, 1f), (l + lightShift).coerceIn(0.05f, 0.95f))
}

/** WCAG 相对亮度与对比度 */
private fun luminance(c: Color): Double {
    fun channel(v: Float): Double {
        val d = v.toDouble()
        return if (d <= 0.03928) d / 12.92 else Math.pow((d + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
}

private fun contrastRatio(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

private val schemeNames = listOf("单色", "互补", "三分", "四分", "类似色")

private fun buildScheme(base: Color, scheme: Int): List<Pair<String, Color>> = when (scheme) {
    0 -> listOf(
        "最浅" to rotate(base, 0f, 0.6f, 0.32f),
        "浅" to rotate(base, 0f, 0.8f, 0.16f),
        "基准" to base,
        "深" to rotate(base, 0f, 1f, -0.14f),
        "最深" to rotate(base, 0f, 1f, -0.26f)
    )
    1 -> listOf(
        "基准" to base,
        "基准浅" to rotate(base, 0f, 0.8f, 0.18f),
        "互补" to rotate(base, 180f),
        "互补浅" to rotate(base, 180f, 0.8f, 0.18f),
        "中性" to rotate(base, 0f, 0.12f, 0.1f)
    )
    2 -> listOf(
        "基准" to base,
        "三分之一" to rotate(base, 120f),
        "三分之二" to rotate(base, 240f),
        "基准浅" to rotate(base, 0f, 0.7f, 0.2f),
        "中性" to rotate(base, 0f, 0.1f, 0.15f)
    )
    3 -> listOf(
        "基准" to base,
        "第二" to rotate(base, 90f),
        "第三" to rotate(base, 180f),
        "第四" to rotate(base, 270f),
        "中性" to rotate(base, 0f, 0.1f, 0.12f)
    )
    else -> listOf(
        "偏左二" to rotate(base, -60f),
        "偏左" to rotate(base, -30f),
        "基准" to base,
        "偏右" to rotate(base, 30f),
        "偏右二" to rotate(base, 60f)
    )
}

@Composable
fun ColorSchemeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val copy = rememberCopy()

    var hexInput by rememberSaveable { mutableStateOf("#007AFF") }
    var schemeIndex by rememberSaveable { mutableStateOf(1) }

    val base = parseHexColor(hexInput) ?: palette.accent
    val scheme = buildScheme(base, schemeIndex)
    val (h, s, l) = rgbToHsl(base)

    val onWhite = contrastRatio(base, Color.White)
    val onBlack = contrastRatio(base, Color.Black)

    val presets = listOf(
        "#007AFF", "#34C759", "#FF9500", "#FF3B30", "#AF52DE", "#5AC8FA", "#FFCC00", "#8E8E93"
    )

    ToolScaffold {
        item { SectionHeader("配色方案") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        scheme.forEach { (_, color) ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(color)
                                    .clickable { copy(hexOf(color)) }
                            )
                        }
                    }
                    Text(
                        "点任意色块复制它的十六进制值。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("色值") }
        item {
            GroupedCard {
                scheme.forEachIndexed { index, (name, color) ->
                    KeyValueRow(name, hexOf(color))
                    if (index != scheme.lastIndex) RowDivider()
                }
            }
        }
        item { SectionHeader("基准色") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        placeholder = "#RRGGBB",
                        mono = true
                    )
                    if (parseHexColor(hexInput) == null) {
                        Text(
                            "格式不对，要六位十六进制，例如 #007AFF",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.take(4).forEach { hex ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1.6f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parseHexColor(hex) ?: Color.Gray)
                                    .clickable { hexInput = hex }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.drop(4).forEach { hex ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1.6f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parseHexColor(hex) ?: Color.Gray)
                                    .clickable { hexInput = hex }
                            )
                        }
                    }
                    SegmentedPicker(
                        options = schemeNames,
                        selectedIndex = schemeIndex,
                        onSelected = { schemeIndex = it }
                    )
                }
            }
        }
        item { SectionHeader("基准色信息") }
        item {
            GroupedCard {
                KeyValueRow("十六进制", hexOf(base))
                RowDivider()
                KeyValueRow(
                    "RGB",
                    (base.red * 255).toInt().toString() + ", " +
                        (base.green * 255).toInt() + ", " + (base.blue * 255).toInt()
                )
                RowDivider()
                KeyValueRow(
                    "HSL",
                    Math.round(h).toString() + "°, " +
                        Math.round(s * 100) + "%, " + Math.round(l * 100) + "%"
                )
            }
        }
        item { SectionHeader("可读性对比度") }
        item {
            GroupedCard {
                KeyValueRow(
                    "白底黑字对比",
                    String.format("%.2f", onWhite) + "　" + (if (onWhite >= 4.5) "达标" else "不达标"),
                    copyable = false
                )
                RowDivider()
                KeyValueRow(
                    "黑底对比",
                    String.format("%.2f", onBlack) + "　" + (if (onBlack >= 4.5) "达标" else "不达标"),
                    copyable = false
                )
                RowDivider()
                KeyValueRow(
                    "建议文字色",
                    if (onWhite > onBlack) "在这个颜色上用白字" else "在这个颜色上用黑字",
                    copyable = false
                )
            }
        }
        item { SectionHeader("导出") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(
                        text = buildString {
                            appendLine("/* " + schemeNames[schemeIndex] + "配色 */")
                            appendLine(":root {")
                            scheme.forEachIndexed { index, (name, color) ->
                                appendLine("  --color-" + (index + 1) + ": " + hexOf(color) + "; /* " + name + " */")
                            }
                            append("}")
                        },
                        label = "CSS 变量"
                    )
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "对比度按 WCAG 标准算，正文要 4.5 以上，大标题 3 以上才算无障碍达标。全部本地计算。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
