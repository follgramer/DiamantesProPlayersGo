package com.follgramer.diamantesproplayersgo

import android.content.Context
import android.os.Build
import android.util.Log
import com.follgramer.diamantesproplayersgo.notifications.AppNotificationManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        private const val PREFS_NAME = "DiamantesProPrefs"
        private const val KEY_PLAYER_ID = "PLAYER_ID"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firebaseDb by lazy {
        FirebaseDatabase.getInstance("https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/").reference
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 Nuevo token FCM: ${token.take(20)}...")

        // Guardar token temporalmente
        getSharedPreferences("fcm_prefs", MODE_PRIVATE).edit()
            .putString("fcm_token", token)
            .putLong("token_timestamp", System.currentTimeMillis())
            .apply()

        // Intentar actualizar en Firebase
        val playerId = getCurrentPlayerId()
        if (playerId.isNotEmpty()) {
            updateTokenInFirebase(playerId, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        try {
            Log.d(TAG, "📨 Mensaje FCM recibido")
            Log.d(TAG, "From: ${message.from}")
            Log.d(TAG, "Data: ${message.data}")
            Log.d(TAG, "Notification: ${message.notification}")

            val playerId = getCurrentPlayerId()
            val data = message.data
            val notification = message.notification

            // Determinar el tipo de notificación
            val type = data["type"] ?: when {
                data.containsKey("winner_id") -> "loser"
                data.containsKey("ban_type") -> "ban"
                data.containsKey("amount") && data.containsKey("unit") -> "gift"
                notification?.title?.contains("GANADOR", ignoreCase = true) == true -> "winner"
                notification?.title?.contains("Sorteo", ignoreCase = true) == true -> "loser"
                else -> "general"
            }

            // Procesar según el tipo
            when (type) {
                "winner", "win" -> processWinnerNotification(message, playerId)
                "loser", "loss" -> processLoserNotification(message, playerId)
                "gift" -> processGiftNotification(message, playerId)
                "ban" -> processBanNotification(message, playerId)
                "unban" -> processUnbanNotification(message, playerId)
                "weekly_draw" -> processWeeklyDrawNotification(message, playerId)
                "test" -> processTestNotification(message, playerId)
                else -> processGeneralNotification(message, playerId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error procesando mensaje FCM: ${e.message}", e)
        }
    }

    private fun processWinnerNotification(message: RemoteMessage, playerId: String) {
        val data = message.data
        val notification = message.notification

        val title = data["title"] ?: notification?.title ?: "🏆 ¡FELICITACIONES, GANADOR!"
        val body = data["body"] ?: data["message"] ?: notification?.body
        ?: "¡Has ganado el sorteo semanal! Te contactaremos pronto."
        val prize = data["prize"] ?: "Premio Sorpresa"

        // Guardar en notificationQueue si tenemos playerId
        if (playerId.isNotEmpty()) {
            saveToNotificationQueue(
                playerId = playerId,
                type = "winner",
                title = title,
                body = body,
                message = "$body\nPremio: $prize",
                extraData = mapOf("prize" to prize)
            )
        }

        // Mostrar a través del AppNotificationManager
        showThroughNotificationManager(
            type = "winner",
            title = title,
            message = "$body\nPremio: $prize",
            data = mapOf("prize" to prize)
        )
    }

    private fun processLoserNotification(message: RemoteMessage, playerId: String) {
        val data = message.data
        val notification = message.notification

        val title = data["title"] ?: notification?.title ?: "📢 Sorteo Finalizado"
        val body = data["body"] ?: data["message"] ?: notification?.body
        ?: "El sorteo ha finalizado. ¡Sigue participando!"
        val winnerId = data["winner_id"] ?: ""

        val finalMessage = if (winnerId.isNotEmpty()) {
            "$body\nGanador: $winnerId"
        } else body

        if (playerId.isNotEmpty()) {
            saveToNotificationQueue(
                playerId = playerId,
                type = "loser",
                title = title,
                body = body,
                message = finalMessage,
                extraData = mapOf("winner_id" to winnerId)
            )
        }

        showThroughNotificationManager(
            type = "loser",
            title = title,
            message = finalMessage,
            data = mapOf("winner_id" to winnerId)
        )
    }

    private fun processGiftNotification(message: RemoteMessage, playerId: String) {
        val data = message.data
        val notification = message.notification

        val title = data["title"] ?: notification?.title ?: "🎁 Regalo Recibido"
        val amount = data["amount"] ?: "0"
        val unit = data["unit"] ?: "tickets"
        val customMessage = data["message"] ?: notification?.body ?: ""

        val body = when {
            customMessage.isNotEmpty() -> customMessage
            unit == "passes" -> "¡Has recibido $amount ${if (amount == "1") "pase" else "pases"}!"
            unit == "tickets" -> "¡Has recibido $amount tickets!"
            unit == "spins" -> "¡Has recibido $amount giros!"
            else -> "¡Tienes un regalo del administrador!"
        }

        if (playerId.isNotEmpty()) {
            saveToNotificationQueue(
                playerId = playerId,
                type = "gift",
                title = title,
                body = body,
                message = body,
                extraData = mapOf("amount" to amount, "unit" to unit)
            )
        }

        showThroughNotificationManager(
            type = "gift",
            title = title,
            message = body,
            data = mapOf("amount" to amount, "unit" to unit)
        )
    }

    private fun processBanNotification(message: RemoteMessage, playerId: String) {
        val data = message.data
        val notification = message.notification

        val banType = data["ban_type"] ?: "temporary"
        val reason = data["reason"] ?: data["message"] ?: notification?.body ?: "Violación de términos"
        val expiresAt = data["expires_at"] ?: "0"

        val title = if (banType == "permanent") {
            "🚫 Cuenta Suspendida Permanentemente"
        } else {
            "⚠️ Suspensión Temporal"
        }

        if (playerId.isNotEmpty()) {
            saveToNotificationQueue(
                playerId = playerId,
                type = "ban",
                title = title,
                body = reason,
                message = reason,
                extraData = mapOf(
                    "ban_type" to banType,
                    "expires_at" to expiresAt
                )
            )
        }

        showThroughNotificationManager(
            type = "ban",
            title = title,
            message = reason,
            data = mapOf(
                "ban_type" to banType,
                "expires_at" to expiresAt
            )
        )
    }

    private fun processUnbanNotification(message: RemoteMessage, playerId: String) {
        val data = message.data
        val notification = message.notification

        val title = data["title"] ?: notification?.title ?: "✅ Cuenta Reactivada"
        val body = data["body"] ?: data["message"] ?: notification?.body
        ?: "Tu cuenta ha sido reactivada. ¡Bienvenido de vuelta!"

        if (playerId.isNotEmpty()) {
            saveToNotificationQueue(
                playerId = playerId,
                type = "unban",
                title = title,
                body = body,
                message = body
            )
        }

        showThroughNotificationManager(
            type = "unban",
            title = title,
            message = body
        )
    }

    private fun processWeeklyDrawNotification(message: RemoteMessage, playerId: String) {
        val data = message.data
        val notification = message.notification

        val status = data["status"] ?: "update"
        val title = data["title"] ?: notification?.title ?: "🎰 Sorteo Semanal"
        val body = data["body"] ?: data["message"] ?: notification?.body ?: ""

        val finalTitle = when (status) {
            "starting" -> "🎲 ¡Sorteo Iniciando!"
            "completed" -> "🏆 Sorteo Completado"
            else -> title
        }

        val finalMessage = when (status) {
            "starting" -> body.ifEmpty { "El sorteo semanal está por comenzar" }
            "completed" -> {
                val winnerId = data["winner_id"] ?: ""
                if (winnerId.isNotEmpty()) {
                    "Ganador: $winnerId\n$body"
                } else body
            }
            else -> body
        }

        if (playerId.isNotEmpty()) {
            saveToNotificationQueue(
                playerId = playerId,
                type = "weekly_draw",
                title = finalTitle,
                body = finalMessage,
                message = finalMessage,
                extraData = mapOf("status" to status)
            )
        }

        showThroughNotificationManager(
            type = "weekly_draw",
            title = finalTitle,
            message = finalMessage,
            data = mapOf("status" to status)
        )
    }

    private fun processTestNotification(message: RemoteMessage, playerId: String) {
        val data = message.data
        val notification = message.notification

        val title = data["title"] ?: notification?.title ?: "🧪 Notificación de Prueba"
        val body = data["body"] ?: data["message"] ?: notification?.body ?: "Esta es una prueba"

        if (playerId.isNotEmpty()) {
            saveToNotificationQueue(
                playerId = playerId,
                type = "test",
                title = title,
                body = body,
                message = body
            )
        }

        showThroughNotificationManager(
            type = "test",
            title = title,
            message = body
        )
    }

    private fun processGeneralNotification(message: RemoteMessage, playerId: String) {
        val data = message.data
        val notification = message.notification

        val title = data["title"] ?: notification?.title ?: "📢 Notificación"
        val body = data["body"] ?: data["message"] ?: notification?.body ?: ""

        if (body.isNotEmpty()) {
            if (playerId.isNotEmpty()) {
                saveToNotificationQueue(
                    playerId = playerId,
                    type = "general",
                    title = title,
                    body = body,
                    message = body
                )
            }

            showThroughNotificationManager(
                type = "general",
                title = title,
                message = body
            )
        }
    }

    private fun saveToNotificationQueue(
        playerId: String,
        type: String,
        title: String,
        body: String,
        message: String,
        extraData: Map<String, String> = emptyMap()
    ) {
        serviceScope.launch {
            try {
                val notificationId = "${System.currentTimeMillis()}_${type}_${(0..9999).random()}"

                val notificationData = mutableMapOf(
                    "type" to type,
                    "title" to title,
                    "body" to body,
                    "message" to message,
                    "timestamp" to ServerValue.TIMESTAMP,
                    "processed" to false
                )

                // Agregar datos extra si existen
                extraData.forEach { (key, value) ->
                    notificationData[key] = value
                }

                firebaseDb.child("notificationQueue")
                    .child(playerId)
                    .child(notificationId)
                    .setValue(notificationData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Notificación guardada en queue: $type")
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "❌ Error guardando en queue: ${error.message}")
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en saveToNotificationQueue: ${e.message}")
            }
        }
    }

    private fun showThroughNotificationManager(
        type: String,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        serviceScope.launch {
            try {
                val notificationManager = AppNotificationManager.getInstance(applicationContext)

                val notificationData = AppNotificationManager.NotificationData(
                    id = "${System.currentTimeMillis()}_${type}_fcm",
                    type = type,
                    title = title,
                    body = message,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    amount = data["amount"],
                    unit = data["unit"],
                    ban_type = data["ban_type"],
                    expires_at = data["expires_at"]
                )

                // Procesar directamente sin pasar por Firebase
                notificationManager.processDirectNotification(notificationData)

                Log.d(TAG, "✅ Notificación procesada localmente: $type")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error mostrando notificación: ${e.message}")
            }
        }
    }

    private fun getCurrentPlayerId(): String {
        return try {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PLAYER_ID, "") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo playerId: ${e.message}")
            ""
        }
    }

    private fun updateTokenInFirebase(playerId: String, token: String) {
        if (playerId.isEmpty()) return

        serviceScope.launch {
            try {
                val tokenData = mapOf(
                    "token" to token,
                    "timestamp" to ServerValue.TIMESTAMP,
                    "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "androidVersion" to Build.VERSION.RELEASE,
                    "appVersion" to try {
                        packageManager.getPackageInfo(packageName, 0).versionName
                    } catch (e: Exception) {
                        "unknown"
                    },
                    "lastUpdate" to ServerValue.TIMESTAMP
                )

                firebaseDb.child("playerTokens")
                    .child(playerId)
                    .setValue(tokenData)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Token actualizado para: ***${playerId.takeLast(3)}")
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "❌ Error actualizando token: ${error.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en updateTokenInFirebase: ${e.message}")
            }
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w(TAG, "⚠️ Mensajes FCM eliminados por exceso")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}