package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 图片编辑分类路由(12 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.imageEditToolsGraph(back: () -> Unit) {
    composable("tool/imagecompress") { ImageCompressToolScreen(back) }
    composable("tool/imageconvert") { ImageConvertToolScreen(back) }
    composable("tool/pickcolor") { PickColorToolScreen(back) }
    composable("tool/image_crop") { ImageCropToolScreen(back) }
    composable("tool/image_filter") { ImageFilterToolScreen(back) }
    composable("tool/image_mosaic") { ImageMosaicToolScreen(back) }
    composable("tool/image_border") { ImageBorderToolScreen(back) }
    composable("tool/image_annotate") { ImageAnnotateToolScreen(back) }
    composable("tool/image_matting") { PlaceholderToolScreen("智能抠图", back) }
    composable("tool/id_photo") { PlaceholderToolScreen("证件照制作", back) }
    composable("tool/image_compare") { ImageCompareToolScreen(back) }
    composable("tool/similar_clean") { PlaceholderToolScreen("相似图片清理", back) }
}
