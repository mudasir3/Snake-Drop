@file:Suppress("ReturnCount", "MaxLineLength")

package com.snakedrop.game

import com.snakedrop.game.GameConstants.ARENA_ROWS
import com.snakedrop.game.GameConstants.START_LEN
import com.snakedrop.game.GameConstants.WELL_ROWS
import com.snakedrop.game.GameConstants.WIDTH

/** Structural checks that must hold after every tick. Used by tests and the fuzzer; not by the app loop. */
public object GameInvariants {
    /** Returns a description of the first violated invariant, or null when the state is consistent. */
    public fun check(s: GameState): String? = checkSnake(s) ?: checkApple(s) ?: checkWell(s) ?: checkArena(s) ?: checkChunk(s)

    private fun checkSnake(s: GameState): String? {
        if (s.snakeLen != START_LEN + s.applesSinceDetach) {
            return "snakeLen=${s.snakeLen} but applesSinceDetach=${s.applesSinceDetach}"
        }
        var bodyCells = 0
        for (i in 0 until WIDTH * ARENA_ROWS) if (s.arena[i] == Cell.BODY || s.arena[i] == Cell.HEAD) bodyCells++
        if (bodyCells != s.snakeLen) return "arena has $bodyCells snake cells, snakeLen=${s.snakeLen}"
        for (i in 0 until s.snakeLen) {
            val expected = if (i == s.snakeLen - 1) Cell.HEAD else Cell.BODY
            if (s.arena[s.snakeCell(i)] != expected) return "snake cell $i not marked $expected in arena"
        }
        if (s.phase != Phase.ENDED && ArenaRules.snakeRowsUsed(s) > s.arenaRows) return "snake outside arenaRows"
        return null
    }

    private fun checkApple(s: GameState): String? {
        var apples = 0
        for (i in 0 until WIDTH * ARENA_ROWS) if (s.arena[i] == Cell.APPLE) apples++
        if (s.phase == Phase.ENDED && s.endCause == EndCause.NO_SPACE) return null
        if (apples > 1) return "$apples apples on the board"
        if (s.phase != Phase.ENDED && apples != 1) return "no apple while running"
        if (apples == 1 && s.arena[s.appleIndex] != Cell.APPLE) return "appleIndex does not point at the apple"
        if (apples == 1 && s.phase != Phase.ENDED && Grid.row(s.appleIndex) >= s.arenaRows) return "apple outside arena"
        return null
    }

    private fun checkWell(s: GameState): String? {
        var top = WELL_ROWS
        for (r in 0 until WELL_ROWS) {
            var filled = 0
            for (c in 0 until WIDTH) {
                val v = s.well[Grid.idx(r, c)]
                if (v < 0 || v > GameConstants.CHUNK_COLORS) return "bad well value $v"
                if (v != 0) filled++
            }
            if (filled == WIDTH) return "full well row $r left uncleared"
            if (filled > 0 && r < top) top = r
        }
        val height = if (top == WELL_ROWS) 0 else WELL_ROWS - top
        return if (height != s.wellHeight) "wellHeight=${s.wellHeight}, actual $height" else null
    }

    private fun checkArena(s: GameState): String? =
        if (s.arenaRows < s.config.arenaMinRows || s.arenaRows > ARENA_ROWS) "arenaRows=${s.arenaRows}" else null

    private fun checkChunk(s: GameState): String? {
        if (!s.chunkActive) return null
        for (i in 0 until GameConstants.CHUNK_LEN) {
            val row = s.chunkTop + s.chunkRow[i]
            if (row < 0 || row >= WELL_ROWS) return "chunk cell outside well"
            if (s.well[Grid.idx(row, s.chunkCol[i])] != 0) return "chunk overlaps stack"
        }
        return null
    }
}
