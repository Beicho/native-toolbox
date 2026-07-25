package com.toolbox.nativetoolbox.ui.tools

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.rememberCopy
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 传输助手:像给自己发消息一样,把文字和文件存进时间线,随时复制/转发。
 * 文件复制进 App 私有目录,清相册也不丢。
 */
private data class Item(val time: Long, val text: String, val fileName: String?, val filePath: String?)

private fun loadItems(prefs: android.content.SharedPreferences): List<Item> = runCatching {
    val arr = JSONArray(prefs.getString("items", "[]"))
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Item(o.getLong("t"), o.optString("x"), o.optString("f").ifBlank { null }, o.optString("p").ifBlank { null })
    }
}.getOrDefault(emptyList())

private fun saveItems(prefs: android.content.SharedPreferences, list: List<Item>) {
    val arr = JSONArray()
    list.takeLast(200).forEach {
        arr.put(JSONObject().put("t", it.time).put("x", it.text).put("f", it.fileName ?: "").put("p", it.filePath ?: ""))
    }
    prefs.edit().putString("items", arr.toString()).apply()
}

@Composable
fun FileTransferToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val copy = rememberCopy()
    val prefs = remember { context.getSharedPreferences("file_transfer", android.content.Context.MODE_PRIVATE) }
    var items by remember { mutableStateOf(loadItems(prefs)) }
    var input by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val dir = remember { File(context.filesDir, "transfer").apply { mkdirs() } }
    val df = remember { SimpleDateFormat("M/d HH:mm", Locale.getDefault()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        var ok = 0
        for (u in uris) {
            val name = runCatching {
                context.contentResolver.query(u, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && i >= 0) c.getString(i) else null
                }
            }.getOrNull() ?: "file_${System.currentTimeMillis()}"
            val dst = File(dir, "${System.currentTimeMillis()}_$name")
            val copied = runCatching {
                context.contentResolver.openInputStream(u)?.use { ins ->
                    dst.outputStream().use { ins.copyTo(it) }
                } != null
            }.getOrDefault(false)
            if (copied) {
                items = items + Item(System.currentTimeMillis(), "", name, dst.absolutePath)
                ok++
            }
        }
        saveItems(prefs, items)
        status = if (ok > 0) "收好了 $ok 个文件" else "文件存不进来"
    }

    fun addText() {
        if (input.isBlank()) return
        items = items + Item(System.currentTimeMillis(), input.trim(), null, null)
        saveItems(prefs, items)
        input = ""
    }

    fun shareFile(path: String, name: String) {
        runCatching {
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(path))
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("*/*")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    "发送 $name"
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { status = "分享失败:${it.message}" }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(input, { input = it }, Modifier.fillMaxWidth(), placeholder = "存一段话:地址、口令、待办…", minHeight = 72.dp)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        SolidButton(onClick = { addText() }, Modifier.weight(1f), enabled = input.isNotBlank()) { Text("存文字") }
                        Spacer(Modifier.width(8.dp))
                        SolidButton(onClick = { picker.launch("*/*") }, Modifier.weight(1f), filled = false) { Text("存文件") }
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.green)
                    }
                }
            }
        }
        item {
            if (items.isNotEmpty()) {
                GroupedCard {
                    val recent = items.sortedByDescending { it.time }.take(50)
                    recent.forEachIndexed { i, it ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(df.format(Date(it.time)), style = MaterialTheme.typography.labelSmall, color = palette.tertiaryLabel)
                            Spacer(Modifier.height(3.dp))
                            if (it.fileName != null) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("📎 " + it.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = palette.label)
                                        val f = it.filePath?.let { p -> File(p) }
                                        Text(
                                            if (f?.exists() == true) FileHelper.formatFileSize(f.length()) else "文件已丢失",
                                            style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel
                                        )
                                    }
                                    SolidButton(
                                        onClick = { it.filePath?.let { p -> shareFile(p, it.fileName) } },
                                        Modifier.width(76.dp), height = 34.dp, filled = false
                                    ) { Text("发送") }
                                }
                            } else {
                                Text(it.text, style = MaterialTheme.typography.bodyMedium, color = palette.label, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    SolidButton(onClick = { copy(it.text) }, Modifier.width(76.dp), height = 30.dp, filled = false) { Text("复制") }
                                }
                            }
                        }
                        if (i != recent.lastIndex) RowDivider()
                    }
                }
            }
        }
        item {
            if (items.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SolidButton(
                        onClick = {
                            items.forEach { it.filePath?.let { p -> File(p).delete() } }
                            items = emptyList()
                            saveItems(prefs, items)
                        },
                        Modifier.fillMaxWidth(), filled = false
                    ) { Text("清空时间线") }
                }
            }
        }
    }
}
