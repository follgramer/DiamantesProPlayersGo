package com.follgramer.diamantesproplayersgo.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import kotlinx.coroutines.*

object AdsInit {
    private const val TAG = "AdsInit"
    private var isInitialized = false
    private var initJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun init(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "AdMob ya inicializado")
            return
        }

        // Cancelar inicialización previa si existe
        initJob?.cancel()

        initJob = scope.launch {
            try {
                Log.d(TAG, "Iniciando AdMob...")

                withContext(Dispatchers.Main) {
                    MobileAds.initialize(context) { initStatus ->
                        handleInitialization(initStatus)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando AdMob: ${e.message}", e)
            }
        }
    }

    private fun handleInitialization(initStatus: InitializationStatus) {
        isInitialized = true

        Log.d(TAG, "✅ AdMob inicializado correctamente")

        // Log de adaptadores
        initStatus.adapterStatusMap.forEach { (className, status) ->
            Log.d(TAG, "Adaptador: $className - Estado: ${status.initializationState}")
        }
    }

    fun isAdMobReady(): Boolean = isInitialized

    fun cleanup() {
        initJob?.cancel()
        scope.cancel()
        isInitialized = false
        Log.d(TAG, "AdsInit limpiado")
    }
}