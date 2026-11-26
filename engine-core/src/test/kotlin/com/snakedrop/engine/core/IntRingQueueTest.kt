package com.snakedrop.engine.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntRingQueueTest {
    @Test
    fun fifoOrder_isPreserved() {
        val q = IntRingQueue(3)
        q.offer(1)
        q.offer(2)
        assertEquals(1, q.poll())
        assertEquals(2, q.poll())
    }

    @Test
    fun offer_whenFull_returnsFalseAndKeepsContents() {
        val q = IntRingQueue(2)
        assertTrue(q.offer(1))
        assertTrue(q.offer(2))
        assertFalse(q.offer(3))
        assertEquals(1, q.poll())
        assertEquals(2, q.poll())
    }

    @Test
    fun poll_whenEmpty_returnsSentinel() {
        assertEquals(IntRingQueue.EMPTY, IntRingQueue(2).poll())
    }

    @Test
    fun wrapAround_worksAcrossManyCycles() {
        val q = IntRingQueue(2)
        repeat(100) { i ->
            q.offer(i)
            assertEquals(i, q.poll())
        }
        assertEquals(0, q.size)
    }

    @Test
    fun peekLast_returnsNewest() {
        val q = IntRingQueue(3)
        assertEquals(IntRingQueue.EMPTY, q.peekLast())
        q.offer(5)
        q.offer(6)
        assertEquals(6, q.peekLast())
    }

    @Test
    fun clear_emptiesQueue() {
        val q = IntRingQueue(2)
        q.offer(1)
        q.clear()
        assertEquals(0, q.size)
        assertEquals(IntRingQueue.EMPTY, q.poll())
    }
}
