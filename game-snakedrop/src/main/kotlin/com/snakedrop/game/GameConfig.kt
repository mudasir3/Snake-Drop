@file:Suppress("MagicNumber", "MaxLineLength")

package com.snakedrop.game

/**
 * Tunable values. Defaults are the PRD values. Remote values are parsed defensively and clamped
 * by [GameConfig.fromRemote]. Speeds are stored x100 so simulation math stays integer.
 */
public data class GameConfig(
    /** PRD 3: start 6 ticks/s. */
    val snakeSpeedStart100: Int = 600,
    /** PRD 3: +0.25 per apple. */
    val snakeSpeedStep100: Int = 25,
    /** PRD 3: cap 11 ticks/s. */
    val snakeSpeedMax100: Int = 1100,
    /** PRD 3: chunk falls 1 row per 150 ms. */
    val chunkFallMs: Int = 150,
    /** PRD 6: 110 ms after 20 apples. */
    val chunkFallMsFast: Int = 110,
    val fastFallAfterApples: Int = 20,
    /** PRD 3: arena shrinks when stack height exceeds 6. */
    val arenaShrinkThreshold: Int = 6,
    /** PRD 3: arena never below 7 rows. */
    val arenaMinRows: Int = 7,
    /** PRD 3: 1.5 s warning before the floor rises. */
    val shrinkWarningMs: Int = 1500,
    /** PRD 3: revive clears the bottom 2 well rows. */
    val reviveRowsCleared: Int = 2,
    /** PRD 7: interstitial after every 3rd game over. */
    val interstitialEveryN: Int = 3,
    /** PRD 7: at least 90 s between interstitials. */
    val interstitialMinGapS: Int = 90,
    /** PRD 7: first 3 runs are ad-free. */
    val interstitialFreeRuns: Int = 3,
    val adsEnabled: Boolean = true,
) {
    /** Microseconds for one chunk fall step, given apples eaten so far. */
    public fun chunkFallUs(applesEaten: Int): Int = (if (applesEaten >= fastFallAfterApples) chunkFallMsFast else chunkFallMs) * US_PER_MS

    public companion object {
        private const val US_PER_MS = 1000

        /**
         * Builds a config from raw remote strings. Missing, malformed, NaN or out-of-range values fall back
         * to the default or are clamped to the allowed range (see api.md).
         */
        public fun fromRemote(raw: Map<String, String?>): GameConfig {
            val d = GameConfig()
            val speedStart = speed(raw["snake_speed_start"], d.snakeSpeedStart100, 400, 800)
            val speedStep = speed(raw["snake_speed_step"], d.snakeSpeedStep100, 0, 50)
            val speedMax = speed(raw["snake_speed_max"], d.snakeSpeedMax100, 800, 1400)
            val fall = int(raw["chunk_fall_ms"], d.chunkFallMs, 80, 300)
            val fast = int(raw["chunk_fall_ms_fast"], d.chunkFallMsFast, 60, 200)
            return GameConfig(
                snakeSpeedStart100 = speedStart,
                snakeSpeedStep100 = speedStep,
                snakeSpeedMax100 = maxOf(speedMax, speedStart),
                chunkFallMs = fall,
                chunkFallMsFast = minOf(fast, fall),
                fastFallAfterApples = int(raw["fast_fall_after_apples"], d.fastFallAfterApples, 10, 60),
                arenaShrinkThreshold = int(raw["arena_shrink_threshold"], d.arenaShrinkThreshold, 4, 8),
                arenaMinRows = int(raw["arena_min_rows"], d.arenaMinRows, 5, GameConstants.ARENA_ROWS),
                shrinkWarningMs = int(raw["shrink_warning_ms"], d.shrinkWarningMs, 800, 3000),
                reviveRowsCleared = int(raw["revive_rows_cleared"], d.reviveRowsCleared, 1, 4),
                interstitialEveryN = int(raw["interstitial_every_n"], d.interstitialEveryN, 2, 6),
                interstitialMinGapS = int(raw["interstitial_min_gap_s"], d.interstitialMinGapS, 60, 300),
                interstitialFreeRuns = int(raw["interstitial_free_runs"], d.interstitialFreeRuns, 0, 6),
                adsEnabled = bool(raw["ads_enabled"], d.adsEnabled),
            )
        }

        private fun bool(
            text: String?,
            default: Boolean,
        ): Boolean =
            when (text?.trim()?.lowercase()) {
                "true" -> true
                "false" -> false
                else -> default
            }

        private fun int(
            text: String?,
            default: Int,
            min: Int,
            max: Int,
        ): Int {
            val v = text?.trim()?.toIntOrNull() ?: return default
            return v.coerceIn(min, max)
        }

        private fun speed(
            text: String?,
            default100: Int,
            min100: Int,
            max100: Int,
        ): Int {
            val v = text?.trim()?.toDoubleOrNull() ?: return default100
            if (v.isNaN() || v.isInfinite()) return default100
            return Math.round(v * 100.0).toInt().coerceIn(min100, max100)
        }
    }
}
