package com.toolbox.nativetoolbox.ui.novel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.nativetoolbox.data.model.SearchResult
import com.toolbox.nativetoolbox.ui.components.*
import com.toolbox.nativetoolbox.ui.theme.*

@Composable
fun NovelScreen(
    viewModel: NovelViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AstroSpacing.md)
    ) {
        // 自定义标签页
        AstroCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(AstroSpacing.xxs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xxs)
            ) {
                AstroTabButton(
                    text = "URL 下载",
                    icon = Icons.Default.Link,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                AstroTabButton(
                    text = "搜索",
                    icon = Icons.Default.Search,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(AstroSpacing.md))

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300, easing = AstroEasing.emphasized)) +
                    slideInHorizontally(
                        animationSpec = tween(300, easing = AstroEasing.emphasized),
                        initialOffsetX = { if (targetState > initialState) it else -it }
                    ) togetherWith
                    fadeOut(animationSpec = tween(300, easing = AstroEasing.emphasized)) +
                    slideOutHorizontally(
                        animationSpec = tween(300, easing = AstroEasing.emphasized),
                        targetOffsetX = { if (targetState > initialState) -it else it }
                    )
            },
            label = "tab_content"
        ) { tab ->
            when (tab) {
                0 -> UrlDownloadTab(viewModel, uiState)
                1 -> SearchTab(viewModel, uiState, searchQuery) { searchQuery = it }
            }
        }
    }
}

@Composable
fun AstroTabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun UrlDownloadTab(viewModel: NovelViewModel, uiState: NovelUiState) {
    var urlInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // URL 输入卡片
        AstroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "📚 输入小说链接",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AstroSpacing.sm))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("番茄小说链接或书籍ID") },
                placeholder = { Text("例如：7441596182398218532") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(AstroSpacing.xs))

            Button(
                onClick = { viewModel.parseNovelUrl(urlInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = urlInput.isNotBlank() && !uiState.isParsing
            ) {
                AnimatedContent(
                    targetState = uiState.isParsing,
                    label = "parse_button"
                ) { isParsing ->
                    if (isParsing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text("解析中...")
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Text("解析小说")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AstroSpacing.md))

        // 书籍信息卡片
        AnimatedVisibility(
            visible = uiState.currentNovel != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            uiState.currentNovel?.let { novel ->
                Column {
                    AstroCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = novel.bookName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(AstroSpacing.xs))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = novel.author,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                uiState.chapters?.let { chapters ->
                                    Spacer(modifier = Modifier.height(AstroSpacing.xxs))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${chapters.size} 章",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AstroSpacing.xs))

                    // 下载按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
                    ) {
                        Button(
                            onClick = { viewModel.downloadNovel(isHighSpeed = true) },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isDownloading && uiState.chapters != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(AstroSpacing.xxs))
                            Text("高速")
                        }

                        OutlinedButton(
                            onClick = { viewModel.downloadNovel(isHighSpeed = false) },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isDownloading && uiState.chapters != null
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null)
                            Spacer(modifier = Modifier.width(AstroSpacing.xxs))
                            Text("降速")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AstroSpacing.md))

        // 下载进度
        AnimatedVisibility(
            visible = uiState.isDownloading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                AstroCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "下载进度",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(AstroSpacing.xxs))
                            Text(
                                text = "${uiState.downloadedChapters}/${uiState.totalChapters} 章",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Text(
                            text = "${(uiState.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(AstroSpacing.sm))

                    AstroLinearProgressBar(
                        progress = uiState.downloadProgress
                    )
                }

                Spacer(modifier = Modifier.height(AstroSpacing.md))
            }
        }

        // 下载日志
        AnimatedVisibility(
            visible = uiState.logs.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📝 下载日志",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(AstroSpacing.xs))

                val listState = rememberLazyListState()

                LaunchedEffect(uiState.logs.size) {
                    if (uiState.logs.isNotEmpty()) {
                        listState.animateScrollToItem(uiState.logs.size - 1)
                    }
                }

                AstroCard(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AstroSpacing.xxs)
                    ) {
                        items(uiState.logs) { log ->
                            Text(
                                text = "→ $log",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTab(
    viewModel: NovelViewModel,
    uiState: NovelUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索框
        AstroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "🔍 搜索小说",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AstroSpacing.sm))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("搜索") },
                placeholder = { Text("输入书名或作者...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.searchNovels(searchQuery) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "搜索")
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(AstroSpacing.md))

        // 搜索结果
        AnimatedContent(
            targetState = when {
                uiState.isSearching -> "loading"
                uiState.searchResults.isEmpty() && searchQuery.isNotBlank() -> "empty"
                uiState.searchResults.isNotEmpty() -> "results"
                else -> "initial"
            },
            transitionSpec = {
                fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
            },
            label = "search_state"
        ) { state ->
            when (state) {
                "loading" -> {
                    AstroLoadingState(
                        message = "搜索中..."
                    )
                }
                "empty" -> {
                    AstroEmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "未找到结果",
                        description = "试试其他关键词吧"
                    )
                }
                "results" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
                    ) {
                        itemsIndexed(
                            items = uiState.searchResults,
                            key = { _, result -> result.bookId }
                        ) { index, result ->
                            SearchResultCard(
                                result = result,
                                viewModel = viewModel,
                                index = index
                            )
                        }
                    }
                }
                else -> {
                    AstroEmptyState(
                        icon = Icons.Default.TravelExplore,
                        title = "开始探索",
                        description = "输入书名或作者开始搜索"
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(
    result: SearchResult,
    viewModel: NovelViewModel,
    index: Int
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 30L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        AstroCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.bookName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(AstroSpacing.xxs))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = result.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (result.category.isNotBlank()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusChip(
                                text = result.category,
                                type = StatusType.INFO
                            )
                        }
                    }
                </Column>

                Spacer(modifier = Modifier.width(AstroSpacing.sm))

                FilledIconButton(
                    onClick = { viewModel.parseNovelUrl(result.bookId) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "下载"
                    )
                }
            }
        }
    }
}
