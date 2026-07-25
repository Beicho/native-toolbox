package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.encoding.EncodingScreen
import com.toolbox.nativetoolbox.ui.tools.*

/** 文本分类路由(16 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.textToolsGraph(back: () -> Unit) {
    composable("tool/textprocess") { TextProcessToolScreen(back) }
    composable("tool/textstats") { TextStatsToolScreen(back) }
    composable("tool/encoding") { EncodingScreen(back) }
    composable("tool/cn_convert") { PlaceholderToolScreen("繁简转换", back) }
    composable("tool/pinyin") { PlaceholderToolScreen("拼音标注", back) }
    composable("tool/translate") { TranslateToolScreen(back) }
    composable("tool/morse") { MorseToolScreen(back) }
    composable("tool/fullwidth") { FullWidthToolScreen(back) }
    composable("tool/fancy_text") { FancyTextToolScreen(back) }
    composable("tool/zero_width") { ZeroWidthToolScreen(back) }
    composable("tool/vertical_text") { VerticalTextToolScreen(back) }
    composable("tool/text_format") { TextFormatToolScreen(back) }
    composable("tool/emoji_lib") { PlaceholderToolScreen("表情符号库", back) }
    composable("tool/mask_sensitive") { MaskSensitiveToolScreen(back) }
    composable("tool/clipboard_shelf") { PlaceholderToolScreen("剪贴板暂存架", back) }
    composable("tool/text_template") { PlaceholderToolScreen("文本模板", back) }
}
