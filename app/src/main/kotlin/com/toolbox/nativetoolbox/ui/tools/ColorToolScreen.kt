package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.roundToInt

@Composable
fun ColorToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var r by rememberSaveable { mutableStateOf(88f) }
    var g by rememberSaveable { mutableStateOf(86f) }
    var b by rememberSaveable { mutableStateOf(214f) }
    var hexInput by rememberSaveable { mutableStateOf("") }

    val color = Color(r / 255f, g / 255f, b / 255f)
    val ri = r.roundToInt(); val gi = g.roundToInt(); val bi = b.roundToInt()
    val hex = "#%02X%02X%02X".format(ri, gi, bi)

    // HSL 计算
    val rf = ri / 255f; val gf = gi / 255f; val bf = bi / 255f
    val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf)
    val l = (max + min) / 2f
    val d = max - min
    val s = if (d == 0f) 0f else d / (1f - kotlin.math.abs(2f * l - 1f))
    val h = when {
        d == 0f -> 0f
        max == rf -> 60f * (((gf - bf) / d) % 6f)
        max == gf -> 60f * ((bf - rf) / d + 2f)
        else -> 60f * ((rf - gf) / d + 4f)
    }.let { if (it < 0) it + 360f else it }

    fun applyHex(text: String) {
        hexInput = text
        val clean = text.trim().removePrefix("#")
        if (clean.length == 6) {
            runCatching {
                r = clean.substring(0, 2).toInt(16).toFloat()
                g = clean.substring(2, 4).toInt(16).toFloat()
                b = clean.substring(4, 6).toInt(16).toFloat()
            }
        }
    }

    @Composable
    fun channelSlider(label: String, value: Float, tint: Color, onChange: (Float) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.width(20.dp), style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = tint,
                    inactiveTrackColor = palette.fill
                )
            )
            Text(
                value.roundToInt().toString(),
                Modifier.width(36.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.label
            )
        }
    }

    ToolScaffold {
        item { SectionHeader("预览") }
        item {
            GroupedCard {
                CardPadding {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color)
                    )
                    IosTextField(
                        value = hexInput,
                        onValueChange = { applyHex(it) },
                        placeholder = "输入 HEX,如 #5856D6",
                        mono = true
                    )
                }
            }
        }
        item { SectionHeader("RGB 调节") }
        item {
            GroupedCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    channelSlider("R", r, Color(0xFFFF3B30)) { r = it }
                    channelSlider("G", g, Color(0xFF34C759)) { g = it }
                    channelSlider("B", b, Color(0xFF007AFF)) { b = it }
                }
            }
        }
        item { SectionHeader("格式(点击复制)") }
        item {
            GroupedCard {
                KeyValueRow("HEX", hex)
                RowDivider()
                KeyValueRow("RGB", "rgb($ri, $gi, $bi)")
                RowDivider()
                KeyValueRow("HSL", "hsl(${h.roundToInt()}, ${(s * 100).roundToInt()}%, ${(l * 100).roundToInt()}%)")
                RowDivider()
                KeyValueRow("Compose", "Color(0xFF${hex.removePrefix("#")})")
            }
        }
    }
}
