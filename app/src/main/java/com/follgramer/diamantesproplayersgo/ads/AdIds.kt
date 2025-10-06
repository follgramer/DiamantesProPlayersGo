package com.follgramer.diamantesproplayersgo.ads

import android.util.Log
import com.follgramer.diamantesproplayersgo.BuildConfig

object AdIds {
    private const val TAG = "AdIds"

    // IDs de PRUEBA (oficiales de Google)
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // IDs de PRODUCCIÓN (tus IDs reales)
    private const val PROD_BANNER_TOP = "ca-app-pub-2024712392092488/8042311663"
    private const val PROD_BANNER_BOTTOM = "ca-app-pub-2024712392092488/4186978125"
    private const val PROD_INTERSTITIAL = "ca-app-pub-2024712392092488/4116891986"
    private const val PROD_REWARDED = "ca-app-pub-2024712392092488/6270064993"
    private const val PROD_NATIVE = "ca-app-pub-2024712392092488/6108630501"

    private fun useTest() = BuildConfig.DEBUG || BuildConfig.USE_TEST_ADS

    fun bannerTop(): String {
        val id = if (useTest()) TEST_BANNER else PROD_BANNER_TOP
        Log.d(TAG, """
            Banner Top solicitado:
            ID: $id
            Modo: ${if (useTest()) "TEST" else "PRODUCCIÓN"}
            BuildConfig.DEBUG: ${BuildConfig.DEBUG}
            BuildConfig.USE_TEST_ADS: ${BuildConfig.USE_TEST_ADS}
        """.trimIndent())
        return id
    }

    fun bannerBottom(): String {
        val id = if (useTest()) TEST_BANNER else PROD_BANNER_BOTTOM
        Log.d(TAG, "Banner Bottom: $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    // MÉTODO AGREGADO - Para compatibilidad con RecyclerViewBannerHelper
    fun banner(): String {
        val id = if (useTest()) TEST_BANNER else PROD_BANNER_BOTTOM
        Log.d(TAG, "Banner genérico: $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun interstitial(): String {
        val id = if (useTest()) TEST_INTERSTITIAL else PROD_INTERSTITIAL
        Log.d(TAG, "Interstitial: $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun rewarded(): String {
        val id = if (useTest()) TEST_REWARDED else PROD_REWARDED
        Log.d(TAG, "Rewarded: $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun native(): String {
        val id = if (useTest()) TEST_NATIVE else PROD_NATIVE
        Log.d(TAG, "Native: $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }
}