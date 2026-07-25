package com.toolbox.nativetoolbox.ui.tools

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

private data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val fileSize: String,
    val permissions: List<String>,
    val signerSha256: String?,
    val signerSha1: String?,
)

private fun analyze(context: android.content.Context, tmp: File): ApkInfo? {
    val pm = context.packageManager
    @Suppress("DEPRECATION")
    val flags = PackageManager.GET_PERMISSIONS or (if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
    @Suppress("DEPRECATION")
    val info: PackageInfo = pm.getPackageArchiveInfo(tmp.absolutePath, flags) ?: return null
    info.applicationInfo?.sourceDir = tmp.absolutePath
    info.applicationInfo?.publicSourceDir = tmp.absolutePath
    val label = info.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: "(未知)"

    // 签名证书摘要
    @Suppress("DEPRECATION")
    val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= 28) {
        info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
    } else {
        info.signatures?.firstOrNull()?.toByteArray()
    }
    fun digest(algo: String) = certBytes?.let {
        MessageDigest.getInstance(algo).digest(it).joinToString(":") { b -> "%02X".format(b) }
    }

    @Suppress("DEPRECATION")
    val verCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    return ApkInfo(
        appName = label,
        packageName = info.packageName ?: "(未知)",
        versionName = info.versionName ?: "(未知)",
        versionCode = verCode,
        minSdk = info.applicationInfo?.minSdkVersion ?: 0,
        targetSdk = info.applicationInfo?.targetSdkVersion ?: 0,
        fileSize = FileHelper.formatFileSize(tmp.length()),
        permissions = info.requestedPermissions?.toList() ?: emptyList(),
        signerSha256 = digest("SHA-256"),
        signerSha1 = digest("SHA-1"),
    )
}

@Composable
fun ApkAnalyzeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<ApkInfo?>(null) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true; status = ""; info = null
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    // content uri 复制到私有缓存(PackageManager 只认文件路径)
                    val tmp = File(context.cacheDir, "analyze.apk")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { input.copyTo(it) }
                    } ?: return@runCatching null
                    val a = analyze(context, tmp)
                    tmp.delete()
                    a
                }.getOrNull()
            }
            info = r
            status = if (r == null) "解析失败:这个文件可能不是有效的 APK" else ""
            busy = false
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    if (info == null) {
                        Text("选一个 APK 文件,看看它的底细", style = MaterialTheme.typography.bodyLarge, color = palette.tertiaryLabel)
                        Spacer(Modifier.height(12.dp))
                    }
                    SolidButton(onClick = { picker.launch("*/*") }, Modifier.fillMaxWidth(), enabled = !busy) {
                        Text(if (busy) "解析中…" else if (info == null) "选 APK 文件" else "换一个")
                    }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = palette.red)
                    }
                }
            }
        }
        val i = info
        if (i != null) {
            item { SectionHeader("基本信息") }
            item {
                GroupedCard {
                    KeyValueRow("应用名", i.appName)
                    RowDivider()
                    KeyValueRow("包名", i.packageName)
                    RowDivider()
                    KeyValueRow("版本", "${i.versionName} (${i.versionCode})")
                    RowDivider()
                    KeyValueRow("文件大小", i.fileSize, copyable = false)
                    RowDivider()
                    KeyValueRow("最低系统", "Android API ${i.minSdk}", copyable = false)
                    RowDivider()
                    KeyValueRow("目标系统", "Android API ${i.targetSdk}", copyable = false)
                }
            }
            if (i.signerSha256 != null) {
                item { SectionHeader("签名证书") }
                item {
                    GroupedCard {
                        CardPadding {
                            Text("SHA-256", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                            Spacer(Modifier.height(2.dp))
                            OutputCard(i.signerSha256, Modifier, label = "")
                            if (i.signerSha1 != null) {
                                Spacer(Modifier.height(8.dp))
                                Text("SHA-1", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                                Spacer(Modifier.height(2.dp))
                                OutputCard(i.signerSha1, Modifier, label = "")
                            }
                        }
                    }
                }
            }
            item { SectionHeader("申请的权限(${i.permissions.size})") }
            item {
                if (i.permissions.isEmpty()) {
                    GroupedCard { CardPadding { Text("一个权限都不要,清爽", style = MaterialTheme.typography.bodyMedium, color = palette.green) } }
                } else {
                    GroupedCard {
                        i.permissions.forEachIndexed { idx, p ->
                            val short = p.substringAfterLast('.')
                            val danger = short in setOf(
                                "CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION",
                                "READ_CONTACTS", "WRITE_CONTACTS", "READ_SMS", "SEND_SMS", "READ_CALL_LOG",
                                "READ_PHONE_STATE", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE",
                                "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "SYSTEM_ALERT_WINDOW",
                                "REQUEST_INSTALL_PACKAGES", "QUERY_ALL_PACKAGES", "ACCESS_BACKGROUND_LOCATION",
                            )
                            KeyValueRow(short, if (danger) "敏感" else "", copyable = false)
                            if (idx != i.permissions.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}
