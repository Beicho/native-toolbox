package com.toolbox.nativetoolbox.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.toolbox.nativetoolbox.ui.theme.IosPalette

data class ToolDef(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: (IosPalette) -> Color
)

data class ToolCategory(val name: String, val tools: List<ToolDef>)

/** 全部工具注册表:首页宫格与导航都从这里生成 */
fun toolCategories(): List<ToolCategory> = listOf(
    ToolCategory(
        "开发者", listOf(
            ToolDef("tool/json", "JSON 工具", "格式化 · 压缩 · 转义", Icons.Rounded.DataObject) { it.orange },
            ToolDef("tool/timestamp", "时间戳", "Unix 时间互转", Icons.Rounded.Schedule) { it.green },
            ToolDef("tool/radix", "进制转换", "2 / 8 / 10 / 16 进制", Icons.Rounded.Numbers) { it.indigo },
            ToolDef("tool/base64", "Base64", "编码 · 解码", Icons.Rounded.Tag) { it.accent },
            ToolDef("tool/url", "URL 编解码", "百分号编码", Icons.Rounded.Link) { it.teal },
            ToolDef("tool/uuid", "UUID", "批量生成", Icons.Rounded.Fingerprint) { it.purple },
            ToolDef("tool/regex", "正则测试", "匹配 · 分组提取", Icons.Rounded.Rule) { it.pink },
            ToolDef("tool/color", "颜色工具", "HEX · RGB · HSL", Icons.Rounded.Palette) { it.red }
        )
    ),
    ToolCategory(
        "文本", listOf(
            ToolDef("tool/hash", "哈希计算", "MD5 · SHA 系列", Icons.Rounded.Password) { it.gray },
            ToolDef("tool/textprocess", "文本处理", "去重 · 排序 · 大小写", Icons.Rounded.TextFields) { it.orange },
            ToolDef("tool/textstats", "字数统计", "字符 · 词数 · 行数", Icons.Rounded.FormatListNumbered) { it.green },
            ToolDef("tool/random", "随机密码", "安全密码生成", Icons.Rounded.Casino) { it.indigo }
        )
    ),
    ToolCategory(
        "图片与文件", listOf(
            ToolDef("tool/qrcode", "二维码", "生成 · 识别", Icons.Rounded.QrCode2) { it.accent },
            ToolDef("tool/imagecompress", "图片压缩", "质量可调 · 对比", Icons.Rounded.Compress) { it.teal },
            ToolDef("tool/imageconvert", "格式转换", "PNG · JPG · WebP", Icons.Rounded.Transform) { it.purple },
            ToolDef("tool/encoding", "文本编码转换", "GBK → UTF-8 批量", Icons.Rounded.FontDownload) { it.pink }
        )
    )
)
