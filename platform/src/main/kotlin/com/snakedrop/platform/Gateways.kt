package com.snakedrop.platform

import android.content.Context

/** Persisted player progress. */
interface ProfileStore {
    var bestScore: Int
    var tutorialDone: Boolean

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
        const val MAX_STARS = 3
    }
}

/** Ads boundary. The real AdMob implementation is added later; `:app` only talks to this interface. */
interface AdsGateway {
    fun isRewardedReady(): Boolean

    fun showInterstitial()

    fun showRewarded(onReward: () -> Unit)
}

/** No ads: used until an ad SDK is approved and added. */
object NoAdsGateway : AdsGateway {
    override fun isRewardedReady(): Boolean = false

    override fun showInterstitial() = Unit

    override fun showRewarded(onReward: () -> Unit) = Unit
}
