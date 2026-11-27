@file:Suppress("MaxLineLength")

package com.snakedrop.game

import com.snakedrop.engine.core.XorShift64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersusTest {
    @Test
    fun garbageTable_isZeroOneTwoFour() {
        assertEquals(listOf(0, 0, 1, 2, 4), (0..4).map { WellRules.garbageFor(it) })
    }

    @Test
    fun clearingTwoRows_earnsOneOutgoingGarbage() {
        val s = T.run()
        T.well(s, "11111111111.", "11111111111.")
        T.freezeSnake(s)
        T.chunk(s, 0 to 11, 1 to 11, 2 to 11, 3 to 11)
        T.untilLanded(s)
        assertEquals(1, s.outgoingGarbage)
        assertEquals(1, SnakeDropEngine.takeOutgoingGarbage(s))
        assertEquals(0, s.outgoingGarbage)
    }

    @Test
    fun incomingGarbage_isAppliedWhenChunkLocks_withOneSharedGap() {
        val s = T.run()
        SnakeDropEngine.receiveGarbage(s, 2)
        assertEquals(0, s.wellHeight)
        T.freezeSnake(s)
        T.chunk(s, 0 to 0, 0 to 1, 0 to 2, 0 to 3)
        T.untilLanded(s)
        assertEquals(0, s.incomingGarbage)
        assertEquals(3, s.wellHeight)
        val gaps = (0 until 12).filter { s.well[Grid.idx(8, it)] == 0 }
        assertEquals(1, gaps.size)
        assertEquals(gaps, (0 until 12).filter { s.well[Grid.idx(7, it)] == 0 })
        assertEquals(GameConstants.GARBAGE_COLOR, s.well[Grid.idx(8, (gaps[0] + 1) % 12)])
        assertNull(GameInvariants.check(s))
    }

    @Test
    fun outgoingGarbage_cancelsIncomingFirst() {
        val s = T.run()
        SnakeDropEngine.receiveGarbage(s, 2)
        T.well(s, "11111111111.", "11111111111.", "11111111111.")
        T.freezeSnake(s)
        T.chunk(s, 0 to 11, 1 to 11, 2 to 11, 3 to 11)
        T.untilLanded(s)
        assertEquals(0, s.incomingGarbage)
        assertEquals(0, s.outgoingGarbage)
        assertEquals(1, s.wellHeight)
    }

    @Test
    fun garbageOverflow_endsTheRun() {
        val s = T.run()
        T.well(s, "1...........", "1...........", "1...........", "1...........", "1...........", "1...........", "1...........")
        SnakeDropEngine.receiveGarbage(s, 3)
        T.freezeSnake(s)
        T.chunk(s, 0 to 5, 0 to 6, 0 to 7, 0 to 8)
        T.untilLanded(s)
        assertEquals(Phase.ENDED, s.phase)
    }

    @Test
    fun receiveGarbage_isIgnoredAfterRunEnds_andCapped() {
        val s = T.run()
        SnakeDropEngine.receiveGarbage(s, 100)
        assertEquals(8, s.incomingGarbage)
        s.phase = Phase.ENDED
        SnakeDropEngine.receiveGarbage(s, 1)
        assertEquals(8, s.incomingGarbage)
    }

    private class Loopback : MatchTransport {
        val sent = mutableListOf<MatchMsg>()
        val inbox = ArrayDeque<MatchMsg>()

        override fun tick() = Unit

        override fun send(msg: MatchMsg) {
            sent += msg
        }

        override fun poll(): MatchMsg? = inbox.removeFirstOrNull()
    }

    @Test
    fun controller_losesWhenOwnRunEnds_andReportsIt() {
        val t = Loopback()
        val m = MatchController(RunSetup(1).create(), t)
        T.apple(m.state, 0, 0)
        var n = 0
        while (m.result == MatchResult.ONGOING && n++ < 5_000) m.step()
        assertEquals(MatchResult.LOST, m.result)
        assertTrue(t.sent.any { it is MatchMsg.Ended })
    }

    @Test
    fun controller_winsWhenOpponentEnds() {
        val t = Loopback()
        val m = MatchController(RunSetup(1).create(), t)
        t.inbox += MatchMsg.Progress(120, 3)
        m.step()
        assertEquals(120, m.opponentScore)
        t.inbox += MatchMsg.Ended(EndCause.WALL)
        m.step()
        assertEquals(MatchResult.WON, m.result)
    }

    @Test
    fun controller_appliesReceivedGarbageToState() {
        val t = Loopback()
        val m = MatchController(RunSetup(1).create(), t)
        t.inbox += MatchMsg.Garbage(2)
        m.step()
        assertEquals(2, m.state.incomingGarbage)
    }

    @Test
    fun botMatches_alwaysFinish_withAWinner_andValidStates() {
        for (seed in 1L..40L) {
            val bot = BotOpponent(seed, Difficulty.MEDIUM)
            val m = MatchController(RunSetup(seed, Difficulty.MEDIUM).create(), bot)
            val rng = XorShift64(seed)
            var t = 0
            while (m.result == MatchResult.ONGOING && t++ < 200_000) {
                if (t % 7 == 0) BotPolicy.decide(m.state, rng, 12) { SnakeDropEngine.queueTurn(m.state, it) }
                m.step()
                m.state.events.clear()
                if (t % 16 == 0) {
                    assertNull(GameInvariants.check(m.state), "player seed $seed tick $t")
                    assertNull(GameInvariants.check(bot.state), "bot seed $seed tick $t")
                }
            }
            assertTrue(m.result != MatchResult.ONGOING, "seed $seed never finished")
        }
    }

    @Test
    fun botMatch_isDeterministic() {
        fun play(): Triple<MatchResult, Int, Long> {
            val bot = BotOpponent(9, Difficulty.HARD)
            val m = MatchController(RunSetup(9, Difficulty.HARD).create(), bot)
            val rng = XorShift64(9)
            var t = 0
            while (m.result == MatchResult.ONGOING && t++ < 200_000) {
                if (t % 7 == 0) BotPolicy.decide(m.state, rng, 12) { SnakeDropEngine.queueTurn(m.state, it) }
                m.step()
                m.state.events.clear()
            }
            return Triple(m.result, m.state.score, m.state.stateHash())
        }
        assertEquals(play(), play())
    }
}
