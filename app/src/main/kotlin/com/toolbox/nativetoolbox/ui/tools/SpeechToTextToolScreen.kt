package com.toolbox.nativetoolbox.ui.tools

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PermissionGate

@Composable
private fun SpeechContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    var listening by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    var finalText by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val recognizer = remember { if (available) SpeechRecognizer.createSpeechRecognizer(context) else null }

    DisposableEffect(Unit) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status = "在听,说吧" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status = "识别中…" }

            override fun onError(error: Int) {
                listening = false
                status = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没听清,再说一次"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "没有录音权限"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务忙,稍等再试"
                    else -> "识别出错(代码 $error),这台手机的语音服务可能不完整"
                }
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    finalText = if (finalText.isBlank()) text else finalText + "\n" + text
                    status = ""
                } else status = "没识别出内容"
                partial = ""
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }

    fun start() {
        if (recognizer == null) return
        partial = ""
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        runCatching { recognizer.startListening(intent); listening = true }
            .onFailure { status = "启动失败:${it.message}" }
    }

    GroupedCard {
        CardPadding {
            if (!available) {
                Text("这台手机没有可用的语音识别服务", style = MaterialTheme.typography.titleMedium, color = palette.red)
                Spacer(Modifier.height(6.dp))
                Text(
                    "语音转文字依赖系统的语音引擎(Google/各厂商语音服务)。可以装一个语音输入法后再来试。",
                    style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel
                )
            } else {
                Text(
                    if (listening) (partial.ifBlank { "……" }) else "按住思路,点一下开始说话",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (listening) palette.label else palette.tertiaryLabel
                )
                Spacer(Modifier.height(12.dp))
                SolidButton(
                    onClick = { if (listening) { recognizer?.stopListening() } else start() },
                    Modifier.fillMaxWidth()
                ) { Text(if (listening) "说完了" else "开始说话") }
                if (status.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.orange)
                }
            }
        }
    }
    if (finalText.isNotBlank()) {
        Spacer(Modifier.height(20.dp))
        OutputCard(finalText, Modifier, label = "速记内容(自动累加)")
        Spacer(Modifier.height(8.dp))
        SolidButton(onClick = { finalText = "" }, Modifier.fillMaxWidth(), filled = false) { Text("清空") }
    }
}

@Composable
fun SpeechToTextToolScreen(onBack: () -> Unit) {
    ToolScaffold {
        item {
            PermissionGate(android.Manifest.permission.RECORD_AUDIO, "语音转文字需要麦克风") {
                SpeechContent()
            }
        }
    }
}
