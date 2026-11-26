package com.snakedrop.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.snakedrop.game.Cell
import com.snakedrop.game.GameConstants.ARENA_ROWS
import com.snakedrop.game.GameConstants.CHUNK_LEN
import com.snakedrop.game.GameConstants.WELL_ROWS
import com.snakedrop.game.GameConstants.WIDTH
import com.snakedrop.game.GameState

/** Total board rows: arena stacked above the well. */
const val BOARD_ROWS: Int = ARENA_ROWS + WELL_ROWS

private const val INSET = 0.08f
private const val WARN_PULSE_MS = 250L

/** Draws the arena and the well for [s] on a [cell]-sized grid at [origin]. Hard edges, no gradients. */
fun DrawScope.drawBoard(
    s: GameState,
    cell: Float,
    origin: Offset,
    nowMs: Long,
) {
    drawRect(Palette.Panel, origin, Size(cell * WIDTH, cell * BOARD_ROWS))
    drawGridLines(cell, origin)
    drawLockedWell(s, cell, origin)
    drawFallingChunk(s, cell, origin)
    drawArenaCells(s, cell, origin)
    drawRemovedArenaRows(s, cell, origin)
    drawDangerLine(s, cell, origin, nowMs)
}

private fun DrawScope.drawGridLines(
    cell: Float,
    origin: Offset,
) {
    val w = cell * WIDTH
    val h = cell * BOARD_ROWS
    for (c in 0..WIDTH) {
        val x = origin.x + c * cell
        drawLine(Palette.Grid, Offset(x, origin.y), Offset(x, origin.y + h), 1f)
    }
    for (r in 0..BOARD_ROWS) {
        val y = origin.y + r * cell
        drawLine(Palette.Grid, Offset(origin.x, y), Offset(origin.x + w, y), 1f)
    }
}

private fun DrawScope.square(
    color: Color,
    row: Int,
    col: Int,
    cell: Float,
    origin: Offset,
) {
    val pad = cell * INSET
    drawRect(
        color,
        Offset(origin.x + col * cell + pad, origin.y + row * cell + pad),
        Size(cell - 2 * pad, cell - 2 * pad),
    )
}

private fun DrawScope.drawLockedWell(
    s: GameState,
    cell: Float,
    origin: Offset,
) {
    for (r in 0 until WELL_ROWS) {
        for (c in 0 until WIDTH) {
            val id = s.well[r * WIDTH + c]
            if (id != 0) square(Palette.Chunks[id], ARENA_ROWS + r, c, cell, origin)
        }
    }
}

private fun DrawScope.drawFallingChunk(
    s: GameState,
    cell: Float,
    origin: Offset,
) {
    if (!s.chunkActive) return
    for (i in 0 until CHUNK_LEN) {
        square(Palette.Chunks[s.chunkColor], ARENA_ROWS + s.chunkTop + s.chunkRow[i], s.chunkCol[i], cell, origin)
    }
}

private fun DrawScope.drawArenaCells(
    s: GameState,
    cell: Float,
    origin: Offset,
) {
    for (r in 0 until ARENA_ROWS) {
        for (c in 0 until WIDTH) {
            val color =
                when (s.arena[r * WIDTH + c]) {
                    Cell.BODY -> Palette.Snake
                    Cell.HEAD -> Palette.SnakeHead
                    Cell.APPLE -> Palette.Apple
                    else -> null
                }
            if (color != null) square(color, r, c, cell, origin)
        }
    }
}

private fun DrawScope.drawRemovedArenaRows(
    s: GameState,
    cell: Float,
    origin: Offset,
) {
    if (s.arenaRows >= ARENA_ROWS) return
    val top = origin.y + s.arenaRows * cell
    drawRect(Palette.Ink, Offset(origin.x, top), Size(cell * WIDTH, (ARENA_ROWS - s.arenaRows) * cell))
}

private fun DrawScope.drawDangerLine(
    s: GameState,
    cell: Float,
    origin: Offset,
    nowMs: Long,
) {
    val y = origin.y + s.arenaRows * cell
    val warning = s.shrinkWarnUs > 0
    val on = !warning || (nowMs / WARN_PULSE_MS) % 2 == 0L
    if (on) drawLine(Palette.Danger, Offset(origin.x, y), Offset(origin.x + cell * WIDTH, y), if (warning) 4f else 2f)
}
