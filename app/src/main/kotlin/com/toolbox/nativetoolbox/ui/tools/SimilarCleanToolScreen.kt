package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import com.toolbox.nativetoolbox.util.PermissionGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Pic(val id: Long, val uri: Uri, val size: Long, val hash: Long, var thumb: Bitmap? = null)

/** dHash:9x8 灰度,相邻比较出 64bit */
private fun dHash(bmp: Bitmap): Long {
    val s = Bitmap.createScaledBitmap(bmp, 9, 8, true)
    var hash = 0L
    var bit = 0
    for (y in 0 until 8) {
        for (x in 0 until 8) {
            val l = s.getPixel(x, y).let { (it shr 16 and 0xFF) + (it shr 8 and 0xFF) + (it and 0xFF) }
            val r = s.getPixel(x + 1, y).let { (it shr 16 and 0xFF) + (it shr 8 and 0xFF) + (it and 0xFF) }
            if (l > r) hash = hash or (1L shl bit)
            bit++
        }
    }
    s.recycle()
    return hash
}

private fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

@Composable
private fun SimilarContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf<List<List<Pic>>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var status by remember { mutableStateOf("") }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            groups = groups.map { g -> g.filter { it.id !in selected } }.filter { it.size >= 2 }
            status = "已删除 ${selected.size} 张"
            selected = emptySet()
        } else status = "取消了删除"
    }

    fun scan() {
        scanning = true; progress = "读取相册…"; groups = emptyList(); selected = emptySet(); status = ""
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val pics = ArrayList<Pic>()
                val proj = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.SIZE)
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
                )?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    while (c.moveToNext() && pics.size < 1200) {
                        val id = c.getLong(idCol)
                        pics.add(Pic(id, Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()), c.getLong(sizeCol), 0))
                    }
                }
                // 逐张缩图算 hash
                val hashed = ArrayList<Pic>()
                pics.forEachIndexed { i, p ->
                    if (i % 40 == 0) progress = "分析中 $i/${pics.size}…"
                    runCatching {
                        val thumb = if (Build.VERSION.SDK_INT >= 29)
                            context.contentResolver.loadThumbnail(p.uri, android.util.Size(160, 160), null)
                        else MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver, p.id, MediaStore.Images.Thumbnails.MINI_KIND, null)
                        if (thumb != null) {
                            hashed.add(p.copy(hash = dHash(thumb)).also { it.thumb = thumb })
                        }
                    }
                }
                // 分组:并查集式贪心,汉明 ≤ 6 归一组
                val used = BooleanArray(hashed.size)
                val out = ArrayList<List<Pic>>()
                for (i in hashed.indices) {
                    if (used[i]) continue
                    val g = arrayListOf(hashed[i]); used[i] = true
                    for (j in i + 1 until hashed.size) {
                        if (!used[j] && hamming(hashed[i].hash, hashed[j].hash) <= 6) {
                            g.add(hashed[j]); used[j] = true
                        }
                    }
                    if (g.size >= 2) out.add(g)
                }
                out.sortedByDescending { it.size }
            }
            groups = result
            progress = ""
            scanning = false
            status = if (result.isEmpty()) "没发现明显重复的照片,相册很干净" else ""
        }
    }

    fun deleteSelected() {
        if (selected.isEmpty()) return
        val uris = groups.flatten().filter { it.id in selected }.map { it.uri }
        if (Build.VERSION.SDK_INT >= 30) {
            val pi = MediaStore.createDeleteRequest(context.contentResolver, uris)
            deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
        } else {
            var ok = 0
            uris.forEach { if (runCatching { context.contentResolver.delete(it, null, null) }.getOrDefault(0) > 0) ok++ }
            groups = groups.map { g -> g.filter { it.id !in selected } }.filter { it.size >= 2 }
            status = "已删除 $ok 张"
            selected = emptySet()
        }
    }

    GroupedCard {
        CardPadding {
            Text(
                if (groups.isEmpty()) "扫描相册,找出连拍和重复保存的照片"
                else "找到 ${groups.size} 组相似照片,共 ${groups.sumOf { it.size }} 张",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.label
            )
            if (progress.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(progress, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
            }
            Spacer(Modifier.height(12.dp))
            SolidButton(onClick = { scan() }, Modifier.fillMaxWidth(), enabled = !scanning) {
                Text(if (scanning) "扫描中…" else if (groups.isEmpty()) "开始扫描" else "重新扫描")
            }
            if (selected.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SolidButton(onClick = { deleteSelected() }, Modifier.fillMaxWidth()) {
                    Text("删除选中的 ${selected.size} 张(会要求确认)")
                }
            }
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.green)
            }
        }
    }
    groups.take(20).forEachIndexed { gi, g ->
        Spacer(Modifier.height(20.dp))
        SectionHeader("第 ${gi + 1} 组 · ${g.size} 张(点选要删的,留一张最清晰的)")
        GroupedCard {
            CardPadding {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    g.take(4).forEach { p ->
                        val isSel = p.id in selected
                        Box(
                            Modifier
                                .weight(1f)
                                .height(84.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selected = if (isSel) selected - p.id else selected + p.id
                                }
                        ) {
                            p.thumb?.let {
                                Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            if (isSel) {
                                Box(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                        .background(androidx.compose.ui.graphics.Color(0x99FF3B30))
                                ) {
                                    Text("删", Modifier.align(Alignment.Center), color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                FileHelper.formatFileSize(p.size),
                                Modifier.align(Alignment.BottomStart).padding(4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                }
                if (g.size > 4) {
                    Text("还有 ${g.size - 4} 张同组未展示", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
    }
}

@Composable
fun SimilarCleanToolScreen(onBack: () -> Unit) {
    val permission = if (Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_IMAGES
    else android.Manifest.permission.READ_EXTERNAL_STORAGE

    ToolScaffold {
        item {
            PermissionGate(permission, "扫描相册找重复照片需要读相册。分析全在本机,不上传") {
                SimilarContent()
            }
        }
    }
}
