package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PinyinUtil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinyinToolScreen(onBack: () -> Unit) {
    com.toolbox.nativetoolbox.ui.components.AssetGate(
        com.toolbox.nativetoolbox.util.AssetProvisioner.Asset.PINYIN
    ) { PinyinContent() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinyinContent() {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var nameMode by rememberSaveable { mutableStateOf(false) }
    var style by rememberSaveable { mutableStateOf(0) } // 0 带调 1 不带调 2 首字母
    var annos by remember { mutableStateOf<List<PinyinUtil.Anno>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 表在 IO 线程加载,2 万多字瞬间完成
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            PinyinUtil.ensureLoaded(context)
        }
        loaded = true
    }

    fun styled(py: String): String = when (style) {
        1 -> PinyinUtil.stripTone(py)
        2 -> PinyinUtil.stripTone(py).take(1)
        else -> py
    }

    ToolScaffold {
        item {
            if (annos.isNotEmpty()) {
                GroupedCard {
                    CardPadding {
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            annos.forEach { a ->
                                if (a.pinyins == null) {
                                    // 非汉字:原样排(标点、英文)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(" ", fontSize = 11.sp, color = palette.tertiaryLabel)
                                        Text(a.char.toString(), fontSize = 20.sp, color = palette.label)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            styled(a.pinyins.first()),
                                            fontSize = 11.sp,
                                            color = palette.accent
                                        )
                                        Text(a.char.toString(), fontSize = 20.sp, color = palette.label)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            if (annos.isNotEmpty()) {
                val flat = annos.joinToString(" ") { a -> a.pinyins?.let { styled(it.first()) } ?: a.char.toString() }
                OutputCard(flat, Modifier, label = "拼音串(可复制)")
            }
        }
        item {
            val poly = annos.filter { (it.pinyins?.size ?: 0) > 1 }.distinctBy { it.char }
            if (poly.isNotEmpty()) {
                GroupedCard {
                    CardPadding {
                        Text("多音字", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = palette.label)
                        Spacer(Modifier.height(6.dp))
                        poly.take(12).forEach { a ->
                            Text(
                                "${a.char}:${a.pinyins!!.joinToString(" / ")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryLabel,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(input, { input = it }, Modifier.fillMaxWidth(), placeholder = "输入汉字,比如你的名字", minHeight = 100.dp)
                    Spacer(Modifier.height(10.dp))
                    SegmentedPicker(listOf("带声调", "不带调", "首字母"), style, { style = it }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    SolidButton(
                        onClick = { annos = PinyinUtil.annotate(input, nameMode) },
                        Modifier.fillMaxWidth(),
                        enabled = loaded && input.isNotBlank()
                    ) { Text(if (loaded) "注音" else "字库加载中…") }
                }
            }
        }
        item {
            GroupedCard {
                ToggleRow("姓名模式(姓氏用姓的读法,如「单」读 shàn)", nameMode, onCheckedChange = { nameMode = it })
            }
        }
    }
}
