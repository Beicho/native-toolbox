package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private fun value(json: JSONObject, vararg keys: String): String {
    keys.forEach { k ->
        val v = json.optString(k)
        if (v.isNotBlank() && v != "null") return v
    }
    return ""
}

private fun arrayToText(arr: JSONArray?): String {
    if (arr == null || arr.length() == 0) return ""
    return (0 until arr.length())
        .mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() && s != "null" } }
        .joinToString("\n")
}

@Composable
fun WhoisToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var domain by rememberSaveable { mutableStateOf("") }
    var raw by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun query() {
        val target = domain.trim()
            .removePrefix("https://").removePrefix("http://")
            .trimEnd('/').substringBefore('/')
        if (target.isBlank() || !target.contains('.')) {
            status = "输入一个完整域名，例如 example.com"
            raw = null
            return
        }
        loading = true
        status = ""
        scope.launch {
            AstroApi.get("/whois", mapOf("domain" to target))
                .onSuccess { res ->
                    raw = res.data.toString()
                    status = cachedHint(res.cachedAt)
                }
                .onFailure { e ->
                    raw = null
                    status = e.message ?: "查询失败，请检查网络"
                }
            loading = false
        }
    }

    val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
    val nameServers = json?.let { arrayToText(it.optJSONArray("nameServers") ?: it.optJSONArray("nameservers")) } ?: ""
    val statusList = json?.let { arrayToText(it.optJSONArray("status") ?: it.optJSONArray("statuses")) } ?: ""

    ToolScaffold {
        item { SectionHeader("域名查询") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        placeholder = "输入域名，例如 example.com",
                        mono = true
                    )
                    SolidButton(onClick = { query() }, enabled = !loading) {
                        Text(if (loading) "查询中…" else "查询")
                    }
                    Text(
                        "查询域名的注册信息、到期时间和 DNS 服务器，需要联网。部分域名注册商会隐藏所有者信息。",
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
            item { SectionHeader("注册信息") }
            item {
                GroupedCard {
                    KeyValueRow("域名", value(json, "domain", "ldhName", "name"))
                    RowDivider()
                    KeyValueRow("注册商", value(json, "registrar", "registrarName"))
                    RowDivider()
                    KeyValueRow("注册时间", value(json, "createdDate", "registered", "creationDate"))
                    RowDivider()
                    KeyValueRow("到期时间", value(json, "expiresDate", "expiry", "expirationDate"))
                    RowDivider()
                    KeyValueRow("最近更新", value(json, "updatedDate", "lastChanged"))
                    RowDivider()
                    KeyValueRow("所有者", value(json, "registrant", "registrantName").ifBlank { "已隐藏" })
                }
            }
            if (nameServers.isNotBlank()) {
                item { SectionHeader("DNS 服务器") }
                item { GroupedCard { CardPadding { OutputCard(text = nameServers, label = "nameservers") } } }
            }
            if (statusList.isNotBlank()) {
                item { SectionHeader("域名状态") }
                item { GroupedCard { CardPadding { OutputCard(text = statusList, label = "status") } } }
            }
        }
    }
}
