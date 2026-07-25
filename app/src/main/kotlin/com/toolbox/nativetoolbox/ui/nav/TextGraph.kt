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
    composable("tool/cn_convert") { CnConvertToolScreen(back) }
    composable("tool/pinyin") { PinyinToolScreen(back) }
    composable("tool/translate") { TranslateToolScreen(back) }
    composable("tool/morse") { MorseToolScreen(back) }
    composable("tool/fullwidth") { FullWidthToolScreen(back) }
    composable("tool/fancy_text") { FancyTextToolScreen(back) }
    composable("tool/zero_width") { ZeroWidthToolScreen(back) }
    composable("tool/vertical_text") { VerticalTextToolScreen(back) }
    composable("tool/text_format") { TextFormatToolScreen(back) }
    composable("tool/emoji_lib") { EmojiLibToolScreen(back) }
    composable("tool/mask_sensitive") { MaskSensitiveToolScreen(back) }
    composable("tool/clipboard_shelf") { ClipboardShelfToolScreen(back) }
    composable("tool/text_template") { TextTemplateToolScreen(back) }
}
