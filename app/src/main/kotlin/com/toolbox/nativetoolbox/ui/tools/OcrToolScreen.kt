package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.toolbox.nativetoolbox.util.rememberPermission

private suspend fun recognize(bmp: Bitmap): String? {
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    val text = suspendCancellableCoroutine<String?> { cont ->
        recognizer.process(InputImage.fromBitmap(bmp, 0))
            .addOnSuccessListener { cont.resume(it.text) }
            .addOnFailureListener { cont.resume(null) }
    }
    recognizer.close()
    return text
}

@Composable
fun OcrToolScreen(onBack: () -> Unit) {
    val cameraOk = rememberPermission(android.Manifest.permission.CAMERA)
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    fun runOcr(bmp: Bitmap) {
        busy = true; status = ""; result = ""
        scope.launch {
            val text = recognize(bmp)
            result = text?.trim() ?: ""
            status = when {
                text == null -> "识别出错了,重试一次"
                result.isEmpty() -> "没认出文字。光线好、字体清晰的图效果最好"
                else -> ""
            }
            busy = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bmp = ImageUtil.loadBitmap(context, uri, 2200)
        source = bmp
        if (bmp != null) runOcr(bmp) else status = "图读不出来"
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            source = bmp
            runOcr(bmp)
        }
    }

    ToolScaffold {
        item {
            if (result.isNotEmpty()) OutputCard(result, Modifier, label = "识别出的文字")
        }
        item {
            GroupedCard {
                CardPadding {
                    val s = source
                    if (s == null) {
                        Text("拍照或选图,一键提取里面的文字", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(4.dp))
                        Text("中英文混排都认,全程本机识别不上传", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Image(s.asImageBitmap(), contentDescription = "原图", modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (busy) {
                        Text("识别中…", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        SolidButton(onClick = {
                            runCatching { camera.launch(null) }
                                .onFailure { status = "相机打不开,先给 Astro Kit 相机权限,或直接选图" }
                        }, Modifier.weight(1f), enabled = !busy) { Text("拍照识别") }
                        Spacer(Modifier.width(8.dp))
                        SolidButton(onClick = { picker.launch("image/*") }, Modifier.weight(1f), filled = false, enabled = !busy) { Text("选图识别") }
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.orange)
                    }
                }
            }
        }
    }
}
