package com.follgramer.diamantesproplayersgo.notifications

import android.util.Log

// EventBus simple para comunicación entre componentes
object NotificationEventBus {
    private const val TAG = "NotificationEventBus"
    private val listeners = mutableListOf<(NotificationEvent) -> Unit>()

    fun subscribe(listener: (NotificationEvent) -> Unit) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
                Log.d(TAG, "Listener suscrito. Total: ${listeners.size}")
            }
        }
    }

    fun unsubscribe(listener: (NotificationEvent) -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
            Log.d(TAG, "Listener desuscrito. Total: ${listeners.size}")
        }
    }

    fun post(event: NotificationEvent) {
        synchronized(listeners) {
            Log.d(TAG, "Enviando evento: ${event::class.simpleName} a ${listeners.size} listeners")
            listeners.toList().forEach { listener ->
                try {
                    listener(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en listener: ${e.message}")
                }
            }
        }
    }

    fun clear() {
        synchronized(listeners) {
            listeners.clear()
            Log.d(TAG, "Todos los listeners eliminados")
        }
    }
}