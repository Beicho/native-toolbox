package com.toolbox.nativetoolbox.ui.tools

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private const val DEMO = """<!DOCTYPE html>
<html><head><style>
  body { font-family: sans-serif; padding: 16px; }
  .card { border-radius: 12px; padding: 16px; background: #eef4ff; }
  button { padding: 8px 16px; border: 0; border-radius: 8px; background: #3478f6; color: #fff; }
</style></head>
<body>
  <div class="card">
    <h2>你好</h2>
    <p>改一改代码,点「运行」立即看效果。</p>
    <button onclick="this.textContent='点到了 '+(++window.n||(window.n=1))+' 次'">点我</button>
  </div>
</body></html>"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlPreviewToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var code by rememberSaveable { mutableStateOf(DEMO) }
    var runId by remember { mutableStateOf(0) }        // 点运行才刷新
    var runningCode by remember { mutableStateOf(DEMO) }
    var tab by rememberSaveable { mutableStateOf(0) }   // 0 预览 1 代码

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("预览", "代码"), tab, { tab = it }, Modifier.fillMaxWidth())
                }
            }
        }
        item {
            if (tab == 0) {
                GroupedCard {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(460.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                // 沙盒:JS 开(预览用),文件/网络访问关,页面完全本地
                                settings.javaScriptEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.blockNetworkLoads = true
                                webViewClient = android.webkit.WebViewClient()
                                loadDataWithBaseURL(null, runningCode, "text/html", "utf-8", null)
                            }
                        },
                        update = { web ->
                            if (web.tag != runId) {
                                web.tag = runId
                                web.loadDataWithBaseURL(null, runningCode, "text/html", "utf-8", null)
                            }
                        }
                    )
                }
            } else {
                GroupedCard {
                    CardPadding {
                        IosTextArea(code, { code = it }, Modifier.fillMaxWidth(), placeholder = "<h1>Hello</h1>", minHeight = 380.dp, mono = true)
                    }
                }
            }
        }
        item {
            GroupedCard {
                CardPadding {
                    SolidButton(onClick = { runningCode = code; runId += 1; tab = 0 }, Modifier.fillMaxWidth()) { Text("运行") }
                    Spacer(Modifier.height(6.dp))
                    Text("完全在本机渲染,不联网、不读文件", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                }
            }
        }
    }
}
