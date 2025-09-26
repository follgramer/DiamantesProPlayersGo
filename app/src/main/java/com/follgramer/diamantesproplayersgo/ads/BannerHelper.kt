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

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        val containerId = System.identityHashCode(container)

        // Cancelar cualquier carga previa
        loadingJobs[containerId]?.cancel()

        // Verificar consentimiento
        if (!UserMessagingPlatform.getConsentInformation(activity).canRequestAds()) {
            Log.d(TAG, "No se puede solicitar anuncios sin consentimiento")
            container.visibility = View.GONE

            // Reintentar después de 3 segundos
            loadingJobs[containerId] = scope.launch {
                delay(3000)
                if (!activity.isFinishing && !activity.isDestroyed) {
                    attachAdaptiveBanner(activity, container)
                }
            }
            return
        }

        // Verificar si ya existe un banner cargado
        loadedBanners[containerId]?.let { existingAd ->
            if (existingAd.parent == null) {
                container.removeAllViews()
                container.addView(existingAd)
                container.visibility = View.VISIBLE
                Log.d(TAG, "Reutilizando banner existente")
                return
            }
        }

        // Crear y cargar nuevo banner
        loadingJobs[containerId] = scope.launch {
            try {
                container.removeAllViews()
                container.visibility = View.VISIBLE // Mantener visible para que tenga dimensiones

                val adView = AdView(activity).apply {
                    adUnitId = getAdUnitId(container)
                    setAdSize(getAdaptiveAdSize(activity, container))
                }

                loadedBanners[containerId] = adView
                container.addView(adView)

                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d(TAG, "✅ Banner cargado exitosamente")
                        container.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "❌ Error cargando banner: ${error.message}")
                        container.visibility = View.GONE

                        // Reintentar en 30 segundos si no es No Fill
                        if (error.code != AdRequest.ERROR_CODE_NO_FILL) {
                            loadingJobs[containerId] = scope.launch {
                                delay(30000)
                                if (!activity.isFinishing) {
                                    attachAdaptiveBanner(activity, container)
                                }
                            }
                        }
                    }
                }

                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)

            } catch (e: Exception) {
                Log.e(TAG, "Error crítico: ${e.message}")
                container.visibility = View.GONE
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
        loadingJobs.values.forEach { it?.cancel() }
        loadingJobs.clear()
        loadedBanners.values.forEach { it.destroy() }
        loadedBanners.clear()
    }
}