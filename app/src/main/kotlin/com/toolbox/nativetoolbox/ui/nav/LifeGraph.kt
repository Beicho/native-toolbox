package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 生活日常分类路由(22 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.lifeToolsGraph(back: () -> Unit) {
    composable("tool/weather") { WeatherToolScreen(back) }
    composable("tool/history_today") { HistoryTodayToolScreen(back) }
    composable("tool/holiday") { HolidayToolScreen(back) }
    composable("tool/lunar") { LunarToolScreen(back) }
    composable("tool/astronomy") { AstronomyToolScreen(back) }
    composable("tool/countdown_day") { CountdownDayToolScreen(back) }
    composable("tool/health_remind") { HealthRemindToolScreen(back) }
    composable("tool/health_record") { HealthRecordToolScreen(back) }
    composable("tool/period") { PeriodToolScreen(back) }
    composable("tool/bookkeeping") { BookkeepingToolScreen(back) }
    composable("tool/parking") { ParkingToolScreen(back) }
    composable("tool/phone_location") { PhoneLocationToolScreen(back) }
    composable("tool/garbage") { GarbageToolScreen(back) }
    composable("tool/mirror") { MirrorToolScreen(back) }
    composable("tool/magnifier") { MagnifierToolScreen(back) }
    composable("tool/ruler") { RulerToolScreen(back) }
    composable("tool/big_clock") { BigClockToolScreen(back) }
    composable("tool/emergency_card") { EmergencyCardToolScreen(back) }
    composable("tool/move_car") { MoveCarToolScreen(back) }
    composable("tool/heart_rate") { HeartRateToolScreen(back) }
    composable("tool/breath") { BreathToolScreen(back) }
    composable("tool/vision_test") { VisionTestToolScreen(back) }
}
