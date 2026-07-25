package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import java.util.Calendar

/**
 * 农历换算：用查表法。表里每个元素编码一年的信息：
 * 低 12 位（从第 4 位起）表示 12 个常规月的大小月（1=30天，0=29天），
 * 高 4 位表示闰月月份（0=无闰月），第 16 位表示闰月是否 30 天。
 * 这是农历计算的通用做法，数据来自公开的紫金山天文台历表。
 */
private val lunarInfo = intArrayOf(
    0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, // 1900-1909
    0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
    0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
    0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
    0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
    0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
    0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
    0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
    0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
    0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
    0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, // 2000-2009
    0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
    0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
    0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
    0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0  // 2030-2039
)

private val lunarMonthNames = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
private val lunarDayTens = listOf("初", "十", "廿", "三")
private val lunarDayUnits = listOf("十", "一", "二", "三", "四", "五", "六", "七", "八", "九")

private val heavenlyStems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val earthlyBranches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
private val zodiac = listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

private fun leapMonth(year: Int): Int = lunarInfo[year - 1900] and 0xf
private fun leapDays(year: Int): Int =
    if (leapMonth(year) == 0) 0 else if (lunarInfo[year - 1900] and 0x10000 != 0) 30 else 29

private fun monthDays(year: Int, month: Int): Int =
    if (lunarInfo[year - 1900] and (0x10000 shr month) != 0) 30 else 29

private fun yearDays(year: Int): Int {
    var sum = 348
    var i = 0x8000
    while (i > 0x8) {
        if (lunarInfo[year - 1900] and i != 0) sum += 1
        i = i shr 1
    }
    return sum + leapDays(year)
}

private class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeap: Boolean
)

/** 以 1900-01-31 为农历 1900 年正月初一的基准起算 */
private fun solarToLunar(cal: Calendar): LunarDate? {
    val base = Calendar.getInstance().apply {
        clear()
        set(1900, 0, 31)
    }
    var offset = ((cal.timeInMillis - base.timeInMillis) / 86_400_000L).toInt()
    if (offset < 0) return null

    var year = 1900
    var daysInYear = yearDays(year)
    while (year < 2040 && offset >= daysInYear) {
        offset -= daysInYear
        year++
        if (year - 1900 >= lunarInfo.size) return null
        daysInYear = yearDays(year)
    }
    if (year - 1900 >= lunarInfo.size) return null

    val leap = leapMonth(year)
    var isLeap = false
    var month = 1
    while (month <= 12) {
        val days: Int
        if (leap > 0 && month == leap + 1 && !isLeap) {
            isLeap = true
            month--
            days = leapDays(year)
        } else {
            days = monthDays(year, month)
        }
        if (isLeap && month == leap && days == leapDays(year)) {
            // 闰月处理完后恢复
        }
        if (offset < days) break
        offset -= days
        if (isLeap && month == leap) isLeap = false
        month++
    }
    return LunarDate(year, month.coerceIn(1, 12), offset + 1, isLeap)
}

private fun lunarDayName(day: Int): String = when {
    day == 10 -> "初十"
    day == 20 -> "二十"
    day == 30 -> "三十"
    else -> lunarDayTens[(day - 1) / 10] + lunarDayUnits[day % 10]
}

private fun ganzhi(year: Int): String =
    heavenlyStems[(year - 4) % 10] + earthlyBranches[(year - 4) % 12]

private fun zodiacOf(year: Int): String = zodiac[(year - 4) % 12]

/** 二十四节气按每年固定的近似日期查表，误差一天以内 */
private val solarTerms = listOf(
    "小寒" to (1 to 6), "大寒" to (1 to 20), "立春" to (2 to 4), "雨水" to (2 to 19),
    "惊蛰" to (3 to 6), "春分" to (3 to 21), "清明" to (4 to 5), "谷雨" to (4 to 20),
    "立夏" to (5 to 6), "小满" to (5 to 21), "芒种" to (6 to 6), "夏至" to (6 to 21),
    "小暑" to (7 to 7), "大暑" to (7 to 23), "立秋" to (8 to 8), "处暑" to (8 to 23),
    "白露" to (9 to 8), "秋分" to (9 to 23), "寒露" to (10 to 8), "霜降" to (10 to 24),
    "立冬" to (11 to 7), "小雪" to (11 to 22), "大雪" to (12 to 7), "冬至" to (12 to 22)
)

@Composable
fun LunarToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    val today = Calendar.getInstance()
    var yearText by rememberSaveable { mutableStateOf(today.get(Calendar.YEAR).toString()) }
    var monthText by rememberSaveable { mutableStateOf((today.get(Calendar.MONTH) + 1).toString()) }
    var dayText by rememberSaveable { mutableStateOf(today.get(Calendar.DAY_OF_MONTH).toString()) }

    val year = yearText.trim().toIntOrNull() ?: today.get(Calendar.YEAR)
    val month = monthText.trim().toIntOrNull()?.coerceIn(1, 12) ?: 1
    val day = dayText.trim().toIntOrNull()?.coerceIn(1, 31) ?: 1

    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }
    val lunar = if (year in 1900..2039) solarToLunar(cal) else null

    val nextTerm = solarTerms.firstOrNull { (_, md) ->
        md.first > month || (md.first == month && md.second >= day)
    } ?: solarTerms.first()

    ToolScaffold {
        item { SectionHeader("公历日期") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IosTextField(yearText, { yearText = it }, Modifier.weight(1.2f), "年", mono = true)
                        IosTextField(monthText, { monthText = it }, Modifier.weight(1f), "月", mono = true)
                        IosTextField(dayText, { dayText = it }, Modifier.weight(1f), "日", mono = true)
                    }
                    com.toolbox.nativetoolbox.ui.components.SolidButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            yearText = now.get(Calendar.YEAR).toString()
                            monthText = (now.get(Calendar.MONTH) + 1).toString()
                            dayText = now.get(Calendar.DAY_OF_MONTH).toString()
                        },
                        filled = false
                    ) { Text("回到今天") }
                    if (lunar == null) {
                        Text(
                            "农历数据覆盖 1900 到 2039 年，这个日期超出范围了",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.orange
                        )
                    }
                }
            }
        }
        if (lunar != null) {
            item { SectionHeader("农历") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell(
                                "农历",
                                (if (lunar.isLeap) "闰" else "") + lunarMonthNames[lunar.month - 1] + "月",
                                Modifier.weight(1f)
                            )
                            StatCell("日", lunarDayName(lunar.day), Modifier.weight(1f))
                            StatCell("生肖", zodiacOf(lunar.year), Modifier.weight(1f))
                        }
                    }
                    KeyValueRow(
                        "完整表示",
                        ganzhi(lunar.year) + "年（" + zodiacOf(lunar.year) + "年）" +
                            (if (lunar.isLeap) "闰" else "") + lunarMonthNames[lunar.month - 1] + "月" +
                            lunarDayName(lunar.day)
                    )
                    RowDivider()
                    KeyValueRow("干支年", ganzhi(lunar.year), copyable = false)
                    RowDivider()
                    KeyValueRow("农历年", lunar.year.toString(), copyable = false)
                    RowDivider()
                    KeyValueRow("本月天数", monthDays(lunar.year, lunar.month).toString() + " 天", copyable = false)
                    RowDivider()
                    KeyValueRow(
                        "今年闰月",
                        if (leapMonth(lunar.year) == 0) "没有" else "闰" + lunarMonthNames[leapMonth(lunar.year) - 1] + "月",
                        copyable = false
                    )
                }
            }
        }
        item { SectionHeader("下一个节气") }
        item {
            GroupedCard {
                KeyValueRow(
                    nextTerm.first,
                    nextTerm.second.first.toString() + " 月 " + nextTerm.second.second + " 日左右",
                    copyable = false
                )
            }
        }
    }
}
