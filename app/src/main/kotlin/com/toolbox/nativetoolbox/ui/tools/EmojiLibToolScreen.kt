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
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.rememberCopy
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private class Sym(val text: String, val keywords: String)

private val kaomoji = listOf(
    Sym("(*^_^*)", "开心 笑"), Sym("(≧∇≦)", "开心 兴奋"), Sym("(´･ω･`)", "无奈 疑问"),
    Sym("(ﾉ*・ω・)ﾉ", "打招呼 加油"), Sym("(╯°□°）╯", "生气 掀桌"), Sym("(´；ω；`)", "哭 难过"),
    Sym("(￣▽￣)", "无语 笑"), Sym("(°ロ°)", "震惊"), Sym("(๑•̀ㅂ•́)و", "加油 决心"),
    Sym("( ˘ω˘ )", "困 睡"), Sym("(눈_눈)", "鄙视 无语"), Sym("(๐•̆ ·̭ •̆๐)", "委屈"),
    Sym("ヽ(°〇°)ﾉ", "惊讶"), Sym("(￢_￢)", "怀疑"), Sym("(´∀｀)b", "赞 好"),
    Sym("orz", "跪 拜服"), Sym("(＾Ｕ＾)ノ~ＹＯ", "打招呼"), Sym("(⁄ ⁄•⁄ω⁄•⁄ ⁄)", "羞 脸红")
)

private val emojis = listOf(
    Sym("😀", "笑 开心"), Sym("😂", "笑哭"), Sym("🥹", "感动 泪"), Sym("😭", "哭"),
    Sym("😅", "尴尬 汗"), Sym("🙂", "微笑"), Sym("😉", "眨眼"), Sym("😍", "爱 心动"),
    Sym("🤔", "思考"), Sym("😴", "睡 困"), Sym("😱", "惊恐"), Sym("😤", "生气 哼"),
    Sym("🤯", "震惊 爆炸"), Sym("🥳", "庆祝 派对"), Sym("😷", "口罩 生病"), Sym("🤖", "机器人"),
    Sym("👍", "赞 好"), Sym("👎", "差 不好"), Sym("🙏", "拜托 感谢"), Sym("👏", "鼓掌"),
    Sym("💪", "加油 强"), Sym("🤝", "握手 合作"), Sym("✌️", "耶 胜利"), Sym("👌", "OK"),
    Sym("❤️", "爱心 红"), Sym("💔", "心碎"), Sym("🔥", "火 热门"), Sym("✨", "闪 亮"),
    Sym("🎉", "庆祝 撒花"), Sym("🎁", "礼物"), Sym("💰", "钱 财"), Sym("📌", "钉 重点"),
    Sym("⚠️", "警告 注意"), Sym("✅", "对 完成"), Sym("❌", "错 取消"), Sym("❓", "问号"),
    Sym("🕐", "时间 钟"), Sym("📅", "日期 日历"), Sym("📱", "手机"), Sym("💻", "电脑"),
    Sym("🌧️", "下雨"), Sym("☀️", "太阳 晴"), Sym("🌙", "月 夜"), Sym("🌈", "彩虹"),
    Sym("🍚", "饭"), Sym("☕", "咖啡"), Sym("🍺", "啤酒"), Sym("🎂", "蛋糕 生日"),
    Sym("🐱", "猫"), Sym("🐶", "狗"), Sym("🚀", "火箭 上线"), Sym("🐛", "bug 虫")
)

private val specialChars = listOf(
    Sym("→", "箭头 右"), Sym("←", "箭头 左"), Sym("↑", "箭头 上"), Sym("↓", "箭头 下"),
    Sym("⇒", "推出 双箭头"), Sym("↔", "双向"), Sym("√", "对 勾"), Sym("×", "错 叉 乘"),
    Sym("±", "正负"), Sym("÷", "除"), Sym("≈", "约等于"), Sym("≠", "不等于"),
    Sym("≤", "小于等于"), Sym("≥", "大于等于"), Sym("∞", "无穷"), Sym("°", "度"),
    Sym("℃", "摄氏度"), Sym("％", "百分号 全角"), Sym("‰", "千分号"), Sym("¥", "人民币"),
    Sym("$", "美元"), Sym("€", "欧元"), Sym("★", "星 实心"), Sym("☆", "星 空心"),
    Sym("●", "圆点 实心"), Sym("○", "圆 空心"), Sym("■", "方块"), Sym("▲", "三角"),
    Sym("§", "章节"), Sym("№", "编号"), Sym("™", "商标"), Sym("©", "版权"),
    Sym("「」", "引号 中文"), Sym("『』", "书名 引号"), Sym("《》", "书名号"), Sym("【】", "方头括号"),
    Sym("…", "省略号"), Sym("—", "破折号"), Sym("·", "间隔号"), Sym("　", "全角空格")
)

private val groups = listOf(
    "颜文字" to kaomoji,
    "表情" to emojis,
    "符号" to specialChars
)

@Composable
fun EmojiLibToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val copy = rememberCopy()

    var tab by rememberSaveable { mutableStateOf(0) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var basket by rememberSaveable { mutableStateOf("") }

    val trimmed = keyword.trim()
    val source = if (trimmed.isBlank()) groups[tab].second
    else groups.flatMap { it.second }.filter { it.keywords.contains(trimmed) || it.text.contains(trimmed) }

    ToolScaffold {
        item { SectionHeader("搜索") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = "输入中文描述，例如 开心、箭头、警告"
                    )
                    if (trimmed.isBlank()) {
                        SegmentedPicker(
                            options = groups.map { it.first },
                            selectedIndex = tab,
                            onSelected = { tab = it }
                        )
                    }
                    Text(
                        if (trimmed.isBlank()) "点任意一个直接复制，长按加进下面的组合框"
                        else "搜到 " + source.size + " 个",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader(if (trimmed.isBlank()) groups[tab].first else "搜索结果") }
        item {
            GroupedCard {
                CardPadding {
                    if (source.isEmpty()) {
                        Text(
                            "没找到，换个词试试",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    } else {
                        source.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { sym ->
                                    SolidButton(
                                        onClick = {
                                            copy(sym.text)
                                            basket += sym.text
                                        },
                                        modifier = Modifier.weight(1f),
                                        filled = false,
                                        height = 46.dp
                                    ) { Text(sym.text, fontSize = 16.sp) }
                                }
                                repeat(4 - row.size) {
                                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader("组合框") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = basket,
                        onValueChange = { basket = it },
                        placeholder = "点上面的符号会累加到这里，可以拼一串再一次复制"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { copy(basket) },
                            modifier = Modifier.weight(1f),
                            enabled = basket.isNotBlank()
                        ) { Text("复制这一串") }
                        SolidButton(
                            onClick = { basket = "" },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = basket.isNotBlank()
                        ) { Text("清空") }
                    }
                    Text(
                        "部分表情在不同系统上显示会有差异，颜文字和符号最稳。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
