package com.snakedrop.game

import com.snakedrop.engine.core.XorShift64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModesTest {
    @Test
    fun difficulty_ordersSpeedAndShrinkSensibly() {
        val s = Difficulty.SMALL.config
        val m = Difficulty.MEDIUM.config
        val h = Difficulty.HARD.config
        assertTrue(s.snakeSpeedStart100 < m.snakeSpeedStart100 && m.snakeSpeedStart100 < h.snakeSpeedStart100)
        assertTrue(s.chunkFallMs > m.chunkFallMs && m.chunkFallMs > h.chunkFallMs)
        assertTrue(s.arenaShrinkThreshold > m.arenaShrinkThreshold && m.arenaShrinkThreshold > h.arenaShrinkThreshold)
        assertEquals(GameConfig(), m)
    }

    @Test
    fun hardStartsWithTwoRubbleRows_neverFull_andValid() {
        for (seed in 1L..50L) {
            val s = RunSetup(seed, Difficulty.HARD).create()
            assertEquals(2, s.wellHeight)
            for (r in 7..8) assertTrue((0 until 12).count { s.well[Grid.idx(r, it)] == 0 } >= 1)
            assertNull(GameInvariants.check(s), "seed $seed")
        }
    }

    @Test
    fun smallAndMedium_startWithEmptyWell() {
        assertEquals(0, RunSetup(1, Difficulty.SMALL).create().wellHeight)
        assertEquals(0, RunSetup(1, Difficulty.MEDIUM).create().wellHeight)
    }

    @Test
    fun levels_haveUniqueIds_andGrowInDifficulty() {
        assertEquals(Levels.all.size, Levels.all.map { it.id }.toSet().size)
        assertEquals(Levels.all.map { it.difficulty.ordinal }, Levels.all.map { it.difficulty.ordinal }.sorted())
        assertNotNull(Levels.byId(12))
        assertNull(Levels.byId(99))
    }

    @Test
    fun level_usesItsRubbleAndDifficultyConfig() {
        val s = RunSetup(5, Difficulty.HARD, levelId = 11).create()
        assertEquals(3, s.wellHeight)
        assertEquals(Difficulty.HARD.config.snakeSpeedStart100, s.config.snakeSpeedStart100)
    }

    @Test
    fun levelGoal_isCompleteWhenTargetReached() {
        val level = Levels.byId(1)!!
        val s = RunSetup(1, levelId = 1).create()
        assertFalse(Levels.isComplete(s, level))
        s.applesEaten = 8
        assertTrue(Levels.isComplete(s, level))
        SnakeDropEngine.win(s)
        assertEquals(Phase.WON, s.phase)
    }

    @Test
    fun stars_followParTime() {
        val level = Levels.byId(1)!!
        val s = RunSetup(1, levelId = 1).create()
        s.tick = 30L * 120
        assertEquals(3, Levels.stars(s, level))
        s.tick = 80L * 120
        assertEquals(2, Levels.stars(s, level))
        s.tick = 200L * 120
        assertEquals(1, Levels.stars(s, level))
    }

    @Test
    fun nonRowLevels_areWinnableByTheBot_atLeastSometimes() {
        for (level in Levels.all.filter { it.goalType != GoalType.ROWS }) {
            var won = false
            for (seed in 1L..150L) {
                val run = RecordedRun(RunSetup(seed, level.difficulty, level.id))
                val rng = XorShift64(seed)
                var t = 0
                while (run.state.phase == Phase.RUNNING && t++ < 60_000) {
                    if (t % 7 == 0) BotPolicy.decide(run.state, rng, 0) { run.queueTurn(it) }
                    run.step()
                    if (Levels.isComplete(run.state, level)) {
                        SnakeDropEngine.win(run.state)
                        won = true
                    }
                }
                if (won) break
            }
            assertTrue(won, "level ${level.id} never completed by the bot")
        }
    }

    @Test
    fun rubbleRow_hasOneFourWideGap_andStraightChunkClearsIt() {
        val s = RunSetup(11, Difficulty.SMALL, levelId = 3).create()
        val gap = (0 until 12).first { s.well[Grid.idx(8, it)] == 0 }
        assertEquals(4, (0 until 12).count { s.well[Grid.idx(8, it)] == 0 })
        T.freezeSnake(s)
        T.chunk(s, 0 to gap, 0 to gap + 1, 0 to gap + 2, 0 to gap + 3)
        T.untilLanded(s)
        assertEquals(1, s.rowsClearedTotal)
        assertTrue(Levels.isComplete(s, Levels.byId(3)!!))
    }

    @Test
    fun dailySeed_isStablePerDay_andDiffersAcrossDays() {
        assertEquals(DailySeed.forEpochDay(20_000), DailySeed.forEpochDay(20_000))
        assertNotEquals(DailySeed.forEpochDay(20_000), DailySeed.forEpochDay(20_001))
    }

    private fun playRecorded(setup: RunSetup): RecordedRun {
        val run = RecordedRun(setup)
        val rng = XorShift64(setup.seed + 9)
        var t = 0
        while (run.state.phase == Phase.RUNNING && t++ < 30_000) {
            if (t % 7 == 0) BotPolicy.decide(run.state, rng, 9) { run.queueTurn(it) }
            run.step()
            run.state.events.clear()
        }
        return run
    }

    @Test
    fun replay_reproducesScoreAndStateExactly() {
        for (seed in 1L..20L) {
            val run = playRecorded(RunSetup(seed, Difficulty.entries[(seed % 3).toInt()]))
            val replay = run.finish()
            val replayed = ReplayPlayer.play(replay)
            assertEquals(run.state.score, replayed.score, "seed $seed")
            assertEquals(run.state.stateHash(), replayed.stateHash(), "seed $seed")
            assertTrue(ReplayPlayer.verify(replay))
        }
    }

    @Test
    fun replay_withRevive_stillMatches() {
        val run = RecordedRun(RunSetup(3))
        val rng = XorShift64(5)
        var t = 0
        while (run.state.phase == Phase.RUNNING && t++ < 30_000) {
            if (t % 7 == 0) BotPolicy.decide(run.state, rng, 9) { run.queueTurn(it) }
            run.step()
            run.state.events.clear()
        }
        if (run.revive()) {
            repeat(2_000) {
                if (it % 7 == 0) BotPolicy.decide(run.state, rng, 9) { d -> run.queueTurn(d) }
                run.step()
                run.state.events.clear()
            }
        }
        val replay = run.finish()
        assertEquals(run.state.stateHash(), ReplayPlayer.play(replay).stateHash())
    }

    @Test
    fun replay_encodeDecodeRoundTrips() {
        val replay = playRecorded(RunSetup(7, Difficulty.HARD)).finish()
        val back = Replay.decode(replay.encode())
        assertNotNull(back)
        assertEquals(replay.setup, back.setup)
        assertEquals(replay.finalScore, back.finalScore)
        assertContentEquals(replay.encode(), back.encode())
        assertTrue(ReplayPlayer.verify(back))
    }

    @Test
    fun replay_decodeRejectsGarbage() {
        assertNull(Replay.decode(ByteArray(0)))
        assertNull(Replay.decode(ByteArray(40) { 9 }))
        val good = playRecorded(RunSetup(2)).finish().encode()
        assertNull(Replay.decode(good.copyOf(good.size - 1)))
    }

    @Test
    fun replay_forgedScoreFailsVerification() {
        val r = playRecorded(RunSetup(4)).finish()
        val forged = Replay(r.setup, r.turnTicks, r.turnDirs, r.reviveTick, r.endTick, r.finalScore + 1000)
        assertFalse(ReplayPlayer.verify(forged))
    }
}
