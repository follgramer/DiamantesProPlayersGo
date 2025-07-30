package com.follgramer.diamantesproplayersgo

import android.content.Context
import android.util.Log
import java.util.UUID

object SessionManager {
    private const val PREFS_NAME = "DiamantesProPrefs"
    private const val KEY_PLAYER_ID = "PLAYER_ID"
    private const val KEY_SESSION_ID = "SESSION_ID"
    private const val KEY_CURRENT_SPINS = "CURRENT_SPINS" // Clave para persistir giros
    private const val TAG = "SessionManager"

    /**
     * Inicializa SessionManager - OBLIGATORIO llamar en onCreate()
     */
    fun init(context: Context) {
        Log.d(TAG, "SessionManager inicializado")

        // Verificar integridad de datos al inicializar
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val playerId = prefs.getString(KEY_PLAYER_ID, "") ?: ""
        val currentSpins = prefs.getInt(KEY_CURRENT_SPINS, -1)

        // ✅ Si es primera vez (no hay giros configurados), establecer SOLO 10 giros
        if (currentSpins == -1) {
            Log.d(TAG, "Primera inicialización - estableciendo EXACTAMENTE 10 giros por defecto")
            setCurrentSpins(context, 10) // ✅ SOLO 10 GIROS
        }

        Log.d(TAG, "Estado inicial - Player ID: ${if (playerId.isEmpty()) "No configurado" else "Configurado"}, Giros: ${getCurrentSpins(context)}")
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
        val previousPlayerId = prefs.getString(KEY_PLAYER_ID, "") ?: ""

        prefs.edit().putString(KEY_PLAYER_ID, playerId).apply()
        Log.d(TAG, "Player ID guardado: $playerId")

        // Solo generar nueva sesión si cambió el ID del jugador
        if (previousPlayerId != playerId && previousPlayerId.isNotEmpty()) {
            Log.d(TAG, "Player ID cambió de '$previousPlayerId' a '$playerId' - generando nueva sesión")
            generateNewSession(context)

            // Al cambiar de jugador, resetear giros a 10
            resetSpinsForNewPlayer(context)
        } else if (previousPlayerId.isEmpty()) {
            // Si es la primera vez que se configura un Player ID
            Log.d(TAG, "Primera configuración de Player ID - generando sesión inicial")
            generateNewSession(context)
        }
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

        // Después de limpiar, reinicializar con valores por defecto
        init(context)
    }

    /**
     * Verifica si el usuario tiene configurado su Player ID
     */
    fun hasPlayerIdConfigured(context: Context): Boolean {
        val playerId = getPlayerId(context)
        val hasConfigured = playerId.isNotEmpty() && playerId.length >= 5
        Log.d(TAG, "Player ID configurado: $hasConfigured")
        return hasConfigured
    }

    // ================ GESTIÓN DE GIROS ================

    /**
     * Obtiene los giros actuales guardados
     * Por defecto: SOLO 10 giros iniciales
     */
    fun getCurrentSpins(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val spins = prefs.getInt(KEY_CURRENT_SPINS, 10) // ✅ Por defecto SOLO 10 giros
        Log.d(TAG, "Giros obtenidos: $spins")
        return spins
    }

    /**
     * Guarda los giros actuales
     */
    fun setCurrentSpins(context: Context, spins: Int) {
        if (spins < 0) {
            Log.w(TAG, "Intentando establecer giros negativos ($spins), estableciendo a 0")
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_CURRENT_SPINS, 0).apply()
            Log.d(TAG, "Giros guardados: 0 (corregido desde $spins)")
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_CURRENT_SPINS, spins).apply()
        Log.d(TAG, "Giros guardados: $spins")
    }

    /**
     * Añade giros a los actuales
     */
    fun addSpins(context: Context, amount: Int) {
        if (amount <= 0) {
            Log.w(TAG, "Intentando añadir cantidad inválida de giros: $amount")
            return
        }

        val currentSpins = getCurrentSpins(context)
        val newSpins = currentSpins + amount
        setCurrentSpins(context, newSpins)
        Log.d(TAG, "Giros añadidos: $amount. Total anterior: $currentSpins, Total nuevo: $newSpins")
    }

    /**
     * Usa un giro (reduce en 1)
     * Retorna true si se pudo usar, false si no hay giros disponibles
     */
    fun useSpin(context: Context): Boolean {
        val currentSpins = getCurrentSpins(context)
        if (currentSpins > 0) {
            val newSpins = currentSpins - 1
            setCurrentSpins(context, newSpins)
            Log.d(TAG, "Giro usado. Anterior: $currentSpins, Restantes: $newSpins")
            return true
        }
        Log.d(TAG, "No hay giros disponibles para usar (current: $currentSpins)")
        return false
    }

    /**
     * Reinicia los giros a 10 (solo para jugadores nuevos o cambio de jugador)
     */
    fun resetSpinsForNewPlayer(context: Context) {
        setCurrentSpins(context, 10) // ✅ Exactamente 10 giros iniciales
        Log.d(TAG, "Giros reiniciados a EXACTAMENTE 10 para nuevo jugador")
    }

    /**
     * Verifica si el usuario tiene giros disponibles
     */
    fun hasSpinsAvailable(context: Context): Boolean {
        val hasSpins = getCurrentSpins(context) > 0
        Log.d(TAG, "Tiene giros disponibles: $hasSpins")
        return hasSpins
    }

    // ================ FUNCIONES DE UTILIDAD ================

    /**
     * Obtiene información completa de la sesión para debugging
     */
    fun getSessionInfo(context: Context): String {
        val playerId = getPlayerId(context)
        val sessionId = getSessionId(context)
        val spins = getCurrentSpins(context)
        val hasPlayerConfigured = hasPlayerIdConfigured(context)

        return """
            SessionManager Info:
            - Player ID: ${if (playerId.isEmpty()) "No configurado" else playerId}
            - Session ID: $sessionId
            - Giros actuales: $spins
            - Player configurado: $hasPlayerConfigured
        """.trimIndent()
    }

    /**
     * Valida la integridad de los datos guardados
     */
    fun validateDataIntegrity(context: Context): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Verificar que los giros no sean negativos
            val spins = prefs.getInt(KEY_CURRENT_SPINS, 10)
            if (spins < 0) {
                Log.w(TAG, "Datos corruptos detectados: giros negativos ($spins). Corrigiendo...")
                setCurrentSpins(context, 10) // ✅ Corregir a 10 giros
                return false
            }

            // ✅ VERIFICAR QUE NO HAYA MÁS DE 50 GIROS (PREVENIR HACKS)
            if (spins > 50) {
                Log.w(TAG, "Giros sospechosamente altos ($spins). Corrigiendo a 10...")
                setCurrentSpins(context, 10)
                return false
            }

            // Verificar que el session ID sea válido
            val sessionId = prefs.getString(KEY_SESSION_ID, "")
            if (!sessionId.isNullOrEmpty()) {
                try {
                    UUID.fromString(sessionId)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Session ID inválido detectado. Generando nuevo...")
                    generateNewSession(context)
                    return false
                }
            }

            Log.d(TAG, "Integridad de datos validada correctamente")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error validando integridad de datos: ${e.message}")
            return false
        }
    }

    /**
     * Función de debugging para resetear completamente el SessionManager
     * ⚠️ SOLO USAR PARA TESTING
     */
    fun resetForTesting(context: Context) {
        Log.w(TAG, "⚠️ RESETEO COMPLETO PARA TESTING ⚠️")
        clearAllData(context)

        // ✅ Reinicializar con SOLO 10 giros
        setCurrentSpins(context, 10) // ✅ SOLO 10 GIROS

        // Limpiar también otros flags relacionados
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("HAS_SHOWN_WELCOME")
            .remove("CONSENT_ACCEPTED")
            .apply()

        Log.w(TAG, "Reset completo terminado - App volverá a estado inicial con EXACTAMENTE 10 giros")
    }

    // ================ FUNCIONES DE SEGURIDAD ================

    /**
     * Verifica si los giros parecen haber sido manipulados
     */
    fun detectSpinTampering(context: Context): Boolean {
        val currentSpins = getCurrentSpins(context)
        val playerId = getPlayerId(context)

        // ✅ REGLAS DE DETECCIÓN DE MANIPULACIÓN:

        // 1. Más de 100 giros es sospechoso
        if (currentSpins > 100) {
            Log.w(TAG, "🚨 Posible manipulación: Demasiados giros ($currentSpins)")
            return true
        }

        // 2. Si no hay Player ID pero hay muchos giros
        if (playerId.isEmpty() && currentSpins > 20) {
            Log.w(TAG, "🚨 Posible manipulación: Muchos giros sin Player ID")
            return true
        }

        // 3. Giros negativos (ya manejado en setCurrentSpins)
        if (currentSpins < 0) {
            Log.w(TAG, "🚨 Posible manipulación: Giros negativos")
            return true
        }

        return false
    }

    /**
     * Corrige datos manipulados
     */
    fun fixTamperedData(context: Context) {
        if (detectSpinTampering(context)) {
            Log.w(TAG, "🔧 Corrigiendo datos manipulados...")
            setCurrentSpins(context, 10) // Resetear a 10 giros seguros

            // Opcional: Marcar como sospechoso
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("TAMPER_DETECTED", true).apply()
        }
    }

    /**
     * Verifica si se detectó manipulación anteriormente
     */
    fun wasTamperDetected(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("TAMPER_DETECTED", false)
    }

    // ================ FUNCIONES DE ESTADÍSTICAS ================

    /**
     * Obtiene estadísticas de uso de giros
     */
    fun getSpinStats(context: Context): Map<String, Any> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return mapOf(
            "currentSpins" to getCurrentSpins(context),
            "totalSpinsUsed" to prefs.getInt("TOTAL_SPINS_USED", 0),
            "totalSpinsEarned" to prefs.getInt("TOTAL_SPINS_EARNED", 10), // 10 iniciales
            "lastSpinTime" to prefs.getLong("LAST_SPIN_TIME", 0),
            "hasPlayerId" to hasPlayerIdConfigured(context)
        )
    }

    /**
     * Registra el uso de un giro para estadísticas
     */
    fun recordSpinUsed(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalUsed = prefs.getInt("TOTAL_SPINS_USED", 0)
        prefs.edit()
            .putInt("TOTAL_SPINS_USED", totalUsed + 1)
            .putLong("LAST_SPIN_TIME", System.currentTimeMillis())
            .apply()

        Log.d(TAG, "📊 Giro registrado. Total usado: ${totalUsed + 1}")
    }

    /**
     * Registra giros ganados para estadísticas
     */
    fun recordSpinsEarned(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalEarned = prefs.getInt("TOTAL_SPINS_EARNED", 10) // 10 iniciales
        prefs.edit()
            .putInt("TOTAL_SPINS_EARNED", totalEarned + amount)
            .apply()

        Log.d(TAG, "📊 Giros ganados registrados: $amount. Total ganado: ${totalEarned + amount}")
    }
}