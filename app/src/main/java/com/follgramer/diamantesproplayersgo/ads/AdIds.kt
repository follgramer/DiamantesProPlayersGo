package com.follgramer.diamantesproplayersgo.ads

import com.follgramer.diamantesproplayersgo.BuildConfig

// TEST IDS
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

// PRODUCTION IDS
private const val PROD_BANNER_TOP_ID = "ca-app-pub-2024712392092488/1328842504"
private const val PROD_BANNER_BOTTOM_ID = "ca-app-pub-2024712392092488/4186978125"
private const val PROD_BANNER_RECYCLER_ID = "ca-app-pub-2024712392092488/2811927139"
private const val PROD_INTERSTITIAL_ID = "ca-app-pub-2024712392092488/2117507153"
private const val PROD_REWARDED_ID = "ca-app-pub-2024712392092488/1363114099"

fun currentBannerTopUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_TOP_ID
}

fun currentBannerBottomUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_BOTTOM_ID
}

fun currentRecyclerBannerUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_RECYCLER_ID
}

fun currentBannerUnitId(): String = currentBannerTopUnitId()

fun currentInterstitialUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID
}

fun currentRewardedUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID
}