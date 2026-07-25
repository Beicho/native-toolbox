package com.toolbox.nativetoolbox.ui.tools

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PermissionGate

@Composable
fun MirrorToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val view = LocalView.current

    var keepAwake by rememberSaveable { mutableStateOf(true) }
    var useFront by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    ToolScaffold {
        item { SectionHeader("镜子") }
        item {
            GroupedCard {
                CardPadding {
                    PermissionGate(Manifest.permission.CAMERA, "照镜子需要用到摄像头画面") {
                        CameraMirrorPreview(useFront)
                    }
                }
            }
        }
        item { SectionHeader("设置") }
        item {
            GroupedCard {
                ToggleRow(
                    "用前置摄像头",
                    useFront,
                    onCheckedChange = { useFront = it },
                    subtitle = "关掉改用后置，可以当放大观察用"
                )
                ToggleRow("屏幕常亮", keepAwake, onCheckedChange = { keepAwake = it })
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "画面只在本机实时显示，不拍照、不录像、不保存、不上传任何内容。退出页面摄像头立即释放。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraMirrorPreview(useFront: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalIosPalette.current
    var error by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                update = { previewView ->
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        runCatching {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder()
                                .setTargetResolution(Size(1080, 1920))
                                .build()
                                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA
                            else CameraSelector.DEFAULT_BACK_CAMERA
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        }.onFailure {
                            error = "打不开摄像头，可能被其他应用占用了"
                        }
                    }, androidx.core.content.ContextCompat.getMainExecutor(context))
                }
            )
        }
        if (error.isNotBlank()) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
        }
    }
}
