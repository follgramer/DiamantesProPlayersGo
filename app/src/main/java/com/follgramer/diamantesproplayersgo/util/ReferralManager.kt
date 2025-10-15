package com.follgramer.diamantesproplayersgo.util

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.follgramer.diamantesproplayersgo.SessionManager
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.*

object ReferralManager {
    private const val TAG = "ReferralManager"
    private lateinit var database: DatabaseReference

    fun initialize() {
        database = FirebaseDatabase.getInstance(
            "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
        ).reference
    }

    /**
     * Genera ID único del dispositivo (hash de Android ID + Advertising ID)
     */
    suspend fun getDeviceId(context: Context): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val advertisingInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            val advertisingId = advertisingInfo.id ?: "unknown"

            val combined = "$androidId:$advertisingId"
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(combined.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(16)

            Log.d(TAG, "Device ID generado: $hash")
            hash
        } catch (e: Exception) {
            Log.e(TAG, "Error generando Device ID: ${e.message}")
            UUID.randomUUID().toString().take(16)
        }
    }

    /**
     * Genera código único de referido para un jugador
     */
    fun generateReferralCode(playerId: String): String {
        return "REF${playerId.takeLast(4)}${System.currentTimeMillis().toString().takeLast(4)}"
    }

    /**
     * Crear/actualizar código de referido
     */
    suspend fun createReferralCode(playerId: String): String {
        return try {
            val code = generateReferralCode(playerId)

            val referralData = mapOf(
                "playerId" to playerId,
                "code" to code,
                "totalReferrals" to 0,
                "totalRewards" to 0,
                "createdAt" to ServerValue.TIMESTAMP,
                "active" to true
            )

            database.child("referralCodes").child(playerId).setValue(referralData).await()

            Log.d(TAG, "Código de referido creado: $code para jugador: $playerId")
            code
        } catch (e: Exception) {
            Log.e(TAG, "Error creando código: ${e.message}")
            ""
        }
    }

    /**
     * Obtener código de referido existente
     */
    suspend fun getReferralCode(playerId: String): String {
        return try {
            val snapshot = database.child("referralCodes").child(playerId).get().await()

            if (snapshot.exists()) {
                snapshot.child("code").getValue(String::class.java) ?: ""
            } else {
                createReferralCode(playerId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo código: ${e.message}")
            ""
        }
    }

    /**
     * Validar y procesar código de referido (para UI)
     */
    fun processReferralCode(activity: AppCompatActivity, referralCode: String) {
        activity.lifecycleScope.launch {
            try {
                val success = processReferralCodeAsync(activity, referralCode)
                Log.d(TAG, "Código procesado: $success")
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando código: ${e.message}")
            }
        }
    }

    /**
     * Función async para procesar código de referido
     */
    suspend fun processReferralCodeAsync(activity: AppCompatActivity, referralCode: String): Boolean {
        return try {
            val currentPlayerId = SessionManager.getPlayerId(activity)
            if (currentPlayerId.isEmpty()) {
                Log.w(TAG, "No hay player ID configurado")
                return false
            }

            val deviceId = getDeviceId(activity)

            // Verificar si el dispositivo ya fue referido
            val deviceCheck = database.child("referrals").child(deviceId).get().await()
            if (deviceCheck.exists()) {
                Log.w(TAG, "Dispositivo ya fue referido anteriormente")
                return false
            }

            // Buscar el código de referido
            val codesQuery = database.child("referralCodes")
                .orderByChild("code")
                .equalTo(referralCode)
                .get()
                .await()

            if (!codesQuery.exists()) {
                Log.w(TAG, "Código de referido no válido: $referralCode")
                return false
            }

            val referrerPlayerId = codesQuery.children.first().key ?: return false

            // No permitir auto-referencia
            if (referrerPlayerId == currentPlayerId) {
                Log.w(TAG, "No se puede usar tu propio código")
                return false
            }

            // Registrar el referido
            val referralData = mapOf(
                "referrerPlayerId" to referrerPlayerId,
                "referredPlayerId" to currentPlayerId,
                "referralCode" to referralCode,
                "timestamp" to ServerValue.TIMESTAMP,
                "deviceId" to deviceId,
                "rewardGranted" to false,
                "referredPlayerPasses" to 0
            )

            database.child("referrals").child(deviceId).setValue(referralData).await()

            // Actualizar contador del referidor
            database.child("referralCodes").child(referrerPlayerId).child("totalReferrals")
                .setValue(ServerValue.increment(1))

            Log.d(TAG, "Referido procesado exitosamente: $currentPlayerId por $referrerPlayerId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando referido: ${e.message}")
            false
        }
    }

    /**
     * Verificar y otorgar recompensa cuando referido alcanza 1 pase
     */
    suspend fun checkAndGrantReward(context: Context, referredPlayerId: String, currentPasses: Long) {
        try {
            if (currentPasses < 1) return

            val deviceId = getDeviceId(context)
            val referralRef = database.child("referrals").child(deviceId)
            val snapshot = referralRef.get().await()

            if (!snapshot.exists()) return

            val referralData = snapshot.value as? Map<String, Any> ?: return
            val storedReferredId = referralData["referredPlayerId"] as? String
            val rewardGranted = referralData["rewardGranted"] as? Boolean ?: false
            val previousPasses = referralData["referredPlayerPasses"] as? Long ?: 0L

            // Verificar que es el jugador correcto y no se ha otorgado recompensa
            if (storedReferredId == referredPlayerId && !rewardGranted && previousPasses < 1 && currentPasses >= 1) {
                val referrerPlayerId = referralData["referrerPlayerId"] as? String ?: return

                // Otorgar 1 pase al referidor
                database.child("players").child(referrerPlayerId).child("passes")
                    .setValue(ServerValue.increment(1))

                // Marcar recompensa como otorgada
                referralRef.child("rewardGranted").setValue(true)
                referralRef.child("referredPlayerPasses").setValue(currentPasses)
                referralRef.child("rewardTimestamp").setValue(ServerValue.TIMESTAMP)

                // Actualizar contador de recompensas
                database.child("referralCodes").child(referrerPlayerId).child("totalRewards")
                    .setValue(ServerValue.increment(1))

                Log.d(TAG, "Recompensa otorgada: $referrerPlayerId recibió 1 pase por referir a $referredPlayerId")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error verificando recompensa: ${e.message}")
        }
    }

    /**
     * Obtener estadísticas de referidos para un jugador
     */
    suspend fun getReferralStats(playerId: String): ReferralStats {
        return try {
            val codeSnapshot = database.child("referralCodes").child(playerId).get().await()

            if (codeSnapshot.exists()) {
                ReferralStats(
                    code = codeSnapshot.child("code").getValue(String::class.java) ?: "",
                    totalReferrals = codeSnapshot.child("totalReferrals").getValue(Long::class.java) ?: 0L,
                    totalRewards = codeSnapshot.child("totalRewards").getValue(Long::class.java) ?: 0L,
                    hasCode = true
                )
            } else {
                ReferralStats()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas: ${e.message}")
            ReferralStats()
        }
    }

    /**
     * Generar links de invitación
     */
    fun generateInviteLinks(referralCode: String): InviteLinks {
        val appLink = "diamantespro://refer?code=$referralCode"
        val webLink = "https://play.google.com/store/apps/details?id=com.follgramer.diamantesproplayersgo&referrer=$referralCode"

        return InviteLinks(appLink, webLink)
    }

    /**
     * Validar formato de código de referido
     */
    fun isValidReferralCode(code: String): Boolean {
        return code.isNotEmpty() &&
                code.startsWith("REF") &&
                code.length >= 8 &&
                code.length <= 12
    }

    /**
     * Limpiar códigos expirados (función de mantenimiento)
     */
    suspend fun cleanupExpiredCodes() {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)

            val expiredQuery = database.child("referralCodes")
                .orderByChild("createdAt")
                .endAt(thirtyDaysAgo.toDouble())
                .get()
                .await()

            for (snapshot in expiredQuery.children) {
                val totalReferrals = snapshot.child("totalReferrals").getValue(Long::class.java) ?: 0L
                if (totalReferrals == 0L) {
                    snapshot.ref.removeValue()
                    Log.d(TAG, "Código expirado eliminado: ${snapshot.key}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando códigos expirados: ${e.message}")
        }
    }

    /**
     * Data classes
     */
    data class ReferralStats(
        val code: String = "",
        val totalReferrals: Long = 0L,
        val totalRewards: Long = 0L,
        val hasCode: Boolean = false
    )

    data class InviteLinks(
        val appLink: String,
        val webLink: String
    )

    data class ReferralActivity(
        val referredPlayerId: String,
        val timestamp: Long,
        val rewardGranted: Boolean
    )
}