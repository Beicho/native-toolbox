package com.toolbox.nativetoolbox.ui.encoding

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.nativetoolbox.data.model.EncodingFile
import com.toolbox.nativetoolbox.data.model.EncodingType
import com.toolbox.nativetoolbox.data.model.FileStatus
import com.toolbox.nativetoolbox.ui.components.*
import com.toolbox.nativetoolbox.ui.theme.*
import com.toolbox.nativetoolbox.util.FileHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncodingScreen(
    viewModel: EncodingViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.addFiles(context, uris)
    }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            filePickerLauncher.launch("*/*")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AstroSpacing.md)
    ) {
        // 顶部控制卡片
        AstroCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 目标编码选择
            Text(
                text = "目标编码",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AstroSpacing.sm))

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.targetEncoding.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    EncodingType.entries.forEach { encoding ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    encoding.displayName,
                                    fontWeight = if (encoding == uiState.targetEncoding)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.setTargetEncoding(encoding)
                                expanded = false
                            },
                            leadingIcon = if (encoding == uiState.targetEncoding) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AstroSpacing.md))

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            filePickerLauncher.launch("*/*")
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        } else {
                            filePickerLauncher.launch("*/*")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(AstroSpacing.xxs))
                    Text("添加")
                }

                OutlinedButton(
                    onClick = { viewModel.clearFiles() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(AstroSpacing.xxs))
                    Text("清空")
                }
            }

            Spacer(modifier = Modifier.height(AstroSpacing.xs))

            Button(
                onClick = { viewModel.convertFiles(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.files.isNotEmpty() && !uiState.isConverting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                AnimatedContent(
                    targetState = uiState.isConverting,
                    label = "convert_button"
                ) { isConverting ->
                    if (isConverting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                            Text("转换中...")
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("开始转换")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AstroSpacing.md))

        // 文件列表
        AnimatedVisibility(
            visible = uiState.files.isEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            AstroEmptyState(
                icon = Icons.Default.Folder,
                title = "暂无文件",
                description = "点击\"添加\"按钮选择要转换的文件"
            )
        }

        AnimatedVisibility(
            visible = uiState.files.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
            ) {
                itemsIndexed(
                    items = uiState.files,
                    key = { _, file -> file.uri }
                ) { index, file ->
                    FileItemCard(
                        file = file,
                        index = index
                    )
                }
            }
        }
    }

    // 消息提示
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }
}

@Composable
fun FileItemCard(
    file: EncodingFile,
    index: Int
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 50L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AstroDuration.normal.inWholeMilliseconds.toInt(),
                easing = AstroEasing.emphasized
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = AstroDuration.normal.inWholeMilliseconds.toInt(),
                easing = AstroEasing.emphasized
            ),
            initialOffsetY = { it / 4 }
        )
    ) {
        AstroCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = when (file.status) {
                FileStatus.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
                FileStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(AstroSpacing.xxs))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = FileHelper.formatFileSize(file.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusChip(
                            text = file.detectedEncoding,
                            type = StatusType.INFO
                        )
                    }
                }

                Spacer(modifier = Modifier.width(AstroSpacing.sm))

                when (file.status) {
                    FileStatus.CONVERTING -> {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    FileStatus.SUCCESS -> {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    FileStatus.ERROR -> {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
