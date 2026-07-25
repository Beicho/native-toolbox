package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 生活日常分类路由(22 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.lifeToolsGraph(back: () -> Unit) {
    composable("tool/weather") { WeatherToolScreen(back) }
    composable("tool/history_today") { PlaceholderToolScreen("历史上的今天", back) }
    composable("tool/holiday") { HolidayToolScreen(back) }
    composable("tool/lunar") { LunarToolScreen(back) }
    composable("tool/astronomy") { PlaceholderToolScreen("天文时刻", back) }
    composable("tool/countdown_day") { CountdownDayToolScreen(back) }
    composable("tool/health_remind") { PlaceholderToolScreen("健康提醒", back) }
    composable("tool/health_record") { PlaceholderToolScreen("健康记录", back) }
    composable("tool/period") { PlaceholderToolScreen("经期记录", back) }
    composable("tool/bookkeeping") { BookkeepingToolScreen(back) }
    composable("tool/parking") { PlaceholderToolScreen("停车助手", back) }
    composable("tool/phone_location") { PlaceholderToolScreen("归属地查询", back) }
    composable("tool/garbage") { GarbageToolScreen(back) }
    composable("tool/mirror") { MirrorToolScreen(back) }
    composable("tool/magnifier") { MagnifierToolScreen(back) }
    composable("tool/ruler") { PlaceholderToolScreen("屏幕测量", back) }
    composable("tool/big_clock") { BigClockToolScreen(back) }
    composable("tool/emergency_card") { EmergencyCardToolScreen(back) }
    composable("tool/move_car") { MoveCarToolScreen(back) }
    composable("tool/heart_rate") { PlaceholderToolScreen("指尖心率", back) }
    composable("tool/breath") { BreathToolScreen(back) }
    composable("tool/vision_test") { PlaceholderToolScreen("视力与色觉", back) }
}
