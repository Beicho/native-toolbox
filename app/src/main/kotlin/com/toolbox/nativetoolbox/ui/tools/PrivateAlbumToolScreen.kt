package com.toolbox.nativetoolbox.ui.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 私密相册:照片 AES-256-GCM 加密后存 App 私有目录,密码只存加盐哈希。
 * 忘了密码 = 谁也解不开,包括我们。
 */
private object Vault {
    private fun dir(ctx: android.content.Context) = File(ctx.filesDir, "vault").apply { mkdirs() }
    private fun metaFile(ctx: android.content.Context) = File(dir(ctx), ".meta")

    fun isSetup(ctx: android.content.Context) = metaFile(ctx).exists()

    private fun key(pass: String, salt: ByteArray): SecretKeySpec {
        // PBKDF2 太慢的低端机也要能用:SHA-256 迭代 6 万次
        var h = pass.toByteArray() + salt
        val md = MessageDigest.getInstance("SHA-256")
        repeat(60_000) { h = md.digest(h) }
        return SecretKeySpec(h, "AES")
    }

    fun setup(ctx: android.content.Context, pass: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val check = MessageDigest.getInstance("SHA-256").digest(key(pass, salt).encoded + salt)
        metaFile(ctx).writeBytes(salt + check)
    }

    fun verify(ctx: android.content.Context, pass: String): Boolean {
        val meta = metaFile(ctx).takeIf { it.exists() }?.readBytes() ?: return false
        val salt = meta.copyOfRange(0, 16)
        val check = MessageDigest.getInstance("SHA-256").digest(key(pass, salt).encoded + salt)
        return check.contentEquals(meta.copyOfRange(16, meta.size))
    }

    private fun cipherKey(ctx: android.content.Context, pass: String): SecretKeySpec {
        val salt = metaFile(ctx).readBytes().copyOfRange(0, 16)
        return key(pass, salt)
    }

    fun importImage(ctx: android.content.Context, pass: String, bytes: ByteArray): Boolean = runCatching {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, cipherKey(ctx, pass), GCMParameterSpec(128, iv))
        val enc = c.doFinal(bytes)
        File(dir(ctx), "${System.currentTimeMillis()}.enc").writeBytes(iv + enc)
        true
    }.getOrDefault(false)

    fun list(ctx: android.content.Context): List<File> =
        dir(ctx).listFiles { f -> f.name.endsWith(".enc") }?.sortedByDescending { it.name } ?: emptyList()

    fun decrypt(ctx: android.content.Context, pass: String, f: File): Bitmap? = runCatching {
        val raw = f.readBytes()
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, cipherKey(ctx, pass), GCMParameterSpec(128, raw.copyOfRange(0, 12)))
        val dec = c.doFinal(raw, 12, raw.size - 12)
        BitmapFactory.decodeByteArray(dec, 0, dec.size)
    }.getOrNull()

    fun export(ctx: android.content.Context, pass: String, f: File): Boolean {
        val bmp = decrypt(ctx, pass, f) ?: return false
        val bytes = ImageUtil.encode(bmp, Bitmap.CompressFormat.JPEG, 95)
        bmp.recycle()
        val ok = ImageUtil.saveToPictures(ctx, "restore_${f.name.removeSuffix(".enc")}.jpg", bytes, "image/jpeg").isSuccess
        if (ok) f.delete()
        return ok
    }

    fun delete(f: File) { f.delete() }
}

@Composable
fun PrivateAlbumToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pass by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var setupMode by remember { mutableStateOf(!Vault.isSetup(context)) }
    var status by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var thumbs by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    var viewing by remember { mutableStateOf<Bitmap?>(null) }
    var viewingFile by remember { mutableStateOf<File?>(null) }

    fun refresh() {
        files = Vault.list(context)
        scope.launch {
            val m = withContext(Dispatchers.Default) {
                files.associate { f ->
                    val full = Vault.decrypt(context, pass, f)
                    val thumb = full?.let { Bitmap.createScaledBitmap(it, 240, 240 * it.height / it.width.coerceAtLeast(1), true) }
                    if (full != null && thumb !== full) full.recycle()
                    f.name to (thumb ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                }
            }
            thumbs = m
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            var ok = 0
            withContext(Dispatchers.IO) {
                for (u in uris) {
                    val bytes = context.contentResolver.openInputStream(u)?.use { it.readBytes() } ?: continue
                    if (Vault.importImage(context, pass, bytes)) ok++
                }
            }
            status = "已加密收纳 $ok 张。原图还在相册里,确认后记得自己删原图"
            refresh()
        }
    }

    ToolScaffold {
        if (!unlocked) {
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            if (setupMode) "先设一个相册密码" else "输入密码解锁",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.label
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (setupMode) "照片会用这个密码加密存进 App 里,相册和文件管理器都看不到。忘了密码谁也找不回来,记牢。"
                            else "照片全程加密存放,只有密码能打开",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                        Spacer(Modifier.height(12.dp))
                        IosTextField(pass, { pass = it }, Modifier.fillMaxWidth(), placeholder = "密码(至少 4 位)")
                        Spacer(Modifier.height(12.dp))
                        SolidButton(
                            onClick = {
                                if (setupMode) {
                                    if (pass.length < 4) { status = "至少 4 位"; return@SolidButton }
                                    Vault.setup(context, pass)
                                    setupMode = false; unlocked = true; status = ""
                                    refresh()
                                } else {
                                    if (Vault.verify(context, pass)) { unlocked = true; status = ""; refresh() }
                                    else status = "密码不对"
                                }
                            },
                            Modifier.fillMaxWidth(),
                            enabled = pass.isNotEmpty()
                        ) { Text(if (setupMode) "创建私密相册" else "解锁") }
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                        }
                    }
                }
            }
        } else {
            item {
                val v = viewing
                if (v != null) {
                    GroupedCard {
                        CardPadding {
                            Image(v.asImageBitmap(), contentDescription = "私密照片", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SolidButton(onClick = { viewing = null; viewingFile = null }, Modifier.weight(1f), filled = false) { Text("收起") }
                                SolidButton(onClick = {
                                    val f = viewingFile ?: return@SolidButton
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) { Vault.export(context, pass, f) }
                                        status = if (ok) "已解密还原到相册" else "还原失败"
                                        viewing = null; viewingFile = null
                                        refresh()
                                    }
                                }, Modifier.weight(1f), filled = false) { Text("还原到相册") }
                                SolidButton(onClick = {
                                    viewingFile?.let { Vault.delete(it) }
                                    viewing = null; viewingFile = null
                                    refresh()
                                }, Modifier.weight(1f)) { Text("彻底删除") }
                            }
                        }
                    }
                }
            }
            item {
                GroupedCard {
                    CardPadding {
                        Text("${files.size} 张已加密", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.label)
                        if (status.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(status, style = MaterialTheme.typography.bodySmall, color = palette.green)
                        }
                        Spacer(Modifier.height(10.dp))
                        SolidButton(onClick = { picker.launch("image/*") }, Modifier.fillMaxWidth()) { Text("添加照片(可多选)") }
                    }
                }
            }
            item {
                if (files.isNotEmpty()) {
                    GroupedCard {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(((files.size + 2) / 3 * 124).coerceAtMost(500).dp)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(files, key = { it.name }) { f ->
                                val thumb = thumbs[f.name]
                                Box(
                                    Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scope.launch {
                                                val full = withContext(Dispatchers.Default) { Vault.decrypt(context, pass, f) }
                                                if (full != null) { viewing = full; viewingFile = f }
                                            }
                                        }
                                ) {
                                    if (thumb != null && thumb.width > 1) {
                                        Image(thumb.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("…", color = palette.tertiaryLabel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
