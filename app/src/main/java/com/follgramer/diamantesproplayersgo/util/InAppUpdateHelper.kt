package com.follgramer.diamantesproplayersgo.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.follgramer.diamantesproplayersgo.BuildConfig
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

object InAppUpdateHelper {
    private const val TAG = "InAppUpdateHelper"
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_LAST_CHECK = "last_update_check"
    private const val KEY_DISMISSED_VERSION = "dismissed_version"

    suspend fun check(activity: Activity) {
        try {
            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
            val now = System.currentTimeMillis()

            // ✅ Solo verificar cada 6 horas
            if (now - lastCheck < 6 * 60 * 60 * 1000) {
                Log.d(TAG, "Última verificación reciente, saltando...")
                return
            }

            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

            val database = FirebaseDatabase.getInstance(
                "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
            ).reference

            // ✅ Verificar si está habilitado
            val enabledSnapshot = database.child("appConfig")
                .child("update_check_enabled")
                .get()
                .await()

            val isEnabled = enabledSnapshot.getValue(Boolean::class.java) ?: false

            if (!isEnabled) {
                Log.d(TAG, "Update check está deshabilitado")
                return
            }

            // ✅ Obtener versión mínima requerida
            val minVersionSnapshot = database.child("appConfig")
                .child("minimum_version_code")
                .get()
                .await()

            val minVersion = minVersionSnapshot.getValue(Int::class.java) ?: 1

            val currentVersion = BuildConfig.VERSION_CODE

            Log.d(TAG, "Versión actual: $currentVersion, Versión mínima: $minVersion")

            if (currentVersion < minVersion) {
                // ✅ Verificar si es update forzado
                val forceSnapshot = database.child("appConfig")
                    .child("force_update")
                    .get()
                    .await()

                val isForced = forceSnapshot.getValue(Boolean::class.java) ?: false

                // ✅ Obtener mensaje personalizado
                val messageSnapshot = database.child("appConfig")
                    .child("update_message")
                    .get()
                    .await()

                val message = messageSnapshot.getValue(String::class.java)
                    ?: "Hay una nueva versión disponible con mejoras y correcciones."

                // ✅ Obtener URL de Play Store (por si cambia el package)
                val playStoreUrlSnapshot = database.child("appConfig")
                    .child("play_store_url")
                    .get()
                    .await()

                val playStoreUrl = playStoreUrlSnapshot.getValue(String::class.java)

                Log.d(TAG, "Actualización requerida. Forzada: $isForced")

                activity.runOnUiThread {
                    showUpdateDialog(activity, message, isForced, playStoreUrl)
                }
            } else {
                Log.d(TAG, "App está actualizada")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error verificando actualizaciones: ${e.message}", e)
        }
    }

    private fun showUpdateDialog(
        activity: Activity,
        message: String,
        isForced: Boolean,
        customPlayStoreUrl: String?
    ) {
        try {
            activity.runOnUiThread {
                MaterialDialog(activity).show {
                    title(text = if (isForced) "🚀 Actualización Requerida" else "🎉 Nueva Versión Disponible")
                    message(text = message)

                    positiveButton(text = "🔄 Actualizar Ahora") {
                        openPlayStore(activity, customPlayStoreUrl)

                        // ✅ Si es forzada, cerrar la app después de abrir Play Store
                        if (isForced) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                activity.finishAffinity()
                            }, 500)
                        }
                    }

                    if (!isForced) {
                        negativeButton(text = "Más tarde") {
                            Log.d(TAG, "Usuario postponió la actualización")
                        }
                    }

                    cancelable(!isForced)
                    cancelOnTouchOutside(!isForced)
                }
            }

            Log.d(TAG, "✅ Modal de actualización mostrado")
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando modal: ${e.message}")
        }
    }

    private fun openPlayStore(activity: Activity, customUrl: String?) {
        try {
            // ✅ Usar URL personalizada si existe
            val packageName = activity.packageName
            val marketUrl = customUrl ?: "market://details?id=$packageName"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(marketUrl)
                setPackage("com.android.vending")
            }

            activity.startActivity(intent)
            Log.d(TAG, "Play Store abierto (app)")

        } catch (e: Exception) {
            // ✅ Fallback a navegador web
            try {
                val webUrl = customUrl?.replace("market://", "https://play.google.com/store/apps/")
                    ?: "https://play.google.com/store/apps/details?id=${activity.packageName}"

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(webUrl)
                }
                activity.startActivity(intent)
                Log.d(TAG, "Play Store abierto (web)")
            } catch (e2: Exception) {
                Log.e(TAG, "Error abriendo Play Store: ${e2.message}")
            }
        }
    }

    /**
     * ✅ FUNCIÓN PARA TESTING: Forzar verificación
     */
    fun forceCheck(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_CHECK, 0).apply()
            Log.d(TAG, "Forzar verificación en próximo inicio")
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
            Log.d(TAG, "Estado de update reseteado para testing")
        } catch (e: Exception) {
            Log.e(TAG, "Error reseteando: ${e.message}")
        }
    }
}// Updated: 2025-10-15 14:29:27
