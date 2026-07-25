package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.security.MessageDigest
import kotlin.math.ln
import kotlin.math.pow

/**
 * 确定性密码：主密码 + 站点名 + 序号 → 派生出固定密码。
 * 不存任何东西，同样的输入永远得到同样的结果，换手机也能算出来。
 * 用 SHA-256 多轮迭代增加暴力成本。
 */
private const val ROUNDS = 20000

private val charsetLower = "abcdefghijkmnopqrstuvwxyz"
private val charsetUpper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
private val charsetDigit = "23456789"
private val charsetSymbol = "!@#$%^&*-_=+?"

private fun derive(master: String, site: String, counter: Int, length: Int, useSymbol: Boolean): String {
    if (master.isEmpty() || site.isEmpty()) return ""
    val digest = MessageDigest.getInstance("SHA-256")
    var data = (master + "|" + site.lowercase().trim() + "|" + counter).toByteArray(Charsets.UTF_8)
    repeat(ROUNDS) { data = digest.digest(data) }

    val pool = charsetLower + charsetUpper + charsetDigit + (if (useSymbol) charsetSymbol else "")
    val builder = StringBuilder()
    var index = 0
    // 前四位强制覆盖各字符类，保证满足常见的密码复杂度要求
    builder.append(charsetLower[(data[index++].toInt() and 0xFF) % charsetLower.length])
    builder.append(charsetUpper[(data[index++].toInt() and 0xFF) % charsetUpper.length])
    builder.append(charsetDigit[(data[index++].toInt() and 0xFF) % charsetDigit.length])
    if (useSymbol) {
        builder.append(charsetSymbol[(data[index++].toInt() and 0xFF) % charsetSymbol.length])
    }
    while (builder.length < length) {
        if (index >= data.size) {
            data = digest.digest(data)
            index = 0
        }
        builder.append(pool[(data[index++].toInt() and 0xFF) % pool.length])
    }
    return builder.toString().take(length)
}

private fun entropyBits(length: Int, poolSize: Int): Double = length * (ln(poolSize.toDouble()) / ln(2.0))

private fun crackTime(bits: Double): String {
    // 假设每秒一百亿次离线爆破
    val seconds = 2.0.pow(bits) / 1e10
    return when {
        seconds < 60 -> "不到一分钟"
        seconds < 3600 -> String.format("%.0f 分钟", seconds / 60)
        seconds < 86400 -> String.format("%.0f 小时", seconds / 3600)
        seconds < 31536000 -> String.format("%.0f 天", seconds / 86400)
        seconds < 31536000.0 * 1000 -> String.format("%.0f 年", seconds / 31536000)
        else -> "上千年以上"
    }
}

private fun masterStrength(master: String): Pair<String, Int> {
    var score = 0
    if (master.length >= 8) score++
    if (master.length >= 12) score++
    if (master.length >= 16) score++
    if (master.any { it.isDigit() }) score++
    if (master.any { it.isUpperCase() } && master.any { it.isLowerCase() }) score++
    if (master.any { !it.isLetterOrDigit() }) score++
    val label = when {
        master.isEmpty() -> "未输入"
        score <= 2 -> "太弱"
        score <= 4 -> "一般"
        else -> "够强"
    }
    return label to score
}

@Composable
fun PasswordVaultToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var master by rememberSaveable { mutableStateOf("") }
    var site by rememberSaveable { mutableStateOf("") }
    var counter by rememberSaveable { mutableStateOf("1") }
    var lengthIndex by rememberSaveable { mutableStateOf(1) }
    var useSymbol by rememberSaveable { mutableStateOf(true) }
    var computed by rememberSaveable { mutableStateOf("") }
    var working by rememberSaveable { mutableStateOf(false) }

    val lengths = listOf(12, 16, 20, 24)
    val length = lengths[lengthIndex]
    val counterValue = counter.trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
    val (strengthLabel, strengthScore) = masterStrength(master)

    val poolSize = charsetLower.length + charsetUpper.length + charsetDigit.length +
        (if (useSymbol) charsetSymbol.length else 0)
    val bits = entropyBits(length, poolSize)

    fun compute() {
        working = true
        computed = derive(master, site, counterValue, length, useSymbol)
        working = false
    }

    ToolScaffold {
        item { SectionHeader("生成的密码") }
        item {
            GroupedCard {
                CardPadding {
                    if (computed.isBlank()) {
                        Text(
                            "填好主密码和站点名，点下面的按钮算出这个站点的密码。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    } else {
                        OutputCard(text = computed, label = site.trim() + " 的密码")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("长度", length.toString(), Modifier.weight(1f))
                            StatCell("熵", String.format("%.0f bit", bits), Modifier.weight(1f))
                            StatCell("破解耗时", crackTime(bits), Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item { SectionHeader("主密码") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = master,
                        onValueChange = {
                            master = it
                            computed = ""
                        },
                        placeholder = "只记这一个，别的都由它算出来"
                    )
                    Text(
                        "主密码强度：" + strengthLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            master.isEmpty() -> palette.tertiaryLabel
                            strengthScore <= 2 -> palette.red
                            strengthScore <= 4 -> palette.orange
                            else -> palette.green
                        }
                    )
                }
            }
        }
        item { SectionHeader("站点") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = site,
                        onValueChange = {
                            site = it
                            computed = ""
                        },
                        placeholder = "站点名或域名，例如 github.com"
                    )
                    IosTextField(
                        value = counter,
                        onValueChange = {
                            counter = it
                            computed = ""
                        },
                        placeholder = "第几次改密码（改密码时加 1）",
                        mono = true
                    )
                    SegmentedPicker(
                        options = lengths.map { it.toString() + " 位" },
                        selectedIndex = lengthIndex,
                        onSelected = {
                            lengthIndex = it
                            computed = ""
                        }
                    )
                    com.toolbox.nativetoolbox.ui.components.ToggleRow(
                        "包含符号",
                        useSymbol,
                        onCheckedChange = {
                            useSymbol = it
                            computed = ""
                        },
                        subtitle = "有些老网站不接受符号，那就关掉"
                    )
                    SolidButton(
                        onClick = { compute() },
                        enabled = master.isNotBlank() && site.isNotBlank() && !working
                    ) { Text(if (working) "计算中…" else "算出密码") }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "主密码是唯一的钥匙，忘了就全丢了，而且泄露等于所有站点一起泄露，请务必用一个又长又只有你知道的。\n\n" +
                            "站点名要每次写得一模一样（大小写和空格会被统一处理，但 github 和 github.com 是两个结果）。\n\n" +
                            "这个页面不保存任何内容，退出即清空。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
    }
}
