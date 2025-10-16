package com.follgramer.diamantesproplayersgo.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

object AdminModalManager {
    private const val TAG = "AdminModalManager"
    private const val PREFS_NAME = "admin_modal_prefs"
    private const val KEY_LAST_SEEN_MODAL_ID = "last_seen_modal_id"

    suspend fun fetchAndShow(activity: Activity) {
        try {
            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val database = FirebaseDatabase.getInstance(
                "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
            ).reference

            // ✅ Usar await() para convertir a coroutine
            val snapshot = database.child("appConfig")
                .child("adminModal")
                .get()
                .await()

            if (!snapshot.exists()) {
                Log.d(TAG, "No admin modal configured")
                return
            }

            // ✅ Verificar si está habilitado
            val enabled = snapshot.child("enabled").getValue(Boolean::class.java) ?: false

            if (!enabled) {
                Log.d(TAG, "Admin modal is disabled")
                return
            }

            // ✅ Obtener datos del modal
            val modalId = snapshot.child("id").getValue(String::class.java) ?: "modal_default"
            val title = snapshot.child("title").getValue(String::class.java) ?: "📢 Notificación"
            val message = snapshot.child("message").getValue(String::class.java) ?: ""
            val showOnce = snapshot.child("showOnce").getValue(Boolean::class.java) ?: true

            if (message.isEmpty()) {
                Log.d(TAG, "Modal message is empty")
                return
            }

            // ✅ Verificar si ya fue visto (solo si showOnce = true)
            if (showOnce) {
                val lastSeenId = prefs.getString(KEY_LAST_SEEN_MODAL_ID, "")
                if (lastSeenId == modalId) {
                    Log.d(TAG, "Modal already seen: $modalId")
                    return
                }
            }

            // ✅ Mostrar modal en UI thread
            activity.runOnUiThread {
                showModal(activity, title, message, modalId, showOnce, prefs)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchAndShow: ${e.message}", e)
        }
    }

    private fun showModal(
        activity: Activity,
        title: String,
        message: String,
        modalId: String,
        showOnce: Boolean,
        prefs: android.content.SharedPreferences
    ) {
        try {
            activity.runOnUiThread {
                MaterialDialog(activity).show {
                    title(text = title)
                    message(text = message)

                    positiveButton(text = "✅ Entendido") {
                        // ✅ Marcar como visto si showOnce = true
                        if (showOnce) {
                            prefs.edit().putString(KEY_LAST_SEEN_MODAL_ID, modalId).apply()
                            Log.d(TAG, "Modal marcado como visto: $modalId")
                        }
                    }

                    cancelable(false) // ✅ Usuario DEBE tocar el botón
                    cancelOnTouchOutside(false)
                }
            }

            Log.d(TAG, "✅ Modal mostrado: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing modal: ${e.message}", e)
        }
    }

    /**
     * ✅ FUNCIÓN PARA TESTING: Resetear modal visto
     */
    fun resetSeenModal(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_LAST_SEEN_MODAL_ID).apply()
            Log.d(TAG, "Modal reset - will show again")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting modal: ${e.message}")
        }
    }

    /**
     * ✅ FUNCIÓN PARA TESTING: Resetear estado completo
     */
    fun resetForTesting(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "Estado de admin modal reseteado para testing")
        } catch (e: Exception) {
            Log.e(TAG, "Error reseteando: ${e.message}")
        }
    }
}// Updated: 2025-10-15 14:29:27
