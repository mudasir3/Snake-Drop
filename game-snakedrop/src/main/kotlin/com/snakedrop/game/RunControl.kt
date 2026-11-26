package com.snakedrop.game

import com.snakedrop.game.GameConstants.ARENA_ROWS
import com.snakedrop.game.GameConstants.WIDTH

/** Shared helpers used by the rule objects. */
internal object RunControl {
    /** Ends the run with [cause]. Safe to call once; later calls are ignored. */
    fun end(
        s: GameState,
        cause: EndCause,
    ) {
        if (s.phase == Phase.ENDED) return
        s.phase = Phase.ENDED
        s.endCause = cause
        s.events.add(Ev.RUN_ENDED, cause.ordinal)
    }

    /**
     * Places a new apple on a random free arena cell inside the current arena rows.
     * Ends the run with [EndCause.NO_SPACE] when no cell is free.
     */
    fun spawnApple(s: GameState) {
        val limit = s.arenaRows * WIDTH
        var free = 0
        for (i in 0 until limit) if (s.arena[i] == Cell.EMPTY) free++
        if (free == 0) {
            s.appleIndex = -1
            end(s, EndCause.NO_SPACE)
            return
        }
        var k = s.rng.nextInt(free)
        for (i in 0 until limit) {
            if (s.arena[i] != Cell.EMPTY) continue
            if (k == 0) {
                s.arena[i] = Cell.APPLE
                s.appleIndex = i
                return
            }
            k--
        }
    }

    /** Removes the apple from the board if it lies in a row at or beyond [rows], then respawns it. */
    fun relocateAppleIfOutside(
        s: GameState,
        rows: Int,
    ) {
        val a = s.appleIndex
        if (a >= 0 && Grid.row(a) >= rows) {
            s.arena[a] = Cell.EMPTY
            s.appleIndex = -1
            spawnApple(s)
        }
    }

    /** Resets arena cells and the apple pointer to empty. */
    fun clearArena(s: GameState) {
        for (i in 0 until WIDTH * ARENA_ROWS) s.arena[i] = Cell.EMPTY
        s.appleIndex = -1
    }
}
