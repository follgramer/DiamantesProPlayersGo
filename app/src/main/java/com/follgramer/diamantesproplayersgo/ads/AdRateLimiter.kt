package com.follgramer.diamantesproplayersgo.ads

import android.util.Log

object AdRateLimiter {
    private const val TAG = "AdRateLimiter"

    // Tiempos más permisivos
    private const val MIN_REQUEST_INTERVAL = 10000L // 10 segundos entre requests
    private const val NO_FILL_COOLDOWN = 60000L // 1 minuto si no hay fill
    private const val GLOBAL_MIN_INTERVAL = 3000L // 3 segundos entre cualquier banner

    private val lastRequestTime = mutableMapOf<String, Long>()
    private val noFillTime = mutableMapOf<String, Long>()
    private val lastGlobalRequest = mutableMapOf<String, Long>()

    fun canRequestAd(adUnitId: String): Boolean {
        val now = System.currentTimeMillis()

        // Verificar tiempo global (entre cualquier banner)
        val globalKey = "global_banner"
        lastGlobalRequest[globalKey]?.let { lastTime ->
            if (now - lastTime < GLOBAL_MIN_INTERVAL) {
                Log.d(TAG, "Esperando tiempo global para banners")
                return false
            }
        }

        // Si hubo No Fill recientemente, esperar más
        noFillTime[adUnitId]?.let { lastNoFill ->
            if (now - lastNoFill < NO_FILL_COOLDOWN) {
                val remaining = (NO_FILL_COOLDOWN - (now - lastNoFill)) / 1000
                Log.d(TAG, "En cooldown por No Fill: ${remaining}s restantes")
                return false
            }
        }

        // Verificar tiempo mínimo entre solicitudes
        lastRequestTime[adUnitId]?.let { lastTime ->
            if (now - lastTime < MIN_REQUEST_INTERVAL) {
                val remaining = (MIN_REQUEST_INTERVAL - (now - lastTime)) / 1000
                Log.d(TAG, "Esperando para $adUnitId: ${remaining}s")
                return false
            }
        }

        return true
    }

    fun markRequestStarted(adUnitId: String) {
        val now = System.currentTimeMillis()
        lastRequestTime[adUnitId] = now
        lastGlobalRequest["global_banner"] = now
        Log.d(TAG, "Solicitud iniciada para $adUnitId")
    }

    fun markRequestCompleted(adUnitId: String, failed: Boolean = false, errorCode: Int = 0) {
        if (failed) {
            if (errorCode == 3) { // No fill
                noFillTime[adUnitId] = System.currentTimeMillis()
                Log.d(TAG, "No fill registrado para $adUnitId")
            } else {
                Log.d(TAG, "Error código $errorCode para $adUnitId")
            }
        } else {
            // Limpiar en éxito
            noFillTime.remove(adUnitId)
            Log.d(TAG, "Carga exitosa para $adUnitId")
        }
    }

    fun reset() {
        lastRequestTime.clear()
        noFillTime.clear()
        lastGlobalRequest.clear()
        Log.d(TAG, "Rate limiter reseteado")
    }
}