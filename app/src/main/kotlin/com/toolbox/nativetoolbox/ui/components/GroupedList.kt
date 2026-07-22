package com.toolbox.nativetoolbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/** iOS 分组列表节标题(全大写小字灰) */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val palette = LocalIosPalette.current
    Text(
        text,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 22.dp, bottom = 7.dp),
        style = MaterialTheme.typography.bodySmall,
        color = palette.secondaryLabel
    )
}

/** iOS inset grouped 卡片容器(实色内容层,10pt 圆角) */
@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalIosPalette.current
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.cardBackground),
        content = content
    )
}

/** 行间分隔线(iOS 风格,左侧缩进) */
@Composable
fun RowDivider(startIndent: androidx.compose.ui.unit.Dp = 16.dp) {
    val palette = LocalIosPalette.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(0.5.dp)
            .background(palette.separator.copy(alpha = 0.3f))
    )
}

/** iOS 设置风格图标块:圆角小方块彩底白图标 */
@Composable
fun IconTile(icon: ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp = 30.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4.4f))
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

/** 可点击行:图标块 + 标题 + 右侧值 + chevron */
@Composable
fun NavRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = LocalIosPalette.current.accent,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalIosPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconTile(icon, iconTint)
            Box(Modifier.size(12.dp, 1.dp))
        }
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = palette.label
        )
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.secondaryLabel
            )
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.tertiaryLabel,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** 开关行 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val palette = LocalIosPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.label)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = palette.green,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = palette.fill,
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

/** 勾选行(单选组用) */
@Composable
fun CheckRow(
    title: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalIosPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = palette.label
        )
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
