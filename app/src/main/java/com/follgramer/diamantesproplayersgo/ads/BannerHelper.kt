package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowMetrics
import android.widget.FrameLayout
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.google.android.gms.ads.*
import kotlinx.coroutines.*

object BannerHelper {
    private const val TAG = "BannerHelper"
    private val bannersMap = mutableMapOf<Int, AdView>()
    private val loadingStatus = mutableMapOf<Int, Boolean>()
    private val retryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        try {
            val containerId = container.hashCode()

            // Limpiar contenedor
            container.removeAllViews()
            container.visibility = View.VISIBLE

            // Verificar si ya existe
            if (bannersMap.containsKey(containerId)) {
                Log.d(TAG, "Banner ya existe")
                return
            }

            if (!AdsInit.isAdMobReady()) {
                Log.w(TAG, "AdMob no listo, reintentando...")
                retryScope.launch {
                    delay(3000)
                    if (!activity.isFinishing) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
                return
            }

            if (loadingStatus[containerId] == true) {
                return
            }

            loadingStatus[containerId] = true

            // Calcular tamaño
            val adWidth = getAdaptiveBannerWidth(activity)
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)

            // Crear AdView
            val adView = AdView(activity).apply {
                adUnitId = determineAdUnitId(container)
                setAdSize(adSize)
            }

            Log.d(TAG, "Cargando banner con ID: ${adView.adUnitId}")

            // Listener
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado")
                    bannersMap[containerId] = adView
                    loadingStatus[containerId] = false

                    activity.runOnUiThread {
                        container.visibility = View.VISIBLE
                        val density = activity.resources.displayMetrics.density
                        container.minimumHeight = (adSize.height * density).toInt()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error: ${error.message}")
                    loadingStatus[containerId] = false

                    // Reintentar
                    retryScope.launch {
                        delay(30000) // 30 segundos
                        if (!activity.isFinishing) {
                            attachAdaptiveBanner(activity, container)
                        }
                    }
                }
            }

            // Añadir al contenedor
            container.addView(adView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))

            // Cargar anuncio
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            loadingStatus[container.hashCode()] = false
        }
    }

    private fun determineAdUnitId(container: ViewGroup): String {
        return try {
            val resourceName = container.context.resources.getResourceEntryName(container.id)
            when {
                resourceName.contains("bannerBottomContainer") -> currentBannerBottomUnitId()
                resourceName.contains("adInProfileContainer") -> currentBannerTopUnitId()
                else -> currentBannerTopUnitId()
            }
        } catch (e: Exception) {
            currentBannerTopUnitId()
        }
    }

    private fun getAdaptiveBannerWidth(activity: Activity): Int {
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            displayMetrics.widthPixels = windowMetrics.bounds.width()
            displayMetrics.density = activity.resources.displayMetrics.density
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay?.getMetrics(displayMetrics)
        }

        return (displayMetrics.widthPixels / displayMetrics.density).toInt()
    }

    fun pauseBanners(container: ViewGroup) {
        bannersMap[container.hashCode()]?.pause()
    }

    fun resumeBanners(container: ViewGroup) {
        bannersMap[container.hashCode()]?.resume()
    }

    fun destroyBanners(container: ViewGroup) {
        val id = container.hashCode()
        bannersMap[id]?.destroy()
        bannersMap.remove(id)
        loadingStatus.remove(id)
        container.removeAllViews()
    }

    fun cleanup() {
        retryScope.cancel()
        bannersMap.values.forEach { it.destroy() }
        bannersMap.clear()
        loadingStatus.clear()
    }
}