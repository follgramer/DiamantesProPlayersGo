// ========================================
// ARCHIVO: BanChecker.kt
// ========================================
package com.follgramer.diamantesproplayersgo

import android.content.Context
import android.util.Log
import com.google.firebase.database.*

class BanChecker(private val context: Context) {

    companion object {
        private const val TAG = "BAN_CHECKER"
        private const val CONTACT_EMAIL = "contacto@follgramer.com"
    }

    fun checkBanStatus(playerId: String, onResult: (BanStatus) -> Unit) {
        if (playerId.isEmpty()) {
            onResult(BanStatus.NotBanned)
            return
        }

        Log.d(TAG, "🔍 Verificando estado de baneo para: $playerId")

        val database = FirebaseDatabase.getInstance("https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/").reference

        database.child("bannedUsers").child(playerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (!snapshot.exists()) {
                            Log.d(TAG, "✅ Usuario no baneado: $playerId")
                            onResult(BanStatus.NotBanned)
                            return
                        }

                        val banData = snapshot.value as? Map<String, Any>
                        if (banData == null) {
                            Log.w(TAG, "⚠️ Datos de baneo inválidos para: $playerId")
                            onResult(BanStatus.NotBanned)
                            return
                        }

                        val type = banData["type"] as? String ?: "permanent"
                        val reason = banData["reason"] as? String ?: "Violación de normas"
                        val timestamp = banData["timestamp"] as? Long ?: 0L
                        val expiresAt = banData["expiresAt"] as? Long

                        when (type) {
                            "temporary" -> {
                                if (expiresAt != null) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime < expiresAt) {
                                        Log.w(TAG, "⚠️ Usuario con advertencia temporal: $playerId")
                                        onResult(BanStatus.TemporaryBan(reason, expiresAt))
                                    } else {
                                        Log.d(TAG, "✅ Advertencia temporal expirada para: $playerId")
                                        // Limpiar baneo expirado
                                        removeExpiredBan(playerId)
                                        onResult(BanStatus.NotBanned)
                                    }
                                } else {
                                    Log.w(TAG, "⚠️ Advertencia temporal sin fecha de expiración: $playerId")
                                    onResult(BanStatus.NotBanned)
                                }
                            }
                            "permanent" -> {
                                Log.e(TAG, "🚫 Usuario baneado permanentemente: $playerId")
                                onResult(BanStatus.PermanentBan(reason))
                            }
                            else -> {
                                Log.w(TAG, "⚠️ Tipo de baneo desconocido: $type para $playerId")
                                onResult(BanStatus.NotBanned)
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error verificando baneo: ${e.message}")
                        onResult(BanStatus.NotBanned)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Error de Firebase: ${error.message}")
                    onResult(BanStatus.NotBanned)
                }
            })
    }

    private fun removeExpiredBan(playerId: String) {
        try {
            val database = FirebaseDatabase.getInstance("https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/").reference
            database.child("bannedUsers").child(playerId).removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Baneo expirado removido para: $playerId")
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "❌ Error removiendo baneo expirado: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en removeExpiredBan: ${e.message}")
        }
    }

    sealed class BanStatus {
        object NotBanned : BanStatus()
        data class TemporaryBan(val reason: String, val expiresAt: Long) : BanStatus()
        data class PermanentBan(val reason: String) : BanStatus()
    }
}