# 工具实现指南（给并行实现 agent 看）

每个工具 = 一个独立 Screen 文件（`ui/tools/XxxToolScreen.kt`），零跨文件冲突。
实现后到对应分类的 `ui/nav/*Graph.kt` 把 `PlaceholderToolScreen("标题", back)` 换成 `XxxToolScreen(back)`。

## 页面骨架（照抄）

```kotlin
package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.runtime.*
import com.toolbox.nativetoolbox.ui.components.*

@Composable
fun XxxToolScreen(onBack: () -> Unit) {
    ToolScaffold {   // 顶栏由 MainActivity 在录制层外渲染,页面不要自己画玻璃顶栏
        item {
            GroupedCard {
                CardPadding {
                    // 你的 UI
                }
            }
        }
    }
}
```

`ToolScaffold { }` 接收 `LazyListScope`（内部用 `item { }` / `items() { }`）。

## 可用组件（严格按签名，别臆造参数）

| 组件 | 签名 | 用途 |
|---|---|---|
| `IosTextField` | `(value, onValueChange, modifier, placeholder="", singleLine=true, minLines=1, mono=false)` | 单行输入 |
| `IosTextArea` | `(value, onValueChange, modifier, placeholder="", minHeight=120.dp, mono=false)` | 多行输入 |
| `SegmentedPicker` | `(options: List<String>, selectedIndex: Int, onSelected: (Int)->Unit, modifier)` | 分段选择器 |
| `SolidButton` | `(onClick, modifier, filled=true, enabled=true, height=44.dp, content: @Composable RowScope.()->Unit)` | 按钮，**用 content slot：`SolidButton(onClick={}){ Text("确定") }`** |
| `OutputCard` | `(text: String, modifier, label="结果", maxLines=Int.MAX_VALUE)` | 结果展示（自带复制按钮） |
| `KeyValueRow` | `(key: String, value: String, copyable=true)` | 键值行 |
| `StatCell` | `(label: String, value: String, modifier)` | 统计格子 |
| `SectionHeader` | `(text: String, modifier)` | 分组标题 |
| `GroupedCard` | `(modifier, content: @Composable ColumnScope.()->Unit)` | iOS 分组卡片容器 |
| `RowDivider` | `(startIndent=16.dp)` | 分隔线 |
| `IconTile` | `(icon: ImageVector, tint: Color, size=30.dp)` | 图标块 |
| `NavRow` / `ToggleRow` / `CheckRow` | 见 GroupedList.kt | 列表行 |
| `CardPadding` | `(content: @Composable ColumnScope.()->Unit)` | 卡片内 16dp 留白列 |
| `rememberCopy()` | `(): (String)->Unit` | 复制到剪贴板 |

配色：`val palette = LocalIosPalette.current`（`ui/theme`），字段 `label/secondaryLabel/tertiaryLabel/accent/groupedBackground/sunkenBackground/orange/green/red/...`。

## 联网工具（标 [网] 的）

用 `com.toolbox.nativetoolbox.net.AstroApi`（唯一网络出口）：

```kotlin
val scope = rememberCoroutineScope()
scope.launch {
    val r = AstroApi.get("/weather", mapOf("lat" to "39.9", "lon" to "116.4"))
    r.onSuccess { res ->
        val json = res.data          // org.json.JSONObject（服务端 data 字段）
        if (res.cachedAt > 0) { /* 断网降级,页面标"缓存于 ${格式化时间}" */ }
    }.onFailure { e -> /* 显示 e.message,给重试按钮 */ }
}
```

端点见 `server/README.md`。GET：update/exchange/weather/ip/dns/whois/ssl/sitecheck/holiday/hitokoto/today；POST：`AstroApi.post("/translate", JSONObject().put("mode","text").put("text",t).put("to","en"))`；临时邮箱 `/mail/new`(POST 空体)、`/mail/list?id=`、`/mail/detail?id=&eid=`。

## 权限工具（标 [权] 的）

包裹 `PermissionGate`（`util/Permissions.kt`）：

```kotlin
import android.Manifest
PermissionGate(Manifest.permission.CAMERA, "需要相机来扫描二维码") {
    // 已授权后的 UI
}
```

## DoD（每个工具都要满足）

1. 空输入/非法输入有提示，不崩溃；长任务有 loading。
2. 结果可复制（`OutputCard` 或 `rememberCopy()`）。
3. 深浅色都正常（用 palette，别硬编码颜色）。
4. 联网工具断网降级到缓存并标注；权限拒绝有说明。
5. 不留 TODO、不留假数据、不 `println` 调试。
6. 文案讲人话，不堆技术术语。

## 禁忌

- 别在页面里画 `drawBackdrop`/玻璃组件（会和 MainActivity 的 `layerBackdrop` 录制层循环引用 → 原生闪退）。卡片内按钮一律 `SolidButton`。
- 别跑 `./gradlew`（本机 PRoot 会崩，构建走 CI）。
- 别改别的分类的文件 / 别改 `MainActivity.kt` / `ToolRegistry.kt`（除了你负责分类的 `ui/nav/XxxGraph.kt`）。
