package com.follgramer.diamantesproplayersgo

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val WINNER_CHANNEL_ID = "winner_notifications"
        private const val GENERAL_CHANNEL_ID = "general_notifications"
        private const val WINNER_NOTIFICATION_ID = 1001
        private const val LOSER_NOTIFICATION_ID = 1002
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM_MESSAGE", "📱 Mensaje FCM recibido")
        Log.d("FCM_MESSAGE", "From: ${remoteMessage.from}")
        Log.d("FCM_MESSAGE", "Data: ${remoteMessage.data}")
        Log.d("FCM_MESSAGE", "Notification: ${remoteMessage.notification}")

        // ✅ DETERMINAR ESTADO DE LA APP
        val isAppInForeground = isAppInForeground()
        Log.d("FCM_MESSAGE", "App en foreground: $isAppInForeground")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        if (data.isNotEmpty()) {
            val type = data["type"] ?: "general"
            val title = notification?.title ?: data["title"] ?: "Club de Recompensas"
            val body = notification?.body ?: data["body"] ?: "Nueva notificación"
            val message = data["message"] ?: body

            Log.d("FCM_MESSAGE", "Procesando: $type - $title")

            // ✅ SIEMPRE CREAR NOTIFICACIÓN (para garantizar que aparezca)
            when (type) {
                "win", "winner" -> sendWinnerNotification(title, message)
                "loss", "loser" -> sendLoserNotification(title, message)
                "test" -> sendTestNotification(title, message)
                else -> sendGeneralNotification(title, message)
            }
        } else if (notification != null) {
            // Si solo hay notification payload, crear notificación básica
            Log.d("FCM_MESSAGE", "Solo notification payload, creando notificación básica")
            sendGeneralNotification(
                notification.title ?: "Club de Recompensas",
                notification.body ?: "Nueva notificación"
            )
        } else {
            Log.w("FCM_MESSAGE", "Mensaje FCM sin data ni notification")
        }
    }

    private fun isAppInForeground(): Boolean {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false

            for (appProcess in appProcesses) {
                if (appProcess.processName == packageName &&
                    appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e("FCM_MESSAGE", "Error checking foreground state: ${e.message}")
            false
        }
    }

    private fun sendWinnerNotification(title: String, messageBody: String) {
        Log.d("FCM_WINNER", "Creando notificación de ganador")

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("notification_type", "winner")
            putExtra("message", messageBody)
            putExtra("redirect_to", "winners")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            WINNER_NOTIFICATION_ID,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        createNotificationChannel(WINNER_CHANNEL_ID, "Notificaciones de Ganadores", NotificationManager.IMPORTANCE_HIGH)

        val notificationBuilder = NotificationCompat.Builder(this, WINNER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🏆 ¡GANADOR!")
            .setContentText("¡Felicitaciones! Has ganado el sorteo.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#FFD700"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        try {
            notificationManager.notify(WINNER_NOTIFICATION_ID, notificationBuilder.build())
            Log.d("FCM_WINNER", "✅ Notificación de ganador enviada exitosamente")
        } catch (e: Exception) {
            Log.e("FCM_WINNER", "❌ Error enviando notificación de ganador: ${e.message}")
        }
    }

    private fun sendLoserNotification(title: String, messageBody: String) {
        Log.d("FCM_LOSER", "Creando notificación de sorteo finalizado")

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("notification_type", "loser")
            putExtra("message", messageBody)
            putExtra("redirect_to", "winners")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            LOSER_NOTIFICATION_ID,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        createNotificationChannel(GENERAL_CHANNEL_ID, "Notificaciones Generales", NotificationManager.IMPORTANCE_DEFAULT)

        val notificationBuilder = NotificationCompat.Builder(this, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🎭 Sorteo Finalizado")
            .setContentText("El sorteo ha finalizado. ¡Sigue participando!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#00A8FF"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        try {
            notificationManager.notify(LOSER_NOTIFICATION_ID, notificationBuilder.build())
            Log.d("FCM_LOSER", "✅ Notificación de sorteo enviada exitosamente")
        } catch (e: Exception) {
            Log.e("FCM_LOSER", "❌ Error enviando notificación de sorteo: ${e.message}")
        }
    }

    private fun sendTestNotification(title: String, messageBody: String) {
        Log.d("FCM_TEST", "Creando notificación de prueba")

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("notification_type", "test")
            putExtra("message", messageBody)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        createNotificationChannel(GENERAL_CHANNEL_ID, "Notificaciones Generales", NotificationManager.IMPORTANCE_DEFAULT)

        val notificationBuilder = NotificationCompat.Builder(this, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 $title")
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#FF9800"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d("FCM_TEST", "✅ Notificación de prueba enviada exitosamente")
        } catch (e: Exception) {
            Log.e("FCM_TEST", "❌ Error enviando notificación de prueba: ${e.message}")
        }
    }

    private fun sendGeneralNotification(title: String, messageBody: String) {
        Log.d("FCM_GENERAL", "Creando notificación general")

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("notification_type", "general")
            putExtra("message", messageBody)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        createNotificationChannel(GENERAL_CHANNEL_ID, "Notificaciones Generales", NotificationManager.IMPORTANCE_DEFAULT)

        val notificationBuilder = NotificationCompat.Builder(this, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#00A8FF"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d("FCM_GENERAL", "✅ Notificación general enviada exitosamente")
        } catch (e: Exception) {
            Log.e("FCM_GENERAL", "❌ Error enviando notificación general: ${e.message}")
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = "Notificaciones del club de recompensas"
                    enableLights(true)
                    lightColor = if (importance == NotificationManager.IMPORTANCE_HIGH) Color.YELLOW else Color.BLUE
                    enableVibration(true)
                    if (importance == NotificationManager.IMPORTANCE_HIGH) {
                        vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                    }
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        null
                    )
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d("FCM_CHANNEL", "✅ Canal $channelId creado con importancia $importance")
            } else {
                Log.d("FCM_CHANNEL", "Canal $channelId ya existe")
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM_TOKEN", "🔄 Nuevo token FCM generado: ${token.take(10)}...")
        sendRegistrationToServer(token)

        // Actualizar token en Firebase Database si hay un usuario activo
        try {
            val playerId = getSharedPreferences("DiamantesProPrefs", Context.MODE_PRIVATE)
                .getString("PLAYER_ID", "")

            if (!playerId.isNullOrEmpty()) {
                updateTokenInDatabase(playerId, token)
            } else {
                Log.d("FCM_TOKEN", "No hay playerId activo, token guardado localmente")
            }
        } catch (e: Exception) {
            Log.e("FCM_TOKEN", "❌ Error actualizando token: ${e.message}")
        }
    }

    private fun sendRegistrationToServer(token: String) {
        Log.d("FCM_TOKEN", "📤 Token enviado al sistema: ${token.take(10)}...")
    }

    private fun updateTokenInDatabase(playerId: String, token: String) {
        try {
            val tokenData = mapOf(
                "token" to token,
                "timestamp" to System.currentTimeMillis(),
                "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "androidVersion" to Build.VERSION.RELEASE,
                "lastUpdate" to System.currentTimeMillis()
            )

            com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("playerTokens")
                .child(playerId)
                .setValue(tokenData)
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN", "✅ Token actualizado en database para: $playerId")
                }
                .addOnFailureListener { error ->
                    Log.e("FCM_TOKEN", "❌ Error actualizando token en database: ${error.message}")
                }
        } catch (e: Exception) {
            Log.e("FCM_TOKEN", "❌ Error en updateTokenInDatabase: ${e.message}")
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w("FCM_MESSAGE", "⚠️ Mensajes FCM eliminados (demasiados pendientes)")
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d("FCM_MESSAGE", "✅ Mensaje FCM enviado: $msgId")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e("FCM_MESSAGE", "❌ Error enviando mensaje FCM $msgId: ${exception.message}")
    }
}