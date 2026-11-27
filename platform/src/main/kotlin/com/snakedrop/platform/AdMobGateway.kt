package com.snakedrop.platform

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * AdMob implementation of [AdsGateway]. Collects consent with Google's UMP first (required in the EEA/UK),
 * and only initializes the SDK and loads ads once consent allows it. Interstitial and rewarded ads are
 * preloaded and reloaded after each show. Must be used from the main thread.
 */
class AdMobGateway(
    private val activity: Activity,
    private val interstitialId: String,
    private val rewardedId: String,
) : AdsGateway {
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var initialized = false
    private var started = false

    override fun start() {
        if (started) return
        started = true
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { initIfAllowed(info) }
            },
            { initIfAllowed(info) },
        )
        initIfAllowed(info)
    }

    private fun initIfAllowed(info: ConsentInformation) {
        if (!info.canRequestAds() || initialized) return
        initialized = true
        MobileAds.initialize(activity) {
            loadInterstitial()
            loadRewarded()
        }
    }

    override fun isRewardedReady(): Boolean = rewarded != null

    override fun showInterstitial(onClosed: () -> Unit) {
        val ad = interstitial
        if (ad == null) {
            loadInterstitial()
            onClosed()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadInterstitial()
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadInterstitial()
                    onClosed()
                }
            }
        ad.show(activity)
    }

    override fun showRewarded(
        onReward: () -> Unit,
        onClosed: () -> Unit,
    ) {
        val ad = rewarded
        if (ad == null) {
            loadRewarded()
            onClosed()
            return
        }
        rewarded = null
        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadRewarded()
                    onClosed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    loadRewarded()
                    onClosed()
                }
            }
        ad.show(activity) { onReward() }
    }

    private fun loadInterstitial() {
        if (!initialized || interstitial != null) return
        InterstitialAd.load(
            activity,
            interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                }
            },
        )
    }

    private fun loadRewarded() {
        if (!initialized || rewarded != null) return
        RewardedAd.load(
            activity,
            rewardedId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                }
            },
        )
    }
}
