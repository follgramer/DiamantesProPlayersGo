package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewCompat
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null

    private val isLoadingInterstitial = AtomicBoolean(false)
    private val isLoadingRewarded = AtomicBoolean(false)
    private val isWarmingUp = AtomicBoolean(false)

    private val mainHandler = Handler(Looper.getMainLooper())
    private const val TAG = "AdManager"
    private const val RETRY_DELAY_MS = 8000L
    private const val MAX_RETRY_ATTEMPTS = 2

    private var interstitialRetryCount = 0
    private var rewardedRetryCount = 0

    fun warmUp(context: Context) {
        if (isWarmingUp.getAndSet(true)) {
            Log.d(TAG, "Ya está ejecutando warmUp")
            return
        }

        Log.d(TAG, "Iniciando warmUp de anuncios")

        if (!AdsInit.isInitialized()) {
            Log.w(TAG, "AdMob no inicializado, postponiendo warmUp")
            isWarmingUp.set(false)
            return
        }

        configureWebViewForAds(context)

        mainHandler.postDelayed({
            if (AdsInit.isAdMobReady()) {
                loadInterstitialIfNeeded(context)
                loadRewardedIfNeeded(context)
            } else {
                Log.w(TAG, "AdMob aún no está listo durante warmUp")
            }
        }, 1000)

        mainHandler.postDelayed({
            isWarmingUp.set(false)
        }, 5000)
    }

    private fun configureWebViewForAds(context: Context) {
        try {
            val webViewPackage = WebViewCompat.getCurrentWebViewPackage(context)
            if (webViewPackage == null) {
                Log.w(TAG, "WebView no disponible en este dispositivo")
                return
            }

            Log.d(TAG, "WebView package: ${webViewPackage.packageName} v${webViewPackage.versionName}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            }

            val webView = try {
                WebView(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error creando WebView: ${e.message}")
                return
            }

            val settings = webView.settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT

                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false

                try {
                    val originalUA = userAgentString
                    userAgentString = "$originalUA AdMobApp/1.0 (${context.packageName})"
                } catch (e: Exception) {
                    Log.w(TAG, "Error configurando User Agent: ${e.message}")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        safeBrowsingEnabled = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Safe browsing no soportado: ${e.message}")
                    }
                }

                try {
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                } catch (e: Exception) {
                    Log.w(TAG, "Error configurando mixed content: ${e.message}")
                }

                setSupportMultipleWindows(false)
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
            }

            try {
                webView.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destruyendo WebView de configuración: ${e.message}")
            }

            Log.d(TAG, "WebView configurado para anuncios")

        } catch (e: Exception) {
            Log.e(TAG, "Error general configurando WebView: ${e.message}")
        }
    }

    private fun loadInterstitialIfNeeded(context: Context) {
        if (interstitial != null || isLoadingInterstitial.getAndSet(true)) {
            Log.d(TAG, "Interstitial ya está cargado o cargando")
            return
        }

        if (interstitialRetryCount >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Máximo de intentos alcanzado para interstitial")
            isLoadingInterstitial.set(false)
            return
        }

        val adRequest = createOptimizedAdRequest()

        Log.d(TAG, "Cargando interstitial...")

        InterstitialAd.load(
            context,
            currentInterstitialUnitId(),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoadingInterstitial.set(false)
                    interstitialRetryCount = 0
                    interstitial = ad

                    interstitial?.fullScreenContentCallback = createFullScreenCallback("interstitial", context)
                    Log.d(TAG, "Interstitial cargado exitosamente")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingInterstitial.set(false)
                    interstitial = null
                    interstitialRetryCount++

                    Log.w(TAG, "Error cargando interstitial: ${loadAdError.code} - ${loadAdError.message}")
                    Log.w(TAG, "Intento $interstitialRetryCount/$MAX_RETRY_ATTEMPTS")

                    if (interstitialRetryCount < MAX_RETRY_ATTEMPTS) {
                        scheduleRetry { loadInterstitialIfNeeded(context) }
                    }
                }
            }
        )
    }

    private fun loadRewardedIfNeeded(context: Context) {
        if (rewarded != null || isLoadingRewarded.getAndSet(true)) {
            Log.d(TAG, "Rewarded ya está cargado o cargando")
            return
        }

        if (rewardedRetryCount >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Máximo de intentos alcanzado para rewarded")
            isLoadingRewarded.set(false)
            return
        }

        val adRequest = createOptimizedAdRequest()

        Log.d(TAG, "Cargando rewarded...")

        RewardedAd.load(
            context,
            currentRewardedUnitId(),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoadingRewarded.set(false)
                    rewardedRetryCount = 0
                    rewarded = ad

                    rewarded?.fullScreenContentCallback = createFullScreenCallback("rewarded", context)
                    Log.d(TAG, "Rewarded cargado exitosamente")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingRewarded.set(false)
                    rewarded = null
                    rewardedRetryCount++

                    Log.w(TAG, "Error cargando rewarded: ${loadAdError.code} - ${loadAdError.message}")
                    Log.w(TAG, "Intento $rewardedRetryCount/$MAX_RETRY_ATTEMPTS")

                    if (rewardedRetryCount < MAX_RETRY_ATTEMPTS) {
                        scheduleRetry { loadRewardedIfNeeded(context) }
                    }
                }
            }
        )
    }

    private fun createOptimizedAdRequest(): AdRequest {
        return AdRequest.Builder()
            .setHttpTimeoutMillis(30000)
            .apply {
                try {
                    setContentUrl("https://play.google.com/store/apps/details?id=com.follgramer.diamantesproplayersgo")
                    addKeyword("games")
                    addKeyword("rewards")
                    addKeyword("mobile")
                } catch (e: Exception) {
                    Log.w(TAG, "Error configurando request: ${e.message}")
                }
            }
            .build()
    }

    /**
     * Configurar Safe Areas antes de mostrar anuncios
     * ESTO ES LO QUE USAN LOS DESARROLLADORES PROFESIONALES
     */
    private fun setupSafeAreasForFullscreenAd(activity: Activity) {
        try {
            // 1. Forzar que respete las barras del sistema
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)

            // 2. En Android P+, configurar cutout mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                activity.window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }

            // 3. Asegurar que las barras del sistema sean visibles
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+
                activity.window.insetsController?.apply {
                    show(WindowInsets.Type.systemBars())
                }
            } else {
                // Android 10 y anteriores
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_VISIBLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }

            // 4. Configurar window flags
            activity.window.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error configurando safe areas: ${e.message}")
        }
    }

    /**
     * Restaurar configuración después de cerrar anuncios
     */
    private fun restoreWindowConfiguration(activity: Activity) {
        try {
            // Restaurar configuración normal de la app
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)

        } catch (e: Exception) {
            Log.e(TAG, "Error restaurando configuración: ${e.message}")
        }
    }

    private fun createFullScreenCallback(adType: String, context: Context) =
        object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "$adType mostrado exitosamente")

                // Aplicar safe areas mientras se muestra
                if (context is Activity) {
                    setupSafeAreasForFullscreenAd(context)
                }
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "$adType cerrado por usuario")

                // Restaurar configuración
                if (context is Activity) {
                    restoreWindowConfiguration(context)
                }

                when (adType) {
                    "interstitial" -> {
                        interstitial = null
                        mainHandler.postDelayed({
                            loadInterstitialIfNeeded(context)
                        }, 2000)
                    }
                    "rewarded" -> {
                        rewarded = null
                        mainHandler.postDelayed({
                            loadRewardedIfNeeded(context)
                        }, 2000)
                    }
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Error mostrando $adType: ${adError.code} - ${adError.message}")

                when (adType) {
                    "interstitial" -> {
                        interstitial = null
                        loadInterstitialIfNeeded(context)
                    }
                    "rewarded" -> {
                        rewarded = null
                        loadRewardedIfNeeded(context)
                    }
                }
            }
        }

    private fun scheduleRetry(retryAction: () -> Unit) {
        mainHandler.postDelayed(retryAction, RETRY_DELAY_MS)
    }

    fun showInterstitial(activity: Activity, onClosed: () -> Unit = {}) {
        try {
            if (activity.isFinishing || activity.isDestroyed) {
                Log.w(TAG, "Activity no válida para mostrar interstitial")
                onClosed()
                return
            }

            // CONFIGURAR SAFE AREAS ANTES DE MOSTRAR
            setupSafeAreasForFullscreenAd(activity)

            val ad = interstitial
            if (ad != null) {
                Log.d(TAG, "Mostrando interstitial con Safe Areas")
                ad.show(activity)
                onClosed()
            } else {
                Log.w(TAG, "Interstitial no disponible")
                loadInterstitialIfNeeded(activity)
                onClosed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception en showInterstitial: ${e.message}")
            onClosed()
        }
    }

    fun showRewarded(activity: Activity, onReward: (amount: Int, type: String) -> Unit) {
        try {
            if (activity.isFinishing || activity.isDestroyed) {
                Log.w(TAG, "Activity no válida para mostrar rewarded")
                return
            }

            // CONFIGURAR SAFE AREAS ANTES DE MOSTRAR
            setupSafeAreasForFullscreenAd(activity)

            val ad = rewarded
            if (ad != null) {
                Log.d(TAG, "Mostrando rewarded con Safe Areas")
                ad.show(activity) { rewardItem ->
                    val amount = rewardItem.amount
                    val type = rewardItem.type
                    Log.d(TAG, "Recompensa obtenida: $amount $type")
                    onReward(amount, type)
                }
            } else {
                Log.w(TAG, "Rewarded no disponible, dando recompensa por defecto")
                loadRewardedIfNeeded(activity)
                onReward(10, "coins")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception en showRewarded: ${e.message}")
            onReward(10, "coins")
        }
    }

    fun hasInterstitialReady(): Boolean = interstitial != null
    fun hasRewardedReady(): Boolean = rewarded != null

    fun forceReload(context: Context) {
        Log.d(TAG, "Forzando recarga de anuncios")

        interstitialRetryCount = 0
        rewardedRetryCount = 0

        isLoadingInterstitial.set(false)
        isLoadingRewarded.set(false)

        interstitial = null
        rewarded = null

        mainHandler.postDelayed({
            loadInterstitialIfNeeded(context)
            loadRewardedIfNeeded(context)
        }, 3000)
    }

    fun cleanup() {
        Log.d(TAG, "Limpiando recursos del AdManager")

        try {
            mainHandler.removeCallbacksAndMessages(null)

            interstitial?.fullScreenContentCallback = null
            rewarded?.fullScreenContentCallback = null

            interstitial = null
            rewarded = null

            isLoadingInterstitial.set(false)
            isLoadingRewarded.set(false)
            isWarmingUp.set(false)

            interstitialRetryCount = 0
            rewardedRetryCount = 0

        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}")
        }
    }

    fun getDebugInfo(): String {
        return """
            AdManager Debug Info:
            - Interstitial: ${if (interstitial != null) "Listo" else "No disponible"} (loading: ${isLoadingInterstitial.get()}, retries: $interstitialRetryCount)
            - Rewarded: ${if (rewarded != null) "Listo" else "No disponible"} (loading: ${isLoadingRewarded.get()}, retries: $rewardedRetryCount)
            - WarmingUp: ${isWarmingUp.get()}
            - Build Type: ${if (BuildConfig.DEBUG) "DEBUG (Test Ads)" else "RELEASE (Real Ads)"}
            - Unit IDs: I=${currentInterstitialUnitId().takeLast(6)}, R=${currentRewardedUnitId().takeLast(6)}, B=${currentBannerUnitId().takeLast(6)}
        """.trimIndent()
    }
}