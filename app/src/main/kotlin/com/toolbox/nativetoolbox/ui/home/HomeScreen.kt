package com.toolbox.nativetoolbox.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.rotate
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
import com.toolbox.nativetoolbox.data.predict.PredictEngine
import com.toolbox.nativetoolbox.data.prefs.UsageStore
import com.toolbox.nativetoolbox.ui.components.IconTile
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.PinyinInitials
import java.util.Calendar

/**
 * 首页。信息层级从上到下:
 *   问候 → 【此刻】(预测大脑) → 【今天】(活数据卡片) → 搜索 → 常用 → 全部工具
 *
 * @param refreshKey 每次导航回主页时 +1,用来重新拉活数据和预测结果
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    usageStore: UsageStore,
    refreshKey: Int = 0,
    onOpenTool: (String) -> Unit,
    onOpenToolFromRecommend: (String) -> Unit = onOpenTool,
) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val categories = remember { toolCategories() }
    val allTools = remember { categories.flatMap { it.tools } }
    val byRoute = remember { allTools.associateBy { it.route } }
    val onlineRoutes = remember { allTools.filter { it.requiresNetwork }.map { it.route }.toSet() }
    // 拼音首字母索引:jsq → 计算器。200 条启动时算一次
    val initialsIndex = remember { allTools.associate { it.route to PinyinInitials.of(it.title) } }
    var query by remember { mutableStateOf("") }
    val usage by usageStore.usageCounts.collectAsState(initial = emptyMap())

    // 预测结果:每次回主页重算(一次 < 5ms,不必缓存)
    var suggestions by remember { mutableStateOf<List<PredictEngine.Suggestion>>(emptyList()) }
    // 动态卡片数据
    var countdowns by remember { mutableStateOf<List<HomeCardData.CountdownItem>>(emptyList()) }
    var todos by remember { mutableStateOf<List<HomeCardData.TodoItem>>(emptyList()) }
    var onThisDay by remember { mutableStateOf<String?>(null) }
    var bookkeep by remember { mutableStateOf<HomeCardData.BookkeepSummary?>(null) }

    LaunchedEffect(refreshKey) {
        suggestions = if (PredictEngine.enabled) {
            PredictEngine.suggest(allTools.map { it.route }, limit = 3, onlineRoutes = onlineRoutes)
        } else emptyList()
        countdowns = HomeCardData.getUpcomingCountdowns(context)
        todos = HomeCardData.getTodos(context)
        onThisDay = HomeCardData.getOnThisDay(context)
        bookkeep = HomeCardData.getBookkeepSummary(context)
    }

    val greeting = remember(refreshKey) {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "早上好"
            in 11..13 -> "中午好"
            in 14..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深了"
        }
    }

    val frequent = remember(usage) {
        usage.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(8)  // 前 8 个常用工具,2x2 大卡片
            .mapNotNull { entry -> byRoute[entry.key] }
    }

    // 分类折叠状态
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }

    val filtered = if (query.isBlank()) null else run {
        val q = query.trim()
        val qLower = q.lowercase()
        allTools.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.subtitle.contains(q, ignoreCase = true) ||
                // 拼音首字母:纯字母查询时启用(jsq → 计算器)
                (qLower.all { c -> c in 'a'..'z' } &&
                    (initialsIndex[it.route]?.contains(qLower) == true))
        }
    }

    val liveCards = buildList {
        if (countdowns.isNotEmpty()) add("countdown")
        if (todos.isNotEmpty()) add("todo")
        if (bookkeep != null) add("bookkeep")
        if (onThisDay != null) add("onthisday")
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.groupedBackground),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 顶部问候 + 搜索框(固定区域,不随内容滚动)
        item {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp)
            ) {
                Text(greeting, style = MaterialTheme.typography.displayLarge, color = palette.label)
                Text(
                    "星辰之匣 · ${allTools.size} 个工具",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryLabel
                )
                Spacer(Modifier.height(16.dp))
                IosTextField(value = query, onValueChange = { query = it }, placeholder = "搜索工具…")
            }
        }

        // 【此刻】—— 预测大脑的门面
        if (query.isBlank()) {
            item {
                NowSection(
                    suggestions = suggestions,
                    toolTitle = { byRoute[it]?.title },
                    toolIcon = { byRoute[it]?.icon },
                    toolTint = { byRoute[it]?.tint?.invoke(palette) },
                    onOpen = onOpenToolFromRecommend,
                    onMute = { route ->
                        PredictEngine.mute(route)
                        suggestions = suggestions.filterNot { it.route == route }
                    }
                )
            }
        }

        // 【今天】—— 活数据卡片(改为横向滚动,不是 Pager)
        if (liveCards.isNotEmpty() && query.isBlank()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    "今天",
                    Modifier.padding(start = 20.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.label
                )
                // 横向滚动 Row,每个卡片宽度 280dp
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(liveCards.size) { index ->
                        Box(Modifier.width(280.dp).height(180.dp)) {
                            when (liveCards[index]) {
                                "countdown" -> CountdownCard(countdowns, onOpenTool)
                                "todo" -> TodoCard(
                                    items = todos,
                                    onToggle = { id, done ->
                                        HomeCardData.toggleTodo(id, done)
                                        todos = HomeCardData.getTodos(context)
                                    },
                                    onOpenTool = onOpenTool,
                                )
                                "bookkeep" -> BookkeepCard(
                                    summary = bookkeep!!,
                                    onQuickAdd = { amount, category ->
                                        HomeCardData.addExpense(amount, category)
                                        bookkeep = HomeCardData.getBookkeepSummary(context)
                                    },
                                    onOpenTool = onOpenTool,
                                )
                                "onthisday" -> OnThisDayCard(onThisDay!!, onOpenTool)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                IosTextField(value = query, onValueChange = { query = it }, placeholder = "搜索工具…")
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
            toolGrid(filtered, onOpenTool, largeTiles = false)
        } else {
            // 常用工具:前 8 个,2x4 大卡片布局
            if (frequent.isNotEmpty()) {
                item {
                    Text(
                        "常用",
                        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.label
                    )
                }
                toolGrid(frequent, onOpenTool, largeTiles = true)
            }
            // 全部工具:按分类折叠
            categories.forEach { category ->
                item {
                    val expanded = category.name in expandedCategories
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 90f else 0f,
                        animationSpec = tween(durationMillis = 300)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedCategories = if (expanded) {
                                    expandedCategories - category.name
                                } else {
                                    expandedCategories + category.name
                                }
                            }
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            category.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.label
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${category.tools.size} 个",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryLabel
                            )
                            Text(
                                " ›",
                                Modifier.rotate(rotation),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryLabel
                            )
                        }
                    }
                }
                item {
                    AnimatedVisibility(
                        visible = category.name in expandedCategories,
                        enter = expandVertically(animationSpec = tween(300)) + fadeIn(tween(200)),
                        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(tween(200))
                    ) {
                        Column {
                            toolGrid(category.tools, onOpenTool, largeTiles = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownCard(items: List<HomeCardData.CountdownItem>, onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    // 点卡片切换到下一个倒数日,不用进工具页
    var idx by remember(items.size) { mutableStateOf(0) }
    val item = items.getOrNull(idx) ?: return
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .combinedClickable(
                onClick = { if (items.size > 1) idx = (idx + 1) % items.size },
                onLongClick = { onOpenTool("tool/countdown_day") }
            )
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("倒数日", style = MaterialTheme.typography.labelSmall, color = palette.tertiaryLabel)
            if (items.size > 1) {
                Text(
                    "${idx + 1}/${items.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.tertiaryLabel
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = palette.label,
            maxLines = 2
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (item.daysLeft == 0) "今天" else "${item.daysLeft}",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp, lineHeight = 54.sp),
                fontWeight = FontWeight.Bold,
                color = if (item.daysLeft <= 3) palette.orange else palette.accent
            )
            if (item.daysLeft > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "天后",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.secondaryLabel,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        Text(item.dateIso, style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
    }
}

@Composable
private fun TodoCard(
    items: List<HomeCardData.TodoItem>,
    onToggle: (String, Boolean) -> Unit,
    onOpenTool: (String) -> Unit,
) {
    val palette = LocalIosPalette.current
    val doneCount = items.count { it.done }
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onOpenTool("tool/notes") },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("待办", style = MaterialTheme.typography.labelSmall, color = palette.tertiaryLabel)
            Text(
                "$doneCount/${items.size}",
                style = MaterialTheme.typography.labelSmall,
                color = if (doneCount == items.size) palette.green else palette.secondaryLabel,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(12.dp))
        // 直接在卡片上打勾 —— 不跳转,这是「每天开三次每次十秒」的关键
        items.take(3).forEach { todo ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(todo.id, !todo.done) }
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (todo.done) palette.green else palette.sunkenBackground),
                    contentAlignment = Alignment.Center
                ) {
                    if (todo.done) {
                        Text("✓", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    todo.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (todo.done) palette.tertiaryLabel else palette.label,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (items.size > 3) {
            Spacer(Modifier.height(4.dp))
            Text(
                "还有 ${items.size - 3} 项",
                style = MaterialTheme.typography.bodySmall,
                color = palette.tertiaryLabel,
                modifier = Modifier.clickable { onOpenTool("tool/notes") }
            )
        }
    }
}

@Composable
private fun BookkeepCard(
    summary: HomeCardData.BookkeepSummary,
    onQuickAdd: (Double, String) -> Unit,
    onOpenTool: (String) -> Unit,
) {
    val palette = LocalIosPalette.current
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("餐饮") }
    val quickCategories = listOf("餐饮", "交通", "购物", "其他")

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onOpenTool("tool/bookkeeping") },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("本月支出", style = MaterialTheme.typography.labelSmall, color = palette.tertiaryLabel)
            if (summary.todayTotal > 0) {
                Text(
                    "今天 ¥%.0f".format(summary.todayTotal),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryLabel
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "¥%.2f".format(summary.monthTotal),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp, lineHeight = 38.sp),
            fontWeight = FontWeight.Bold,
            color = palette.label
        )
        Spacer(Modifier.height(12.dp))
        // 卡片上直接记一笔
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IosTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.weight(1f),
                placeholder = "记一笔",
            )
            Spacer(Modifier.width(8.dp))
            SolidButton(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onQuickAdd(amt, category)
                        amountText = ""
                    }
                },
                modifier = Modifier.width(64.dp),
                height = 40.dp,
                enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true
            ) { Text("记") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quickCategories.forEach { c ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (category == c) palette.accent.copy(alpha = 0.15f) else palette.sunkenBackground)
                        .clickable { category = c }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        c,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (category == c) palette.accent else palette.secondaryLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun OnThisDayCard(event: String, onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .clickable { onOpenTool("tool/history_today") }
            .padding(20.dp)
    ) {
        Text("历史上的今天", style = MaterialTheme.typography.labelSmall, color = palette.tertiaryLabel)
        Spacer(Modifier.height(12.dp))
        Text(
            event,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.label,
            lineHeight = 22.sp,
            maxLines = 4
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.toolGrid(
    tools: List<ToolDef>,
    onOpenTool: (String) -> Unit,
    largeTiles: Boolean = false
) {
    if (largeTiles) {
        // 大卡片:常用工具,2x4 布局,每个占半宽,图标 48dp
        tools.chunked(2).forEach { pair ->
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { tool ->
                        LargeToolCard(
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
    } else {
        // 小卡片:全部工具和搜索结果,2 列布局
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
}

@Composable
private fun LargeToolCard(
    tool: ToolDef,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalIosPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardBackground)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(tool.icon, tool.tint(palette), size = 48.dp)
        Column {
            Text(
                tool.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.label,
                maxLines = 1
            )
            Text(
                tool.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryLabel,
                minLines = 2,
                maxLines = 2
            )
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
                minLines = 2,   // 固定两行高度:两列卡片高度才对得齐
                maxLines = 2
            )
        }
    }
}
