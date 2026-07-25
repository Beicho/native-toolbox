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

private const val KITCHEN = 0
private const val RECYCLABLE = 1
private const val HAZARD = 2
private const val OTHER = 3

private val categoryNames = listOf("厨余垃圾", "可回收物", "有害垃圾", "其他垃圾")
private val categoryDesc = listOf(
    "会腐烂的food和植物残余。投放前尽量沥干水分，包装袋要另外扔。",
    "干净、干燥、能再利用的。脏了油了的纸盒算其他垃圾。",
    "含重金属或有毒物质，会污染环境。要投到专门的收集点。",
    "以上都不属于的日常废弃物。不确定就扔这里，别污染可回收。"
)

private val items: List<Pair<String, Int>> = listOf(
    // 厨余
    "剩饭剩菜" to KITCHEN, "菜叶菜根" to KITCHEN, "果皮果核" to KITCHEN, "鱼骨" to KITCHEN,
    "鸡骨" to KITCHEN, "蛋壳" to KITCHEN, "茶叶渣" to KITCHEN, "咖啡渣" to KITCHEN,
    "过期食品" to KITCHEN, "花卉绿植" to KITCHEN, "西瓜皮" to KITCHEN, "面包蛋糕" to KITCHEN,
    "中药药渣" to KITCHEN, "坚果壳" to KITCHEN,
    // 可回收
    "纸箱" to RECYCLABLE, "报纸书本" to RECYCLABLE, "塑料瓶" to RECYCLABLE, "玻璃瓶" to RECYCLABLE,
    "易拉罐" to RECYCLABLE, "铁罐" to RECYCLABLE, "旧衣服" to RECYCLABLE, "毛绒玩具" to RECYCLABLE,
    "锅碗瓢盆" to RECYCLABLE, "旧家电" to RECYCLABLE, "充电宝" to RECYCLABLE, "数据线" to RECYCLABLE,
    "泡沫塑料" to RECYCLABLE, "牛奶盒" to RECYCLABLE, "旧鞋子" to RECYCLABLE, "包装纸盒" to RECYCLABLE,
    // 有害
    "过期药品" to HAZARD, "药品包装" to HAZARD, "废电池" to HAZARD, "纽扣电池" to HAZARD,
    "荧光灯管" to HAZARD, "节能灯" to HAZARD, "水银温度计" to HAZARD, "杀虫剂" to HAZARD,
    "油漆桶" to HAZARD, "指甲油" to HAZARD, "染发剂" to HAZARD, "废胶片" to HAZARD,
    "打印墨盒" to HAZARD, "消毒剂" to HAZARD,
    // 其他
    "卫生纸" to OTHER, "纸尿裤" to OTHER, "湿巾" to OTHER, "烟头" to OTHER,
    "陶瓷碎片" to OTHER, "一次性餐具" to OTHER, "污损纸张" to OTHER, "大骨头" to OTHER,
    "贝壳" to OTHER, "榴莲壳" to OTHER, "椰子壳" to OTHER, "创可贴" to OTHER,
    "口罩" to OTHER, "牙刷" to OTHER, "笔和笔芯" to OTHER, "胶带" to OTHER,
    "灰土" to OTHER, "猫砂" to OTHER, "干燥剂" to OTHER, "打火机" to OTHER
)

@Composable
fun GarbageToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var keyword by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(0) }

    val trimmed = keyword.trim()
    val searchHits = if (trimmed.isBlank()) emptyList()
    else items.filter { it.first.contains(trimmed) || categoryNames[it.second].contains(trimmed) }

    val tabItems = items.filter { it.second == tab }

    val colorOf = listOf(palette.green, palette.accent, palette.red, palette.gray)

    ToolScaffold {
        item { SectionHeader("查一样东西") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = "输入物品名称，例如 电池、西瓜皮"
                    )
                    if (trimmed.isNotBlank()) {
                        Text(
                            if (searchHits.isEmpty()) "这个词没收录。不确定的话投「其他垃圾」最稳妥。"
                            else "找到 " + searchHits.size + " 条",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (searchHits.isEmpty()) palette.orange else palette.secondaryLabel
                        )
                    }
                }
            }
        }
        if (searchHits.isNotEmpty()) {
            item { SectionHeader("查询结果") }
            item {
                GroupedCard {
                    searchHits.forEachIndexed { index, (name, category) ->
                        KeyValueRow(name, categoryNames[category], copyable = false)
                        if (index != searchHits.lastIndex) RowDivider()
                    }
                }
            }
        }
        item { SectionHeader("按分类浏览") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = categoryNames,
                        selectedIndex = tab,
                        onSelected = { tab = it }
                    )
                    Text(
                        categoryDesc[tab],
                        style = MaterialTheme.typography.bodySmall,
                        color = colorOf[tab]
                    )
                }
            }
        }
        item { SectionHeader(categoryNames[tab] + "（" + tabItems.size + " 项）") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        tabItems.joinToString("　·　") { it.first },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.label
                    )
                }
            }
        }
        item { SectionHeader("容易搞错的") }
        item {
            GroupedCard {
                val tricky = listOf(
                    "大骨头" to "其他垃圾（太硬，不易腐烂）",
                    "鸡骨鱼刺" to "厨余垃圾（细小易腐）",
                    "卫生纸" to "其他垃圾（遇水降解，不可回收）",
                    "污损纸张" to "其他垃圾（油污破坏纸浆）",
                    "药品包装" to "有害垃圾（跟着药品一起）",
                    "榴莲壳椰子壳" to "其他垃圾（太硬）",
                    "坚果壳" to "厨余垃圾（较软）",
                    "充电宝" to "可回收物（含锂电池，最好交回收点）"
                )
                tricky.forEachIndexed { index, (name, answer) ->
                    KeyValueRow(name, answer, copyable = false)
                    if (index != tricky.lastIndex) RowDivider()
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "各城市标准略有差异，这里用的是全国通行的四分类口径。以当地环卫部门公布的规则为准。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
