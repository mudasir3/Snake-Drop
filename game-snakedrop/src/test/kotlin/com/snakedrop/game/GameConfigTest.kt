package com.snakedrop.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameConfigTest {
    @Test // FR-19
    fun emptyRemote_givesPrdDefaults() {
        assertEquals(GameConfig(), GameConfig.fromRemote(emptyMap()))
    }

    @Test // FR-19
    fun malformedValues_fallBackToDefaults() {
        val raw = mapOf("snake_speed_start" to "fast", "chunk_fall_ms" to "", "arena_min_rows" to "NaN")
        assertEquals(GameConfig(), GameConfig.fromRemote(raw))
    }

    @Test // FR-19
    fun nanAndInfinity_fallBackToDefaults() {
        val raw = mapOf("snake_speed_start" to "NaN", "snake_speed_max" to "Infinity")
        assertEquals(GameConfig(), GameConfig.fromRemote(raw))
    }

    @Test // FR-19
    fun outOfRangeValues_areClamped() {
        val c =
            GameConfig.fromRemote(
                mapOf(
                    "snake_speed_start" to "99",
                    "snake_speed_step" to "-3",
                    "chunk_fall_ms" to "5",
                    "arena_shrink_threshold" to "100",
                    "interstitial_every_n" to "1",
                    "interstitial_min_gap_s" to "5",
                    "revive_rows_cleared" to "50",
                ),
            )
        assertEquals(800, c.snakeSpeedStart100)
        assertEquals(0, c.snakeSpeedStep100)
        assertEquals(80, c.chunkFallMs)
        assertEquals(8, c.arenaShrinkThreshold)
        assertEquals(2, c.interstitialEveryN)
        assertEquals(60, c.interstitialMinGapS)
        assertEquals(4, c.reviveRowsCleared)
    }

    @Test // FR-19
    fun fastFall_neverSlowerThanNormalFall() {
        val c = GameConfig.fromRemote(mapOf("chunk_fall_ms" to "100", "chunk_fall_ms_fast" to "200"))
        assertTrue(c.chunkFallMsFast <= c.chunkFallMs)
    }

    @Test // FR-19
    fun maxSpeed_neverBelowStartSpeed() {
        val c = GameConfig.fromRemote(mapOf("snake_speed_start" to "8", "snake_speed_max" to "8"))
        assertTrue(c.snakeSpeedMax100 >= c.snakeSpeedStart100)
    }

    @Test // FR-19
    fun adsEnabled_parsesStrictly() {
        assertFalse(GameConfig.fromRemote(mapOf("ads_enabled" to " FALSE ")).adsEnabled)
        assertTrue(GameConfig.fromRemote(mapOf("ads_enabled" to "maybe")).adsEnabled)
    }

    @Test
    fun chunkFallUs_switchesAfterConfiguredApples() {
        val c = GameConfig()
        assertEquals(150_000, c.chunkFallUs(19))
        assertEquals(110_000, c.chunkFallUs(20))
    }
}
