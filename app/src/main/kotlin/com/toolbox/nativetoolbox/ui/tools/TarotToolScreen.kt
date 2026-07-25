package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.util.Calendar
import kotlin.random.Random

private data class TarotCard(val name: String, val symbol: String, val upright: String, val reversed: String)

// 22 大阿卡纳:全牌含义
private val MAJOR = listOf(
    TarotCard("愚者", "0", "新的开始、冒险、无限可能。放下顾虑,大胆迈出第一步。", "冲动行事、准备不足。先想清楚再出发,别裸辞式冒险。"),
    TarotCard("魔术师", "I", "创造力上线、资源齐备。你有把想法变成现实的全部工具。", "光说不练、小聪明。别把精力花在包装上,拿出真东西。"),
    TarotCard("女祭司", "II", "直觉敏锐、适合沉淀学习。答案在你心里,安静下来就能听到。", "忽视直觉、被表象迷惑。你其实早就知道答案,别骗自己。"),
    TarotCard("女皇", "III", "丰盛、滋养、成果落地。适合享受生活、照顾自己和身边人。", "过度付出或过度享乐。先把自己的杯子倒满,再去照顾别人。"),
    TarotCard("皇帝", "IV", "秩序、掌控、稳步推进。定好规则和框架,事情自然运转。", "控制欲过强或权威受挑战。松一点,不是所有事都得听你的。"),
    TarotCard("教皇", "V", "学习传统、寻求指导。找个靠谱的前辈或导师,少走弯路。", "教条束缚、不敢打破常规。规则是参考,不是枷锁。"),
    TarotCard("恋人", "VI", "重要的选择、心意相通。跟随内心的选择,通常不会错。", "犹豫不决、价值观冲突。两头都想要,往往两头都落空。"),
    TarotCard("战车", "VII", "意志坚定、高歌猛进。目标明确,现在就是全力冲刺的时候。", "方向失控、用力过猛。先确认方向盘在手里,再踩油门。"),
    TarotCard("力量", "VIII", "以柔克刚、内在强大。温和而坚定,比强硬更有力量。", "自我怀疑、硬碰硬。你比自己以为的更强,别急着对抗。"),
    TarotCard("隐者", "IX", "独处思考、向内探索。暂时离开喧嚣,答案会自己浮现。", "过度封闭、逃避社交。独处是充电,不是躲起来。"),
    TarotCard("命运之轮", "X", "转机来了、时来运转。顺势而为,机会窗口正在打开。", "时运不济、循环重复。低谷是周期的一部分,撑住就好。"),
    TarotCard("正义", "XI", "公平裁决、因果分明。诚实面对,该是你的跑不掉。", "失衡、逃避责任。出来混总要还,早面对早解脱。"),
    TarotCard("倒吊人", "XII", "换个角度、以退为进。暂停不是损失,是看清全局的机会。", "无谓牺牲、卡在原地。停下可以,但别把等待当成努力。"),
    TarotCard("死神", "XIII", "旧的结束、新的开始。断舍离的时候到了,结束即重生。", "抗拒改变、拖着不放。越舍不得,越难开始新篇章。"),
    TarotCard("节制", "XIV", "平衡、调和、细水长流。不急不躁,慢慢来反而快。", "失衡、极端摇摆。工作和生活、付出和收获,该调一调了。"),
    TarotCard("恶魔", "XV", "被欲望或习惯捆绑。看见枷锁是挣脱的第一步。", "挣脱束缚、重获自由。你已经在松绑的路上,继续。"),
    TarotCard("高塔", "XVI", "突发变故、旧结构崩塌。塌掉的本来就不牢,重建更稳。", "惊险避过、余震未平。危机暂缓,但根本问题还得解决。"),
    TarotCard("星星", "XVII", "希望、疗愈、灵感。黑夜过去了,跟着那道光走。", "信心不足、灵感枯竭。星星一直在,只是云还没散。"),
    TarotCard("月亮", "XVIII", "迷雾、不安、直觉预警。看不清就先别走夜路,等天亮。", "迷雾散去、真相浮现。之前的担心大多是自己吓自己。"),
    TarotCard("太阳", "XIX", "成功、快乐、能量满格。大方接受好运,你值得。", "小有阴霾、快乐打折。好事是真的,别自己给自己泼冷水。"),
    TarotCard("审判", "XX", "觉醒、复盘、二次机会。过去的经验正在变成新的召唤。", "自我批判过度、不敢翻篇。复盘是为了前进,不是自责。"),
    TarotCard("世界", "XXI", "圆满完成、周期闭环。庆祝吧,然后开启下一段旅程。", "差一步圆满、有始无终。最后一公里,坚持走完。"),
)

private val SUITS = listOf(
    Triple("权杖", "🔥", "行动与热情"),
    Triple("圣杯", "💧", "情感与关系"),
    Triple("宝剑", "🌬️", "思维与沟通"),
    Triple("星币", "🪙", "物质与事业"),
)

// 小阿卡纳 56 张:每张给独立含义(数字牌按牌意口诀,宫廷牌按人格)
private val MINOR_MEANING = mapOf(
    // 数字牌通义:花色 x 数字
    "权杖1" to Pair("新项目点火,热情爆棚,干就完了。", "雷声大雨点小,热度三分钟。"),
    "权杖2" to Pair("站在规划图前,世界在等你选方向。", "安于现状,不敢跨出舒适区。"),
    "权杖3" to Pair("船已出海,初步成果在路上。", "计划受阻,远景不如预期。"),
    "权杖4" to Pair("阶段性庆祝,根基稳固,值得开心。", "庆祝略早,基础还需加固。"),
    "权杖5" to Pair("良性竞争,摩擦中出火花。", "内耗严重,争而无果。"),
    "权杖6" to Pair("凯旋时刻,努力被看见被认可。", "掌声迟到,或名不副实的压力。"),
    "权杖7" to Pair("守住高地,你有能力应对挑战。", "疲于应付,考虑是否值得死守。"),
    "权杖8" to Pair("进展飞速,消息与机会齐飞。", "节奏太快失控,或迟迟没有动静。"),
    "权杖9" to Pair("最后一道坎,伤痕累累也要站稳。", "戒备过度,累到不想再撑。"),
    "权杖10" to Pair("担子很重但终点在望,咬牙搬完。", "揽了太多,学会放下和分担。"),
    "权杖侍从" to Pair("好奇心旺盛的探索者,消息带来机会。", "三分钟热度,消息不靠谱。"),
    "权杖骑士" to Pair("冲劲十足,说走就走的行动派。", "冲动鲁莽,虎头蛇尾。"),
    "权杖王后" to Pair("自信有魅力,热情感染全场。", "强势过头,嫉妒或自我怀疑。"),
    "权杖国王" to Pair("有远见的领导者,敢想敢干。", "独断专行,画饼不兑现。"),
    "圣杯1" to Pair("新感情或新灵感涌现,心被填满。", "情感堵塞,给出去的爱没有回音。"),
    "圣杯2" to Pair("两情相悦,伙伴关系天作之合。", "关系失衡,貌合神离。"),
    "圣杯3" to Pair("和朋友庆祝,友谊滋养生活。", "社交过载,或圈子里有小裂痕。"),
    "圣杯4" to Pair("对眼前的好意兴趣缺缺,需要新刺激。", "从倦怠中醒来,重新看见身边的好。"),
    "圣杯5" to Pair("为打翻的牛奶哭泣,但还有两杯没倒。", "开始放下遗憾,转身看到剩下的。"),
    "圣杯6" to Pair("童年、旧友、怀旧的温暖。", "沉溺过去,该向前看了。"),
    "圣杯7" to Pair("选择太多如梦似幻,小心画大饼。", "迷雾散去,终于看清哪个是真的。"),
    "圣杯8" to Pair("放下已有的,去寻找更深的意义。", "想走又舍不得,徘徊不定。"),
    "圣杯9" to Pair("愿望达成,心满意足的一张牌。", "满足流于表面,内心仍有空缺。"),
    "圣杯10" to Pair("家庭和睦,情感圆满,彩虹当空。", "表面幸福下的小裂缝,需要沟通。"),
    "圣杯侍从" to Pair("浪漫的信使,直觉和创意的小惊喜。", "情绪化、幻想过多。"),
    "圣杯骑士" to Pair("白马骑士带着心意而来,浪漫提案。", "承诺动听但难兑现。"),
    "圣杯王后" to Pair("温柔共情,情绪稳定的疗愈者。", "情绪泛滥,过度共情耗尽自己。"),
    "圣杯国王" to Pair("情绪成熟,风浪中保持慈悲与稳定。", "情绪压抑,或用感情操控。"),
    "宝剑1" to Pair("头脑清明,真相大白,一剑破局。", "思路混乱,误判形势。"),
    "宝剑2" to Pair("蒙眼持剑,僵局中拖延决定。", "僵局松动,不得不面对选择。"),
    "宝剑3" to Pair("心碎时刻,痛但看得清楚。", "伤口开始愈合,原谅正在发生。"),
    "宝剑4" to Pair("强制休息,充电后再战。", "休息够了,该起来行动了。"),
    "宝剑5" to Pair("赢了争吵输了关系,代价太大。", "放下争端,及时止损。"),
    "宝剑6" to Pair("渡向平静水域,离开是为了更好。", "行李太重渡不了河,先放下过去。"),
    "宝剑7" to Pair("策略行事,但别耍小聪明。", "计谋败露,坦白从宽。"),
    "宝剑8" to Pair("自我设限,其实绳子是松的。", "挣脱心理牢笼,原来门没锁。"),
    "宝剑9" to Pair("深夜焦虑,担心的九成不会发生。", "噩梦醒来,焦虑开始消退。"),
    "宝剑10" to Pair("谷底了,再坏也就这样,天快亮了。", "从谷底爬起,复原比想象快。"),
    "宝剑侍从" to Pair("机敏的观察者,新想法新消息。", "流言蜚语,说话欠考虑。"),
    "宝剑骑士" to Pair("目标明确,火力全开冲向目标。", "横冲直撞,伤人伤己。"),
    "宝剑王后" to Pair("清醒独立,界限分明,一针见血。", "刻薄冷漠,理性过头缺了温度。"),
    "宝剑国王" to Pair("理性权威,规则与逻辑的主人。", "冷酷滥权,钻牛角尖。"),
    "星币1" to Pair("实实在在的新机会,种子已到手。", "机会溜走,或计划缺乏落地性。"),
    "星币2" to Pair("多线并行还能跳舞,灵活平衡。", "球太多接不住,该做减法了。"),
    "星币3" to Pair("团队协作出精品,手艺被认可。", "配合不畅,标准不一。"),
    "星币4" to Pair("守住财富和成果,稳字当头。", "抓得太紧,守财反而错过机会。"),
    "星币5" to Pair("寒风中同行,困难是暂时的,门就在旁边。", "转机出现,援手已伸出,记得接住。"),
    "星币6" to Pair("有来有往,给予和接受都平衡。", "施与受失衡,附带条件的帮助。"),
    "星币7" to Pair("耐心等待收成,评估下一步投入。", "投入产出不成正比,考虑换方向。"),
    "星币8" to Pair("匠人模式,一锤一锤打磨技能。", "机械重复,忘了为什么出发。"),
    "星币9" to Pair("独立富足,享受自己挣来的花园。", "过度工作换来的孤独,记得生活。"),
    "星币10" to Pair("家业丰厚,长期积累开花结果。", "家庭与财务的纠纷,长期规划动摇。"),
    "星币侍从" to Pair("踏实的学徒,新技能新副业的种子。", "拖延学习,计划停在纸面。"),
    "星币骑士" to Pair("最勤恳的骑士,慢但一定到。", "过于保守,原地踏步。"),
    "星币王后" to Pair("务实温暖,把日子过成花园。", "顾此失彼,忙到忘了自己。"),
    "星币国王" to Pair("事业有成,点石成金的稳健掌舵人。", "唯利是图,用财富衡量一切。"),
)

private fun fullDeck(): List<TarotCard> {
    val minor = mutableListOf<TarotCard>()
    for ((suit, emoji, _) in SUITS) {
        val ranks = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "侍从", "骑士", "王后", "国王")
        for (r in ranks) {
            val key = suit + r
            val (up, rev) = MINOR_MEANING[key] ?: Pair("", "")
            val display = if (r == "1") "$suit王牌" else "$suit$r"
            minor.add(TarotCard(display, emoji, up, rev))
        }
    }
    return MAJOR + minor
}

private data class Draw(val card: TarotCard, val reversed: Boolean)

private fun drawCards(seed: Long?, n: Int): List<Draw> {
    val deck = fullDeck()
    val rnd = if (seed != null) Random(seed) else Random(System.nanoTime())
    val picked = deck.shuffled(rnd).take(n)
    return picked.map { Draw(it, rnd.nextBoolean()) }
}

@Composable
private fun TarotCardView(draw: Draw, label: String?, palette: com.toolbox.nativetoolbox.ui.theme.IosPalette) {
    Column(Modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
            Spacer(Modifier.height(6.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2E2157), Color(0xFF1A1333), Color(0xFF0F0B22))
                    )
                )
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(draw.card.symbol, fontSize = 34.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    draw.card.name + if (draw.reversed) "(逆位)" else "(正位)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEDE7FF),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (draw.reversed) draw.card.reversed else draw.card.upright,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCEC4F0),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TarotToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var mode by rememberSaveable { mutableStateOf(0) } // 0 每日一抽 1 自由抽 2 三牌阵
    var freeDraws by remember { mutableStateOf<List<Draw>>(emptyList()) }
    var spreadDraws by remember { mutableStateOf<List<Draw>>(emptyList()) }

    // 每日一抽:用「年月日」当种子,当天结果固定
    val todaySeed = remember {
        val c = Calendar.getInstance()
        c.get(Calendar.YEAR) * 10000L + (c.get(Calendar.MONTH) + 1) * 100L + c.get(Calendar.DAY_OF_MONTH)
    }
    val daily = remember(todaySeed) { drawCards(todaySeed, 1).first() }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("每日一张", "自由抽", "三牌阵"), mode, { mode = it }, Modifier.fillMaxWidth())
                }
            }
        }
        when (mode) {
            0 -> {
                item { SectionHeader("今日运势 · 当天不变") }
                item { TarotCardView(daily, null, palette) }
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "每天零点换一张,想多抽切「自由抽」",
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel,
                        textAlign = TextAlign.Center
                    )
                }
            }
            1 -> {
                item {
                    GroupedCard {
                        CardPadding {
                            Text("心里默念问题,然后抽一张", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                            Spacer(Modifier.height(10.dp))
                            SolidButton(onClick = { freeDraws = drawCards(null, 1) }, Modifier.fillMaxWidth()) {
                                Text(if (freeDraws.isEmpty()) "抽一张" else "再抽一张")
                            }
                        }
                    }
                }
                if (freeDraws.isNotEmpty()) {
                    item { TarotCardView(freeDraws.first(), null, palette) }
                }
            }
            else -> {
                item {
                    GroupedCard {
                        CardPadding {
                            Text("过去 · 现在 · 未来,一次看清来龙去脉", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                            Spacer(Modifier.height(10.dp))
                            SolidButton(onClick = { spreadDraws = drawCards(null, 3) }, Modifier.fillMaxWidth()) {
                                Text(if (spreadDraws.isEmpty()) "开牌" else "重新开牌")
                            }
                        }
                    }
                }
                if (spreadDraws.size == 3) {
                    item { TarotCardView(spreadDraws[0], "过去", palette) }
                    item { TarotCardView(spreadDraws[1], "现在", palette) }
                    item { TarotCardView(spreadDraws[2], "未来", palette) }
                }
            }
        }
        item {
            Text(
                "娱乐解闷用,人生大事还是你自己说了算",
                Modifier.fillMaxWidth().padding(20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = palette.tertiaryLabel,
                textAlign = TextAlign.Center
            )
        }
    }
}
