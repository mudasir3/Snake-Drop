@file:Suppress("ReturnCount", "MaxLineLength")

package com.snakedrop.game

import java.nio.ByteBuffer

/** A complete, replayable run: the setup plus every accepted input. Small enough to upload (about 5 bytes per turn). */
public class Replay(
    public val setup: RunSetup,
    internal val turnTicks: IntArray,
    internal val turnDirs: ByteArray,
    public val reviveTick: Long,
    public val endTick: Long,
    public val finalScore: Int,
) {
    /** Number of recorded turns. */
    public val turnCount: Int get() = turnTicks.size

    /** Serializes to bytes (format version 1). */
    public fun encode(): ByteArray {
        val b = ByteBuffer.allocate(HEADER + turnTicks.size * TURN_BYTES)
        b.put(VERSION)
        b.putLong(setup.seed)
        b.put(setup.difficulty.ordinal.toByte())
        b.putShort(setup.levelId.toShort())
        b.putLong(reviveTick)
        b.putLong(endTick)
        b.putInt(finalScore)
        b.putInt(turnTicks.size)
        for (i in turnTicks.indices) {
            b.putInt(turnTicks[i])
            b.put(turnDirs[i])
        }
        return b.array()
    }

    public companion object {
        private const val VERSION: Byte = 1
        private const val HEADER = 1 + 8 + 1 + 2 + 8 + 8 + 4 + 4
        private const val TURN_BYTES = 5

        /** Parses bytes from [encode]. Returns null for anything malformed. */
        public fun decode(bytes: ByteArray): Replay? {
            if (bytes.size < HEADER) return null
            val b = ByteBuffer.wrap(bytes)
            if (b.get() != VERSION) return null
            val seed = b.getLong()
            val diff = Difficulty.entries.getOrNull(b.get().toInt()) ?: return null
            val level = b.getShort().toInt()
            val revive = b.getLong()
            val end = b.getLong()
            val score = b.getInt()
            val n = b.getInt()
            if (n < 0 || bytes.size != HEADER + n * TURN_BYTES) return null
            val ticks = IntArray(n)
            val dirs = ByteArray(n)
            for (i in 0 until n) {
                ticks[i] = b.getInt()
                dirs[i] = b.get()
                if (dirs[i] !in 0..3 || ticks[i] < 0) return null
            }
            return Replay(RunSetup(seed, diff, level), ticks, dirs, revive, end, score)
        }
    }
}

/** A live run that records itself. The app drives this instead of the raw engine. */
public class RecordedRun(public val setup: RunSetup) {
    public val state: GameState = setup.create()
    private var ticks = IntArray(INITIAL)
    private var dirs = ByteArray(INITIAL)
    private var count = 0
    private var reviveTick = -1L

    /** Queues a turn and records it when accepted. */
    public fun queueTurn(dir: Int): Boolean {
        if (!SnakeDropEngine.queueTurn(state, dir)) return false
        if (count == ticks.size) {
            ticks = ticks.copyOf(count * 2)
            dirs = dirs.copyOf(count * 2)
        }
        ticks[count] = state.tick.toInt()
        dirs[count] = dir.toByte()
        count++
        return true
    }

    /** Advances one tick. */
    public fun step(): Unit = SnakeDropEngine.step(state)

    /** Revives the run and records when it happened. */
    public fun revive(): Boolean {
        val at = state.tick
        if (!SnakeDropEngine.revive(state)) return false
        reviveTick = at
        return true
    }

    /** Snapshot of the run so far as a [Replay]. */
    public fun finish(): Replay = Replay(setup, ticks.copyOf(count), dirs.copyOf(count), reviveTick, state.tick, state.score)

    private companion object {
        const val INITIAL = 64
    }
}

/** Re-simulates replays. Deterministic: the same replay always yields the same state. */
public object ReplayPlayer {
    /** Runs [replay] to its end tick and returns the final state. */
    public fun play(replay: Replay): GameState {
        val s = replay.setup.create()
        var i = 0
        var revived = false
        while (true) {
            if (!revived && replay.reviveTick == s.tick && s.phase == Phase.ENDED) {
                SnakeDropEngine.revive(s)
                revived = true
            }
            if (s.phase != Phase.RUNNING || s.tick >= replay.endTick) break
            while (i < replay.turnCount && replay.turnTicks[i].toLong() == s.tick) {
                SnakeDropEngine.queueTurn(s, replay.turnDirs[i].toInt())
                i++
            }
            SnakeDropEngine.step(s)
            s.events.clear()
        }
        return s
    }

    /** True when re-simulating gives the score the replay claims. Use server-side to reject forged scores. */
    public fun verify(replay: Replay): Boolean = play(replay).score == replay.finalScore
}
