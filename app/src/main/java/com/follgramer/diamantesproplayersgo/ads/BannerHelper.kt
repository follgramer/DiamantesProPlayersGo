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
    private val visibilityJobs = mutableMapOf<Int, Job>()

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)

        Log.d(TAG, "╔═══════════════════════════════════════╗")
        Log.d(TAG, "🎯 attachAdaptiveBanner INICIADO")
        Log.d(TAG, "Container ID: $containerId")

        // ✅ EMPEZAR OCULTO
        container.visibility = View.GONE
        container.layoutParams = container.layoutParams.apply {
            height = 0
        }

        // Cancelar trabajos previos
        loadingJobs[containerId]?.cancel()
        visibilityJobs[containerId]?.cancel()

        // Verificar consentimiento
        val canRequestAds = try {
            val result = UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
            Log.d(TAG, "📋 Consentimiento: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando consentimiento: ${e.message}")
            false
        }

        if (!canRequestAds) {
            Log.w(TAG, "⚠️ Sin consentimiento")
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

        Log.d(TAG, "✅ AdMob listo")

        // Reutilizar banner existente
        loadedBanners[containerId]?.let { existingAd ->
            if (existingAd.parent == null) {
                Log.d(TAG, "♻️ Reutilizando banner")
                container.removeAllViews()
                container.addView(existingAd)
                container.visibility = View.VISIBLE
                container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                ensureBannerStaysVisible(containerId, container, existingAd)
                return
            }
        }

        // Crear nuevo banner
        loadingJobs[containerId] = scope.launch {
            try {
                Log.d(TAG, "🚀 Creando nuevo banner...")
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

                Log.d(TAG, "✅ AdView agregado")

                // Configurar listener
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "╔═══════════════════════════════════════╗")
                        Log.d(TAG, "✅ ¡BANNER CARGADO!")
                        Log.d(TAG, "╚═══════════════════════════════════════╝")

                        // ✅ MOSTRAR
                        container.visibility = View.VISIBLE
                        container.layoutParams = container.layoutParams.apply {
                            height = ViewGroup.LayoutParams.WRAP_CONTENT
                        }

                        adView.requestLayout()
                        container.requestLayout()

                        failedContainers.removeAll { it == containerId }

                        // ✅ MANTENER VISIBLE
                        ensureBannerStaysVisible(containerId, container, adView)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "╔═══════════════════════════════════════╗")
                        Log.e(TAG, "❌ ERROR CARGANDO BANNER")
                        Log.e(TAG, "Código: ${error.code}")
                        Log.e(TAG, "Mensaje: ${error.message}")
                        Log.e(TAG, "╚═══════════════════════════════════════╝")

                        container.visibility = View.GONE
                        container.layoutParams.height = 0

                        handleAdLoadError(error, containerId, activity, container)
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "👁️ Impresión")
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "🖱️ Click")
                    }
                }

                val adRequest = AdRequest.Builder().build()
                Log.d(TAG, "📤 Enviando solicitud...")
                adView.loadAd(adRequest)

            } catch (e: Exception) {
                Log.e(TAG, "💥 EXCEPCIÓN: ${e.message}", e)
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

    // ✅ NUEVA FUNCIÓN PARA MANTENER BANNERS VISIBLES
    private fun ensureBannerStaysVisible(containerId: Int, container: ViewGroup, adView: AdView) {
        visibilityJobs[containerId]?.cancel()

        visibilityJobs[containerId] = scope.launch {
            while (isActive) {
                delay(5000)

                try {
                    if (container.visibility != View.VISIBLE && adView.parent != null) {
                        Log.w(TAG, "⚠️ Banner $containerId se ocultó, restaurando...")

                        withContext(Dispatchers.Main) {
                            container.visibility = View.VISIBLE
                            container.layoutParams = container.layoutParams?.apply {
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                            container.requestLayout()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en visibility check: ${e.message}")
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
            3 -> {
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
                    Log.e(TAG, "❌ No Fill persistente")
                }
            }

            2 -> {
                Log.w(TAG, "⚠️ Error de red - Reintentando en 10s")
                loadingJobs[containerId] = scope.launch {
                    delay(10000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
            }

            1 -> {
                Log.e(TAG, "❌ Solicitud inválida")
            }

            else -> {
                Log.w(TAG, "⚠️ Error ${error.code} - Reintento en 30s")
                loadingJobs[containerId] = scope.launch {
                    delay(30000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
            }
        }
    }

    // ✅ FUNCIÓN CORREGIDA PARA CALCULAR TAMAÑO
    private fun getAdaptiveAdSize(activity: Activity, container: ViewGroup): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        // ✅ USAR ANCHO DE PANTALLA
        val adWidthPixels = outMetrics.widthPixels.toFloat()

        // ✅ RESTAR MÁRGENES (24dp total)
        val marginsDp = 24
        val marginsPixels = (marginsDp * density).toInt()

        val adWidth = ((adWidthPixels - marginsPixels) / density).toInt()

        Log.d(TAG, "📏 Tamaño calculado:")
        Log.d(TAG, "   Screen: ${outMetrics.widthPixels}px")
        Log.d(TAG, "   Density: $density")
        Log.d(TAG, "   Margins: ${marginsPixels}px")
        Log.d(TAG, "   Ad Width: ${adWidth}dp")

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    private fun getAdUnitId(container: ViewGroup): String {
        val adId = when (container.id) {
            R.id.home_banner_container -> {
                Log.d(TAG, "📌 Container: BANNER TOP HOME")
                AdIds.bannerTop()
            }
            R.id.bottom_banner_container -> {
                Log.d(TAG, "📌 Container: BANNER BOTTOM")
                AdIds.bannerBottom()
            }
            else -> {
                Log.w(TAG, "⚠️ Container desconocido: ${container.id}")
                AdIds.bannerBottom()
            }
        }
        return adId
    }

    fun pause(parent: ViewGroup) {
        try {
            loadedBanners.values.forEach { it.pause() }
            Log.d(TAG, "⏸️ Banners pausados")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausando: ${e.message}")
        }
    }

    fun resume(parent: ViewGroup) {
        try {
            loadedBanners.values.forEach { it.resume() }
            Log.d(TAG, "▶️ Banners resumidos")
        } catch (e: Exception) {
            Log.e(TAG, "Error resumiendo: ${e.message}")
        }
    }

    fun destroy(parent: ViewGroup) {
        try {
            Log.d(TAG, "🗑️ Destruyendo BannerHelper...")
            visibilityJobs.values.forEach { it.cancel() }
            visibilityJobs.clear()
            loadingJobs.values.forEach { it?.cancel() }
            loadingJobs.clear()
            loadedBanners.values.forEach { it.destroy() }
            loadedBanners.clear()
            failedContainers.clear()
            Log.d(TAG, "✅ BannerHelper destruido")
        } catch (e: Exception) {
            Log.e(TAG, "Error destruyendo: ${e.message}")
        }
    }

    fun forceRefresh(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)
        Log.d(TAG, "🔄 Forzando recarga - Container: $containerId")
        visibilityJobs[containerId]?.cancel()
        failedContainers.removeAll { it == containerId }
        loadedBanners.remove(containerId)?.destroy()
        attachAdaptiveBanner(activity, container)
    }
}