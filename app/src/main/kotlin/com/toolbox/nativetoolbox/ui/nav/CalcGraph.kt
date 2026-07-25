package com.toolbox.nativetoolbox.ui.nav

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.toolbox.nativetoolbox.ui.tools.*

/** 计算换算分类路由(21 个)。并行实现时只改本文件,与其他分类零冲突。 */
fun NavGraphBuilder.calcToolsGraph(back: () -> Unit) {
    composable("tool/unit") { UnitToolScreen(back) }
    composable("tool/datecalc") { DateCalcToolScreen(back) }
    composable("tool/sci_calc") { SciCalcToolScreen(back) }
    composable("tool/exchange") { ExchangeToolScreen(back) }
    composable("tool/mortgage") { MortgageToolScreen(back) }
    composable("tool/interest") { InterestToolScreen(back) }
    composable("tool/tax") { TaxToolScreen(back) }
    composable("tool/amount_upper") { AmountUpperToolScreen(back) }
    composable("tool/percent") { PercentToolScreen(back) }
    composable("tool/combo_calc") { ComboCalcToolScreen(back) }
    composable("tool/price_compare") { PriceCompareToolScreen(back) }
    composable("tool/cost_split") { CostSplitToolScreen(back) }
    composable("tool/relative_name") { RelativeNameToolScreen(back) }
    composable("tool/health_calc") { HealthCalcToolScreen(back) }
    composable("tool/timezone") { TimezoneToolScreen(back) }
    composable("tool/random_num") { RandomNumToolScreen(back) }
    composable("tool/statistics") { StatisticsToolScreen(back) }
    composable("tool/geometry") { GeometryToolScreen(back) }
    composable("tool/fuel_calc") { FuelCalcToolScreen(back) }
    composable("tool/pace_calc") { PaceCalcToolScreen(back) }
    composable("tool/decoration") { DecorationToolScreen(back) }
}
