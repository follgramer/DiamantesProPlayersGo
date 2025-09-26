package com.follgramer.diamantesproplayersgo.ads

import com.follgramer.diamantesproplayersgo.BuildConfig

object AdIds {
    // Test (oficiales de Google)
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    // Producción (tus IDs reales)
    private const val PROD_BANNER_TOP = "ca-app-pub-2024712392092488/1328842504"
    private const val PROD_BANNER_BOTTOM = "ca-app-pub-2024712392092488/4186978125"
    private const val PROD_BANNER_RECYCLER = "ca-app-pub-2024712392092488/2811927139"
    private const val PROD_INTERSTITIAL = "ca-app-pub-2024712392092488/2117507153"
    private const val PROD_REWARDED = "ca-app-pub-2024712392092488/1363114099"

    private fun useTest() = BuildConfig.DEBUG || BuildConfig.USE_TEST_ADS

    fun bannerTop() = if (useTest()) TEST_BANNER else PROD_BANNER_TOP
    fun bannerBottom() = if (useTest()) TEST_BANNER else PROD_BANNER_BOTTOM
    fun bannerRecycler() = if (useTest()) TEST_BANNER else PROD_BANNER_RECYCLER
    fun interstitial() = if (useTest()) TEST_INTERSTITIAL else PROD_INTERSTITIAL
    fun rewarded() = if (useTest()) TEST_REWARDED else PROD_REWARDED
}