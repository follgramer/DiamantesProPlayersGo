package com.follgramer.diamantesproplayersgo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NOTIFICATION_RECEIVER", "Received: ${intent.action}")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("NOTIFICATION_RECEIVER", "Device rebooted")
        }
    }
}