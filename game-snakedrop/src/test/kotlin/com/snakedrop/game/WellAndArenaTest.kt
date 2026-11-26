package com.snakedrop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WellAndArenaTest {
    private fun hasEvent(
        s: GameState,
        type: Int,
    ): Boolean {
        for (i in 0 until s.events.count) if (s.events.type(i) == type) return true
        return false
    }

    private fun landVertical(
        s: GameState,
        col: Int,
    ) {
        T.freezeSnake(s)
        T.chunk(s, 0 to col, 1 to col, 2 to col, 3 to col)
        T.untilLanded(s)
    }

    private fun fillColumnZero(
        s: GameState,
        height: Int,
    ) {
        for (r in GameConstants.WELL_ROWS - height until GameConstants.WELL_ROWS) s.well[Grid.idx(r, 0)] = 1
        WellRules.refreshHeight(s)
    }

    @Test // FR-07
    fun singleRowClear_scores100PlusChunkPoints() {
        val s = T.run()
        T.freezeSnake(s)
        T.well(s, "11111111....")
        T.chunk(s, 0 to 8, 0 to 9, 0 to 10, 0 to 11)
        T.untilLanded(s)
        assertEquals(120, s.score)
        assertEquals(1, s.rowsClearedTotal)
        assertEquals(0, s.wellHeight)
        assertTrue(hasEvent(s, Ev.ROWS_CLEARED))
    }

    @Test // FR-07
    fun doubleClear_scores400_andDropsRemainingCellsAsWholeRows() {
        val s = T.run()
        T.well(s, "11111111111.", "11111111111.")
        landVertical(s, 11)
        assertEquals(420, s.score)
        assertEquals(2, s.bestRowClear)
        assertEquals(1, s.well[Grid.idx(7, 11)])
        assertEquals(1, s.well[Grid.idx(8, 11)])
        assertEquals(2, s.wellHeight)
        assertNull(GameInvariants.check(s))
    }

    @Test // FR-07
    fun quadClear_scores1600() {
        val s = T.run()
        T.well(s, "11111111111.", "11111111111.", "11111111111.", "11111111111.")
        landVertical(s, 11)
        assertEquals(1620, s.score)
        assertEquals(0, s.wellHeight)
    }

    @Test // FR-07
    fun rowGravity_movesWholeRowsNotLooseCells() {
        val s = T.run()
        T.well(s, "1.1.........", "11111111111.", "11111111111.")
        landVertical(s, 11)
        for (c in 0 until GameConstants.WIDTH) {
            val expectedBottom = if (c == 0 || c == 2 || c == 11) 1 else 0
            assertEquals(expectedBottom, s.well[Grid.idx(8, c)], "bottom col $c")
        }
        assertEquals(1, s.well[Grid.idx(7, 11)])
        assertEquals(0, s.well[Grid.idx(7, 0)])
    }

    @Test // FR-11
    fun removeBottomRows_dropsStackAndRefreshesHeight() {
        val s = T.run()
        T.well(s, "1...........", "2...........", "3...........")
        WellRules.removeBottomRows(s, 2)
        assertEquals(1, s.well[Grid.idx(8, 0)])
        assertEquals(0, s.well[Grid.idx(7, 0)])
        assertEquals(1, s.wellHeight)
    }

    @Test // FR-08
    fun arenaTarget_followsHeightFormula() {
        val s = T.run()
        val expected = mapOf(0 to 10, 6 to 10, 7 to 9, 8 to 8, 9 to 7)
        for ((h, rows) in expected) {
            s.well.fill(0)
            if (h > 0) fillColumnZero(s, h)
            assertEquals(rows, ArenaRules.targetRows(s), "height $h")
        }
    }

    @Test // FR-08
    fun shrink_warnsFor1500ms_thenRaisesFloor() {
        val s = T.run()
        T.freezeSnake(s)
        fillColumnZero(s, 7)
        ArenaRules.onWellChanged(s)
        assertEquals(1_500_000, s.shrinkWarnUs)
        assertTrue(hasEvent(s, Ev.ARENA_WARNING))
        T.ticks(s, 180)
        assertEquals(10, s.arenaRows)
        T.ticks(s, 1)
        assertEquals(9, s.arenaRows)
        assertTrue(hasEvent(s, Ev.ARENA_SHRUNK))
    }

    @Test // FR-08
    fun shrink_atHeightNine_goesStraightToMinimumSeven() {
        val s = T.run()
        T.freezeSnake(s)
        fillColumnZero(s, 9)
        ArenaRules.onWellChanged(s)
        T.ticks(s, 181)
        assertEquals(7, s.arenaRows)
    }

    @Test // FR-08
    fun shrink_crushesSnakeStandingInRemovedRow() {
        val s = T.snake(T.run(), Dir.RIGHT, 9 to 2, 9 to 3, 9 to 4, 9 to 5)
        T.apple(s, 0, 0)
        T.freezeSnake(s)
        fillColumnZero(s, 7)
        ArenaRules.onWellChanged(s)
        T.ticks(s, 181)
        assertEquals(EndCause.CRUSHED, s.endCause)
    }

    @Test // FR-08
    fun shrink_spareSnakeElsewhere_andRelocatesApple() {
        val s = T.run()
        T.apple(s, 9, 0)
        T.freezeSnake(s)
        fillColumnZero(s, 7)
        ArenaRules.onWellChanged(s)
        T.ticks(s, 181)
        assertEquals(9, s.arenaRows)
        assertEquals(Phase.RUNNING, s.phase)
        assertTrue(Grid.row(s.appleIndex) < 9)
        assertNull(GameInvariants.check(s))
    }

    @Test // FR-08
    fun arena_growsBackImmediatelyWhenStackShrinks() {
        val s = T.run()
        T.freezeSnake(s)
        fillColumnZero(s, 7)
        ArenaRules.onWellChanged(s)
        T.ticks(s, 181)
        s.well.fill(0)
        WellRules.refreshHeight(s)
        ArenaRules.onWellChanged(s)
        assertEquals(10, s.arenaRows)
        assertTrue(hasEvent(s, Ev.ARENA_GROWN))
    }

    @Test // FR-08
    fun warning_isCancelledWhenStackDropsBeforeItExpires() {
        val s = T.run()
        T.freezeSnake(s)
        fillColumnZero(s, 7)
        ArenaRules.onWellChanged(s)
        T.ticks(s, 90)
        s.well.fill(0)
        WellRules.refreshHeight(s)
        ArenaRules.onWellChanged(s)
        assertEquals(0, s.shrinkWarnUs)
        T.ticks(s, 200)
        assertEquals(10, s.arenaRows)
    }

    @Test // FR-08
    fun snake_cannotEnterRemovedRow() {
        val s = T.snake(T.run(), Dir.DOWN, 5 to 3, 6 to 3, 7 to 3, 8 to 3)
        T.apple(s, 0, 0)
        s.arenaRows = 9
        T.moves(s, 1)
        assertEquals(EndCause.WALL, s.endCause)
    }
}
