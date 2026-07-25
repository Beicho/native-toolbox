package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private fun knock(context: android.content.Context) {
    runCatching {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as android.os.VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(28, 120))
    }
}

@Composable
fun WoodenFishToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }

    var count by rememberSaveable { mutableStateOf(0) }
    var hapticOn by rememberSaveable { mutableStateOf(true) }
    var autoOn by rememberSaveable { mutableStateOf(false) }
    var intervalText by rememberSaveable { mutableStateOf("1.0") }
    var goalText by rememberSaveable { mutableStateOf("108") }
    var pulse by remember { mutableIntStateOf(0) }

    val interval = (intervalText.trim().toDoubleOrNull() ?: 1.0).coerceIn(0.2, 10.0)
    val goal = goalText.trim().toIntOrNull()?.coerceAtLeast(1) ?: 108

    // pulse 每次自增触发一次缩放动画
    val scaleTarget = remember(pulse) { if (pulse % 2 == 0) 1f else 0.92f }
    val animatedScale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(90),
        label = "knock"
    )

    fun hit() {
        count += 1
        pulse += 1
        if (hapticOn) knock(context)
    }

    DisposableEffect(autoOn, interval) {
        if (!autoOn) {
            onDispose { }
        } else {
            val job = scope.launch {
                while (isActive && autoOn) {
                    delay((interval * 1000).toLong())
                    hit()
                }
            }
            onDispose { job.cancel() }
        }
    }

    val progress = (count.toFloat() / goal).coerceIn(0f, 1f)

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        Modifier
                            .size(180.dp)
                            .scale(animatedScale)
                            .clip(CircleShape)
                            .background(palette.orange.copy(alpha = 0.16f))
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) { hit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("敲", fontSize = 56.sp, fontWeight = FontWeight.Light, color = palette.orange)
                    }
                    Text(
                        count.toString(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                        color = palette.label
                    )
                    Text(
                        if (count >= goal) "已完成 " + goal + " 次" else "目标 " + goal + " 次，还差 " + (goal - count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (count >= goal) palette.green else palette.secondaryLabel
                    )
                    Text(
                        "点圆圈敲一下",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("设置") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = goalText,
                            onValueChange = { goalText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "目标次数",
                            mono = true
                        )
                        IosTextField(
                            value = intervalText,
                            onValueChange = { intervalText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "自动间隔（秒）",
                            mono = true
                        )
                    }
                    SolidButton(onClick = { count = 0 }, filled = false) { Text("计数归零") }
                }
                ToggleRow("敲击震动", hapticOn, onCheckedChange = { hapticOn = it })
                ToggleRow(
                    "自动敲",
                    autoOn,
                    onCheckedChange = { autoOn = it },
                    subtitle = "按设定间隔自己敲，放着当白噪音"
                )
            }
        }
        item { SectionHeader("进度") }
        item {
            GroupedCard {
                KeyValueRow("已敲", count.toString() + " 次", copyable = false)
                RowDivider()
                KeyValueRow("完成度", String.format("%.0f%%", progress * 100), copyable = false)
                RowDivider()
                KeyValueRow(
                    "自动模式",
                    if (autoOn) "开，每 " + intervalText.trim() + " 秒一次" else "关",
                    copyable = false
                )
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "纯粹图个安静，没有声音只有震动，随时可以关掉震动。完全离线，不记录任何数据。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
