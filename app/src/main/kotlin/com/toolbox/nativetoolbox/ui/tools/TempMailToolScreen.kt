package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.rememberCopy
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private class MailItem(val id: String, val from: String, val subject: String, val date: String)

private fun parseList(arr: JSONArray?): List<MailItem> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).mapNotNull { i ->
        val obj = arr.optJSONObject(i) ?: return@mapNotNull null
        MailItem(
            id = obj.optString("id"),
            from = listOf("from", "sender", "fromAddress").map { obj.optString(it) }
                .firstOrNull { it.isNotBlank() && it != "null" } ?: "未知发件人",
            subject = obj.optString("subject").ifBlank { "（无主题）" },
            date = listOf("date", "receivedAt", "createdAt").map { obj.optString(it) }
                .firstOrNull { it.isNotBlank() && it != "null" } ?: ""
        )
    }
}

@Composable
fun TempMailToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val copy = rememberCopy()

    var boxId by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var expiresAt by rememberSaveable { mutableStateOf("") }
    var listRaw by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var autoRefresh by rememberSaveable { mutableStateOf(true) }

    fun createBox() {
        loading = true
        status = ""
        detail = ""
        listRaw = ""
        scope.launch {
            AstroApi.post("/mail/new", JSONObject())
                .onSuccess { res ->
                    boxId = res.data.optString("id")
                    address = res.data.optString("address")
                    expiresAt = res.data.optString("expiresAt")
                    status = if (address.isBlank()) "服务没返回地址，稍后再试" else ""
                }
                .onFailure { e -> status = e.message ?: "创建失败，请检查网络" }
            loading = false
        }
    }

    fun refresh(silent: Boolean = false) {
        if (boxId.isBlank()) return
        if (!silent) loading = true
        scope.launch {
            AstroApi.get("/mail/list", mapOf("id" to boxId))
                .onSuccess { res -> listRaw = res.data.toString() }
                .onFailure { e -> if (!silent) status = e.message ?: "刷新失败" }
            if (!silent) loading = false
        }
    }

    fun openMail(id: String) {
        loading = true
        scope.launch {
            AstroApi.get("/mail/detail", mapOf("id" to boxId, "eid" to id))
                .onSuccess { res ->
                    val obj = res.data
                    val body = listOf("text", "body", "textBody", "html")
                        .map { obj.optString(it) }
                        .firstOrNull { it.isNotBlank() && it != "null" } ?: "（这封邮件没有正文）"
                    detail = "发件人：" + obj.optString("from") + "\n主题：" + obj.optString("subject") + "\n\n" + body
                }
                .onFailure { e -> status = e.message ?: "读取失败" }
            loading = false
        }
    }

    fun dropBox() {
        if (boxId.isBlank()) return
        val id = boxId
        boxId = ""
        address = ""
        listRaw = ""
        detail = ""
        status = "邮箱已销毁"
        scope.launch { AstroApi.get("/mail/drop", mapOf("id" to id)) }
    }

    // 有邮箱且开了自动刷新时，每 10 秒静默拉一次收件箱
    DisposableEffect(boxId, autoRefresh) {
        val job = scope.launch {
            while (isActive && boxId.isNotBlank() && autoRefresh) {
                delay(10_000)
                refresh(silent = true)
            }
        }
        onDispose { job.cancel() }
    }

    val mails = runCatching { parseList(JSONObject(listRaw.ifBlank { "{}" }).optJSONArray("emails")) }
        .getOrElse { emptyList() }
        .ifEmpty {
            runCatching { parseList(JSONArray(listRaw.ifBlank { "[]" })) }.getOrElse { emptyList() }
        }

    ToolScaffold {
        item { SectionHeader("临时邮箱") }
        item {
            GroupedCard {
                CardPadding {
                    if (address.isBlank()) {
                        Text(
                            "生成一个临时邮箱地址，用来接收验证码，30 分钟后自动失效。需要联网。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                        SolidButton(onClick = { createBox() }, enabled = !loading) {
                            Text(if (loading) "创建中…" else "生成邮箱地址")
                        }
                    } else {
                        OutputCard(text = address, label = "点右上角复制地址")
                        if (expiresAt.isNotBlank()) {
                            Text(
                                "有效期至 " + expiresAt,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SolidButton(
                                onClick = { refresh() },
                                modifier = Modifier.weight(1f),
                                enabled = !loading
                            ) { Text(if (loading) "刷新中…" else "刷新收件箱") }
                            SolidButton(
                                onClick = { autoRefresh = !autoRefresh },
                                modifier = Modifier.weight(1f),
                                filled = false
                            ) { Text(if (autoRefresh) "自动刷新：开" else "自动刷新：关") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SolidButton(
                                onClick = { copy(address) },
                                modifier = Modifier.weight(1f),
                                filled = false
                            ) { Text("复制地址") }
                            SolidButton(
                                onClick = { createBox() },
                                modifier = Modifier.weight(1f),
                                filled = false
                            ) { Text("换一个") }
                            SolidButton(
                                onClick = { dropBox() },
                                modifier = Modifier.weight(1f),
                                filled = false
                            ) { Text("销毁") }
                        }
                    }
                    if (status.isNotBlank()) {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                }
            }
        }
        if (address.isNotBlank()) {
            item { SectionHeader(if (mails.isEmpty()) "收件箱" else "收到 " + mails.size + " 封") }
            item {
                GroupedCard {
                    if (mails.isEmpty()) {
                        CardPadding {
                            Text(
                                "还没有邮件。发信到上面的地址，这里会自动出现。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel
                            )
                        }
                    } else {
                        mails.forEachIndexed { index, mail ->
                            com.toolbox.nativetoolbox.ui.components.NavRow(
                                title = mail.subject,
                                value = mail.from,
                                onClick = { openMail(mail.id) }
                            )
                            if (index != mails.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
        if (detail.isNotBlank()) {
            item { SectionHeader("邮件内容") }
            item { GroupedCard { CardPadding { OutputCard(text = detail, label = "正文") } } }
        }
    }
}
