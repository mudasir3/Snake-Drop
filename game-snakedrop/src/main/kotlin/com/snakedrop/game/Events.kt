package com.snakedrop.game

/** Event type ids emitted by the engine. Consumers (audio, haptics, particles, analytics) drain them each frame. */
public object Ev {
    /** a = apples eaten so far. */
    public const val APPLE_EATEN: Int = 1

    /** Tail detached and a chunk entered the well. */
    public const val CHUNK_DETACHED: Int = 2

    /** a = cells landed. */
    public const val CHUNK_LANDED: Int = 3

    /** a = rows cleared in one lock. */
    public const val ROWS_CLEARED: Int = 4

    /** a = target arena rows. */
    public const val ARENA_WARNING: Int = 5

    /** a = new arena rows. */
    public const val ARENA_SHRUNK: Int = 6

    /** a = new arena rows. */
    public const val ARENA_GROWN: Int = 7

    /** a = [EndCause] ordinal. */
    public const val RUN_ENDED: Int = 8

    /** Run continued after a revive. */
    public const val REVIVED: Int = 9

    /** a = garbage rows added to this well. */
    public const val GARBAGE_RECEIVED: Int = 10
}

/** Preallocated event queue. Drop-on-full; the host drains it every frame. */
public class EventBuffer(private val capacity: Int = DEFAULT_CAPACITY) {
    private val types = IntArray(capacity)
    private val args = IntArray(capacity)

    /** Number of buffered events. */
    public var count: Int = 0
        private set

    /** Number of events dropped because the buffer was full (should stay 0). */
    public var dropped: Int = 0
        private set

    internal fun add(
        type: Int,
        arg: Int = 0,
    ) {
        if (count == capacity) {
            dropped++
            return
        }
        types[count] = type
        args[count] = arg
        count++
    }

    /** Type of event [i]. */
    public fun type(i: Int): Int = types[i]

    /** Argument of event [i]. */
    public fun arg(i: Int): Int = args[i]

    /** Removes all events. Call after processing. */
    public fun clear() {
        count = 0
    }

    private companion object {
        const val DEFAULT_CAPACITY = 64
    }
}
