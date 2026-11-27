package com.snakedrop.game

import com.snakedrop.engine.core.XorShift64

/** Everything needed to recreate a run from scratch: seed plus mode. [levelId] 0 means endless. */
public data class RunSetup(
    val seed: Long,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val levelId: Int = 0,
) {
    /** The level, or null for endless runs. */
    public fun level(): Level? = Levels.byId(levelId)

    /** Config for this setup: the level's config when a level is set, else the difficulty preset. */
    public fun config(): GameConfig = level()?.config() ?: difficulty.config

    /** Creates a fresh run. */
    public fun create(): GameState = SnakeDropEngine.newRun(seed, config())
}

/** Seeds shared by all players on the same day, so everyone races the same board. */
public object DailySeed {
    private const val MIX = -7046029254386353131L

    /** Seed for the given day number (days since the Unix epoch, UTC). */
    public fun forEpochDay(day: Long): Long = XorShift64(day * MIX + 1L).nextLong()
}
