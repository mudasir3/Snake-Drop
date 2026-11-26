package com.snakedrop.engine.core

/** Fixed-capacity FIFO of ints. Allocation free after construction. */
public class IntRingQueue(private val capacity: Int) {
    private val items = IntArray(capacity)
    private var head = 0

    /** Number of queued items. */
    public var size: Int = 0
        private set

    init {
        require(capacity > 0) { "capacity must be positive" }
    }

    /** Appends [value]. Returns false (and drops it) when full. */
    public fun offer(value: Int): Boolean {
        if (size == capacity) return false
        items[(head + size) % capacity] = value
        size++
        return true
    }

    /** Removes and returns the oldest item, or [EMPTY] when the queue is empty. */
    public fun poll(): Int {
        if (size == 0) return EMPTY
        val v = items[head]
        head = (head + 1) % capacity
        size--
        return v
    }

    /** Returns the newest item without removing it, or [EMPTY] when empty. */
    public fun peekLast(): Int = if (size == 0) EMPTY else items[(head + size - 1) % capacity]

    /** Removes everything. */
    public fun clear() {
        head = 0
        size = 0
    }

    public companion object {
        /** Sentinel returned when no item is available. */
        public const val EMPTY: Int = -1
    }
}
