package com.toolbox.nativetoolbox.ui.tools

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
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
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private data class TagInfo(
    val uid: String,
    val techs: List<String>,
    val ndefText: String?,
    val ndefCapacity: Int,
    val writable: Boolean,
)

private fun parseTag(tag: Tag): TagInfo {
    val uid = tag.id?.joinToString(":") { "%02X".format(it) } ?: "(无)"
    val techs = tag.techList.map { it.substringAfterLast('.') }
    var text: String? = null
    var capacity = 0
    var writable = false
    runCatching {
        Ndef.get(tag)?.let { ndef ->
            ndef.connect()
            capacity = ndef.maxSize
            writable = ndef.isWritable
            val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage
            text = msg?.records?.joinToString("\n") { rec ->
                when {
                    rec.tnf == NdefRecord.TNF_WELL_KNOWN && rec.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                        val payload = rec.payload
                        val langLen = payload[0].toInt() and 0x3F
                        String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
                    }
                    rec.tnf == NdefRecord.TNF_WELL_KNOWN && rec.type.contentEquals(NdefRecord.RTD_URI) -> rec.toUri()?.toString() ?: ""
                    else -> String(rec.payload, Charsets.UTF_8)
                }
            }
            ndef.close()
        }
    }
    return TagInfo(uid, techs, text?.takeIf { it.isNotBlank() }, capacity, writable)
}

private fun writeTag(tag: Tag, text: String): String {
    val record = NdefRecord.createTextRecord("zh", text)
    val msg = NdefMessage(arrayOf(record))
    return runCatching {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            ndef.connect()
            if (!ndef.isWritable) { ndef.close(); return "这张标签是只读的,写不进去" }
            if (msg.toByteArray().size > ndef.maxSize) { ndef.close(); return "内容超过标签容量(${ndef.maxSize} 字节)" }
            ndef.writeNdefMessage(msg)
            ndef.close()
            "写入成功"
        } else {
            val fmt = NdefFormatable.get(tag) ?: return "这张标签不支持写入"
            fmt.connect()
            fmt.format(msg)
            fmt.close()
            "已格式化并写入成功"
        }
    }.getOrElse { "写入失败:${it.message ?: "标签可能已移开"}" }
}

@Composable
fun NfcToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val activity = context as? Activity
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }

    var mode by rememberSaveable { mutableStateOf(0) } // 0 读 1 写
    var info by remember { mutableStateOf<TagInfo?>(null) }
    var writeText by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    DisposableEffect(mode, writeText) {
        if (adapter != null && activity != null && adapter.isEnabled) {
            adapter.enableReaderMode(
                activity,
                { tag ->
                    if (mode == 0) {
                        val parsed = parseTag(tag)
                        activity.runOnUiThread { info = parsed; status = "读到一张标签" }
                    } else {
                        val r = writeTag(tag, writeText.ifBlank { " " })
                        activity.runOnUiThread { status = r }
                    }
                },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }
        onDispose { if (activity != null) runCatching { adapter?.disableReaderMode(activity) } }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("读标签", "写标签"), mode, { mode = it; status = "" }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            adapter == null -> "这台手机没有 NFC"
                            !adapter.isEnabled -> "NFC 没开,去下拉快捷开关打开"
                            mode == 0 -> "把卡片/标签贴到手机背面"
                            else -> "填好内容,再把空白标签贴上来"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (adapter?.isEnabled == true) palette.secondaryLabel else palette.red
                    )
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = if (status.contains("成功") || status.contains("读到")) palette.green else palette.orange)
                    }
                }
            }
        }
        if (mode == 1) {
            item {
                GroupedCard {
                    CardPadding {
                        IosTextArea(writeText, { writeText = it }, Modifier.fillMaxWidth(), placeholder = "要写进标签的文字…", minHeight = 100.dp)
                        Spacer(Modifier.height(6.dp))
                        Text("适合写便签、WiFi 密码提示、联系方式。公交卡银行卡是加密的,写不了。", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                    }
                }
            }
        }
        val i = info
        if (mode == 0 && i != null) {
            item { SectionHeader("标签信息") }
            item {
                GroupedCard {
                    KeyValueRow("卡号 UID", i.uid)
                    RowDivider()
                    KeyValueRow("类型", i.techs.joinToString(" / "))
                    if (i.ndefCapacity > 0) {
                        RowDivider()
                        KeyValueRow("容量", "${i.ndefCapacity} 字节", copyable = false)
                        RowDivider()
                        KeyValueRow("可写", if (i.writable) "是" else "否(只读)", copyable = false)
                    }
                }
            }
            if (i.ndefText != null) {
                item { OutputCard(i.ndefText, Modifier, label = "标签里的内容") }
            }
        }
    }
}
