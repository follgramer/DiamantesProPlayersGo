package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.graphics.Color
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

        // Verificar si ya falló antes (reducir el tiempo de cooldown)
        if (failedContainers.contains(containerId)) {
            Log.d(TAG, "Container $containerId en cooldown temporal")
            // Reducir el cooldown a 30 segundos en lugar de permanente
            scope.launch {
                delay(30000)
                failedContainers.remove(containerId)
                // Reintentar después del cooldown
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Verificar consentimiento
        if (!UserMessagingPlatform.getConsentInformation(activity).canRequestAds()) {
            Log.d(TAG, "⏸️ Esperando consentimiento para container $containerId")
            // NO ocultar el contenedor, mantenerlo visible para futuros intentos
            container.visibility = View.VISIBLE
            container.layoutParams.height = 1 // Altura mínima

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
            container.visibility = View.VISIBLE
            container.layoutParams.height = 1 // Altura mínima

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

                // IMPORTANTE: Mantener visible con altura mínima mientras carga
                container.visibility = View.VISIBLE
                container.layoutParams.height = 50.dpToPx(activity) // Altura mínima de 50dp
                container.setBackgroundColor(Color.parseColor("#1A1A1A1A")) // Fondo semi-transparente temporal

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

                        // Ajustar altura al contenido real
                        container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        container.setBackgroundColor(Color.TRANSPARENT)
                        container.visibility = View.VISIBLE

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

                        // En caso de error, mantener una altura mínima para futuros intentos
                        container.visibility = View.VISIBLE
                        container.layoutParams.height = 1
                        container.setBackgroundColor(Color.TRANSPARENT)

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
                                // Error de red, reintentar más rápido
                                loadingJobs[containerId] = scope.launch {
                                    delay(5000) // 5 segundos
                                    if (!activity.isFinishing) {
                                        attachAdaptiveBanner(activity, container)
                                    }
                                }
                            }
                            else -> {
                                // Otros errores, reintentar en 15 segundos
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
                container.visibility = View.VISIBLE
                container.layoutParams.height = 1
                container.setBackgroundColor(Color.TRANSPARENT)

                // Reintentar después de un error crítico
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
            R.id.adInProfileContainer -> AdIds.bannerTop()
            R.id.bannerBottomContainer -> AdIds.bannerBottom()
            else -> AdIds.bannerBottom()
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

    // Función de extensión auxiliar para convertir dp a pixels
    private fun Int.dpToPx(activity: Activity): Int {
        return (this * activity.resources.displayMetrics.density).toInt()
    }
}