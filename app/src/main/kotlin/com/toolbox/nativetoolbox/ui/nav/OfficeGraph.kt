package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 效率办公分类路由(15 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.officeToolsGraph(back: () -> Unit) {
    composable("tool/ocr") { OcrToolScreen(back) }
    composable("tool/doc_scan") { DocScanToolScreen(back) }
    composable("tool/pdf_tools") { PdfToolsToolScreen(back) }
    composable("tool/signature") { SignatureToolScreen(back) }
    composable("tool/speech_to_text") { SpeechToTextToolScreen(back) }
    composable("tool/pomodoro") { PomodoroToolScreen(back) }
    composable("tool/notes") { NotesToolScreen(back) }
    composable("tool/countdown") { CountdownToolScreen(back) }
    composable("tool/counter") { CounterToolScreen(back) }
    composable("tool/teleprompter") { TeleprompterToolScreen(back) }
    composable("tool/random_group") { RandomGroupToolScreen(back) }
    composable("tool/whiteboard") { WhiteboardToolScreen(back) }
    composable("tool/file_transfer") { FileTransferToolScreen(back) }
    composable("tool/batch_rename") { BatchRenameToolScreen(back) }
    composable("tool/zip") { ZipToolScreen(back) }
}
