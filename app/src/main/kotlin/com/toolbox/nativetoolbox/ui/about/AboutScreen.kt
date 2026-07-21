package com.toolbox.nativetoolbox.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.toolbox.nativetoolbox.ui.components.*
import com.toolbox.nativetoolbox.ui.theme.*

@Composable
fun AboutScreen(
    viewModel: AboutViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    // 星星旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "star_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        viewModel.checkUpdate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(AstroSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(AstroSpacing.xl))

        // 应用图标 - 带动画的星星
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .rotate(rotation)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(AstroSpacing.lg))

        // 应用名称
        Text(
            text = "Astro Kit",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(AstroSpacing.xs))

        StatusChip(
            text = "v3.0.0",
            type = StatusType.INFO
        )

        Spacer(modifier = Modifier.height(AstroSpacing.xxl))

        // 版本信息卡片
        AstroCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前版本",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "v3.0.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = uiState.isCheckingUpdate,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(AstroSpacing.sm))
                    AstroIndeterminateProgressBar()
                }
            }

            uiState.latestVersion?.let { version ->
                Spacer(modifier = Modifier.height(AstroSpacing.sm))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(AstroSpacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最新版本",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
                    ) {
                        Text(
                            text = version,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (version != "v3.0.0") {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        if (version != "v3.0.0") {
                            StatusChip(
                                text = "可更新",
                                type = StatusType.WARNING
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AstroSpacing.md))

        // GitHub 按钮
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://github.com/Beicho/native-toolbox")
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Language, contentDescription = null)
            Spacer(modifier = Modifier.width(AstroSpacing.xs))
            Text("访问 GitHub 仓库")
        }

        Spacer(modifier = Modifier.height(AstroSpacing.xl))

        // 功能特性
        AstroCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = "✨ 功能特性",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(AstroSpacing.md))

            FeatureItem(
                icon = Icons.Default.Code,
                title = "编码转换",
                description = "智能检测文件编码，支持 UTF-8、GBK、Big5 等多种格式批量转换"
            )
            Spacer(modifier = Modifier.height(AstroSpacing.sm))
            FeatureItem(
                icon = Icons.Default.MenuBook,
                title = "小说下载",
                description = "支持番茄小说搜索与高速下载，实时进度显示，保存为 TXT 格式"
            )
            Spacer(modifier = Modifier.height(AstroSpacing.sm))
            FeatureItem(
                icon = Icons.Default.Palette,
                title = "Material You",
                description = "动态取色系统，完美适配深色模式，流畅的动画体验"
            )
            Spacer(modifier = Modifier.height(AstroSpacing.sm))
            FeatureItem(
                icon = Icons.Default.Speed,
                title = "原生性能",
                description = "Kotlin + Jetpack Compose 打造，体积小速度快，流畅丝滑"
            )
        }

        Spacer(modifier = Modifier.height(AstroSpacing.md))

        // 免责声明
        AstroCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AstroSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "免责声明",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(AstroSpacing.sm))

            Text(
                text = "本应用仅供学习交流使用，下载的小说内容版权归原作者所有。请勿用于商业用途，下载后请在 24 小时内删除。使用本应用产生的一切后果由使用者自行承担。",
                style = MaterialTheme.typography.bodySmall,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5f
            )
        }

        Spacer(modifier = Modifier.height(AstroSpacing.xxl))

        // 版权信息
        Text(
            text = "© 2024-2026 Astro Kit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(AstroSpacing.md))
    }
}

@Composable
fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AstroSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AstroSpacing.xxs))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4f
            )
        }
    }
}
