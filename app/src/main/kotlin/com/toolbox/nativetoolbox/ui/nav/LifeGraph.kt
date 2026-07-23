package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 生活日常分类路由(22 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.lifeToolsGraph(back: () -> Unit) {
    composable("tool/weather") { PlaceholderToolScreen("天气", back) }
    composable("tool/history_today") { PlaceholderToolScreen("历史上的今天", back) }
    composable("tool/holiday") { PlaceholderToolScreen("假期日历", back) }
    composable("tool/lunar") { PlaceholderToolScreen("农历黄历", back) }
    composable("tool/astronomy") { PlaceholderToolScreen("天文时刻", back) }
    composable("tool/countdown_day") { PlaceholderToolScreen("倒数日", back) }
    composable("tool/health_remind") { PlaceholderToolScreen("健康提醒", back) }
    composable("tool/health_record") { PlaceholderToolScreen("健康记录", back) }
    composable("tool/period") { PlaceholderToolScreen("经期记录", back) }
    composable("tool/bookkeeping") { PlaceholderToolScreen("极简记账", back) }
    composable("tool/parking") { PlaceholderToolScreen("停车助手", back) }
    composable("tool/phone_location") { PlaceholderToolScreen("归属地查询", back) }
    composable("tool/garbage") { PlaceholderToolScreen("垃圾分类", back) }
    composable("tool/mirror") { PlaceholderToolScreen("镜子", back) }
    composable("tool/magnifier") { PlaceholderToolScreen("放大镜", back) }
    composable("tool/ruler") { PlaceholderToolScreen("屏幕测量", back) }
    composable("tool/big_clock") { PlaceholderToolScreen("大字时钟", back) }
    composable("tool/emergency_card") { PlaceholderToolScreen("急救信息卡", back) }
    composable("tool/move_car") { PlaceholderToolScreen("挪车码", back) }
    composable("tool/heart_rate") { PlaceholderToolScreen("指尖心率", back) }
    composable("tool/breath") { PlaceholderToolScreen("呼吸放松", back) }
    composable("tool/vision_test") { PlaceholderToolScreen("视力与色觉", back) }
}
