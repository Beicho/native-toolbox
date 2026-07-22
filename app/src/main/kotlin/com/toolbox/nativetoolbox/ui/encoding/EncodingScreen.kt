package com.toolbox.nativetoolbox.ui.encoding

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.nativetoolbox.data.model.EncodingType
import com.toolbox.nativetoolbox.data.model.FileStatus
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.FileHelper

private val quickTargets = listOf(
    EncodingType.UTF8, EncodingType.GBK, EncodingType.GB18030, EncodingType.BIG5
)

@Composable
fun EncodingScreen(onBack: () -> Unit, viewModel: EncodingViewModel = viewModel()) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(context, uris)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val targetIndex = quickTargets.indexOf(uiState.targetEncoding).coerceAtLeast(0)

    ToolScaffold(title = "编码转换", onBack = onBack) {
        item { SectionHeader("目标编码") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = quickTargets.map { it.displayName },
                        selectedIndex = targetIndex,
                        onSelected = { viewModel.setTargetEncoding(quickTargets[it]) }
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        SolidButton(onClick = { picker.launch("text/*") }, filled = true) {
                            Text("添加文件", color = Color.White)
                        }
                        SolidButton(
                            onClick = { viewModel.convertFiles(context) },
                            filled = false,
                            enabled = uiState.files.isNotEmpty() && !uiState.isConverting
                        ) {
                            if (uiState.isConverting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(18.dp),
                                    strokeWidth = 2.dp,
                                    color = palette.accent
                                )
                            } else {
                                Text("开始转换", color = palette.accent)
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("文件列表 · ${uiState.files.size} 个(输出到 下载/综合工具包)") }
        item {
            GroupedCard {
                if (uiState.files.isEmpty()) {
                    Text(
                        "还没有文件,点上面「添加文件」选择 TXT",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.tertiaryLabel
                    )
                } else {
                    uiState.files.forEachIndexed { index, file ->
                        if (index > 0) RowDivider()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    file.name,
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.label,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(8.dp))
                                val (statusText, statusColor) = when (file.status) {
                                    FileStatus.PENDING -> "待转换" to palette.secondaryLabel
                                    FileStatus.CONVERTING -> "转换中…" to palette.orange
                                    FileStatus.SUCCESS -> "完成" to palette.green
                                    FileStatus.ERROR -> "失败" to palette.red
                                }
                                Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                            }
                            Text(
                                "${FileHelper.formatFileSize(file.size)} · 检测编码 ${file.detectedEncoding}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryLabel
                            )
                        }
                    }
                }
            }
        }

        if (uiState.files.isNotEmpty()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SolidButton(onClick = { viewModel.clearFiles() }, filled = false) {
                        Text("清空列表", color = palette.red)
                    }
                }
            }
        }
    }
}
