package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONObject

internal fun cachedHint(millis: Long): String {
    if (millis <= 0) return ""
    val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return "网络不通，显示的是 " + fmt.format(java.util.Date(millis)) + " 的缓存数据"
}

private fun pick(json: JSONObject, vararg keys: String): String {
    keys.forEach { key ->
        val v = json.optString(key)
        if (v.isNotBlank() && v != "null") return v
    }
    return ""
}

@Composable
fun IpQueryToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var data by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun load(target: String) {
        loading = true
        status = ""
        scope.launch {
            val params = if (target.isBlank()) emptyMap() else mapOf("ip" to target)
            AstroApi.get("/ip", params)
                .onSuccess { res ->
                    data = res.data.toString()
                    status = cachedHint(res.cachedAt)
                }
                .onFailure { e ->
                    data = null
                    status = e.message ?: "查询失败，请检查网络"
                }
            loading = false
        }
    }

    LaunchedEffect(Unit) { if (data == null) load("") }

    val json = data?.let { runCatching { JSONObject(it) }.getOrNull() }

    ToolScaffold {
        item { SectionHeader("查询") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "留空查自己的公网 IP，或输入要查的 IP",
                        mono = true
                    )
                    SolidButton(onClick = { load(query.trim()) }, enabled = !loading) {
                        Text(if (loading) "查询中…" else if (query.isBlank()) "查我的 IP" else "查询")
                    }
                    Text(
                        "这个功能需要联网。",
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
            item { SectionHeader("结果") }
            item {
                GroupedCard {
                    KeyValueRow("IP", pick(json, "ip", "query"))
                    RowDivider()
                    KeyValueRow("国家或地区", pick(json, "country"))
                    RowDivider()
                    KeyValueRow("省份", pick(json, "region", "regionName"))
                    RowDivider()
                    KeyValueRow("城市", pick(json, "city"))
                    RowDivider()
                    KeyValueRow("运营商", pick(json, "isp", "org"))
                    RowDivider()
                    KeyValueRow("自治域", pick(json, "as", "asn"))
                    RowDivider()
                    KeyValueRow("时区", pick(json, "timezone"))
                    RowDivider()
                    KeyValueRow("经纬度", listOf(pick(json, "lat"), pick(json, "lon")).filter { it.isNotBlank() }.joinToString(", "))
                }
            }
        }
    }
}
