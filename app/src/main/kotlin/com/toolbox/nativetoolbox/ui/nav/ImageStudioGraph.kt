package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 图片创作分类路由(11 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.imageStudioToolsGraph(back: () -> Unit) {
    composable("tool/qrcode") { QrToolScreen(back) }
    composable("tool/wifiqr") { WifiQrToolScreen(back) }
    composable("tool/watermark") { WatermarkToolScreen(back) }
    composable("tool/gridcut") { GridCutToolScreen(back) }
    composable("tool/stitch") { StitchToolScreen(back) }
    composable("tool/gif_make") { PlaceholderToolScreen("GIF 制作", back) }
    composable("tool/barcode") { PlaceholderToolScreen("条形码", back) }
    composable("tool/ascii_art") { PlaceholderToolScreen("艺术化转换", back) }
    composable("tool/meme_maker") { PlaceholderToolScreen("表情包制作", back) }
    composable("tool/checkin_watermark") { PlaceholderToolScreen("打卡水印", back) }
    composable("tool/color_scheme") { PlaceholderToolScreen("配色方案", back) }
}
