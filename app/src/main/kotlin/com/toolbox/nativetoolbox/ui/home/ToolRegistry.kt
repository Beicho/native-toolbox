package com.toolbox.nativetoolbox.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.toolbox.nativetoolbox.ui.theme.IosPalette

data class ToolDef(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val requiresNetwork: Boolean = false,
    val permissions: List<String> = emptyList(),
    val tint: (IosPalette) -> Color
)

data class ToolCategory(val name: String, val tools: List<ToolDef>)

/** 全部工具注册表:首页宫格、搜索、导航、顶栏标题都从这里生成 */
fun toolCategories(): List<ToolCategory> = listOf(
    // 6.1 开发者（26）
    ToolCategory("开发者", listOf(
        ToolDef("tool/json", "JSON 工具", "格式化 · 压缩 · 转义", Icons.Rounded.DataObject) { it.orange },
        ToolDef("tool/timestamp", "时间戳", "Unix 时间互转", Icons.Rounded.Schedule) { it.green },
        ToolDef("tool/radix", "进制转换", "2 / 8 / 10 / 16 进制", Icons.Rounded.Numbers) { it.indigo },
        ToolDef("tool/base64", "Base 编码", "Base64/32/58/85 全家", Icons.Rounded.Tag) { it.accent },
        ToolDef("tool/url", "URL 编解码", "百分号编码", Icons.Rounded.Link) { it.teal },
        ToolDef("tool/uuid", "UUID", "批量生成 v4/v7", Icons.Rounded.Fingerprint) { it.purple },
        ToolDef("tool/regex", "正则测试", "匹配 · 分组提取", Icons.Rounded.Rule) { it.pink },
        ToolDef("tool/color", "颜色工具", "HEX · RGB · HSL", Icons.Rounded.Palette) { it.red },
        ToolDef("tool/unicode_escape", "字符转义", "Unicode · HTML · Punycode", Icons.Rounded.Code) { it.orange },
        ToolDef("tool/mockdata", "Mock 数据", "假姓名手机号批量生成", Icons.Rounded.PersonAdd) { it.green },
        ToolDef("tool/curl_parse", "cURL 解析", "粘贴 cURL 看结构", Icons.Rounded.Terminal) { it.indigo },
        ToolDef("tool/http_ref", "HTTP 速查", "状态码 · 请求头速查", Icons.Rounded.Http) { it.accent },
        ToolDef("tool/ua_parse", "UA 解析", "User-Agent 解析", Icons.Rounded.Devices) { it.teal },
        ToolDef("tool/sql_format", "SQL 格式化", "美化 SQL 关键字高亮", Icons.Rounded.Storage) { it.purple },
        ToolDef("tool/config_convert", "配置互转", "JSON ⇄ YAML ⇄ TOML", Icons.Rounded.SwapHoriz) { it.pink },
        ToolDef("tool/markdown_preview", "Markdown 预览", "实时渲染 · 导出 HTML", Icons.Rounded.Description) { it.red },
        ToolDef("tool/html_preview", "HTML 预览", "沙盒渲染 HTML/CSS/JS", Icons.Rounded.WebAsset) { it.orange },
        ToolDef("tool/css_gen", "CSS 样式生成", "阴影渐变圆角可视化", Icons.Rounded.Brush) { it.green },
        ToolDef("tool/svg_tool", "SVG 工具", "预览 · 优化 · 转 PNG", Icons.Rounded.VectorSquare) { it.indigo },
        ToolDef("tool/chmod", "chmod 计算", "rwx ⇄ 755 互转", Icons.Rounded.Security) { it.accent },
        ToolDef("tool/cmd_ref", "命令速查", "Git · Docker · Linux", Icons.Rounded.Terminal) { it.teal },
        ToolDef("tool/ascii", "ASCII 码表", "查码点 · UTF-8 字节", Icons.Rounded.TextFormat) { it.purple },
        ToolDef("tool/json2code", "JSON 转代码", "生成 Kotlin · TS 类", Icons.Rounded.Code) { it.pink },
        ToolDef("tool/android_ref", "Android 速查", "API Level 对照表", Icons.Rounded.Android) { it.green },
        ToolDef("tool/icon_gen", "图标生成", "App 图标全尺寸导出", Icons.Rounded.AppShortcut) { it.red },
        ToolDef("tool/resistor", "电阻色环", "色环 ⇄ 阻值换算", Icons.Rounded.Cable) { it.orange }
    )),

    // 6.2 开发者进阶（10）
    ToolCategory("开发者进阶", listOf(
        ToolDef("tool/jwt", "JWT 解码", "Header · Payload · 有效期", Icons.Rounded.Key) { it.orange },
        ToolDef("tool/diff", "文本对比", "行级 Diff 高亮", Icons.Rounded.CompareArrows) { it.green },
        ToolDef("tool/cron", "Cron 解析", "含义 + 未来执行时间", Icons.Rounded.Alarm) { it.indigo },
        ToolDef("tool/hash", "哈希与校验", "文本/文件 MD5 · SHA · HMAC", Icons.Rounded.Tag) { it.gray },
        ToolDef("tool/hex_viewer", "Hex 查看器", "十六进制 + ASCII 对照", Icons.Rounded.ViewInAr) { it.accent },
        ToolDef("tool/apk_analyze", "APK 分析", "包名版本权限签名", Icons.Rounded.InstallMobile) { it.teal },
        ToolDef("tool/log_analyze", "日志分析", "按级别过滤 · 高频错误", Icons.Rounded.BugReport) { it.purple },
        ToolDef("tool/code_screenshot", "代码截图", "语法高亮精美分享图", Icons.Rounded.Screenshot) { it.pink },
        ToolDef("tool/http_test", "HTTP 请求测试", "迷你 Postman", Icons.Rounded.Http) { it.red },
        ToolDef("tool/websocket_test", "WebSocket 测试", "连接收发日志", Icons.Rounded.Cable) { it.orange }
    )),

    // 6.3 网络（13）
    ToolCategory("网络", listOf(
        ToolDef("tool/ip_query", "IP 查询", "归属地 · ASN · 反查", Icons.Rounded.Language, tint = { it.orange }, requiresNetwork = true),
        ToolDef("tool/dns_query", "DNS 查询", "A/AAAA/CNAME/MX/TXT", Icons.Rounded.Dns) { it.green },
        ToolDef("tool/whois", "Whois", "域名注册商到期查询", Icons.Rounded.Domain, tint = { it.indigo }, requiresNetwork = true),
        ToolDef("tool/ssl_cert", "SSL 证书", "证书链有效期查看", Icons.Rounded.VerifiedUser, tint = { it.accent }, requiresNetwork = true),
        ToolDef("tool/site_check", "网站体检", "DNS/TLS/响应头评分", Icons.Rounded.HealthAndSafety, tint = { it.teal }, requiresNetwork = true),
        ToolDef("tool/tcp_ping", "连通性测试", "批量 TCP ping", Icons.Rounded.Speed) { it.purple },
        ToolDef("tool/speed_test", "网络测速", "下载上传延迟", Icons.Rounded.NetworkCheck, tint = { it.pink }, requiresNetwork = true),
        ToolDef("tool/short_url", "短链生成", "长链转短链", Icons.Rounded.Link, tint = { it.red }, requiresNetwork = true),
        ToolDef("tool/unshort_url", "短链还原", "展开跳转链防钓鱼", Icons.Rounded.OpenInNew, tint = { it.orange }, requiresNetwork = true),
        ToolDef("tool/temp_mail", "临时邮箱", "一次性邮箱收验证码", Icons.Rounded.Email, tint = { it.green }, requiresNetwork = true),
        ToolDef("tool/phone_share", "手机网盘", "局域网 HTTP 文件互传", Icons.Rounded.Folder) { it.indigo },
        ToolDef("tool/file_download", "文件下载器", "多线程断点续传", Icons.Rounded.Download) { it.accent },
        ToolDef("tool/wol", "网络唤醒", "WOL 魔术包唤醒电脑", Icons.Rounded.PowerSettingsNew) { it.teal }
    )),

    // 6.4 文本（16）
    ToolCategory("文本", listOf(
        ToolDef("tool/textprocess", "文本处理", "去重 · 排序 · 大小写", Icons.Rounded.TextFields) { it.orange },
        ToolDef("tool/textstats", "字数统计", "字符 · 词数 · 高频词", Icons.Rounded.FormatListNumbered) { it.green },
        ToolDef("tool/encoding", "编码转换", "GBK → UTF-8 批量", Icons.Rounded.FontDownload) { it.pink },
        ToolDef("tool/cn_convert", "繁简转换", "简繁 · 港台用语", Icons.Rounded.Translate) { it.indigo },
        ToolDef("tool/pinyin", "拼音标注", "汉字注音 · 姓名拼音", Icons.Rounded.Spellcheck) { it.accent },
        ToolDef("tool/translate", "翻译与词典", "多语言互译 · 查词", Icons.Rounded.GTranslate, tint = { it.teal }, requiresNetwork = true),
        ToolDef("tool/morse", "摩斯电码", "文本 ⇄ 摩斯 · 声光播放", Icons.Rounded.GraphicEq) { it.purple },
        ToolDef("tool/fullwidth", "全角半角", "全半角互转", Icons.Rounded.FormatSize) { it.pink },
        ToolDef("tool/fancy_text", "花式文字", "Unicode 花体圆圈字", Icons.Rounded.AutoAwesome) { it.red },
        ToolDef("tool/zero_width", "零宽隐写", "隐形水印藏信息", Icons.Rounded.Visibility) { it.orange },
        ToolDef("tool/vertical_text", "竖排古风", "横排转竖排右起", Icons.Rounded.VerticalAlignTop) { it.green },
        ToolDef("tool/text_format", "文案排版", "盘古之白 · 标点规范", Icons.Rounded.FormatAlignLeft) { it.indigo },
        ToolDef("tool/emoji_lib", "表情符号库", "emoji · 颜文字", Icons.Rounded.EmojiEmotions) { it.accent },
        ToolDef("tool/mask_sensitive", "敏感信息打码", "手机号身份证自动打码", Icons.Rounded.HideSource) { it.teal },
        ToolDef("tool/clipboard_shelf", "剪贴板暂存架", "多条文本暂存合并", Icons.Rounded.ContentPaste) { it.purple },
        ToolDef("tool/text_template", "文本模板", "请假条通知模板", Icons.Rounded.Description) { it.pink }
    )),

    // 6.5 加密与隐私（9）
    ToolCategory("加密与隐私", listOf(
        ToolDef("tool/random", "密码工具", "随机密码 · 强度体检", Icons.Rounded.Password) { it.indigo },
        ToolDef("tool/exif", "EXIF 隐私", "查看抹除照片元数据", Icons.Rounded.PrivacyTip) { it.red },
        ToolDef("tool/totp", "TOTP 验证器", "两步验证码生成", Icons.Rounded.Security) { it.orange },
        ToolDef("tool/password_vault", "密码保险箱", "加密存储账号密码", Icons.Rounded.Lock) { it.green },
        ToolDef("tool/encrypt_capsule", "加密胶囊", "口令加密文本文件", Icons.Rounded.EnhancedEncryption) { it.indigo },
        ToolDef("tool/keypair_gen", "密钥对生成", "RSA/EC 密钥对", Icons.Rounded.VpnKey) { it.accent },
        ToolDef("tool/image_steg", "图片隐写", "图片里藏文字", Icons.Rounded.ImageSearch) { it.teal },
        ToolDef("tool/app_permissions", "应用权限透视", "高危权限清单", Icons.Rounded.Shield) { it.purple },
        ToolDef("tool/private_album", "私密相册", "照片视频加密存储", Icons.Rounded.PhotoLibrary) { it.pink }
    )),

    // 6.6 图片编辑（12）
    ToolCategory("图片编辑", listOf(
        ToolDef("tool/imagecompress", "图片压缩", "质量可调 · 批量处理", Icons.Rounded.Compress) { it.green },
        ToolDef("tool/imageconvert", "格式转换", "PNG/JPG/WebP/HEIC", Icons.Rounded.Transform) { it.purple },
        ToolDef("tool/pickcolor", "图片取色", "点哪取哪 · 主色调提取", Icons.Rounded.Colorize) { it.red },
        ToolDef("tool/image_crop", "裁剪旋转", "自由裁剪 · 旋转翻转", Icons.Rounded.Crop) { it.orange },
        ToolDef("tool/image_filter", "滤镜调色", "亮度对比饱和", Icons.Rounded.FilterBAndW) { it.green },
        ToolDef("tool/image_mosaic", "打码涂抹", "马赛克 · 高斯模糊", Icons.Rounded.Blur) { it.indigo },
        ToolDef("tool/image_border", "圆角边框", "圆角描边阴影", Icons.Rounded.RoundedCorner) { it.accent },
        ToolDef("tool/image_annotate", "图片标注", "箭头方框文字", Icons.Rounded.Edit) { it.teal },
        ToolDef("tool/image_matting", "智能抠图", "人像分割透明背景", Icons.Rounded.AutoFixHigh) { it.purple },
        ToolDef("tool/id_photo", "证件照制作", "换底色 · 一寸二寸", Icons.Rounded.Portrait) { it.pink },
        ToolDef("tool/image_compare", "图片对比", "滑动对比差异高亮", Icons.Rounded.Compare) { it.red },
        ToolDef("tool/similar_clean", "相似图片清理", "扫相册找重复", Icons.Rounded.CleaningServices) { it.orange }
    )),

    // 6.7 图片创作（11）
    ToolCategory("图片创作", listOf(
        ToolDef("tool/qrcode", "二维码", "生成识别 · logo 嵌入", Icons.Rounded.QrCode2) { it.accent },
        ToolDef("tool/wifiqr", "WiFi 二维码", "扫码即连", Icons.Rounded.Wifi) { it.teal },
        ToolDef("tool/watermark", "证件水印", "平铺防盗用水印", Icons.Rounded.BrandingWatermark) { it.accent },
        ToolDef("tool/gridcut", "九宫格切图", "朋友圈裂图", Icons.Rounded.GridOn) { it.orange },
        ToolDef("tool/stitch", "图片拼接", "长图拼接 · 网格拼", Icons.Rounded.ViewAgenda) { it.indigo },
        ToolDef("tool/gif_make", "GIF 制作", "多图合成 GIF", Icons.Rounded.Gif) { it.green },
        ToolDef("tool/barcode", "条形码", "Code128/EAN-13 生成", Icons.Rounded.QrCodeScanner) { it.purple },
        ToolDef("tool/ascii_art", "艺术化转换", "字符画 · 像素画", Icons.Rounded.Grain) { it.pink },
        ToolDef("tool/meme_maker", "表情包制作", "顶底白字梗字幕", Icons.Rounded.TagFaces) { it.red },
        ToolDef("tool/checkin_watermark", "打卡水印", "时间地点天气盖章", Icons.Rounded.LocationOn) { it.orange },
        ToolDef("tool/color_scheme", "配色方案", "互补类似配色卡", Icons.Rounded.Palette) { it.green }
    )),

    // 6.8 音频视频（16）
    ToolCategory("音频视频", listOf(
        ToolDef("tool/recorder", "录音机", "高品质录音书签标记", Icons.Rounded.Mic, tint = { it.orange }, permissions = listOf("android.permission.RECORD_AUDIO")),
        ToolDef("tool/audio_edit", "音频剪辑", "波形裁剪拼接", Icons.Rounded.AudioFile) { it.green },
        ToolDef("tool/audio_convert", "音频格式转换", "wav/m4a/flac/ogg", Icons.Rounded.SwapHoriz) { it.indigo },
        ToolDef("tool/ringtone_make", "铃声制作", "歌曲截段设为铃声", Icons.Rounded.MusicNote) { it.accent },
        ToolDef("tool/audio_extract", "音频提取", "视频抽出音轨", Icons.Rounded.LibraryMusic) { it.teal },
        ToolDef("tool/voice_change", "变声与倒放", "变速变调 · 倒放", Icons.Rounded.RecordVoiceOver) { it.purple },
        ToolDef("tool/ab_player", "AB 循环播放器", "变速 AB 段循环", Icons.Rounded.Repeat) { it.pink },
        ToolDef("tool/tts", "文字朗读", "文本 TTS 朗读", Icons.Rounded.VolumeUp) { it.red },
        ToolDef("tool/video_compress", "视频压缩", "降码率分辨率", Icons.Rounded.VideoSettings) { it.orange },
        ToolDef("tool/video_to_gif", "视频转 GIF", "选段转 GIF", Icons.Rounded.Gif) { it.green },
        ToolDef("tool/video_frame", "视频截帧", "逐帧预览导出", Icons.Rounded.Image) { it.indigo },
        ToolDef("tool/video_info", "视频信息", "编码码率帧率", Icons.Rounded.Info) { it.accent },
        ToolDef("tool/metronome", "节拍器", "可视节拍练琴", Icons.Rounded.Speed) { it.teal },
        ToolDef("tool/tuner", "调音器", "麦克风频率检测", Icons.Rounded.Tune, tint = { it.purple }, permissions = listOf("android.permission.RECORD_AUDIO")),
        ToolDef("tool/white_noise", "白噪音", "雨声海浪混音", Icons.Rounded.Headphones) { it.pink },
        ToolDef("tool/earphone_test", "耳机测试", "左右声道频响", Icons.Rounded.Headset) { it.red }
    )),

    // 6.9 计算换算（21）
    ToolCategory("计算换算", listOf(
        ToolDef("tool/unit", "单位换算", "长度重量存储面积", Icons.Rounded.Straighten) { it.green },
        ToolDef("tool/datecalc", "日期计算", "间隔倒数工作日", Icons.Rounded.Event) { it.teal },
        ToolDef("tool/sci_calc", "科学计算器", "函数括号历史", Icons.Rounded.Calculate) { it.indigo },
        ToolDef("tool/exchange", "汇率换算", "170+ 币种", Icons.Rounded.CurrencyExchange, tint = { it.accent }, requiresNetwork = true),
        ToolDef("tool/mortgage", "房贷计算", "等额本息本金对比", Icons.Rounded.Home) { it.orange },
        ToolDef("tool/interest", "利息与利率", "复利 IRR 真实年化", Icons.Rounded.TrendingUp) { it.green },
        ToolDef("tool/tax", "个税计算", "工资年终奖个税", Icons.Rounded.AccountBalance) { it.purple },
        ToolDef("tool/amount_upper", "金额大写", "数字转人民币大写", Icons.Rounded.MoneyOff) { it.pink },
        ToolDef("tool/percent", "百分比折扣", "打折涨跌幅", Icons.Rounded.Percent) { it.red },
        ToolDef("tool/combo_calc", "凑单计算", "满减最优凑单", Icons.Rounded.ShoppingCart) { it.orange },
        ToolDef("tool/price_compare", "比价计算", "单位价格对比", Icons.Rounded.CompareArrows) { it.green },
        ToolDef("tool/cost_split", "费用分摊", "AA 制分摊", Icons.Rounded.People) { it.indigo },
        ToolDef("tool/relative_name", "亲戚称呼", "爸爸的表哥查称呼", Icons.Rounded.FamilyRestroom) { it.accent },
        ToolDef("tool/health_calc", "健康计算", "BMI/BMR/体脂率", Icons.Rounded.FitnessCenter) { it.teal },
        ToolDef("tool/timezone", "时区对照", "多城市会议时间", Icons.Rounded.Public) { it.purple },
        ToolDef("tool/random_num", "随机数", "范围个数去重", Icons.Rounded.Casino) { it.pink },
        ToolDef("tool/statistics", "统计计算", "均值中位数方差", Icons.Rounded.Analytics) { it.red },
        ToolDef("tool/geometry", "几何计算", "面积体积勾股", Icons.Rounded.Category) { it.orange },
        ToolDef("tool/fuel_calc", "油耗计算", "百公里油耗成本", Icons.Rounded.LocalGasStation) { it.green },
        ToolDef("tool/pace_calc", "配速计算", "马拉松配速", Icons.Rounded.DirectionsRun) { it.indigo },
        ToolDef("tool/decoration", "装修计算", "地板瓷砖用量", Icons.Rounded.Construction) { it.accent }
    )),

    // 6.10 设备硬件（17）
    ToolCategory("设备硬件", listOf(
        ToolDef("tool/deviceinfo", "设备信息", "硬件系统屏幕", Icons.Rounded.PhoneAndroid) { it.gray },
        ToolDef("tool/level", "水平仪", "玻璃气泡倾角", Icons.Rounded.Balance) { it.accent },
        ToolDef("tool/screentest", "屏幕检测", "坏点漏光触控", Icons.Rounded.Monitor) { it.purple },
        ToolDef("tool/sensor_dash", "传感器仪表盘", "陀螺仪加速度实时曲线", Icons.Rounded.Sensors) { it.orange },
        ToolDef("tool/compass", "指南针", "罗盘真北磁北", Icons.Rounded.Explore, tint = { it.green }, permissions = listOf("android.permission.ACCESS_FINE_LOCATION")),
        ToolDef("tool/battery_info", "电池信息", "功率电压温度", Icons.Rounded.Battery) { it.indigo },
        ToolDef("tool/performance", "性能监控", "CPU 内存温度曲线", Icons.Rounded.Memory) { it.accent },
        ToolDef("tool/hardware_test", "硬件体检", "扬声器麦克风检测", Icons.Rounded.Build) { it.teal },
        ToolDef("tool/storage_clean", "存储清理", "大文件重复文件", Icons.Rounded.CleaningServices) { it.purple },
        ToolDef("tool/wifi_analyze", "WiFi 分析", "信号强度信道", Icons.Rounded.Wifi, tint = { it.pink }, permissions = listOf("android.permission.ACCESS_FINE_LOCATION")),
        ToolDef("tool/bluetooth_scan", "蓝牙雷达", "周边设备扫描", Icons.Rounded.Bluetooth) { it.red },
        ToolDef("tool/nfc_tool", "NFC 读写", "读卡 UID · 写 NDEF", Icons.Rounded.Nfc, tint = { it.orange }, permissions = listOf("android.permission.NFC")),
        ToolDef("tool/gps_speed", "GPS 速度表", "卫星数 HUD 速度", Icons.Rounded.Speed, tint = { it.green }, permissions = listOf("android.permission.ACCESS_FINE_LOCATION")),
        ToolDef("tool/decibel_meter", "分贝仪", "环境噪音测量", Icons.Rounded.GraphicEq, tint = { it.indigo }, permissions = listOf("android.permission.RECORD_AUDIO")),
        ToolDef("tool/flashlight", "手电筒", "常亮 SOS 爆闪", Icons.Rounded.Flashlight) { it.accent },
        ToolDef("tool/screen_on", "屏幕常亮", "临时防熄屏", Icons.Rounded.Lightbulb) { it.teal },
        ToolDef("tool/screen_time", "屏幕时间", "各 App 使用时长", Icons.Rounded.HourglassEmpty) { it.purple }
    )),

    // 6.11 效率办公（15）
    ToolCategory("效率办公", listOf(
        ToolDef("tool/ocr", "OCR 识字", "拍照提取文字", Icons.Rounded.TextFields, tint = { it.orange }, permissions = listOf("android.permission.CAMERA")),
        ToolDef("tool/doc_scan", "文档扫描", "透视矫正多页 PDF", Icons.Rounded.Scanner, tint = { it.green }, permissions = listOf("android.permission.CAMERA")),
        ToolDef("tool/pdf_tools", "PDF 工具箱", "合并拆分抽页", Icons.Rounded.PictureAsPdf) { it.indigo },
        ToolDef("tool/signature", "电子签名", "手写签名透明 PNG", Icons.Rounded.Draw) { it.accent },
        ToolDef("tool/speech_to_text", "语音转文字", "实时速记", Icons.Rounded.KeyboardVoice, tint = { it.teal }, permissions = listOf("android.permission.RECORD_AUDIO")),
        ToolDef("tool/pomodoro", "番茄钟", "专注短休长休", Icons.Rounded.Timer) { it.purple },
        ToolDef("tool/notes", "便签与清单", "Markdown 便签", Icons.Rounded.StickyNote2) { it.pink },
        ToolDef("tool/countdown", "倒计时秒表", "多组倒计时", Icons.Rounded.AvTimer) { it.red },
        ToolDef("tool/counter", "计数器", "多计数器并行", Icons.Rounded.PlusOne) { it.orange },
        ToolDef("tool/teleprompter", "提词器", "悬浮滚动镜像", Icons.Rounded.Subtitles) { it.green },
        ToolDef("tool/random_group", "随机分组点名", "名单分组点名", Icons.Rounded.Groups) { it.indigo },
        ToolDef("tool/whiteboard", "白板画板", "手写板涂鸦", Icons.Rounded.Brush) { it.accent },
        ToolDef("tool/file_transfer", "传输助手", "文字文件时间线", Icons.Rounded.Send) { it.teal },
        ToolDef("tool/batch_rename", "批量重命名", "文件批量改名", Icons.Rounded.DriveFileRenameOutline) { it.purple },
        ToolDef("tool/zip", "压缩解压", "zip 打包解压", Icons.Rounded.FolderZip) { it.pink }
    )),

    // 6.12 生活日常（22）
    ToolCategory("生活日常", listOf(
        ToolDef("tool/weather", "天气", "7 日逐小时空气质量", Icons.Rounded.WbSunny, tint = { it.orange }, requiresNetwork = true, permissions = listOf("android.permission.ACCESS_FINE_LOCATION")),
        ToolDef("tool/history_today", "历史上的今天", "今天发生过什么", Icons.Rounded.HistoryEdu, tint = { it.green }, requiresNetwork = true),
        ToolDef("tool/holiday", "假期日历", "放假调休倒计时", Icons.Rounded.CalendarMonth) { it.indigo },
        ToolDef("tool/lunar", "农历黄历", "农历节气宜忌", Icons.Rounded.CalendarToday) { it.accent },
        ToolDef("tool/astronomy", "天文时刻", "日出日落月相", Icons.Rounded.DarkMode) { it.teal },
        ToolDef("tool/countdown_day", "倒数日", "纪念日考试倒计时", Icons.Rounded.Today) { it.purple },
        ToolDef("tool/health_remind", "健康提醒", "喝水吃药久坐提醒", Icons.Rounded.NotificationImportant, tint = { it.pink }, permissions = listOf("android.permission.POST_NOTIFICATIONS")),
        ToolDef("tool/health_record", "健康记录", "体重血压血糖", Icons.Rounded.MonitorWeight) { it.red },
        ToolDef("tool/period", "经期记录", "周期记录预测", Icons.Rounded.FavoriteBorder) { it.orange },
        ToolDef("tool/bookkeeping", "极简记账", "三秒记一笔", Icons.Rounded.Wallet) { it.green },
        ToolDef("tool/parking", "停车助手", "拍照定位找车", Icons.Rounded.LocalParking, tint = { it.indigo }, permissions = listOf("android.permission.ACCESS_FINE_LOCATION")),
        ToolDef("tool/phone_location", "归属地查询", "手机号车牌身份证", Icons.Rounded.LocationCity) { it.accent },
        ToolDef("tool/garbage", "垃圾分类", "物品搜索该扔哪", Icons.Rounded.DeleteOutline) { it.teal },
        ToolDef("tool/mirror", "镜子", "前摄镜子补光", Icons.Rounded.PhotoCamera, tint = { it.purple }, permissions = listOf("android.permission.CAMERA")),
        ToolDef("tool/magnifier", "放大镜", "后摄放大手电", Icons.Rounded.Search, tint = { it.pink }, permissions = listOf("android.permission.CAMERA")),
        ToolDef("tool/ruler", "屏幕测量", "屏幕直尺量角器", Icons.Rounded.Straighten) { it.red },
        ToolDef("tool/big_clock", "大字时钟", "床头翻页钟", Icons.Rounded.AccessTime) { it.orange },
        ToolDef("tool/emergency_card", "急救信息卡", "血型过敏史紧急联系人", Icons.Rounded.LocalHospital) { it.green },
        ToolDef("tool/move_car", "挪车码", "挪车提示牌", Icons.Rounded.DirectionsCar) { it.indigo },
        ToolDef("tool/heart_rate", "指尖心率", "指尖盖摄像头测心率", Icons.Rounded.Favorite, tint = { it.accent }, permissions = listOf("android.permission.CAMERA")),
        ToolDef("tool/breath", "呼吸放松", "4-7-8 呼吸引导", Icons.Rounded.Air) { it.teal },
        ToolDef("tool/vision_test", "视力与色觉", "对数视力表色觉图", Icons.Rounded.RemoveRedEye) { it.purple }
    )),

    // 6.13 整活娱乐（12）
    ToolCategory("整活娱乐", listOf(
        ToolDef("tool/banner", "弹幕横幅", "演唱会举牌神器", Icons.Rounded.Campaign) { it.pink },
        ToolDef("tool/decider", "决定神器", "选项轮盘抛硬币", Icons.Rounded.Celebration) { it.yellow },
        ToolDef("tool/fidget", "解压玩具", "泡泡纸指尖陀螺", Icons.Rounded.Toys) { it.orange },
        ToolDef("tool/party_games", "聚会游戏盒", "谁是卧底真心话", Icons.Rounded.SportsEsports) { it.green },
        ToolDef("tool/wooden_fish", "电子木鱼", "敲击功德 +1", Icons.Rounded.SelfImprovement) { it.indigo },
        ToolDef("tool/fireworks", "口袋烟花", "点击放烟花", Icons.Rounded.Celebration) { it.accent },
        ToolDef("tool/tarot", "塔罗抽牌", "每日一抽", Icons.Rounded.AutoAwesome) { it.teal },
        ToolDef("tool/hitokoto", "每日一言", "一言彩虹屁毒鸡汤", Icons.Rounded.FormatQuote, tint = { it.purple }, requiresNetwork = true),
        ToolDef("tool/reaction_test", "反应测试", "毫秒级反应速度", Icons.Rounded.Speed) { it.pink },
        ToolDef("tool/classic_games", "经典小游戏", "2048 扫雷数独", Icons.Rounded.VideogameAsset) { it.red },
        ToolDef("tool/typing_test", "打字测速", "中英文打字速度", Icons.Rounded.Keyboard) { it.orange },
        ToolDef("tool/math_training", "速算训练", "口算练习 24 点", Icons.Rounded.Calculate) { it.green }
    ))
)

