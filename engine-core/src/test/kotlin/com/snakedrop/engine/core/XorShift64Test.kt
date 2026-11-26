package com.snakedrop.engine.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class XorShift64Test {
    @Test
    fun sameSeed_producesSameSequence() {
        val a = XorShift64(1234L)
        val b = XorShift64(1234L)
        repeat(1000) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun differentSeeds_produceDifferentSequences() {
        val a = XorShift64(1L)
        val b = XorShift64(2L)
        assertNotEquals(a.nextLong(), b.nextLong())
    }

    @Test
    fun zeroSeed_isReplacedSoGeneratorNeverSticks() {
        val r = XorShift64(0L)
        assertNotEquals(0L, r.state)
        assertNotEquals(r.nextLong(), r.nextLong())
    }

    @Test
    fun nextInt_staysWithinBound() {
        val r = XorShift64(99L)
        repeat(10_000) {
            val v = r.nextInt(7)
            assertTrue(v in 0..6)
        }
    }

    @Test
    fun nextInt_coversWholeRange() {
        val r = XorShift64(5L)
        val seen = BooleanArray(10)
        repeat(1000) { seen[r.nextInt(10)] = true }
        assertTrue(seen.all { it })
    }

    @Test
    fun nextInt_rejectsNonPositiveBound() {
        assertFailsWith<IllegalArgumentException> { XorShift64(1L).nextInt(0) }
    }
}
