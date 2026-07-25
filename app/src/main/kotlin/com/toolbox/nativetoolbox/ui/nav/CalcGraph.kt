package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 计算换算分类路由(21 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.calcToolsGraph(back: () -> Unit) {
    composable("tool/unit") { UnitToolScreen(back) }
    composable("tool/datecalc") { DateCalcToolScreen(back) }
    composable("tool/sci_calc") { SciCalcToolScreen(back) }
    composable("tool/exchange") { PlaceholderToolScreen("汇率换算", back) }
    composable("tool/mortgage") { MortgageToolScreen(back) }
    composable("tool/interest") { PlaceholderToolScreen("利息与利率", back) }
    composable("tool/tax") { TaxToolScreen(back) }
    composable("tool/amount_upper") { AmountUpperToolScreen(back) }
    composable("tool/percent") { PercentToolScreen(back) }
    composable("tool/combo_calc") { PlaceholderToolScreen("凑单计算", back) }
    composable("tool/price_compare") { PlaceholderToolScreen("比价计算", back) }
    composable("tool/cost_split") { CostSplitToolScreen(back) }
    composable("tool/relative_name") { PlaceholderToolScreen("亲戚称呼", back) }
    composable("tool/health_calc") { HealthCalcToolScreen(back) }
    composable("tool/timezone") { PlaceholderToolScreen("时区对照", back) }
    composable("tool/random_num") { PlaceholderToolScreen("随机数", back) }
    composable("tool/statistics") { StatisticsToolScreen(back) }
    composable("tool/geometry") { PlaceholderToolScreen("几何计算", back) }
    composable("tool/fuel_calc") { PlaceholderToolScreen("油耗计算", back) }
    composable("tool/pace_calc") { PaceCalcToolScreen(back) }
    composable("tool/decoration") { PlaceholderToolScreen("装修计算", back) }
}
