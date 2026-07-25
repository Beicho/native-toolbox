package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.data.store.AstroStore
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.IosTextArea
import com.toolbox.nativetoolbox.ui.components.IosTextField
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette

/**
 * 便签 + 待办。数据走 AstroStore —— 待办与主页卡片是同一份,
 * 主页打的勾这里立刻能看见,反过来也一样。
 */
@Composable
fun NotesToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var mode by rememberSaveable { mutableStateOf(0) } // 0 待办 1 便签
    var version by remember { mutableStateOf(0) }
    var todoInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val todos = remember(version) {
        AstroStore.all(AstroStore.Collection.TODO)
            .sortedWith(compareBy({ it.bool("done") }, { -it.createdAt }))
    }
    val notes = remember(version) { AstroStore.all(AstroStore.Collection.NOTES) }

    /** 删除按钮:第一次点变红问「确定?」,再点才真删 */
    @Composable
    fun deleteChip(id: String, collection: AstroStore.Collection) {
        val confirming = pendingDelete == id
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (confirming) palette.red else palette.sunkenBackground)
                .clickable {
                    if (confirming) {
                        AstroStore.remove(collection, id)
                        pendingDelete = null
                        version++
                    } else pendingDelete = id
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                if (confirming) "确定?" else "删",
                style = MaterialTheme.typography.labelSmall,
                color = if (confirming) Color.White else palette.secondaryLabel
            )
        }
    }

    ToolScaffold {
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(listOf("待办", "便签"), mode, { mode = it; pendingDelete = null }, Modifier.fillMaxWidth())
                }
            }
        }

        if (mode == 0) {
            item {
                GroupedCard {
                    CardPadding {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IosTextField(
                                value = todoInput,
                                onValueChange = { todoInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = "要做什么"
                            )
                            SolidButton(
                                onClick = {
                                    if (todoInput.isNotBlank()) {
                                        AstroStore.add(AstroStore.Collection.TODO) {
                                            put("text", todoInput.trim())
                                            put("done", false)
                                        }
                                        todoInput = ""
                                        version++
                                    }
                                },
                                modifier = Modifier.width(64.dp),
                                enabled = todoInput.isNotBlank()
                            ) { Text("加") }
                        }
                    }
                }
            }
            if (todos.isEmpty()) {
                item {
                    Text(
                        "没有待办。加一条,它也会出现在主页卡片上。",
                        Modifier.fillMaxWidth().padding(24.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val doneCount = todos.count { it.bool("done") }
                item { SectionHeader("清单($doneCount/${todos.size})") }
                item {
                    GroupedCard {
                        todos.forEachIndexed { index, r ->
                            val done = r.bool("done")
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (done) palette.green else palette.sunkenBackground)
                                        .clickable {
                                            AstroStore.update(AstroStore.Collection.TODO, r.id) { put("done", !done) }
                                            version++
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (done) Text("✓", fontSize = 13.sp, color = Color.White)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    r.str("text"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (done) palette.tertiaryLabel else palette.label,
                                    textDecoration = if (done) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                                deleteChip(r.id, AstroStore.Collection.TODO)
                            }
                            if (index != todos.lastIndex) RowDivider()
                        }
                    }
                }
                if (doneCount > 0) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            SolidButton(
                                onClick = {
                                    todos.filter { it.bool("done") }.forEach {
                                        AstroStore.remove(AstroStore.Collection.TODO, it.id)
                                    }
                                    version++
                                },
                                Modifier.fillMaxWidth(),
                                filled = false
                            ) { Text("清掉 $doneCount 条已完成") }
                        }
                    }
                }
            }
        } else {
            item {
                GroupedCard {
                    CardPadding {
                        IosTextArea(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = "随手记点什么…",
                            minHeight = 90.dp
                        )
                        SolidButton(
                            onClick = {
                                if (noteInput.isNotBlank()) {
                                    AstroStore.add(AstroStore.Collection.NOTES) { put("text", noteInput.trim()) }
                                    noteInput = ""
                                    version++
                                }
                            },
                            enabled = noteInput.isNotBlank()
                        ) { Text("存下") }
                    }
                }
            }
            if (notes.isEmpty()) {
                item {
                    Text(
                        "还没有便签",
                        Modifier.fillMaxWidth().padding(24.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.tertiaryLabel,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                item { SectionHeader("便签(${notes.size})") }
                item {
                    GroupedCard {
                        notes.forEachIndexed { index, r ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    r.str("text"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.label,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(10.dp))
                                deleteChip(r.id, AstroStore.Collection.NOTES)
                            }
                            if (index != notes.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }
}
