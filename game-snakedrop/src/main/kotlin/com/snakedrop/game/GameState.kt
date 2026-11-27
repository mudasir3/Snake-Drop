package com.snakedrop.game

import com.snakedrop.engine.core.IntRingQueue
import com.snakedrop.engine.core.XorShift64
import com.snakedrop.game.GameConstants.ARENA_ROWS
import com.snakedrop.game.GameConstants.CHUNK_LEN
import com.snakedrop.game.GameConstants.WELL_ROWS
import com.snakedrop.game.GameConstants.WIDTH

/**
 * Complete mutable state of one run. Flat primitive arrays only (see schema.md); the engine mutates it
 * in place so a step allocates nothing. Construct through [SnakeDropEngine.newRun].
 */
public class GameState internal constructor(
    public val seed: Long,
    public val config: GameConfig,
) {
    internal val rng: XorShift64 = XorShift64(seed)

    /** Total simulation ticks since run start. */
    public var tick: Long = 0L
        internal set

    /** Arena grid: [Cell] values. Index = row * WIDTH + col. */
    public val arena: IntArray = IntArray(WIDTH * ARENA_ROWS)

    /** Ring buffer of snake cell indices, tail first. */
    internal val snake: IntArray = IntArray(WIDTH * ARENA_ROWS)
    internal var snakeStart: Int = 0

    /** Snake length in cells. */
    public var snakeLen: Int = 0
        internal set

    /** Current heading, one of [Dir]. */
    public var heading: Int = Dir.RIGHT
        internal set
    internal val turns: IntRingQueue = IntRingQueue(MAX_QUEUED_TURNS)

    /** Snake speed in ticks per second x100. */
    public var snakeSpeed100: Int = config.snakeSpeedStart100
        internal set
    internal var moveAccum: Int = 0

    /** Cell index of the apple, or -1. */
    public var appleIndex: Int = -1
        internal set
    public var applesEaten: Int = 0
        internal set
    public var applesSinceDetach: Int = 0
        internal set

    /** Well grid: 0 empty, 1..6 chunk color id. Row 0 is the top, row 8 the floor. */
    public val well: IntArray = IntArray(WIDTH * WELL_ROWS)

    /** True while a chunk is falling. */
    public var chunkActive: Boolean = false
        internal set
    public val chunkRow: IntArray = IntArray(CHUNK_LEN)
    public val chunkCol: IntArray = IntArray(CHUNK_LEN)

    /** Well row of the chunk's top edge. */
    public var chunkTop: Int = 0
        internal set
    public var chunkColor: Int = 1
        internal set
    internal var chunkAccumUs: Int = 0

    internal var hasPending: Boolean = false
    internal val pendRow: IntArray = IntArray(CHUNK_LEN)
    internal val pendCol: IntArray = IntArray(CHUNK_LEN)

    public var chunksLanded: Int = 0
        internal set

    /** Garbage rows waiting to be added to this well when the next chunk locks (versus mode). */
    public var incomingGarbage: Int = 0
        internal set

    /** Garbage rows this player earned that the host has not yet sent to the opponent. */
    public var outgoingGarbage: Int = 0
        internal set

    /** Height of the locked stack, 0..9. */
    public var wellHeight: Int = 0
        internal set

    /** Playable arena rows, 7..10. */
    public var arenaRows: Int = ARENA_ROWS
        internal set

    /** Microseconds left in the shrink warning, 0 when none. */
    public var shrinkWarnUs: Int = 0
        internal set
    public var score: Int = 0
        internal set
    public var rowsClearedTotal: Int = 0
        internal set
    public var bestRowClear: Int = 0
        internal set
    public var reviveUsed: Boolean = false
        internal set

    /** Microseconds of freeze left after a revive. */
    public var graceUs: Int = 0
        internal set
    public var phase: Phase = Phase.RUNNING
        internal set
    public var endCause: EndCause = EndCause.NONE
        internal set

    /** Events produced since the host last cleared the buffer. */
    public val events: EventBuffer = EventBuffer()

    /** Ring index of the snake cell [i] counted from the tail (0 = tail, snakeLen-1 = head). */
    public fun snakeCell(i: Int): Int = snake[(snakeStart + i) % snake.size]

    /** Cell index of the snake head. */
    public fun headIndex(): Int = snakeCell(snakeLen - 1)

    /** FNV-1a hash of the simulation-relevant state. Used by determinism tests only. */
    public fun stateHash(): Long {
        var h = FNV_OFFSET

        fun mix(v: Long) {
            h = (h xor v) * FNV_PRIME
        }
        arena.forEach { mix(it.toLong()) }
        well.forEach { mix(it.toLong()) }
        for (i in 0 until snakeLen) mix(snakeCell(i).toLong())
        mix(score.toLong())
        mix(rng.state)
        mix(tick)
        return h
    }

    private companion object {
        const val MAX_QUEUED_TURNS = 2
        const val FNV_OFFSET = -3750763034362895579L
        const val FNV_PRIME = 1099511628211L
    }
}
