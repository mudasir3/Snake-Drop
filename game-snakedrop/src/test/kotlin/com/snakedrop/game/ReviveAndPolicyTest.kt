package com.snakedrop.game

import com.snakedrop.engine.core.XorShift64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReviveAndPolicyTest {
    private fun hitWall(s: GameState) {
        T.apple(s, 0, 0)
        T.moves(s, 6)
        assertEquals(EndCause.WALL, s.endCause)
    }

    @Test // FR-11
    fun revive_clearsBottomTwoRows_keepsScore_andFreezesBriefly() {
        val s = T.run()
        T.well(s, "1...........", "2...........", "3...........")
        s.score = 50
        hitWall(s)
        assertTrue(SnakeDropEngine.revive(s))
        assertEquals(Phase.RUNNING, s.phase)
        assertEquals(50, s.score)
        assertEquals(1, s.wellHeight)
        assertEquals(1, s.well[Grid.idx(8, 0)])
        val head = s.headIndex()
        T.ticks(s, 179)
        assertEquals(head, s.headIndex())
        assertNull(GameInvariants.check(s))
    }

    @Test // FR-11
    fun revive_afterWall_turnsSnakeToSafeHeading() {
        val s = T.run()
        hitWall(s)
        SnakeDropEngine.revive(s)
        assertNotEquals(Dir.RIGHT, s.heading)
        T.ticks(s, 181 + 20)
        assertEquals(Phase.RUNNING, s.phase)
    }

    @Test // FR-11
    fun revive_isAllowedOncePerRun() {
        val s = T.run()
        hitWall(s)
        assertTrue(SnakeDropEngine.revive(s))
        s.phase = Phase.ENDED
        s.endCause = EndCause.WALL
        assertFalse(SnakeDropEngine.canRevive(s))
        assertFalse(SnakeDropEngine.revive(s))
    }

    @Test // FR-11
    fun revive_isNotOfferedWhileRunningOrOnNoSpace() {
        val s = T.run()
        assertFalse(SnakeDropEngine.canRevive(s))
        s.phase = Phase.ENDED
        s.endCause = EndCause.NO_SPACE
        assertFalse(SnakeDropEngine.canRevive(s))
    }

    @Test // FR-11
    fun revive_afterCrush_keepsSnakeInsideArena() {
        val s = T.snake(T.run(), Dir.RIGHT, 9 to 2, 9 to 3, 9 to 4, 9 to 5)
        T.apple(s, 0, 0)
        T.freezeSnake(s)
        for (r in 2..8) s.well[Grid.idx(r, 0)] = 1
        WellRules.refreshHeight(s)
        ArenaRules.onWellChanged(s)
        T.ticks(s, 181)
        assertEquals(EndCause.CRUSHED, s.endCause)
        assertTrue(SnakeDropEngine.revive(s))
        assertNull(GameInvariants.check(s))
    }

    @Test
    fun determinism_sameSeedAndInputs_giveSameStateHash() {
        fun play(seed: Long): Long {
            val s = SnakeDropEngine.newRun(seed)
            val r = XorShift64(77)
            repeat(40_000) { t ->
                if (t % 17 == 0) SnakeDropEngine.queueTurn(s, r.nextInt(4))
                SnakeDropEngine.step(s)
                s.events.clear()
            }
            return s.stateHash()
        }
        assertEquals(play(42), play(42))
        assertNotEquals(play(42), play(43))
    }

    @Test
    fun randomRuns_neverBreakInvariants() {
        repeat(200) { seed ->
            val s = SnakeDropEngine.newRun(seed + 1L)
            val r = XorShift64(seed + 1000L)
            var t = 0
            while (s.phase == Phase.RUNNING && t++ < 20_000) {
                if (t % 11 == 0) SnakeDropEngine.queueTurn(s, r.nextInt(4))
                SnakeDropEngine.step(s)
                s.events.clear()
                assertNull(GameInvariants.check(s), "seed $seed tick $t")
            }
        }
    }

    @Test // FR-12
    fun interstitial_neverInFirstThreeRuns_thenEveryThirdAfterThat() {
        val p = AdPolicy()
        val shown = (1..9).map { p.onGameOver(it * 100_000L) }
        assertEquals(listOf(false, false, false, false, false, true, false, false, true), shown)
    }

    @Test // FR-12
    fun interstitial_respectsNinetySecondGap() {
        val p = AdPolicy(totalGameOvers = 10, gameOversSinceInterstitial = 2, lastInterstitialMs = 1_000_000L)
        assertFalse(p.onGameOver(1_030_000L))
        assertTrue(p.onGameOver(1_090_000L))
    }

    @Test // FR-13
    fun removeAds_stopsInterstitials() {
        val p = AdPolicy(totalGameOvers = 10, gameOversSinceInterstitial = 5)
        p.adsRemoved = true
        assertFalse(p.onGameOver(5_000_000L))
    }

    @Test
    fun killSwitch_disablesInterstitialsAndRevive() {
        val cfg = GameConfig(adsEnabled = false)
        val p = AdPolicy(cfg, totalGameOvers = 10, gameOversSinceInterstitial = 5)
        assertFalse(p.onGameOver(5_000_000L))
        val s = T.run(cfg)
        s.phase = Phase.ENDED
        s.endCause = EndCause.WALL
        assertFalse(p.canOfferRevive(s, rewardedReady = true))
    }

    @Test // FR-11
    fun revive_offerNeedsReadyRewardedAd() {
        val s = T.run()
        s.phase = Phase.ENDED
        s.endCause = EndCause.WALL
        val p = AdPolicy()
        assertTrue(p.canOfferRevive(s, rewardedReady = true))
        assertFalse(p.canOfferRevive(s, rewardedReady = false))
    }

    @Test // FR-11 regression: fuzzer seed 97
    fun revive_afterNoSpawn_respawnsMissingApple() {
        val s = T.run()
        s.phase = Phase.ENDED
        s.endCause = EndCause.NO_SPAWN
        s.arena[s.appleIndex] = Cell.EMPTY
        s.appleIndex = -1
        assertTrue(SnakeDropEngine.revive(s))
        assertTrue(s.appleIndex >= 0)
        assertNull(GameInvariants.check(s))
    }
}
