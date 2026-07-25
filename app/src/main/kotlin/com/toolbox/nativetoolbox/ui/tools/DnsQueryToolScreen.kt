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
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val recordTypes = listOf("A", "AAAA", "CNAME", "MX", "TXT", "NS")

private val typeHelp = mapOf(
    "A" to "域名指向的 IPv4 地址",
    "AAAA" to "域名指向的 IPv6 地址",
    "CNAME" to "别名，指向另一个域名",
    "MX" to "邮件服务器，数字越小优先级越高",
    "TXT" to "文本记录，常用于域名验证和 SPF",
    "NS" to "这个域名由哪些 DNS 服务器负责解析"
)

@Composable
fun DnsQueryToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var domain by rememberSaveable { mutableStateOf("") }
    var typeIndex by rememberSaveable { mutableStateOf(0) }
    var raw by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun query() {
        val name = domain.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        if (name.isBlank()) {
            status = "先输入要查的域名"
            raw = null
            return
        }
        loading = true
        status = ""
        scope.launch {
            AstroApi.get("/dns", mapOf("name" to name, "type" to recordTypes[typeIndex]))
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
    val answers: List<Pair<String, String>> = json?.let { obj ->
        val list = ArrayList<Pair<String, String>>()
        val arr: JSONArray? = obj.optJSONArray("answers") ?: obj.optJSONArray("Answer") ?: obj.optJSONArray("records")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val value = listOf("data", "value", "address", "target")
                    .map { item.optString(it) }
                    .firstOrNull { it.isNotBlank() && it != "null" } ?: continue
                val ttl = item.optInt("TTL", item.optInt("ttl", -1))
                list.add(value to if (ttl > 0) "TTL " + ttl + " 秒" else "")
            }
        }
        list
    } ?: emptyList()

    ToolScaffold {
        item { SectionHeader("查询") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        placeholder = "输入域名，例如 example.com",
                        mono = true
                    )
                    SegmentedPicker(
                        options = recordTypes,
                        selectedIndex = typeIndex,
                        onSelected = { typeIndex = it }
                    )
                    Text(
                        typeHelp[recordTypes[typeIndex]] ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                    SolidButton(onClick = { query() }, enabled = !loading) {
                        Text(if (loading) "查询中…" else "查询")
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
            item { SectionHeader(if (answers.isEmpty()) "没有解析记录" else "共 " + answers.size + " 条记录") }
            item {
                GroupedCard {
                    if (answers.isEmpty()) {
                        CardPadding {
                            Text(
                                "这个域名没有 " + recordTypes[typeIndex] + " 记录，换个类型试试",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel
                            )
                        }
                    } else {
                        answers.forEachIndexed { index, (value, ttl) ->
                            KeyValueRow(if (ttl.isBlank()) recordTypes[typeIndex] else ttl, value)
                            if (index != answers.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}
