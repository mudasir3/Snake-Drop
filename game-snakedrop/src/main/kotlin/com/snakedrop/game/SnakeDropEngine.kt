@file:Suppress("MaxLineLength")

package com.snakedrop.game

import com.snakedrop.game.GameConstants.REVIVE_GRACE_US
import com.snakedrop.game.GameConstants.TICK_US

/**
 * Entry point of the rules. Deterministic: the same seed and the same sequence of
 * [queueTurn] calls between [step] calls always produce the same state.
 */
public object SnakeDropEngine {
    /** Creates a fresh run: snake placed, first apple spawned from [seed]. */
    public fun newRun(
        seed: Long,
        config: GameConfig = GameConfig(),
    ): GameState {
        val s = GameState(seed, config)
        SnakeRules.placeStartSnake(s)
        RunControl.spawnApple(s)
        return s
    }

    /** Queues a turn. Returns false when it is ignored (no-op, reversal, queue full, run not active). */
    public fun queueTurn(
        s: GameState,
        dir: Int,
    ): Boolean = SnakeRules.queueTurn(s, dir)

    /** Advances the simulation by exactly one fixed tick (1/120 s). Allocation free. */
    public fun step(s: GameState) {
        if (s.phase != Phase.RUNNING) return
        s.tick++
        if (s.graceUs > 0) {
            s.graceUs = maxOf(0, s.graceUs - TICK_US)
            return
        }
        ChunkRules.tick(s)
        if (s.phase != Phase.RUNNING) return
        ArenaRules.tick(s)
        if (s.phase != Phase.RUNNING) return
        SnakeRules.tick(s)
    }

    /** Pauses a running run. */
    public fun pause(s: GameState) {
        if (s.phase == Phase.RUNNING) s.phase = Phase.PAUSED
    }

    /** Resumes a paused run. */
    public fun resume(s: GameState) {
        if (s.phase == Phase.PAUSED) s.phase = Phase.RUNNING
    }

    /** True when a revive is allowed now. */
    public fun canRevive(s: GameState): Boolean = s.phase == Phase.ENDED && !s.reviveUsed && s.endCause != EndCause.NO_SPACE

    /**
     * Continues an ended run once: clears the bottom well rows, keeps the score and gives a short freeze.
     * Returns false when a revive is not allowed.
     */
    public fun revive(s: GameState): Boolean {
        if (!canRevive(s)) return false
        s.reviveUsed = true
        s.phase = Phase.RUNNING
        s.endCause = EndCause.NONE
        WellRules.removeBottomRows(s, s.config.reviveRowsCleared)
        s.arenaRows = maxOf(ArenaRules.targetRows(s), ArenaRules.snakeRowsUsed(s))
        s.shrinkWarnUs = 0
        if (s.arenaRows > ArenaRules.targetRows(s)) ArenaRules.onWellChanged(s)
        RunControl.relocateAppleIfOutside(s, s.arenaRows)
        if (s.appleIndex < 0) RunControl.spawnApple(s)
        if (s.hasPending && !s.chunkActive) {
            if (ChunkRules.pendingFits(s)) ChunkRules.spawnPending(s) else s.hasPending = false
        }
        s.turns.clear()
        s.moveAccum = 0
        SnakeRules.chooseSafeHeading(s)
        s.graceUs = REVIVE_GRACE_US
        s.events.add(Ev.REVIVED)
        return true
    }
}
