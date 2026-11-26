package com.snakedrop.game

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChunkRulesTest {
    private fun hasEvent(
        s: GameState,
        type: Int,
    ): Boolean {
        for (i in 0 until s.events.count) if (s.events.type(i) == type) return true
        return false
    }

    private fun straightSevenWithApple(): GameState {
        val s = T.snake(T.run(), Dir.RIGHT, 5 to 2, 5 to 3, 5 to 4, 5 to 5, 5 to 6, 5 to 7, 5 to 8)
        T.apple(s, 5, 9)
        return s
    }

    @Test // FR-05
    fun fourthApple_detachesLastFourSegments_snakeLengthIsFour() {
        val s = straightSevenWithApple()
        T.moves(s, 1)
        assertEquals(4, s.snakeLen)
        assertEquals(0, s.applesSinceDetach)
        assertTrue(s.chunkActive)
        assertContentEquals(intArrayOf(2, 3, 4, 5), s.chunkCol)
        assertContentEquals(intArrayOf(0, 0, 0, 0), s.chunkRow)
        assertEquals(Grid.idx(5, 9), s.headIndex())
        for (c in 2..5) assertEquals(Cell.EMPTY, s.arena[Grid.idx(5, c)])
        assertTrue(hasEvent(s, Ev.CHUNK_DETACHED))
        assertNull(GameInvariants.check(s))
    }

    @Test // FR-05
    fun detach_preservesBentShapeAndColumns() {
        val s = T.snake(T.run(), Dir.RIGHT, 4 to 2, 4 to 3, 5 to 3, 5 to 4, 5 to 5, 5 to 6, 5 to 7)
        T.apple(s, 5, 8)
        T.moves(s, 1)
        assertContentEquals(intArrayOf(0, 0, 1, 1), s.chunkRow)
        assertContentEquals(intArrayOf(2, 3, 3, 4), s.chunkCol)
        assertNull(GameInvariants.check(s))
    }

    @Test // FR-05
    fun detach_assignsColorFromLandedCount() {
        val s = straightSevenWithApple()
        s.chunksLanded = 7
        T.moves(s, 1)
        assertEquals(2, s.chunkColor)
    }

    @Test // FR-06
    fun chunk_fallsOneRowAfter150ms() {
        val s = T.run()
        T.freezeSnake(s)
        T.chunk(s, 0 to 2, 0 to 3, 0 to 4, 0 to 5)
        T.ticks(s, 18)
        assertEquals(0, s.chunkTop)
        T.ticks(s, 1)
        assertEquals(1, s.chunkTop)
    }

    @Test // FR-06
    fun chunk_fallsFasterAfterTwentyApples() {
        val s = T.run()
        T.freezeSnake(s)
        s.applesEaten = 20
        T.chunk(s, 0 to 2, 0 to 3, 0 to 4, 0 to 5)
        T.ticks(s, 14)
        assertEquals(1, s.chunkTop)
    }

    @Test // FR-06
    fun chunk_locksOnFloorAndAwardsFivePerCell() {
        val s = T.run()
        T.freezeSnake(s)
        T.chunk(s, 0 to 2, 0 to 3, 0 to 4, 0 to 5)
        T.untilLanded(s)
        for (c in 2..5) assertEquals(1, s.well[Grid.idx(8, c)])
        assertEquals(1, s.chunksLanded)
        assertEquals(20, s.score)
        assertEquals(1, s.wellHeight)
        assertFalse(s.chunkActive)
    }

    @Test // FR-06
    fun chunk_locksOnTopOfStack() {
        val s = T.run()
        T.freezeSnake(s)
        T.well(s, "..1111......")
        T.chunk(s, 0 to 2, 0 to 3, 0 to 4, 0 to 5)
        s.chunkColor = 2
        T.untilLanded(s)
        for (c in 2..5) assertEquals(2, s.well[Grid.idx(7, c)])
        assertEquals(2, s.wellHeight)
    }

    @Test // FR-06
    fun chunk_fallsAsOneRigidBody() {
        val s = T.run()
        T.freezeSnake(s)
        T.well(s, "1...........")
        T.chunk(s, 0 to 0, 1 to 0, 1 to 1, 2 to 1)
        s.chunkColor = 2
        T.untilLanded(s)
        assertEquals(2, s.well[Grid.idx(6, 0)])
        assertEquals(2, s.well[Grid.idx(7, 0)])
        assertEquals(2, s.well[Grid.idx(7, 1)])
        assertEquals(2, s.well[Grid.idx(8, 1)])
        assertEquals(1, s.well[Grid.idx(8, 0)])
    }

    @Test // FR-06
    fun secondDetach_whileChunkFalls_queuesAndSpawnsAfterLanding() {
        val s = straightSevenWithApple()
        T.freezeSnake(s)
        T.chunk(s, 0 to 0, 0 to 1, 0 to 2, 0 to 3)
        s.moveAccum = 0
        T.moves(s, 1)
        assertTrue(s.hasPending)
        assertContentEquals(intArrayOf(0, 1, 2, 3), s.chunkCol)
        T.freezeSnake(s)
        while (s.chunksLanded < 1) SnakeDropEngine.step(s)
        assertTrue(s.chunkActive)
        assertContentEquals(intArrayOf(2, 3, 4, 5), s.chunkCol)
        assertFalse(s.hasPending)
    }

    @Test // FR-09
    fun spawnBlockedByStack_endsRunWithNoSpawn() {
        val s = straightSevenWithApple()
        s.well[Grid.idx(0, 2)] = 1
        WellRules.refreshHeight(s)
        T.moves(s, 1)
        assertEquals(EndCause.NO_SPAWN, s.endCause)
    }
}
