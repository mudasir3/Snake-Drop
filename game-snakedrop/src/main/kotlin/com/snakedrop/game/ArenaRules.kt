package com.snakedrop.game

import com.snakedrop.game.GameConstants.ARENA_ROWS
import com.snakedrop.game.GameConstants.TICK_US

/** Arena size as a function of stack height, with warning and crush handling. */
internal object ArenaRules {
    /** Arena rows for the current stack: 10 - max(0, h - threshold), never below the configured minimum. */
    fun targetRows(s: GameState): Int {
        val c = s.config
        val raw = ARENA_ROWS - maxOf(0, s.wellHeight - c.arenaShrinkThreshold)
        return raw.coerceIn(c.arenaMinRows, ARENA_ROWS)
    }

    /** Call whenever the stack changed. Grows the arena at once, or starts the shrink warning. */
    fun onWellChanged(s: GameState) {
        val target = targetRows(s)
        when {
            target > s.arenaRows -> {
                s.arenaRows = target
                s.events.add(Ev.ARENA_GROWN, target)
                s.shrinkWarnUs = 0
            }
            target < s.arenaRows ->
                if (s.shrinkWarnUs == 0) {
                    s.shrinkWarnUs = s.config.shrinkWarningMs * US_PER_MS
                    s.events.add(Ev.ARENA_WARNING, target)
                }
            else -> s.shrinkWarnUs = 0
        }
    }

    /** Advances the warning timer; applies the shrink and checks for a crush when it expires. */
    fun tick(s: GameState) {
        if (s.shrinkWarnUs <= 0) return
        s.shrinkWarnUs -= TICK_US
        if (s.shrinkWarnUs > 0) return
        s.shrinkWarnUs = 0
        val target = targetRows(s)
        if (target >= s.arenaRows) return
        s.arenaRows = target
        s.events.add(Ev.ARENA_SHRUNK, target)
        if (snakeBeyond(s, target)) {
            RunControl.end(s, EndCause.CRUSHED)
            return
        }
        RunControl.relocateAppleIfOutside(s, target)
    }

    /** Highest snake row plus one. */
    fun snakeRowsUsed(s: GameState): Int {
        var maxRow = -1
        for (i in 0 until s.snakeLen) maxRow = maxOf(maxRow, Grid.row(s.snakeCell(i)))
        return maxRow + 1
    }

    private fun snakeBeyond(
        s: GameState,
        rows: Int,
    ): Boolean = snakeRowsUsed(s) > rows

    private const val US_PER_MS = 1000
}
