package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import kotlin.random.Random

// ---- 谁是卧底词库(平民词 / 卧底词) ----
private val UNDERCOVER_WORDS = listOf(
    "苹果" to "梨", "可乐" to "雪碧", "牛奶" to "豆浆", "包子" to "饺子", "火锅" to "麻辣烫",
    "奶茶" to "咖啡", "薯条" to "薯片", "冰淇淋" to "雪糕", "披萨" to "馅饼", "寿司" to "饭团",
    "面条" to "米线", "烧烤" to "铁板烧", "蛋糕" to "面包", "粽子" to "汤圆", "辣条" to "辣片",
    "老虎" to "狮子", "乌龟" to "甲鱼", "蝴蝶" to "蜜蜂", "企鹅" to "海豹", "熊猫" to "考拉",
    "兔子" to "仓鼠", "鲨鱼" to "鲸鱼", "麻雀" to "燕子", "青蛙" to "蟾蜍", "螃蟹" to "龙虾",
    "医生" to "护士", "警察" to "保安", "老师" to "教授", "歌手" to "主播", "厨师" to "面点师",
    "司机" to "骑手", "演员" to "模特", "作家" to "编剧", "画家" to "设计师", "军人" to "武警",
    "眼镜" to "墨镜", "手表" to "手环", "雨伞" to "雨衣", "拖鞋" to "凉鞋", "围巾" to "披肩",
    "口红" to "唇膏", "香水" to "花露水", "钱包" to "卡包", "背包" to "行李箱", "帽子" to "头盔",
    "枕头" to "抱枕", "被子" to "毯子", "沙发" to "躺椅", "冰箱" to "冰柜", "空调" to "风扇",
    "电梯" to "扶梯", "高铁" to "地铁", "飞机" to "直升机", "轮船" to "游艇", "自行车" to "电动车",
    "篮球" to "排球", "足球" to "橄榄球", "乒乓球" to "羽毛球", "游泳" to "潜水", "跑步" to "竞走",
    "瑜伽" to "普拉提", "拳击" to "散打", "滑板" to "轮滑", "滑雪" to "滑冰", "钓鱼" to "捞鱼",
    "微信" to "QQ", "抖音" to "快手", "淘宝" to "拼多多", "支付宝" to "云闪付", "微博" to "贴吧",
    "王者荣耀" to "英雄联盟", "和平精英" to "穿越火线", "原神" to "崩坏", "斗地主" to "麻将", "五子棋" to "围棋",
    "结婚" to "订婚", "生日" to "周年", "春节" to "元旦", "中秋" to "端午", "情人节" to "七夕",
    "月亮" to "太阳", "星星" to "流星", "彩虹" to "极光", "白云" to "雾", "小雨" to "毛毛雨",
    "沙滩" to "海岛", "森林" to "竹林", "沙漠" to "戈壁", "瀑布" to "喷泉", "温泉" to "澡堂",
    "小说" to "漫画", "电影" to "电视剧", "演唱会" to "音乐节", "博物馆" to "美术馆", "动物园" to "植物园",
    "洗发水" to "护发素", "牙膏" to "牙粉", "肥皂" to "沐浴露", "纸巾" to "湿巾", "拖把" to "扫把",
)

// ---- 真心话题库 ----
private val TRUTH_QUESTIONS = listOf(
    "你手机里最舍不得删的一张照片是什么?",
    "最近一次撒谎是什么时候,为了什么?",
    "在座的人里,你最想和谁互换一天人生?",
    "你做过最勇敢的一件事是什么?",
    "小时候干过最丢人的事是什么?",
    "你最后悔花的一笔钱是什么?",
    "如果明天可以辞职/休学,你第一件事做什么?",
    "你暗恋过的第一个人是什么样的?",
    "你被人误会最深的一次是什么?",
    "手机相册里最近一张截图是什么?现在展示。",
    "你最近一次哭是因为什么?",
    "如果只能留一个 App,你留哪个?",
    "你收过最难忘的礼物是什么?",
    "你现在最想删掉的一段聊天记录是和谁的?",
    "有没有一件事,你从来没告诉过任何人?可以只说类型。",
    "你觉得自己最大的优点和缺点是什么?",
    "过去一年你最骄傲的一件事?",
    "你最怕别人问你什么问题?",
    "如果能回到过去改变一件事,你会改什么?",
    "你的初恋是什么时候?",
    "在座的人第一印象和现在差别最大的是谁?",
    "你最近在攒钱买什么?",
    "你最离谱的一次网购是什么?",
    "你半夜睡不着的时候在想什么?",
    "你理想中十年后的自己是什么样?",
    "你最不能接受另一半的什么行为?",
    "你最近一次心动是什么时候?",
    "你人生中最尴尬的瞬间是?",
    "如果中了五百万,第一件事做什么?",
    "你觉得在座谁最有可能先脱单/先暴富?",
    "你唱歌最拿手的一首是什么?现在来两句。",
    "你保存最久的一件东西是什么?",
    "你最近偷偷羡慕过谁?",
    "你敢公开你的屏幕使用时间吗?现在展示。",
    "如果可以拥有一个超能力,你选什么?",
    "你最怕过节还是最爱过节?为什么?",
    "你做过最疯狂的追星/追剧行为是什么?",
    "你人生中撒过最大的谎是什么?",
    "你手机备忘录里最奇怪的一条是什么?",
    "现在心里第一个浮现的人是谁?",
)

// ---- 大冒险任务库 ----
private val DARE_TASKS = listOf(
    "学一种动物叫,让大家猜是什么。",
    "给通讯录第 8 个人发「在吗,我跟你说个事」,十分钟内不许解释。",
    "用最夸张的语气朗读你最近一条朋友圈/动态。",
    "模仿在座任意一个人,直到有人猜出是谁。",
    "做 10 个深蹲,边做边喊口号。",
    "用歌声说出「我饿了想吃饭」。",
    "让左边的人给你摆一个姿势,保持 30 秒。",
    "打电话给亲友说「我跟你讲个笑话」然后冷场 5 秒挂掉。",
    "接下来三轮说话必须带「喵」结尾。",
    "展示你手机输入法打「w」出来的前三个词。",
    "用普通话+方言各说一遍「我是全场最靓的仔」。",
    "闭眼原地转三圈,然后走直线回座位。",
    "让大家给你拍一张丑照,保存 24 小时不许删。",
    "对窗外/门外大喊「明天一定早睡」。",
    "把你的微信步数展示给大家看。",
    "用嘴叼住一张纸巾保持三轮游戏。",
    "现场表演一个魔术,没有道具就徒手编一个。",
    "说出在座每个人一个优点,不许重复。",
    "接下来一轮你说的每句话都要押韵。",
    "做一个自创的舞蹈动作,并教会大家。",
    "用左手(惯用右手的话)写自己的名字给大家检查。",
    "把手机音量开到最大,播放最近听的一首歌 10 秒。",
    "跟在座一个人击掌 10 次,每次喊一个数字。",
    "模仿新闻主播播报「今晚大家都很开心」。",
    "选一个人,认真地夸他 30 秒不许笑场。",
    "用「今天天气真好」演绎开心、愤怒、悲伤三种情绪。",
    "展示你相册里最早的一张自拍。",
    "原地表演「踩到香蕉皮滑倒」的全过程。",
    "接下来三轮,别人叫你名字你要回答「到!」并敬礼。",
    "现场编一句土味情话送给右边的人。",
)

private enum class UcPhase { Setup, Deal, Discuss }

/** 卧底局状态:提升到页面级,切 tab 不毁局 */
private class UcState {
    var phase by mutableStateOf(UcPhase.Setup)
    var playerCount by mutableIntStateOf(6)
    var spyCount by mutableIntStateOf(1)
    var withBlank by mutableStateOf(false)
    var words by mutableStateOf(UNDERCOVER_WORDS.first())
    var roles by mutableStateOf(listOf<String>())
    var current by mutableIntStateOf(0)
    var revealed by mutableStateOf(false)
    var showAnswer by mutableStateOf(false)

    fun deal() {
        val pair = UNDERCOVER_WORDS[Random.nextInt(UNDERCOVER_WORDS.size)]
        val (civilian, spy) = if (Random.nextBoolean()) pair else pair.second to pair.first
        val list = MutableList(playerCount) { civilian }
        val idx = (0 until playerCount).shuffled().take(spyCount + if (withBlank) 1 else 0)
        idx.take(spyCount).forEach { list[it] = spy }
        if (withBlank) list[idx.last()] = ""
        words = civilian to spy
        roles = list
        current = 0
        revealed = false
        showAnswer = false
        phase = UcPhase.Deal
    }
}

@Composable
fun PartyGamesToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var tab by rememberSaveable { mutableStateOf(0) }
    val uc = remember { UcState() }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("谁是卧底", "真心话", "大冒险"), tab, { tab = it }, Modifier.fillMaxWidth())
                }
            }
        }
        when (tab) {
            0 -> undercoverSection(uc, palette)
            1 -> {
                item {
                    var q by remember { mutableStateOf<String?>(null) }
                    GroupedCard {
                        CardPadding {
                            Text(
                                q ?: "点下面抽一题,答不上来就罚",
                                style = if (q == null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                                fontWeight = if (q == null) FontWeight.Normal else FontWeight.SemiBold,
                                color = if (q == null) palette.tertiaryLabel else palette.label
                            )
                            Spacer(Modifier.height(14.dp))
                            SolidButton(onClick = { q = TRUTH_QUESTIONS.random() }, Modifier.fillMaxWidth()) {
                                Text(if (q == null) "抽真心话" else "下一题")
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    var d by remember { mutableStateOf<String?>(null) }
                    GroupedCard {
                        CardPadding {
                            Text(
                                d ?: "点下面抽一个任务,做不到就再抽两个",
                                style = if (d == null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                                fontWeight = if (d == null) FontWeight.Normal else FontWeight.SemiBold,
                                color = if (d == null) palette.tertiaryLabel else palette.label
                            )
                            Spacer(Modifier.height(14.dp))
                            SolidButton(onClick = { d = DARE_TASKS.random() }, Modifier.fillMaxWidth()) {
                                Text(if (d == null) "抽大冒险" else "下一个")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 谁是卧底:人数设置 → 轮流看词 → 讨论投票(身份词全程本机保密) */
private fun androidx.compose.foundation.lazy.LazyListScope.undercoverSection(
    uc: UcState,
    palette: com.toolbox.nativetoolbox.ui.theme.IosPalette,
) {
    item {
        GroupedCard {
            CardPadding {
                when (uc.phase) {
                    UcPhase.Setup -> {
                        Text("人数与配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("玩家", "${uc.playerCount} 人", Modifier.weight(1f))
                            StatCell("卧底", "${uc.spyCount} 人", Modifier.weight(1f))
                            StatCell("白板", if (uc.withBlank) "1 人" else "无", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("玩家人数", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(listOf("4", "5", "6", "7", "8", "10", "12"), listOf(4, 5, 6, 7, 8, 10, 12).indexOf(uc.playerCount).coerceAtLeast(0), {
                            uc.playerCount = listOf(4, 5, 6, 7, 8, 10, 12)[it]
                            uc.spyCount = uc.spyCount.coerceAtMost((uc.playerCount - 2) / 2)
                        }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Text("卧底人数", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        val maxSpy = ((uc.playerCount - 2) / 2).coerceAtLeast(1)
                        SegmentedPicker((1..maxSpy.coerceAtMost(4)).map { "$it" }, (uc.spyCount - 1).coerceIn(0, maxSpy - 1), { uc.spyCount = it + 1 }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("加一个白板玩家(没有词,全靠演)", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.weight(1f))
                            androidx.compose.material3.Switch(checked = uc.withBlank, onCheckedChange = { uc.withBlank = it })
                        }
                        Spacer(Modifier.height(12.dp))
                        SolidButton(onClick = { uc.deal() }, Modifier.fillMaxWidth()) { Text("发牌") }
                    }
                    UcPhase.Deal -> {
                        Text("${uc.current + 1} 号玩家", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (uc.revealed) palette.sunkenBackground else palette.accent.copy(alpha = 0.12f))
                                .clickable { uc.revealed = !uc.revealed },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uc.revealed) {
                                Text(
                                    uc.roles[uc.current].ifEmpty { "你是白板\n(没有词,凭描述混过去)" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.label,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text("确认只有你在看\n点一下看词", style = MaterialTheme.typography.bodyLarge, color = palette.accent, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SolidButton(
                            onClick = {
                                if (uc.revealed) {
                                    if (uc.current + 1 >= uc.roles.size) uc.phase = UcPhase.Discuss
                                    else { uc.current += 1; uc.revealed = false }
                                }
                            },
                            Modifier.fillMaxWidth(),
                            enabled = uc.revealed
                        ) { Text(if (uc.current + 1 >= uc.roles.size) "看完,开始讨论" else "记住了,传给下一位") }
                    }
                    UcPhase.Discuss -> {
                        Text("讨论开始", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "按座位顺序每人一句话描述自己的词,不能直接说出词本身。每轮结束投票,票最多的出局。揪出全部卧底平民赢;卧底活到最后卧底赢。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryLabel
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("玩家", "${uc.roles.size} 人", Modifier.weight(1f))
                            StatCell("卧底", "${uc.roles.count { it == uc.words.second }} 人", Modifier.weight(1f))
                            StatCell("白板", "${uc.roles.count { it.isEmpty() }} 人", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        if (uc.showAnswer) {
                            Text("平民词:${uc.words.first} / 卧底词:${uc.words.second}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = palette.red)
                            Spacer(Modifier.height(12.dp))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SolidButton(onClick = { uc.showAnswer = !uc.showAnswer }, Modifier.weight(1f), filled = false) {
                                Text(if (uc.showAnswer) "藏答案" else "亮答案")
                            }
                            SolidButton(onClick = { uc.phase = UcPhase.Setup }, Modifier.weight(1f)) { Text("再来一局") }
                        }
                    }
                }
            }
        }
    }
}
