package com.snakedrop.engine.core

/**
 * Deterministic xorshift64* generator. The only randomness source allowed inside simulation code.
 * Same seed always yields the same sequence on every device.
 */
public class XorShift64(seed: Long) {
    /** Current internal state. Exposed so it can be hashed in determinism tests. */
    public var state: Long = if (seed == 0L) FALLBACK_SEED else seed
        private set

    /** Returns the next 64 random bits. */
    public fun nextLong(): Long {
        var x = state
        x = x xor (x ushr SHIFT_A)
        x = x xor (x shl SHIFT_B)
        x = x xor (x ushr SHIFT_C)
        state = x
        return x * MULTIPLIER
    }

    /** Returns a value in `[0, bound)`. [bound] must be positive. */
    public fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    private companion object {
        const val FALLBACK_SEED = -7046029254386353131L // 0x9E3779B97F4A7C15
        const val SHIFT_A = 12
        const val SHIFT_B = 25
        const val SHIFT_C = 27
        const val MULTIPLIER = 0x2545F4914F6CDD1DL
    }
}
