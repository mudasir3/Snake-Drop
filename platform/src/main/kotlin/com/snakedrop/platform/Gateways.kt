package com.snakedrop.platform

import android.content.Context

/** Persists the best score. */
interface ScoreStore {
    var bestScore: Int
}

/** SharedPreferences-backed [ScoreStore]. */
class PrefsScoreStore(context: Context) : ScoreStore {
    private val prefs = context.getSharedPreferences("snake_drop", Context.MODE_PRIVATE)

    override var bestScore: Int
        get() = prefs.getInt(KEY_BEST, 0)
        set(value) = prefs.edit().putInt(KEY_BEST, value).apply()

    private companion object {
        const val KEY_BEST = "best_score"
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
