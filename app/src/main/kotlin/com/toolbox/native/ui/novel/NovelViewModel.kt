package com.toolbox.native.ui.novel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.native.data.api.RetrofitClient
import com.toolbox.native.data.model.ChapterItem
import com.toolbox.native.data.model.NovelDetail
import com.toolbox.native.data.model.SearchResult
import com.toolbox.native.util.FileHelper
import com.toolbox.native.util.NovelUrlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

data class NovelUiState(
    val currentNovel: NovelDetail? = null,
    val chapters: List<ChapterItem>? = null,
    val isParsing: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedChapters: Int = 0,
    val totalChapters: Int = 0,
    val logs: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val searchResults: List<SearchResult> = emptyList()
)

class NovelViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NovelUiState())
    val uiState: StateFlow<NovelUiState> = _uiState.asStateFlow()

    private val api = RetrofitClient.novelApi

    fun parseNovelUrl(url: String) {
        val bookId = NovelUrlParser.parseBookId(url)
        if (bookId == null) {
            addLog("[错误] 无法解析书籍ID")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isParsing = true, logs = emptyList()) }
            addLog("[解析] 开始解析书籍 ID: $bookId")

            try {
                // 获取书籍详情
                val detail = withContext(Dispatchers.IO) {
                    api.getBookDetail(bookId)
                }
                addLog("[成功] 书名: ${detail.bookName}")
                addLog("[成功] 作者: ${detail.author}")

                // 获取章节目录
                val directory = withContext(Dispatchers.IO) {
                    api.getChapterDirectory(bookId)
                }
                addLog("[成功] 章节总数: ${directory.itemList.size}")

                _uiState.update { state ->
                    state.copy(
                        currentNovel = detail,
                        chapters = directory.itemList,
                        isParsing = false,
                        totalChapters = directory.itemList.size
                    )
                }
            } catch (e: Exception) {
                addLog("[错误] 解析失败: ${e.message}")
                _uiState.update { it.copy(isParsing = false) }
            }
        }
    }

    fun downloadNovel(isHighSpeed: Boolean) {
        val chapters = _uiState.value.chapters ?: return
        val novel = _uiState.value.currentNovel ?: return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isDownloading = true,
                    downloadProgress = 0f,
                    downloadedChapters = 0
                )
            }

            addLog("[开始] ${if (isHighSpeed) "高速" else "降速"}下载模式")
            addLog("[开始] 下载《${novel.bookName}》共 ${chapters.size} 章")

            val content = StringBuilder()
            content.append("《${novel.bookName}》\n")
            content.append("作者：${novel.author}\n")
            content.append("=" .repeat(50)).append("\n\n")

            try {
                if (isHighSpeed) {
                    downloadHighSpeed(chapters, content)
                } else {
                    downloadLowSpeed(chapters, content)
                }

                // 保存文件
                val fileName = "${novel.bookName}_${novel.author}.txt"
                val bytes = content.toString().toByteArray(Charsets.UTF_8)

                withContext(Dispatchers.IO) {
                    val result = FileHelper.saveToDownloads(
                        getApplication(),
                        fileName,
                        bytes
                    )

                    if (result.isSuccess) {
                        addLog("[完成] 已保存到: ${result.getOrNull()}")
                    } else {
                        addLog("[错误] 保存失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                addLog("[错误] 下载失败: ${e.message}")
            } finally {
                _uiState.update { it.copy(isDownloading = false) }
            }
        }
    }

    private suspend fun downloadHighSpeed(
        chapters: List<ChapterItem>,
        content: StringBuilder
    ) {
        val semaphore = Semaphore(10) // 并发数限制
        val chapterContents = mutableMapOf<Int, String>()

        withContext(Dispatchers.IO) {
            chapters.forEachIndexed { index, chapter ->
                launch {
                    semaphore.acquire()
                    try {
                        val chapterContent = api.getChapterContent(chapter.itemId)
                        synchronized(chapterContents) {
                            chapterContents[index] = buildString {
                                append("第${index + 1}章 ${chapter.title}\n")
                                append("-".repeat(50)).append("\n")
                                append(chapterContent.content).append("\n\n")
                            }
                        }

                        _uiState.update { state ->
                            val downloaded = state.downloadedChapters + 1
                            state.copy(
                                downloadedChapters = downloaded,
                                downloadProgress = downloaded.toFloat() / chapters.size
                            )
                        }

                        if ((index + 1) % 10 == 0 || index == chapters.size - 1) {
                            addLog("[进度] 已下载 ${index + 1}/${chapters.size} 章")
                        }
                    } catch (e: Exception) {
                        addLog("[警告] 章节 ${chapter.title} 下载失败")
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }

        // 按顺序拼接内容
        chapters.indices.forEach { index ->
            chapterContents[index]?.let { content.append(it) }
        }
    }

    private suspend fun downloadLowSpeed(
        chapters: List<ChapterItem>,
        content: StringBuilder
    ) {
        withContext(Dispatchers.IO) {
            chapters.forEachIndexed { index, chapter ->
                try {
                    val chapterContent = api.getChapterContent(chapter.itemId)

                    content.append("第${index + 1}章 ${chapter.title}\n")
                    content.append("-".repeat(50)).append("\n")
                    content.append(chapterContent.content).append("\n\n")

                    _uiState.update { state ->
                        val downloaded = state.downloadedChapters + 1
                        state.copy(
                            downloadedChapters = downloaded,
                            downloadProgress = downloaded.toFloat() / chapters.size
                        )
                    }

                    if ((index + 1) % 10 == 0 || index == chapters.size - 1) {
                        addLog("[进度] 已下载 ${index + 1}/${chapters.size} 章")
                    }

                    // 降速：每章延迟 100ms
                    kotlinx.coroutines.delay(100)
                } catch (e: Exception) {
                    addLog("[警告] 章节 ${chapter.title} 下载失败")
                }
            }
        }
    }

    fun searchNovels(keyword: String) {
        if (keyword.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList()) }
            addLog("[搜索] 关键词: $keyword")

            try {
                val response = withContext(Dispatchers.IO) {
                    api.searchBooks(keyword)
                }

                _uiState.update { state ->
                    state.copy(
                        isSearching = false,
                        searchResults = response.data
                    )
                }

                addLog("[搜索] 找到 ${response.data.size} 个结果")
            } catch (e: Exception) {
                addLog("[错误] 搜索失败: ${e.message}")
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    private fun addLog(message: String) {
        _uiState.update { state ->
            state.copy(logs = state.logs + message)
        }
    }
}
