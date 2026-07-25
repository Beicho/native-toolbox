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

// 键名与后端 timing 对象一致
private val timingLabels = listOf(
    "dns" to "域名解析",
    "connect" to "建立连接",
    "http" to "服务器响应",
    "total" to "总耗时"
)

// 键名与后端 security.detail 一致,值是该项的得分(0 = 未设置)
private val headerLabels = listOf(
    "hsts" to "强制 HTTPS",
    "csp" to "内容安全策略",
    "xcto" to "禁止类型嗅探",
    "xfo" to "防嵌套点击劫持",
    "referrer" to "来源信息控制"
)

private fun ms(value: Int): String = if (value <= 0) "—" else value.toString() + " ms"

@Composable
fun SiteCheckToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var url by rememberSaveable { mutableStateOf("") }
    var raw by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun run() {
        var target = url.trim()
        if (target.isBlank()) {
            status = "先输入网址"
            raw = null
            return
        }
        if (!target.startsWith("http://") && !target.startsWith("https://")) target = "https://" + target
        loading = true
        status = ""
        scope.launch {
            AstroApi.get("/sitecheck", mapOf("url" to target))
                .onSuccess { res ->
                    raw = res.data.toString()
                    status = cachedHint(res.cachedAt)
                }
                .onFailure { e ->
                    raw = null
                    status = e.message ?: "检测失败，请检查网络或网址"
                }
            loading = false
        }
    }

    val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
    val httpCode = json?.optInt("statusCode", 0) ?: 0
    val score = json?.optJSONObject("security")?.optInt("score", -1) ?: -1
    val timings = json?.optJSONObject("timing")
    val security = json?.optJSONObject("security")
    val headers = security?.optJSONObject("detail")

    ToolScaffold {
        item { SectionHeader("网站体检") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "输入网址，例如 example.com",
                        mono = true
                    )
                    SolidButton(onClick = { run() }, enabled = !loading) {
                        Text(if (loading) "检测中…" else "开始体检")
                    }
                    Text(
                        "会实际访问一次目标网站，测量各阶段耗时并检查安全响应头，需要联网。",
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
            item { SectionHeader("总体") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("状态码", if (httpCode > 0) httpCode.toString() else "—", Modifier.weight(1f))
                            StatCell("安全评分", if (score >= 0) score.toString() else "—", Modifier.weight(1f))
                            StatCell(
                                "总耗时",
                                ms(timings?.optInt("total", 0) ?: 0),
                                Modifier.weight(1f)
                            )
                        }
                        json.optString("ip").takeIf { it.isNotBlank() }?.let {
                            Text("服务器 IP:$it", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        }
                        if (httpCode in 200..299) {
                            Text("网站可以正常访问。", style = MaterialTheme.typography.bodySmall, color = palette.green)
                        } else if (httpCode > 0) {
                            Text(
                                "返回了 " + httpCode + "，不是正常成功状态。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.orange
                            )
                        }
                    }
                }
            }
            if (timings != null) {
                item { SectionHeader("各阶段耗时") }
                item {
                    GroupedCard {
                        timingLabels.forEachIndexed { index, (key, label) ->
                            KeyValueRow(label, ms(timings.optInt(key, 0)), copyable = false)
                            if (index != timingLabels.lastIndex) RowDivider()
                        }
                    }
                }
            }
            item { SectionHeader("安全响应头") }
            item {
                GroupedCard {
                    headerLabels.forEachIndexed { index, (key, label) ->
                        // 后端返回的是该项得分,0 表示没设置
                        val points = headers?.optInt(key, 0) ?: 0
                        KeyValueRow(
                            label,
                            if (points > 0) "已启用" else "未设置",
                            copyable = false
                        )
                        if (index != headerLabels.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
