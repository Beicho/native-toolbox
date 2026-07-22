package com.toolbox.nativetoolbox.ui.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.liquid.LiquidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle

@Composable
fun NovelScreen(viewModel: NovelViewModel = viewModel()) {
    val palette = LocalIosPalette.current
    val uiState by viewModel.uiState.collectAsState()
    var keyword by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.groupedBackground)
            .imePadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Text(
                "小说下载",
                Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                style = MaterialTheme.typography.displayLarge,
                color = palette.label
            )
        }

        item { SectionHeader("搜索番茄小说 / 粘贴书籍链接") }
        item {
            GroupedCard {
                CardPadding {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IosTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            placeholder = "书名 / 作者 / 链接 / 书籍 ID",
                            modifier = Modifier.weight(1f)
                        )
                        LiquidButton(
                            onClick = {
                                val text = keyword.trim()
                                if (text.isEmpty()) return@LiquidButton
                                if (text.any { it.isDigit() } && (text.startsWith("http") || text.all { it.isDigit() })) {
                                    viewModel.parseNovelUrl(text)
                                } else {
                                    viewModel.searchNovels(text)
                                }
                            },
                            tint = palette.accent,
                            height = 40.dp
                        ) {
                            if (uiState.isSearching || uiState.isParsing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("搜索", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (uiState.searchResults.isNotEmpty() && uiState.currentNovel == null) {
            item { SectionHeader("搜索结果 · ${uiState.searchResults.size}") }
            item {
                GroupedCard {
                    uiState.searchResults.take(20).forEachIndexed { index, result ->
                        if (index > 0) RowDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    result.bookName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.label,
                                    maxLines = 1
                                )
                                Text(
                                    "${result.author}${if (result.category.isNotBlank()) " · ${result.category}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryLabel,
                                    maxLines = 1
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            LiquidButton(
                                onClick = { viewModel.parseNovelUrl(result.bookId) },
                                height = 34.dp
                            ) {
                                Text("解析", color = palette.accent, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        uiState.currentNovel?.let { novel ->
            item { SectionHeader("当前书籍") }
            item {
                GroupedCard {
                    CardPadding {
                        Text(novel.bookName, style = MaterialTheme.typography.headlineMedium, color = palette.label)
                        Text(
                            "作者 ${novel.author} · ${uiState.totalChapters} 章" +
                                if (novel.category.isNotBlank()) " · ${novel.category}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                        if (uiState.isDownloading) {
                            Column {
                                LinearProgressIndicator(
                                    progress = { uiState.downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = palette.accent,
                                    trackColor = palette.fill
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${uiState.downloadedChapters} / ${uiState.totalChapters} 章",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryLabel
                                )
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                            ) {
                                LiquidButton(onClick = { viewModel.downloadNovel(true) }, tint = palette.accent) {
                                    Text("高速下载", color = Color.White)
                                }
                                LiquidButton(onClick = { viewModel.downloadNovel(false) }) {
                                    Text("降速下载", color = palette.accent)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.logs.isNotEmpty()) {
            item { SectionHeader("日志") }
            item {
                GroupedCard {
                    Column(Modifier.padding(16.dp)) {
                        uiState.logs.takeLast(12).forEach { log ->
                            Text(
                                log,
                                style = MonoStyle,
                                color = when {
                                    log.contains("[错误]") -> palette.red
                                    log.contains("[警告]") -> palette.orange
                                    log.contains("[完成]") || log.contains("[成功]") -> palette.green
                                    else -> palette.secondaryLabel
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
