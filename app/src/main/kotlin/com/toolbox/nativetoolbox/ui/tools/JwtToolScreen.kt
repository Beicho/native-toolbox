package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.util.ShareBus
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

private val jwtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private fun decodePart(part: String): String {
    val bytes = Base64.getUrlDecoder().decode(part.padEnd((part.length + 3) / 4 * 4, '='))
    val json = String(bytes, Charsets.UTF_8)
    return runCatching { JSONObject(json).toString(2) }.getOrDefault(json)
}

private fun claimTime(payload: String, key: String): String {
    return runCatching {
        val ts = JSONObject(payload).optLong(key, -1)
        if (ts <= 0) "" else LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneId.systemDefault()).format(jwtFmt)
    }.getOrDefault("")
}

@Composable
fun JwtToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf(ShareBus.consume() ?: "") }

    val parts = input.trim().split(".")
    val header = if (parts.size >= 2) runCatching { decodePart(parts[0]) }.getOrNull() else null
    val payload = if (parts.size >= 2) runCatching { decodePart(parts[1]) }.getOrNull() else null
    val rawPayload = if (parts.size >= 2) runCatching {
        String(Base64.getUrlDecoder().decode(parts[1].padEnd((parts[1].length + 3) / 4 * 4, '=')), Charsets.UTF_8)
    }.getOrNull() else null

    ToolScaffold {
        item { SectionHeader("JWT Token") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴 xxx.yyy.zzz 格式的 JWT…",
                        minHeight = 110.dp,
                        mono = true
                    )
                }
            }
        }
        item { SectionHeader("Header") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(text = header ?: "", label = "算法与类型")
                }
            }
        }
        item { SectionHeader("Payload") }
        item {
            GroupedCard {
                CardPadding {
                    OutputCard(text = payload ?: "", label = "载荷")
                }
                if (rawPayload != null) {
                    KeyValueRow("签发 iat", claimTime(rawPayload, "iat"))
                    RowDivider()
                    KeyValueRow("过期 exp", claimTime(rawPayload, "exp"))
                    RowDivider()
                    KeyValueRow("生效 nbf", claimTime(rawPayload, "nbf"))
                }
            }
        }
    }
}
