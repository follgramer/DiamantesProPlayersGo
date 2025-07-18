package com.follgramer.diamantesproplayersgo.presentation.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*

class BannerAdManager(private val context: Context) {

    companion object {
        private const val TAG = "BannerAdManager"
        private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    }

    private var bannerAd: AdView? = null

    fun loadBannerAd(adView: AdView) {
        bannerAd = adView
        bannerAd?.adUnitId = BANNER_AD_UNIT_ID
        bannerAd?.setAdSize(AdSize.BANNER)

        bannerAd?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Banner ad failed to load: ${adError.message}")
            }

            override fun onAdClicked() {
                Log.d(TAG, "Banner ad clicked")
            }
        }

        val adRequest = AdRequest.Builder().build()
        bannerAd?.loadAd(adRequest)
    }

    fun destroyBannerAd() {
        bannerAd?.destroy()
        bannerAd = null
    }
}