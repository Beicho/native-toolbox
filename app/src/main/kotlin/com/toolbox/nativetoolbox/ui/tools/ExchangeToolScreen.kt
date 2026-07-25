package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.net.AstroApi
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import org.json.JSONObject

private val commonCurrencies = listOf(
    "CNY" to "人民币",
    "USD" to "美元",
    "EUR" to "欧元",
    "JPY" to "日元",
    "HKD" to "港币",
    "GBP" to "英镑",
    "KRW" to "韩元",
    "AUD" to "澳元",
    "CAD" to "加元",
    "SGD" to "新加坡元",
    "TWD" to "新台币",
    "THB" to "泰铢"
)

/** 全部支持的货币中文名。后端返回 160 种汇率,这里覆盖常见的几十种,
 *  没收录中文名的直接显示代码,不影响换算。 */
private val currencyNames = mapOf(
    "CNY" to "人民币", "USD" to "美元", "EUR" to "欧元", "JPY" to "日元",
    "HKD" to "港币", "GBP" to "英镑", "KRW" to "韩元", "AUD" to "澳元",
    "CAD" to "加元", "SGD" to "新加坡元", "TWD" to "新台币", "THB" to "泰铢",
    "MYR" to "马来西亚林吉特", "VND" to "越南盾", "PHP" to "菲律宾比索",
    "IDR" to "印尼卢比", "INR" to "印度卢比", "RUB" to "俄罗斯卢布",
    "CHF" to "瑞士法郎", "SEK" to "瑞典克朗", "NOK" to "挪威克朗",
    "DKK" to "丹麦克朗", "NZD" to "新西兰元", "MOP" to "澳门元",
    "BRL" to "巴西雷亚尔", "MXN" to "墨西哥比索", "ZAR" to "南非兰特",
    "TRY" to "土耳其里拉", "AED" to "阿联酋迪拉姆", "SAR" to "沙特里亚尔",
    "EGP" to "埃及镑", "ILS" to "以色列新谢克尔", "PLN" to "波兰兹罗提",
    "CZK" to "捷克克朗", "HUF" to "匈牙利福林", "RON" to "罗马尼亚列伊",
    "UAH" to "乌克兰格里夫纳", "KZT" to "哈萨克坚戈", "PKR" to "巴基斯坦卢比",
    "BDT" to "孟加拉塔卡", "LKR" to "斯里兰卡卢比", "NPR" to "尼泊尔卢比",
    "MMK" to "缅甸元", "KHR" to "柬埔寨瑞尔", "LAK" to "老挝基普",
    "MNT" to "蒙古图格里克", "ARS" to "阿根廷比索", "CLP" to "智利比索",
    "COP" to "哥伦比亚比索", "PEN" to "秘鲁索尔", "NGN" to "尼日利亚奈拉",
    "KES" to "肯尼亚先令", "GHS" to "加纳塞地", "MAD" to "摩洛哥迪拉姆",
    "XAU" to "黄金(盎司)",
)

private fun currencyLabel(code: String): String =
    currencyNames[code]?.let { "$code  $it" } ?: code

private fun fmt(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    return when {
        value >= 1000 -> String.format("%,.2f", value)
        value >= 1 -> String.format("%.4f", value)
        else -> String.format("%.6f", value)
    }
}

@Composable
fun ExchangeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val scope = rememberCoroutineScope()

    var amountText by rememberSaveable { mutableStateOf("100") }
    var from by rememberSaveable { mutableStateOf("CNY") }
    var to by rememberSaveable { mutableStateOf("USD") }
    var raw by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }

    fun load() {
        loading = true
        status = ""
        scope.launch {
            AstroApi.get("/exchange", mapOf("base" to from.uppercase()))
                .onSuccess { res ->
                    raw = res.data.toString()
                    status = cachedHint(res.cachedAt)
                }
                .onFailure { e ->
                    raw = null
                    status = e.message ?: "获取汇率失败，请检查网络"
                }
            loading = false
        }
    }

    LaunchedEffect(from) { load() }

    val json = raw?.let { runCatching { JSONObject(it) }.getOrNull() }
    val rates = json?.optJSONObject("rates") ?: json
    val updatedAt = json?.let {
        listOf("time_last_update_utc", "date", "updated").map { k -> it.optString(k) }
            .firstOrNull { s -> s.isNotBlank() && s != "null" }
    } ?: ""

    val amount = amountText.trim().replace(",", "").toDoubleOrNull() ?: 0.0
    val rate = rates?.optDouble(to.uppercase(), Double.NaN) ?: Double.NaN
    val converted = if (rate.isNaN()) Double.NaN else amount * rate

    ToolScaffold {
        item { SectionHeader("换算") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = "金额",
                        mono = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = from,
                            onValueChange = { from = it.uppercase().take(3) },
                            modifier = Modifier.weight(1f),
                            placeholder = "从",
                            mono = true
                        )
                        IosTextField(
                            value = to,
                            onValueChange = { to = it.uppercase().take(3) },
                            modifier = Modifier.weight(1f),
                            placeholder = "换成",
                            mono = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = {
                                val old = from
                                from = to
                                to = old
                            },
                            modifier = Modifier.weight(1f),
                            filled = false
                        ) { Text("对调") }
                        SolidButton(
                            onClick = { load() },
                            modifier = Modifier.weight(1f),
                            filled = false,
                            enabled = !loading
                        ) { Text(if (loading) "更新中…" else "刷新汇率") }
                    }
                    Text(
                        "汇率需要联网获取，断网时用最近一次的缓存。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCell(to.uppercase(), fmt(converted), Modifier.weight(1f))
                        StatCell("单价", if (rate.isNaN()) "—" else fmt(rate), Modifier.weight(1f))
                    }
                    if (status.isNotBlank()) {
                        Text(status, style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    } else if (rate.isNaN() && raw != null) {
                        Text(
                            "没有 " + to.uppercase() + " 这个货币代码，检查一下拼写",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    } else if (updatedAt.isNotBlank()) {
                        Text(
                            "汇率更新于 " + updatedAt,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                    }
                }
            }
        }
        if (rates != null) {
            item { SectionHeader("常用货币（1 " + from.uppercase() + " 可换）") }
            item {
                GroupedCard {
                    val list = commonCurrencies.filter { it.first != from.uppercase() }
                    list.forEachIndexed { index, (code, name) ->
                        val r = rates.optDouble(code, Double.NaN)
                        KeyValueRow(
                            code + "　" + name,
                            if (r.isNaN()) "—" else fmt(r),
                            copyable = false
                        )
                        if (index != list.lastIndex) RowDivider()
                    }
                }
            }
            item { SectionHeader("按当前金额折算") }
            item {
                GroupedCard {
                    val list = commonCurrencies.filter { it.first != from.uppercase() }
                    list.forEachIndexed { index, (code, name) ->
                        val r = rates.optDouble(code, Double.NaN)
                        KeyValueRow(
                            name,
                            if (r.isNaN()) "" else fmt(amount * r) + " " + code,
                            copyable = false
                        )
                        if (index != list.lastIndex) RowDivider()
                    }
                }
            }

            // 后端返回 160 种货币,常用之外的也列出来 —— 出国时用得上
            item { SectionHeader("全部货币（1 " + from.uppercase() + " 可换）") }
            item {
                GroupedCard {
                    val commonCodes = commonCurrencies.map { it.first }.toSet()
                    val others = rates.keys().asSequence()
                        .filterNot { commonCodes.contains(it) || it == from.uppercase() }
                        .sortedWith(compareBy({ currencyNames[it] == null }, { it }))
                        .toList()
                    others.forEachIndexed { index, code ->
                        val r = rates.optDouble(code, Double.NaN)
                        if (!r.isNaN()) {
                            KeyValueRow(currencyLabel(code), fmt(r), copyable = false)
                            if (index != others.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}
