package com.snakedrop.game

import com.snakedrop.game.GameConstants.CHUNK_CELL_POINTS
import com.snakedrop.game.GameConstants.CHUNK_COLORS
import com.snakedrop.game.GameConstants.CHUNK_LEN
import com.snakedrop.game.GameConstants.TICK_US
import com.snakedrop.game.GameConstants.WELL_ROWS

/** Detaching the tail into a chunk, spawning it in the well, falling and locking. */
internal object ChunkRules {
    /**
     * Detaches the [CHUNK_LEN] tail-most segments, keeping their exact shape and columns, and hands them to the
     * well. The snake returns to its start length.
     */
    fun detachTail(s: GameState) {
        var minRow = Int.MAX_VALUE
        for (i in 0 until CHUNK_LEN) minRow = minOf(minRow, Grid.row(s.snakeCell(i)))
        // Stage into the pending slot; relative rows keep the shape.
        if (s.hasPending) {
            RunControl.end(s, EndCause.NO_SPAWN)
            return
        }
        for (i in 0 until CHUNK_LEN) {
            val cell = s.snakeCell(i)
            s.pendRow[i] = Grid.row(cell) - minRow
            s.pendCol[i] = Grid.col(cell)
            s.arena[cell] = Cell.EMPTY
        }
        s.snakeStart = (s.snakeStart + CHUNK_LEN) % s.snake.size
        s.snakeLen -= CHUNK_LEN
        s.hasPending = true
        s.applesSinceDetach = 0
        if (!s.chunkActive) spawnPending(s)
    }

    /** True when the pending chunk does not overlap the stack at the top of the well. */
    fun pendingFits(s: GameState): Boolean {
        for (i in 0 until CHUNK_LEN) {
            if (s.well[Grid.idx(s.pendRow[i], s.pendCol[i])] != 0) return false
        }
        return true
    }

    /** Moves the pending chunk to the top of the well. Ends the run when it overlaps the stack. */
    fun spawnPending(s: GameState) {
        if (!s.hasPending) return
        if (!pendingFits(s)) {
            RunControl.end(s, EndCause.NO_SPAWN)
            return
        }
        for (i in 0 until CHUNK_LEN) {
            s.chunkRow[i] = s.pendRow[i]
            s.chunkCol[i] = s.pendCol[i]
        }
        s.hasPending = false
        s.chunkActive = true
        s.chunkTop = 0
        s.chunkColor = s.chunksLanded % CHUNK_COLORS + 1
        s.chunkAccumUs = 0
        s.events.add(Ev.CHUNK_DETACHED)
    }

    /** Advances the fall timer by one tick and moves or locks the chunk. */
    fun tick(s: GameState) {
        if (!s.chunkActive) return
        s.chunkAccumUs += TICK_US
        val interval = s.config.chunkFallUs(s.applesEaten)
        while (s.chunkAccumUs >= interval && s.chunkActive && s.phase == Phase.RUNNING) {
            s.chunkAccumUs -= interval
            fallOne(s)
        }
    }

    private fun fallOne(s: GameState) {
        if (canMoveDown(s)) s.chunkTop++ else lock(s)
    }

    private fun canMoveDown(s: GameState): Boolean {
        for (i in 0 until CHUNK_LEN) {
            val row = s.chunkTop + s.chunkRow[i] + 1
            if (row >= WELL_ROWS) return false
            if (s.well[Grid.idx(row, s.chunkCol[i])] != 0) return false
        }
        return true
    }

    private fun lock(s: GameState) {
        for (i in 0 until CHUNK_LEN) {
            s.well[Grid.idx(s.chunkTop + s.chunkRow[i], s.chunkCol[i])] = s.chunkColor
        }
        s.chunksLanded++
        s.chunkActive = false
        s.score += CHUNK_CELL_POINTS * CHUNK_LEN
        s.events.add(Ev.CHUNK_LANDED, CHUNK_LEN)
        val cleared = WellRules.clearFullRows(s)
        WellRules.refreshHeight(s)
        WellRules.exchangeGarbage(s, cleared)
        ArenaRules.onWellChanged(s)
        spawnPending(s)
    }
}
