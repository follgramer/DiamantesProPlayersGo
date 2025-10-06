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
    private val failedContainers = mutableListOf<Int>()

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)

        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🎯 attachAdaptiveBanner INICIADO")
        Log.d(TAG, "Container ID: $containerId")

        // ✅ CRÍTICO: Empezar OCULTO, solo mostrar si el anuncio carga
        container.visibility = View.GONE
        container.layoutParams = container.layoutParams.apply {
            height = 0
        }

        // Cancelar cualquier carga previa
        loadingJobs[containerId]?.cancel()

        // Verificar consentimiento
        val canRequestAds = try {
            val result = UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
            Log.d(TAG, "🔐 Consentimiento: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando consentimiento: ${e.message}")
            false
        }

        if (!canRequestAds) {
            Log.w(TAG, "⚠️ Sin consentimiento - contenedor permanecerá oculto")
            return
        }

        // Verificar AdMob
        if (!AdsInit.isAdMobReady()) {
            Log.w(TAG, "⚠️ AdMob no listo - reintentando en 2s")
            loadingJobs[containerId] = scope.launch {
                delay(2000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        Log.d(TAG, "✅ AdMob listo y consentimiento obtenido")

        // Reutilizar banner existente
        loadedBanners[containerId]?.let { existingAd ->
            if (existingAd.parent == null) {
                Log.d(TAG, "♻️ Reutilizando banner existente")
                container.removeAllViews()
                container.addView(existingAd)
                // ✅ MOSTRAR solo si hay banner existente
                container.visibility = View.VISIBLE
                container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                return
            }
        }

        // Crear y cargar nuevo banner
        loadingJobs[containerId] = scope.launch {
            try {
                Log.d(TAG, "🚀 Iniciando carga de nuevo banner...")
                container.removeAllViews()

                val adUnitId = getAdUnitId(container)
                val adSize = getAdaptiveAdSize(activity, container)

                Log.d(TAG, "📋 Configuración:")
                Log.d(TAG, "   Ad Unit ID: $adUnitId")
                Log.d(TAG, "   Ad Size: ${adSize.width}x${adSize.height} dp")

                val adView = AdView(activity).apply {
                    this.adUnitId = adUnitId
                    setAdSize(adSize)
                }

                loadedBanners[containerId] = adView

                val layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                container.addView(adView, layoutParams)

                Log.d(TAG, "✅ AdView agregado al contenedor")

                // Configurar listener
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "═══════════════════════════════════════")
                        Log.d(TAG, "✅ ¡BANNER CARGADO! - MOSTRANDO contenedor")
                        Log.d(TAG, "═══════════════════════════════════════")

                        // ✅ SOLO AHORA mostrar el contenedor
                        container.visibility = View.VISIBLE
                        container.layoutParams = container.layoutParams.apply {
                            height = ViewGroup.LayoutParams.WRAP_CONTENT
                        }

                        adView.requestLayout()
                        container.requestLayout()

                        failedContainers.removeAll { it == containerId }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "═══════════════════════════════════════")
                        Log.e(TAG, "❌ ERROR CARGANDO BANNER")
                        Log.e(TAG, "Código: ${error.code}")
                        Log.e(TAG, "Mensaje: ${error.message}")
                        Log.e(TAG, "═══════════════════════════════════════")

                        // ✅ MANTENER OCULTO si falla
                        container.visibility = View.GONE
                        container.layoutParams.height = 0

                        handleAdLoadError(error, containerId, activity, container)
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "👁️ Impresión registrada")
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "🖱️ Banner clickeado")
                    }
                }

                val adRequest = AdRequest.Builder().build()
                Log.d(TAG, "📤 Enviando solicitud de banner...")
                adView.loadAd(adRequest)

            } catch (e: Exception) {
                Log.e(TAG, "💥 EXCEPCIÓN CRÍTICA: ${e.message}", e)
                container.visibility = View.GONE
                container.layoutParams.height = 0

                loadingJobs[containerId] = scope.launch {
                    delay(10000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
            }
        }
    }

    private fun handleAdLoadError(
        error: LoadAdError,
        containerId: Int,
        activity: Activity,
        container: ViewGroup
    ) {
        when (error.code) {
            3 -> { // No Fill - común en producción nueva
                val attempts = failedContainers.count { it == containerId }
                if (attempts < 2) {
                    Log.w(TAG, "⚠️ No Fill - Intento ${attempts + 1}/2 en 60s")
                    failedContainers.add(containerId)
                    loadingJobs[containerId] = scope.launch {
                        delay(60000)
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            attachAdaptiveBanner(activity, container)
                        }
                    }
                } else {
                    Log.e(TAG, "❌ No Fill persistente - contenedor permanecerá oculto")
                }
            }

            2 -> { // Network Error
                Log.w(TAG, "⚠️ Error de red - Reintentando en 10s")
                loadingJobs[containerId] = scope.launch {
                    delay(10000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
            }

            1 -> { // Invalid Request
                Log.e(TAG, "❌ Solicitud inválida - Verificar configuración")
                // Dejar oculto permanentemente
            }

            else -> {
                Log.w(TAG, "⚠️ Error desconocido (${error.code}) - Reintento en 30s")
                loadingJobs[containerId] = scope.launch {
                    delay(30000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
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

        Log.d(TAG, "📏 Tamaño calculado:")
        Log.d(TAG, "   Screen: ${outMetrics.widthPixels}px")
        Log.d(TAG, "   Container: ${container.width}px")
        Log.d(TAG, "   Density: $density")
        Log.d(TAG, "   Ad Width: ${adWidth}dp")

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    private fun getAdUnitId(container: ViewGroup): String {
        val adId = when (container.id) {
            R.id.adInProfileContainer -> {
                Log.d(TAG, "📌 Container: BANNER TOP")
                AdIds.bannerTop()
            }
            R.id.bannerBottomContainer -> {
                Log.d(TAG, "📌 Container: BANNER BOTTOM")
                AdIds.bannerBottom()
            }
            else -> {
                Log.w(TAG, "⚠️ Container ID desconocido: ${container.id}")
                AdIds.bannerBottom()
            }
        }
        Log.d(TAG, "📌 Ad Unit ID: $adId")
        return adId
    }

    fun pause(parent: ViewGroup) {
        try {
            loadedBanners.values.forEach { it.pause() }
            Log.d(TAG, "⏸️ ${loadedBanners.size} banners pausados")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error pausando: ${e.message}")
        }
    }

    fun resume(parent: ViewGroup) {
        try {
            loadedBanners.values.forEach { it.resume() }
            Log.d(TAG, "▶️ ${loadedBanners.size} banners resumidos")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resumiendo: ${e.message}")
        }
    }

    fun destroy(parent: ViewGroup) {
        try {
            Log.d(TAG, "🗑️ Destruyendo BannerHelper...")
            loadingJobs.values.forEach { it?.cancel() }
            loadingJobs.clear()
            loadedBanners.values.forEach { it.destroy() }
            loadedBanners.clear()
            failedContainers.clear()
            Log.d(TAG, "✅ BannerHelper destruido")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error destruyendo: ${e.message}")
        }
    }

    fun forceRefresh(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)
        Log.d(TAG, "🔄 Forzando recarga - Container: $containerId")
        failedContainers.removeAll { it == containerId }
        loadedBanners.remove(containerId)?.destroy()
        attachAdaptiveBanner(activity, container)
    }
}