package com.follgramer.diamantesproplayersgo.ads

import android.content.Context
import android.util.Log
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.AdapterStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdsInit {
    private const val TAG = "AdsInit"
    private var isAdMobInitialized = false
    private var isFacebookInitialized = false

    /**
     * Inicializa el sistema de anuncios (AdMob + Facebook)
     *
     * IMPORTANTE: La mediación de Facebook se configura en AdMob Console
     * y funciona vía servidor (server-side mediation). No verás el adaptador
     * de Facebook en los logs, pero funcionará automáticamente cuando:
     * - Los grupos de mediación estén activos (24-48h después de crearlos)
     * - AdMob no tenga inventario disponible
     * - Tu app tenga suficiente tráfico
     */
    fun init(context: Context, onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🚀 INICIANDO SISTEMA DE ANUNCIOS")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // Paso 1: Inicializar Facebook SDK (requerido para mediación)
            initializeFacebook(context)

            // Paso 2: Inicializar AdMob
            initializeAdMob(context)

            onComplete?.invoke()
        }
    }

    private fun initializeFacebook(context: Context) {
        if (isFacebookInitialized) {
            Log.d(TAG, "Facebook ya inicializado")
            return
        }

        try {
            Log.d(TAG, "🔷 Inicializando Facebook Audience Network...")

            // Activar modo de prueba solo en DEBUG
            if (BuildConfig.DEBUG) {
                // Device ID del dispositivo físico
                AdSettings.addTestDevice("F9C1540151B5789BE4ADD3DF55FA783D")

                // Device ID del emulador
                AdSettings.addTestDevice("35cc4c8a-9430-4f2f-a846-3adc6dc67d95")

                Log.d(TAG, "🧪 Modo de prueba de Facebook activado")
            }

            // Inicializar Facebook Audience Network
            if (!AudienceNetworkAds.isInitialized(context)) {
                AudienceNetworkAds.initialize(context)
                isFacebookInitialized = true
                Log.d(TAG, "✅ Facebook Audience Network inicializado")
            } else {
                isFacebookInitialized = true
                Log.d(TAG, "✅ Facebook ya estaba inicializado")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando Facebook: ${e.message}", e)
        }
    }

    private fun initializeAdMob(context: Context) {
        if (isAdMobInitialized) {
            Log.d(TAG, "AdMob ya inicializado")
            return
        }

        try {
            Log.d(TAG, "🔵 Inicializando AdMob...")

            MobileAds.initialize(context) { initializationStatus ->
                isAdMobInitialized = true

                Log.d(TAG, "")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅ SISTEMA DE ANUNCIOS INICIALIZADO")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "")
                Log.d(TAG, "📊 Estado de adaptadores:")

                var adapterCount = 0
                initializationStatus.adapterStatusMap.forEach { (className, status) ->
                    adapterCount++
                    val statusName = when (status.initializationState) {
                        AdapterStatus.State.READY -> "READY ✅"
                        AdapterStatus.State.NOT_READY -> "NOT_READY ⚠️"
                        else -> "UNKNOWN ❓"
                    }

                    val simpleName = className.substringAfterLast('.')
                    Log.d(TAG, "  $adapterCount. $simpleName: $statusName")

                    if (status.description.isNotEmpty()) {
                        Log.d(TAG, "     → ${status.description}")
                    }
                }

                Log.d(TAG, "")
                Log.d(TAG, "📡 Mediación de Facebook:")
                Log.d(TAG, "  → Configurada vía AdMob Console (server-side)")
                Log.d(TAG, "  → No aparece en logs locales")
                Log.d(TAG, "  → Se activa en 24-48h después de crear grupos")
                Log.d(TAG, "  → Funciona automáticamente cuando AdMob no tiene inventario")
                Log.d(TAG, "")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando AdMob: ${e.message}", e)
        }
    }

    /**
     * Limpia recursos del sistema de anuncios
     */
    fun cleanup() {
        try {
            Log.d(TAG, "🧹 Limpiando recursos de anuncios...")
            // Facebook y AdMob se limpian automáticamente
        } catch (e: Exception) {
            Log.e(TAG, "Error en cleanup: ${e.message}", e)
        }
    }

    /**
     * Verifica si AdMob está listo
     */
    fun isAdMobReady() = isAdMobInitialized

    /**
     * Verifica si Facebook está listo
     */
    fun isFacebookReady() = isFacebookInitialized
}