package com.follgramer.diamantesproplayersgo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    Log.d(TAG, "📱 Device boot completed")
                    handleBootCompleted(context)
                }
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_REPLACED -> {
                    Log.d(TAG, "📦 Package updated")
                    handlePackageUpdated(context, intent)
                }
                else -> {
                    Log.d(TAG, "🔔 Unknown action: ${intent.action}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in NotificationReceiver: ${e.message}")
        }
    }

    private fun handleBootCompleted(context: Context) {
        try {
            // Reinicializar notificaciones después del boot
            Log.d(TAG, "🔄 Reinitializing app after boot")

            // Opcional: Restart servicios si es necesario
            // startBackgroundServices(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling boot completed: ${e.message}")
        }
    }

    private fun handlePackageUpdated(context: Context, intent: Intent) {
        try {
            val data = intent.data
            if (data != null && data.schemeSpecificPart == context.packageName) {
                Log.d(TAG, "📱 Our app was updated")

                // Manejar actualización de la app si es necesario
                // clearOldCache(context)
                // updateConfigurationIfNeeded(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling package update: ${e.message}")
        }
    }
}// Updated: 2025-10-15 14:29:27
