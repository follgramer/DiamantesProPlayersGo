package com.follgramer.diamantesproplayersgo.ads

import android.util.Log
import kotlinx.coroutines.*

object AdRequestManager {
    private const val TAG = "AdRequestManager"
    private const val MIN_INTERVAL_MS = 10000L // 10 segundos entre requests

    private var lastRequestTime = 0L
    private val requestQueue = mutableListOf<suspend () -> Unit>()
    private var isProcessing = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun queueRequest(request: suspend () -> Unit) {
        requestQueue.add(request)
        Log.d(TAG, "Request encolada. Total en cola: ${requestQueue.size}")
        if (!isProcessing) {
            processQueue()
        }
    }

    private fun processQueue() {
        if (requestQueue.isEmpty()) {
            isProcessing = false
            Log.d(TAG, "Cola vacía, deteniendo procesamiento")
            return
        }

        isProcessing = true
        scope.launch {
            while (requestQueue.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val timeSinceLastRequest = now - lastRequestTime

                if (timeSinceLastRequest < MIN_INTERVAL_MS) {
                    val waitTime = MIN_INTERVAL_MS - timeSinceLastRequest
                    Log.d(TAG, "Esperando ${waitTime}ms antes del siguiente request")
                    delay(waitTime)
                }

                val request = requestQueue.removeAt(0)
                try {
                    Log.d(TAG, "Procesando request. Quedan: ${requestQueue.size}")
                    request.invoke()
                    lastRequestTime = System.currentTimeMillis()
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando request: ${e.message}")
                }

                delay(MIN_INTERVAL_MS)
            }
            isProcessing = false
            Log.d(TAG, "Todas las requests procesadas")
        }
    }

    fun clearQueue() {
        requestQueue.clear()
        isProcessing = false
        Log.d(TAG, "Cola limpiada")
    }

    fun getQueueSize(): Int = requestQueue.size
}