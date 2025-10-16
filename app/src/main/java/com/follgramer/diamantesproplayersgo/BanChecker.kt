package com.follgramer.diamantesproplayersgo

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BanChecker(private val context: Context) {

    sealed class BanStatus {
        object NotBanned : BanStatus()
        data class TemporaryBan(val reason: String, val expiresAt: Long) : BanStatus()
        data class PermanentBan(val reason: String) : BanStatus()
    }

    private val database = FirebaseDatabase.getInstance(
        "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
    ).reference

    fun checkBanStatus(playerId: String, onResult: (BanStatus) -> Unit) {
        if (playerId.isEmpty()) {
            onResult(BanStatus.NotBanned)
            return
        }

        database.child("bannedUsers").child(playerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onResult(BanStatus.NotBanned)
                        return
                    }

                    val banType = snapshot.child("type").getValue(String::class.java) ?: "temporary"
                    val reason = snapshot.child("reason").getValue(String::class.java)
                        ?: "Violación de términos de servicio"

                    when (banType) {
                        "permanent" -> {
                            onResult(BanStatus.PermanentBan(reason))
                        }
                        "temporary" -> {
                            val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java) ?: 0L

                            if (System.currentTimeMillis() < expiresAt) {
                                onResult(BanStatus.TemporaryBan(reason, expiresAt))
                            } else {
                                // Ban temporal expirado, eliminar de la base de datos
                                snapshot.ref.removeValue()
                                onResult(BanStatus.NotBanned)
                            }
                        }
                        else -> {
                            onResult(BanStatus.NotBanned)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // En caso de error, asumimos que no está baneado
                    onResult(BanStatus.NotBanned)
                }
            })
    }
}// Updated: 2025-10-15 14:29:27
