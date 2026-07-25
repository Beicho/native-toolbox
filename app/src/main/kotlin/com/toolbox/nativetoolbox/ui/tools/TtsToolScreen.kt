package com.toolbox.nativetoolbox.ui.tools

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.util.Locale

@Composable
fun TtsToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var supported by remember { mutableStateOf(true) }
    var speaking by remember { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableFloatStateOf(1f) }
    var pitch by rememberSaveable { mutableFloatStateOf(1f) }

    var ttsRef by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.CHINESE)
                supported = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                ready = true
                ttsRef = tts
            } else supported = false
        }
        onDispose { tts?.stop(); tts?.shutdown() }
    }

    fun speak() {
        val tts = ttsRef ?: return
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "astro_tts")
        speaking = true
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(text, { text = it }, Modifier.fillMaxWidth(), placeholder = "输入要朗读的文字…", minHeight = 140.dp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("语速", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.width(40.dp))
                        Slider(rate, { rate = it }, valueRange = 0.5f..2f, modifier = Modifier.weight(1f))
                        Text("%.1fx".format(rate), style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("音调", style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.width(40.dp))
                        Slider(pitch, { pitch = it }, valueRange = 0.5f..2f, modifier = Modifier.weight(1f))
                        Text("%.1f".format(pitch), style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        SolidButton(
                            onClick = { speak() },
                            Modifier.weight(2f),
                            enabled = ready && supported && text.isNotBlank()
                        ) { Text(if (!ready) "语音引擎启动中…" else "朗读") }
                        Spacer(Modifier.width(8.dp))
                        SolidButton(onClick = { ttsRef?.stop(); speaking = false }, Modifier.weight(1f), filled = false, enabled = ready) { Text("停") }
                    }
                    if (!supported) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "这台手机的语音引擎不支持中文。到系统设置 → 无障碍 → 文字转语音里换个引擎(或装个讯飞/Google TTS)。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.orange
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("适合读长文、给孩子读故事、校对稿子听感", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
    }
}
