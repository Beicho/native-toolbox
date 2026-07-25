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
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

private val bloodTypes = listOf("未填", "A", "B", "AB", "O")
private val rhTypes = listOf("未填", "阳性", "阴性")

private val emergencyNumbers = listOf(
    "120" to "急救中心",
    "119" to "火警与救援",
    "110" to "公安报警",
    "122" to "交通事故",
    "12395" to "水上搜救",
    "12320" to "卫生健康热线"
)

@Composable
fun EmergencyCardToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current

    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var bloodIndex by rememberSaveable { mutableStateOf(0) }
    var rhIndex by rememberSaveable { mutableStateOf(0) }
    var allergy by rememberSaveable { mutableStateOf("") }
    var disease by rememberSaveable { mutableStateOf("") }
    var medicine by rememberSaveable { mutableStateOf("") }
    var contact1Name by rememberSaveable { mutableStateOf("") }
    var contact1Phone by rememberSaveable { mutableStateOf("") }
    var contact2Name by rememberSaveable { mutableStateOf("") }
    var contact2Phone by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    val bloodText = if (bloodIndex == 0) "未填写" else
        bloodTypes[bloodIndex] + "型" + (if (rhIndex == 0) "" else "　Rh " + rhTypes[rhIndex])

    val card = buildString {
        appendLine("【急救信息卡】")
        appendLine("姓名：" + name.ifBlank { "未填写" })
        appendLine("年龄：" + age.ifBlank { "未填写" })
        appendLine("血型：" + bloodText)
        appendLine("过敏：" + allergy.ifBlank { "无或未填写" })
        appendLine("疾病史：" + disease.ifBlank { "无或未填写" })
        appendLine("长期用药：" + medicine.ifBlank { "无或未填写" })
        if (contact1Name.isNotBlank() || contact1Phone.isNotBlank()) {
            appendLine("紧急联系人一：" + contact1Name + " " + contact1Phone)
        }
        if (contact2Name.isNotBlank() || contact2Phone.isNotBlank()) {
            appendLine("紧急联系人二：" + contact2Name + " " + contact2Phone)
        }
        if (note.isNotBlank()) appendLine("其他说明：" + note)
        append("急救电话 120")
    }

    val filled = listOf(name, age, allergy, disease, medicine, contact1Phone).count { it.isNotBlank() } +
        (if (bloodIndex > 0) 1 else 0)

    ToolScaffold {
        item { SectionHeader("完成度") }
        item {
            GroupedCard {
                CardPadding {
                    Text(
                        "已填 " + filled + " / 7 项关键信息",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (filled >= 5) palette.green else palette.orange
                    )
                    Text(
                        "填好后复制这张卡，存进手机锁屏信息、备忘录，或者截图打印随身携带。急救人员看到能省下宝贵时间。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel
                    )
                }
            }
        }
        item { SectionHeader("基本信息") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.weight(2f),
                            placeholder = "姓名"
                        )
                        IosTextField(
                            value = age,
                            onValueChange = { age = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "年龄",
                            mono = true
                        )
                    }
                    SegmentedPicker(
                        options = bloodTypes,
                        selectedIndex = bloodIndex,
                        onSelected = { bloodIndex = it }
                    )
                    if (bloodIndex > 0) {
                        SegmentedPicker(
                            options = rhTypes,
                            selectedIndex = rhIndex,
                            onSelected = { rhIndex = it }
                        )
                    }
                }
            }
        }
        item { SectionHeader("医疗信息") }
        item {
            GroupedCard {
                CardPadding {
                    IosTextField(
                        value = allergy,
                        onValueChange = { allergy = it },
                        placeholder = "过敏药物或食物，例如 青霉素、海鲜"
                    )
                    IosTextField(
                        value = disease,
                        onValueChange = { disease = it },
                        placeholder = "疾病史，例如 高血压、糖尿病、癫痫"
                    )
                    IosTextField(
                        value = medicine,
                        onValueChange = { medicine = it },
                        placeholder = "长期服用的药"
                    )
                    Text(
                        "过敏史最关键，用药冲突可能致命。有装心脏起搏器、支架之类的也一定要写。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.red
                    )
                }
            }
        }
        item { SectionHeader("紧急联系人") }
        item {
            GroupedCard {
                CardPadding {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = contact1Name,
                            onValueChange = { contact1Name = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "称呼"
                        )
                        IosTextField(
                            value = contact1Phone,
                            onValueChange = { contact1Phone = it },
                            modifier = Modifier.weight(2f),
                            placeholder = "电话",
                            mono = true
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IosTextField(
                            value = contact2Name,
                            onValueChange = { contact2Name = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "称呼（备用）"
                        )
                        IosTextField(
                            value = contact2Phone,
                            onValueChange = { contact2Phone = it },
                            modifier = Modifier.weight(2f),
                            placeholder = "电话",
                            mono = true
                        )
                    }
                    IosTextArea(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = "其他要交代的（可选）",
                        minHeight = 80.dp
                    )
                }
            }
        }
        item { SectionHeader("生成的卡片") }
        item { GroupedCard { CardPadding { OutputCard(text = card, label = "复制后保存") } } }
    }
}
