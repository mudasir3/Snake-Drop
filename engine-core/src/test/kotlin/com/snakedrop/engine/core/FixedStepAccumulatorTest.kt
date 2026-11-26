package com.snakedrop.engine.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FixedStepAccumulatorTest {
    private val step = 8_000_000L

    @Test
    fun smallDelta_runsNoStepButKeepsRemainder() {
        val acc = FixedStepAccumulator(step)
        assertEquals(0, acc.consume(5_000_000L))
        assertEquals(1, acc.consume(5_000_000L))
    }

    @Test
    fun largeDelta_runsManySteps() {
        val acc = FixedStepAccumulator(step)
        assertEquals(10, acc.consume(80_000_000L))
    }

    @Test
    fun hugeDelta_isClampedToMaxFrame() {
        val acc = FixedStepAccumulator(step, maxFrameNanos = 250_000_000L)
        assertEquals(31, acc.consume(10_000_000_000L))
    }

    @Test
    fun negativeDelta_isTreatedAsZero() {
        val acc = FixedStepAccumulator(step)
        assertEquals(0, acc.consume(-5L))
    }

    @Test
    fun totalSteps_matchTotalTimeOverManyFrames() {
        val acc = FixedStepAccumulator(step)
        var total = 0
        repeat(1000) { total += acc.consume(16_666_667L) }
        assertTrue(total in 2082..2084, "steps=$total")
    }

    @Test
    fun alpha_isFractionOfStep() {
        val acc = FixedStepAccumulator(step)
        acc.consume(2_000_000L)
        assertEquals(0.25f, acc.alpha(), 0.0001f)
    }

    @Test
    fun reset_dropsAccumulatedTime() {
        val acc = FixedStepAccumulator(step)
        acc.consume(7_000_000L)
        acc.reset()
        assertEquals(0, acc.consume(1_000_000L))
    }

    @Test
    fun constructor_rejectsBadArguments() {
        assertFailsWith<IllegalArgumentException> { FixedStepAccumulator(0L) }
        assertFailsWith<IllegalArgumentException> { FixedStepAccumulator(10L, maxFrameNanos = 5L) }
    }
}
