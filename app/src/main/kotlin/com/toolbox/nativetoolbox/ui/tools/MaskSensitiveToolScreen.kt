package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold

private val phoneRe = Regex("(?<!\\d)1[3-9]\\d{9}(?!\\d)")
private val idCardRe = Regex("(?<!\\d)\\d{6}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx](?!\\d)")
private val emailRe = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
private val bankRe = Regex("(?<!\\d)\\d{16,19}(?!\\d)")
private val ipRe = Regex("(?<!\\d)((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?!\\d)")

private fun keepEnds(value: String, head: Int, tail: Int): String {
    if (value.length <= head + tail) return "*".repeat(value.length)
    return value.take(head) + "*".repeat(value.length - head - tail) + value.takeLast(tail)
}

private data class MaskResult(val text: String, val counts: Map<String, Int>)

private fun mask(
    input: String,
    phone: Boolean,
    idCard: Boolean,
    email: Boolean,
    bank: Boolean,
    ip: Boolean
): MaskResult {
    var out = input
    val counts = linkedMapOf<String, Int>()

    fun apply(name: String, re: Regex, transform: (String) -> String) {
        var hit = 0
        out = re.replace(out) { m -> hit++; transform(m.value) }
        if (hit > 0) counts[name] = hit
    }

    // 身份证与银行卡都是长数字串，先处理身份证避免被银行卡规则吃掉
    if (idCard) apply("身份证", idCardRe) { keepEnds(it, 6, 2) }
    if (bank) apply("银行卡", bankRe) { keepEnds(it, 4, 4) }
    if (phone) apply("手机号", phoneRe) { keepEnds(it, 3, 4) }
    if (email) apply("邮箱", emailRe) { raw ->
        val at = raw.indexOf('@')
        keepEnds(raw.take(at), 1, 1) + raw.substring(at)
    }
    if (ip) apply("IP", ipRe) { raw -> raw.split('.').let { "${it[0]}.${it[1]}.*.*" } }

    return MaskResult(out, counts)
}

@Composable
fun MaskSensitiveToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf(true) }
    var idCard by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf(true) }
    var bank by rememberSaveable { mutableStateOf(true) }
    var ip by rememberSaveable { mutableStateOf(false) }

    val result = mask(input, phone, idCard, email, bank, ip)
    val total = result.counts.values.sum()

    ToolScaffold {
        item { SectionHeader("原文") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴要发出去之前需要脱敏的内容"
                    )
                }
            }
        }
        item { SectionHeader("要打码的类型") }
        item {
            GroupedCard {
                ToggleRow("手机号", phone) { phone = it }
                ToggleRow("身份证号", idCard) { idCard = it }
                ToggleRow("邮箱", email) { email = it }
                ToggleRow("银行卡号", bank) { bank = it }
                ToggleRow("IP 地址", ip) { ip = it }
            }
        }
        item { SectionHeader(if (total > 0) "已打码 $total 处" else "打码结果") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(text = result.text)
                    if (result.counts.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            result.counts.entries.take(3).forEach { (name, count) ->
                                StatCell(name, count.toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
