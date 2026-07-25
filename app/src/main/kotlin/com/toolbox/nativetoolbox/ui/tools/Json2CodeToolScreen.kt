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
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.OutputCard
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToggleRow
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import org.json.JSONArray
import org.json.JSONObject

private val languages = listOf("Kotlin", "TypeScript", "Java", "Go", "Dart")

private fun pascal(name: String): String = name.split('_', '-', ' ')
    .filter { it.isNotEmpty() }
    .joinToString("") { it.first().uppercase() + it.drop(1) }

private fun camel(name: String): String = pascal(name).let {
    if (it.isEmpty()) it else it.first().lowercase() + it.drop(1)
}

private class Field(val name: String, val type: String, val nullable: Boolean)

private class Model(val name: String, val fields: List<Field>)

/** 递归拆出所有需要生成的模型；嵌套对象按字段名生成子模型 */
private fun collectModels(name: String, obj: JSONObject, lang: Int, out: MutableList<Model>) {
    val fields = ArrayList<Field>()
    obj.keys().forEach { key ->
        val value = obj.get(key)
        val nullable = value == JSONObject.NULL
        val type = when {
            value is JSONObject -> {
                val childName = pascal(key)
                collectModels(childName, value, lang, out)
                childName
            }
            value is JSONArray -> {
                val first = if (value.length() > 0) value.get(0) else null
                val inner = when {
                    first is JSONObject -> {
                        val childName = pascal(key).removeSuffix("s").ifEmpty { pascal(key) }
                        collectModels(childName, first, lang, out)
                        childName
                    }
                    first is Int || first is Long -> intType(lang)
                    first is Double || first is Float -> doubleType(lang)
                    first is Boolean -> boolType(lang)
                    first == null -> anyType(lang)
                    else -> stringType(lang)
                }
                listType(lang, inner)
            }
            value is Int || value is Long -> intType(lang)
            value is Double || value is Float -> doubleType(lang)
            value is Boolean -> boolType(lang)
            nullable -> anyType(lang)
            else -> stringType(lang)
        }
        fields.add(Field(key, type, nullable))
    }
    out.add(Model(name, fields))
}

private fun intType(lang: Int) = listOf("Long", "number", "long", "int64", "int")[lang]
private fun doubleType(lang: Int) = listOf("Double", "number", "double", "float64", "double")[lang]
private fun boolType(lang: Int) = listOf("Boolean", "boolean", "boolean", "bool", "bool")[lang]
private fun stringType(lang: Int) = listOf("String", "string", "String", "string", "String")[lang]
private fun anyType(lang: Int) = listOf("Any?", "unknown", "Object", "interface{}", "dynamic")[lang]
private fun listType(lang: Int, inner: String) = when (lang) {
    0 -> "List<" + inner + ">"
    1 -> inner + "[]"
    2 -> "List<" + inner + ">"
    3 -> "[]" + inner
    else -> "List<" + inner + ">"
}

private fun render(models: List<Model>, lang: Int, nullableAll: Boolean): String =
    models.reversed().joinToString("\n\n") { model ->
        when (lang) {
            0 -> "data class " + model.name + "(\n" + model.fields.joinToString(",\n") { f ->
                "    val " + camel(f.name) + ": " + f.type + if (f.nullable || nullableAll) "?" else ""
            } + "\n)"
            1 -> "export interface " + model.name + " {\n" + model.fields.joinToString("\n") { f ->
                "  " + f.name + (if (f.nullable || nullableAll) "?" else "") + ": " + f.type + ";"
            } + "\n}"
            2 -> "public class " + model.name + " {\n" + model.fields.joinToString("\n") { f ->
                "    private " + f.type + " " + camel(f.name) + ";"
            } + "\n}"
            3 -> "type " + model.name + " struct {\n" + model.fields.joinToString("\n") { f ->
                "\t" + pascal(f.name) + " " + f.type + " `json:\"" + f.name + "\"`"
            } + "\n}"
            else -> "class " + model.name + " {\n" + model.fields.joinToString("\n") { f ->
                "  final " + f.type + (if (f.nullable || nullableAll) "?" else "") + " " + camel(f.name) + ";"
            } + "\n}"
        }
    }

@Composable
fun Json2CodeToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var input by rememberSaveable { mutableStateOf("") }
    var langIndex by rememberSaveable { mutableStateOf(0) }
    var rootName by rememberSaveable { mutableStateOf("Response") }
    var nullableAll by rememberSaveable { mutableStateOf(false) }

    val root: JSONObject? = if (input.isBlank()) null else runCatching {
        val t = input.trim()
        if (t.startsWith("[")) {
            val arr = JSONArray(t)
            if (arr.length() > 0) arr.optJSONObject(0) else null
        } else JSONObject(t)
    }.getOrNull()

    val models = ArrayList<Model>()
    if (root != null) collectModels(pascal(rootName.ifBlank { "Response" }), root, langIndex, models)
    val code = if (models.isEmpty()) "" else render(models, langIndex, nullableAll)
    val totalFields = models.sumOf { it.fields.size }

    ToolScaffold {
        item { SectionHeader("JSON") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextArea(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = "粘贴接口返回的 JSON。数组会取第一个元素推断结构。",
                        minHeight = 150.dp,
                        mono = true
                    )
                    if (input.isNotBlank() && root == null) {
                        Text(
                            "解析不了。要是一个对象或者非空对象数组。",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red
                        )
                    }
                }
            }
        }
        item { SectionHeader("生成选项") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = languages,
                        selectedIndex = langIndex,
                        onSelected = { langIndex = it }
                    )
                    IosTextField(
                        value = rootName,
                        onValueChange = { rootName = it },
                        placeholder = "顶层类名"
                    )
                }
                ToggleRow(
                    "所有字段都可空",
                    nullableAll,
                    onCheckedChange = { nullableAll = it },
                    subtitle = "接口不稳定时建议打开，避免解析崩溃"
                )
            }
        }
        if (models.isNotEmpty()) {
            item { SectionHeader("结构概览") }
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCell("类数量", models.size.toString(), Modifier.weight(1f))
                            StatCell("字段总数", totalFields.toString(), Modifier.weight(1f))
                            StatCell("语言", languages[langIndex], Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionHeader("生成的代码") }
            item { GroupedCard { CardPadding { OutputCard(text = code) } } }
            item { SectionHeader("字段类型推断") }
            item {
                GroupedCard {
                    val flat = models.flatMap { m -> m.fields.map { m.name + "." + it.name to it.type } }
                    flat.take(40).forEachIndexed { index, (k, v) ->
                        KeyValueRow(k, v, copyable = false)
                        if (index != flat.take(40).lastIndex) RowDivider()
                    }
                }
            }
        }
    }
}
