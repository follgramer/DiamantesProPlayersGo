package com.follgramer.diamantesproplayersgo.presentation.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(private val context: Context) {

    private val TAG = "RewardedAdManager"
    private var rewardedAd: RewardedAd? = null
    private val adUnitId = "ca-app-pub-3940256099942544/5224354917" // Test ad unit

    init {
        loadRewardedAd()
    }

    fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            adUnitId,
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
}