package com.follgramer.diamantesproplayersgo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.UUID

/**
 * SESSIONMANAGER CORREGIDO - GESTIÓN SEGURA DE DATOS
 * Maneja Player ID, giros, sesiones con validación anti-tamper
 * ✅ CORREGIDO: Uso de EncryptedSharedPreferences para mayor seguridad
 */
object SessionManager {
    private const val PREFS_NAME = "DiamantesProPrefs"
    private const val KEY_PLAYER_ID = "PLAYER_ID"
    private const val KEY_SESSION_ID = "SESSION_ID"
    private const val KEY_CURRENT_SPINS = "CURRENT_SPINS"
    private const val KEY_TOTAL_SPINS_USED = "TOTAL_SPINS_USED"
    private const val KEY_TOTAL_SPINS_EARNED = "TOTAL_SPINS_EARNED"
    private const val KEY_LAST_SPIN_TIME = "LAST_SPIN_TIME"
    private const val KEY_INTEGRITY_CHECK = "INTEGRITY_CHECK"
    private const val KEY_FIRST_LAUNCH = "FIRST_LAUNCH"
    private const val TAG = "SessionManager"

    // CONSTANTES DE SEGURIDAD
    private const val MAX_ALLOWED_SPINS = 100 // Límite anti-hack
    private const val DEFAULT_INITIAL_SPINS = 10 // Solo 10 giros iniciales
    private const val MIN_PLAYER_ID_LENGTH = 5

    // ✅ NUEVO: Cache para evitar múltiples accesos a SharedPreferences
    private var prefsCache: SharedPreferences? = null

    /**
     * ✅ CORREGIDO: Inicializa SessionManager con EncryptedSharedPreferences
     */
    fun init(context: Context) {
        try {
            Log.d(TAG, "SessionManager inicializado")

            // ✅ NUEVO: Inicializar EncryptedSharedPreferences solo una vez
            if (prefsCache == null) {
                prefsCache = getSecurePrefs(context)
            }

            val prefs = prefsCache!!

            // Verificar si es primera vez
            val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

            if (isFirstLaunch) {
                Log.d(TAG, "Primera inicialización - configurando valores por defecto")
                initializeForFirstTime(context)
            } else {
                // Validar integridad en launches subsecuentes
                validateAndFixData(context)
            }

            logCurrentState(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error en init: ${e.message}")
            // En caso de error, usar SharedPreferences normales como fallback
            initializeWithFallback(context)
        }
    }

    /**
     * ✅ NUEVO: Obtener SharedPreferences seguras con fallback
     */
    private fun getSecurePrefs(context: Context): SharedPreferences {
        return try {
            // ✅ CORREGIDO: Usar EncryptedSharedPreferences para mayor seguridad
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo crear EncryptedSharedPreferences, usando normales: ${e.message}")
            // Fallback a SharedPreferences normales
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * ✅ NUEVO: Inicialización con fallback para dispositivos antiguos
     */
    private fun initializeWithFallback(context: Context) {
        try {
            prefsCache = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            initializeForFirstTime(context)
            Log.d(TAG, "Inicializado con SharedPreferences normales (fallback)")
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en inicialización: ${e.message}")
        }
    }

    /**
     * Obtiene SharedPreferences de forma segura con cache
     */
    private fun getPrefs(context: Context): SharedPreferences {
        if (prefsCache == null) {
            prefsCache = getSecurePrefs(context)
        }
        return prefsCache!!
    }

    /**
     * Configuración inicial para primera vez
     */
    private fun initializeForFirstTime(context: Context) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()

        editor.putInt(KEY_CURRENT_SPINS, DEFAULT_INITIAL_SPINS)
        editor.putInt(KEY_TOTAL_SPINS_EARNED, DEFAULT_INITIAL_SPINS)
        editor.putInt(KEY_TOTAL_SPINS_USED, 0)
        editor.putLong(KEY_LAST_SPIN_TIME, 0)
        editor.putBoolean(KEY_FIRST_LAUNCH, false)
        editor.putString(KEY_INTEGRITY_CHECK, generateIntegrityHash(context))

        // ✅ CORREGIDO: Usar commit() en lugar de apply() para operaciones críticas
        val success = editor.commit()

        if (success) {
            Log.d(TAG, "Inicialización completada - $DEFAULT_INITIAL_SPINS giros establecidos")
        } else {
            Log.e(TAG, "Error guardando configuración inicial")
        }
    }

    /**
     * Obtiene el Player ID
     */
    fun getPlayerId(context: Context): String {
        val playerId = getPrefs(context).getString(KEY_PLAYER_ID, "") ?: ""
        Log.d(TAG, "Player ID obtenido: ${if (playerId.isEmpty()) "No configurado" else "***${playerId.takeLast(3)}"}")
        return playerId
    }

    /**
     * ✅ CORREGIDO: Configura el Player ID con validaciones mejoradas
     */
    fun setPlayerId(context: Context, playerId: String) {
        if (!isValidPlayerId(playerId)) {
            Log.w(TAG, "Player ID inválido: $playerId")
            throw IllegalArgumentException("Player ID debe tener al menos $MIN_PLAYER_ID_LENGTH dígitos")
        }

        val prefs = getPrefs(context)
        val previousPlayerId = prefs.getString(KEY_PLAYER_ID, "") ?: ""

        // ✅ CORREGIDO: Transacción atómica para cambios críticos
        val editor = prefs.edit()
        editor.putString(KEY_PLAYER_ID, playerId)

        val success = editor.commit()

        if (success) {
            Log.d(TAG, "Player ID configurado: ***${playerId.takeLast(3)}")

            // Generar nueva sesión si cambió el Player ID
            if (previousPlayerId != playerId && previousPlayerId.isNotEmpty()) {
                Log.d(TAG, "Player ID cambió - generando nueva sesión y reseteando giros")
                generateNewSession(context)
                resetSpinsForNewPlayer(context)
            } else if (previousPlayerId.isEmpty()) {
                Log.d(TAG, "Primera configuración de Player ID")
                generateNewSession(context)
            }

            updateIntegrityHash(context)
        } else {
            Log.e(TAG, "Error guardando Player ID")
            throw RuntimeException("No se pudo guardar el Player ID")
        }
    }

    /**
     * ✅ CORREGIDO: Validación mejorada de Player ID
     */
    private fun isValidPlayerId(playerId: String): Boolean {
        return try {
            playerId.length >= MIN_PLAYER_ID_LENGTH &&
                    playerId.matches(Regex("\\d+")) &&
                    playerId.length <= 15 && // Máximo razonable
                    !playerId.startsWith("0") && // No puede empezar con 0
                    playerId.toLongOrNull() != null // Debe ser numérico válido
        } catch (e: Exception) {
            Log.w(TAG, "Error validando Player ID: ${e.message}")
            false
        }
    }

    /**
     * Obtiene Session ID único
     */
    fun getSessionId(context: Context): String {
        val prefs = getPrefs(context)
        var sessionId = prefs.getString(KEY_SESSION_ID, "")

        if (sessionId.isNullOrEmpty()) {
            sessionId = generateNewSession(context)
        }

        return sessionId
    }

    /**
     * ✅ CORREGIDO: Generación de sesión más robusta
     */
    fun generateNewSession(context: Context): String {
        val newSessionId = UUID.randomUUID().toString()
        val prefs = getPrefs(context)

        val success = prefs.edit()
            .putString(KEY_SESSION_ID, newSessionId)
            .commit()

        if (success) {
            Log.d(TAG, "Nueva sesión generada: ${newSessionId.substring(0, 8)}...")
            return newSessionId
        } else {
            Log.e(TAG, "Error generando nueva sesión")
            return newSessionId // Retornar de todas formas
        }
    }

    // ================ GESTIÓN DE GIROS ================

    /**
     * ✅ CORREGIDO: Obtiene giros actuales con validación mejorada
     */
    fun getCurrentSpins(context: Context): Int {
        return try {
            val prefs = getPrefs(context)
            var spins = prefs.getInt(KEY_CURRENT_SPINS, DEFAULT_INITIAL_SPINS)

            // Validación anti-tamper mejorada
            when {
                spins < 0 -> {
                    Log.w(TAG, "Giros negativos detectados ($spins), corrigiendo a 0")
                    spins = 0
                    setCurrentSpins(context, spins)
                }
                spins > MAX_ALLOWED_SPINS -> {
                    Log.w(TAG, "Giros excesivos detectados ($spins), corrigiendo a $DEFAULT_INITIAL_SPINS")
                    spins = DEFAULT_INITIAL_SPINS
                    setCurrentSpins(context, spins)
                    recordTamperAttempt(context)
                }
            }

            Log.d(TAG, "Giros obtenidos: $spins")
            spins
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo giros: ${e.message}")
            DEFAULT_INITIAL_SPINS
        }
    }

    /**
     * ✅ CORREGIDO: Establece giros con transacción atómica
     */
    fun setCurrentSpins(context: Context, spins: Int) {
        val validatedSpins = when {
            spins < 0 -> {
                Log.w(TAG, "Intentando establecer giros negativos ($spins), estableciendo a 0")
                0
            }
            spins > MAX_ALLOWED_SPINS -> {
                Log.w(TAG, "Intentando establecer demasiados giros ($spins), limitando a $MAX_ALLOWED_SPINS")
                recordTamperAttempt(context)
                MAX_ALLOWED_SPINS
            }
            else -> spins
        }

        try {
            val success = getPrefs(context).edit()
                .putInt(KEY_CURRENT_SPINS, validatedSpins)
                .commit()

            if (success) {
                updateIntegrityHash(context)
                Log.d(TAG, "Giros establecidos: $validatedSpins")
            } else {
                Log.e(TAG, "Error estableciendo giros")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en setCurrentSpins: ${e.message}")
        }
    }

    /**
     * ✅ CORREGIDO: Añade giros con validación de límites
     */
    fun addSpins(context: Context, amount: Int) {
        if (amount <= 0 || amount > 50) {
            Log.w(TAG, "Cantidad de giros inválida: $amount")
            return
        }

        val currentSpins = getCurrentSpins(context)
        val newSpins = (currentSpins + amount).coerceAtMost(MAX_ALLOWED_SPINS)

        setCurrentSpins(context, newSpins)
        recordSpinsEarned(context, amount)

        Log.d(TAG, "Giros añadidos: $amount. Total: $currentSpins -> $newSpins")
    }

    /**
     * ✅ CORREGIDO: Usa un giro con validación atómica
     */
    fun useSpin(context: Context): Boolean {
        return try {
            val prefs = getPrefs(context)
            val currentSpins = getCurrentSpins(context)

            if (currentSpins <= 0) {
                Log.d(TAG, "No hay giros disponibles para usar")
                return false
            }

            val newSpins = currentSpins - 1
            val success = prefs.edit()
                .putInt(KEY_CURRENT_SPINS, newSpins)
                .commit()

            if (success) {
                recordSpinUsed(context)
                updateIntegrityHash(context)
                Log.d(TAG, "Giro usado. Restantes: $newSpins")
                true
            } else {
                Log.e(TAG, "Error usando giro")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en useSpin: ${e.message}")
            false
        }
    }

    /**
     * ✅ CORREGIDO: Reset atómico para nuevo jugador
     */
    fun resetSpinsForNewPlayer(context: Context) {
        try {
            val prefs = getPrefs(context)
            val editor = prefs.edit()

            editor.putInt(KEY_CURRENT_SPINS, DEFAULT_INITIAL_SPINS)
            editor.putInt(KEY_TOTAL_SPINS_USED, 0)
            editor.putInt(KEY_TOTAL_SPINS_EARNED, DEFAULT_INITIAL_SPINS)
            editor.putLong(KEY_LAST_SPIN_TIME, 0)

            val success = editor.commit()

            if (success) {
                updateIntegrityHash(context)
                Log.d(TAG, "Giros reseteados a $DEFAULT_INITIAL_SPINS para nuevo jugador")
            } else {
                Log.e(TAG, "Error reseteando giros")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en resetSpinsForNewPlayer: ${e.message}")
        }
    }

    /**
     * Verifica si tiene giros disponibles
     */
    fun hasSpinsAvailable(context: Context): Boolean {
        return getCurrentSpins(context) > 0
    }

    /**
     * Verifica si tiene Player ID configurado
     */
    fun hasPlayerIdConfigured(context: Context): Boolean {
        val playerId = getPlayerId(context)
        return isValidPlayerId(playerId)
    }

    // ================ FUNCIONES DE SEGURIDAD ================

    /**
     * ✅ CORREGIDO: Genera hash de integridad más robusto
     */
    private fun generateIntegrityHash(context: Context): String {
        return try {
            val prefs = getPrefs(context)
            val data = "${prefs.getInt(KEY_CURRENT_SPINS, 0)}_" +
                    "${prefs.getString(KEY_PLAYER_ID, "")}_" +
                    "${System.currentTimeMillis() / 86400000}_" + // Día actual
                    "${BuildConfig.VERSION_CODE}" // Versión de la app

            data.hashCode().toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error generando hash de integridad: ${e.message}")
            "default_hash"
        }
    }

    /**
     * Actualiza hash de integridad
     */
    private fun updateIntegrityHash(context: Context) {
        try {
            val newHash = generateIntegrityHash(context)
            getPrefs(context).edit()
                .putString(KEY_INTEGRITY_CHECK, newHash)
                .apply() // Aquí sí podemos usar apply() porque no es crítico
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando hash de integridad: ${e.message}")
        }
    }

    /**
     * ✅ CORREGIDO: Validación y corrección más robusta
     */
    private fun validateAndFixData(context: Context) {
        try {
            val prefs = getPrefs(context)

            // Verificar giros
            val spins = prefs.getInt(KEY_CURRENT_SPINS, DEFAULT_INITIAL_SPINS)
            if (spins < 0 || spins > MAX_ALLOWED_SPINS) {
                Log.w(TAG, "Datos corruptos detectados - giros: $spins")
                setCurrentSpins(context, DEFAULT_INITIAL_SPINS)
            }

            // Verificar Player ID
            val playerId = prefs.getString(KEY_PLAYER_ID, "") ?: ""
            if (playerId.isNotEmpty() && !isValidPlayerId(playerId)) {
                Log.w(TAG, "Player ID corrupto detectado, limpiando")
                prefs.edit().remove(KEY_PLAYER_ID).commit()
            }

            // Verificar Session ID
            val sessionId = prefs.getString(KEY_SESSION_ID, "") ?: ""
            if (sessionId.isNotEmpty()) {
                try {
                    UUID.fromString(sessionId)
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Session ID corrupto, generando nuevo")
                    generateNewSession(context)
                }
            }

            // Verificar estadísticas
            val totalUsed = prefs.getInt(KEY_TOTAL_SPINS_USED, 0)
            val totalEarned = prefs.getInt(KEY_TOTAL_SPINS_EARNED, DEFAULT_INITIAL_SPINS)

            if (totalUsed < 0 || totalEarned < 0) {
                Log.w(TAG, "Estadísticas corruptas, corrigiendo")
                prefs.edit()
                    .putInt(KEY_TOTAL_SPINS_USED, 0)
                    .putInt(KEY_TOTAL_SPINS_EARNED, DEFAULT_INITIAL_SPINS)
                    .commit()
            }

            updateIntegrityHash(context)
            Log.d(TAG, "Validación de datos completada")

        } catch (e: Exception) {
            Log.e(TAG, "Error validando datos: ${e.message}")
            // En caso de error grave, resetear todo
            clearAllData(context)
            initializeForFirstTime(context)
        }
    }

    /**
     * ✅ CORREGIDO: Registro de intentos de manipulación más detallado
     */
    private fun recordTamperAttempt(context: Context) {
        try {
            val prefs = getPrefs(context)
            val attempts = prefs.getInt("TAMPER_ATTEMPTS", 0)
            val newAttempts = attempts + 1

            prefs.edit()
                .putInt("TAMPER_ATTEMPTS", newAttempts)
                .putLong("LAST_TAMPER_TIME", System.currentTimeMillis())
                .putString("TAMPER_BUILD", BuildConfig.VERSION_NAME)
                .apply()

            Log.w(TAG, "Intento de manipulación registrado (#$newAttempts)")

            // Si hay demasiados intentos, resetear datos
            if (newAttempts >= 5) {
                Log.e(TAG, "Demasiados intentos de manipulación, reseteando datos")
                clearAllData(context)
                initializeForFirstTime(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando intento de manipulación: ${e.message}")
        }
    }

    /**
     * Detecta manipulación de datos
     */
    fun detectTampering(context: Context): Boolean {
        return try {
            val prefs = getPrefs(context)
            val attempts = prefs.getInt("TAMPER_ATTEMPTS", 0)
            attempts > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error detectando manipulación: ${e.message}")
            false
        }
    }

    // ================ ESTADÍSTICAS ================

    /**
     * Registra uso de giro
     */
    private fun recordSpinUsed(context: Context) {
        try {
            val prefs = getPrefs(context)
            val totalUsed = prefs.getInt(KEY_TOTAL_SPINS_USED, 0)
            prefs.edit()
                .putInt(KEY_TOTAL_SPINS_USED, totalUsed + 1)
                .putLong(KEY_LAST_SPIN_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando uso de giro: ${e.message}")
        }
    }

    /**
     * Registra giros ganados
     */
    private fun recordSpinsEarned(context: Context, amount: Int) {
        try {
            val prefs = getPrefs(context)
            val totalEarned = prefs.getInt(KEY_TOTAL_SPINS_EARNED, DEFAULT_INITIAL_SPINS)
            prefs.edit()
                .putInt(KEY_TOTAL_SPINS_EARNED, totalEarned + amount)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando giros ganados: ${e.message}")
        }
    }

    /**
     * Obtiene estadísticas de uso
     */
    fun getSpinStats(context: Context): Map<String, Any> {
        return try {
            val prefs = getPrefs(context)
            mapOf(
                "currentSpins" to getCurrentSpins(context),
                "totalSpinsUsed" to prefs.getInt(KEY_TOTAL_SPINS_USED, 0),
                "totalSpinsEarned" to prefs.getInt(KEY_TOTAL_SPINS_EARNED, DEFAULT_INITIAL_SPINS),
                "lastSpinTime" to prefs.getLong(KEY_LAST_SPIN_TIME, 0),
                "hasPlayerId" to hasPlayerIdConfigured(context),
                "tamperAttempts" to prefs.getInt("TAMPER_ATTEMPTS", 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas: ${e.message}")
            emptyMap()
        }
    }

    // ================ LIMPIEZA Y DEBUGGING ================

    /**
     * ✅ CORREGIDO: Limpieza segura de todos los datos
     */
    fun clearAllData(context: Context) {
        try {
            val success = getPrefs(context).edit().clear().commit()
            if (success) {
                Log.d(TAG, "Todos los datos limpiados")
                prefsCache = null // Limpiar cache también
            } else {
                Log.e(TAG, "Error limpiando datos")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en clearAllData: ${e.message}")
        }
    }

    /**
     * Limpia solo la sesión
     */
    fun clearSession(context: Context) {
        try {
            getPrefs(context).edit()
                .remove(KEY_SESSION_ID)
                .commit()
            Log.d(TAG, "Sesión limpiada")
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando sesión: ${e.message}")
        }
    }

    /**
     * ✅ CORREGIDO: Información completa para debugging con manejo de errores
     */
    fun getSessionInfo(context: Context): String {
        return try {
            val prefs = getPrefs(context)
            val playerId = getPlayerId(context)
            val sessionId = getSessionId(context)
            val spins = getCurrentSpins(context)
            val stats = getSpinStats(context)

            """
            SessionManager Info:
            - Player ID: ${if (playerId.isEmpty()) "No configurado" else "***${playerId.takeLast(3)}"}
            - Session ID: ${sessionId.substring(0, 8)}...
            - Giros actuales: $spins
            - Player configurado: ${hasPlayerIdConfigured(context)}
            - Total usado: ${stats["totalSpinsUsed"]}
            - Total ganado: ${stats["totalSpinsEarned"]}
            - Intentos tamper: ${stats["tamperAttempts"]}
            - Primera vez: ${prefs.getBoolean(KEY_FIRST_LAUNCH, true)}
            - Tipo SharedPrefs: ${if (prefsCache is EncryptedSharedPreferences) "Encrypted" else "Normal"}
            """.trimIndent()
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo info de sesión: ${e.message}")
            "Error obteniendo información de sesión"
        }
    }

    /**
     * Log del estado actual
     */
    private fun logCurrentState(context: Context) {
        try {
            val playerId = getPlayerId(context)
            val spins = getCurrentSpins(context)

            Log.d(TAG, "Estado actual - Player ID: ${
                if (playerId.isEmpty()) "No configurado" else "Configurado"
            }, Giros: $spins")
        } catch (e: Exception) {
            Log.e(TAG, "Error logueando estado: ${e.message}")
        }
    }

    /**
     * Reset completo para testing - SOLO DEBUG
     */
    fun resetForTesting(context: Context) {
        if (!BuildConfig.DEBUG) {
            Log.w(TAG, "resetForTesting solo disponible en DEBUG")
            return
        }

        try {
            Log.w(TAG, "RESETEO COMPLETO PARA TESTING")
            clearAllData(context)
            initializeForFirstTime(context)

            // Limpiar flags adicionales
            val prefs = getPrefs(context)
            prefs.edit()
                .remove("HAS_SHOWN_WELCOME")
                .remove("CONSENT_ACCEPTED")
                .commit()

            Log.w(TAG, "Reset completo terminado")
        } catch (e: Exception) {
            Log.e(TAG, "Error en resetForTesting: ${e.message}")
        }
    }
}// Updated: 2025-10-15 14:29:27
