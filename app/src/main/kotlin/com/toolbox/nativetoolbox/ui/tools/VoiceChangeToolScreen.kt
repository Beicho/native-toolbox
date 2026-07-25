package com.toolbox.nativetoolbox.ui.tools

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.AudioKit
import com.toolbox.nativetoolbox.util.PcmPlayer
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VoiceChangeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<AudioKit.Pcm?>(null) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var reversed by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    val player = remember { PcmPlayer() }

    DisposableEffect(Unit) { onDispose { player.stop() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; status = "解码中…"
        scope.launch {
            source = withContext(Dispatchers.IO) { AudioKit.decode(context, uri, maxDurationMs = 5 * 60_000) }
            status = if (source == null) "解码失败,这个文件可能不是音频" else "已载入 ${source!!.durationMs / 1000} 秒音频"
            busy = false
        }
    }

    fun processed(): AudioKit.Pcm? {
        val s = source ?: return null
        var p = AudioKit.resampleSpeed(s, speed)
        if (reversed) p = AudioKit.reverse(p)
        return p
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        when {
                            source == null -> "选一段音频,变速变调、倒着放"
                            else -> status.ifBlank { "已载入" }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (source == null) palette.tertiaryLabel else palette.label
                    )
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { picker.launch("audio/*") }, Modifier.fillMaxWidth(), filled = source == null, enabled = !busy) {
                        Text(if (busy) "解码中…" else if (source == null) "选音频" else "换一个")
                    }
                }
            }
        }
        item {
            if (source != null) {
                GroupedCard {
                    CardPadding {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("速度", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.width(44.dp))
                            Slider(speed, { speed = it }, valueRange = 0.5f..2f, modifier = Modifier.weight(1f))
                            Text("%.1fx".format(speed), style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        }
                        Text("快放变尖(花栗鼠),慢放变粗(大叔),像磁带一样", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                    }
                    ToggleRow("倒放", reversed, onCheckedChange = { reversed = it })
                    CardPadding {
                        Row(Modifier.fillMaxWidth()) {
                            SolidButton(
                                onClick = {
                                    if (playing) { player.stop(); playing = false }
                                    else {
                                        val p = processed() ?: return@SolidButton
                                        playing = true
                                        player.play(p) { playing = false }
                                    }
                                },
                                Modifier.weight(1f)
                            ) { Text(if (playing) "停止" else "试听") }
                            Spacer(Modifier.width(8.dp))
                            SolidButton(
                                onClick = {
                                    busy = true; status = "导出中…"
                                    scope.launch {
                                        val r = withContext(Dispatchers.Default) {
                                            val p = processed() ?: return@withContext null
                                            val tmp = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                                            if (!AudioKit.encodeM4a(p, tmp)) return@withContext null
                                            val bytes = tmp.readBytes()
                                            tmp.delete()
                                            FileHelper.saveToDownloads(context, "变声_${System.currentTimeMillis()}.m4a", bytes).getOrNull()
                                        }
                                        status = if (r != null) "已存到 $r" else "导出失败"
                                        busy = false
                                    }
                                },
                                Modifier.weight(1f), filled = false, enabled = !busy
                            ) { Text("导出 m4a") }
                        }
                        if (status.isNotEmpty() && source != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("失败")) palette.red else palette.green)
                        }
                    }
                }
            }
        }
    }
}
