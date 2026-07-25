package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.HttpShareServer
import com.toolbox.nativetoolbox.util.ImageUtil
import java.net.Inet4Address
import java.net.NetworkInterface

/** 取局域网 IPv4(优先 wlan) */
private fun lanIp(context: android.content.Context): String? {
    runCatching {
        val ifaces = NetworkInterface.getNetworkInterfaces()
        val candidates = mutableListOf<Pair<String, String>>()
        for (ni in ifaces) {
            if (!ni.isUp || ni.isLoopback) continue
            for (addr in ni.inetAddresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    candidates.add(ni.name to addr.hostAddress.orEmpty())
                }
            }
        }
        return candidates.firstOrNull { it.first.startsWith("wlan") }?.second
            ?: candidates.firstOrNull()?.second
    }
    return null
}

@Composable
fun PhoneShareToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var serverOn by remember { mutableStateOf(false) }
    var ip by remember { mutableStateOf<String?>(null) }
    var files by remember { mutableStateOf<List<HttpShareServer.ShareFile>>(emptyList()) }
    var received by remember { mutableStateOf<List<String>>(emptyList()) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf("") }

    val server = remember {
        HttpShareServer(context).apply {
            onUpload = { name -> received = received + name }
        }
    }

    DisposableEffect(Unit) { onDispose { server.stop() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val newFiles = uris.mapNotNull { uri ->
            val name = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                }
            }.getOrNull() ?: "file_${System.currentTimeMillis()}"
            val size = FileHelper.getFileSize(context, uri)
            HttpShareServer.ShareFile(name, size, uri)
        }
        files = (files + newFiles).distinctBy { it.uri }
        server.shares = files
    }

    fun toggle() {
        if (serverOn) {
            server.stop(); serverOn = false; qr = null
        } else {
            val addr = lanIp(context)
            if (addr == null) { error = "没连 WiFi,手机和电脑要在同一个网络里"; return }
            if (!server.start()) { error = "端口被占用,稍后再试"; return }
            error = ""
            ip = addr
            serverOn = true
            qr = ImageUtil.generateQr("http://$addr:${server.port}", 600)
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (serverOn && ip != null) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            qr?.let { Image(it.asImageBitmap(), contentDescription = "地址二维码", modifier = Modifier.size(190.dp)) }
                            Spacer(Modifier.height(8.dp))
                            Text("http://$ip:${server.port}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                            Spacer(Modifier.height(4.dp))
                            Text("电脑浏览器打开这个地址(或扫码),就能互传文件", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        }
                    } else {
                        Text("把手机变成局域网小网盘:电脑浏览器直接下载手机分享的文件,也能上传文件到手机,不走外网。", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                    }
                    Spacer(Modifier.height(12.dp))
                    SolidButton(onClick = { toggle() }, Modifier.fillMaxWidth()) {
                        Text(if (serverOn) "停止服务" else "启动服务")
                    }
                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                }
            }
        }
        item { SectionHeader("分享给电脑的文件(${files.size})") }
        item {
            GroupedCard {
                if (files.isEmpty()) {
                    CardPadding { Text("还没添加,点下面按钮选文件", style = MaterialTheme.typography.bodyMedium, color = palette.tertiaryLabel) }
                } else {
                    files.forEachIndexed { i, f ->
                        KeyValueRow(f.name, FileHelper.formatFileSize(f.size), copyable = false)
                        if (i != files.lastIndex) RowDivider()
                    }
                }
                CardPadding {
                    Row(Modifier.fillMaxWidth()) {
                        SolidButton(onClick = { picker.launch("*/*") }, Modifier.weight(1f), filled = false) { Text("添加文件") }
                        if (files.isNotEmpty()) {
                            Spacer(Modifier.size(8.dp))
                            SolidButton(onClick = { files = emptyList(); server.shares = emptyList() }, Modifier.weight(1f), filled = false) { Text("清空") }
                        }
                    }
                }
            }
        }
        item { if (received.isNotEmpty()) SectionHeader("电脑传来的(存在下载/AstroKit)") }
        item {
            if (received.isNotEmpty()) {
                GroupedCard {
                    received.forEachIndexed { i, name ->
                        KeyValueRow(name, "已收到", copyable = false)
                        if (i != received.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
