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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val zoomSteps = listOf(1f, 2f, 4f, 6f)

@Composable
fun MagnifierToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val view = LocalView.current

    var zoomIndex by rememberSaveable { mutableStateOf(1) }
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var keepAwake by rememberSaveable { mutableStateOf(true) }

    DisposableEffect(keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    ToolScaffold {
        item { SectionHeader("放大镜") }
        item {
            GroupedCard {
                CardPadding {
                    PermissionGateWrapper {
                        MagnifierPreview(zoomSteps[zoomIndex], torchOn)
                    }
                }
            }
        }
        item { SectionHeader("放大倍数") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = zoomSteps.map { it.toInt().toString() + "×" },
                        selectedIndex = zoomIndex,
                        onSelected = { zoomIndex = it }
                    )
                    Text(
                        "实际倍数取决于手机镜头的变焦能力，超出范围会自动取最大值。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
                ToggleRow(
                    "打开补光",
                    torchOn,
                    onCheckedChange = { torchOn = it },
                    subtitle = "看药品说明书、小字标签时很有用"
                )
                ToggleRow("屏幕常亮", keepAwake, onCheckedChange = { keepAwake = it })
            }
        }
    }
}

@Composable
private fun PermissionGateWrapper(content: @Composable () -> Unit) {
    com.toolbox.nativetoolbox.util.PermissionGate(
        Manifest.permission.CAMERA,
        "放大镜需要用摄像头取景"
    ) { content() }
}

@Composable
private fun MagnifierPreview(zoom: Float, torchOn: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalIosPalette.current
    var error by remember { mutableStateOf("") }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // 变焦和补光都作用在已绑定的 Camera 上，不用重新绑定
    DisposableEffect(zoom, torchOn, camera) {
        camera?.let { cam ->
            runCatching {
                val state = cam.cameraInfo.zoomState.value
                val max = state?.maxZoomRatio ?: 1f
                val min = state?.minZoomRatio ?: 1f
                cam.cameraControl.setZoomRatio(zoom.coerceIn(min, max))
                if (cam.cameraInfo.hasFlashUnit()) cam.cameraControl.enableTorch(torchOn)
            }
        }
        onDispose { runCatching { camera?.cameraControl?.enableTorch(false) } }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                },
                modifier = Modifier.fillMaxWidth(),
                update = { previewView ->
                    if (camera != null) return@AndroidView
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        runCatching {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder()
                                .setTargetResolution(Size(1080, 1920))
                                .build()
                                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
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
