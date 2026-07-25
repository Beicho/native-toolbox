package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 一条记录:kind 0体重 1血压 2血糖;v1 主值(体重/收缩压/血糖),v2 血压的舒张压 */
private data class HRecord(val time: Long, val kind: Int, val v1: Double, val v2: Double = 0.0)

private fun loadRecords(prefs: android.content.SharedPreferences): List<HRecord> = runCatching {
    val arr = JSONArray(prefs.getString("records", "[]"))
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        HRecord(o.getLong("t"), o.getInt("k"), o.getDouble("a"), o.optDouble("b", 0.0))
    }
}.getOrDefault(emptyList())

private fun saveRecords(prefs: android.content.SharedPreferences, list: List<HRecord>) {
    val arr = JSONArray()
    list.takeLast(500).forEach { r ->
        arr.put(JSONObject().put("t", r.time).put("k", r.kind).put("a", r.v1).put("b", r.v2))
    }
    prefs.edit().putString("records", arr.toString()).apply()
}

@Composable
fun HealthRecordToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("health_record", android.content.Context.MODE_PRIVATE) }
    var records by remember { mutableStateOf(loadRecords(prefs)) }
    var kind by rememberSaveable { mutableStateOf(0) }
    var input1 by rememberSaveable { mutableStateOf("") }
    var input2 by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    val kindNames = listOf("体重", "血压", "血糖")
    val units = listOf("kg", "mmHg", "mmol/L")
    val filtered = records.filter { it.kind == kind }.sortedBy { it.time }
    val df = remember { SimpleDateFormat("M/d HH:mm", Locale.getDefault()) }

    fun add() {
        val a = input1.toDoubleOrNull()
        if (a == null || a <= 0) { status = "先把数值填对"; return }
        val b = input2.toDoubleOrNull() ?: 0.0
        if (kind == 1 && (b <= 0 || b >= a)) { status = "血压要填两个数,低压小于高压"; return }
        val sane = when (kind) {
            0 -> a in 2.0..400.0
            1 -> a in 50.0..260.0 && b in 30.0..200.0
            else -> a in 1.0..35.0
        }
        if (!sane) { status = "这个数不太对劲,检查一下"; return }
        records = records + HRecord(System.currentTimeMillis(), kind, a, b)
        saveRecords(prefs, records)
        input1 = ""; input2 = ""; status = "记好了"
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(kindNames, kind, { kind = it; status = "" }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    if (kind == 1) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("高压(收缩)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                                Spacer(Modifier.height(4.dp))
                                IosTextField(input1, { input1 = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), placeholder = "120")
                            }
                            Column(Modifier.weight(1f)) {
                                Text("低压(舒张)", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                                Spacer(Modifier.height(4.dp))
                                IosTextField(input2, { input2 = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), placeholder = "80")
                            }
                        }
                    } else {
                        Text("${kindNames[kind]}(${units[kind]})", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                        Spacer(Modifier.height(4.dp))
                        IosTextField(input1, { input1 = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), placeholder = if (kind == 0) "65.5" else "5.6")
                    }
                    Spacer(Modifier.height(10.dp))
                    SolidButton(onClick = { add() }, Modifier.fillMaxWidth()) { Text("记一笔") }
                    if (status.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(status, style = MaterialTheme.typography.bodySmall, color = if (status == "记好了") palette.green else palette.red)
                    }
                }
            }
        }
        item {
            if (filtered.size >= 2) {
                GroupedCard {
                    CardPadding {
                        Text("趋势(最近 ${filtered.takeLast(30).size} 条)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = palette.label)
                        Spacer(Modifier.height(10.dp))
                        val data = filtered.takeLast(30)
                        val accent = palette.accent
                        val red = palette.red
                        val gridColor = palette.sunkenBackground
                        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                            val vals = data.map { it.v1 } + if (kind == 1) data.map { it.v2 } else emptyList()
                            val minV = (vals.min() * 0.95)
                            val maxV = (vals.max() * 1.05)
                            val range = (maxV - minV).coerceAtLeast(0.1)
                            fun toY(v: Double) = (size.height * (1 - (v - minV) / range)).toFloat()
                            fun toX(i: Int) = if (data.size == 1) size.width / 2 else size.width * i / (data.size - 1f)
                            // 网格
                            for (g in 0..3) {
                                val y = size.height * g / 3f
                                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.5f)
                            }
                            fun drawSeries(pick: (HRecord) -> Double, color: androidx.compose.ui.graphics.Color) {
                                val path = Path()
                                data.forEachIndexed { i, r ->
                                    val x = toX(i); val y = toY(pick(r))
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, color, style = Stroke(4f))
                                data.forEachIndexed { i, r -> drawCircle(color, 5f, Offset(toX(i), toY(pick(r)))) }
                            }
                            drawSeries({ it.v1 }, accent)
                            if (kind == 1) drawSeries({ it.v2 }, red)
                        }
                        if (kind == 1) {
                            Spacer(Modifier.height(6.dp))
                            Text("蓝线高压 · 红线低压", style = MaterialTheme.typography.bodySmall, color = palette.tertiaryLabel)
                        }
                    }
                }
            }
        }
        item { if (filtered.isNotEmpty()) SectionHeader("${kindNames[kind]}记录") }
        item {
            if (filtered.isNotEmpty()) {
                GroupedCard {
                    val recent = filtered.takeLast(10).reversed()
                    recent.forEachIndexed { i, r ->
                        val value = if (kind == 1) "${r.v1.toInt()}/${r.v2.toInt()} mmHg" else "${r.v1} ${units[kind]}"
                        val note = when (kind) {
                            1 -> if (r.v1 >= 140 || r.v2 >= 90) "偏高" else if (r.v1 < 90) "偏低" else "正常"
                            2 -> if (r.v1 >= 7.0) "偏高(空腹)" else if (r.v1 < 3.9) "偏低" else "正常"
                            else -> ""
                        }
                        KeyValueRow(df.format(Date(r.time)), value + if (note.isNotEmpty()) " · $note" else "", copyable = false)
                        if (i != recent.lastIndex) RowDivider()
                    }
                }
            }
        }
        item {
            if (filtered.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    SolidButton(
                        onClick = {
                            records = records.filter { it.kind != kind }
                            saveRecords(prefs, records)
                        },
                        Modifier.fillMaxWidth(), filled = false
                    ) { Text("清空${kindNames[kind]}记录") }
                }
            }
        }
    }
}
