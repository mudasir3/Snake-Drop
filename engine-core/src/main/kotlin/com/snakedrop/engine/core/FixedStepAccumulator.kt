package com.snakedrop.engine.core

/**
 * Converts variable frame deltas into a whole number of fixed simulation steps.
 * Usage per frame: `repeat(acc.consume(dtNanos)) { engine.step(state) }`.
 * Allocation free.
 */
public class FixedStepAccumulator(
    private val stepNanos: Long,
    private val maxFrameNanos: Long = DEFAULT_MAX_FRAME_NANOS,
) {
    private var pendingNanos: Long = 0L

    init {
        require(stepNanos > 0) { "stepNanos must be positive" }
        require(maxFrameNanos >= stepNanos) { "maxFrameNanos must cover at least one step" }
    }

    /** Adds [dtNanos] (clamped to the max frame time) and returns how many steps to run now. */
    public fun consume(dtNanos: Long): Int {
        val dt = dtNanos.coerceIn(0L, maxFrameNanos)
        pendingNanos += dt
        val steps = (pendingNanos / stepNanos).toInt()
        pendingNanos -= steps * stepNanos
        return steps
    }

    /** Fraction of the next step already accumulated, in `[0, 1)`. Use for render interpolation. */
    public fun alpha(): Float = pendingNanos.toFloat() / stepNanos

    /** Drops any accumulated time (call on resume from pause). */
    public fun reset() {
        pendingNanos = 0L
    }

    public companion object {
        /** Frame deltas above 250 ms are clamped so pauses and spikes cannot cause a spiral of death. */
        public const val DEFAULT_MAX_FRAME_NANOS: Long = 250_000_000L
    }
}
