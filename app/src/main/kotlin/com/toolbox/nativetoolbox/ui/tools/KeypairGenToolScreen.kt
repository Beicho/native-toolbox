package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest

private val kinds = listOf(
    Triple("RSA 2048", "RSA", 2048),
    Triple("RSA 4096", "RSA", 4096),
    Triple("EC P-256", "EC", 256),
    Triple("EC P-384", "EC", 384)
)

private fun pem(label: String, bytes: ByteArray): String {
    val body = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    val wrapped = body.chunked(64).joinToString("\n")
    return "-----BEGIN " + label + "-----\n" + wrapped + "\n-----END " + label + "-----"
}

private fun fingerprint(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString(":") { String.format("%02x", it) }
}

@Composable
fun KeypairGenToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var kindIndex by rememberSaveable { mutableStateOf(0) }
    var publicPem by rememberSaveable { mutableStateOf("") }
    var privatePem by rememberSaveable { mutableStateOf("") }
    var fp by rememberSaveable { mutableStateOf("") }
    var generating by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }

    fun generate() {
        generating = true
        error = ""
        publicPem = ""
        privatePem = ""
        fp = ""
        val (_, algorithm, size) = kinds[kindIndex]
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val generator = KeyPairGenerator.getInstance(algorithm)
                    generator.initialize(size)
                    generator.generateKeyPair()
                }
            }
            result.onSuccess { pair: KeyPair ->
                publicPem = pem("PUBLIC KEY", pair.public.encoded)
                privatePem = pem("PRIVATE KEY", pair.private.encoded)
                fp = fingerprint(pair.public.encoded)
            }.onFailure { e ->
                error = e.message ?: "生成失败，换个类型试试"
            }
            generating = false
        }
    }

    ToolScaffold {
        item { SectionHeader("密钥类型") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = kinds.map { it.first },
                        selectedIndex = kindIndex,
                        onSelected = { kindIndex = it }
                    )
                    Text(
                        when (kinds[kindIndex].second) {
                            "RSA" -> "兼容性最好，几乎所有系统都支持。4096 位更安全但生成和运算都慢一些。"
                            else -> "椭圆曲线密钥，短小且快，安全强度不输长 RSA。现代系统都支持。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                    SolidButton(onClick = { generate() }, enabled = !generating) {
                        Text(if (generating) "生成中，稍等…" else "生成密钥对")
                    }
                    if (error.isNotBlank()) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                    if (generating && kinds[kindIndex].third >= 4096) {
                        Text(
                            "4096 位可能要十几秒，别退出页面。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.orange
                        )
                    }
                }
            }
        }
        if (publicPem.isNotBlank()) {
            item { SectionHeader("公钥指纹") }
            item {
                GroupedCard {
                    KeyValueRow("类型", kinds[kindIndex].first, copyable = false)
                    RowDivider()
                    KeyValueRow("SHA-256", fp)
                }
            }
            item { SectionHeader("公钥（可以公开）") }
            item { GroupedCard { CardPadding { OutputCard(text = publicPem, label = "PUBLIC KEY") } } }
            item { SectionHeader("私钥（务必保密）") }
            item {
                GroupedCard {
                    CardPadding {
                        OutputCard(text = privatePem, label = "PRIVATE KEY")
                        Text(
                            "私钥泄露等于身份被冒用。不要发到聊天软件或截图，存进密码管理器里。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                }
            }
        }
    }
}
