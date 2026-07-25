package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONObject

private fun firstNonBlank(json: JSONObject, vararg keys: String): String {
    keys.forEach { k ->
        val v = json.optString(k)
        if (v.isNotBlank() && v != "null") return v
    }
    return ""
}

@Composable
fun SslCertToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var host by rememberSaveable { mutableStateOf("") }
    var raw by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun check() {
        val target = host.trim()
            .removePrefix("https://").removePrefix("http://")
            .trimEnd('/').substringBefore('/')
        if (target.isBlank()) {
            status = "先输入网站域名"
            raw = null
            return
        }
        loading = true
        status = ""
        scope.launch {
            AstroApi.get("/ssl", mapOf("host" to target))
                .onSuccess { res ->
                    raw = res.data.toString()
                    status = cachedHint(res.cachedAt)
                }
                .onFailure { e ->
                    raw = null
                    status = e.message ?: "检测失败，请检查网络或域名"
                }
            loading = false
        }
    }

    val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
    val daysLeft = json?.optInt("daysLeft", json.optInt("days_left", -1)) ?: -1
    val expiryColor = when {
        daysLeft < 0 -> palette.secondaryLabel
        daysLeft <= 7 -> palette.red
        daysLeft <= 30 -> palette.orange
        else -> palette.green
    }

    ToolScaffold {
        item { SectionHeader("检测证书") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = host,
                        onValueChange = { host = it },
                        placeholder = "输入网站域名，例如 github.com",
                        mono = true
                    )
                    SolidButton(onClick = { check() }, enabled = !loading) {
                        Text(if (loading) "检测中…" else "开始检测")
                    }
                    Text(
                        "会实时连接目标网站读取证书链，需要联网。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        if (status.isNotBlank()) {
            item {
                GroupedCard {
                    CardPadding {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    }
                }
            }
        }
        if (json != null) {
            item { SectionHeader("有效期") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell(
                                "剩余天数",
                                if (daysLeft >= 0) daysLeft.toString() else "—",
                                Modifier.weight(1f)
                            )
                            StatCell(
                                "状态",
                                when {
                                    daysLeft < 0 -> "未知"
                                    daysLeft == 0 -> "今天到期"
                                    daysLeft <= 30 -> "即将到期"
                                    else -> "正常"
                                },
                                Modifier.weight(1f)
                            )
                        }
                        if (daysLeft in 0..30) {
                            Text(
                                "证书快过期了，记得续期。",
                                style = MaterialTheme.typography.bodySmall,
                                color = expiryColor
                            )
                        }
                    }
                }
            }
            item { SectionHeader("证书信息") }
            item {
                GroupedCard {
                    KeyValueRow("域名", firstNonBlank(json, "host", "subject", "commonName"))
                    RowDivider()
                    KeyValueRow("颁发给", firstNonBlank(json, "subject", "commonName"))
                    RowDivider()
                    KeyValueRow("颁发机构", firstNonBlank(json, "issuer", "issuerOrg"))
                    RowDivider()
                    KeyValueRow("生效时间", firstNonBlank(json, "validFrom", "notBefore"))
                    RowDivider()
                    KeyValueRow("到期时间", firstNonBlank(json, "validTo", "notAfter"))
                    RowDivider()
                    KeyValueRow("签名算法", firstNonBlank(json, "signatureAlgorithm", "sigAlg"))
                    RowDivider()
                    KeyValueRow("序列号", firstNonBlank(json, "serialNumber", "serial"))
                }
            }
            val sans = json.optJSONArray("dnsNames") ?: json.optJSONArray("san")
            if (sans != null && sans.length() > 0) {
                item { SectionHeader("覆盖的域名 " + sans.length() + " 个") }
                item {
                    GroupedCard {
                        val names = (0 until sans.length()).mapNotNull { sans.optString(it).takeIf { s -> s.isNotBlank() } }
                        names.forEachIndexed { index, name ->
                            KeyValueRow((index + 1).toString(), name)
                            if (index != names.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}
