package com.toolbox.nativetoolbox.ui.tools

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private fun queryName(context: android.content.Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && i >= 0) c.getString(i) else null
        }
    }.getOrNull() ?: "file_${System.currentTimeMillis()}"

@Composable
fun ZipToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(0) } // 0 打包 1 解压
    var picked by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var zipUri by remember { mutableStateOf<Uri?>(null) }
    var zipEntries by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) { picked = picked + uris; status = "" }
    }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        zipUri = uri
        status = ""
        scope.launch {
            zipEntries = withContext(Dispatchers.IO) {
                runCatching {
                    val out = ArrayList<Pair<String, Long>>()
                    context.contentResolver.openInputStream(uri)?.use { ins ->
                        ZipInputStream(ins.buffered()).use { z ->
                            var e: ZipEntry? = z.nextEntry
                            var n = 0
                            while (e != null && n < 500) {
                                if (!e.isDirectory) { out.add(e.name to e.size); n++ }
                                z.closeEntry()
                                e = z.nextEntry
                            }
                        }
                    }
                    out
                }.getOrDefault(emptyList())
            }
            if (zipEntries.isEmpty()) status = "打不开:可能不是 zip,或是带密码的压缩包(暂不支持密码)"
        }
    }

    fun doZip() {
        if (picked.isEmpty()) { status = "先选文件"; return }
        busy = true; status = "打包中…"
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val buf = java.io.ByteArrayOutputStream()
                    ZipOutputStream(buf).use { z ->
                        val usedNames = HashSet<String>()
                        for (u in picked) {
                            var name = queryName(context, u)
                            var i = 1
                            while (!usedNames.add(name)) { name = "${i++}_$name" }
                            z.putNextEntry(ZipEntry(name))
                            context.contentResolver.openInputStream(u)?.use { it.copyTo(z) }
                            z.closeEntry()
                        }
                    }
                    val bytes = buf.toByteArray()
                    val path = FileHelper.saveToDownloads(context, "打包_${System.currentTimeMillis()}.zip", bytes).getOrThrow()
                    path to bytes.size.toLong()
                }
            }
            status = r.fold(
                { (path, size) -> "已存到 $path(${FileHelper.formatFileSize(size)})" },
                { "打包失败:${it.message}" }
            )
            busy = false
        }
    }

    fun doUnzip() {
        val u = zipUri ?: return
        busy = true; status = "解压中…"
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    var count = 0
                    context.contentResolver.openInputStream(u)?.use { ins ->
                        ZipInputStream(ins.buffered()).use { z ->
                            var e: ZipEntry? = z.nextEntry
                            while (e != null) {
                                if (!e.isDirectory && count < 200) {
                                    // zip slip 防御:去掉路径只留文件名
                                    val safeName = e.name.substringAfterLast('/').substringAfterLast('\\')
                                    if (safeName.isNotBlank()) {
                                        val bytes = z.readBytes()
                                        FileHelper.saveToDownloads(context, safeName, bytes)
                                        count++
                                    }
                                }
                                z.closeEntry()
                                e = z.nextEntry
                            }
                        }
                    }
                    count
                }
            }
            status = r.fold({ "解压完成,$it 个文件已存到下载/AstroKit" }, { "解压失败:${it.message}" })
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("打包 zip", "解压 zip"), mode, { mode = it; status = "" }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    if (mode == 0) {
                        Text(
                            if (picked.isEmpty()) "选几个文件打成一个 zip 包" else "已选 ${picked.size} 个文件",
                            style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel
                        )
                        Spacer(Modifier.height(10.dp))
                        SolidButton(onClick = { multiPicker.launch("*/*") }, Modifier.fillMaxWidth(), filled = picked.isEmpty()) {
                            Text(if (picked.isEmpty()) "选文件(可多选)" else "继续加")
                        }
                        if (picked.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            SolidButton(onClick = { doZip() }, Modifier.fillMaxWidth(), enabled = !busy) { Text(if (busy) "打包中…" else "打包") }
                            Spacer(Modifier.height(8.dp))
                            SolidButton(onClick = { picked = emptyList() }, Modifier.fillMaxWidth(), filled = false) { Text("清空") }
                        }
                    } else {
                        SolidButton(onClick = { zipPicker.launch("application/zip") }, Modifier.fillMaxWidth(), filled = zipUri == null) {
                            Text(if (zipUri == null) "选 zip 文件" else "换一个")
                        }
                        if (zipEntries.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            SolidButton(onClick = { doUnzip() }, Modifier.fillMaxWidth(), enabled = !busy) {
                                Text(if (busy) "解压中…" else "全部解压(${zipEntries.size} 个文件)")
                            }
                        }
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("失败") || status.startsWith("打不开")) palette.red else palette.green)
                    }
                }
            }
        }
        item { if (mode == 1 && zipEntries.isNotEmpty()) SectionHeader("包内文件") }
        item {
            if (mode == 1 && zipEntries.isNotEmpty()) {
                GroupedCard {
                    val show = zipEntries.take(30)
                    show.forEachIndexed { i, (name, size) ->
                        KeyValueRow(name, if (size >= 0) FileHelper.formatFileSize(size) else "", copyable = false)
                        if (i != show.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
