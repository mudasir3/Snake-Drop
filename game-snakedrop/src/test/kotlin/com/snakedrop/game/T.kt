package com.snakedrop.game

import com.snakedrop.game.GameConstants.TICK_US

/** Builders for hand-made states. Cells are (row, col) pairs. */
internal object T {
    const val SEED = 1234L

    fun run(config: GameConfig = GameConfig()): GameState = SnakeDropEngine.newRun(SEED, config)

    /** Replaces the snake with [cells] (tail first) and the given heading. Clears the old apple. */
    fun snake(
        s: GameState,
        heading: Int,
        vararg cells: Pair<Int, Int>,
    ): GameState {
        RunControl.clearArena(s)
        s.snakeStart = 0
        s.snakeLen = cells.size
        s.applesSinceDetach = cells.size - GameConstants.START_LEN
        cells.forEachIndexed { i, (r, c) ->
            val idx = Grid.idx(r, c)
            s.snake[i] = idx
            s.arena[idx] = if (i == cells.size - 1) Cell.HEAD else Cell.BODY
        }
        s.heading = heading
        s.turns.clear()
        return s
    }

    fun apple(
        s: GameState,
        row: Int,
        col: Int,
    ) {
        if (s.appleIndex >= 0) s.arena[s.appleIndex] = Cell.EMPTY
        s.appleIndex = Grid.idx(row, col)
        s.arena[s.appleIndex] = Cell.APPLE
    }

    /** Runs [n] fixed ticks. */
    fun ticks(
        s: GameState,
        n: Int,
    ) = repeat(n) { SnakeDropEngine.step(s) }

    /** Runs enough ticks for [n] snake moves at the current speed. */
    fun moves(
        s: GameState,
        n: Int,
    ) {
        var done = 0
        while (done < n && s.phase == Phase.RUNNING) {
            val before = s.moveAccum
            SnakeDropEngine.step(s)
            if (s.moveAccum < before) done++
        }
    }

    /** Ticks needed to cover [ms] milliseconds, rounding up. */
    fun ticksFor(ms: Int): Int = (ms * 1000 + TICK_US - 1) / TICK_US

    /** Fills well rows from ASCII lines (top to bottom, '.' empty, digit = color). Lines map to the bottom rows. */
    fun well(
        s: GameState,
        vararg lines: String,
    ) {
        val start = GameConstants.WELL_ROWS - lines.size
        lines.forEachIndexed { i, line ->
            line.forEachIndexed { c, ch -> s.well[Grid.idx(start + i, c)] = if (ch == '.') 0 else ch - '0' }
        }
        WellRules.refreshHeight(s)
    }

    /** Puts a falling chunk made of (rowOffset, col) cells at the top of the well. */
    fun chunk(
        s: GameState,
        vararg cells: Pair<Int, Int>,
    ) {
        cells.forEachIndexed { i, (r, c) ->
            s.chunkRow[i] = r
            s.chunkCol[i] = c
        }
        s.chunkActive = true
        s.chunkTop = 0
        s.chunkColor = 1
        s.chunkAccumUs = 0
    }

    /** Steps until the active chunk has locked (or the run ends). */
    fun untilLanded(s: GameState) {
        var guard = 0
        while (s.chunkActive && s.phase == Phase.RUNNING && guard++ < LAND_GUARD) SnakeDropEngine.step(s)
    }

    /** Freezes the snake so chunk/arena tests are not disturbed by movement. */
    fun freezeSnake(s: GameState) {
        s.moveAccum = Int.MIN_VALUE / 2
    }

    private const val LAND_GUARD = 10_000
}
