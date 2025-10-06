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
    private const val MAX_NATIVE_ADS = 3
    private const val MIN_REQUEST_INTERVAL_MS = 3000L

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
                Log.d(TAG, "Esperando ${waitTime}ms antes de cargar holder $holderId")
                scope.launch {
                    delay(waitTime)
                    loadNativeAd(activity, container, nativeAdView, holderId)
                }
                return
            }

            if (loadingStates[holderId] == true) {
                Log.d(TAG, "Ya cargando anuncio nativo para holder $holderId")
                return
            }

            val currentFailCount = failCounts[holderId] ?: 0
            if (currentFailCount >= 3) {
                if (noFillHolders.contains(holderId)) {
                    Log.d(TAG, "Holder $holderId en cooldown extendido")
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
                // CRÍTICO: Remover del parent anterior
                (nativeAdView.parent as? ViewGroup)?.removeView(nativeAdView)

                populateNativeAdView(existingAd, nativeAdView)
                container.removeAllViews()
                container.addView(nativeAdView)
                showContainerWithAnimation(container)
                Log.d(TAG, "Reutilizando anuncio nativo existente para holder $holderId")
                return
            }

            val activeAds = loadingStates.count { it.value }
            if (activeAds >= MAX_NATIVE_ADS) {
                Log.d(TAG, "Demasiados anuncios nativos activos ($activeAds)")
                return
            }

            loadingStates[holderId] = true
            lastRequestTime[holderId] = now

            val adUnitId = AdIds.native()

            Log.d(TAG, "Encolando anuncio nativo para holder $holderId con ID: $adUnitId")

            val adLoader = AdLoader.Builder(activity, adUnitId)
                .forNativeAd { nativeAd ->
                    Log.d(TAG, "Anuncio nativo cargado para holder $holderId")

                    loadedAds[holderId]?.destroy()
                    loadedAds[holderId] = nativeAd
                    loadingStates[holderId] = false
                    noFillHolders.remove(holderId)
                    failCounts[holderId] = 0

                    retryJobs[holderId]?.cancel()
                    retryJobs.remove(holderId)

                    // CRÍTICO: Remover del parent anterior si existe
                    (nativeAdView.parent as? ViewGroup)?.removeView(nativeAdView)

                    populateNativeAdView(nativeAd, nativeAdView)
                    container.removeAllViews()
                    container.addView(nativeAdView)
                    showContainerWithAnimation(container)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Error cargando anuncio nativo: ${error.message}, código: ${error.code}")
                        loadingStates[holderId] = false

                        failCounts[holderId] = (failCounts[holderId] ?: 0) + 1

                        container.apply {
                            visibility = View.GONE
                            layoutParams?.height = 0
                            removeAllViews()
                        }

                        when (error.code) {
                            3 -> { // No Fill
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
                            1 -> { // Invalid Request - Rate Limiting
                                Log.e(TAG, "Rate limiting detectado, esperando 10 segundos")
                                scheduleRetry(activity, container, nativeAdView, holderId, 10000)
                            }
                            2 -> { // Network Error
                                scheduleRetry(activity, container, nativeAdView, holderId, 10000)
                            }
                            else -> {
                                scheduleRetry(activity, container, nativeAdView, holderId, 60000)
                            }
                        }
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Anuncio nativo clickeado - holder $holderId")
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "Impresión registrada - holder $holderId")
                    }
                })
                .build()

            AdRequestManager.queueRequest {
                withContext(Dispatchers.Main) {
                    try {
                        val adRequest = AdRequest.Builder().build()
                        adLoader.loadAd(adRequest)
                        Log.d(TAG, "Request de anuncio nativo ejecutada para holder $holderId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en loadAd: ${e.message}")
                        loadingStates[holderId] = false
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error crítico: ${e.message}", e)

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
        nativeAdView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
            if (nativeAd.icon != null) {
                it.setImageDrawable(nativeAd.icon?.drawable)
                it.visibility = View.VISIBLE
            } else {
                it.visibility = View.GONE
            }
            nativeAdView.iconView = it
        }

        nativeAdView.findViewById<TextView>(R.id.ad_headline)?.let {
            it.text = nativeAd.headline
            nativeAdView.headlineView = it
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
            } else {
                it.visibility = View.GONE
            }
            nativeAdView.mediaView = it
        }

        nativeAdView.findViewById<Button>(R.id.ad_call_to_action)?.let {
            if (nativeAd.callToAction != null) {
                it.text = nativeAd.callToAction
                it.visibility = View.VISIBLE
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
    }

    private fun showContainerWithAnimation(container: ViewGroup) {
        container.apply {
            layoutParams = layoutParams?.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            visibility = View.VISIBLE

            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(300)
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

        retryJobs[holderId] = scope.launch {
            delay(delayMs)
            if (!activity.isFinishing && !activity.isDestroyed) {
                Log.d(TAG, "Reintentando carga de anuncio nativo para holder $holderId")
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

            Log.d(TAG, "Anuncio nativo destruido para holder $holderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error destruyendo anuncio nativo: ${e.message}")
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

            Log.d(TAG, "NativeAdHelper limpiado completamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}")
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