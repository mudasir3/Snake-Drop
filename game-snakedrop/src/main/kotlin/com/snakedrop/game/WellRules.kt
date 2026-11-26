package com.snakedrop.game

import com.snakedrop.game.GameConstants.WELL_ROWS
import com.snakedrop.game.GameConstants.WIDTH

/** Row detection, clearing, gravity and height of the well. */
internal object WellRules {
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
