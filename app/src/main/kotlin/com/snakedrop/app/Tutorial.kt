@file:Suppress("MagicNumber", "MaxLineLength")

package com.snakedrop.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.snakedrop.render.Palette
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private class Page(val title: String, val body: String)

private val pages =
    listOf(
        Page("SWIPE TO STEER", "Swipe anywhere to turn the snake. Eat apples. Hit a wall or yourself and the run ends."),
        Page("EVERY 4TH APPLE DROPS YOUR TAIL", "Your last 4 segments break off and fall into the well below, in the same shape."),
        Page("FILL A ROW TO CLEAR IT", "You can't move falling blocks. Bend your tail to aim the drop. A full row disappears."),
        Page("DON'T STACK TOO HIGH", "A tall stack shrinks your arena. Clear rows to get your space back."),
        Page("NOW YOU TRY", "Swipe on the board to steer. Eat apples. This practice board wraps around and can't hurt you."),
    )

private const val LOOP_MS = 4000L
private const val COLS = 12

/** First-run animated tutorial. Four looping scenes drawn on a canvas; no assets. */
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var timeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { timeMs = (it - start) / NANOS_PER_MS_T }
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { i ->
                Box(Modifier.size(width = 28.dp, height = 6.dp).background(if (i == page) Palette.Accent else Palette.Grid))
            }
        }
        if (page == pages.lastIndex) {
            PracticePad()
        } else {
            Canvas(Modifier.fillMaxWidth().height(260.dp).background(Palette.Panel)) {
                val p = (timeMs % LOOP_MS).toFloat() / LOOP_MS
                when (page) {
                    0 -> sceneSteer(p)
                    1 -> sceneDetach(p)
                    2 -> sceneClear(p)
                    else -> sceneStack(p)
                }
            }
        }
        ArcadeText(pages[page].title, 18, Palette.Snake)
        ArcadeText(pages[page].body, 13, Palette.TextHi)
        Box(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) Box(Modifier.weight(1f)) { ArcadeButton("BACK", primary = false) { page-- } }
            Box(Modifier.weight(1f)) {
                ArcadeButton(if (page == pages.lastIndex) "START" else "NEXT") {
                    if (page == pages.lastIndex) onDone() else page++
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (page < pages.lastIndex) {
                Box(Modifier.padding(4.dp)) { SkipLink(onDone) }
            }
        }
    }
}

@Composable
private fun SkipLink(onDone: () -> Unit) {
    Box(Modifier.clickable(onClick = onDone).padding(8.dp)) { ArcadeText("SKIP", 12, Palette.TextDim) }
}

private const val NANOS_PER_MS_T = 1_000_000L

private class Board(val scope: DrawScope, val cell: Float, val origin: Offset, val rows: Int)

private fun DrawScope.board(rows: Int): Board {
    val cell = minOf(size.width / COLS, size.height / rows)
    val origin = Offset((size.width - cell * COLS) / 2f, (size.height - cell * rows) / 2f)
    return Board(this, cell, origin, rows)
}

private fun Board.sq(
    row: Float,
    col: Float,
    color: Color,
    scale: Float = 1f,
) {
    val pad = cell * (1f - scale) / 2f + cell * 0.08f
    scope.drawRect(
        color,
        Offset(origin.x + col * cell + pad, origin.y + row * cell + pad),
        Size(cell - 2 * pad, cell - 2 * pad),
    )
}

private fun Board.grid() {
    for (c in 0..COLS) scope.drawLine(
        Palette.Grid,
        Offset(origin.x + c * cell, origin.y),
        Offset(origin.x + c * cell, origin.y + rows * cell),
        1f,
    )
    for (r in 0..rows) scope.drawLine(
        Palette.Grid,
        Offset(origin.x, origin.y + r * cell),
        Offset(origin.x + COLS * cell, origin.y + r * cell),
        1f,
    )
}

private fun Board.dangerLine(row: Float) {
    scope.drawLine(Palette.Danger, Offset(origin.x, origin.y + row * cell), Offset(origin.x + COLS * cell, origin.y + row * cell), 3f)
}

private fun DrawScope.sceneSteer(p: Float) {
    val b = board(6)
    b.grid()
    val path = listOf(3 to 1, 3 to 2, 3 to 3, 3 to 4, 3 to 5, 3 to 6, 3 to 7, 2 to 7, 1 to 7, 1 to 8, 1 to 9)
    val head = 3 + (p * (path.size - 3)).toInt()
    val blink = p > 0.9f && (p * 20).toInt() % 2 == 0
    if (head < path.lastIndex) {
        b.sq(1f, 9f, Palette.Apple)
    } else if (!blink) {
        b.sq(1f, 9f, Palette.Apple, 1.3f)
    }
    for (i in 0 until 4) {
        val idx = (head - i).coerceAtLeast(0)
        b.sq(path[idx].first.toFloat(), path[idx].second.toFloat(), if (i == 0) Palette.SnakeHead else Palette.Snake)
    }
    if (head in 6..8) drawSwipeHint(b, 7f, 3f, up = true)
}

private fun drawSwipeHint(
    b: Board,
    col: Float,
    row: Float,
    up: Boolean,
) {
    val x = b.origin.x + (col + 0.5f) * b.cell + b.cell * 1.5f
    val y = b.origin.y + row * b.cell
    val dy = if (up) -b.cell * 1.2f else b.cell * 1.2f
    b.scope.drawLine(Palette.AccentAlt, Offset(x, y), Offset(x, y + dy), 4f)
    b.scope.drawCircle(Palette.AccentAlt, b.cell * 0.28f, Offset(x, y))
}

private fun DrawScope.sceneDetach(p: Float) {
    val b = board(8)
    b.grid()
    b.dangerLine(4f)
    val split = 0.35f
    val fallEnd = 0.8f
    if (p < split) {
        for (c in 1..8) b.sq(1f, c.toFloat(), if (c == 8) Palette.SnakeHead else Palette.Snake)
        b.sq(1f, 9f, Palette.Apple, 1f + 0.2f * abs(((p * 10) % 1f) - 0.5f))
        return
    }
    for (c in 5..9) b.sq(1f, c.toFloat(), if (c == 9) Palette.SnakeHead else Palette.Snake)
    val t = ((p - split) / (fallEnd - split)).coerceIn(0f, 1f)
    val row = 1f + t * 6f
    for (c in 1..4) b.sq(row, c.toFloat(), Palette.Chunks[3])
    for (c in 0 until COLS) if (c !in 1..4 && t >= 1f) b.sq(7f, c.toFloat(), Palette.Chunks[1 + c % 2], 0.9f)
}

private fun DrawScope.sceneClear(p: Float) {
    val b = board(5)
    b.grid()
    val fall = (p / 0.5f).coerceIn(0f, 1f)
    val clearing = p > 0.6f
    val shrink = if (clearing) 1f - ((p - 0.6f) / 0.3f).coerceIn(0f, 1f) else 1f
    for (c in 0 until COLS) {
        if (c in 4..7) continue
        b.sq(4f, c.toFloat(), Palette.Chunks[1 + c % 3], shrink)
    }
    for (c in 4..7) {
        val row = if (clearing) 4f else 0f + fall * 4f
        if (row < 5f) b.sq(row, c.toFloat(), Palette.Chunks[3], shrink)
    }
    if (p > 0.6f) {
        b.scope.drawRect(
            Palette.Snake.copy(alpha = (1f - abs(p - 0.65f) * 4f).coerceIn(0f, 0.5f)),
            Offset(b.origin.x, b.origin.y + 4 * b.cell),
            Size(COLS * b.cell, b.cell),
        )
    }
}

private fun DrawScope.sceneStack(p: Float) {
    val b = board(9)
    val height = 3 + (p * 5).toInt()
    val danger = 5f - (height - 5).coerceAtLeast(0) * 0.5f
    val arenaBottom = if (height > 5) 4.5f else 5f
    b.scope.drawRect(
        Palette.Grid.copy(alpha = 0.6f),
        Offset(b.origin.x, b.origin.y + arenaBottom * b.cell),
        Size(COLS * b.cell, (5f - arenaBottom) * b.cell),
    )
    b.grid()
    b.dangerLine(danger.coerceAtLeast(arenaBottom))
    for (r in 0 until height) for (c in 0 until COLS) b.sq(8f - r, c.toFloat(), Palette.Chunks[1 + (r + c) % 6], 0.9f)
    for (c in 2..5) b.sq(1f, c.toFloat(), if (c == 5) Palette.SnakeHead else Palette.Snake)
}

private const val PRACTICE_ROWS = 8
private const val PRACTICE_START_LEN = 3

/** Free-roam practice board: swipe to steer, wraps at the edges, eating apples never ends the run. */
@Composable
private fun PracticePad() {
    val snake = remember { mutableListOf(3 * COLS + 2, 3 * COLS + 3, 3 * COLS + 4) }
    val dir = remember { intArrayOf(1) }
    val apple = remember { intArrayOf(5 * COLS + 9) }
    var eaten by remember { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(170)
            val head = snake.last()
            var r = head / COLS
            var c = head % COLS
            when (dir[0]) {
                0 -> r = (r + PRACTICE_ROWS - 1) % PRACTICE_ROWS
                1 -> c = (c + 1) % COLS
                2 -> r = (r + 1) % PRACTICE_ROWS
                else -> c = (c + COLS - 1) % COLS
            }
            val next = r * COLS + c
            if (next in snake.drop(1)) {
                while (snake.size > PRACTICE_START_LEN) snake.removeAt(0)
            }
            snake.add(next)
            if (next == apple[0]) {
                eaten++
                var a: Int
                do a = Random.nextInt(COLS * PRACTICE_ROWS) while (a in snake)
                apple[0] = a
            } else {
                snake.removeAt(0)
            }
            version++
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ArcadeText("APPLES $eaten", 14, Palette.Snake)
        Canvas(
            Modifier.fillMaxWidth().height(260.dp).background(Palette.Panel).pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(onDragStart = {
                    dx = 0f
                    dy = 0f
                }) { change, drag ->
                    change.consume()
                    dx += drag.x
                    dy += drag.y
                    if (abs(dx) >= 30f || abs(dy) >= 30f) {
                        val d = if (abs(dx) > abs(dy)) (if (dx > 0) 1 else 3) else (if (dy > 0) 2 else 0)
                        if (d != (dir[0] + 2) % 4) dir[0] = d
                        dx = 0f
                        dy = 0f
                    }
                }
            },
        ) {
            version.let { }
            val b = board(PRACTICE_ROWS)
            b.grid()
            b.sq((apple[0] / COLS).toFloat(), (apple[0] % COLS).toFloat(), Palette.Apple)
            snake.forEachIndexed { i, cell ->
                b.sq((cell / COLS).toFloat(), (cell % COLS).toFloat(), if (i == snake.lastIndex) Palette.SnakeHead else Palette.Snake)
            }
        }
    }
}
