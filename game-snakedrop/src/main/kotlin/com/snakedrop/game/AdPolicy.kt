package com.snakedrop.game

/**
 * Pure decision logic for interstitials and revive offers (PRD 7). It never shows ads; `:platform` does.
 * Time is injected so tests need no clock.
 */
public class AdPolicy(
    private val config: GameConfig = GameConfig(),
    totalGameOvers: Int = 0,
    gameOversSinceInterstitial: Int = 0,
    lastInterstitialMs: Long = NEVER,
) {
    /** Game overs recorded across the install. Persist this. */
    public var totalGameOvers: Int = totalGameOvers
        private set

    /** Game overs counted toward the next interstitial. Persist this. */
    public var gameOversSinceInterstitial: Int = gameOversSinceInterstitial
        private set

    /** Time the last interstitial was shown, epoch ms, or [NEVER]. Persist this. */
    public var lastInterstitialMs: Long = lastInterstitialMs
        private set

    /** Set when the player buys Remove Ads. Interstitials stop; rewarded ads stay opt-in. */
    public var adsRemoved: Boolean = false

    /**
     * Records a game over and returns true when an interstitial should be shown now: not in the first free runs,
     * after every Nth game over, at least the minimum gap after the previous one.
     */
    public fun onGameOver(nowMs: Long): Boolean {
        totalGameOvers++
        if (!config.adsEnabled || adsRemoved || totalGameOvers <= config.interstitialFreeRuns) return false
        gameOversSinceInterstitial++
        if (gameOversSinceInterstitial < config.interstitialEveryN) return false
        val gapMs = config.interstitialMinGapS * MS_PER_S
        if (lastInterstitialMs != NEVER && nowMs - lastInterstitialMs < gapMs) return false
        gameOversSinceInterstitial = 0
        lastInterstitialMs = nowMs
        return true
    }

    /** True when the Revive (watch ad) button may be offered. */
    public fun canOfferRevive(
        state: GameState,
        rewardedReady: Boolean,
    ): Boolean = config.adsEnabled && rewardedReady && SnakeDropEngine.canRevive(state)

    public companion object {
        /** Sentinel: no interstitial shown yet. */
        public const val NEVER: Long = -1L
        private const val MS_PER_S = 1000L
    }
}
