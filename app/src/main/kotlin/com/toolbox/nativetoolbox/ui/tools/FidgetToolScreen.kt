package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private fun pop(context: android.content.Context) {
    runCatching {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as android.os.VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(18, 90))
    }
}

@Composable
fun FidgetToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current

    var toy by rememberSaveable { mutableStateOf(0) }
    var haptic by rememberSaveable { mutableStateOf(true) }
    var popCount by remember { mutableIntStateOf(0) }

    // 泡泡纸：6×8 个泡泡，戳破后不回弹，可一键重置
    val bubbles = remember { MutableList(48) { false }.toMutableStateList() }
    // 按压球：按下缩小
    var squeezing by remember { mutableStateOf(false) }
    val squeezeScale by animateFloatAsState(
        targetValue = if (squeezing) 0.82f else 1f,
        animationSpec = tween(120),
        label = "squeeze"
    )
    // 开关阵列
    val switches = remember { MutableList(8) { false }.toMutableStateList() }

    fun feedback() {
        popCount += 1
        if (haptic) pop(context)
    }

    ToolScaffold {
        item { SectionHeader("玩什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("泡泡纸", "按压球", "开关"),
                        selectedIndex = toy,
                        onSelected = { toy = it }
                    )
                }
                ToggleRow("触觉反馈", haptic, onCheckedChange = { haptic = it })
            }
        }
        item { SectionHeader(listOf("泡泡纸", "按压球", "开关")[toy]) }
        item {
            GroupedCard {
                CardPadding {
                    when (toy) {
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                (0 until 8).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        (0 until 6).forEach { col ->
                                            val index = row * 6 + col
                                            val popped = bubbles[index]
                                            Box(
                                                Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (popped) palette.sunkenBackground
                                                        else palette.accent.copy(alpha = 0.28f)
                                                    )
                                                    .clickable(enabled = !popped) {
                                                        bubbles[index] = true
                                                        feedback()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                            val remaining = bubbles.count { !it }
                            Text(
                                if (remaining == 0) "全戳完了" else "还剩 " + remaining + " 个",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (remaining == 0) palette.green else palette.secondaryLabel
                            )
                            SolidButton(
                                onClick = { for (i in bubbles.indices) bubbles[i] = false },
                                filled = false
                            ) { Text("重新铺一张") }
                        }
                        1 -> {
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(200.dp)
                                        .scale(squeezeScale)
                                        .clip(CircleShape)
                                        .background(palette.pink.copy(alpha = 0.35f))
                                        .clickable {
                                            squeezing = !squeezing
                                            feedback()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (squeezing) "松开" else "按",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Light,
                                        color = palette.label
                                    )
                                }
                            }
                            Text(
                                "点一下捏紧，再点一下松开。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.tertiaryLabel
                            )
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                switches.indices.chunked(4).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { index ->
                                            val on = switches[index]
                                            Box(
                                                Modifier
                                                    .weight(1f)
                                                    .aspectRatio(0.8f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (on) palette.green.copy(alpha = 0.55f)
                                                        else palette.sunkenBackground
                                                    )
                                                    .clickable {
                                                        switches[index] = !on
                                                        feedback()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    if (on) "开" else "关",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = palette.label
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            SolidButton(
                                onClick = { for (i in switches.indices) switches[i] = false },
                                filled = false
                            ) { Text("全部关掉") }
                        }
                    }
                }
            }
        }
        item { SectionHeader("统计") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell("本次操作", popCount.toString(), Modifier.weight(1f))
                        StatCell("触觉", if (haptic) "开" else "关", Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
