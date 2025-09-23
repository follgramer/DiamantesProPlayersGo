package com.follgramer.diamantesproplayersgo.ads

import android.content.Context
import android.util.Log
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.follgramer.diamantesproplayersgo.DiamantesApplication
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import java.util.concurrent.atomic.AtomicBoolean

object AdsInit {
    private val isInitialized = AtomicBoolean(false)
    private const val TAG = "AdsInit"

    fun init(context: Context) {
        if (isInitialized.get()) {
            Log.d(TAG, "AdMob ya inicializado")
            return
        }

        try {
            Log.d(TAG, "🚀 Iniciando AdMob...")
            Log.d(TAG, "🔧 Modo: ${if (BuildConfig.DEBUG) "DEBUG con test ads" else "RELEASE con ads reales"}")

            // Configuración según el tipo de build
            if (BuildConfig.DEBUG) {
                // Modo DEBUG: Solo anuncios de prueba
                Log.d(TAG, "🧪 Configurando anuncios de prueba")

                val testDevices = listOf(
                    AdRequest.DEVICE_ID_EMULATOR,
                    "72F928A6866D6BC25A62E7A6F4AFC402" // Tu dispositivo
                )

                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDevices)
                    .setTagForChildDirectedTreatment(
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                    )
                    .build()

                MobileAds.setRequestConfiguration(configuration)

            } else {
                // Modo RELEASE: Anuncios reales
                Log.d(TAG, "🚀 Configurando anuncios reales de producción")

                val configuration = RequestConfiguration.Builder()
                    .setTagForChildDirectedTreatment(
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                    )
                    .build()

                MobileAds.setRequestConfiguration(configuration)
            }

            // Inicializar AdMob
            MobileAds.initialize(context) { initializationStatus ->
                try {
                    Log.d(TAG, "📋 Estado de inicialización:")
                    initializationStatus.adapterStatusMap.forEach { (adapter, status) ->
                        Log.d(TAG, "  - $adapter: ${status?.initializationState}")
                    }

                    isInitialized.set(true)
                    DiamantesApplication.markAdMobAsInitialized()

                    Log.d(TAG, "✅ AdMob inicializado correctamente")
                    Log.d(TAG, "📍 IDs en uso:")
                    Log.d(TAG, "  Banner: ${currentBannerUnitId().takeLast(10)}")
                    Log.d(TAG, "  Interstitial: ${currentInterstitialUnitId().takeLast(10)}")
                    Log.d(TAG, "  Rewarded: ${currentRewardedUnitId().takeLast(10)}")

                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando estado: ${e.message}")
                    isInitialized.set(true)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando AdMob: ${e.message}")
            isInitialized.set(false)
        }
    }

    fun isAdMobReady(): Boolean = isInitialized.get()
    fun isInitialized(): Boolean = isInitialized.get()

    fun reset() {
        Log.w(TAG, "🔄 Reseteando estado de AdMob")
        isInitialized.set(false)
        DiamantesApplication.resetAdMobInitialization()
    }
}