package com.toolbox.nativetoolbox.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.data.HomeCardData
import com.toolbox.nativetoolbox.data.prefs.UsageStore
import com.toolbox.nativetoolbox.ui.components.IconTile
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** 首页:动态卡片 + 搜索 + 常用置顶 + 分类工具宫格 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(usageStore: UsageStore, onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val categories = remember { toolCategories() }
    val allTools = remember { categories.flatMap { it.tools } }
    var query by remember { mutableStateOf("") }
    val usage by usageStore.usageCounts.collectAsState(initial = emptyMap())

    // 动态卡片数据
    var countdowns by remember { mutableStateOf<List<HomeCardData.CountdownItem>>(emptyList()) }
    var todos by remember { mutableStateOf<List<HomeCardData.TodoItem>>(emptyList()) }
    var onThisDay by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        countdowns = HomeCardData.getUpcomingCountdowns(context)
        todos = HomeCardData.getTodos(context)
        onThisDay = HomeCardData.getOnThisDay(context)
    }

    val frequent = remember(usage) {
        usage.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(4)
            .mapNotNull { entry -> allTools.find { it.route == entry.key } }
    }

    val filtered = if (query.isBlank()) null else allTools.filter {
        it.title.contains(query.trim(), ignoreCase = true) ||
            it.subtitle.contains(query.trim(), ignoreCase = true)
    }

    val liveCards = buildList {
        if (countdowns.isNotEmpty()) add("countdown")
        if (todos.isNotEmpty()) add("todo")
        if (onThisDay != null) add("onthisday")
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.groupedBackground),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp)
            ) {
                Text(
                    "Astro Kit",
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.label
                )
                Text(
                    "星辰之匣 · ${allTools.size} 个工具",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryLabel
                )
                Spacer(Modifier.height(12.dp))
                IosTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "搜索工具…"
                )
            }
        }

        // 动态卡片区
        if (liveCards.isNotEmpty() && query.isBlank()) {
            item {
                Spacer(Modifier.height(20.dp))
                val pagerState = rememberPagerState(pageCount = { liveCards.size })
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    when (liveCards[page]) {
                        "countdown" -> CountdownCard(countdowns.first(), onOpenTool)
                        "todo" -> TodoCard(todos, onOpenTool)
                        "onthisday" -> OnThisDayCard(onThisDay!!, onOpenTool)
                    }
                }
                if (liveCards.size > 1) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(liveCards.size) { i ->
                            Box(
                                Modifier
                                    .size(if (pagerState.currentPage == i) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(if (pagerState.currentPage == i) palette.label else palette.tertiaryLabel.copy(alpha = 0.3f))
                            )
                            if (i != liveCards.lastIndex) Spacer(Modifier.width(6.dp))
                        }
                    }
                }
            }
        }

        if (filtered != null) {
            item {
                Text(
                    if (filtered.isEmpty()) "没有匹配的工具" else "搜索结果",
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.label
                )
            }
            toolGrid(filtered, onOpenTool)
        } else {
            if (frequent.isNotEmpty()) {
                item {
                    Text(
                        "常用",
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.label
                    )
                }
                toolGrid(frequent, onOpenTool)
            }
            categories.forEach { category ->
                item {
                    Text(
                        category.name,
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.label
                    )
                }
                toolGrid(category.tools, onOpenTool)
            }
        }
    }
}

@Composable
private fun CountdownCard(item: HomeCardData.CountdownItem, onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .clickable { onOpenTool("countdown_day") }
            .padding(20.dp)
    ) {
        Text("倒数日", style = MaterialTheme.typography.labelSmall, color = palette.tertiaryLabel, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(8.dp))
        Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = palette.label, maxLines = 2)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${item.daysLeft}",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 56.sp),
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
            Spacer(Modifier.width(6.dp))
            Text("天", style = MaterialTheme.typography.titleMedium, color = palette.secondaryLabel, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(item.dateIso, style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
    }
}

@Composable
private fun TodoCard(items: List<HomeCardData.TodoItem>, onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    val doneCount = items.count { it.done }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .clickable { onOpenTool("todo") }
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("待办事项", style = MaterialTheme.typography.labelSmall, color = palette.tertiaryLabel, letterSpacing = 0.5.sp)
            Text("$doneCount/${items.size}", style = MaterialTheme.typography.labelSmall, color = palette.green, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        items.take(3).forEach { todo ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (todo.done) palette.green else palette.tertiaryLabel.copy(alpha = 0.2f))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    todo.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (todo.done) palette.tertiaryLabel else palette.label,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (items.size > 3) {
            Text("还有 ${items.size - 3} 项…", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
        }
    }
}

@Composable
private fun OnThisDayCard(event: String, onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .clickable { onOpenTool("onthisday") }
            .padding(20.dp)
    ) {
        Text("历史上的今天", style = MaterialTheme.typography.labelSmall, color = palette.tertiaryLabel, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(12.dp))
        Text(event, style = MaterialTheme.typography.bodyLarge, color = palette.label, lineHeight = 22.sp, maxLines = 4)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.toolGrid(
    tools: List<ToolDef>,
    onOpenTool: (String) -> Unit
) {
    tools.chunked(2).forEach { pair ->
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { tool ->
                    ToolCard(
                        tool = tool,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenTool(tool.route) }
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ToolCard(
    tool: ToolDef,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalIosPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardBackground)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconTile(tool.icon, tool.tint(palette), size = 36.dp)
        Column {
            Text(
                tool.title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.label,
                maxLines = 1
            )
            Text(
                tool.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryLabel,
                maxLines = 2
            )
        }
    }
}
