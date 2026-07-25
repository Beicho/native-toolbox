package com.toolbox.nativetoolbox.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.data.predict.PredictEngine
import com.toolbox.nativetoolbox.data.store.AstroStore
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 预测与数据设置页。
 *
 * 这一页本身是产品叙事的一部分:
 * 「它好像监听了我,但它什么都没上传」—— 这个反差要在这里说清楚,
 * 而且要给足控制权(开关、清空、导出)。透明是信任的前提。
 */
@Composable
fun PredictSettingsScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var predictOn by remember { mutableStateOf(PredictEngine.enabled) }
    var clipOn by remember { mutableStateOf(PredictEngine.clipboardSniffing) }
    var confirmClear by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val (eventCount, ageDays) = remember(status) { PredictEngine.learningProgress() }
    val mutedCount = remember(status) { PredictEngine.mutedRoutes().size }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) { status = "文件读不出来"; return@launch }
            AstroStore.importAll(text)
                .onSuccess { status = "导入成功,合并了 $it 条记录" }
                .onFailure { status = "导入失败:${it.message}" }
        }
    }

    ToolScaffold {
        item { SectionHeader("此刻推荐") }
        item {
            GroupedCard {
                ToggleRow("打开预测", predictOn, onCheckedChange = {
                    predictOn = it
                    PredictEngine.enabled = it
                })
                RowDivider()
                ToggleRow("看一眼剪贴板类型", clipOn, onCheckedChange = {
                    clipOn = it
                    PredictEngine.clipboardSniffing = it
                })
                CardPadding {
                    Text(
                        "剪贴板只判断「是网址、还是英文、还是一串数字」这种形状," +
                            "不读内容、不存内容、不发出去。刚复制一段英文时把翻译放到第一个,靠的就是这个。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }

        item { SectionHeader("学到了什么") }
        item {
            GroupedCard {
                KeyValueRow("记录的使用次数", "$eventCount 次", copyable = false)
                RowDivider()
                KeyValueRow("开始学习", if (ageDays <= 0) "今天" else "$ageDays 天前", copyable = false)
                RowDivider()
                KeyValueRow("你拒绝过的推荐", "$mutedCount 个", copyable = false)
                CardPadding {
                    Text(
                        when {
                            eventCount < 8 -> "还在观察阶段。用够 8 次之后,主页顶部会开始出现「此刻」。"
                            ageDays < 30 -> "还在学你的习惯,${30 - ageDays} 天后会完全按你的规律来。"
                            else -> "已经比较懂你了。推荐不准时长按可以让它别再出现。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }

        item { SectionHeader("这些数据在哪") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "全部在你手机里,一个字节都不上传。",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.label
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "· 用过哪个工具、什么时候用的 —— 存在本地文件里\n" +
                            "· WiFi 只存名字的哈希值,认得出是不是同一个网,但还原不出叫什么\n" +
                            "· 剪贴板只判形状,判完就丢\n" +
                            "· 完全不碰:位置、通讯录、通话记录、别的 App 的数据\n" +
                            "· 没有账号、没有服务器同步、没有任何统计上报",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                    Spacer(Modifier.height(12.dp))
                    SolidButton(
                        onClick = { confirmClear = true },
                        Modifier.fillMaxWidth(),
                        filled = false
                    ) { Text("清空学习记录") }
                }
            }
        }

        item { SectionHeader("备份你的数据") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "记账、倒数日、待办、笔记这些都能导出成一个文件。换手机、重装前记得导一份。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                    Spacer(Modifier.height(12.dp))
                    SolidButton(
                        onClick = {
                            scope.launch {
                                val json = withContext(Dispatchers.Default) { AstroStore.exportAll() }
                                val name = "AstroKit备份_${System.currentTimeMillis()}.json"
                                val r = withContext(Dispatchers.IO) {
                                    FileHelper.saveToDownloads(context, name, json.toByteArray())
                                }
                                status = r.fold({ "已导出到 $it" }, { "导出失败:${it.message}" })
                            }
                        },
                        Modifier.fillMaxWidth()
                    ) { Text("导出全部数据") }
                    Spacer(Modifier.height(8.dp))
                    SolidButton(
                        onClick = { importPicker.launch("application/json") },
                        Modifier.fillMaxWidth(),
                        filled = false
                    ) { Text("从备份文件恢复") }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (status.contains("失败")) palette.red else palette.green
                        )
                    }
                }
            }
        }

        item { SectionHeader("各类数据条数") }
        item {
            GroupedCard {
                val entries = AstroStore.Collection.entries.filter { AstroStore.count(it) > 0 }
                if (entries.isEmpty()) {
                    CardPadding {
                        Text(
                            "还没有数据。用用记账、倒数日这些就会有了。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                } else {
                    entries.forEachIndexed { i, c ->
                        KeyValueRow(c.displayName, "${AstroStore.count(c)} 条", copyable = false)
                        if (i != entries.lastIndex) RowDivider()
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = palette.cardBackground,
            title = { Text("清空学习记录?", color = palette.label) },
            text = {
                Text(
                    "「此刻」会回到刚装好的状态,重新开始观察你的习惯。\n\n" +
                        "你的记账、倒数日这些数据不受影响。",
                    color = palette.secondaryLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    PredictEngine.clearLearningData()
                    confirmClear = false
                    status = "已清空,重新开始学习"
                }) { Text("清空", color = palette.red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("算了", color = palette.secondaryLabel)
                }
            }
        )
    }
}
