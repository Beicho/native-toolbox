package com.toolbox.nativetoolbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.AssetProvisioner
import kotlinx.coroutines.launch

/**
 * 字库下载门:需要大字库的工具(归属地/拼音)首次进入时显示。
 *
 * 为什么不打包进 APK:这两个库合起来 4.6MB,但只有 2 个工具用得到。
 * 让所有用户都下 4.6MB 不合理,首次用时再拉 5 秒就好。
 */
@Composable
fun AssetGate(
    asset: AssetProvisioner.Asset,
    onReady: @Composable () -> Unit,
) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(AssetProvisioner.isAvailable(asset)) }
    var downloading by remember { mutableStateOf(false) }
    var percent by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf("") }

    if (ready) {
        onReady()
        return
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "需要先下载${asset.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.label
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "这个功能要用一个 %.1f MB 的字库。".format(asset.approxBytes / 1024f / 1024f) +
                            "为了不让所有人的安装包都变大,改成用到时再下。下载一次就永久可用,之后完全离线。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryLabel
                    )
                    Spacer(Modifier.height(14.dp))

                    if (downloading) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(palette.sunkenBackground)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(percent / 100f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.accent)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "下载中 $percent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    } else {
                        SolidButton(
                            onClick = {
                                downloading = true
                                error = ""
                                scope.launch {
                                    AssetProvisioner.download(asset) { p -> percent = p.percent }
                                        .onSuccess { ready = true }
                                        .onFailure { error = "下载失败:${it.message ?: "检查一下网络"}" }
                                    downloading = false
                                }
                            },
                            Modifier.fillMaxWidth()
                        ) { Text("下载并使用") }
                    }

                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                }
            }
        }
    }
}
