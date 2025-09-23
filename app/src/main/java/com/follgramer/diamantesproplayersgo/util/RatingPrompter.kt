package com.follgramer.diamantesproplayersgo.util

import android.app.Activity
import android.content.Context
import android.util.Log

object RatingPrompter {
    private const val PREF = "rating_prefs"
    private const val KEY_LAUNCH = "launch_count"

    fun onAppStart(context: Context) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        p.edit().putInt(KEY_LAUNCH, p.getInt(KEY_LAUNCH, 0) + 1).apply()
        Log.d("RatingPrompter", "App start recorded")
    }

    fun maybeAsk(activity: Activity) {
        // Implementación simplificada para evitar errores
        // TODO: Implementar rating cuando sea necesario
        Log.d("RatingPrompter", "Rating check - implementación pendiente")
    }
}