package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 效率办公分类路由(15 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.officeToolsGraph(back: () -> Unit) {
    composable("tool/ocr") { PlaceholderToolScreen("OCR 识字", back) }
    composable("tool/doc_scan") { PlaceholderToolScreen("文档扫描", back) }
    composable("tool/pdf_tools") { PlaceholderToolScreen("PDF 工具箱", back) }
    composable("tool/signature") { PlaceholderToolScreen("电子签名", back) }
    composable("tool/speech_to_text") { PlaceholderToolScreen("语音转文字", back) }
    composable("tool/pomodoro") { PlaceholderToolScreen("番茄钟", back) }
    composable("tool/notes") { PlaceholderToolScreen("便签与清单", back) }
    composable("tool/countdown") { PlaceholderToolScreen("倒计时秒表", back) }
    composable("tool/counter") { PlaceholderToolScreen("计数器", back) }
    composable("tool/teleprompter") { PlaceholderToolScreen("提词器", back) }
    composable("tool/random_group") { PlaceholderToolScreen("随机分组点名", back) }
    composable("tool/whiteboard") { PlaceholderToolScreen("白板画板", back) }
    composable("tool/file_transfer") { PlaceholderToolScreen("传输助手", back) }
    composable("tool/batch_rename") { PlaceholderToolScreen("批量重命名", back) }
    composable("tool/zip") { PlaceholderToolScreen("压缩解压", back) }
}
