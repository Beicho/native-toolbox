package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 整活娱乐分类路由(12 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.funToolsGraph(back: () -> Unit) {
    composable("tool/banner") { BannerToolScreen(back) }
    composable("tool/decider") { DeciderToolScreen(back) }
    composable("tool/fidget") { PlaceholderToolScreen("解压玩具", back) }
    composable("tool/party_games") { PlaceholderToolScreen("聚会游戏盒", back) }
    composable("tool/wooden_fish") { PlaceholderToolScreen("电子木鱼", back) }
    composable("tool/fireworks") { PlaceholderToolScreen("口袋烟花", back) }
    composable("tool/tarot") { PlaceholderToolScreen("塔罗抽牌", back) }
    composable("tool/hitokoto") { PlaceholderToolScreen("每日一言", back) }
    composable("tool/reaction_test") { PlaceholderToolScreen("反应测试", back) }
    composable("tool/classic_games") { PlaceholderToolScreen("经典小游戏", back) }
    composable("tool/typing_test") { PlaceholderToolScreen("打字测速", back) }
    composable("tool/math_training") { PlaceholderToolScreen("速算训练", back) }
}
