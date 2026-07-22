package com.toolbox.nativetoolbox.ui.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

@Composable
fun FileHashToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf(0L) }
    var computing by remember { mutableStateOf(false) }
    var hashes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u ->
        if (u != null) {
            uri = u
            fileName = u.lastPathSegment ?: "文件"
            fileSize = FileHelper.getFileSize(context, u)
            hashes = emptyMap()
        }
    }

    LaunchedEffect(uri) {
        val u = uri ?: return@LaunchedEffect
        computing = true
        hashes = withContext(Dispatchers.IO) {
            runCatching {
                val digests = listOf("MD5", "SHA-1", "SHA-256").associateWith { MessageDigest.getInstance(it) }
                context.contentResolver.openInputStream(u)?.use { input ->
                    val buf = ByteArray(1 shl 16)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        digests.values.forEach { it.update(buf, 0, n) }
                    }
                }
                digests.mapValues { (_, d) -> d.digest().joinToString("") { "%02x".format(it) } }
            }.getOrDefault(mapOf("错误" to "读取失败"))
        }
        computing = false
    }

    ToolScaffold {
        item { SectionHeader("文件") }
        item {
            GroupedCard {
                CardPadding {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        SolidButton(onClick = { picker.launch("*/*") }) {
                            Text("选择文件", color = Color.White)
                        }
                    }
                }
                KeyValueRow("文件名", fileName, copyable = false)
                RowDivider()
                KeyValueRow("大小", if (fileSize > 0) FileHelper.formatFileSize(fileSize) else "", copyable = false)
            }
        }
        item { SectionHeader(if (computing) "计算中…" else "哈希(点击复制)") }
        item {
            GroupedCard {
                if (computing) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = palette.accent, strokeWidth = 3.dp)
                    }
                } else {
                    KeyValueRow("MD5", hashes["MD5"] ?: "")
                    RowDivider()
                    KeyValueRow("SHA-1", hashes["SHA-1"] ?: "")
                    RowDivider()
                    KeyValueRow("SHA-256", hashes["SHA-256"] ?: "")
                }
            }
        }
    }
}
