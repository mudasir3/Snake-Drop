package com.snakedrop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SnakeRulesTest {
    @Test // FR-02
    fun newRun_startsWithLengthFourHeadingRightAndOneApple() {
        val s = T.run()
        assertEquals(4, s.snakeLen)
        assertEquals(Dir.RIGHT, s.heading)
        assertEquals(Grid.idx(4, 6), s.headIndex())
        assertTrue(s.appleIndex >= 0)
        assertEquals(Cell.APPLE, s.arena[s.appleIndex])
        assertEquals(10, s.arenaRows)
        assertNull(GameInvariants.check(s))
    }

    @Test // FR-02
    fun snake_movesOneCellEvery20TicksAtStartSpeed() {
        val s = T.run()
        T.ticks(s, 19)
        assertEquals(Grid.idx(4, 6), s.headIndex())
        T.ticks(s, 1)
        assertEquals(Grid.idx(4, 7), s.headIndex())
    }

    @Test // FR-02
    fun snake_makesSixMovesPerSecondAtStartSpeed() {
        val s = T.run()
        T.apple(s, 0, 0)
        T.ticks(s, 100)
        assertEquals(Grid.idx(4, 11), s.headIndex())
        assertEquals(Phase.RUNNING, s.phase)
    }

    @Test // FR-02
    fun wall_endsRunWhenHeadLeavesBoard() {
        val s = T.run()
        T.apple(s, 0, 0)
        T.moves(s, 6)
        assertEquals(Phase.ENDED, s.phase)
        assertEquals(EndCause.WALL, s.endCause)
    }

    @Test // FR-03
    fun turn_sameDirectionIsRejected() {
        assertFalse(SnakeDropEngine.queueTurn(T.run(), Dir.RIGHT))
    }

    @Test // FR-03
    fun turn_reversalIsRejected() {
        assertFalse(SnakeDropEngine.queueTurn(T.run(), Dir.LEFT))
    }

    @Test // FR-03
    fun turn_oppositeOfQueuedTurnIsRejected_inTheSameTick() {
        val s = T.run()
        assertTrue(SnakeDropEngine.queueTurn(s, Dir.UP))
        assertFalse(SnakeDropEngine.queueTurn(s, Dir.DOWN))
    }

    @Test // FR-03
    fun turn_queueHoldsAtMostTwo() {
        val s = T.run()
        assertTrue(SnakeDropEngine.queueTurn(s, Dir.UP))
        assertTrue(SnakeDropEngine.queueTurn(s, Dir.LEFT))
        assertFalse(SnakeDropEngine.queueTurn(s, Dir.UP))
    }

    @Test // FR-03
    fun turn_isAppliedOnNextMove() {
        val s = T.run()
        T.apple(s, 0, 0)
        SnakeDropEngine.queueTurn(s, Dir.DOWN)
        T.moves(s, 1)
        assertEquals(Grid.idx(5, 6), s.headIndex())
        assertEquals(Dir.DOWN, s.heading)
    }

    @Test // FR-02
    fun self_collisionEndsRun() {
        val s = T.snake(T.run(), Dir.RIGHT, 5 to 2, 5 to 3, 5 to 4, 5 to 5, 5 to 6)
        T.apple(s, 0, 0)
        SnakeDropEngine.queueTurn(s, Dir.DOWN)
        T.moves(s, 1)
        SnakeDropEngine.queueTurn(s, Dir.LEFT)
        T.moves(s, 1)
        SnakeDropEngine.queueTurn(s, Dir.UP)
        T.moves(s, 1)
        assertEquals(EndCause.SELF, s.endCause)
    }

    @Test // FR-02
    fun head_mayEnterCellTheTailIsLeaving() {
        val s = T.snake(T.run(), Dir.LEFT, 5 to 4, 5 to 5, 6 to 5, 6 to 4)
        T.apple(s, 0, 0)
        SnakeDropEngine.queueTurn(s, Dir.UP)
        T.moves(s, 1)
        assertEquals(Phase.RUNNING, s.phase)
        assertEquals(Grid.idx(5, 4), s.headIndex())
        assertEquals(4, s.snakeLen)
    }

    @Test // FR-04
    fun eating_growsScoresSpeedsUpAndRespawnsApple() {
        val s = T.run()
        T.apple(s, 4, 7)
        T.moves(s, 1)
        assertEquals(5, s.snakeLen)
        assertEquals(10, s.score)
        assertEquals(1, s.applesEaten)
        assertEquals(625, s.snakeSpeed100)
        assertNotEquals(Grid.idx(4, 7), s.appleIndex)
        assertEquals(Cell.APPLE, s.arena[s.appleIndex])
        assertNull(GameInvariants.check(s))
    }

    @Test // FR-04
    fun speed_isCappedAtEleven() {
        val s = T.run()
        s.applesEaten = 100
        T.apple(s, 4, 7)
        T.moves(s, 1)
        assertEquals(1100, s.snakeSpeed100)
    }

    @Test // FR-04
    fun apple_neverSpawnsOnTheSnake() {
        repeat(200) { seed ->
            val s = SnakeDropEngine.newRun(seed.toLong() + 1)
            assertEquals(Cell.APPLE, s.arena[s.appleIndex])
            assertNull(GameInvariants.check(s))
        }
    }

    @Test // FR-04
    fun apple_withNoFreeCell_endsRunWithNoSpace() {
        val s = T.run()
        for (i in 0 until s.arenaRows * GameConstants.WIDTH) if (s.arena[i] == Cell.EMPTY) s.arena[i] = Cell.BODY
        s.appleIndex = -1
        RunControl.spawnApple(s)
        assertEquals(EndCause.NO_SPACE, s.endCause)
    }

    @Test
    fun pause_stopsSimulation_andResumeContinues() {
        val s = T.run()
        SnakeDropEngine.pause(s)
        T.ticks(s, 200)
        assertEquals(0L, s.tick)
        SnakeDropEngine.resume(s)
        T.ticks(s, 20)
        assertEquals(Grid.idx(4, 7), s.headIndex())
    }

    @Test
    fun turn_isIgnoredWhenRunHasEnded() {
        val s = T.run()
        s.phase = Phase.ENDED
        assertFalse(SnakeDropEngine.queueTurn(s, Dir.UP))
    }
}
