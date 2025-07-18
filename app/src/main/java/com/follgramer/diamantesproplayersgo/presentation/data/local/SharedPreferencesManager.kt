package com.follgramer.diamantesproplayersgo.data.local

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager {

    companion object {
        @Volatile
        private var INSTANCE: SharedPreferencesManager? = null
        private const val PREF_NAME = "com.follgramer.diamantesproplayersgo.preferences"

        // Keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_TICKETS = "tickets"
        private const val KEY_PASSES = "passes"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_LAST_SPIN_TIME = "last_spin_time"
        private const val KEY_WEEKLY_SPINS = "weekly_spins"

        fun getInstance(context: Context): SharedPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SharedPreferencesManager()
                instance.init(context)
                INSTANCE = instance
                instance
            }
        }
    }

    private lateinit var sharedPreferences: SharedPreferences

    private fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // For testing/simplified constructor
    constructor() {
        // Will be initialized later via getInstance
    }

    // User properties
    var userId: String
        get() = if (::sharedPreferences.isInitialized) sharedPreferences.getString(KEY_USER_ID, "") ?: "" else ""
        set(value) = if (::sharedPreferences.isInitialized) sharedPreferences.edit().putString(KEY_USER_ID, value).apply() else Unit

    var userName: String
        get() = if (::sharedPreferences.isInitialized) sharedPreferences.getString(KEY_USER_NAME, "") ?: "" else ""
        set(value) = if (::sharedPreferences.isInitialized) sharedPreferences.edit().putString(KEY_USER_NAME, value).apply() else Unit

    var tickets: Int
        get() = if (::sharedPreferences.isInitialized) sharedPreferences.getInt(KEY_TICKETS, 0) else 0
        set(value) = if (::sharedPreferences.isInitialized) sharedPreferences.edit().putInt(KEY_TICKETS, value).apply() else Unit

    var passes: Int
        get() = if (::sharedPreferences.isInitialized) sharedPreferences.getInt(KEY_PASSES, 0) else 0
        set(value) = if (::sharedPreferences.isInitialized) sharedPreferences.edit().putInt(KEY_PASSES, value).apply() else Unit

    // Settings
    var soundEnabled: Boolean
        get() = if (::sharedPreferences.isInitialized) sharedPreferences.getBoolean(KEY_SOUND_ENABLED, true) else true
        set(value) = if (::sharedPreferences.isInitialized) sharedPreferences.edit().putBoolean(KEY_SOUND_ENABLED, value).apply() else Unit

    var notificationsEnabled: Boolean
        get() = if (::sharedPreferences.isInitialized) sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true) else true
        set(value) = if (::sharedPreferences.isInitialized) sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply() else Unit

    // Game data
    var lastSpinTime: Long
        get() = if (::sharedPreferences.isInitialized) sharedPreferences.getLong(KEY_LAST_SPIN_TIME, 0) else 0
        set(value) = if (::sharedPreferences.isInitialized) sharedPreferences.edit().putLong(KEY_LAST_SPIN_TIME, value).apply() else Unit

    var weeklySpins: Int
        get() = if (::sharedPreferences.isInitialized) sharedPreferences.getInt(KEY_WEEKLY_SPINS, 0) else 0
        set(value) = if (::sharedPreferences.isInitialized) sharedPreferences.edit().putInt(KEY_WEEKLY_SPINS, value).apply() else Unit

    fun addTickets(amount: Int) {
        tickets += amount
    }

    fun useTicket(): Boolean {
        return if (tickets > 0) {
            tickets -= 1
            true
        } else {
            false
        }
    }

    fun resetWeeklyData() {
        weeklySpins = 0
        lastSpinTime = 0
    }

    fun canSpin(): Boolean {
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000
        return (currentTime - lastSpinTime) >= oneDayMillis
    }

    fun clearAllData() {
        if (::sharedPreferences.isInitialized) {
            sharedPreferences.edit().clear().apply()
        }
    }
}