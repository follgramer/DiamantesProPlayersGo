package com.follgramer.diamantesproplayersgo.ads

import android.util.Log
import com.follgramer.diamantesproplayersgo.BuildConfig

object AdIds {
    private const val TAG = "AdIds"

    // Test IDs (oficiales de Google)
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // Producción - IDS REALES DE ADMOB
    private const val PROD_BANNER_TOP = "ca-app-pub-2024712392092488/8042311663"
    private const val PROD_BANNER_BOTTOM = "ca-app-pub-2024712392092488/4186978125"
    private const val PROD_INTERSTITIAL = "ca-app-pub-2024712392092488/4116891986"
    private const val PROD_REWARDED = "ca-app-pub-2024712392092488/6270064993"
    private const val PROD_NATIVE = "ca-app-pub-2024712392092488/6108630501"

    private fun useTest() = BuildConfig.DEBUG || BuildConfig.USE_TEST_ADS

    // ✅ FUNCIONES PÚBLICAS CORREGIDAS
    fun bannerTop(): String {
        val id = if (useTest()) TEST_BANNER else PROD_BANNER_TOP
        Log.d(TAG, "bannerTop() = $id (modo: ${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun bannerBottom(): String {
        val id = if (useTest()) TEST_BANNER else PROD_BANNER_BOTTOM
        Log.d(TAG, "bannerBottom() = $id (modo: ${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun interstitial(): String {
        val id = if (useTest()) TEST_INTERSTITIAL else PROD_INTERSTITIAL
        Log.d(TAG, "interstitial() = $id (modo: ${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun rewarded(): String {
        val id = if (useTest()) TEST_REWARDED else PROD_REWARDED
        Log.d(TAG, "rewarded() = $id (modo: ${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun native(): String {
        val id = if (useTest()) TEST_NATIVE else PROD_NATIVE
        Log.d(TAG, "native() = $id (modo: ${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    // ✅ ALIAS PARA COMPATIBILIDAD
    fun banner() = bannerTop()
    fun bannerRecycler() = bannerTop() // Usa el mismo que top

    // ✅ FUNCIÓN DE DEBUG
    fun getDebugInfo(): String {
        return """
            ═══════════════════════════════════════
            AdIds Configuration
            ═══════════════════════════════════════
            Modo: ${if (useTest()) "TEST (Debug)" else "PRODUCTION (Release)"}
            BuildConfig.DEBUG: ${BuildConfig.DEBUG}
            BuildConfig.USE_TEST_ADS: ${BuildConfig.USE_TEST_ADS}
            
            Banner Top: ${bannerTop()}
            Banner Bottom: ${bannerBottom()}
            Interstitial: ${interstitial()}
            Rewarded: ${rewarded()}
            Native: ${native()}
            ═══════════════════════════════════════
        """.trimIndent()
    }
}