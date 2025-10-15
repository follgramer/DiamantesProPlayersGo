package com.follgramer.diamantesproplayersgo.ads

import com.follgramer.diamantesproplayersgo.R
import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.*

object NativeAdHelper {
    private const val TAG = "NativeAdHelper"
    private const val MAX_NATIVE_ADS = 2
    private const val MIN_REQUEST_INTERVAL_MS = 15000L

    private val loadedAds = mutableMapOf<Int, NativeAd>()
    private val loadingStates = mutableMapOf<Int, Boolean>()
    private val retryJobs = mutableMapOf<Int, Job>()
    private val failCounts = mutableMapOf<Int, Int>()
    private val noFillHolders = mutableSetOf<Int>()
    private val lastRequestTime = mutableMapOf<Int, Long>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun loadNativeAd(
        activity: Activity,
        container: ViewGroup,
        nativeAdView: NativeAdView,
        holderId: Int
    ) {
        try {
            Log.d(TAG, "🎯 loadNativeAd llamado para holder $holderId")

            // Limpiar contenedor
            container.apply {
                visibility = View.GONE
                layoutParams = layoutParams?.apply {
                    height = 0
                } ?: ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0
                )
                removeAllViews()
            }

            val now = System.currentTimeMillis()
            val lastRequest = lastRequestTime[holderId] ?: 0L
            val timeSinceLastRequest = now - lastRequest

            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                val waitTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest
                Log.d(TAG, "⏳ Esperando ${waitTime}ms antes de cargar holder $holderId")
                scope.launch {
                    delay(waitTime)
                    loadNativeAd(activity, container, nativeAdView, holderId)
                }
                return
            }

            if (loadingStates[holderId] == true) {
                Log.d(TAG, "⚠️ Ya cargando anuncio nativo para holder $holderId")
                return
            }

            val currentFailCount = failCounts[holderId] ?: 0
            if (currentFailCount >= 3) {
                if (noFillHolders.contains(holderId)) {
                    Log.d(TAG, "🚫 Holder $holderId en cooldown extendido")
                    return
                }

                noFillHolders.add(holderId)
                scope.launch {
                    delay(300000)
                    noFillHolders.remove(holderId)
                    failCounts[holderId] = 0
                }
                return
            }

            loadedAds[holderId]?.let { existingAd ->
                Log.d(TAG, "♻️ Reutilizando anuncio existente para holder $holderId")
                (nativeAdView.parent as? ViewGroup)?.removeView(nativeAdView)

                populateNativeAdView(existingAd, nativeAdView)
                container.removeAllViews()
                container.addView(nativeAdView)
                showContainerWithAnimation(container)
                return
            }

            val activeAds = loadingStates.count { it.value }
            if (activeAds >= MAX_NATIVE_ADS) {
                Log.d(TAG, "⚠️ Demasiados anuncios nativos activos ($activeAds)")
                return
            }

            loadingStates[holderId] = true
            lastRequestTime[holderId] = now

            val adUnitId = AdIds.native()

            Log.d(TAG, "📢 Iniciando carga de anuncio nativo para holder $holderId")
            Log.d(TAG, "🔑 Ad Unit ID: $adUnitId")

            val adLoader = AdLoader.Builder(activity, adUnitId)
                .forNativeAd { nativeAd ->
                    Log.d(TAG, "✅ Anuncio nativo cargado para holder $holderId")

                    loadedAds[holderId]?.destroy()
                    loadedAds[holderId] = nativeAd
                    loadingStates[holderId] = false
                    noFillHolders.remove(holderId)
                    failCounts[holderId] = 0

                    retryJobs[holderId]?.cancel()
                    retryJobs.remove(holderId)

                    (nativeAdView.parent as? ViewGroup)?.removeView(nativeAdView)

                    populateNativeAdView(nativeAd, nativeAdView)
                    container.removeAllViews()
                    container.addView(nativeAdView)
                    showContainerWithAnimation(container)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "❌ Error cargando anuncio nativo holder $holderId")
                        Log.e(TAG, "   Mensaje: ${error.message}")
                        Log.e(TAG, "   Código: ${error.code}")
                        Log.e(TAG, "   Domain: ${error.domain}")
                        Log.e(TAG, "   Cause: ${error.cause}")

                        loadingStates[holderId] = false

                        failCounts[holderId] = (failCounts[holderId] ?: 0) + 1

                        container.apply {
                            visibility = View.GONE
                            layoutParams?.height = 0
                            removeAllViews()
                        }

                        when (error.code) {
                            3 -> {
                                Log.w(TAG, "⚠️ NO FILL - No hay anuncios disponibles")
                                if (failCounts[holderId] ?: 0 >= 2) {
                                    noFillHolders.add(holderId)
                                    scope.launch {
                                        delay(120000)
                                        noFillHolders.remove(holderId)
                                        failCounts[holderId] = 0
                                    }
                                } else {
                                    scheduleRetry(activity, container, nativeAdView, holderId, 30000)
                                }
                            }
                            1 -> {
                                Log.e(TAG, "⚠️ INVALID REQUEST - Revisa la configuración")
                                scheduleRetry(activity, container, nativeAdView, holderId, 15000)
                            }
                            2 -> {
                                Log.e(TAG, "⚠️ NETWORK ERROR - Problema de conexión")
                                scheduleRetry(activity, container, nativeAdView, holderId, 10000)
                            }
                            else -> {
                                Log.e(TAG, "⚠️ ERROR DESCONOCIDO - Código ${error.code}")
                                scheduleRetry(activity, container, nativeAdView, holderId, 60000)
                            }
                        }
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "👆 Anuncio nativo clickeado - holder $holderId")
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "👁️ Impresión registrada - holder $holderId")
                    }
                })
                .build()

            AdRequestManager.queueRequest {
                withContext(Dispatchers.Main) {
                    try {
                        val adRequest = AdRequest.Builder().build()
                        adLoader.loadAd(adRequest)
                        Log.d(TAG, "📡 Request enviada para holder $holderId")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error en loadAd: ${e.message}", e)
                        loadingStates[holderId] = false
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error crítico: ${e.message}", e)
            e.printStackTrace()

            container.apply {
                visibility = View.GONE
                layoutParams?.height = 0
                removeAllViews()
            }

            loadingStates[holderId] = false
            scheduleRetry(activity, container, nativeAdView, holderId, 60000)
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        Log.d(TAG, "🎨 Poblando NativeAdView...")

        try {
            nativeAdView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
                if (nativeAd.icon != null) {
                    it.setImageDrawable(nativeAd.icon?.drawable)
                    it.visibility = View.VISIBLE
                    Log.d(TAG, "  ✅ Icon configurado")
                } else {
                    it.visibility = View.GONE
                }
                nativeAdView.iconView = it
            }

            nativeAdView.findViewById<TextView>(R.id.ad_headline)?.let {
                it.text = nativeAd.headline
                nativeAdView.headlineView = it
                Log.d(TAG, "  ✅ Headline: ${nativeAd.headline}")
            }

            nativeAdView.findViewById<TextView>(R.id.ad_body)?.let {
                if (nativeAd.body != null) {
                    it.text = nativeAd.body
                    it.visibility = View.VISIBLE
                } else {
                    it.visibility = View.GONE
                }
                nativeAdView.bodyView = it
            }

            nativeAdView.findViewById<MediaView>(R.id.ad_media)?.let {
                if (nativeAd.mediaContent != null) {
                    it.setMediaContent(nativeAd.mediaContent!!)
                    it.visibility = View.VISIBLE
                    Log.d(TAG, "  ✅ Media configurado")
                } else {
                    it.visibility = View.GONE
                }
                nativeAdView.mediaView = it
            }

            nativeAdView.findViewById<Button>(R.id.ad_call_to_action)?.let {
                if (nativeAd.callToAction != null) {
                    it.text = nativeAd.callToAction
                    it.visibility = View.VISIBLE
                    Log.d(TAG, "  ✅ CTA: ${nativeAd.callToAction}")
                } else {
                    it.visibility = View.GONE
                }
                nativeAdView.callToActionView = it
            }

            nativeAdView.findViewById<RatingBar>(R.id.ad_stars)?.let {
                if (nativeAd.starRating != null && nativeAd.starRating!! > 0) {
                    it.rating = nativeAd.starRating!!.toFloat()
                    it.visibility = View.VISIBLE
                } else {
                    it.visibility = View.GONE
                }
                nativeAdView.starRatingView = it
            }

            nativeAdView.findViewById<TextView>(R.id.ad_advertiser)?.let {
                if (nativeAd.advertiser != null) {
                    it.text = nativeAd.advertiser
                    it.visibility = View.VISIBLE
                } else {
                    it.visibility = View.GONE
                }
                nativeAdView.advertiserView = it
            }

            nativeAdView.setNativeAd(nativeAd)
            Log.d(TAG, "✅ NativeAdView completamente configurado")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error poblando NativeAdView: ${e.message}", e)
        }
    }

    private fun showContainerWithAnimation(container: ViewGroup) {
        Log.d(TAG, "🎬 Mostrando contenedor con animación...")

        container.apply {
            layoutParams = layoutParams?.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            visibility = View.VISIBLE

            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction {
                    Log.d(TAG, "✅ Animación completada")
                }
                .start()
        }
    }

    private fun scheduleRetry(
        activity: Activity,
        container: ViewGroup,
        nativeAdView: NativeAdView,
        holderId: Int,
        delayMs: Long
    ) {
        retryJobs[holderId]?.cancel()

        Log.d(TAG, "🔄 Programando reintento para holder $holderId en ${delayMs}ms")

        retryJobs[holderId] = scope.launch {
            delay(delayMs)
            if (!activity.isFinishing && !activity.isDestroyed) {
                Log.d(TAG, "🔄 Reintentando carga para holder $holderId")
                loadNativeAd(activity, container, nativeAdView, holderId)
            }
        }
    }

    fun destroyNativeAd(holderId: Int) {
        try {
            retryJobs[holderId]?.cancel()
            retryJobs.remove(holderId)

            loadedAds[holderId]?.destroy()
            loadedAds.remove(holderId)

            loadingStates.remove(holderId)
            noFillHolders.remove(holderId)
            failCounts.remove(holderId)
            lastRequestTime.remove(holderId)

            Log.d(TAG, "🗑️ Anuncio nativo destruido para holder $holderId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error destruyendo anuncio nativo: ${e.message}")
        }
    }

    fun cleanup() {
        try {
            scope.cancel()

            retryJobs.values.forEach { it.cancel() }
            retryJobs.clear()

            loadedAds.values.forEach { it.destroy() }
            loadedAds.clear()

            loadingStates.clear()
            noFillHolders.clear()
            failCounts.clear()
            lastRequestTime.clear()

            Log.d(TAG, "🧹 NativeAdHelper limpiado completamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en cleanup: ${e.message}")
        }
    }

    fun getStats(): String {
        return """
            NativeAdHelper Stats:
            - Anuncios cargados: ${loadedAds.size}
            - Cargando: ${loadingStates.count { it.value }}
            - En cooldown: ${noFillHolders.size}
            - Reintentos programados: ${retryJobs.size}
            - Holders con fallos: ${failCounts.filter { it.value > 0 }.size}
        """.trimIndent()
    }
}