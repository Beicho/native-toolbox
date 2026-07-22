package com.toolbox.nativetoolbox.ui.liquid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/**
 * 悬浮玻璃顶栏:圆形玻璃返回按钮 + 玻璃胶囊标题,内容从下方滚过。
 */
@Composable
fun LiquidTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actionIcon: (@Composable () -> Unit)? = null,
    onAction: (() -> Unit)? = null
) {
    val palette = LocalIosPalette.current
    val backdrop = LocalRootBackdrop.current

    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            LiquidIconButton(onClick = onBack, size = 44.dp) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "返回",
                    modifier = Modifier.size(18.dp).padding(start = 2.dp),
                    tint = palette.accent
                )
            }
        } else {
            Box(Modifier.size(44.dp))
        }

        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .liquidPanel(
                        backdrop = backdrop,
                        surfaceColor = palette.glassSurface,
                        shape = { Capsule() },
                        blurRadius = 4.dp,
                        lensHeight = 12.dp,
                        lensAmount = 16.dp,
                        withShadow = false
                    )
                    .height(36.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actionIcon != null && onAction != null) {
            LiquidIconButton(onClick = onAction, size = 44.dp) { actionIcon() }
        } else {
            Box(Modifier.size(44.dp))
        }
    }
}
