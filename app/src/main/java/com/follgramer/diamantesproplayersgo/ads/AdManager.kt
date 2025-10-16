package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import kotlinx.coroutines.*

object AdManager {
    private const val TAG = "AdManager"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Control de frecuencia
    private var lastInterstitialTime = 0L
    private const val MIN_INTERSTITIAL_INTERVAL = 30000L // 30 segundos mínimo entre intersticiales

    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "AdManager ya inicializado")
            return
        }

        isInitialized = true
        Log.d(TAG, "✅ AdManager inicializado")

        // Pre-cargar anuncios
        scope.launch {
            delay(1000) // Pequeño delay para no saturar el inicio
            preloadInterstitial(context)
            preloadRewarded(context)
        }
    }

    private fun preloadInterstitial(context: Context) {
        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial ya cargado")
            return
        }

        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdIds.interstitial()

        Log.d(TAG, "🔄 Cargando interstitial: $adUnitId")

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "✅ Interstitial pre-cargado exitosamente")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error cargando interstitial: ${error.message}")
                    Log.e(TAG, "Código de error: ${error.code}")
                    interstitialAd = null

                    // Reintentar en 30 segundos
                    scope.launch {
                        delay(30000)
                        preloadInterstitial(context)
                    }
                }
            }
        )
    }

    private fun preloadRewarded(context: Context) {
        if (rewardedAd != null) {
            Log.d(TAG, "Rewarded ya cargado")
            return
        }

        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdIds.rewarded()

        Log.d(TAG, "🔄 Cargando rewarded: $adUnitId")

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "✅ Rewarded pre-cargado exitosamente")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Error cargando rewarded: ${error.message}")
                    Log.e(TAG, "Código de error: ${error.code}")
                    rewardedAd = null

                    // Reintentar en 30 segundos
                    scope.launch {
                        delay(30000)
                        preloadRewarded(context)
                    }
                }
            }
        )
    }

    /**
     * Muestra un anuncio intersticial
     */
    fun showInterstitial(
        activity: Activity,
        onDismiss: () -> Unit = {}
    ) {
        // Verificar control de frecuencia
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterstitialTime < MIN_INTERSTITIAL_INTERVAL) {
            Log.d(TAG, "⏱️ Interstitial bloqueado por control de frecuencia")
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            lastInterstitialTime = currentTime

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitial cerrado")
                    interstitialAd = null
                    // Pre-cargar siguiente anuncio
                    scope.launch {
                        delay(2000)
                        preloadInterstitial(activity)
                    }
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "❌ Error mostrando interstitial: ${error.message}")
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "📺 Interstitial mostrado")
                    interstitialAd = null // Limpiar referencia
                }

                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Impresión de interstitial registrada")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "👆 Interstitial clickeado")
                }
            }

            try {
                ad.show(activity)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción mostrando interstitial: ${e.message}")
                interstitialAd = null
                preloadInterstitial(activity)
                onDismiss()
            }
        } else {
            Log.w(TAG, "⚠️ Interstitial no está listo")
            preloadInterstitial(activity)
            onDismiss()
        }
    }

    /**
     * Muestra un anuncio rewarded
     */
    fun showRewarded(
        activity: Activity,
        onReward: (RewardItem) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad != null) {
            var wasRewarded = false

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Rewarded cerrado (recompensa dada: $wasRewarded)")
                    rewardedAd = null

                    // Pre-cargar siguiente anuncio
                    scope.launch {
                        delay(2000)
                        preloadRewarded(activity)
                    }

                    // Solo llamar onDismiss si NO se dio recompensa
                    if (!wasRewarded) {
                        onDismiss()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "❌ Error mostrando rewarded: ${error.message}")
                    rewardedAd = null
                    preloadRewarded(activity)
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "📺 Rewarded mostrado")
                    rewardedAd = null // Limpiar referencia
                }

                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Impresión de rewarded registrada")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "👆 Rewarded clickeado")
                }
            }

            try {
                ad.show(activity) { rewardItem ->
                    wasRewarded = true
                    Log.d(TAG, "🎁 Recompensa obtenida: ${rewardItem.amount} ${rewardItem.type}")
                    onReward(rewardItem)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción mostrando rewarded: ${e.message}")
                rewardedAd = null
                preloadRewarded(activity)
                onDismiss()
            }
        } else {
            Log.w(TAG, "⚠️ Rewarded no está listo, intentando cargar...")
            preloadRewarded(activity)

            // Mostrar mensaje al usuario
            scope.launch {
                kotlinx.coroutines.delay(1000)
                // Si después de 1 segundo sigue sin estar listo, avisar
                if (rewardedAd == null) {
                    Log.w(TAG, "⚠️ Rewarded aún no disponible después de 1 segundo")
                }
            }

            onDismiss()
        }
    }

    /**
     * ✅ Verifica si hay un interstitial listo para mostrar
     */
    fun isInterstitialReady(): Boolean {
        val ready = interstitialAd != null
        val canShow = System.currentTimeMillis() - lastInterstitialTime >= MIN_INTERSTITIAL_INTERVAL
        Log.d(TAG, "Interstitial ready: $ready, Can show (frequency): $canShow")
        return ready && canShow
    }

    /**
     * ✅ Verifica si hay un rewarded ad listo para mostrar
     */
    fun isRewardedReady(): Boolean {
        val ready = rewardedAd != null
        Log.d(TAG, "Rewarded ready: $ready")
        return ready
    }

    /**
     * Pre-carga ambos tipos de anuncios
     */
    fun preloadAds(context: Context) {
        Log.d(TAG, "🔄 Pre-cargando todos los anuncios...")
        preloadInterstitial(context)
        preloadRewarded(context)
    }

    /**
     * Fuerza la recarga de anuncios
     */
    fun forceReload(context: Context) {
        Log.d(TAG, "🔄 Forzando recarga de anuncios...")
        interstitialAd = null
        rewardedAd = null
        preloadInterstitial(context)
        preloadRewarded(context)
    }

    /**
     * Limpia todos los recursos
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Limpiando AdManager...")
        try {
            scope.cancel()
            interstitialAd = null
            rewardedAd = null
            isInitialized = false
            lastInterstitialTime = 0L
            Log.d(TAG, "✅ AdManager limpiado completamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en cleanup: ${e.message}")
        }
    }

    /**
     * Obtiene información de debug
     */
    fun getDebugInfo(): String {
        return """
            📊 AdManager Debug Info:
            - Inicializado: $isInitialized
            - Interstitial listo: ${interstitialAd != null}
            - Rewarded listo: ${rewardedAd != null}
            - Último interstitial: ${if (lastInterstitialTime > 0) "${(System.currentTimeMillis() - lastInterstitialTime) / 1000}s atrás" else "Nunca"}
            - Puede mostrar interstitial: ${System.currentTimeMillis() - lastInterstitialTime >= MIN_INTERSTITIAL_INTERVAL}
        """.trimIndent()
    }
}// Updated: 2025-10-15 14:29:27
