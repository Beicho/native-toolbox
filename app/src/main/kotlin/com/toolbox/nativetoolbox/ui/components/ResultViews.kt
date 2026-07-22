package com.toolbox.nativetoolbox.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import com.toolbox.nativetoolbox.ui.theme.MonoStyle

/** 复制到剪贴板 + 轻提示 */
@Composable
fun rememberCopy(): (String) -> Unit {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    return { text ->
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
    }
}

/** 等宽输出卡:点右上角复制 */
@Composable
fun OutputCard(
    text: String,
    modifier: Modifier = Modifier,
    label: String = "结果",
    maxLines: Int = Int.MAX_VALUE
) {
    val palette = LocalIosPalette.current
    val copy = rememberCopy()
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(palette.sunkenBackground)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = palette.secondaryLabel)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = "复制",
                tint = palette.accent,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { if (text.isNotEmpty()) copy(text) }
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text.ifEmpty { "—" },
            style = MonoStyle,
            color = if (text.isEmpty()) palette.tertiaryLabel else palette.label,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 键值行(哈希/颜色格式等),点击整行复制值 */
@Composable
fun KeyValueRow(key: String, value: String, copyable: Boolean = true) {
    val palette = LocalIosPalette.current
    val copy = rememberCopy()
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (copyable && value.isNotEmpty()) Modifier.clickable { copy(value) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = palette.secondaryLabel)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Text(
                value.ifEmpty { "—" },
                style = MonoStyle,
                color = if (value.isEmpty()) palette.tertiaryLabel else palette.label,
                maxLines = 1,
                modifier = Modifier.horizontalScroll(scroll)
            )
        }
    }
}

/** 统计格子行 */
@Composable
fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalIosPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.sunkenBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = palette.label, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.secondaryLabel, maxLines = 1)
    }
}
