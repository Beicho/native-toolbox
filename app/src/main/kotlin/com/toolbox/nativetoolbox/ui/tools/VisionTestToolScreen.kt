package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.random.Random

/**
 * 视力自测(E 字方向,对数表 4.0~5.2 换算)+ 辨色力挑战(找不同色块,难度递增)。
 * 只是居家自测,配镜请去医院。
 */

// E 字每行:对数视力值 与 E 高度 mm(标准 5 分表 @ 40cm 换算近用尺寸)
private val E_LEVELS = listOf(
    4.0 to 29.0, 4.2 to 18.4, 4.4 to 11.6, 4.6 to 7.3, 4.8 to 4.6,
    5.0 to 2.9, 5.1 to 2.3, 5.2 to 1.8,
)

@Composable
private fun EyeChart(palette: com.toolbox.nativetoolbox.ui.theme.IosPalette) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dm = context.resources.displayMetrics
    val pxPerMm = dm.ydpi / 25.4f

    var level by remember { mutableIntStateOf(0) }
    var dir by remember { mutableIntStateOf(Random.nextInt(4)) } // 0右 1下 2左 3上
    var wrong by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Double?>(null) }

    fun next(correct: Boolean) {
        if (correct) {
            if (level >= E_LEVELS.lastIndex) { result = E_LEVELS.last().first; return }
            level++; wrong = 0
        } else {
            wrong++
            if (wrong >= 2) { result = if (level == 0) null else E_LEVELS[level - 1].first; return }
        }
        dir = Random.nextInt(4)
    }

    GroupedCard {
        CardPadding {
            val r = result
            if (r != null || (result == null && wrong >= 2 && level == 0)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (r == null) "低于 4.0" else "约 ${"%.1f".format(r)}",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = palette.label
                    )
                    Text(
                        when {
                            r == null -> "这个距离看不清最大的 E,建议去查一下"
                            r >= 5.0 -> "相当不错,保持"
                            r >= 4.8 -> "接近正常,注意用眼休息"
                            else -> "偏低,长期这样建议去医院验光"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { level = 0; wrong = 0; result = null; dir = Random.nextInt(4) }, Modifier.fillMaxWidth()) { Text("再测一次") }
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("手机放在离眼睛 40 厘米处(约一臂弯)", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                    Spacer(Modifier.height(4.dp))
                    Text("第 ${level + 1}/${E_LEVELS.size} 行 · 视力 ${E_LEVELS[level].first}", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                    Spacer(Modifier.height(18.dp))
                    val hMm = E_LEVELS[level].second
                    val sizeDp = (hMm * pxPerMm / dm.density).dp
                    Box(Modifier.height(90.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "E",
                            fontSize = (hMm * pxPerMm / dm.density / 0.7f).sp, // 字形高约 0.7em,放大补偿
                            fontWeight = FontWeight.Black,
                            color = palette.label,
                            modifier = Modifier.rotate(dir * 90f)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("E 的开口朝哪边?", style = MaterialTheme.typography.bodyMedium, color = palette.label)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("右" to 0, "下" to 1, "左" to 2, "上" to 3).forEach { (name, d) ->
                            SolidButton(onClick = { next(d == dir) }, Modifier.weight(1f), filled = false) { Text(name) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SolidButton(onClick = { next(false) }, Modifier.fillMaxWidth(), filled = false) { Text("看不清") }
                }
            }
        }
    }
}

@Composable
private fun ColorTest(palette: com.toolbox.nativetoolbox.ui.theme.IosPalette) {
    var round by remember { mutableIntStateOf(1) }
    var best by remember { mutableIntStateOf(0) }
    var seed by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }

    val grid = (2 + (round - 1) / 3).coerceAtMost(6) // 2x2 → 3x3 → … 最大 6x6
    // 每轮的目标格与颜色都由 (round, seed) 派生,升关后自动匹配新格数
    val oddIndex = remember(round, seed) { Random.nextInt(grid * grid) }
    val delta = (0.20f / (1 + round * 0.35f)).coerceAtLeast(0.012f)
    val baseHue = remember(round, seed) { Random.nextFloat() * 360f }
    val lighter = remember(round, seed) { Random.nextBoolean() }
    val baseColor = Color.hsl(baseHue, 0.62f, 0.55f)
    val oddColor = Color.hsl(baseHue, 0.62f, (0.55f + if (lighter) delta else -delta).coerceIn(0.08f, 0.92f))

    GroupedCard {
        CardPadding {
            if (over) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("闯到第 $best 关", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.label)
                    Text(
                        when {
                            best >= 18 -> "辨色力惊人,设计师水平"
                            best >= 12 -> "辨色力很好"
                            best >= 7 -> "正常水平"
                            else -> "偏弱,光线暗或屏幕护眼模式也会影响,换个环境再试试"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel
                    )
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { round = 1; over = false; seed++ }, Modifier.fillMaxWidth()) { Text("重新挑战") }
                }
            } else {
                Text("第 $round 关 · 点出颜色不一样的那块", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (r in 0 until grid) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (c in 0 until grid) {
                                val idx = r * grid + c
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (idx == oddIndex) oddColor else baseColor)
                                        .clickable {
                                            if (idx == oddIndex) {
                                                best = round; round++; seed++
                                            } else over = true
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisionTestToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var tab by rememberSaveable { mutableStateOf(0) }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("视力自测", "辨色挑战"), tab, { tab = it }, Modifier.fillMaxWidth())
                }
            }
        }
        item { if (tab == 0) EyeChart(palette) else ColorTest(palette) }
        item {
            Text(
                "居家粗测,不能替代医院验光",
                Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = palette.tertiaryLabel,
                textAlign = TextAlign.Center
            )
        }
    }
}
