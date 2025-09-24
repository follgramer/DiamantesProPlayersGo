package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.google.android.gms.ads.*
import kotlinx.coroutines.*

object BannerHelper {
    private const val TAG = "BannerHelper"
    private val bannersMap = mutableMapOf<Int, AdView>()
    private val loadingStatus = mutableMapOf<Int, Boolean>()
    private val retryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val retryCount = mutableMapOf<Int, Int>()
    private const val MAX_RETRY_ATTEMPTS = 3

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        try {
            val containerId = container.hashCode()

            // Limpiar contenedor
            container.removeAllViews()
            container.visibility = View.VISIBLE

            // Verificar si ya existe
            if (bannersMap.containsKey(containerId)) {
                Log.d(TAG, "Banner ya existe para container $containerId")
                return
            }

            // Verificar si AdMob está listo
            if (!AdsInit.isAdMobReady()) {
                Log.w(TAG, "AdMob no está listo, reintentando en 3 segundos...")
                retryScope.launch {
                    delay(3000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
                return
            }

            // Verificar si ya está cargando
            if (loadingStatus[containerId] == true) {
                Log.d(TAG, "Banner ya está cargando para container $containerId")
                return
            }

            loadingStatus[containerId] = true

            // Calcular tamaño adaptativo
            val adWidth = getAdaptiveBannerWidth(activity)
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)

            Log.d(TAG, "Banner size: ${adSize.width}x${adSize.height}")

            // Crear AdView
            val adView = AdView(activity).apply {
                adUnitId = determineAdUnitId(container)
                setAdSize(adSize)
            }

            Log.d(TAG, "Cargando banner con ID: ${adView.adUnitId}")

            // Configurar listener
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado exitosamente")
                    bannersMap[containerId] = adView
                    loadingStatus[containerId] = false
                    retryCount.remove(containerId)

                    activity.runOnUiThread {
                        container.visibility = View.VISIBLE
                        val density = activity.resources.displayMetrics.density
                        val minHeight = (adSize.height * density).toInt()
                        container.minimumHeight = minHeight
                        container.requestLayout()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Error cargando banner: ${error.message}")
                    loadingStatus[containerId] = false

                    // Reintentar después de 30 segundos
                    retryScope.launch {
                        delay(30000)
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            attachAdaptiveBanner(activity, container)
                        }
                    }
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner clickeado")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Banner mostrado (impresión registrada)")
                }
            }

            // Añadir al contenedor
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            container.addView(adView, layoutParams)

            // Crear y cargar AdRequest
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en attachAdaptiveBanner: ${e.message}", e)
            loadingStatus[container.hashCode()] = false

            try {
                container.visibility = View.GONE
            } catch (viewError: Exception) {
                Log.e(TAG, "Error ocultando contenedor: ${viewError.message}")
            }
        }
    }

    private fun determineAdUnitId(container: ViewGroup): String {
        return try {
            val resourceName = container.context.resources.getResourceEntryName(container.id)
            Log.d(TAG, "Determinando Ad Unit ID para: $resourceName")

            when {
                resourceName.contains("bannerBottomContainer") -> currentBannerBottomUnitId()
                resourceName.contains("adInProfileContainer") -> currentBannerTopUnitId()
                resourceName.contains("adLeaderboardContainer") -> currentRecyclerBannerUnitId()
                resourceName.contains("adTasksContainer") -> currentRecyclerBannerUnitId()
                else -> currentBannerTopUnitId()
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo determinar el resource name, usando banner top por defecto")
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

        val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        Log.d(TAG, "Ancho calculado para banner adaptativo: $adWidth dp")
        return adWidth
    }

    fun pauseBanners(container: ViewGroup) {
        try {
            bannersMap[container.hashCode()]?.pause()
        } catch (e: Exception) {
            Log.w(TAG, "Error pausando banner: ${e.message}")
        }
    }

    fun resumeBanners(container: ViewGroup) {
        try {
            bannersMap[container.hashCode()]?.resume()
        } catch (e: Exception) {
            Log.w(TAG, "Error resumiendo banner: ${e.message}")
        }
    }

    fun destroyBanners(container: ViewGroup) {
        try {
            val id = container.hashCode()
            bannersMap[id]?.destroy()
            bannersMap.remove(id)
            loadingStatus.remove(id)
            container.removeAllViews()
        } catch (e: Exception) {
            Log.w(TAG, "Error destruyendo banner: ${e.message}")
        }
    }

    fun cleanup() {
        try {
            retryScope.cancel()
            bannersMap.values.forEach { it.destroy() }
            bannersMap.clear()
            loadingStatus.clear()
            retryCount.clear()
            Log.d(TAG, "BannerHelper cleanup completado")
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}")
        }
    }
}