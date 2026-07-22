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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.IconTile
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** 首页:大标题 + 分类工具宫格(2 列实色卡片) */
@Composable
fun HomeScreen(onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    val categories = toolCategories()

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
                    "星辰之匣 · ${categories.sumOf { it.tools.size }} 个工具",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryLabel
                )
            }
        }

        categories.forEach { category ->
            item {
                Text(
                    category.name,
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.label
                )
            }
            val rows = category.tools.chunked(2)
            rows.forEach { pair ->
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
