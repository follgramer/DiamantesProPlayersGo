package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object RecyclerViewBannerHelper {
    private const val TAG = "RVBannerHelper"
    private val loadedBanners = mutableMapOf<Int, AdView>()
    private val lastLoadTime = mutableMapOf<Int, Long>()
    private const val MIN_RELOAD_INTERVAL = 30000L // 30 segundos mínimo entre recargas

    fun loadAdaptiveBanner(
        activity: Activity,
        container: ViewGroup,
        horizontalMarginDp: Int = 0,
        viewHolderId: Int = 0
    ) {
        try {
            // Verificar si ya hay un banner cargado recientemente
            val currentTime = System.currentTimeMillis()
            val lastTime = lastLoadTime[viewHolderId] ?: 0L

            if (currentTime - lastTime < MIN_RELOAD_INTERVAL) {
                val existingBanner = loadedBanners[viewHolderId]
                if (existingBanner?.parent == null) {
                    container.removeAllViews()
                    container.addView(existingBanner)
                    container.visibility = android.view.View.VISIBLE
                }
                return
            }

            container.removeAllViews()
            container.visibility = android.view.View.VISIBLE

            val displayMetrics = activity.resources.displayMetrics
            val density = displayMetrics.density
            val screenWidthPx = displayMetrics.widthPixels
            val screenWidthDp = (screenWidthPx / density).toInt()

            val parentPaddingDp = when {
                viewHolderId >= 1000 -> 20  // Winners
                else -> 24  // Leaderboard
            }

            val availableWidthDp = screenWidthDp - parentPaddingDp

            Log.d(TAG, "Configurando banner adaptativo: ${availableWidthDp}dp")

            val adView = AdView(activity).apply {
                adUnitId = currentRecyclerBannerUnitId()
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    activity,
                    availableWidthDp
                ))
            }

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado en RecyclerView")
                    container.visibility = android.view.View.VISIBLE
                    loadedBanners[viewHolderId] = adView
                    lastLoadTime[viewHolderId] = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Error cargando banner: ${error.message}")
                    container.visibility = android.view.View.GONE
                    container.updateLayoutParams {
                        height = 0
                    }
                }
            }

            container.addView(adView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            container.visibility = android.view.View.GONE
        }
    }

    fun cleanup() {
        loadedBanners.values.forEach { it.destroy() }
        loadedBanners.clear()
        lastLoadTime.clear()
    }
}