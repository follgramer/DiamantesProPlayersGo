package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Gestor centralizado y profesional de anuncios AdMob
 * Maneja Interstitial y Rewarded Ads con retry automático y gestión de errores
 */
object AdManager {
    private const val TAG = "AdManager"
    private const val MAX_RETRY_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 2000L
    private const val MIN_REWARD_FALLBACK = 5
    private const val DEFAULT_REWARD_FALLBACK = 10

    // Estados de carga
    private var interstitialRetryCount = 0
    private var rewardedRetryCount = 0
    private val isLoadingInterstitial = AtomicBoolean(false)
    private val isLoadingRewarded = AtomicBoolean(false)

    // Instancias de anuncios
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    // Handler para delays
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Inicializa el AdManager cargando anuncios iniciales
     */
    fun initialize(context: Context) {
        Log.d(TAG, "Inicializando AdManager...")
        Log.d(TAG, "Modo de anuncios: ${if (isUsingTestAds()) "TEST" else "PRODUCCIÓN"}")

        if (AdsInit.isAdMobReady()) {
            loadInterstitialIfNeeded(context)
            loadRewardedIfNeeded(context)
        } else {
            Log.w(TAG, "AdMob no está listo, esperando inicialización...")
            mainHandler.postDelayed({
                if (AdsInit.isAdMobReady()) {
                    loadInterstitialIfNeeded(context)
                    loadRewardedIfNeeded(context)
                }
            }, 3000)
        }
    }

    /**
     * Función de warmUp - precarga anuncios (alias para initialize)
     */
    fun warmUp(context: Context) {
        Log.d(TAG, "Warming up AdManager...")
        initialize(context)
    }

    // ==========================================
    // ANUNCIOS INTERSTICIALES
    // ==========================================

    /**
     * Muestra un anuncio intersticial si está disponible
     * @param activity Actividad donde mostrar el anuncio
     * @param onClosed Callback ejecutado cuando se cierra el anuncio
     */
    fun showInterstitial(activity: Activity, onClosed: () -> Unit = {}) {
        try {
            if (!isValidActivity(activity)) {
                Log.w(TAG, "Activity no válida para mostrar intersticial")
                onClosed()
                return
            }

            val ad = interstitialAd
            if (ad != null) {
                Log.d(TAG, "📱 Mostrando anuncio intersticial")

                setupSystemBarsForAd(activity)

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "✅ Intersticial mostrado exitosamente")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Intersticial cerrado por usuario")
                        restoreSystemBars(activity)
                        interstitialAd = null

                        // Recargar para la próxima vez
                        mainHandler.postDelayed({
                            loadInterstitialIfNeeded(activity)
                        }, RETRY_DELAY_MS)

                        onClosed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "❌ Error mostrando intersticial: ${adError.message}")
                        restoreSystemBars(activity)
                        interstitialAd = null
                        loadInterstitialIfNeeded(activity)
                        onClosed()
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "Impresión de intersticial registrada")
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Click en intersticial registrado")
                    }
                }

                ad.show(activity)
            } else {
                Log.w(TAG, "❌ Intersticial no disponible")
                loadInterstitialIfNeeded(activity)
                onClosed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception en showInterstitial: ${e.message}", e)
            onClosed()
        }
    }

    /**
     * Carga un anuncio intersticial si es necesario
     */
    private fun loadInterstitialIfNeeded(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial.getAndSet(true)) {
            Log.d(TAG, "Intersticial ya está cargado o cargando")
            return
        }

        if (interstitialRetryCount >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Máximo de intentos alcanzado para intersticial")
            isLoadingInterstitial.set(false)
            return
        }

        val adRequest = createAdRequest()
        Log.d(TAG, "🔄 Cargando anuncio intersticial...")
        Log.d(TAG, "Unit ID: ${currentInterstitialUnitId()}")

        InterstitialAd.load(
            context,
            currentInterstitialUnitId(),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoadingInterstitial.set(false)
                    interstitialRetryCount = 0
                    interstitialAd = ad
                    Log.d(TAG, "✅ Intersticial cargado exitosamente")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingInterstitial.set(false)
                    interstitialAd = null
                    interstitialRetryCount++

                    Log.w(TAG, "❌ Error cargando intersticial: ${loadAdError.code} - ${loadAdError.message}")
                    Log.w(TAG, "Intento $interstitialRetryCount/$MAX_RETRY_ATTEMPTS")

                    if (interstitialRetryCount < MAX_RETRY_ATTEMPTS) {
                        scheduleRetry { loadInterstitialIfNeeded(context) }
                    }
                }
            }
        )
    }

    // ==========================================
    // ANUNCIOS RECOMPENSADOS
    // ==========================================

    /**
     * Muestra un anuncio recompensado si está disponible
     * @param activity Actividad donde mostrar el anuncio
     * @param onReward Callback con la recompensa obtenida (amount, type)
     */
    fun showRewarded(activity: Activity, onReward: (amount: Int, type: String) -> Unit) {
        try {
            if (!isValidActivity(activity)) {
                Log.w(TAG, "Activity no válida para mostrar recompensado")
                onReward(DEFAULT_REWARD_FALLBACK, "coins")
                return
            }

            val ad = rewardedAd
            if (ad != null) {
                Log.d(TAG, "📺 Mostrando video recompensado")

                setupSystemBarsForAd(activity)

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "✅ Video recompensado mostrado exitosamente")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Video recompensado cerrado por usuario")
                        restoreSystemBars(activity)
                        rewardedAd = null

                        // Recargar para la próxima vez
                        mainHandler.postDelayed({
                            loadRewardedIfNeeded(activity)
                        }, RETRY_DELAY_MS)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "❌ Error mostrando video: ${adError.message}")
                        Log.e(TAG, "Código de error: ${adError.code}")
                        restoreSystemBars(activity)
                        rewardedAd = null
                        loadRewardedIfNeeded(activity)

                        // Dar recompensa mínima en caso de error
                        onReward(MIN_REWARD_FALLBACK, "coins")
                    }

                    override fun onAdImpression() {
                        Log.d(TAG, "Impresión de video registrada")
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Click en video registrado")
                    }
                }

                ad.show(activity) { rewardItem ->
                    val amount = rewardItem.amount
                    val type = rewardItem.type
                    Log.d(TAG, "🎁 Recompensa obtenida: $amount $type")
                    onReward(amount, type)
                }

            } else {
                Log.w(TAG, "❌ Video recompensado no disponible")
                loadRewardedIfNeeded(activity)

                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Video cargando, intenta en unos segundos...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception en showRewarded: ${e.message}", e)
            onReward(DEFAULT_REWARD_FALLBACK, "coins")
        }
    }

    /**
     * Carga un anuncio recompensado si es necesario
     */
    private fun loadRewardedIfNeeded(context: Context) {
        if (rewardedAd != null || isLoadingRewarded.getAndSet(true)) {
            Log.d(TAG, "Recompensado ya está cargado o cargando")
            return
        }

        if (rewardedRetryCount >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Máximo de intentos alcanzado para recompensado")
            isLoadingRewarded.set(false)
            return
        }

        val adRequest = createAdRequest()
        Log.d(TAG, "🔄 Cargando video recompensado...")
        Log.d(TAG, "Unit ID: ${currentRewardedUnitId()}")

        RewardedAd.load(
            context,
            currentRewardedUnitId(),
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoadingRewarded.set(false)
                    rewardedRetryCount = 0
                    rewardedAd = ad
                    Log.d(TAG, "✅ Rewarded video cargado exitosamente")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingRewarded.set(false)
                    rewardedAd = null
                    rewardedRetryCount++

                    Log.w(TAG, "❌ Error cargando recompensado: ${loadAdError.code} - ${loadAdError.message}")
                    Log.w(TAG, "Intento $rewardedRetryCount/$MAX_RETRY_ATTEMPTS")

                    if (rewardedRetryCount < MAX_RETRY_ATTEMPTS) {
                        scheduleRetry { loadRewardedIfNeeded(context) }
                    }
                }
            }
        )
    }

    // ==========================================
    // FUNCIONES AUXILIARES
    // ==========================================

    /**
     * Crea un AdRequest configurado según el modo de compilación
     */
    private fun createAdRequest(): AdRequest {
        // Los test devices se configuran a nivel de aplicación en AdsInit
        return AdRequest.Builder().build()
    }

    /**
     * Valida si la actividad es válida para mostrar anuncios
     */
    private fun isValidActivity(activity: Activity): Boolean {
        return !activity.isFinishing && !activity.isDestroyed
    }

    /**
     * Configura las barras del sistema para anuncios de pantalla completa
     */
    private fun setupSystemBarsForAd(activity: Activity) {
        Log.d(TAG, "Configurando barras del sistema para anuncio")
    }

    /**
     * Restaura las barras del sistema después del anuncio
     */
    private fun restoreSystemBars(activity: Activity) {
        Log.d(TAG, "Restaurando barras del sistema")
    }

    /**
     * Programa un reintento después de un delay
     */
    private fun scheduleRetry(action: () -> Unit) {
        mainHandler.postDelayed(action, RETRY_DELAY_MS)
    }

    // ==========================================
    // FUNCIONES PÚBLICAS DE ESTADO
    // ==========================================

    /**
     * Verifica si hay un intersticial listo para mostrar
     */
    fun isInterstitialReady(): Boolean = interstitialAd != null

    /**
     * Verifica si hay un intersticial listo para mostrar (alias)
     */
    fun hasInterstitialReady(): Boolean = isInterstitialReady()

    /**
     * Verifica si hay un recompensado listo para mostrar
     */
    fun isRewardedReady(): Boolean = rewardedAd != null

    /**
     * Reinicia todos los contadores de reintento
     */
    fun resetRetryCounters() {
        interstitialRetryCount = 0
        rewardedRetryCount = 0
        isLoadingInterstitial.set(false)
        isLoadingRewarded.set(false)
        Log.d(TAG, "Contadores de reintento reiniciados")
    }

    /**
     * Limpia todas las referencias de anuncios
     */
    fun cleanup() {
        Log.d(TAG, "Limpiando AdManager...")
        interstitialAd = null
        rewardedAd = null
        resetRetryCounters()
        mainHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Información de debug del estado actual
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== AD MANAGER DEBUG INFO ===")
            appendLine("Intersticial listo: ${isInterstitialReady()}")
            appendLine("Recompensado listo: ${isRewardedReady()}")
            appendLine("Cargando intersticial: ${isLoadingInterstitial.get()}")
            appendLine("Cargando recompensado: ${isLoadingRewarded.get()}")
            appendLine("Reintentos intersticial: $interstitialRetryCount/$MAX_RETRY_ATTEMPTS")
            appendLine("Reintentos recompensado: $rewardedRetryCount/$MAX_RETRY_ATTEMPTS")
            appendLine("Modo anuncios: ${if (isUsingTestAds()) "TEST" else "PRODUCCIÓN"}")
            appendLine("============================")
        }
    }
}