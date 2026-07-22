package com.toolbox.nativetoolbox.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Monitor
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Wifi
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

/** 全部工具注册表:首页宫格、搜索、导航、顶栏标题都从这里生成 */
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
        "开发者进阶", listOf(
            ToolDef("tool/jwt", "JWT 解码", "Header · Payload · 有效期", Icons.Rounded.Key) { it.orange },
            ToolDef("tool/diff", "文本对比", "行级 Diff 高亮", Icons.Rounded.CompareArrows) { it.green },
            ToolDef("tool/cron", "Cron 解析", "含义 + 未来执行时间", Icons.Rounded.Alarm) { it.indigo },
            ToolDef("tool/filehash", "文件哈希", "任意文件 MD5 · SHA", Icons.Rounded.Description) { it.gray }
        )
    ),
    ToolCategory(
        "文本", listOf(
            ToolDef("tool/hash", "哈希计算", "MD5 · SHA 系列", Icons.Rounded.Password) { it.gray },
            ToolDef("tool/textprocess", "文本处理", "去重 · 排序 · 大小写", Icons.Rounded.TextFields) { it.orange },
            ToolDef("tool/textstats", "字数统计", "字符 · 词数 · 行数", Icons.Rounded.FormatListNumbered) { it.green },
            ToolDef("tool/random", "随机密码", "安全密码生成", Icons.Rounded.Casino) { it.indigo },
            ToolDef("tool/encoding", "文本编码转换", "GBK → UTF-8 批量", Icons.Rounded.FontDownload) { it.pink }
        )
    ),
    ToolCategory(
        "图片", listOf(
            ToolDef("tool/qrcode", "二维码", "生成 · 识别", Icons.Rounded.QrCode2) { it.accent },
            ToolDef("tool/wifiqr", "WiFi 二维码", "扫码即连", Icons.Rounded.Wifi) { it.teal },
            ToolDef("tool/imagecompress", "图片压缩", "质量可调 · 对比", Icons.Rounded.Compress) { it.green },
            ToolDef("tool/imageconvert", "格式转换", "PNG · JPG · WebP", Icons.Rounded.Transform) { it.purple },
            ToolDef("tool/pickcolor", "图片取色", "点哪取哪", Icons.Rounded.Colorize) { it.red }
        )
    ),
    ToolCategory(
        "图片创作", listOf(
            ToolDef("tool/watermark", "证件水印", "平铺防盗用", Icons.Rounded.BrandingWatermark) { it.accent },
            ToolDef("tool/gridcut", "九宫格切图", "朋友圈裂图", Icons.Rounded.GridOn) { it.orange },
            ToolDef("tool/stitch", "长图拼接", "截图竖向合成", Icons.Rounded.ViewAgenda) { it.indigo },
            ToolDef("tool/exif", "EXIF 隐私", "查看 · 抹除元数据", Icons.Rounded.PrivacyTip) { it.red }
        )
    ),
    ToolCategory(
        "实用", listOf(
            ToolDef("tool/unit", "单位换算", "长度 · 重量 · 存储", Icons.Rounded.Straighten) { it.green },
            ToolDef("tool/datecalc", "日期计算", "间隔 · 倒数日", Icons.Rounded.Event) { it.teal },
            ToolDef("tool/deviceinfo", "设备信息", "硬件 · 系统 · 屏幕", Icons.Rounded.PhoneAndroid) { it.gray },
            ToolDef("tool/level", "水平仪", "玻璃气泡 · 倾角", Icons.Rounded.Balance) { it.accent },
            ToolDef("tool/screentest", "屏幕检测", "坏点 · 漏光", Icons.Rounded.Monitor) { it.purple }
        )
    ),
    ToolCategory(
        "整活", listOf(
            ToolDef("tool/banner", "弹幕横幅", "演唱会举牌神器", Icons.Rounded.Campaign) { it.pink },
            ToolDef("tool/decider", "帮我决定", "今天吃什么", Icons.Rounded.Celebration) { it.yellow }
        )
    )
)
