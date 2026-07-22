package com.toolbox.nativetoolbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/**
 * 工具页脚手架:实色 grouped 背景 + 顶部预留悬浮玻璃顶栏高度。
 * 注意:玻璃顶栏本体由 MainActivity 在 layerBackdrop 录制层【外】渲染,
 * 放在页面内部会导致 backdrop 层画到自己 → RenderThread 无限递归闪退。
 */
@Composable
fun ToolScaffold(content: LazyListScope.() -> Unit) {
    val palette = LocalIosPalette.current
    // 顶栏实际高度 = 状态栏 inset + 8(上边距) + 44(栏高) + 12(呼吸)
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.groupedBackground)
            .imePadding(),
        contentPadding = PaddingValues(top = statusBar + 64.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        content()
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
