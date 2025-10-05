package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.graphics.Color
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.follgramer.diamantesproplayersgo.R
import com.google.android.gms.ads.*
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.*

object BannerHelper {
    private const val TAG = "BannerHelper"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val loadedBanners = mutableMapOf<Int, AdView>()
    private val loadingJobs = mutableMapOf<Int, Job?>()
    private val failedContainers = mutableSetOf<Int>()

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)

        // Cancelar cualquier carga previa
        loadingJobs[containerId]?.cancel()

        // Verificar cooldown
        if (failedContainers.contains(containerId)) {
            Log.d(TAG, "Container $containerId en cooldown temporal")
            scope.launch {
                delay(30000)
                failedContainers.remove(containerId)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Verificar consentimiento
        if (!UserMessagingPlatform.getConsentInformation(activity).canRequestAds()) {
            Log.d(TAG, "⏸️ Esperando consentimiento para container $containerId")
            // Mantener oculto (100% legal)
            container.apply {
                visibility = View.GONE
                layoutParams.height = 0
                setBackgroundColor(Color.TRANSPARENT)
            }

            loadingJobs[containerId] = scope.launch {
                delay(2000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Verificar AdMob
        if (!AdsInit.isAdMobReady()) {
            Log.d(TAG, "⏸️ AdMob no está listo para container $containerId")
            container.apply {
                visibility = View.GONE
                layoutParams.height = 0
                setBackgroundColor(Color.TRANSPARENT)
            }

            loadingJobs[containerId] = scope.launch {
                delay(1000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Reutilizar banner existente
        loadedBanners[containerId]?.let { existingAd ->
            if (existingAd.parent == null) {
                container.removeAllViews()
                container.addView(existingAd)
                container.visibility = View.VISIBLE
                container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                Log.d(TAG, "♻️ Reutilizando banner existente para container $containerId")
                return
            }
        }

        // Crear y cargar nuevo banner
        loadingJobs[containerId] = scope.launch {
            try {
                container.removeAllViews()

                // INICIAR OCULTO (100% legal para AdMob)
                container.apply {
                    visibility = View.GONE
                    layoutParams.height = 0
                    setBackgroundColor(Color.TRANSPARENT)
                }

                val adView = AdView(activity).apply {
                    adUnitId = getAdUnitId(container)
                    setAdSize(getAdaptiveAdSize(activity, container))
                }

                loadedBanners[containerId] = adView
                container.addView(adView)

                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "✅ Banner cargado para container $containerId")
                        failedContainers.remove(containerId)

                        // SOLO AHORA mostrar con altura real
                        container.apply {
                            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                            setBackgroundColor(Color.TRANSPARENT)
                            visibility = View.VISIBLE
                        }

                        // Animación suave
                        container.alpha = 0f
                        container.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "❌ Error cargando banner: ${error.message} (código: ${error.code})")
                        Log.e(TAG, "Dominio: ${error.domain}, Causa: ${error.cause}")

                        // Mantener oculto en caso de error
                        container.apply {
                            visibility = View.GONE
                            layoutParams.height = 0
                            setBackgroundColor(Color.TRANSPARENT)
                        }

                        when (error.code) {
                            AdRequest.ERROR_CODE_NO_FILL -> {
                                failedContainers.add(containerId)
                                scope.launch {
                                    delay(60000) // Reintentar en 1 minuto
                                    failedContainers.remove(containerId)
                                    if (!activity.isFinishing) {
                                        attachAdaptiveBanner(activity, container)
                                    }
                                }
                            }
                            AdRequest.ERROR_CODE_NETWORK_ERROR -> {
                                loadingJobs[containerId] = scope.launch {
                                    delay(5000) // 5 segundos
                                    if (!activity.isFinishing) {
                                        attachAdaptiveBanner(activity, container)
                                    }
                                }
                            }
                            else -> {
                                loadingJobs[containerId] = scope.launch {
                                    delay(15000)
                                    if (!activity.isFinishing) {
                                        attachAdaptiveBanner(activity, container)
                                    }
                                }
                            }
                        }
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "🖱️ Banner clickeado")
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "👁️ Impresión de banner registrada")
                    }

                    override fun onAdOpened() {
                        Log.d(TAG, "📱 Banner abierto")
                    }

                    override fun onAdClosed() {
                        Log.d(TAG, "📱 Banner cerrado")
                    }
                }

                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)

                Log.d(TAG, "📤 Solicitando banner para container $containerId con ID: ${adView.adUnitId}")

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error crítico: ${e.message}", e)
                container.apply {
                    visibility = View.GONE
                    layoutParams.height = 0
                    setBackgroundColor(Color.TRANSPARENT)
                }

                // Reintentar después de error crítico
                loadingJobs[containerId] = scope.launch {
                    delay(30000) // 30 segundos
                    if (!activity.isFinishing) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
            }
        }
    }

    private fun getAdaptiveAdSize(activity: Activity, container: ViewGroup): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = container.width.toFloat()

        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    private fun getAdUnitId(container: ViewGroup): String {
        val adId = when (container.id) {
            R.id.adInProfileContainer -> AdIds.banner()
            R.id.bannerBottomContainer -> AdIds.bannerBottom()
            else -> AdIds.banner()
        }
        Log.d(TAG, "📌 Usando Ad Unit ID: $adId para container ${container.id}")
        return adId
    }

    fun pause(parent: ViewGroup) {
        try {
            loadedBanners.values.forEach { it.pause() }
            Log.d(TAG, "Banners pausados")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausando banners: ${e.message}")
        }
    }

    fun resume(parent: ViewGroup) {
        try {
            loadedBanners.values.forEach { it.resume() }
            Log.d(TAG, "Banners resumidos")
        } catch (e: Exception) {
            Log.e(TAG, "Error resumiendo banners: ${e.message}")
        }
    }

    fun destroy(parent: ViewGroup) {
        try {
            loadingJobs.values.forEach { it?.cancel() }
            loadingJobs.clear()
            loadedBanners.values.forEach { it.destroy() }
            loadedBanners.clear()
            failedContainers.clear()
            scope.cancel()
            Log.d(TAG, "Banners destruidos y recursos liberados")
        } catch (e: Exception) {
            Log.e(TAG, "Error destruyendo banners: ${e.message}")
        }
    }

    fun forceRefresh(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)
        failedContainers.remove(containerId)
        loadedBanners.remove(containerId)?.destroy()
        loadingJobs[containerId]?.cancel()
        attachAdaptiveBanner(activity, container)
        Log.d(TAG, "Forzando recarga de banner para container $containerId")
    }
}