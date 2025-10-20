package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.graphics.Color
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.*
import kotlinx.coroutines.*

object SimpleBannerHelper {
    private const val TAG = "SimpleBanner"
    private val loadedBanners = mutableMapOf<Int, AdView>()
    private val containerStates = mutableMapOf<Int, Boolean>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun loadBanner(activity: Activity, container: ViewGroup, adUnitId: String) {
        try {
            val containerId = System.identityHashCode(container)

            // Verificar AdMob
            if (!AdsInit.isAdMobReady()) {
                Log.w(TAG, "AdMob no está listo, reintentando en 2 segundos")
                scope.launch {
                    delay(2000)
                    if (!activity.isFinishing) {
                        loadBanner(activity, container, adUnitId)
                    }
                }
                return
            }

            // INICIAR COMPLETAMENTE OCULTO
            container.apply {
                visibility = View.GONE
                layoutParams?.height = 0
                removeAllViews()
                setBackgroundColor(Color.TRANSPARENT)
            }

            // Reutilizar banner existente
            loadedBanners[containerId]?.let { existing ->
                if (existing.parent == null) {
                    container.addView(existing)
                    if (containerStates[containerId] == true) {
                        container.visibility = View.VISIBLE
                        container.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    Log.d(TAG, "Reutilizando banner existente")
                    return
                }
            }

            // Crear AdView
            val adView = AdView(activity).apply {
                this.adUnitId = adUnitId
                setAdSize(getAdaptiveAdSize(activity))
            }

            // Configurar listener
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado exitosamente")
                    loadedBanners[containerId] = adView
                    containerStates[containerId] = true

                    container.apply {
                        visibility = View.VISIBLE
                        layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        setBackgroundColor(Color.TRANSPARENT)
                    }

                    container.alpha = 0f
                    container.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error cargando banner: ${error.message} (${error.code})")
                    Log.e(TAG, "Dominio: ${error.domain}")
                    containerStates[containerId] = false

                    container.apply {
                        visibility = View.GONE
                        layoutParams?.height = 0
                        removeAllViews()
                        setBackgroundColor(Color.TRANSPARENT)
                    }

                    when (error.code) {
                        3 -> { // No fill
                            Log.d(TAG, "No fill, esperando 60 segundos")
                            scope.launch {
                                delay(60000)
                                if (!activity.isFinishing) {
                                    loadBanner(activity, container, adUnitId)
                                }
                            }
                        }
                        2 -> { // Network error
                            scope.launch {
                                delay(5000)
                                if (!activity.isFinishing) {
                                    loadBanner(activity, container, adUnitId)
                                }
                            }
                        }
                        1 -> {
                            Log.e(TAG, "Request inválido, verificar configuración")
                        }
                    }
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Banner abierto")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner clickeado")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Impresión registrada")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner cerrado")
                }
            }

            container.addView(adView)

            Log.d(TAG, "📤 Encolando banner con ID: $adUnitId")

            // USAR AdRequestManager para controlar timing
            AdRequestManager.queueRequest {
                withContext(Dispatchers.Main) {
                    try {
                        val adRequest = AdRequest.Builder().build()
                        adView.loadAd(adRequest)
                        Log.d(TAG, "Request de banner ejecutada")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en loadAd: ${e.message}")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en loadBanner: ${e.message}", e)
            container.apply {
                visibility = View.GONE
                layoutParams?.height = 0
                setBackgroundColor(Color.TRANSPARENT)
                removeAllViews()
            }
        }
    }

    private fun getAdaptiveAdSize(activity: Activity): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val widthPixels = outMetrics.widthPixels
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    fun cleanup() {
        try {
            scope.cancel()
            loadedBanners.values.forEach { it.destroy() }
            loadedBanners.clear()
            containerStates.clear()
            Log.d(TAG, "Todos los banners limpiados")
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}")
        }
    }
}// Updated: 2025-10-15 14:29:27
