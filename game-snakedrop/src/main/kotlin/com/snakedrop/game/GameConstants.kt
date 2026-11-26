package com.snakedrop.game

/** Fixed layout and timing constants. These define the length invariant and are never remotely tunable. */
public object GameConstants {
    /** Board width in cells (PRD 3). */
    public const val WIDTH: Int = 12

    /** Arena rows at full size (PRD 3). */
    public const val ARENA_ROWS: Int = 10

    /** Well rows (PRD 3). */
    public const val WELL_ROWS: Int = 9

    /** Snake length at run start. */
    public const val START_LEN: Int = 4

    /** Apples between tail detaches. */
    public const val APPLES_PER_CHUNK: Int = 4

    /** Segments in a detached chunk. After detach the snake returns to [START_LEN]. */
    public const val CHUNK_LEN: Int = 4

    /** Fixed simulation rate in ticks per second. */
    public const val SIM_HZ: Int = 120

    /** Duration of one simulation tick in microseconds. */
    public const val TICK_US: Int = 1_000_000 / SIM_HZ

    /** Snake speed accumulator threshold: one move each time it reaches this (speed is stored x100). */
    public const val MOVE_THRESHOLD: Int = SIM_HZ * 100

    /** Number of distinct chunk colors. */
    public const val CHUNK_COLORS: Int = 6

    /** Points per apple. */
    public const val APPLE_POINTS: Int = 10

    /** Points per cell of a landed chunk. */
    public const val CHUNK_CELL_POINTS: Int = 5

    /** Base points per row clear; total = base x rows squared. */
    public const val ROW_CLEAR_BASE: Int = 100

    /** Freeze after a revive, in microseconds. */
    public const val REVIVE_GRACE_US: Int = 1_500_000
}

/** Cell index helpers. Index = row * WIDTH + col, row 0 at the top. */
public object Grid {
    /** Returns the flat index for ([row], [col]). */
    public fun idx(
        row: Int,
        col: Int,
    ): Int = row * GameConstants.WIDTH + col

    /** Returns the row of [index]. */
    public fun row(index: Int): Int = index / GameConstants.WIDTH

    /** Returns the column of [index]. */
    public fun col(index: Int): Int = index % GameConstants.WIDTH
}

/** Heading values and helpers. */
public object Dir {
    public const val UP: Int = 0
    public const val RIGHT: Int = 1
    public const val DOWN: Int = 2
    public const val LEFT: Int = 3

    internal val DROW: IntArray = intArrayOf(-1, 0, 1, 0)
    internal val DCOL: IntArray = intArrayOf(0, 1, 0, -1)

    /** Returns the direction opposite to [d]. */
    public fun opposite(d: Int): Int = (d + 2) % 4
}

/** Arena cell contents. */
public object Cell {
    public const val EMPTY: Int = 0
    public const val BODY: Int = 1
    public const val HEAD: Int = 2
    public const val APPLE: Int = 3
}

/** Why a run ended. */
public enum class EndCause { NONE, WALL, SELF, CRUSHED, NO_SPAWN, NO_SPACE }

/** Run phase. */
public enum class Phase { RUNNING, PAUSED, ENDED }
