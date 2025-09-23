package com.follgramer.diamantesproplayersgo.ads

import android.content.Context
import android.util.Log
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.follgramer.diamantesproplayersgo.DiamantesApplication
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
            val configBuilder = RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "🧪 Configurando anuncios de prueba")

                val testDevices = listOf(
                    "72F928A6866D6BC25A62E7A6F4AFC402",
                    "B3EEABB8EE11C2BE770B684D95219ECB"
                )

                configBuilder.setTestDeviceIds(testDevices)

                Log.d(TAG, "Dispositivos de prueba configurados:")
                testDevices.forEach { device ->
                    Log.d(TAG, "  - $device")
                }
            } else {
                Log.d(TAG, "🚀 Configurando anuncios reales de producción")
            }

            val configuration = configBuilder.build()
            MobileAds.setRequestConfiguration(configuration)

            // Inicializar AdMob
            MobileAds.initialize(context) { initializationStatus ->
                try {
                    Log.d(TAG, "📋 Estado de inicialización de AdMob:")

                    var allAdaptersReady = true
                    initializationStatus.adapterStatusMap.forEach { (adapter, status) ->
                        val state = status?.initializationState?.name ?: "UNKNOWN"
                        val description = status?.description ?: "Sin descripción"

                        Log.d(TAG, "  - Adaptador: $adapter")
                        Log.d(TAG, "    Estado: $state")
                        if (description.isNotEmpty()) {
                            Log.d(TAG, "    Descripción: $description")
                        }

                        if (state != "READY") {
                            allAdaptersReady = false
                        }
                    }

                    isInitialized.set(true)
                    DiamantesApplication.markAdMobAsInitialized()

                    Log.d(TAG, "✅ AdMob inicializado correctamente")
                    Log.d(TAG, "📝 Configuración actual:")
                    Log.d(TAG, "  Modo: ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}")
                    Log.d(TAG, "  SDK Version: ${MobileAds.getVersion()}")
                    Log.d(TAG, "  Todos los adaptadores listos: $allAdaptersReady")

                    Log.d(TAG, "📝 IDs de anuncios en uso:")
                    Log.d(TAG, "  Banner Top: ${currentBannerTopUnitId()}")
                    Log.d(TAG, "  Banner Bottom: ${currentBannerBottomUnitId()}")
                    Log.d(TAG, "  Banner Recycler: ${currentRecyclerBannerUnitId()}")
                    Log.d(TAG, "  Interstitial: ${currentInterstitialUnitId()}")
                    Log.d(TAG, "  Rewarded: ${currentRewardedUnitId()}")

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "🧪 MODO DEBUG - Todos los anuncios serán de prueba")
                        Log.d(TAG, "⚠️ Los anuncios de prueba pueden tardar un poco en cargar la primera vez")
                    } else {
                        Log.d(TAG, "💰 MODO RELEASE - Anuncios reales activos")
                        Log.d(TAG, "⚠️ Asegúrate de que tu cuenta de AdMob esté verificada")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando estado de inicialización: ${e.message}", e)
                    isInitialized.set(true) // Marcar como inicializado de todos modos
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error crítico inicializando AdMob: ${e.message}", e)
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
            SDK Version: ${try { MobileAds.getVersion() } catch (e: Exception) { "No disponible" }}
            Banner Top: ${currentBannerTopUnitId()}
            Banner Bottom: ${currentBannerBottomUnitId()}
            Banner Recycler: ${currentRecyclerBannerUnitId()}
            Interstitial: ${currentInterstitialUnitId()}
            Rewarded: ${currentRewardedUnitId()}
        """.trimIndent()
    }
}