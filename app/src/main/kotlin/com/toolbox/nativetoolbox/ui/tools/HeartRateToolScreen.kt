package com.toolbox.nativetoolbox.ui.tools

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PermissionGate

/**
 * 指尖心率:手指盖住后摄+闪光灯,血液脉动引起画面亮度周期变化(PPG 原理),
 * 峰值间隔换算心率。测 20 秒,取中位间隔。
 */
private class PpgProcessor {
    val samples = ArrayList<Pair<Long, Double>>(600) // (时间ms, 亮度)
    var fingerDetected = false

    fun add(timeMs: Long, luma: Double, redish: Boolean) {
        fingerDetected = redish
        if (!redish) { samples.clear(); return }
        samples.add(timeMs to luma)
        // 只留最近 25 秒
        while (samples.isNotEmpty() && timeMs - samples.first().first > 25_000) samples.removeAt(0)
    }

    /** 去趋势 + 找峰,返回 bpm(样本不足返回 null) */
    fun bpm(): Int? {
        if (samples.size < 90) return null
        val span = samples.last().first - samples.first().first
        if (span < 8000) return null
        // 移动均值去趋势
        val vals = samples.map { it.second }
        val win = 15
        val detrended = vals.mapIndexed { i, v ->
            val lo = (i - win).coerceAtLeast(0)
            val hi = (i + win).coerceAtMost(vals.lastIndex)
            v - vals.subList(lo, hi + 1).average()
        }
        // 峰值:比两侧都高且超过阈值
        val std = kotlin.math.sqrt(detrended.sumOf { it * it } / detrended.size)
        val thresh = std * 0.55
        val peaks = ArrayList<Long>()
        for (i in 2 until detrended.size - 2) {
            if (detrended[i] > thresh &&
                detrended[i] >= detrended[i - 1] && detrended[i] >= detrended[i - 2] &&
                detrended[i] > detrended[i + 1] && detrended[i] > detrended[i + 2]
            ) {
                val t = samples[i].first
                if (peaks.isEmpty() || t - peaks.last() > 330) peaks.add(t) // 最高 180bpm
            }
        }
        if (peaks.size < 6) return null
        val gaps = peaks.zipWithNext { a, b -> b - a }.sorted()
        val median = gaps[gaps.size / 2].toDouble()
        val bpm = (60_000 / median).toInt()
        return if (bpm in 35..200) bpm else null
    }
}

@Composable
private fun HeartRateContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var measuring by remember { mutableStateOf(false) }
    var bpm by remember { mutableStateOf<Int?>(null) }
    var finalBpm by remember { mutableStateOf<Int?>(null) }
    var fingerOn by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var wave by remember { mutableStateOf<List<Double>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    val processor = remember { PpgProcessor() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    fun stop() {
        runCatching { provider?.unbindAll() }
        measuring = false
    }

    fun start() {
        error = ""; finalBpm = null; bpm = null; progress = 0
        processor.samples.clear()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val p = runCatching { future.get() }.getOrNull()
            if (p == null) { error = "相机启动失败"; return@addListener }
            provider = p
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            val startAt = System.currentTimeMillis()
            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { img ->
                val now = System.currentTimeMillis()
                // Y 平面亮度均值(采样跳步,省电)
                val buf = img.planes[0].buffer
                var sum = 0L; var n = 0
                var i = 0
                val step = 97
                while (i < buf.capacity()) {
                    sum += (buf.get(i).toInt() and 0xFF); n++; i += step
                }
                val luma = sum.toDouble() / n
                // 手指盖住:画面暗(闪光灯下仍偏亮红,Y 中等)且方差小 → 简化判据:亮度在 20..250 且 U/V 偏红
                var redish = false
                if (img.planes.size >= 3) {
                    val vBuf = img.planes[2].buffer
                    var vs = 0L; var vn = 0; var j = 0
                    while (j < vBuf.capacity()) { vs += (vBuf.get(j).toInt() and 0xFF); vn++; j += step }
                    val vMean = vs.toDouble() / vn
                    redish = vMean > 140  // V 分量偏红
                }
                processor.add(now, luma, redish)
                fingerOn = processor.fingerDetected
                if (fingerOn) {
                    progress = (((now - startAt) / 200L).toInt()).coerceAtMost(100)
                    bpm = processor.bpm()
                    wave = processor.samples.takeLast(150).map { it.second }
                    if (now - startAt >= 20_000 && bpm != null) {
                        finalBpm = bpm
                        stop()
                    }
                } else {
                    progress = 0
                    wave = emptyList()
                }
                img.close()
            }
            runCatching {
                p.unbindAll()
                val camera = p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
                camera.cameraControl.enableTorch(true)
                measuring = true
            }.onFailure { error = "相机被占用或不可用:${it.message}" }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) { onDispose { stop() } }

    GroupedCard {
        CardPadding {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                val display = finalBpm ?: bpm
                Text(
                    display?.toString() ?: "--",
                    fontSize = 76.sp, fontWeight = FontWeight.Bold,
                    color = if (finalBpm != null) palette.red else palette.label
                )
                Text("次/分", style = MaterialTheme.typography.titleMedium, color = palette.secondaryLabel)
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        finalBpm != null -> when {
                            finalBpm!! < 60 -> "偏慢。运动员常见,普通人常这样可以留意"
                            finalBpm!! <= 100 -> "在正常范围(60~100)"
                            else -> "偏快。刚运动完/紧张很正常,静息状态持续偏快要留意"
                        }
                        !measuring -> "用指腹完全盖住后置摄像头和闪光灯"
                        !fingerOn -> "没检测到手指,盖严一点(会有点温热,正常)"
                        else -> "测量中 $progress% · 保持别动"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (measuring && fingerOn) palette.green else palette.secondaryLabel
                )
            }
            if (wave.size > 10) {
                Spacer(Modifier.height(10.dp))
                val accent = palette.red
                Canvas(Modifier.fillMaxWidth().height(64.dp)) {
                    val minV = wave.min(); val maxV = wave.max()
                    val range = (maxV - minV).coerceAtLeast(0.5)
                    val path = Path()
                    wave.forEachIndexed { i, v ->
                        val x = size.width * i / (wave.size - 1f)
                        val y = (size.height * (1 - (v - minV) / range)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, accent, style = Stroke(3f))
                }
            }
            Spacer(Modifier.height(12.dp))
            SolidButton(onClick = { if (measuring) stop() else start() }, Modifier.fillMaxWidth()) {
                Text(if (measuring) "停止" else if (finalBpm != null) "再测一次" else "开始测量")
            }
            if (error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(error, style = MaterialTheme.typography.bodyMedium, color = palette.red)
            }
            Spacer(Modifier.height(6.dp))
            Text("测着玩可以,看病请用医疗设备", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
        }
    }
}

@Composable
fun HeartRateToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item {
            PermissionGate(android.Manifest.permission.CAMERA, "测心率要用摄像头对着指尖采光,画面不保存不上传") {
                HeartRateContent()
            }
        }
    }
}
