package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.*

object RecyclerViewBannerHelper {
    private const val TAG = "RVBannerHelper"
    private val loadedBanners = mutableMapOf<Int, AdView>()

    fun loadAdaptiveBanner(
        activity: Activity,
        container: ViewGroup,
        horizontalMarginDp: Int = 0,
        viewHolderId: Int = 0
    ) {
        try {
            // Limpiar contenedor
            container.removeAllViews()
            container.visibility = View.VISIBLE

            // Reutilizar si existe
            loadedBanners[viewHolderId]?.let { existing ->
                if (existing.parent == null) {
                    container.addView(existing)
                    return
                }
            }

            // Calcular tamaño
            val displayMetrics = activity.resources.displayMetrics
            val screenWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val availableWidthDp = screenWidthDp - 24

            // Crear AdView
            val adView = AdView(activity).apply {
                adUnitId = currentRecyclerBannerUnitId()
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity, availableWidthDp
                ))
            }

            // Listener
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado")
                    loadedBanners[viewHolderId] = adView
                    container.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error: ${error.message}")
                    container.visibility = View.GONE
                }
            }

            // Añadir y cargar
            container.addView(adView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))

            adView.loadAd(AdRequest.Builder().build())

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            container.visibility = View.GONE
        }
    }

    fun cleanup() {
        loadedBanners.values.forEach { it.destroy() }
        loadedBanners.clear()
    }
}