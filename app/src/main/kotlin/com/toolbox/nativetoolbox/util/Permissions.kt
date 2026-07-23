package com.toolbox.nativetoolbox.util

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.toolbox.nativetoolbox.ui.components.SolidButton

/**
 * 运行时权限门:工具页首次用到敏感能力时就地申请。
 * 拒绝后展示一句话用途解释 + 引导去系统设置,绝不反复弹窗骚扰。
 */
@Composable
fun PermissionGate(
    permission: String,
    rationale: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var denied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        granted = ok
        if (!ok) denied = true
    }

    if (granted) {
        content()
        return
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            rationale,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (!denied) {
            SolidButton(text = "允许使用") { launcher.launch(permission) }
        } else {
            Text(
                "你拒绝了授权,此功能无法工作。可以去系统设置里手动开启。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            SolidButton(text = "去设置开启") {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
