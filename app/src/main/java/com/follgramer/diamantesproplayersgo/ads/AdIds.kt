package com.follgramer.diamantesproplayersgo.ads

import com.follgramer.diamantesproplayersgo.BuildConfig

object AdIds {
    // Test (oficiales de Google)
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // Producción - USA TUS IDS REALES DE ADMOB
    private const val PROD_BANNER_TOP = "ca-app-pub-2024712392092488/8042311663"
    private const val PROD_BANNER_BOTTOM = "ca-app-pub-2024712392092488/4186978125"
    private const val PROD_INTERSTITIAL = "ca-app-pub-2024712392092488/4116891986"
    private const val PROD_REWARDED = "ca-app-pub-2024712392092488/6270064993"
    private const val PROD_NATIVE = "ca-app-pub-2024712392092488/6108630501"

    private fun useTest() = BuildConfig.DEBUG || BuildConfig.USE_TEST_ADS

    // Funciones públicas
    fun banner() = if (useTest()) TEST_BANNER else PROD_BANNER_TOP
    fun bannerBottom() = if (useTest()) TEST_BANNER else PROD_BANNER_BOTTOM
    fun interstitial() = if (useTest()) TEST_INTERSTITIAL else PROD_INTERSTITIAL
    fun rewarded() = if (useTest()) TEST_REWARDED else PROD_REWARDED
    fun native() = if (useTest()) TEST_NATIVE else PROD_NATIVE

    // ELIMINAR ESTAS LÍNEAS (no son necesarias):
    // fun bannerTop() = banner()
    // fun bannerRecycler() = banner()
}