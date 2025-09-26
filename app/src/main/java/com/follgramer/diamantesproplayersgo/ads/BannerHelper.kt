package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

        // Si ya falló antes, no reintentar inmediatamente
        if (failedContainers.contains(containerId)) {
            container.visibility = View.GONE
            container.layoutParams.height = 0
            return
        }

        // Verificar consentimiento primero
        if (!UserMessagingPlatform.getConsentInformation(activity).canRequestAds()) {
            Log.d(TAG, "⏸️ Esperando consentimiento para container $containerId")
            container.visibility = View.GONE
            container.layoutParams.height = 0

            // Reintentar después de 2 segundos
            loadingJobs[containerId] = scope.launch {
                delay(2000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Verificar si AdMob está listo
        if (!AdsInit.isAdMobReady()) {
            Log.d(TAG, "⏸️ AdMob no está listo para container $containerId")
            container.visibility = View.GONE
            container.layoutParams.height = 0

            // Reintentar después de 1 segundo
            loadingJobs[containerId] = scope.launch {
                delay(1000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Reutilizar banner existente si está disponible
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

                // Mantener invisible hasta que cargue
                container.visibility = View.GONE
                container.layoutParams.height = 0

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

                        // Hacer visible solo cuando carga exitosamente
                        container.visibility = View.VISIBLE
                        container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

                        // Animación suave de entrada
                        container.alpha = 0f
                        container.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "❌ Error cargando banner: ${error.message} (código: ${error.code})")

                        // Mantener oculto si falla
                        container.visibility = View.GONE
                        container.layoutParams.height = 0

                        // Marcar como fallido si es No Fill
                        if (error.code == AdRequest.ERROR_CODE_NO_FILL) {
                            failedContainers.add(containerId)

                            // Limpiar después de 5 minutos
                            scope.launch {
                                delay(300000) // 5 minutos
                                failedContainers.remove(containerId)
                            }
                        } else {
                            // Reintentar para otros errores después de 30 segundos
                            loadingJobs[containerId] = scope.launch {
                                delay(30000)
                                if (!activity.isFinishing) {
                                    attachAdaptiveBanner(activity, container)
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
                }

                // Cargar el anuncio
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)

                Log.d(TAG, "📤 Solicitando banner para container $containerId con ID: ${adView.adUnitId}")

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error crítico: ${e.message}")
                container.visibility = View.GONE
                container.layoutParams.height = 0
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
            R.id.adInProfileContainer -> AdIds.bannerTop()
            R.id.bannerBottomContainer -> AdIds.bannerBottom()
            else -> AdIds.bannerBottom()
        }
        Log.d(TAG, "📌 Usando Ad Unit ID: $adId para container ${container.id}")
        return adId
    }

    fun pause(parent: ViewGroup) {
        loadedBanners.values.forEach { it.pause() }
    }

    fun resume(parent: ViewGroup) {
        loadedBanners.values.forEach { it.resume() }
    }

    fun destroy(parent: ViewGroup) {
        loadingJobs.values.forEach { it?.cancel() }
        loadingJobs.clear()
        loadedBanners.values.forEach { it.destroy() }
        loadedBanners.clear()
        failedContainers.clear()
    }

    fun forceRefresh(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)
        failedContainers.remove(containerId)
        loadedBanners.remove(containerId)?.destroy()
        attachAdaptiveBanner(activity, container)
    }
}