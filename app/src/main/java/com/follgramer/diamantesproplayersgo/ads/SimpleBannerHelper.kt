package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import com.google.android.gms.ads.*

object SimpleBannerHelper {
    private const val TAG = "SimpleBanner"

    fun loadBanner(activity: Activity, container: ViewGroup, adUnitId: String) {
        try {
            // Asegurar que el contenedor está visible
            container.visibility = android.view.View.VISIBLE
            container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            container.removeAllViews()

            // Crear AdView
            val adView = AdView(activity)
            adView.adUnitId = adUnitId

            // Calcular tamaño adaptativo
            val display = activity.windowManager.defaultDisplay
            val outMetrics = android.util.DisplayMetrics()
            display.getMetrics(outMetrics)
            val widthPixels = outMetrics.widthPixels.toFloat()
            val density = outMetrics.density
            val adWidth = (widthPixels / density).toInt()

            adView.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
            )

            // Listener simple
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado exitosamente")
                    container.visibility = android.view.View.VISIBLE
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error: ${error.message} (${error.code})")
                    container.layoutParams.height = 1
                }
            }

            // Agregar vista y cargar
            container.addView(adView)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            Log.d(TAG, "📤 Solicitando banner con ID: $adUnitId")

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error crítico: ${e.message}")
        }
    }
}