package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.CheckRow
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

private val surnames = listOf("王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴", "徐", "孙", "马", "朱", "胡", "林", "郭", "何")
private val givenNames = listOf("伟", "芳", "娜", "秀英", "敏", "静", "丽", "强", "磊", "洋", "艳", "勇", "军", "杰", "娟", "涛", "明", "超", "秀兰", "霞")
private val cities = listOf("北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆", "苏州", "长沙", "青岛", "合肥")
private val streets = listOf("人民路", "解放路", "中山路", "建设街", "文化路", "长江大道", "科技园路", "花园街")
private val domains = listOf("example.com", "test.cn", "demo.net", "mail.com", "sample.org")
private val companySuffix = listOf("科技有限公司", "信息技术有限公司", "网络科技有限公司", "文化传媒有限公司", "贸易有限公司")
private val companyPrefix = listOf("恒宇", "天成", "华信", "远大", "创联", "锐驰", "博远", "启明", "云谷", "星辰")
private val phonePrefix = listOf("139", "138", "137", "136", "135", "150", "151", "152", "158", "159", "180", "181", "182", "186", "187", "188", "199")

private class MockGen(private val random: Random) {
    fun name() = surnames.random(random) + givenNames.random(random)
    fun phone() = phonePrefix.random(random) + (10000000..99999999).random(random).toString()
    fun email(): String {
        val user = ('a'..'z').let { pool -> (1..(6 + random.nextInt(4))).map { pool.random(random) }.joinToString("") }
        return user + "@" + domains.random(random)
    }
    fun city() = cities.random(random)
    fun address() = city() + "市" + streets.random(random) + (1..999).random(random) + "号"
    fun company() = companyPrefix.random(random) + companySuffix.random(random)
    fun idCard(): String {
        val area = (110000..659000).random(random)
        val year = (1960..2005).random(random)
        val month = (1..12).random(random)
        val day = (1..28).random(random)
        val seq = (100..999).random(random)
        val body = "%06d%04d%02d%02d%03d".format(area, year, month, day, seq)
        return body + checksum(body)
    }
    /** 身份证第 18 位校验码，按国标 GB 11643 算 */
    private fun checksum(body: String): String {
        val weights = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
        val codes = charArrayOf('1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2')
        var sum = 0
        body.forEachIndexed { i, c -> sum += (c - '0') * weights[i] }
        return codes[sum % 11].toString()
    }
    fun date(): String = "%04d-%02d-%02d".format((2020..2026).random(random), (1..12).random(random), (1..28).random(random))
    fun ip() = "%d.%d.%d.%d".format((1..223).random(random), (0..255).random(random), (0..255).random(random), (1..254).random(random))
    fun amount() = "%.2f".format((1..99999).random(random) / 100.0)
    fun bool() = random.nextBoolean()
    fun id(index: Int) = index + 1
}

private data class FieldSpec(val key: String, val label: String)

private val allFields = listOf(
    FieldSpec("id", "序号"),
    FieldSpec("name", "姓名"),
    FieldSpec("phone", "手机号"),
    FieldSpec("email", "邮箱"),
    FieldSpec("idCard", "身份证号"),
    FieldSpec("city", "城市"),
    FieldSpec("address", "地址"),
    FieldSpec("company", "公司"),
    FieldSpec("date", "日期"),
    FieldSpec("ip", "IP"),
    FieldSpec("amount", "金额"),
    FieldSpec("active", "是否启用")
)

private fun generate(count: Int, fields: List<FieldSpec>, seed: Int): List<Map<String, Any>> {
    val gen = MockGen(Random(seed))
    return (0 until count).map { index ->
        val row = LinkedHashMap<String, Any>()
        fields.forEach { field ->
            row[field.key] = when (field.key) {
                "id" -> gen.id(index)
                "name" -> gen.name()
                "phone" -> gen.phone()
                "email" -> gen.email()
                "idCard" -> gen.idCard()
                "city" -> gen.city()
                "address" -> gen.address()
                "company" -> gen.company()
                "date" -> gen.date()
                "ip" -> gen.ip()
                "amount" -> gen.amount()
                else -> gen.bool()
            }
        }
        row
    }
}

private fun toJson(rows: List<Map<String, Any>>): String {
    val array = JSONArray()
    rows.forEach { row ->
        val obj = JSONObject()
        row.forEach { (k, v) -> obj.put(k, v) }
        array.put(obj)
    }
    return array.toString(2)
}

private fun toCsv(rows: List<Map<String, Any>>): String {
    if (rows.isEmpty()) return ""
    val keys = rows.first().keys.toList()
    val header = keys.joinToString(",")
    val body = rows.joinToString("\n") { row ->
        keys.joinToString(",") { key ->
            val value = row[key].toString()
            if (value.contains(',') || value.contains('"')) "\"" + value.replace("\"", "\"\"") + "\"" else value
        }
    }
    return header + "\n" + body
}

private fun toSql(rows: List<Map<String, Any>>, table: String): String {
    if (rows.isEmpty()) return ""
    val keys = rows.first().keys.toList()
    return rows.joinToString("\n") { row ->
        val values = keys.joinToString(", ") { key ->
            val v = row[key]
            when (v) {
                is Int, is Boolean -> v.toString()
                else -> "'" + v.toString().replace("'", "''") + "'"
            }
        }
        "INSERT INTO " + table + " (" + keys.joinToString(", ") + ") VALUES (" + values + ");"
    }
}

@Composable
fun MockDataToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var countText by rememberSaveable { mutableStateOf("10") }
    var format by rememberSaveable { mutableStateOf(0) }
    var tableName by rememberSaveable { mutableStateOf("users") }
    var seed by rememberSaveable { mutableStateOf(1) }
    var selectedKeys by rememberSaveable { mutableStateOf("id,name,phone,email,city") }

    val selected = selectedKeys.split(',').filter { it.isNotBlank() }.toSet()
    val fields = allFields.filter { selected.contains(it.key) }
    val count = (countText.trim().toIntOrNull() ?: 0).coerceIn(0, 200)
    val rows = if (fields.isEmpty() || count == 0) emptyList() else generate(count, fields, seed)

    val output = when {
        rows.isEmpty() -> ""
        format == 0 -> toJson(rows)
        format == 1 -> toCsv(rows)
        else -> toSql(rows, tableName.trim().ifBlank { "users" })
    }

    fun toggleField(key: String) {
        val current = selectedKeys.split(',').filter { it.isNotBlank() }.toMutableList()
        if (current.contains(key)) current.remove(key) else current.add(key)
        selectedKeys = current.joinToString(",")
    }

    ToolScaffold {
        item { SectionHeader("生成设置") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        placeholder = "条数（最多 200）",
                        mono = true
                    )
                    SegmentedPicker(
                        options = listOf("JSON", "CSV", "SQL"),
                        selectedIndex = format,
                        onSelected = { format = it }
                    )
                    if (format == 2) {
                        IosTextField(
                            value = tableName,
                            onValueChange = { tableName = it },
                            placeholder = "表名",
                            mono = true
                        )
                    }
                    SolidButton(onClick = { seed = seed + 1 }) { Text("换一批") }
                    Text(
                        "数据是本地随机生成的假数据，姓名地址都是拼出来的，身份证号校验位算法正确但不对应真实人员。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("包含字段") }
        item {
            GroupedCard {
                allFields.forEach { field ->
                    CheckRow(field.label, selected.contains(field.key)) { toggleField(field.key) }
                }
            }
        }
        item { SectionHeader(if (rows.isEmpty()) "结果" else "已生成 " + rows.size + " 条") }
        item {
            GroupedCard {
                CardPadding {
                    if (rows.isEmpty()) {
                        Text(
                            "至少选一个字段，条数要大于 0",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel
                        )
                    } else {
                        OutputCard(text = output)
                    }
                }
            }
        }
    }
}
