package com.follgramer.diamantesproplayersgo.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

object RatingPrompter {
    private const val TAG = "RatingPrompter"
    private const val PREFS_NAME = "rating_prefs"
    private const val KEY_APP_OPENS = "app_opens"
    private const val KEY_LAST_PROMPT = "last_prompt"
    private const val KEY_ALREADY_RATED = "already_rated"
    private const val KEY_NEVER_SHOW = "never_show_rating"

    // ✅ Configuración desde Firebase
    private var minOpens = 4 // Por defecto 4 sesiones
    private var daysBetweenPrompts = 7

    fun onAppStart(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val opens = prefs.getInt(KEY_APP_OPENS, 0) + 1
            prefs.edit().putInt(KEY_APP_OPENS, opens).apply()
            Log.d(TAG, "App opens: $opens")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onAppStart: ${e.message}")
        }
    }

    suspend fun maybeAsk(activity: Activity) {
        try {
            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // ✅ Verificar si está habilitado desde Firebase
            val database = FirebaseDatabase.getInstance(
                "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
            ).reference

            val configSnapshot = database.child("appConfig")
                .child("rating_prompt_enabled")
                .get()
                .await()

            val isEnabled = configSnapshot.getValue(Boolean::class.java) ?: true

            if (!isEnabled) {
                Log.d(TAG, "Rating prompt deshabilitado desde Firebase")
                return
            }

            // Obtener configuración dinámica
            val sessionsSnapshot = database.child("appConfig")
                .child("rating_sessions_threshold")
                .get()
                .await()

            minOpens = sessionsSnapshot.getValue(Int::class.java) ?: 4

            // Si ya calificó o dijo que nunca
            if (prefs.getBoolean(KEY_ALREADY_RATED, false) ||
                prefs.getBoolean(KEY_NEVER_SHOW, false)) {
                return
            }

            val opens = prefs.getInt(KEY_APP_OPENS, 0)
            val lastPrompt = prefs.getLong(KEY_LAST_PROMPT, 0)
            val daysSinceLastPrompt = (System.currentTimeMillis() - lastPrompt) / (1000 * 60 * 60 * 24)

            // Verificar si cumple condiciones
            if (opens >= minOpens && daysSinceLastPrompt >= daysBetweenPrompts) {
                activity.runOnUiThread {
                    showCustomRatingDialog(activity, prefs)
                }
                prefs.edit().putLong(KEY_LAST_PROMPT, System.currentTimeMillis()).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en maybeAsk: ${e.message}")
        }
    }

    /**
     * ✅ Modal personalizado con botones claros
     */
    private fun showCustomRatingDialog(activity: Activity, prefs: android.content.SharedPreferences) {
        try {
            MaterialDialog(activity).show {
                title(text = "⭐ ¿Te gusta nuestra app?")
                message(text = "Tu opinión es muy importante para nosotros. ¿Te gustaría calificarnos en Play Store?")

                positiveButton(text = "⭐ Calificar") {
                    // ✅ Usar Google In-App Review
                    val manager = ReviewManagerFactory.create(activity)
                    val request = manager.requestReviewFlow()

                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val flow = manager.launchReviewFlow(activity, reviewInfo)

                            flow.addOnCompleteListener {
                                prefs.edit().putBoolean(KEY_ALREADY_RATED, true).apply()
                                Log.d(TAG, "✅ Usuario completó la calificación")
                            }
                        } else {
                            Log.w(TAG, "⚠️ Review flow no disponible, abriendo Play Store")
                            openPlayStore(activity)
                            prefs.edit().putBoolean(KEY_ALREADY_RATED, true).apply()
                        }
                    }
                }

                negativeButton(text = "Más tarde") {
                    Log.d(TAG, "Usuario postponió la calificación")
                }

                neutralButton(text = "No volver a preguntar") {
                    prefs.edit().putBoolean(KEY_NEVER_SHOW, true).apply()
                    Log.d(TAG, "Usuario rechazó permanentemente")
                }

                cancelable(true)
                cancelOnTouchOutside(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando modal personalizado: ${e.message}")
        }
    }

    /**
     * ✅ Abrir Play Store directamente
     */
    private fun openPlayStore(activity: Activity) {
        try {
            val packageName = activity.packageName
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            activity.startActivity(intent)
            Log.d(TAG, "Play Store abierto (app)")
        } catch (e: Exception) {
            // Fallback a navegador
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
                }
                activity.startActivity(intent)
                Log.d(TAG, "Play Store abierto (web)")
            } catch (e2: Exception) {
                Log.e(TAG, "Error abriendo Play Store: ${e2.message}")
            }
        }
    }

    /**
     * ✅ Marcar como "nunca mostrar"
     */
    fun neverShowAgain(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_NEVER_SHOW, true).apply()
            Log.d(TAG, "Usuario marcó 'No volver a preguntar'")
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
        }
    }

    /**
     * ✅ FUNCIÓN PARA TESTING: Resetear estado
     */
    fun resetForTesting(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "Estado de rating reseteado para testing")
        } catch (e: Exception) {
            Log.e(TAG, "Error reseteando: ${e.message}")
        }
    }
}// Updated: 2025-10-15 14:29:27
