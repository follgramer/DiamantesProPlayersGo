package com.follgramer.diamantesproplayersgo.presentation.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AdMobManager? = null
        private const val TAG = "AdMobManager"

        // Test Ad Unit IDs
        private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        fun getInstance(context: Context): AdMobManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AdMobManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun initialize() {
        MobileAds.initialize(context) {
            Log.d(TAG, "AdMob initialized successfully")
        }
    }

    fun loadBannerAd(adView: AdView) {
        val adRequest = AdRequest.Builder().build()

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d(TAG, "Banner ad loaded successfully")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                Log.e(TAG, "Banner ad failed to load: ${error.message}")
            }
        }

        adView.loadAd(adRequest)
    }

    fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    super.onAdLoaded(ad)
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    interstitialAd = null
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity): Boolean {
        return if (interstitialAd != null) {
            interstitialAd?.show(activity)
            interstitialAd = null
            loadInterstitialAd() // Preload next ad
            true
        } else {
            Log.d(TAG, "Interstitial ad not ready")
            loadInterstitialAd() // Try to load if not available
            false
        }
    }

    fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    super.onAdLoaded(ad)
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    rewardedAd = null
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: (RewardItem) -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned(rewardItem)
            }
            rewardedAd = null
            loadRewardedAd() // Preload next ad
        } else {
            Log.d(TAG, "Rewarded ad not ready")
            loadRewardedAd() // Try to load if not available
        }
    }

    fun destroyBannerAd() {
        // Cleanup if needed
        Log.d(TAG, "Banner ad cleanup")
    }
}