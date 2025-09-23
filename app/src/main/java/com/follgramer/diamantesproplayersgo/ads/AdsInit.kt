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
                Log.d(TAG, "🧪 Configurando anuncios de prueba")

                val testDevices = listOf(
                    AdRequest.DEVICE_ID_EMULATOR,
                    "72F928A6866D6BC25A62E7A6F4AFC402",
                    "B3EEABB8EE11C2BE770B684D95219ECB" // Añade más IDs si necesitas
                )

                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDevices)
                    .setTagForChildDirectedTreatment(
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                    )
                    .build()

                MobileAds.setRequestConfiguration(configuration)

            } else {
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
                        val state = status?.initializationState?.name ?: "UNKNOWN"
                        val description = status?.description ?: "Sin descripción"
                        Log.d(TAG, "  - $adapter: $state")
                        if (description.isNotEmpty()) {
                            Log.d(TAG, "    Descripción: $description")
                        }
                    }

                    isInitialized.set(true)
                    DiamantesApplication.markAdMobAsInitialized()

                    Log.d(TAG, "✅ AdMob inicializado correctamente")
                    Log.d(TAG, "📍 IDs en uso:")
                    Log.d(TAG, "  Banner Top: ${currentBannerTopUnitId()}")
                    Log.d(TAG, "  Banner Bottom: ${currentBannerBottomUnitId()}")
                    Log.d(TAG, "  Banner Recycler: ${currentRecyclerBannerUnitId()}")
                    Log.d(TAG, "  Interstitial: ${currentInterstitialUnitId()}")
                    Log.d(TAG, "  Rewarded: ${currentRewardedUnitId()}")

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "🧪 MODO DEBUG - Todos los anuncios serán de prueba")
                    } else {
                        Log.d(TAG, "💰 MODO RELEASE - Anuncios reales activos")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando estado: ${e.message}")
                    isInitialized.set(true)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando AdMob: ${e.message}")
            e.printStackTrace()
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

    fun getDebugInfo(): String {
        return """
            === ADMOB DEBUG INFO ===
            Estado: ${if (isInitialized.get()) "✅ INICIALIZADO" else "❌ NO INICIALIZADO"}
            Modo: ${if (BuildConfig.DEBUG) "DEBUG (Test Ads)" else "RELEASE (Real Ads)"}
            Banner Top: ${currentBannerTopUnitId()}
            Banner Bottom: ${currentBannerBottomUnitId()}
            Banner Recycler: ${currentRecyclerBannerUnitId()}
            Interstitial: ${currentInterstitialUnitId()}
            Rewarded: ${currentRewardedUnitId()}
        """.trimIndent()
    }
}