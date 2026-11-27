@file:Suppress("MaxLineLength")

package com.snakedrop.game

import com.snakedrop.game.GameConstants.WELL_ROWS
import com.snakedrop.game.GameConstants.WIDTH

/** Row detection, clearing, gravity and height of the well. */
internal object WellRules {
    private const val MAX_RUBBLE = 4

    /** Points for clearing [rows] rows at once: 100 x rows squared. */
    fun rowClearPoints(rows: Int): Int = GameConstants.ROW_CLEAR_BASE * rows * rows

    /** Clears every full row and drops the rows above as whole rows. Returns how many rows were cleared. */
    fun clearFullRows(s: GameState): Int {
        var cleared = 0
        var write = WELL_ROWS - 1
        for (read in WELL_ROWS - 1 downTo 0) {
            if (isFull(s, read)) {
                cleared++
            } else {
                if (write != read) copyRow(s, read, write)
                write--
            }
        }
        for (r in write downTo 0) fillRow(s, r)
        if (cleared > 0) {
            s.score += rowClearPoints(cleared)
            s.rowsClearedTotal += cleared
            if (cleared > s.bestRowClear) s.bestRowClear = cleared
            s.events.add(Ev.ROWS_CLEARED, cleared)
        }
        return cleared
    }

    /** Removes the bottom [rows] rows (revive) and drops everything above. No score is awarded. */
    fun removeBottomRows(
        s: GameState,
        rows: Int,
    ) {
        val n = rows.coerceIn(0, WELL_ROWS)
        for (r in WELL_ROWS - 1 downTo n) copyRow(s, r - n, r)
        for (r in 0 until n) fillRow(s, r)
        refreshHeight(s)
    }

    /** Garbage sent for clearing [rows] rows at once: 0, 1, 2, 4 for singles to quads. */
    fun garbageFor(rows: Int): Int =
        when (rows) {
            0, 1 -> 0
            2 -> 1
            3 -> 2
            else -> GameConstants.QUAD_GARBAGE
        }

    /** Cancels incoming garbage with what [cleared] earns, queues the rest outgoing, and applies any garbage left. */
    fun exchangeGarbage(
        s: GameState,
        cleared: Int,
    ) {
        var send = garbageFor(cleared)
        val cancel = minOf(send, s.incomingGarbage)
        s.incomingGarbage -= cancel
        send -= cancel
        s.outgoingGarbage += send
        if (s.incomingGarbage > 0) {
            val n = s.incomingGarbage
            s.incomingGarbage = 0
            addGarbage(s, n)
        }
    }

    /** Raises the stack by [n] garbage rows (each full but for one shared gap). Ends the run when the stack overflows. */
    fun addGarbage(
        s: GameState,
        n: Int,
    ) {
        if (s.wellHeight + n > WELL_ROWS) {
            RunControl.end(s, EndCause.NO_SPAWN)
            return
        }
        for (r in 0 until WELL_ROWS - n) copyRow(s, r + n, r)
        val gap = s.rng.nextInt(WIDTH)
        for (i in 0 until n) {
            val r = WELL_ROWS - 1 - i
            for (c in 0 until WIDTH) s.well[Grid.idx(r, c)] = if (c == gap) 0 else GameConstants.GARBAGE_COLOR
        }
        refreshHeight(s)
        ArenaRules.onWellChanged(s)
        s.events.add(Ev.GARBAGE_RECEIVED, n)
    }

    /** Fills the bottom [rows] well rows with random colored cells and one 4-wide gap each, so a straight chunk can clear it. */
    fun addRubble(
        s: GameState,
        rows: Int,
    ) {
        val n = rows.coerceIn(0, MAX_RUBBLE)
        for (i in 0 until n) {
            val r = WELL_ROWS - 1 - i
            val gap = s.rng.nextInt(WIDTH - GameConstants.CHUNK_LEN + 1)
            for (c in 0 until WIDTH) {
                val inGap = c >= gap && c < gap + GameConstants.CHUNK_LEN
                s.well[Grid.idx(r, c)] = if (inGap) 0 else 1 + s.rng.nextInt(GameConstants.CHUNK_COLORS)
            }
        }
        refreshHeight(s)
    }

    /** Recomputes the cached stack height. */
    fun refreshHeight(s: GameState) {
        var h = 0
        for (r in 0 until WELL_ROWS) {
            if (!isEmpty(s, r)) {
                h = WELL_ROWS - r
                break
            }
        }
        s.wellHeight = h
    }

    private fun isFull(
        s: GameState,
        row: Int,
    ): Boolean {
        for (c in 0 until WIDTH) if (s.well[Grid.idx(row, c)] == 0) return false
        return true
    }

    private fun isEmpty(
        s: GameState,
        row: Int,
    ): Boolean {
        for (c in 0 until WIDTH) if (s.well[Grid.idx(row, c)] != 0) return false
        return true
    }

    private fun copyRow(
        s: GameState,
        from: Int,
        to: Int,
    ) {
        for (c in 0 until WIDTH) s.well[Grid.idx(to, c)] = s.well[Grid.idx(from, c)]
    }

    private fun fillRow(
        s: GameState,
        row: Int,
    ) {
        for (c in 0 until WIDTH) s.well[Grid.idx(row, c)] = 0
    }
}
