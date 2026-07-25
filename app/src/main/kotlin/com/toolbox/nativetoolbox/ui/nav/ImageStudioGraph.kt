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
    composable("tool/gif_make") { GifMakeToolScreen(back) }
    composable("tool/barcode") { BarcodeToolScreen(back) }
    composable("tool/ascii_art") { AsciiArtToolScreen(back) }
    composable("tool/meme_maker") { MemeMakerToolScreen(back) }
    composable("tool/checkin_watermark") { CheckinWatermarkToolScreen(back) }
    composable("tool/color_scheme") { ColorSchemeToolScreen(back) }
}
