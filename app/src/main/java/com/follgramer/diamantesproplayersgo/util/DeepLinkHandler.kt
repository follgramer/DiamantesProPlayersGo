package com.follgramer.diamantesproplayersgo.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.follgramer.diamantesproplayersgo.SessionManager

object DeepLinkHandler {
    private const val TAG = "DeepLinkHandler"

    fun handle(activity: AppCompatActivity, intent: Intent?) {
        try {
            intent?.data?.let { uri ->
                Log.d(TAG, "Deep link received: $uri")
                when (uri.scheme) {
                    "diamantespro" -> {
                        when (uri.host) {
                            "refer", "invite" -> {
                                val code = uri.getQueryParameter("code")
                                if (!code.isNullOrEmpty()) {
                                    handleReferralCode(activity, code)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling deep link: ${e.message}")
        }
    }

    fun checkPendingReferralCode(activity: AppCompatActivity) {
        try {
            val prefs = activity.getSharedPreferences("referral_prefs", Context.MODE_PRIVATE)
            val pendingCode = prefs.getString("pending_referral_code", null)
            if (!pendingCode.isNullOrEmpty()) {
                prefs.edit().remove("pending_referral_code").apply()

                // ✅ USAR lifecycleScope del activity
                activity.lifecycleScope.launch {
                    val success = ReferralManager.processReferralCodeAsync(activity, pendingCode)
                    Log.d(TAG, "Pending referral code processed: $success")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking pending referral: ${e.message}")
        }
    }

    private fun handleReferralCode(activity: AppCompatActivity, code: String) {
        try {
            val playerId = SessionManager.getPlayerId(activity)
            if (playerId.isEmpty()) {
                val prefs = activity.getSharedPreferences("referral_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("pending_referral_code", code).apply()
                Log.d(TAG, "Referral code saved for later: $code")
            } else {
                activity.lifecycleScope.launch {
                    val success = ReferralManager.processReferralCodeAsync(activity, code)
                    Log.d(TAG, "Referral code processed immediately: $success")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling referral code: ${e.message}")
        }
    }
}