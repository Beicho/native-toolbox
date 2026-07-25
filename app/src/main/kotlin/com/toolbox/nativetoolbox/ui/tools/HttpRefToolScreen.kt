package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val statusCodes = listOf(
    Triple("200", "OK", "请求成功"),
    Triple("201", "Created", "创建成功，常用于 POST 后返回新资源"),
    Triple("204", "No Content", "成功但没有返回内容，常用于删除"),
    Triple("206", "Partial Content", "断点续传返回的部分内容"),
    Triple("301", "Moved Permanently", "永久重定向，搜索引擎会更新收录"),
    Triple("302", "Found", "临时重定向"),
    Triple("304", "Not Modified", "命中缓存，内容没变"),
    Triple("307", "Temporary Redirect", "临时重定向且保持请求方法"),
    Triple("308", "Permanent Redirect", "永久重定向且保持请求方法"),
    Triple("400", "Bad Request", "请求本身有问题，参数或格式不对"),
    Triple("401", "Unauthorized", "没登录或凭据无效"),
    Triple("403", "Forbidden", "已登录但没有权限"),
    Triple("404", "Not Found", "地址不存在"),
    Triple("405", "Method Not Allowed", "这个地址不支持该请求方法"),
    Triple("409", "Conflict", "和现有状态冲突，如重复创建"),
    Triple("413", "Payload Too Large", "上传内容超过限制"),
    Triple("415", "Unsupported Media Type", "Content-Type 不被支持"),
    Triple("422", "Unprocessable Entity", "格式对但内容校验不过"),
    Triple("429", "Too Many Requests", "触发限流，看 Retry-After"),
    Triple("500", "Internal Server Error", "服务端自己出错了"),
    Triple("502", "Bad Gateway", "网关拿不到上游正常响应"),
    Triple("503", "Service Unavailable", "服务暂时不可用，通常是过载或维护"),
    Triple("504", "Gateway Timeout", "网关等上游超时")
)

private val headers = listOf(
    Triple("Authorization", "请求", "身份凭据，如 Bearer <token>"),
    Triple("Content-Type", "请求/响应", "正文格式，如 application/json"),
    Triple("Accept", "请求", "客户端希望收到的格式"),
    Triple("User-Agent", "请求", "客户端标识"),
    Triple("Referer", "请求", "来源页面地址"),
    Triple("Cookie", "请求", "携带的 Cookie"),
    Triple("If-None-Match", "请求", "配合 ETag 做缓存校验"),
    Triple("Range", "请求", "请求部分内容，断点续传用"),
    Triple("Cache-Control", "请求/响应", "缓存策略，如 no-cache、max-age=3600"),
    Triple("ETag", "响应", "内容指纹，用于缓存校验"),
    Triple("Location", "响应", "重定向目标地址"),
    Triple("Set-Cookie", "响应", "下发 Cookie"),
    Triple("Retry-After", "响应", "多久后可以重试，429/503 常见"),
    Triple("Content-Disposition", "响应", "指定下载文件名"),
    Triple("Access-Control-Allow-Origin", "响应", "允许跨域访问的来源"),
    Triple("Strict-Transport-Security", "响应", "强制浏览器只用 HTTPS"),
    Triple("Content-Security-Policy", "响应", "限制页面能加载的资源来源"),
    Triple("X-Content-Type-Options", "响应", "nosniff：禁止浏览器猜测类型")
)

private val methods = listOf(
    Triple("GET", "读取", "不应产生副作用，可缓存"),
    Triple("POST", "创建/提交", "有副作用，不可重复安全执行"),
    Triple("PUT", "整体替换", "幂等：重复执行结果一致"),
    Triple("PATCH", "局部更新", "只改传过来的字段"),
    Triple("DELETE", "删除", "幂等"),
    Triple("HEAD", "只要响应头", "用于探测存在性和大小"),
    Triple("OPTIONS", "查询能力", "跨域预检用")
)

@Composable
fun HttpRefToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var keyword by rememberSaveable { mutableStateOf("") }

    val source = when (tab) {
        0 -> statusCodes
        1 -> headers
        else -> methods
    }
    val filtered = if (keyword.isBlank()) source else source.filter { (a, b, c) ->
        listOf(a, b, c).any { it.contains(keyword.trim(), ignoreCase = true) }
    }

    ToolScaffold {
        item { SectionHeader("查什么") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("状态码", "请求头", "方法"),
                        selectedIndex = tab,
                        onSelected = { tab = it }
                    )
                    IosTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = "搜索关键词，中英文都行"
                    )
                }
            }
        }
        item { SectionHeader(if (filtered.isEmpty()) "没找到" else "共 ${filtered.size} 条") }
        if (filtered.isEmpty()) {
            item {
                GroupedCard {
                    CardPadding {
                        Text(
                            "换个关键词试试",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    }
                }
            }
        } else {
            item {
                GroupedCard {
                    filtered.forEachIndexed { index, (first, second, third) ->
                        KeyValueRow("$first　$second", third, copyable = false)
                        if (index != filtered.lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
