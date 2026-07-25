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
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private const val ZW_ZERO = '​'   // zero width space  -> bit 0
private const val ZW_ONE = '‌'    // zero width non-joiner -> bit 1
private const val ZW_SEP = '‍'    // zero width joiner -> 分隔

/** 把密信按 UTF-8 逐位编码成零宽字符，藏进载体文本首字之后 */
private fun hide(carrier: String, secret: String): String {
    if (secret.isEmpty()) return carrier
    val bits = secret.toByteArray(Charsets.UTF_8)
        .joinToString("") { byte ->
            (0..7).reversed().joinToString("") { i -> if ((byte.toInt() shr i) and 1 == 1) "1" else "0" }
        }
    val payload = buildString {
        append(ZW_SEP)
        bits.forEach { append(if (it == '1') ZW_ONE else ZW_ZERO) }
        append(ZW_SEP)
    }
    return if (carrier.isEmpty()) payload else carrier.first() + payload + carrier.drop(1)
}

private fun reveal(text: String): String {
    val bits = text.filter { it == ZW_ZERO || it == ZW_ONE }
        .map { if (it == ZW_ONE) '1' else '0' }
        .joinToString("")
    if (bits.length < 8) return ""
    val bytes = bits.chunked(8).filter { it.length == 8 }
        .map { it.toInt(2).toByte() }
        .toByteArray()
    return runCatching { String(bytes, Charsets.UTF_8) }.getOrDefault("")
}

@Composable
fun ZeroWidthToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var mode by rememberSaveable { mutableStateOf(0) }
    var carrier by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var encoded by rememberSaveable { mutableStateOf("") }

    ToolScaffold {
        item { SectionHeader("模式") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("藏字", "读取"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    Text(
                        if (mode == 0) "密信会变成看不见的字符，混进这段文字里。复制出去外观完全不变。"
                        else "把可能藏了字的整段文字粘进来，自动还原。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        if (mode == 0) {
            item { SectionHeader("载体文字") }
            item {
                GroupedCard {
                    CardPadding {
                        IosTextArea(
                            value = carrier,
                            onValueChange = { carrier = it },
                            placeholder = "随便一段看起来正常的话"
                        )
                    }
                }
            }
            item { SectionHeader("要藏的内容") }
            item {
                GroupedCard {
                    CardPadding {
                        IosTextField(
                            value = secret,
                            onValueChange = { secret = it },
                            placeholder = "支持中文"
                        )
                    }
                }
            }
            item { SectionHeader("结果") }
            item {
                GroupedCard {
                    CardPadding {
                        val out = if (carrier.isBlank() || secret.isBlank()) "" else hide(carrier, secret)
                        if (out.isBlank()) {
                            Text("载体和密信都填上就会出结果", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        } else {
                            OutputCard(text = out, label = "复制这段（外观和载体一致）")
                        }
                    }
                }
            }
        } else {
            item { SectionHeader("粘贴文字") }
            item {
                GroupedCard {
                    CardPadding {
                        IosTextArea(
                            value = encoded,
                            onValueChange = { encoded = it },
                            placeholder = "粘贴可能藏了内容的文字"
                        )
                    }
                }
            }
            item { SectionHeader("读取结果") }
            item {
                GroupedCard {
                    CardPadding {
                        val found = reveal(encoded)
                        if (encoded.isBlank()) {
                            Text("等待粘贴", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        } else if (found.isBlank()) {
                            Text("这段文字里没有藏内容", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        } else {
                            OutputCard(text = found, label = "藏的内容")
                        }
                    }
                }
            }
        }
    }
}
