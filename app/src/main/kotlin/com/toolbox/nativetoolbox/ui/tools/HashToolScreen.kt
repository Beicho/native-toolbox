package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import java.security.MessageDigest

private fun hash(algorithm: String, input: String, upper: Boolean): String {
    if (input.isEmpty()) return ""
    val digest = MessageDigest.getInstance(algorithm).digest(input.toByteArray(Charsets.UTF_8))
    val hex = digest.joinToString("") { "%02x".format(it) }
    return if (upper) hex.uppercase() else hex
}

@Composable
fun HashToolScreen(onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var upper by rememberSaveable { mutableStateOf(false) }

    ToolScaffold(title = "哈希计算", onBack = onBack) {
        item { SectionHeader("输入") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "输入要计算哈希的文本…",
                        minHeight = 120.dp
                    )
                }
                ToggleRow("大写输出", upper, { upper = it })
            }
        }
        item { SectionHeader("结果(点击复制)") }
        item {
            GroupedCard {
                KeyValueRow("MD5", hash("MD5", input, upper))
                RowDivider()
                KeyValueRow("SHA-1", hash("SHA-1", input, upper))
                RowDivider()
                KeyValueRow("SHA-256", hash("SHA-256", input, upper))
                RowDivider()
                KeyValueRow("SHA-512", hash("SHA-512", input, upper))
            }
        }
    }
}
