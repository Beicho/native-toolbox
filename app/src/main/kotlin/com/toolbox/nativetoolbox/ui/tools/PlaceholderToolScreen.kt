package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

@Composable
fun PlaceholderToolScreen(title: String, onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Construction,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = palette.secondaryLabel
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = palette.label
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "正在打磨中",
            style = MaterialTheme.typography.bodyLarge,
            color = palette.secondaryLabel,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "这个工具即将在后续版本上线\n敬请期待",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.tertiaryLabel,
            textAlign = TextAlign.Center
        )
    }
}
