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
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

private class Shape(
    val name: String,
    val inputs: List<String>,
    val compute: (List<Double>) -> List<Pair<String, Double>>
)

private val shapes = listOf(
    Shape("圆形", listOf("半径")) { v ->
        val r = v[0]
        listOf("周长" to 2 * PI * r, "面积" to PI * r * r, "直径" to 2 * r)
    },
    Shape("矩形", listOf("长", "宽")) { v ->
        val (a, b) = v[0] to v[1]
        listOf("周长" to 2 * (a + b), "面积" to a * b, "对角线" to sqrt(a * a + b * b))
    },
    Shape("三角形", listOf("边 a", "边 b", "边 c")) { v ->
        val (a, b, c) = Triple(v[0], v[1], v[2])
        val s = (a + b + c) / 2
        val valid = a + b > c && a + c > b && b + c > a && a > 0 && b > 0 && c > 0
        val area = if (valid) sqrt(s * (s - a) * (s - b) * (s - c)) else Double.NaN
        listOf("周长" to a + b + c, "面积" to area, "外接圆半径" to if (valid && area > 0) a * b * c / (4 * area) else Double.NaN)
    },
    Shape("梯形", listOf("上底", "下底", "高")) { v ->
        val (a, b, h) = Triple(v[0], v[1], v[2])
        listOf("面积" to (a + b) * h / 2, "中位线" to (a + b) / 2)
    },
    Shape("球体", listOf("半径")) { v ->
        val r = v[0]
        listOf("表面积" to 4 * PI * r * r, "体积" to 4.0 / 3.0 * PI * r.pow(3), "大圆周长" to 2 * PI * r)
    },
    Shape("圆柱", listOf("半径", "高")) { v ->
        val (r, h) = v[0] to v[1]
        listOf("侧面积" to 2 * PI * r * h, "表面积" to 2 * PI * r * (r + h), "体积" to PI * r * r * h)
    },
    Shape("圆锥", listOf("半径", "高")) { v ->
        val (r, h) = v[0] to v[1]
        val l = sqrt(r * r + h * h)
        listOf("母线" to l, "侧面积" to PI * r * l, "表面积" to PI * r * (r + l), "体积" to PI * r * r * h / 3)
    },
    Shape("长方体", listOf("长", "宽", "高")) { v ->
        val (a, b, c) = Triple(v[0], v[1], v[2])
        listOf("表面积" to 2 * (a * b + a * c + b * c), "体积" to a * b * c, "体对角线" to sqrt(a * a + b * b + c * c))
    }
)

private fun fmt(value: Double): String {
    if (value.isNaN()) return "无法计算"
    if (value.isInfinite()) return "—"
    val rounded = Math.round(value * 10000.0) / 10000.0
    return if (rounded == Math.floor(rounded)) Math.round(rounded).toString() else rounded.toString()
}

@Composable
fun GeometryToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var shapeIndex by rememberSaveable { mutableStateOf(0) }
    var v1 by rememberSaveable { mutableStateOf("") }
    var v2 by rememberSaveable { mutableStateOf("") }
    var v3 by rememberSaveable { mutableStateOf("") }

    val shape = shapes[shapeIndex]
    val raw = listOf(v1, v2, v3).take(shape.inputs.size)
    val values = raw.map { it.trim().toDoubleOrNull() ?: Double.NaN }
    val ready = values.all { !it.isNaN() && it > 0 }
    val results = if (ready) shape.compute(values) else emptyList()

    val triangleInvalid = shape.name == "三角形" && ready &&
        !(values[0] + values[1] > values[2] && values[0] + values[2] > values[1] && values[1] + values[2] > values[0])

    ToolScaffold {
        item { SectionHeader("图形") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = shapes.take(4).map { it.name },
                        selectedIndex = shapeIndex.coerceAtMost(3),
                        onSelected = { shapeIndex = it }
                    )
                    SegmentedPicker(
                        options = shapes.drop(4).map { it.name },
                        selectedIndex = (shapeIndex - 4).coerceAtLeast(0),
                        onSelected = { shapeIndex = it + 4 }
                    )
                    Text(
                        "当前：" + shape.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.accent
                    )
                }
            }
        }
        item { SectionHeader("尺寸") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(value = v1, onValueChange = { v1 = it }, placeholder = shape.inputs[0], mono = true)
                    if (shape.inputs.size > 1) {
                        IosTextField(value = v2, onValueChange = { v2 = it }, placeholder = shape.inputs[1], mono = true)
                    }
                    if (shape.inputs.size > 2) {
                        IosTextField(value = v3, onValueChange = { v3 = it }, placeholder = shape.inputs[2], mono = true)
                    }
                    Text(
                        if (triangleInvalid) "这三条边构不成三角形（任意两边之和要大于第三边）"
                        else "单位自己定，输入厘米出来就是厘米和平方厘米。",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (triangleInvalid) palette.red else palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                if (results.isEmpty()) {
                    CardPadding {
                        Text(
                            "把尺寸填完整（必须大于 0）",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                } else {
                    results.forEachIndexed { index, (label, value) ->
                        KeyValueRow(label, fmt(value))
                        if (index != results.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
