#!/usr/bin/env python3
"""从 ToolRegistry.kt 生成 13 个按分类拆分的 NavGraph 文件,并改写 MainActivity 的 NavHost。
纯机械搬运:已实现工具→真实 Screen,未实现→PlaceholderToolScreen。运行一次即可,可重复运行(幂等)。"""
import re
import pathlib

ROOT = pathlib.Path("/root/native-toolbox/app/src/main/kotlin/com/toolbox/nativetoolbox")
REG = (ROOT / "ui/home/ToolRegistry.kt").read_text()

# 已实现工具 route → Screen composable 名(31 + base64)
IMPLEMENTED = {
    "tool/json": "JsonToolScreen", "tool/timestamp": "TimestampToolScreen",
    "tool/radix": "RadixToolScreen", "tool/base64": "Base64ToolScreen",
    "tool/url": "UrlToolScreen", "tool/uuid": "UuidToolScreen",
    "tool/regex": "RegexToolScreen", "tool/color": "ColorToolScreen",
    "tool/jwt": "JwtToolScreen", "tool/diff": "DiffToolScreen",
    "tool/cron": "CronToolScreen", "tool/hash": "HashToolScreen",
    "tool/textprocess": "TextProcessToolScreen", "tool/textstats": "TextStatsToolScreen",
    "tool/random": "RandomToolScreen", "tool/encoding": "EncodingScreen",
    "tool/qrcode": "QrToolScreen", "tool/wifiqr": "WifiQrToolScreen",
    "tool/imagecompress": "ImageCompressToolScreen", "tool/imageconvert": "ImageConvertToolScreen",
    "tool/pickcolor": "PickColorToolScreen", "tool/watermark": "WatermarkToolScreen",
    "tool/gridcut": "GridCutToolScreen", "tool/stitch": "StitchToolScreen",
    "tool/exif": "ExifToolScreen", "tool/unit": "UnitToolScreen",
    "tool/datecalc": "DateCalcToolScreen", "tool/deviceinfo": "DeviceInfoToolScreen",
    "tool/level": "LevelToolScreen", "tool/screentest": "ScreenTestToolScreen",
    "tool/banner": "BannerToolScreen", "tool/decider": "DeciderToolScreen",
}

# 分类中文名 → (Graph 函数名, 文件名)
CAT_MAP = [
    ("开发者", "devToolsGraph", "DevGraph.kt"),
    ("开发者进阶", "devProToolsGraph", "DevProGraph.kt"),
    ("网络", "networkToolsGraph", "NetworkGraph.kt"),
    ("文本", "textToolsGraph", "TextGraph.kt"),
    ("加密与隐私", "securityToolsGraph", "SecurityGraph.kt"),
    ("图片编辑", "imageEditToolsGraph", "ImageEditGraph.kt"),
    ("图片创作", "imageStudioToolsGraph", "ImageStudioGraph.kt"),
    ("音频视频", "avToolsGraph", "AvGraph.kt"),
    ("计算换算", "calcToolsGraph", "CalcGraph.kt"),
    ("设备硬件", "deviceToolsGraph", "DeviceGraph.kt"),
    ("效率办公", "officeToolsGraph", "OfficeGraph.kt"),
    ("生活日常", "lifeToolsGraph", "LifeGraph.kt"),
    ("整活娱乐", "funToolsGraph", "FunGraph.kt"),
]

# 解析每个分类的 (route, title) 列表
blocks = re.split(r'ToolCategory\(', REG)
cat_tools = {}
for b in blocks[1:]:
    mname = re.search(r'"([^"]+)"', b)
    if not mname:
        continue
    cat = mname.group(1)
    tools = re.findall(r'ToolDef\(\s*"(tool/[^"]+)"\s*,\s*"([^"]+)"', b)
    cat_tools[cat] = tools

nav_dir = ROOT / "ui/nav"
nav_dir.mkdir(parents=True, exist_ok=True)

graph_calls = []
total = 0
for cat, fn, fname in CAT_MAP:
    tools = cat_tools.get(cat, [])
    total += len(tools)
    lines = []
    for route, title in tools:
        if route in IMPLEMENTED:
            lines.append(f'    composable("{route}") {{ {IMPLEMENTED[route]}(back) }}')
        else:
            t = title.replace('"', '\\"')
            lines.append(f'    composable("{route}") {{ PlaceholderToolScreen("{t}", back) }}')
    body = "\n".join(lines)
    content = f'''package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** {cat}分类路由({len(tools)} 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.{fn}(back: () -> Unit) {{
{body}
}}
'''
    (nav_dir / fname).write_text(content)
    graph_calls.append(f"                    {fn}(back)")

print(f"生成 {len(CAT_MAP)} 个 Graph 文件,共 {total} 个路由")

# 改写 MainActivity:把 composable("tool/...") 那一大段 + 动态占位段 换成 13 行调用
MA = ROOT / "MainActivity.kt"
src = MA.read_text()

# 找到第一个 tool/ composable 到动态注册块结束
start = src.index('composable("tool/json")')
end_marker = '.forEach { tool ->'
end = src.index('}', src.index(end_marker, start))
# forEach 块后还有一个 composable{} 的闭合 }, 再找其闭合
end = src.index('}', end + 1)  # 关闭 composable(tool.route){...}
end = src.index('}', end + 1)  # 关闭 forEach
# 定位到该 forEach 语句块整体结束的换行
block_end = src.index('\n', end) + 1

new_calls = "\n".join(graph_calls) + "\n"
# 保留 import(通配 ui.nav)
if "import com.toolbox.nativetoolbox.ui.nav.*" not in src:
    src = src.replace(
        "import com.toolbox.nativetoolbox.ui.home.*",
        "import com.toolbox.nativetoolbox.ui.home.*\nimport com.toolbox.nativetoolbox.ui.nav.*",
        1,
    )
    # 因替换改变了偏移,重算
    start = src.index('composable("tool/json")')
    end = src.index('}', src.index(end_marker, start))
    end = src.index('}', end + 1)
    end = src.index('}', end + 1)
    block_end = src.index('\n', end) + 1

# 缩进对齐(composable("tool/json") 前的空白)
line_start = src.rfind('\n', 0, start) + 1
indent = src[line_start:start]
src = src[:line_start] + new_calls + src[block_end:]

MA.write_text(src)
print("MainActivity NavHost 已改写为 13 行 Graph 调用")
