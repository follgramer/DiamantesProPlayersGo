package com.follgramer.diamantesproplayersgo.ads

import com.follgramer.diamantesproplayersgo.BuildConfig

// --- IDS DE PRUEBA (SOLO PARA DEBUG) ---
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

// --- TUS IDS REALES DE PRODUCCIÃƒâ€œN ---
private const val PROD_BANNER_TOP_ID = "ca-app-pub-2024712392092488/1328842504"
private const val PROD_BANNER_BOTTOM_ID = "ca-app-pub-2024712392092488/4186978125"
private const val PROD_BANNER_RECYCLER_ID = "ca-app-pub-2024712392092488/2811927139"
private const val PROD_INTERSTITIAL_ID = "ca-app-pub-2024712392092488/2117507153"
private const val PROD_REWARDED_ID = "ca-app-pub-2024712392092488/1363114099"

// --- FUNCIONES PÃƒÅ¡BLICAS PARA ACCEDER A LOS IDS ---

/**
 * Devuelve el ID del banner superior. Usa el ID de prueba en DEBUG.
 */
fun currentBannerTopUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_TOP_ID
}

/**
 * Devuelve el ID del banner inferior. Usa el ID de prueba en DEBUG.
 */
fun currentBannerBottomUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_BOTTOM_ID
}

/**
 * Devuelve el ID del banner para RecyclerViews. Usa el ID de prueba en DEBUG.
 */
fun currentRecyclerBannerUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_RECYCLER_ID
}

/**
 * Devuelve un ID de banner genÃƒÂ©rico (alias para el superior).
 */
fun currentBannerUnitId(): String = currentBannerTopUnitId()

/**
 * Devuelve el ID del Interstitial. Usa el ID de prueba en DEBUG.
 */
fun currentInterstitialUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID
}

/**
 * Devuelve el ID del Rewarded. Usa el ID de prueba en DEBUG.
 */
fun currentRewardedUnitId(): String {
    return if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID
}