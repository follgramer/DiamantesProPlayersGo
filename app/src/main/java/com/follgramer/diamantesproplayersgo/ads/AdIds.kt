package com.follgramer.diamantesproplayersgo.ads

import com.follgramer.diamantesproplayersgo.BuildConfig

/**
 * Gestión centralizada de IDs de anuncios AdMob
 * Cambia automáticamente entre anuncios de prueba y producción según BuildConfig.USE_TEST_ADS
 */

// ============================================
// TEST IDS - OFICIALES DE GOOGLE ADMOB
// ============================================
private const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

// ============================================
// PRODUCTION IDS - TUS IDS REALES DE ADMOB
// ============================================
<<<<<<< HEAD
private const val PROD_APP_ID = "ca-app-pub-2024712392092488~7992650364"
=======
// IMPORTANTE: Reemplaza estos IDs con los tuyos reales de tu cuenta AdMob
private const val PROD_APP_ID = "ca-app-pub-2024712392092488~1234567890" // Tu App ID real
>>>>>>> 999fd88ece3337ae0871be7e0514342d32569941
private const val PROD_BANNER_TOP_ID = "ca-app-pub-2024712392092488/1328842504"
private const val PROD_BANNER_BOTTOM_ID = "ca-app-pub-2024712392092488/4186978125"
private const val PROD_BANNER_RECYCLER_ID = "ca-app-pub-2024712392092488/2811927139"
private const val PROD_INTERSTITIAL_ID = "ca-app-pub-2024712392092488/2117507153"
private const val PROD_REWARDED_ID = "ca-app-pub-2024712392092488/1363114099"

/**
 * Obtiene el App ID actual según el modo de compilación
 */
fun currentAppId(): String {
    return if (BuildConfig.USE_TEST_ADS) TEST_APP_ID else PROD_APP_ID
}

/**
 * Obtiene el ID del banner superior según el modo de compilación
 */
fun currentBannerTopUnitId(): String {
    return if (BuildConfig.USE_TEST_ADS) TEST_BANNER_ID else PROD_BANNER_TOP_ID
}

/**
 * Obtiene el ID del banner inferior según el modo de compilación
 */
fun currentBannerBottomUnitId(): String {
    return if (BuildConfig.USE_TEST_ADS) TEST_BANNER_ID else PROD_BANNER_BOTTOM_ID
}

/**
 * Obtiene el ID del banner para RecyclerView según el modo de compilación
 */
fun currentRecyclerBannerUnitId(): String {
    return if (BuildConfig.USE_TEST_ADS) TEST_BANNER_ID else PROD_BANNER_RECYCLER_ID
}

/**
 * Obtiene el ID del banner por defecto (usa el banner superior)
 */
fun currentBannerUnitId(): String = currentBannerTopUnitId()

/**
 * Obtiene el ID del anuncio intersticial según el modo de compilación
 */
fun currentInterstitialUnitId(): String {
    return if (BuildConfig.USE_TEST_ADS) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID
}

/**
 * Obtiene el ID del anuncio recompensado según el modo de compilación
 */
fun currentRewardedUnitId(): String {
    return if (BuildConfig.USE_TEST_ADS) TEST_REWARDED_ID else PROD_REWARDED_ID
}

/**
 * Verifica si estamos usando anuncios de prueba
 */
fun isUsingTestAds(): Boolean = BuildConfig.USE_TEST_ADS

/**
 * Obtiene información de debug sobre los IDs actuales
 */
fun getAdIdsDebugInfo(): String {
    return buildString {
        appendLine("=== AD IDS DEBUG INFO ===")
        appendLine("Modo: ${if (isUsingTestAds()) "TEST ADS" else "PRODUCTION ADS"}")
        appendLine("App ID: ${currentAppId()}")
        appendLine("Banner Top: ${currentBannerTopUnitId()}")
        appendLine("Banner Bottom: ${currentBannerBottomUnitId()}")
        appendLine("Banner Recycler: ${currentRecyclerBannerUnitId()}")
        appendLine("Interstitial: ${currentInterstitialUnitId()}")
        appendLine("Rewarded: ${currentRewardedUnitId()}")
        appendLine("========================")
    }
}