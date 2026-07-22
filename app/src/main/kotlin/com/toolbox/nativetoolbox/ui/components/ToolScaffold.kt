package com.toolbox.nativetoolbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.liquid.LiquidTopBar
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/**
 * 工具页脚手架:实色 grouped 背景内容 + 悬浮玻璃顶栏。
 * 内容顶部预留顶栏高度,底部预留导航栏与键盘。
 */
@Composable
fun ToolScaffold(
    title: String,
    onBack: () -> Unit,
    actionIcon: (@Composable () -> Unit)? = null,
    onAction: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    val palette = LocalIosPalette.current
    // 顶栏实际高度 = 状态栏 inset + 8(上边距) + 44(栏高) + 8(下边距)
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.groupedBackground)
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(top = statusBar + 64.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            content()
        }
        LiquidTopBar(
            title = title,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
            actionIcon = actionIcon,
            onAction = onAction
        )
    }
}

/** 卡片内部统一留白列 */
@Composable
fun CardPadding(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}
