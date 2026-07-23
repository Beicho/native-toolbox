package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 文本分类路由(16 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.textToolsGraph(back: () -> Unit) {
    composable("tool/textprocess") { TextProcessToolScreen(back) }
    composable("tool/textstats") { TextStatsToolScreen(back) }
    composable("tool/encoding") { EncodingScreen(back) }
    composable("tool/cn_convert") { PlaceholderToolScreen("繁简转换", back) }
    composable("tool/pinyin") { PlaceholderToolScreen("拼音标注", back) }
    composable("tool/translate") { PlaceholderToolScreen("翻译与词典", back) }
    composable("tool/morse") { PlaceholderToolScreen("摩斯电码", back) }
    composable("tool/fullwidth") { PlaceholderToolScreen("全角半角", back) }
    composable("tool/fancy_text") { PlaceholderToolScreen("花式文字", back) }
    composable("tool/zero_width") { PlaceholderToolScreen("零宽隐写", back) }
    composable("tool/vertical_text") { PlaceholderToolScreen("竖排古风", back) }
    composable("tool/text_format") { PlaceholderToolScreen("文案排版", back) }
    composable("tool/emoji_lib") { PlaceholderToolScreen("表情符号库", back) }
    composable("tool/mask_sensitive") { PlaceholderToolScreen("敏感信息打码", back) }
    composable("tool/clipboard_shelf") { PlaceholderToolScreen("剪贴板暂存架", back) }
    composable("tool/text_template") { PlaceholderToolScreen("文本模板", back) }
}
