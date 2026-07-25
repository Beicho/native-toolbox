package com.toolbox.nativetoolbox.ui.tools

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MODE_STEADY = 0
private const val MODE_SOS = 1
private const val MODE_STROBE = 2

/** SOS 摩斯码：三短三长三短，单位是「点」的时长 */
private val sosPattern = listOf(
    1, 1, 1, 1, 1, 3,
    3, 1, 3, 1, 3, 3,
    1, 1, 1, 1, 1, 7
)

private fun torchCameraId(manager: CameraManager): String? = runCatching {
    manager.cameraIdList.firstOrNull { id ->
        manager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }
}.getOrNull()

@Composable
fun FlashlightToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }

    val manager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    val cameraId = remember { manager?.let { torchCameraId(it) } }
    val available = manager != null && cameraId != null

    var on by rememberSaveable { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(MODE_STEADY) }
    var strobeHzText by rememberSaveable { mutableStateOf("5") }
    var error by rememberSaveable { mutableStateOf("") }

    fun setTorch(enabled: Boolean) {
        if (!available) return
        runCatching { manager!!.setTorchMode(cameraId!!, enabled) }
            .onFailure { error = "闪光灯被其他应用占用了，关掉相机再试" }
    }

    val strobeHz = (strobeHzText.trim().toDoubleOrNull() ?: 5.0).coerceIn(0.5, 20.0)

    DisposableEffect(on, mode, strobeHz) {
        if (!on || !available) {
            setTorch(false)
            onDispose { setTorch(false) }
        } else {
            val job = scope.launch {
                when (mode) {
                    MODE_STEADY -> setTorch(true)
                    MODE_SOS -> {
                        val unit = 220L
                        while (isActive && on) {
                            sosPattern.forEachIndexed { index, units ->
                                if (!isActive || !on) return@forEachIndexed
                                setTorch(index % 2 == 0)
                                delay(units * unit)
                            }
                            setTorch(false)
                            delay(600)
                        }
                    }
                    else -> {
                        val halfPeriod = (500.0 / strobeHz).toLong()
                        var lit = false
                        while (isActive && on) {
                            lit = !lit
                            setTorch(lit)
                            delay(halfPeriod)
                        }
                    }
                }
            }
            onDispose {
                job.cancel()
                setTorch(false)
            }
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(if (on) palette.yellow.copy(alpha = 0.9f) else palette.sunkenBackground)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                enabled = available
                            ) {
                                error = ""
                                on = !on
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (on) "关" else "开",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Light,
                            color = if (on) palette.label else palette.secondaryLabel
                        )
                    }
                    Text(
                        if (!available) "这台设备没有可用的闪光灯"
                        else if (on) when (mode) {
                            MODE_STEADY -> "常亮中"
                            MODE_SOS -> "正在发 SOS 求救信号"
                            else -> "爆闪中，每秒 " + strobeHzText.trim() + " 次"
                        } else "点圆圈开灯",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (available) palette.secondaryLabel else palette.red
                    )
                    if (error.isNotBlank()) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                }
            }
        }
        item { SectionHeader("模式") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("常亮", "SOS", "爆闪"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    if (mode == MODE_STROBE) {
                        IosTextField(
                            value = strobeHzText,
                            onValueChange = { strobeHzText = it },
                            placeholder = "每秒闪几次（0.5 到 20）",
                            mono = true
                        )
                    }
                    Text(
                        when (mode) {
                            MODE_STEADY -> "普通照明。长时间开会让机身发热。"
                            MODE_SOS -> "国际通用求救信号，三短三长三短循环。野外遇险时向远处示意用。"
                            else -> "高频闪烁。注意：强闪光可能诱发光敏性癫痫，别对着人眼照。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mode == MODE_STROBE) palette.orange else palette.secondaryLabel
                    )
                }
            }
        }
    }
}
