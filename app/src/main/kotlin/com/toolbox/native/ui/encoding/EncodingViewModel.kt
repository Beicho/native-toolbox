package com.toolbox.native.ui.encoding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.native.data.model.EncodingFile
import com.toolbox.native.data.model.EncodingType
import com.toolbox.native.data.model.FileStatus
import com.toolbox.native.util.EncodingDetector
import com.toolbox.native.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EncodingUiState(
    val files: List<EncodingFile> = emptyList(),
    val targetEncoding: EncodingType = EncodingType.UTF8,
    val isConverting: Boolean = false,
    val message: String? = null
)

class EncodingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EncodingUiState())
    val uiState: StateFlow<EncodingUiState> = _uiState.asStateFlow()

    fun addFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val newFiles = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    try {
                        val name = uri.lastPathSegment ?: "未知文件"
                        val size = FileHelper.getFileSize(context, uri)

                        // 检测编码
                        val detectedEncoding = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            EncodingDetector.detectEncoding(inputStream)
                        } ?: "未知"

                        EncodingFile(
                            uri = uri.toString(),
                            name = name,
                            size = size,
                            detectedEncoding = detectedEncoding,
                            targetEncoding = _uiState.value.targetEncoding.charset
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            _uiState.update { state ->
                state.copy(
                    files = state.files + newFiles,
                    message = "已添加 ${newFiles.size} 个文件"
                )
            }
        }
    }

    fun setTargetEncoding(encoding: EncodingType) {
        _uiState.update { it.copy(targetEncoding = encoding) }
    }

    fun clearFiles() {
        _uiState.update { it.copy(files = emptyList()) }
    }

    fun convertFiles(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConverting = true) }

            val targetCharset = _uiState.value.targetEncoding.charset
            var successCount = 0
            var failCount = 0

            withContext(Dispatchers.IO) {
                _uiState.value.files.forEach { file ->
                    // 更新状态为转换中
                    _uiState.update { state ->
                        state.copy(
                            files = state.files.map {
                                if (it.uri == file.uri) it.copy(status = FileStatus.CONVERTING)
                                else it
                            }
                        )
                    }

                    try {
                        // 读取文件
                        val bytes = FileHelper.readFromUri(context, Uri.parse(file.uri))
                            ?: throw Exception("无法读取文件")

                        // 转换编码
                        val convertedBytes = bytes.inputStream().use { inputStream ->
                            EncodingDetector.convertEncoding(
                                inputStream,
                                file.detectedEncoding,
                                targetCharset
                            )
                        }

                        // 保存文件
                        val newFileName = file.name.replaceAfterLast(".", "txt")
                        val result = FileHelper.saveToDownloads(context, newFileName, convertedBytes)

                        if (result.isSuccess) {
                            successCount++
                            _uiState.update { state ->
                                state.copy(
                                    files = state.files.map {
                                        if (it.uri == file.uri) it.copy(status = FileStatus.SUCCESS)
                                        else it
                                    }
                                )
                            }
                        } else {
                            throw result.exceptionOrNull() ?: Exception("保存失败")
                        }
                    } catch (e: Exception) {
                        failCount++
                        _uiState.update { state ->
                            state.copy(
                                files = state.files.map {
                                    if (it.uri == file.uri) it.copy(status = FileStatus.ERROR)
                                    else it
                                }
                            )
                        }
                    }
                }
            }

            _uiState.update { state ->
                state.copy(
                    isConverting = false,
                    message = "转换完成：成功 $successCount 个，失败 $failCount 个"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
