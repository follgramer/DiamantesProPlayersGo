package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowMetrics
import android.widget.FrameLayout
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.*

object BannerHelper {
    private const val TAG = "BannerHelper"
    private val bannersMap = mutableMapOf<Int, AdView>()
    private val loadingStatus = mutableMapOf<Int, Boolean>()
    private val lastRequestTime = mutableMapOf<Int, Long>()
    private const val MIN_REQUEST_INTERVAL = 60000L
    private const val NO_FILL_RETRY_DELAY = 180000L
    private val failureCount = mutableMapOf<Int, Int>()
    private const val MAX_FAILURES = 3
    private val retryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun attachAdaptiveBanner(activity: Activity, container: ViewGroup) {
        try {
            val containerId = container.hashCode()

            container.visibility = View.GONE
            container.removeAllViews()
            container.minimumHeight = 0

            if (!AdsInit.isAdMobReady()) {
                retryScope.launch {
                    delay(5000)
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        attachAdaptiveBanner(activity, container)
                    }
                }
                return
            }

            val currentTime = System.currentTimeMillis()
            val lastTime = lastRequestTime[containerId] ?: 0L

            if (currentTime - lastTime < MIN_REQUEST_INTERVAL) {
                return
            }

            val failures = failureCount[containerId] ?: 0
            if (failures >= MAX_FAILURES) {
                return
            }

            if (loadingStatus[containerId] == true) {
                return
            }

            lastRequestTime[containerId] = currentTime
            loadingStatus[containerId] = true

            val adWidth = getAdaptiveBannerWidth(activity)
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)

            val adView = AdView(activity).apply {
                adUnitId = determineAdUnitId(activity, container)
                setAdSize(adSize)
            }

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    bannersMap[containerId] = adView
                    loadingStatus[containerId] = false
                    failureCount[containerId] = 0

                    activity.runOnUiThread {
                        val density = activity.resources.displayMetrics.density
                        val minHeightPx = (adSize.height * density).toInt()
                        container.minimumHeight = minHeightPx
                        container.visibility = View.VISIBLE
                        container.alpha = 0f
                        container.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)

                    activity.runOnUiThread {
                        container.visibility = View.GONE
                        container.minimumHeight = 0
                        container.removeAllViews()
                    }

                    failureCount[containerId] = (failureCount[containerId] ?: 0) + 1
                    loadingStatus[containerId] = false

                    handleAdError(error, activity, container, containerId)
                }
            }

            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            container.addView(adView, layoutParams)
            container.visibility = View.GONE

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

        } catch (e: Exception) {
            Log.e(TAG, "Error en attachAdaptiveBanner: ${e.message}")
            loadingStatus[container.hashCode()] = false
            container.visibility = View.GONE
            container.minimumHeight = 0
        }
    }

    private fun handleAdError(
        error: LoadAdError,
        activity: Activity,
        container: ViewGroup,
        containerId: Int
    ) {
        // Sin placeholders, solo ocultar el contenedor
        activity.runOnUiThread {
            container.visibility = View.GONE
            container.minimumHeight = 0
            container.removeAllViews()
        }

        val retryDelay = when (error.code) {
            0 -> 300000L  // Error interno: 5 minutos
            1 -> 240000L  // Request inválido: 4 minutos
            2 -> 120000L  // Error de red: 2 minutos
            3 -> NO_FILL_RETRY_DELAY // No fill: 3 minutos
            else -> MIN_REQUEST_INTERVAL
        }

        lastRequestTime[containerId] = System.currentTimeMillis() + retryDelay - MIN_REQUEST_INTERVAL

        retryScope.launch {
            delay(retryDelay)
            if (!activity.isFinishing && !activity.isDestroyed) {
                if (failureCount[containerId] ?: 0 >= MAX_FAILURES) {
                    failureCount[containerId] = 1
                }
                attachAdaptiveBanner(activity, container)
            }
        }
    }

    private fun determineAdUnitId(activity: Activity, container: ViewGroup): String {
        return try {
            val resourceName = getResourceName(activity, container.id)
            when {
                resourceName.contains("bannerBottomContainer") -> currentBannerBottomUnitId()
                resourceName.contains("adInProfileContainer") -> currentBannerTopUnitId()
                else -> currentBannerTopUnitId()
            }
        } catch (e: Exception) {
            currentBannerTopUnitId()
        }
    }

    private fun getResourceName(activity: Activity, id: Int): String {
        return try {
            if (id == View.NO_ID) "NO_ID"
            else activity.resources.getResourceEntryName(id)
        } catch (e: Exception) {
            "UNKNOWN_$id"
        }
    }

    private fun getAdaptiveBannerWidth(activity: Activity): Int {
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            displayMetrics.widthPixels = bounds.width()
            displayMetrics.density = activity.resources.displayMetrics.density
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay?.getMetrics(displayMetrics)
        }

        val density = displayMetrics.density
        val adWidthPixels = displayMetrics.widthPixels.toFloat()
        return (adWidthPixels / density).toInt()
    }

    fun pauseBanners(container: ViewGroup) {
        try {
            bannersMap[container.hashCode()]?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausando banner: ${e.message}")
        }
    }

    fun resumeBanners(container: ViewGroup) {
        try {
            bannersMap[container.hashCode()]?.resume()
        } catch (e: Exception) {
            Log.e(TAG, "Error resumiendo banner: ${e.message}")
        }
    }

    fun destroyBanners(container: ViewGroup) {
        try {
            val containerId = container.hashCode()
            bannersMap[containerId]?.destroy()
            bannersMap.remove(containerId)
            loadingStatus.remove(containerId)
            lastRequestTime.remove(containerId)
            failureCount.remove(containerId)
            container.removeAllViews()
            container.visibility = View.GONE
            container.minimumHeight = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error destruyendo banner: ${e.message}")
        }
    }

    fun cleanup() {
        try {
            retryScope.cancel()
            bannersMap.values.forEach { it.destroy() }
            bannersMap.clear()
            loadingStatus.clear()
            lastRequestTime.clear()
            failureCount.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}")
        }
    }
}