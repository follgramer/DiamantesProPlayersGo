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
    private val containerStates = mutableMapOf<Int, ContainerState>()

    private data class ContainerState(
        var isLoading: Boolean = false,
        var hasError: Boolean = false,
        var lastAttempt: Long = 0L
    )

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)
        val state = containerStates.getOrPut(containerId) { ContainerState() }

        // Evitar múltiples intentos simultáneos
        if (state.isLoading) {
            Log.d(TAG, "⏳ Ya cargando para container $containerId")
            return
        }

        // Limitar reintentos (máximo 1 cada 30 segundos)
        val now = System.currentTimeMillis()
        if (state.hasError && (now - state.lastAttempt) < 30000) {
            Log.d(TAG, "⏰ Esperando cooldown para container $containerId")
            return
        }

        // Verificar consentimiento
        if (!UserMessagingPlatform.getConsentInformation(activity).canRequestAds()) {
            Log.d(TAG, "🚫 Sin consentimiento para ads")
            hideContainer(container)
            // Reintentar en 3 segundos
            scope.launch {
                delay(3000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Verificar AdMob - LÍNEA 58 CORREGIDA
        if (!AdsInit.isAdMobReady()) {
            Log.d(TAG, "⸏ AdMob no está listo")
            hideContainer(container)
            // Reintentar en 2 segundos
            scope.launch {
                delay(2000)
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
                showContainer(container)
                Log.d(TAG, "♻️ Reutilizando banner para container $containerId")
                return
            }
        }

        // Marcar como cargando
        state.isLoading = true
        state.lastAttempt = now

        // Crear nuevo banner
        createAndLoadBanner(activity, container, containerId, state)
    }

    private fun createAndLoadBanner(
        activity: Activity,
        container: ViewGroup,
        containerId: Int,
        state: ContainerState
    ) {
        try {
            container.removeAllViews()

            // Mantener container visible pero sin contenido mientras carga
            container.visibility = View.VISIBLE
            container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

            val adView = AdView(activity).apply {
                adUnitId = getAdUnitId(container)
                setAdSize(getAdaptiveAdSize(activity, container))
            }

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner cargado para container $containerId")
                    state.isLoading = false
                    state.hasError = false
                    loadedBanners[containerId] = adView

                    // Asegurar que el container sea visible
                    showContainer(container)

                    // Animación suave de entrada
                    container.alpha = 0f
                    container.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error cargando banner: ${error.message} (código: ${error.code})")
                    state.isLoading = false
                    state.hasError = true

                    // Ocultar container si falla
                    hideContainer(container)

                    // Si es error de no fill (3), esperar más tiempo
                    val retryDelay = if (error.code == 3) 60000L else 30000L

                    scope.launch {
                        delay(retryDelay)
                        if (!activity.isFinishing) {
                            attachAdaptiveBanner(activity, container)
                        }
                    }
                }

                override fun onAdClicked() {
                    Log.d(TAG, "🖱️ Banner clickeado")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Impresión de banner")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "📱 Banner abierto")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "❌ Banner cerrado")
                }
            }

            // Agregar AdView al container
            container.addView(adView)

            // Cargar el anuncio
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            Log.d(TAG, "📤 Solicitando banner para container $containerId")

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error crítico: ${e.message}")
            state.isLoading = false
            state.hasError = true
            hideContainer(container)
        }
    }

    private fun showContainer(container: ViewGroup) {
        container.visibility = View.VISIBLE
        container.layoutParams = container.layoutParams.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    private fun hideContainer(container: ViewGroup) {
        container.visibility = View.GONE
        container.layoutParams = container.layoutParams.apply {
            height = 0
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
        return when (container.id) {
            R.id.adInProfileContainer -> AdIds.bannerTop()
            R.id.bannerBottomContainer -> AdIds.bannerBottom()
            else -> AdIds.bannerBottom()
        }
    }

    fun pause(parent: ViewGroup) {
        loadedBanners.values.forEach { it.pause() }
    }

    fun resume(parent: ViewGroup) {
        loadedBanners.values.forEach { it.resume() }
    }

    fun destroy(parent: ViewGroup) {
        scope.cancel()
        loadedBanners.values.forEach { it.destroy() }
        loadedBanners.clear()
        containerStates.clear()
    }

    fun forceRefresh(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)
        containerStates.remove(containerId)
        loadedBanners.remove(containerId)?.destroy()
        attachAdaptiveBanner(activity, container)
    }
}