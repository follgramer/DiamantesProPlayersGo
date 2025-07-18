package com.follgramer.diamantesproplayersgo.presentation.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(private val context: Context) {

    private val TAG = "InterstitialAdManager"
    private var interstitialAd: InterstitialAd? = null
    private val adUnitId = "ca-app-pub-3940256099942544/1033173712" // Test ad unit

    init {
        loadInterstitialAd()
    }

    fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
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

    fun showInterstitial(activity: Activity): Boolean {
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
}