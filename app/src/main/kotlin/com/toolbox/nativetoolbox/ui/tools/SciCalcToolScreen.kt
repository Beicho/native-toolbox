package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 递归下降表达式求值。支持 + - * / % ^、括号、一元负号、常量 pi/e、
 * 阶乘 !、以及 sin cos tan asin acos atan ln log sqrt cbrt abs exp 函数。
 * 三角函数按角度制（更符合日常使用），函数名后必须跟括号。
 */
private class Evaluator(private val src: String, private val degrees: Boolean) {
    private var pos = 0

    fun evaluate(): Double {
        val value = parseExpression()
        skipSpaces()
        if (pos < src.length) error("第 ${pos + 1} 个字符处看不懂：'${src[pos]}'")
        return value
    }

    private fun skipSpaces() {
        while (pos < src.length && src[pos].isWhitespace()) pos++
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            skipSpaces()
            if (pos >= src.length) return value
            when (src[pos]) {
                '+' -> { pos++; value += parseTerm() }
                '-' -> { pos++; value -= parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parsePower()
        while (true) {
            skipSpaces()
            if (pos >= src.length) return value
            when (src[pos]) {
                '*', '×' -> { pos++; value *= parsePower() }
                '/', '÷' -> {
                    pos++
                    val divisor = parsePower()
                    if (divisor == 0.0) error("不能除以 0")
                    value /= divisor
                }
                '%' -> {
                    pos++
                    val m = parsePower()
                    if (m == 0.0) error("不能对 0 取余")
                    value %= m
                }
                else -> return value
            }
        }
    }

    private fun parsePower(): Double {
        val base = parseUnary()
        skipSpaces()
        if (pos < src.length && src[pos] == '^') {
            pos++
            return base.pow(parsePower())
        }
        return base
    }

    private fun parseUnary(): Double {
        skipSpaces()
        if (pos < src.length && (src[pos] == '-' || src[pos] == '+')) {
            val negative = src[pos] == '-'
            pos++
            val v = parseUnary()
            return if (negative) -v else v
        }
        return parsePostfix()
    }

    private fun parsePostfix(): Double {
        var value = parseAtom()
        skipSpaces()
        while (pos < src.length && src[pos] == '!') {
            pos++
            value = factorial(value)
            skipSpaces()
        }
        return value
    }

    private fun factorial(v: Double): Double {
        if (v < 0 || abs(v - Math.round(v)) > 1e-9) error("阶乘只支持非负整数")
        if (v > 170) error("阶乘结果太大了")
        var acc = 1.0
        for (i in 2..v.toInt()) acc *= i
        return acc
    }

    private fun parseAtom(): Double {
        skipSpaces()
        if (pos >= src.length) error("表达式没写完")
        val c = src[pos]
        if (c == '(') {
            pos++
            val v = parseExpression()
            skipSpaces()
            if (pos >= src.length || src[pos] != ')') error("括号没有闭合")
            pos++
            return v
        }
        if (c.isDigit() || c == '.') {
            val start = pos
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
            return src.substring(start, pos).toDoubleOrNull() ?: error("数字写错了：${src.substring(start, pos)}")
        }
        if (c.isLetter()) {
            val start = pos
            while (pos < src.length && src[pos].isLetter()) pos++
            val name = src.substring(start, pos).lowercase()
            if (name == "pi" || name == "π") return Math.PI
            if (name == "e") return Math.E
            skipSpaces()
            if (pos >= src.length || src[pos] != '(') error("$name 后面要跟括号")
            pos++
            val arg = parseExpression()
            skipSpaces()
            if (pos >= src.length || src[pos] != ')') error("$name 的括号没有闭合")
            pos++
            return applyFunction(name, arg)
        }
        error("看不懂的符号：'$c'")
    }

    private fun toRadians(v: Double) = if (degrees) v * Math.PI / 180.0 else v
    private fun fromRadians(v: Double) = if (degrees) v * 180.0 / Math.PI else v

    private fun applyFunction(name: String, arg: Double): Double = when (name) {
        "sin" -> sin(toRadians(arg))
        "cos" -> cos(toRadians(arg))
        "tan" -> tan(toRadians(arg))
        "asin" -> {
            if (arg < -1 || arg > 1) error("asin 的参数要在 -1 到 1 之间")
            fromRadians(asin(arg))
        }
        "acos" -> {
            if (arg < -1 || arg > 1) error("acos 的参数要在 -1 到 1 之间")
            fromRadians(acos(arg))
        }
        "atan" -> fromRadians(atan(arg))
        "ln" -> {
            if (arg <= 0) error("ln 的参数要大于 0")
            ln(arg)
        }
        "log" -> {
            if (arg <= 0) error("log 的参数要大于 0")
            log10(arg)
        }
        "sqrt" -> {
            if (arg < 0) error("负数开不了平方根")
            sqrt(arg)
        }
        "cbrt" -> cbrt(arg)
        "abs" -> abs(arg)
        "exp" -> exp(arg)
        else -> error("不支持的函数：$name")
    }

    private fun error(message: String): Nothing = throw IllegalArgumentException(message)
}

private fun formatResult(value: Double): String {
    if (value.isNaN()) return "无法计算"
    if (value.isInfinite()) return "结果超出范围"
    if (abs(value) >= 1e12 || (abs(value) < 1e-9 && value != 0.0)) return String.format("%.6e", value)
    val rounded = Math.round(value * 1e9) / 1e9
    return if (abs(rounded - Math.round(rounded)) < 1e-9) Math.round(rounded).toString()
    else rounded.toString().trimEnd('0').trimEnd('.')
}

private val keypad = listOf(
    listOf("7", "8", "9", "/", "("),
    listOf("4", "5", "6", "*", ")"),
    listOf("1", "2", "3", "-", "^"),
    listOf("0", ".", "%", "+", "!")
)
private val funcKeys = listOf("sin", "cos", "tan", "ln", "log", "sqrt", "pi", "e")

@Composable
fun SciCalcToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var expr by rememberSaveable { mutableStateOf("") }
    var degrees by rememberSaveable { mutableStateOf(true) }

    val outcome: Result<Double> = if (expr.isBlank()) Result.failure(IllegalStateException(""))
    else runCatching { Evaluator(expr, degrees).evaluate() }

    ToolScaffold {
        item { SectionHeader("表达式") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = expr,
                        onValueChange = { expr = it },
                        placeholder = "例如 2^10 + sqrt(144) * sin(30)",
                        mono = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SolidButton(
                            onClick = { degrees = !degrees },
                            modifier = Modifier.weight(1f),
                            filled = false
                        ) { Text(if (degrees) "角度" else "弧度") }
                        SolidButton(
                            onClick = { if (expr.isNotEmpty()) expr = expr.dropLast(1) },
                            modifier = Modifier.weight(1f),
                            filled = false
                        ) { Text("退格") }
                        SolidButton(
                            onClick = { expr = "" },
                            modifier = Modifier.weight(1f),
                            filled = false
                        ) { Text("清空") }
                    }
                }
            }
        }
        item { SectionHeader("结果") }
        item {
            GroupedCard {
                CardPadding {
                    when {
                        expr.isBlank() -> Text(
                            "支持括号、乘方 ^、阶乘 !、常量 pi 和 e，以及 sin cos tan asin acos atan ln log sqrt cbrt abs exp。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.tertiaryLabel
                        )
                        outcome.isSuccess -> OutputCard(text = formatResult(outcome.getOrThrow()), label = "= ")
                        else -> Text(
                            outcome.exceptionOrNull()?.message ?: "算不出来",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                }
            }
        }
        item { SectionHeader("常用函数") }
        item {
            GroupedCard {
                CardPadding {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        funcKeys.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { key ->
                                    SolidButton(
                                        onClick = { expr += if (key == "pi" || key == "e") key else "$key(" },
                                        modifier = Modifier.weight(1f),
                                        filled = false,
                                        height = 40.dp
                                    ) { Text(key) }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader("数字键") }
        item {
            GroupedCard {
                CardPadding {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        keypad.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { key ->
                                    SolidButton(
                                        onClick = { expr += key },
                                        modifier = Modifier.weight(1f),
                                        filled = false,
                                        height = 44.dp
                                    ) { Text(key) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
