package com.follgramer.diamantesproplayersgo.ads

import android.content.Context
import android.util.Log
import com.follgramer.diamantesproplayersgo.BuildConfig
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
            // Configuración sin dispositivos de prueba
            val builder = RequestConfiguration.Builder()
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)

            // Si quieres agregar dispositivos de prueba en DEBUG
            if (BuildConfig.DEBUG) {
                builder.setTestDeviceIds(listOf(
                    "EMULATOR" // Para el emulador
                ))
            }

            MobileAds.setRequestConfiguration(builder.build())

            MobileAds.initialize(context) { initStatus ->
                isInitialized.set(true)
                Log.d(TAG, "✅ MobileAds inicializado | SDK=${MobileAds.getVersion()}")

                initStatus.adapterStatusMap.forEach { (adapterClass, status) ->
                    Log.d(TAG, "Adapter: $adapterClass - ${status.initializationState}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando AdMob: ${e.message}", e)
            isInitialized.set(false)
        }
    }

    fun isAdMobReady(): Boolean = isInitialized.get()

    fun getDebugInfo(): String = """
        === ADMOB DEBUG INFO ===
        Estado: ${if (isInitialized.get()) "✅ INICIALIZADO" else "❌ NO INICIALIZADO"}
        Modo: ${if (BuildConfig.DEBUG) "DEBUG (Test Ads)" else "RELEASE (Real Ads)"}
        USE_TEST_ADS: ${BuildConfig.USE_TEST_ADS}
        SDK Version: ${try { MobileAds.getVersion() } catch (_: Exception) { "N/D" }}
        ========================
    """.trimIndent()
}