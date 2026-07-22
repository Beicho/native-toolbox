package com.toolbox.nativetoolbox.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.data.prefs.UsageStore
import com.toolbox.nativetoolbox.ui.components.IconTile
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** 首页:大标题 + 搜索 + 常用置顶 + 分类工具宫格 */
@Composable
fun HomeScreen(usageStore: UsageStore, onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    val categories = remember { toolCategories() }
    val allTools = remember { categories.flatMap { it.tools } }
    var query by remember { mutableStateOf("") }
    val usage by usageStore.usageCounts.collectAsState(initial = emptyMap())

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IosTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "搜索工具…"
                    )
                }
            }
        }

        if (filtered != null) {
            // 搜索结果
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
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryLabel,
                maxLines = 1
            )
        }
    }
}
