@file:Suppress("MaxLineLength")

package com.snakedrop.platform

import android.content.Context

/** Persisted player progress. */
interface ProfileStore {
    var bestScore: Int
    var tutorialDone: Boolean

    /** Ad pacing counters, persisted so the first free runs and gaps survive app restarts. */
    var totalGameOvers: Int
    var gameOversSinceInterstitial: Int
    var lastInterstitialMs: Long

    /** Stars (0..3) earned on a level; 0 means not completed. */
    fun stars(levelId: Int): Int

    fun setStars(
        levelId: Int,
        stars: Int,
    )
}

/** SharedPreferences-backed [ProfileStore]. */
class PrefsProfileStore(context: Context) : ProfileStore {
    private val prefs = context.getSharedPreferences("snake_drop", Context.MODE_PRIVATE)

    override var bestScore: Int
        get() = prefs.getInt(KEY_BEST, 0)
        set(value) = prefs.edit().putInt(KEY_BEST, value).apply()

    override var tutorialDone: Boolean
        get() = prefs.getBoolean(KEY_TUTORIAL, false)
        set(value) = prefs.edit().putBoolean(KEY_TUTORIAL, value).apply()

    override var totalGameOvers: Int
        get() = prefs.getInt(KEY_TOTAL_OVERS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_OVERS, value).apply()

    override var gameOversSinceInterstitial: Int
        get() = prefs.getInt(KEY_SINCE_AD, 0)
        set(value) = prefs.edit().putInt(KEY_SINCE_AD, value).apply()

    override var lastInterstitialMs: Long
        get() = prefs.getLong(KEY_LAST_AD, -1L)
        set(value) = prefs.edit().putLong(KEY_LAST_AD, value).apply()

    override fun stars(levelId: Int): Int = prefs.getInt("$KEY_STARS$levelId", 0)

    override fun setStars(
        levelId: Int,
        stars: Int,
    ) {
        if (stars > stars(levelId)) prefs.edit().putInt("$KEY_STARS$levelId", stars.coerceIn(0, MAX_STARS)).apply()
    }

    private companion object {
        const val KEY_BEST = "best_score"
        const val KEY_TUTORIAL = "tutorial_done"
        const val KEY_STARS = "stars_"
        const val KEY_TOTAL_OVERS = "total_game_overs"
        const val KEY_SINCE_AD = "overs_since_ad"
        const val KEY_LAST_AD = "last_interstitial_ms"
        const val MAX_STARS = 3
    }
}

/** Ads boundary. `:app` only talks to this interface; the AdMob implementation lives in [AdMobGateway]. */
interface AdsGateway {
    /** Collects consent where required, initializes the SDK and preloads ads. Safe to call once per launch. */
    fun start()

    fun isRewardedReady(): Boolean

    /** Shows an interstitial if one is loaded, then calls [onClosed]. Calls [onClosed] right away when none is ready. */
    fun showInterstitial(onClosed: () -> Unit)

    /** Shows a rewarded ad. [onReward] fires only if the user earned it; [onClosed] always fires after the ad is gone. */
    fun showRewarded(
        onReward: () -> Unit,
        onClosed: () -> Unit,
    )
}

/** No ads: used in tests and when ads are disabled. */
object NoAdsGateway : AdsGateway {
    override fun start() = Unit

    override fun isRewardedReady(): Boolean = false

    override fun showInterstitial(onClosed: () -> Unit) = onClosed()

    override fun showRewarded(
        onReward: () -> Unit,
        onClosed: () -> Unit,
    ) = onClosed()
}
