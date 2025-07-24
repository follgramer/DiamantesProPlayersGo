package com.follgramer.diamantesproplayersgo

import android.content.Context
import android.util.Log
import java.util.UUID

object SessionManager {
    private const val PREFS_NAME = "DiamantesProPrefs"
    private const val KEY_PLAYER_ID = "PLAYER_ID"
    private const val KEY_SESSION_ID = "SESSION_ID"
    private const val TAG = "SessionManager"

    /**
     * Inicializa SessionManager - OBLIGATORIO llamar en onCreate()
     */
    fun init(context: Context) {
        Log.d(TAG, "SessionManager inicializado")
    }

    /**
     * Obtiene el ID del jugador de Free Fire (ingresado por el usuario)
     */
    fun getPlayerId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val playerId = prefs.getString(KEY_PLAYER_ID, "") ?: ""
        Log.d(TAG, "Player ID obtenido: ${if (playerId.isEmpty()) "No configurado" else playerId}")
        return playerId
    }

    /**
     * Guarda el ID del jugador de Free Fire
     */
    fun setPlayerId(context: Context, playerId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PLAYER_ID, playerId).apply()
        Log.d(TAG, "Player ID guardado: $playerId")

        // Al cambiar de jugador, generar nueva sesión
        generateNewSession(context)
    }

    /**
     * Obtiene el ID de sesión único para este dispositivo
     */
    fun getSessionId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var sessionId = prefs.getString(KEY_SESSION_ID, "")

        if (sessionId.isNullOrEmpty()) {
            sessionId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_SESSION_ID, sessionId).apply()
            Log.d(TAG, "Nueva sesión creada: $sessionId")
        } else {
            Log.d(TAG, "Sesión existente: $sessionId")
        }

        return sessionId
    }

    /**
     * Genera una nueva sesión
     */
    fun generateNewSession(context: Context): String {
        val newSessionId = UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SESSION_ID, newSessionId).apply()
        Log.d(TAG, "Nueva sesión generada: $newSessionId")
        return newSessionId
    }

    /**
     * Limpia la sesión actual
     */
    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SESSION_ID).apply()
        Log.d(TAG, "Sesión limpiada")
    }

    /**
     * Limpia completamente todos los datos guardados
     */
    fun clearAllData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d(TAG, "Todos los datos limpiados completamente")
    }

    /**
     * Verifica si el usuario tiene configurado su Player ID
     */
    fun hasPlayerIdConfigured(context: Context): Boolean {
        val playerId = getPlayerId(context)
        return playerId.isNotEmpty() && playerId.length >= 5
    }
}