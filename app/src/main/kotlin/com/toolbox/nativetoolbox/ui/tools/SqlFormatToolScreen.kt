package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val majorKeywords = listOf(
    "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "LIMIT", "OFFSET",
    "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM",
    "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "FULL JOIN", "CROSS JOIN", "JOIN",
    "UNION ALL", "UNION", "ON", "AND", "OR"
)

private val allKeywords = (majorKeywords + listOf(
    "AS", "IN", "NOT", "NULL", "IS", "LIKE", "BETWEEN", "EXISTS", "DISTINCT",
    "COUNT", "SUM", "AVG", "MIN", "MAX", "CASE", "WHEN", "THEN", "ELSE", "END",
    "ASC", "DESC", "CREATE", "TABLE", "INDEX", "ALTER", "DROP"
)).distinct()

/** 先把字符串字面量替换成占位符，避免格式化改动引号内的内容 */
private fun protectLiterals(sql: String): Pair<String, List<String>> {
    val literals = ArrayList<String>()
    val out = Regex("'[^']*'").replace(sql) { m ->
        literals.add(m.value)
        " @@LIT" + (literals.size - 1) + "@@ "
    }
    return out to literals
}

private fun restoreLiterals(sql: String, literals: List<String>): String {
    var out = sql
    literals.forEachIndexed { index, value ->
        out = out.replace("@@LIT" + index + "@@", value)
    }
    return out
}

private fun upperKeywordsIn(sql: String): String {
    var out = sql
    allKeywords.sortedByDescending { it.length }.forEach { kw ->
        out = Regex("(?i)(?<![A-Za-z_])" + Regex.escape(kw) + "(?![A-Za-z_])").replace(out) { kw }
    }
    return out
}

private fun formatSql(input: String, upper: Boolean): String {
    val (protectedSql, literals) = protectLiterals(input)
    var sql = protectedSql.replace(Regex("\\s+"), " ").trim()
    if (upper) sql = upperKeywordsIn(sql)

    majorKeywords.sortedByDescending { it.length }.forEach { kw ->
        sql = Regex("(?i)(?<![A-Za-z_])" + Regex.escape(kw) + "(?![A-Za-z_])").replace(sql) { m ->
            "\n" + m.value
        }
    }
    sql = sql.replace(Regex(",\\s*"), ",\n  ")

    val builder = StringBuilder()
    sql.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
        val isMajor = majorKeywords.any { line.uppercase().startsWith(it) }
        builder.append(if (isMajor) "" else "  ").append(line).append("\n")
    }
    return restoreLiterals(builder.toString().trimEnd(), literals)
}

private fun compressSql(input: String): String {
    val (protectedSql, literals) = protectLiterals(input)
    return restoreLiterals(protectedSql.replace(Regex("\\s+"), " ").trim(), literals)
}

@Composable
fun SqlFormatToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var input by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(0) }
    var upper by rememberSaveable { mutableStateOf(true) }

    val result = when {
        input.isBlank() -> ""
        mode == 0 -> formatSql(input, upper)
        else -> compressSql(input)
    }

    ToolScaffold {
        item { SectionHeader("SQL") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴一段 SQL",
                        mono = true
                    )
                    SegmentedPicker(
                        options = listOf("格式化", "压成一行"),
                        selectedIndex = mode,
                        onSelected = { mode = it }
                    )
                    if (mode == 0) {
                        ToggleRow("关键字转大写", upper, onCheckedChange = { upper = it })
                    }
                    Text(
                        "引号里的内容不会被改动。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel
                    )
                }
            }
        }
        item { SectionHeader("结果") }
        item { GroupedCard { CardPadding { OutputCard(text = result) } } }
    }
}
