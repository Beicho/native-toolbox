package com.toolbox.nativetoolbox.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.data.predict.PredictEngine
import com.toolbox.nativetoolbox.ui.components.IconTile
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/**
 * 主页顶部「此刻」区块 —— 预测大脑的门面。
 *
 * 设计取舍:
 *  - **克制**:最多 3 个,一行放完,不占满首屏。喧宾夺主就成了推荐流。
 *  - **带理由**:每张卡下面一行小字说明为什么推荐它。用户看到「刚分享进来的图」
 *    才会觉得神,看到干巴巴的图标只会觉得随机。
 *  - **可纠正**:长按 → 不再推荐。信任建立在能反悔上。
 *  - **数据不够就不出现**:宁可没有,也不要出现得很蠢。
 */
@Composable
fun NowSection(
    suggestions: List<PredictEngine.Suggestion>,
    toolTitle: (String) -> String?,
    toolIcon: (String) -> androidx.compose.ui.graphics.vector.ImageVector?,
    toolTint: (String) -> androidx.compose.ui.graphics.Color?,
    onOpen: (String) -> Unit,
    onMute: (String) -> Unit,
) {
    if (suggestions.isEmpty()) return
    val palette = LocalIosPalette.current
    var muteTarget by remember { mutableStateOf<PredictEngine.Suggestion?>(null) }

    // 展示埋点:用于「连续展示没被点就降权」的自省机制
    LaunchedEffect(suggestions.map { it.route }.joinToString()) {
        PredictEngine.onSuggestionsShown(suggestions.map { it.route })
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically()
    ) {
        Column(Modifier.padding(top = 18.dp)) {
            Text(
                "此刻",
                Modifier.padding(start = 20.dp, bottom = 10.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.label
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                suggestions.forEach { s ->
                    val title = toolTitle(s.route) ?: return@forEach
                    val icon = toolIcon(s.route)
                    val tint = toolTint(s.route) ?: palette.accent
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.cardBackground)
                            .combinedClickable(
                                onClick = {
                                    PredictEngine.onSuggestionClicked(s.route)
                                    onOpen(s.route)
                                },
                                onLongClick = { muteTarget = s }
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (icon != null) IconTile(icon, tint, size = 34.dp)
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.label,
                            maxLines = 1
                        )
                        // 推荐理由 —— 「读心感」的一半靠这行字
                        Text(
                            s.reason,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = palette.tertiaryLabel,
                            maxLines = 2
                        )
                    }
                }
                // 不足 3 个时补空位,保持卡片宽度一致
                repeat(3 - suggestions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    muteTarget?.let { s ->
        val title = toolTitle(s.route) ?: ""
        AlertDialog(
            onDismissRequest = { muteTarget = null },
            containerColor = palette.cardBackground,
            title = { Text("不再推荐「$title」?", color = palette.label) },
            text = {
                Text(
                    "推荐它是因为:${s.reason}\n\n" +
                        "关掉后一个月内不会再出现在这里。你还是能在工具列表里找到它。",
                    color = palette.secondaryLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onMute(s.route)
                    muteTarget = null
                }) { Text("不再推荐", color = palette.red) }
            },
            dismissButton = {
                TextButton(onClick = { muteTarget = null }) {
                    Text("留着吧", color = palette.secondaryLabel)
                }
            }
        )
    }
}
