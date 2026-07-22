package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val bannerColors = listOf(
    Color(0xFFFF3B30) to Color.White,
    Color(0xFF000000) to Color(0xFF34FF6A),
    Color(0xFF007AFF) to Color.White,
    Color(0xFFFFD60A) to Color.Black,
    Color(0xFFFFFFFF) to Color.Black,
    Color(0xFFFF2D55) to Color.White
)

@Composable
fun BannerToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var text by rememberSaveable { mutableStateOf("前方高能!!") }
    var colorIndex by rememberSaveable { mutableStateOf(0) }
    var speedIndex by rememberSaveable { mutableStateOf(1) }
    var scroll by rememberSaveable { mutableStateOf(true) }
    var fullscreen by remember { mutableStateOf(false) }

    if (fullscreen) {
        // 全屏横幅:横向滚动大字,点击退出(把手机横过来举)
        val (bg, fg) = bannerColors[colorIndex]
        var boxWidth by remember { mutableStateOf(1) }
        val transition = rememberInfiniteTransition(label = "banner")
        val progress by transition.animateFloat(
            initialValue = 1f,
            targetValue = -1f,
            animationSpec = infiniteRepeatable(
                tween(listOf(9000, 6000, 3500)[speedIndex], easing = LinearEasing)
            ),
            label = "bannerX"
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .onSizeChanged { boxWidth = it.width }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { fullscreen = false },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text.ifBlank { "…" },
                modifier = Modifier.graphicsLayer {
                    rotationZ = 90f
                    if (scroll) translationY = progress * boxWidth * 1.4f
                },
                color = fg,
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible
            )
        }
        return
    }

    ToolScaffold {
        item { SectionHeader("弹幕横幅(演唱会 · 接机 · 加油打气)") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(value = text, onValueChange = { text = it }, placeholder = "要喊的话…")
                    Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                        bannerColors.forEachIndexed { index, (bg, _) ->
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .border(
                                        width = if (index == colorIndex) 3.dp else 1.dp,
                                        color = if (index == colorIndex) palette.accent else palette.separator.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { colorIndex = index }
                            )
                        }
                    }
                    SegmentedPicker(
                        options = listOf("慢速", "中速", "快速"),
                        selectedIndex = speedIndex,
                        onSelected = { speedIndex = it }
                    )
                    SegmentedPicker(
                        options = listOf("固定显示", "滚动播放"),
                        selectedIndex = if (scroll) 1 else 0,
                        onSelected = { scroll = it == 1 }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                        SolidButton(onClick = { fullscreen = true }, enabled = text.isNotBlank()) {
                            Text("全屏展示(点屏幕退出)", color = Color.White)
                        }
                    }
                }
            }
        }
        item { SectionHeader("预览") }
        item {
            GroupedCard {
                CardPadding {
                    val (bg, fg) = bannerColors[colorIndex]
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text.ifBlank { "…" },
                            color = fg,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
