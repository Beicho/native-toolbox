package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/**
 * 亲戚称呼推算：把关系链拆成一步步的基本关系，用查表逐级推进。
 * 表以「我」为起点，key = 当前称呼 + 下一步关系。覆盖日常够用的三代范围。
 */
private val steps = listOf(
    "父亲" to "爸爸",
    "母亲" to "妈妈",
    "哥哥" to "哥哥",
    "弟弟" to "弟弟",
    "姐姐" to "姐姐",
    "妹妹" to "妹妹",
    "儿子" to "儿子",
    "女儿" to "女儿",
    "丈夫" to "老公",
    "妻子" to "老婆"
)

private val table: Map<Pair<String, String>, String> = buildMap {
    // 第一代
    put("我" to "父亲", "爸爸")
    put("我" to "母亲", "妈妈")
    put("我" to "哥哥", "哥哥")
    put("我" to "弟弟", "弟弟")
    put("我" to "姐姐", "姐姐")
    put("我" to "妹妹", "妹妹")
    put("我" to "儿子", "儿子")
    put("我" to "女儿", "女儿")
    put("我" to "丈夫", "老公")
    put("我" to "妻子", "老婆")

    // 父亲往上和旁系
    put("爸爸" to "父亲", "爷爷")
    put("爸爸" to "母亲", "奶奶")
    put("爸爸" to "哥哥", "伯父")
    put("爸爸" to "弟弟", "叔叔")
    put("爸爸" to "姐姐", "姑姑")
    put("爸爸" to "妹妹", "姑姑")
    put("爸爸" to "儿子", "兄弟")
    put("爸爸" to "女儿", "姐妹")
    put("爸爸" to "妻子", "妈妈")

    // 母亲往上和旁系
    put("妈妈" to "父亲", "外公")
    put("妈妈" to "母亲", "外婆")
    put("妈妈" to "哥哥", "舅舅")
    put("妈妈" to "弟弟", "舅舅")
    put("妈妈" to "姐姐", "姨妈")
    put("妈妈" to "妹妹", "姨妈")
    put("妈妈" to "丈夫", "爸爸")

    // 祖辈
    put("爷爷" to "父亲", "曾祖父")
    put("爷爷" to "母亲", "曾祖母")
    put("爷爷" to "哥哥", "伯祖父")
    put("爷爷" to "弟弟", "叔祖父")
    put("爷爷" to "妻子", "奶奶")
    put("奶奶" to "丈夫", "爷爷")
    put("外公" to "妻子", "外婆")
    put("外婆" to "丈夫", "外公")

    // 伯叔姑舅姨的配偶与子女
    put("伯父" to "妻子", "伯母")
    put("叔叔" to "妻子", "婶婶")
    put("姑姑" to "丈夫", "姑父")
    put("舅舅" to "妻子", "舅妈")
    put("姨妈" to "丈夫", "姨父")
    put("伯父" to "儿子", "堂哥或堂弟")
    put("伯父" to "女儿", "堂姐或堂妹")
    put("叔叔" to "儿子", "堂哥或堂弟")
    put("叔叔" to "女儿", "堂姐或堂妹")
    put("姑姑" to "儿子", "表哥或表弟")
    put("姑姑" to "女儿", "表姐或表妹")
    put("舅舅" to "儿子", "表哥或表弟")
    put("舅舅" to "女儿", "表姐或表妹")
    put("姨妈" to "儿子", "表哥或表弟")
    put("姨妈" to "女儿", "表姐或表妹")

    // 兄弟姐妹的配偶与子女
    put("哥哥" to "妻子", "嫂子")
    put("弟弟" to "妻子", "弟媳")
    put("姐姐" to "丈夫", "姐夫")
    put("妹妹" to "丈夫", "妹夫")
    put("哥哥" to "儿子", "侄子")
    put("哥哥" to "女儿", "侄女")
    put("弟弟" to "儿子", "侄子")
    put("弟弟" to "女儿", "侄女")
    put("姐姐" to "儿子", "外甥")
    put("姐姐" to "女儿", "外甥女")
    put("妹妹" to "儿子", "外甥")
    put("妹妹" to "女儿", "外甥女")

    // 子女方向
    put("儿子" to "儿子", "孙子")
    put("儿子" to "女儿", "孙女")
    put("儿子" to "妻子", "儿媳")
    put("女儿" to "儿子", "外孙")
    put("女儿" to "女儿", "外孙女")
    put("女儿" to "丈夫", "女婿")
    put("孙子" to "儿子", "曾孙")
    put("孙子" to "女儿", "曾孙女")

    // 配偶方向
    put("老公" to "父亲", "公公")
    put("老公" to "母亲", "婆婆")
    put("老公" to "哥哥", "大伯")
    put("老公" to "弟弟", "小叔子")
    put("老公" to "姐姐", "大姑姐")
    put("老公" to "妹妹", "小姑子")
    put("老婆" to "父亲", "岳父")
    put("老婆" to "母亲", "岳母")
    put("老婆" to "哥哥", "大舅子")
    put("老婆" to "弟弟", "小舅子")
    put("老婆" to "姐姐", "大姨子")
    put("老婆" to "妹妹", "小姨子")
    put("老婆" to "儿子", "儿子")
    put("老公" to "儿子", "儿子")
}

@Composable
fun RelativeNameToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var chain by rememberSaveable { mutableStateOf("") }

    val parts = chain.split("→").filter { it.isNotBlank() }
    var current = "我"
    var failedAt = -1
    parts.forEachIndexed { index, step ->
        if (failedAt >= 0) return@forEachIndexed
        val next = table[current to step]
        if (next == null) failedAt = index else current = next
    }

    val result = when {
        parts.isEmpty() -> ""
        failedAt >= 0 -> ""
        else -> current
    }

    ToolScaffold {
        item { SectionHeader("称呼结果") }
        item {
            GroupedCard {
                CardPadding {
                    if (result.isNotBlank()) {
                        Text(
                            "应该叫「" + result + "」",
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.accent
                        )
                    } else if (parts.isEmpty()) {
                        Text(
                            "点下面的按钮，一步步描述关系。比如「爸爸 → 哥哥 → 儿子」就是堂兄弟。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    } else {
                        Text(
                            "推到第 " + (failedAt + 1) + " 步走不通了。这条关系链比较罕见，本表覆盖三代内的常见称呼。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.orange
                        )
                    }
                    if (parts.isNotEmpty()) {
                        OutputCard(text = "我 → " + parts.joinToString(" → "), label = "关系链")
                    }
                }
            }
        }
        item { SectionHeader("添加一步") }
        item {
            GroupedCard {
                CardPadding {
                    steps.chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (key, label) ->
                                SolidButton(
                                    onClick = { chain = if (chain.isBlank()) key else chain + "→" + key },
                                    modifier = Modifier.weight(1f),
                                    filled = false,
                                    height = 40.dp
                                ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { chain = parts.dropLast(1).joinToString("→") },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = parts.isNotEmpty()
                        ) { Text("退一步") }
                        SolidButton(
                            onClick = { chain = "" },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = parts.isNotEmpty()
                        ) { Text("重新开始") }
                    }
                }
            }
        }
    }
}
