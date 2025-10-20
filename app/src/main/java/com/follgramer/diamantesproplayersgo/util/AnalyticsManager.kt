package com.follgramer.diamantesproplayersgo.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsManager {
    private const val TAG = "AnalyticsManager"
    private lateinit var analytics: FirebaseAnalytics
    private var isInitialized = false

    fun initialize(context: Context) {
        try {
            analytics = Firebase.analytics
            isInitialized = true
            Log.d(TAG, "✅ Analytics inicializado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando Analytics: ${e.message}")
        }
    }

    // ==================== EVENTOS DE APP ====================

    fun logAppOpened() {
        logEvent("app_opened", Bundle())
    }

    fun logPlayerIdConfigured(playerId: String) {
        logEvent("player_id_configured", Bundle().apply {
            putString("player_id_length", playerId.length.toString())
        })
    }

    // ==================== EVENTOS DE SPIN ====================

    fun logSpinCompleted(ticketsWon: Int) {
        logEvent("spin_completed", Bundle().apply {
            putInt("tickets_won", ticketsWon)
            putLong("timestamp", System.currentTimeMillis())
        })
    }

    fun logSpinsEarned(amount: Int, source: String) {
        logEvent("spins_earned", Bundle().apply {
            putInt("amount", amount)
            putString("source", source) // "rewarded_ad", "gift", etc.
        })
    }

    fun logSpinFailed(reason: String) {
        logEvent("spin_failed", Bundle().apply {
            putString("reason", reason)
        })
    }

    // ==================== EVENTOS DE ANUNCIOS ====================

    fun logAdImpression(adType: String, adUnitId: String) {
        logEvent("ad_impression", Bundle().apply {
            putString("ad_type", adType) // "banner", "interstitial", "rewarded"
            putString("ad_unit_id", adUnitId)
        })
    }

    fun logAdClicked(adType: String) {
        logEvent("ad_clicked", Bundle().apply {
            putString("ad_type", adType)
        })
    }

    fun logRewardedAdCompleted(reward: Int, rewardType: String) {
        logEvent("rewarded_ad_completed", Bundle().apply {
            putInt("reward_amount", reward)
            putString("reward_type", rewardType)
        })
    }

    fun logAdLoadFailed(adType: String, errorCode: Int) {
        logEvent("ad_load_failed", Bundle().apply {
            putString("ad_type", adType)
            putInt("error_code", errorCode)
        })
    }

    // ==================== EVENTOS DE NAVEGACIÓN ====================

    fun logScreenView(screenName: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        })
    }

    fun logNavigationClicked(destination: String) {
        logEvent("navigation_clicked", Bundle().apply {
            putString("destination", destination)
        })
    }

    // ==================== EVENTOS DE TICKETS Y PASES ====================

    fun logTicketsEarned(amount: Int, source: String) {
        logEvent("tickets_earned", Bundle().apply {
            putInt("amount", amount)
            putString("source", source)
        })
    }

    fun logPassEarned() {
        logEvent("pass_earned", Bundle())
    }

    fun logMilestoneReached(milestone: String, value: Int) {
        logEvent("milestone_reached", Bundle().apply {
            putString("milestone_type", milestone) // "tickets", "passes", "spins"
            putInt("value", value)
        })
    }

    // ==================== EVENTOS DE SORTEO ====================

    fun logDrawStarted() {
        logEvent("draw_started", Bundle())
    }

    fun logDrawCompleted(winnerId: String, prizeType: String) {
        logEvent("draw_completed", Bundle().apply {
            putString("winner_id_length", winnerId.length.toString())
            putString("prize_type", prizeType)
        })
    }

    fun logUserWonDraw(prize: String) {
        logEvent("user_won_draw", Bundle().apply {
            putString("prize", prize)
        })
    }

    // ==================== EVENTOS DE NOTIFICACIONES ====================

    fun logNotificationReceived(notificationType: String) {
        logEvent("notification_received", Bundle().apply {
            putString("notification_type", notificationType)
        })
    }

    fun logNotificationOpened(notificationType: String) {
        logEvent("notification_opened", Bundle().apply {
            putString("notification_type", notificationType)
        })
    }

    // ==================== EVENTOS DE ENGAGEMENT ====================

    fun logSessionDuration(durationMs: Long) {
        logEvent("session_duration", Bundle().apply {
            putLong("duration_ms", durationMs)
            putLong("duration_minutes", durationMs / 60000)
        })
    }

    fun logUserRetention(daysActive: Int) {
        logEvent("user_retention", Bundle().apply {
            putInt("days_active", daysActive)
        })
    }

    fun logFeatureUsed(featureName: String) {
        logEvent("feature_used", Bundle().apply {
            putString("feature_name", featureName)
        })
    }

    // ==================== EVENTOS DE REFERIDOS ====================

    fun logReferralAccepted(referralCode: String, referrerId: String) {
        logEvent("referral_accepted", Bundle().apply {
            putString("referral_code", referralCode)
            putString("referrer_id", referrerId)
        })
    }

    fun logReferralBonusGranted(playerId: String, bonusAmount: Int) {
        logEvent("referral_bonus_granted", Bundle().apply {
            putString("player_id_length", playerId.length.toString())
            putInt("bonus_amount", bonusAmount)
        })
    }

    // ==================== EVENTOS DE ERRORS ====================

    fun logError(errorType: String, errorMessage: String) {
        logEvent("app_error", Bundle().apply {
            putString("error_type", errorType)
            putString("error_message", errorMessage.take(100))
        })
    }

    fun logCriticalError(errorType: String, stackTrace: String) {
        logEvent("critical_error", Bundle().apply {
            putString("error_type", errorType)
            putString("stack_trace", stackTrace.take(500))
        })
    }

    // ==================== FUNCIÓN GENÉRICA ====================

    // ✅ CAMBIAR A internal PARA QUE SEA ACCESIBLE EN EL MISMO MÓDULO
    internal fun logEvent(eventName: String, params: Bundle) {
        if (!isInitialized) {
            Log.w(TAG, "⚠️ Analytics no inicializado, evento omitido: $eventName")
            return
        }

        try {
            analytics.logEvent(eventName, params)
            Log.d(TAG, "📊 Evento registrado: $eventName")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error registrando evento $eventName: ${e.message}")
        }
    }

    // ==================== USER PROPERTIES ====================

    fun setUserProperty(propertyName: String, value: String) {
        if (!isInitialized) return

        try {
            analytics.setUserProperty(propertyName, value)
            Log.d(TAG, "👤 Propiedad establecida: $propertyName = $value")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error estableciendo propiedad: ${e.message}")
        }
    }

    fun setUserId(userId: String) {
        if (!isInitialized) return

        try {
            analytics.setUserId(userId)
            Log.d(TAG, "👤 User ID establecido")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error estableciendo User ID: ${e.message}")
        }
    }
}// Updated: 2025-10-15 14:29:27
