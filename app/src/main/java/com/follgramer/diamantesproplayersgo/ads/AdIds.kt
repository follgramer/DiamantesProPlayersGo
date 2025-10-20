package com.follgramer.diamantesproplayersgo.ads

import android.util.Log
import com.follgramer.diamantesproplayersgo.BuildConfig

object AdIds {
    private const val TAG = "AdIds"

    // ==================== GOOGLE ADMOB IDs ====================

    // IDs de PRUEBA de AdMob (oficiales de Google)
    private const val TEST_ADMOB_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_ADMOB_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_ADMOB_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_ADMOB_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // IDs de PRODUCCIÓN de AdMob (tus IDs reales)
    private const val PROD_ADMOB_BANNER_TOP = "ca-app-pub-2024712392092488/8042311663"
    private const val PROD_ADMOB_BANNER_BOTTOM = "ca-app-pub-2024712392092488/4186978125"
    private const val PROD_ADMOB_INTERSTITIAL = "ca-app-pub-2024712392092488/4116891986"
    private const val PROD_ADMOB_REWARDED = "ca-app-pub-2024712392092488/6270064993"
    private const val PROD_ADMOB_NATIVE = "ca-app-pub-2024712392092488/6108630501"

    // ==================== FACEBOOK AUDIENCE NETWORK IDs ====================

    // IDs de PRUEBA de Facebook (usa los de producción en modo test)
    private const val TEST_FB_BANNER = "4267800920118134_4267828120115414"
    private const val TEST_FB_INTERSTITIAL = "4267800920118134_4267828803448679"
    private const val TEST_FB_REWARDED = "4267800920118134_4267829346781958"
    private const val TEST_FB_NATIVE = "4267800920118134_4267829570115269"

    // IDs de PRODUCCIÓN de Facebook (tus Placement IDs)
    private const val PROD_FB_BANNER_TOP = "4267800920118134_4267828120115414"
    private const val PROD_FB_BANNER_BOTTOM = "4267800920118134_4267828613448698"
    private const val PROD_FB_INTERSTITIAL = "4267800920118134_4267828803448679"
    private const val PROD_FB_REWARDED = "4267800920118134_4267829346781958"
    private const val PROD_FB_NATIVE = "4267800920118134_4267829570115269"

    // ==================== HELPERS ====================

    private fun useTest() = BuildConfig.DEBUG || BuildConfig.USE_TEST_ADS

    // ==================== ADMOB GETTERS ====================

    fun bannerTop(): String {
        val id = if (useTest()) TEST_ADMOB_BANNER else PROD_ADMOB_BANNER_TOP
        Log.d(TAG, "Banner Top (AdMob): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun bannerBottom(): String {
        val id = if (useTest()) TEST_ADMOB_BANNER else PROD_ADMOB_BANNER_BOTTOM
        Log.d(TAG, "Banner Bottom (AdMob): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun banner(): String {
        val id = if (useTest()) TEST_ADMOB_BANNER else PROD_ADMOB_BANNER_BOTTOM
        Log.d(TAG, "Banner genérico (AdMob): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun interstitial(): String {
        val id = if (useTest()) TEST_ADMOB_INTERSTITIAL else PROD_ADMOB_INTERSTITIAL
        Log.d(TAG, "Interstitial (AdMob): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun rewarded(): String {
        val id = if (useTest()) TEST_ADMOB_REWARDED else PROD_ADMOB_REWARDED
        Log.d(TAG, "Rewarded (AdMob): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun native(): String {
        val id = if (useTest()) TEST_ADMOB_NATIVE else PROD_ADMOB_NATIVE
        Log.d(TAG, "Native (AdMob): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    // ==================== FACEBOOK GETTERS ====================

    fun fbBannerTop(): String {
        val id = if (useTest()) TEST_FB_BANNER else PROD_FB_BANNER_TOP
        Log.d(TAG, "Banner Top (Facebook): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun fbBannerBottom(): String {
        val id = if (useTest()) TEST_FB_BANNER else PROD_FB_BANNER_BOTTOM
        Log.d(TAG, "Banner Bottom (Facebook): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun fbInterstitial(): String {
        val id = if (useTest()) TEST_FB_INTERSTITIAL else PROD_FB_INTERSTITIAL
        Log.d(TAG, "Interstitial (Facebook): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun fbRewarded(): String {
        val id = if (useTest()) TEST_FB_REWARDED else PROD_FB_REWARDED
        Log.d(TAG, "Rewarded (Facebook): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }

    fun fbNative(): String {
        val id = if (useTest()) TEST_FB_NATIVE else PROD_FB_NATIVE
        Log.d(TAG, "Native (Facebook): $id (${if (useTest()) "TEST" else "PROD"})")
        return id
    }
}// Updated: 2025-10-15 14:29:27
