package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 加密胶囊：AES-256-GCM + PBKDF2-HMAC-SHA256（12 万次迭代）。
 * 输出格式 astro1.<base64(salt|iv|cipher)>，salt 16 字节、iv 12 字节。
 */
private const val PREFIX = "astro1."
private const val ITERATIONS = 120_000
private const val SALT_LEN = 16
private const val IV_LEN = 12
private const val TAG_BITS = 128

private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
    return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
}

private fun encrypt(plain: String, password: String): Result<String> = runCatching {
    val random = SecureRandom()
    val salt = ByteArray(SALT_LEN).also { random.nextBytes(it) }
    val iv = ByteArray(IV_LEN).also { random.nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password.toCharArray(), salt), GCMParameterSpec(TAG_BITS, iv))
    val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
    val packed = salt + iv + body
    PREFIX + android.util.Base64.encodeToString(packed, android.util.Base64.NO_WRAP)
}

private fun decrypt(capsule: String, password: String): Result<String> = runCatching {
    val trimmed = capsule.trim()
    require(trimmed.startsWith(PREFIX)) { "这不是加密胶囊，密文应该以 astro1. 开头" }
    val packed = android.util.Base64.decode(trimmed.removePrefix(PREFIX), android.util.Base64.DEFAULT)
    require(packed.size > SALT_LEN + IV_LEN) { "密文不完整" }
    val salt = packed.copyOfRange(0, SALT_LEN)
    val iv = packed.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
    val body = packed.copyOfRange(SALT_LEN + IV_LEN, packed.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, deriveKey(password.toCharArray(), salt), GCMParameterSpec(TAG_BITS, iv))
    String(cipher.doFinal(body), Charsets.UTF_8)
}

@Composable
fun EncryptCapsuleToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var mode by rememberSaveable { mutableStateOf(0) }
    var content by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var working by rememberSaveable { mutableStateOf(false) }

    fun run() {
        error = ""
        output = ""
        if (content.isBlank()) {
            error = if (mode == 0) "先输入要加密的内容" else "先粘贴密文"
            return
        }
        if (password.length < 4) {
            error = "密码至少 4 位，建议 8 位以上"
            return
        }
        working = true
        val result = if (mode == 0) encrypt(content, password) else decrypt(content, password)
        working = false
        result.onSuccess { output = it }.onFailure { e ->
            error = when {
                mode == 1 && e is javax.crypto.AEADBadTagException -> "密码不对，或者密文被改动过"
                e.message != null -> e.message!!
                else -> "处理失败"
            }
        }
    }

    ToolScaffold {
        item { SectionHeader("模式") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("加密", "解密"),
                        selectedIndex = mode,
                        onSelected = {
                            mode = it
                            output = ""
                            error = ""
                        }
                    )
                    Text(
                        if (mode == 0) "把一段文字用密码锁起来，只有知道密码的人能打开。"
                        else "输入密文和密码，还原出原文。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader(if (mode == 0) "要加密的内容" else "密文") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = if (mode == 0) "输入要保护的文字" else "粘贴以 astro1. 开头的密文",
                        mono = mode == 1
                    )
                }
            }
        }
        item { SectionHeader("密码") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "两边必须用同一个密码"
                    )
                    SolidButton(onClick = { run() }, enabled = !working) {
                        Text(if (working) "处理中…" else if (mode == 0) "加密" else "解密")
                    }
                    if (error.isNotBlank()) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = palette.red)
                    }
                }
            }
        }
        if (output.isNotBlank()) {
            item { SectionHeader(if (mode == 0) "密文（复制发给对方）" else "原文") }
            item { GroupedCard { CardPadding { OutputCard(text = output) } } }
        }
        item { SectionHeader("怎么保证安全") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "用 AES-256-GCM 加密，密码经过 12 万次派生才变成密钥，暴力破解成本很高。" +
                            "加解密全在手机本地完成，内容和密码都不会离开设备。\n\n" +
                            "密码没有找回途径，忘了就真的打不开了。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
    }
}
