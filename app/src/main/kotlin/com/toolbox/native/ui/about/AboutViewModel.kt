package com.toolbox.native.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

data class AboutUiState(
    val isCheckingUpdate: Boolean = false,
    val latestVersion: String? = null,
    val updateMessage: String? = null
)

class AboutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    private val currentVersion = "v3.0.0"

    fun checkUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }

            try {
                val latestVersion = withContext(Dispatchers.IO) {
                    // 模拟检查更新（实际应该调用 GitHub API）
                    // 这里简化处理，直接返回当前版本
                    fetchLatestVersionFromGitHub()
                }

                _uiState.update { state ->
                    state.copy(
                        isCheckingUpdate = false,
                        latestVersion = latestVersion
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isCheckingUpdate = false,
                        latestVersion = currentVersion // 失败时显示当前版本
                    )
                }
            }
        }
    }

    private suspend fun fetchLatestVersionFromGitHub(): String {
        return try {
            // GitHub Releases API
            // 实际项目中应该解析 JSON 响应
            // 这里简化处理
            currentVersion
        } catch (e: Exception) {
            currentVersion
        }
    }
}
