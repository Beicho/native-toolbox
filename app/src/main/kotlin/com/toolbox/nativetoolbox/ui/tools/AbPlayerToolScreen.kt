package com.toolbox.nativetoolbox.ui.tools

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private fun fmtMs(ms: Int): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

/**
 * AB 循环:学外语跟读、扒谱神器。设 A、设 B,选中段落无限循环,还能慢速。
 */
@Composable
fun AbPlayerToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var duration by remember { mutableIntStateOf(0) }
    var position by remember { mutableIntStateOf(0) }
    var pointA by remember { mutableIntStateOf(-1) }
    var pointB by remember { mutableIntStateOf(-1) }
    var speedIdx by rememberSaveable { mutableStateOf(2) } // 0.5/0.75/1.0/1.25
    var playing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f)

    DisposableEffect(Unit) { onDispose { runCatching { player?.release() } } }

    // 进度轮询 + AB 循环判断
    LaunchedEffect(playing, pointA, pointB) {
        while (playing) {
            val p = player ?: break
            position = runCatching { p.currentPosition }.getOrDefault(0)
            if (pointA >= 0 && pointB > pointA && position >= pointB) {
                runCatching { p.seekTo(pointA) }
            }
            kotlinx.coroutines.delay(120)
        }
    }

    fun applySpeed(p: MediaPlayer) {
        runCatching {
            val wasPlaying = p.isPlaying
            p.playbackParams = PlaybackParams().setSpeed(speeds[speedIdx])
            if (!wasPlaying) p.pause() // setPlaybackParams 会顺带 start
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { player?.release() }
        val p = MediaPlayer()
        runCatching {
            p.setDataSource(context, uri)
            p.prepare()
        }.onFailure { status = "打不开这个音频"; p.release(); return@rememberLauncherForActivityResult }
        player = p
        duration = p.duration
        position = 0; pointA = -1; pointB = -1
        playing = false
        status = ""
        p.setOnCompletionListener {
            if (pointA >= 0) { runCatching { p.seekTo(pointA); p.start() } } else playing = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (player == null) {
                        Text("学外语、练听力、扒谱:选中一段反复听,还能放慢", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${fmtMs(position)} / ${fmtMs(duration)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.label
                            )
                            if (pointA >= 0 || pointB >= 0) {
                                Text(
                                    "A ${if (pointA >= 0) fmtMs(pointA) else "--"} → B ${if (pointB >= 0) fmtMs(pointB) else "--"}" +
                                        if (pointA >= 0 && pointB > pointA) " 循环中" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.accent
                                )
                            }
                        }
                        Slider(
                            position.toFloat(),
                            { v -> position = v.toInt(); runCatching { player?.seekTo(v.toInt()) } },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    SolidButton(onClick = { picker.launch("audio/*") }, Modifier.fillMaxWidth(), filled = player == null) {
                        Text(if (player == null) "选音频" else "换一个")
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                }
            }
        }
        item {
            if (player != null) {
                GroupedCard {
                    CardPadding {
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(
                                onClick = {
                                    val p = player ?: return@SolidButton
                                    if (playing) { runCatching { p.pause() }; playing = false }
                                    else { runCatching { p.start(); applySpeed(p) }; playing = true }
                                },
                                Modifier.weight(2f)
                            ) { Text(if (playing) "暂停" else "播放") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(onClick = {
                                val p = player ?: return@SolidButton
                                runCatching { p.seekTo((p.currentPosition - 5000).coerceAtLeast(0)) }
                            }, Modifier.weight(1f), filled = false) { Text("-5s") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(
                                onClick = { pointA = position; if (pointB in 0..pointA) pointB = -1 },
                                Modifier.weight(1f), filled = pointA >= 0
                            ) { Text(if (pointA >= 0) "A ${fmtMs(pointA)}" else "设 A 点") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(
                                onClick = { if (position > pointA) pointB = position },
                                Modifier.weight(1f), filled = pointB >= 0, enabled = pointA >= 0
                            ) { Text(if (pointB >= 0) "B ${fmtMs(pointB)}" else "设 B 点") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(
                                onClick = { pointA = -1; pointB = -1 },
                                Modifier.weight(1f), filled = false, enabled = pointA >= 0 || pointB >= 0
                            ) { Text("清除") }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("速度", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(6.dp))
                        SegmentedPicker(listOf("0.5x", "0.75x", "1x", "1.25x"), speedIdx, {
                            speedIdx = it
                            player?.let { p -> if (playing) applySpeed(p) }
                        }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
