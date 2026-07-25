package com.toolbox.nativetoolbox.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolbox.nativetoolbox.ui.components.CardPadding
import com.toolbox.nativetoolbox.ui.components.GroupedCard
import com.toolbox.nativetoolbox.ui.components.KeyValueRow
import com.toolbox.nativetoolbox.ui.components.RowDivider
import com.toolbox.nativetoolbox.ui.components.SectionHeader
import com.toolbox.nativetoolbox.ui.components.SegmentedPicker
import com.toolbox.nativetoolbox.ui.components.SolidButton
import com.toolbox.nativetoolbox.ui.components.StatCell
import com.toolbox.nativetoolbox.ui.components.ToolScaffold
import com.toolbox.nativetoolbox.ui.theme.LocalIosPalette
import kotlin.random.Random

private const val GAME_TICTACTOE = 0
private const val GAME_2048 = 1
private const val GAME_MINESWEEPER = 2

// ── 井字棋 ──

private fun winnerOf(board: List<String>): String? {
    val lines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6)
    )
    lines.forEach { line ->
        val a = board[line[0]]
        if (a.isNotEmpty() && line.all { board[it] == a }) return a
    }
    return null
}

/** 电脑走子：能赢就赢，能堵就堵，否则占中心或角 */
private fun computerMove(board: List<String>): Int? {
    val empty = board.indices.filter { board[it].isEmpty() }
    if (empty.isEmpty()) return null
    listOf("O", "X").forEach { mark ->
        empty.forEach { index ->
            val trial = board.toMutableList().also { it[index] = mark }
            if (winnerOf(trial) == mark) return index
        }
    }
    if (board[4].isEmpty()) return 4
    val corners = listOf(0, 2, 6, 8).filter { board[it].isEmpty() }
    if (corners.isNotEmpty()) return corners.random()
    return empty.random()
}

@Composable
private fun TicTacToe(palette: com.toolbox.nativetoolbox.ui.theme.IosPalette) {
    val board = remember { MutableList(9) { "" }.toMutableStateList() }
    var playerWins by rememberSaveable { mutableStateOf(0) }
    var computerWins by rememberSaveable { mutableStateOf(0) }
    var draws by rememberSaveable { mutableStateOf(0) }

    val winner = winnerOf(board)
    val full = board.none { it.isEmpty() }
    var settled by remember { mutableStateOf(false) }

    // 结算必须放在副作用里：在组合期改状态会触发无限重组，界面直接卡死退出
    LaunchedEffect(winner, full) {
        if ((winner != null || full) && !settled) {
            settled = true
            when (winner) {
                "X" -> playerWins += 1
                "O" -> computerWins += 1
                else -> draws += 1
            }
        }
    }

    fun reset() {
        for (i in board.indices) board[i] = ""
        settled = false
    }

    fun play(index: Int) {
        if (board[index].isNotEmpty() || winner != null) return
        board[index] = "X"
        if (winnerOf(board) == null && board.any { it.isEmpty() }) {
            computerMove(board)?.let { board[it] = "O" }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            when {
                winner == "X" -> "你赢了"
                winner == "O" -> "电脑赢了"
                full -> "平局"
                else -> "你是 X，点格子下棋"
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (winner == "X") palette.green else if (winner == "O") palette.red else palette.secondaryLabel
        )
        (0..2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..2).forEach { col ->
                    val index = row * 3 + col
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.sunkenBackground)
                            .clickable { play(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            board[index],
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light,
                            color = if (board[index] == "X") palette.accent else palette.orange
                        )
                    }
                }
            }
        }
        SolidButton(onClick = { reset() }, filled = false) { Text("再来一局") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell("你赢", playerWins.toString(), Modifier.weight(1f))
            StatCell("电脑赢", computerWins.toString(), Modifier.weight(1f))
            StatCell("平局", draws.toString(), Modifier.weight(1f))
        }
    }
}

// ── 2048 ──

private fun slideRow(row: List<Int>): Pair<List<Int>, Int> {
    val nums = row.filter { it != 0 }.toMutableList()
    var gained = 0
    var i = 0
    val merged = ArrayList<Int>()
    while (i < nums.size) {
        if (i + 1 < nums.size && nums[i] == nums[i + 1]) {
            merged.add(nums[i] * 2)
            gained += nums[i] * 2
            i += 2
        } else {
            merged.add(nums[i])
            i++
        }
    }
    while (merged.size < 4) merged.add(0)
    return merged to gained
}

private fun rotate(grid: List<Int>): List<Int> {
    val out = MutableList(16) { 0 }
    for (r in 0..3) for (c in 0..3) out[c * 4 + (3 - r)] = grid[r * 4 + c]
    return out
}

private fun spawn(grid: MutableList<Int>) {
    val empty = grid.indices.filter { grid[it] == 0 }
    if (empty.isEmpty()) return
    grid[empty.random()] = if (Random.nextInt(10) == 0) 4 else 2
}

@Composable
private fun Game2048(palette: com.toolbox.nativetoolbox.ui.theme.IosPalette) {
    val grid = remember { MutableList(16) { 0 }.also { spawn(it); spawn(it) }.toMutableStateList() }
    var score by rememberSaveable { mutableStateOf(0) }
    var best by rememberSaveable { mutableStateOf(0) }

    fun move(direction: Int) {
        var working = grid.toList()
        repeat(direction) { working = rotate(working) }
        var gained = 0
        val next = ArrayList<Int>(16)
        for (r in 0..3) {
            val (slid, g) = slideRow(working.subList(r * 4, r * 4 + 4))
            next.addAll(slid)
            gained += g
        }
        var result: List<Int> = next
        repeat((4 - direction) % 4) { result = rotate(result) }
        if (result != grid.toList()) {
            for (i in grid.indices) grid[i] = result[i]
            spawn(grid)
            score += gained
            if (score > best) best = score
        }
    }

    fun reset() {
        for (i in grid.indices) grid[i] = 0
        spawn(grid)
        spawn(grid)
        score = 0
    }

    val movable = grid.contains(0) || (0..3).any { dir ->
        var working = grid.toList()
        repeat(dir) { working = rotate(working) }
        (0..3).any { r -> slideRow(working.subList(r * 4, r * 4 + 4)).first != working.subList(r * 4, r * 4 + 4) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell("分数", score.toString(), Modifier.weight(1f))
            StatCell("最高", best.toString(), Modifier.weight(1f))
        }
        if (!movable) {
            Text("没有可以移动的方向了，游戏结束", style = MaterialTheme.typography.bodyMedium, color = palette.red)
        }
        (0..3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..3).forEach { col ->
                    val value = grid[row * 4 + col]
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    value == 0 -> palette.sunkenBackground
                                    value <= 4 -> palette.gray.copy(alpha = 0.3f)
                                    value <= 16 -> palette.yellow.copy(alpha = 0.5f)
                                    value <= 64 -> palette.orange.copy(alpha = 0.6f)
                                    value <= 256 -> palette.pink.copy(alpha = 0.6f)
                                    else -> palette.purple.copy(alpha = 0.7f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value > 0) {
                            Text(
                                value.toString(),
                                fontSize = if (value >= 1024) 16.sp else 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.label
                            )
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SolidButton(onClick = { move(0) }, modifier = Modifier.weight(1f), filled = false) { Text("←") }
            SolidButton(onClick = { move(1) }, modifier = Modifier.weight(1f), filled = false) { Text("↓") }
            SolidButton(onClick = { move(2) }, modifier = Modifier.weight(1f), filled = false) { Text("→") }
            SolidButton(onClick = { move(3) }, modifier = Modifier.weight(1f), filled = false) { Text("↑") }
        }
        SolidButton(onClick = { reset() }, filled = false) { Text("重开") }
        Text(
            "点方向按钮移动，相同数字相撞就合并。目标是拼出 2048。",
            style = MaterialTheme.typography.bodySmall,
            color = palette.tertiaryLabel
        )
    }
}

// ── 扫雷 ──

private class Cell(val mine: Boolean, var revealed: Boolean = false, var flagged: Boolean = false, var around: Int = 0)

@Composable
private fun Minesweeper(palette: com.toolbox.nativetoolbox.ui.theme.IosPalette) {
    val size = 8
    val mineCount = 10
    val cells = remember {
        val list = MutableList(size * size) { Cell(false) }
        val positions = (0 until size * size).shuffled().take(mineCount)
        val withMines = MutableList(size * size) { index -> Cell(positions.contains(index)) }
        withMines.forEachIndexed { index, cell ->
            val r = index / size
            val c = index % size
            var count = 0
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until size && nc in 0 until size && withMines[nr * size + nc].mine) count++
            }
            cell.around = count
        }
        list.clear()
        list.addAll(withMines)
        list.toMutableStateList()
    }
    var flagMode by rememberSaveable { mutableStateOf(false) }
    var dead by rememberSaveable { mutableStateOf(false) }

    val revealedCount = cells.count { it.revealed }
    val won = !dead && revealedCount == size * size - mineCount

    fun reveal(index: Int) {
        if (dead || won) return
        val cell = cells[index]
        if (cell.revealed || cell.flagged) return
        if (cell.mine) {
            dead = true
            cells.forEachIndexed { i, c -> if (c.mine) cells[i] = Cell(true, true, false, c.around) }
            return
        }
        // 空白格连锁展开
        val queue = ArrayDeque<Int>().apply { add(index) }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val c = cells[current]
            if (c.revealed) continue
            cells[current] = Cell(c.mine, true, false, c.around)
            if (c.around == 0) {
                val r = current / size
                val col = current % size
                for (dr in -1..1) for (dc in -1..1) {
                    val nr = r + dr
                    val nc = col + dc
                    if (nr in 0 until size && nc in 0 until size) {
                        val ni = nr * size + nc
                        if (!cells[ni].revealed && !cells[ni].mine) queue.add(ni)
                    }
                }
            }
        }
    }

    fun toggleFlag(index: Int) {
        val c = cells[index]
        if (c.revealed) return
        cells[index] = Cell(c.mine, false, !c.flagged, c.around)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell("地雷", mineCount.toString(), Modifier.weight(1f))
            StatCell("已插旗", cells.count { it.flagged }.toString(), Modifier.weight(1f))
            StatCell("已翻开", revealedCount.toString(), Modifier.weight(1f))
        }
        Text(
            when {
                dead -> "踩雷了"
                won -> "全部排完，赢了"
                else -> if (flagMode) "插旗模式：点格子标记地雷" else "翻开模式：点格子翻开"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (dead) palette.red else if (won) palette.green else palette.secondaryLabel
        )
        (0 until size).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (0 until size).forEach { col ->
                    val index = row * size + col
                    val cell = cells[index]
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    cell.revealed && cell.mine -> palette.red
                                    cell.revealed -> palette.cardBackground
                                    cell.flagged -> palette.orange.copy(alpha = 0.6f)
                                    else -> palette.sunkenBackground
                                }
                            )
                            .clickable { if (flagMode) toggleFlag(index) else reveal(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when {
                                cell.revealed && cell.mine -> "雷"
                                cell.revealed && cell.around > 0 -> cell.around.toString()
                                cell.flagged -> "旗"
                                else -> ""
                            },
                            fontSize = 11.sp,
                            color = palette.label
                        )
                    }
                }
            }
        }
        SolidButton(onClick = { flagMode = !flagMode }, filled = flagMode) {
            Text(if (flagMode) "插旗模式（点这里切回翻开）" else "切到插旗模式")
        }
    }
}

@Composable
fun ClassicGamesToolScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var game by rememberSaveable { mutableStateOf(GAME_TICTACTOE) }

    ToolScaffold {
        item { SectionHeader("选个游戏") }
        item {
            GroupedCard {
                CardPadding {
                    SegmentedPicker(
                        options = listOf("井字棋", "2048", "扫雷"),
                        selectedIndex = game,
                        onSelected = { game = it }
                    )
                }
            }
        }
        item { SectionHeader(listOf("井字棋", "2048", "扫雷")[game]) }
        item {
            GroupedCard {
                CardPadding {
                    when (game) {
                        GAME_TICTACTOE -> TicTacToe(palette)
                        GAME_2048 -> Game2048(palette)
                        else -> Minesweeper(palette)
                    }
                }
            }
        }
    }
}
