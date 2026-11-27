@file:Suppress("MagicNumber")

package com.snakedrop.game

/** What a level asks the player to reach. */
public enum class GoalType { APPLES, ROWS, SCORE, CHUNKS }

/**
 * One goal level. [parSeconds] is the time for 3 stars (1.5x for 2 stars); the values are first estimates and
 * need tuning from playtests.
 */
public class Level(
    public val id: Int,
    public val difficulty: Difficulty,
    public val goalType: GoalType,
    public val target: Int,
    public val rubbleRows: Int,
    public val parSeconds: Int,
) {
    /** Config for this level: the difficulty preset plus this level's rubble. */
    public fun config(): GameConfig = difficulty.config.copy(startRubbleRows = rubbleRows)
}

/** Level list and goal checks. */
public object Levels {
    public val all: List<Level> =
        listOf(
            Level(1, Difficulty.SMALL, GoalType.APPLES, 8, 0, 60),
            Level(2, Difficulty.SMALL, GoalType.CHUNKS, 2, 0, 90),
            Level(3, Difficulty.SMALL, GoalType.ROWS, 1, 1, 120),
            Level(4, Difficulty.SMALL, GoalType.SCORE, 400, 0, 150),
            Level(5, Difficulty.MEDIUM, GoalType.ROWS, 2, 1, 150),
            Level(6, Difficulty.MEDIUM, GoalType.CHUNKS, 5, 0, 180),
            Level(7, Difficulty.MEDIUM, GoalType.ROWS, 3, 1, 200),
            Level(8, Difficulty.MEDIUM, GoalType.SCORE, 600, 1, 240),
            Level(9, Difficulty.HARD, GoalType.ROWS, 3, 2, 240),
            Level(10, Difficulty.HARD, GoalType.CHUNKS, 8, 2, 270),
            Level(11, Difficulty.HARD, GoalType.ROWS, 5, 3, 300),
            Level(12, Difficulty.HARD, GoalType.SCORE, 700, 3, 360),
        )

    /** Looks up a level by id, or null. */
    public fun byId(id: Int): Level? = all.firstOrNull { it.id == id }

    /** Current value of the level's goal in [s]. */
    public fun progress(
        s: GameState,
        level: Level,
    ): Int =
        when (level.goalType) {
            GoalType.APPLES -> s.applesEaten
            GoalType.ROWS -> s.rowsClearedTotal
            GoalType.SCORE -> s.score
            GoalType.CHUNKS -> s.chunksLanded
        }

    /** True once the goal is reached. */
    public fun isComplete(
        s: GameState,
        level: Level,
    ): Boolean = progress(s, level) >= level.target

    /** 1..3 stars by completion time. Call after the level is complete. */
    public fun stars(
        s: GameState,
        level: Level,
    ): Int {
        val seconds = (s.tick / GameConstants.SIM_HZ).toInt()
        return when {
            seconds <= level.parSeconds -> 3
            seconds <= level.parSeconds * 3 / 2 -> 2
            else -> 1
        }
    }
}
