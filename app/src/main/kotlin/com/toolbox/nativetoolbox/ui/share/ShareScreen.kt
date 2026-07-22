package com.toolbox.nativetoolbox.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.NavRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.util.ShareBus

/** 系统分享进入的中转页:预览文本 + 选择用哪个工具处理 */
@Composable
fun ShareScreen(onOpenTool: (String) -> Unit) {
    val palette = LocalIosPalette.current
    val text = ShareBus.peek() ?: ""

    val targets = listOf(
        Triple("tool/json", "JSON 格式化", Icons.Rounded.DataObject),
        Triple("tool/base64", "Base64 编解码", Icons.Rounded.Tag),
        Triple("tool/url", "URL 编解码", Icons.Rounded.Link),
        Triple("tool/jwt", "JWT 解码", Icons.Rounded.Key),
        Triple("tool/textstats", "字数统计", Icons.Rounded.FormatListNumbered),
        Triple("tool/hash", "哈希计算", Icons.Rounded.Password),
        Triple("tool/qrcode", "生成二维码", Icons.Rounded.QrCode2)
    )

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.groupedBackground),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            Text(
                "分享到 Astro Kit",
                Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                style = MaterialTheme.typography.displayMedium,
                color = palette.label
            )
        }
        item { SectionHeader("收到的内容") }
        item {
            GroupedCard {
                Text(
                    text,
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryLabel,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item { SectionHeader("用哪个工具处理?") }
        item {
            GroupedCard {
                targets.forEachIndexed { index, (route, name, icon) ->
                    if (index > 0) RowDivider(startIndent = 58.dp)
                    NavRow(
                        name,
                        icon = icon,
                        iconTint = palette.accent,
                        onClick = { onOpenTool(route) }
                    )
                }
            }
        }
    }
}
