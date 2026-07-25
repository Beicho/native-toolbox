package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.rememberCopy
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Base32 解码（RFC 4648，TOTP 密钥的标准编码） */
private fun base32Decode(input: String): ByteArray? {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    val clean = input.uppercase().replace(" ", "").replace("-", "").trimEnd('=')
    if (clean.isEmpty() || clean.any { !alphabet.contains(it) }) return null
    var buffer = 0
    var bitsLeft = 0
    val out = ArrayList<Byte>()
    clean.forEach { c ->
        buffer = (buffer shl 5) or alphabet.indexOf(c)
        bitsLeft += 5
        if (bitsLeft >= 8) {
            out.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
            bitsLeft -= 8
        }
    }
    return out.toByteArray()
}

/** RFC 6238 TOTP */
private fun totp(secret: ByteArray, timeSeconds: Long, period: Int, digits: Int, algorithm: String): String? {
    return runCatching {
        val counter = timeSeconds / period
        val data = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            data[i] = (value and 0xFF).toByte()
            value = value shr 8
        }
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(secret, algorithm))
        val hash = mac.doFinal(data)
        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val mod = Math.pow(10.0, digits.toDouble()).toInt()
        (binary % mod).toString().padStart(digits, '0')
    }.getOrNull()
}

/** 解析 otpauth:// 链接，把参数填进表单 */
private class OtpUri(val secret: String, val issuer: String, val account: String, val digits: Int, val period: Int, val algorithm: String)

private fun parseOtpUri(text: String): OtpUri? {
    if (!text.startsWith("otpauth://totp/", ignoreCase = true)) return null
    val withoutScheme = text.removePrefix("otpauth://totp/")
    val label = withoutScheme.substringBefore('?')
    val query = withoutScheme.substringAfter('?', "")
    val params = query.split('&').mapNotNull {
        val idx = it.indexOf('=')
        if (idx <= 0) null else it.take(idx) to java.net.URLDecoder.decode(it.substring(idx + 1), "UTF-8")
    }.toMap()
    val secret = params["secret"] ?: return null
    val decodedLabel = java.net.URLDecoder.decode(label, "UTF-8")
    return OtpUri(
        secret = secret,
        issuer = params["issuer"] ?: decodedLabel.substringBefore(':', ""),
        account = decodedLabel.substringAfter(':', decodedLabel),
        digits = params["digits"]?.toIntOrNull() ?: 6,
        period = params["period"]?.toIntOrNull() ?: 30,
        algorithm = when (params["algorithm"]?.uppercase()) {
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }
    )
}

private val algorithms = listOf("SHA1" to "HmacSHA1", "SHA256" to "HmacSHA256", "SHA512" to "HmacSHA512")

@Composable
fun TotpToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()
    val copy = rememberCopy()

    var secretText by rememberSaveable { mutableStateOf("") }
    var digitsIndex by rememberSaveable { mutableStateOf(0) }
    var periodIndex by rememberSaveable { mutableStateOf(0) }
    var algoIndex by rememberSaveable { mutableStateOf(0) }
    var label by rememberSaveable { mutableStateOf("") }

    var nowSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }

    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                nowSeconds = System.currentTimeMillis() / 1000
                delay(500)
            }
        }
        onDispose { job.cancel() }
    }

    // 粘进来的是 otpauth 链接就自动拆参数
    val uri = parseOtpUri(secretText.trim())
    val effectiveSecret = uri?.secret ?: secretText
    val digits = uri?.digits ?: listOf(6, 8)[digitsIndex]
    val period = uri?.period ?: listOf(30, 60)[periodIndex]
    val algorithm = uri?.algorithm ?: algorithms[algoIndex].second
    val displayLabel = uri?.let {
        listOf(it.issuer, it.account).filter { part -> part.isNotBlank() }.joinToString(" · ")
    } ?: label

    val secretBytes = base32Decode(effectiveSecret)
    val code = secretBytes?.let { totp(it, nowSeconds, period, digits, algorithm) }
    val remaining = (period - (nowSeconds % period)).toInt()
    val nextCode = secretBytes?.let { totp(it, nowSeconds + period, period, digits, algorithm) }

    ToolScaffold {
        item { SectionHeader("动态验证码") }
        item {
            GroupedCard {
                CardPadding {
                    if (code != null) {
                        Text(
                            code.chunked(if (digits == 8) 4 else 3).joinToString(" "),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Light,
                            color = palette.accent
                        )
                        if (displayLabel.isNotBlank()) {
                            Text(displayLabel, style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("剩余有效", remaining.toString() + " 秒", Modifier.weight(1f))
                            StatCell("下一个", nextCode ?: "—", Modifier.weight(1f))
                        }
                        SolidButton(onClick = { copy(code) }) { Text("复制验证码") }
                        if (remaining <= 5) {
                            Text(
                                "快过期了，等下一个再用更稳妥。",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.orange
                            )
                        }
                    } else {
                        Text(
                            if (secretText.isBlank()) "填入密钥后这里会实时生成验证码"
                            else "密钥格式不对。TOTP 密钥是 Base32（只含 A-Z 和 2-7），也可以直接粘 otpauth:// 链接。",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (secretText.isBlank()) palette.tertiaryLabel else palette.red
                        )
                    }
                }
            }
        }
        item { SectionHeader("密钥") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = secretText,
                        onValueChange = { secretText = it },
                        placeholder = "Base32 密钥，或整条 otpauth:// 链接",
                        mono = true
                    )
                    if (uri == null) {
                        IosTextField(
                            value = label,
                            onValueChange = { label = it },
                            placeholder = "备注（可选，例如 GitHub）"
                        )
                    } else {
                        Text(
                            "已从链接里读出参数，下面的选项自动跟随链接。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.green
                        )
                    }
                }
            }
        }
        if (uri == null) {
            item { SectionHeader("参数") }
            item {
                GroupedCard {
                    CardPadding {
                        SegmentedPicker(
                            options = listOf("6 位", "8 位"),
                            selectedIndex = digitsIndex,
                            onSelected = { digitsIndex = it }
                        )
                        SegmentedPicker(
                            options = listOf("30 秒", "60 秒"),
                            selectedIndex = periodIndex,
                            onSelected = { periodIndex = it }
                        )
                        SegmentedPicker(
                            options = algorithms.map { it.first },
                            selectedIndex = algoIndex,
                            onSelected = { algoIndex = it }
                        )
                        Text(
                            "绝大多数网站用 6 位、30 秒、SHA1，不确定就别改。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
        item { SectionHeader("当前配置") }
        item {
            GroupedCard {
                KeyValueRow("位数", digits.toString(), copyable = false)
                RowDivider()
                KeyValueRow("周期", period.toString() + " 秒", copyable = false)
                RowDivider()
                KeyValueRow("算法", algorithm.removePrefix("Hmac"), copyable = false)
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "密钥只在本机内存里用于计算，不会保存也不会上传。关掉页面就没了，请自行保管好密钥。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
    }
}
