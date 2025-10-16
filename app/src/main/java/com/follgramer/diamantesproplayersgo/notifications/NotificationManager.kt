package com.follgramer.diamantesproplayersgo.notifications

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.follgramer.diamantesproplayersgo.MainActivity
import com.follgramer.diamantesproplayersgo.R
import com.google.firebase.database.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class AppNotificationManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppNotificationManager"
        private const val CHANNEL_GENERAL = "general_notifications"
        private const val CHANNEL_WINNER = "winner_notifications"
        private const val CHANNEL_GIFT = "gift_notifications"
        private const val CHANNEL_BAN = "ban_notifications"

        private const val MAX_NOTIFICATION_AGE = 24 * 60 * 60 * 1000L
        private const val NOTIFICATION_EXPIRY = 7 * 24 * 60 * 60 * 1000L

        @Volatile
        private var INSTANCE: AppNotificationManager? = null

        fun getInstance(context: Context): AppNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppNotificationManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Almacenamiento temporal en memoria (sin base de datos)
    private val inMemoryNotifications = mutableListOf<NotificationData>()
    private var unreadCount = 0

    private val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager
    private val processedNotifications = ConcurrentHashMap<String, Long>()
    private val firebaseDb = FirebaseDatabase.getInstance(
        "https://diamantes-pro-players-go-f3510-default-rtdb.firebaseio.com/"
    ).reference

    private var currentListener: ChildEventListener? = null
    private var currentPlayerId: String? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        createNotificationChannels()
        cleanupOldNotifications()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val winnerChannel = NotificationChannel(
                CHANNEL_WINNER,
                "Notificaciones de Ganadores",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando ganas el sorteo"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "Notificaciones Generales",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones generales del club"
                enableLights(true)
                lightColor = Color.BLUE
            }

            val giftChannel = NotificationChannel(
                CHANNEL_GIFT,
                "Regalos del Admin",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de regalos"
                enableLights(true)
                lightColor = Color.GREEN
            }

            val banChannel = NotificationChannel(
                CHANNEL_BAN,
                "Notificaciones de Moderación",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones importantes del sistema"
                enableLights(true)
                lightColor = Color.RED
            }

            val channels = listOf(winnerChannel, generalChannel, giftChannel, banChannel)
            channels.forEach { systemNotificationManager.createNotificationChannel(it) }
        }
    }

    fun startListening(playerId: String) {
        if (playerId.isEmpty()) return

        stopListening()
        currentPlayerId = playerId

        val notifRef = firebaseDb.child("notificationQueue").child(playerId)

        currentListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                scope.launch {
                    processNotification(snapshot)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                scope.launch {
                    processNotification(snapshot)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error en listener: ${error.message}")
            }
        }

        notifRef.addChildEventListener(currentListener!!)
        Log.d(TAG, "✅ Listener de notificaciones iniciado para: $playerId")
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun processNotification(snapshot: DataSnapshot) {
        try {
            val notifId = snapshot.key ?: return
            val data = snapshot.value as? Map<String, Any> ?: return

            val processed = data["processed"] as? Boolean ?: false
            if (processed) {
                snapshot.ref.removeValue()
                return
            }

            val timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis()
            val age = System.currentTimeMillis() - timestamp

            if (age > MAX_NOTIFICATION_AGE) {
                Log.d(TAG, "Notificación muy vieja, eliminando: $notifId")
                snapshot.ref.removeValue()
                return
            }

            if (processedNotifications.containsKey(notifId)) {
                snapshot.ref.removeValue()
                return
            }

            val notification = NotificationData(
                id = notifId,
                type = data["type"] as? String ?: "general",
                title = data["title"] as? String ?: "Notificación",
                body = data["body"] as? String,
                message = data["message"] as? String ?: data["body"] as? String ?: "",
                timestamp = timestamp,
                amount = data["amount"] as? String,
                unit = data["unit"] as? String,
                ban_type = data["ban_type"] as? String,
                expires_at = data["expires_at"]?.toString(),
                sound = (data["sound"] as? Boolean) ?: true,
                vibrate = (data["vibrate"] as? Boolean) ?: true,
                isRead = false
            )

            // Guardar en memoria
            synchronized(inMemoryNotifications) {
                inMemoryNotifications.add(notification)
                if (!notification.isRead) {
                    unreadCount++
                }
            }

            // Mostrar notificación solo si es reciente
            if (age < 5 * 60 * 1000) {
                withContext(Dispatchers.Main) {
                    showNotification(notification)
                }
            }

            processedNotifications[notifId] = System.currentTimeMillis()
            NotificationEventBus.post(CounterUpdatedEvent)

            delay(1000)
            snapshot.ref.removeValue()

        } catch (e: Exception) {
            Log.e(TAG, "Error procesando notificación: ${e.message}")
            snapshot.ref.removeValue()
        }
    }

    private fun showNotification(data: NotificationData) {
        try {
            when (data.type) {
                "win", "winner" -> handleWinnerNotification(data)
                "loss", "loser" -> handleLoserNotification(data)
                "gift" -> handleGiftNotification(data)
                "ban" -> handleBanNotification(data)
                "unban" -> handleUnbanNotification(data)
                else -> handleGeneralNotification(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando notificación: ${e.message}")
        }
    }

    private fun handleWinnerNotification(data: NotificationData) {
        val finalMessage = data.message.ifEmpty {
            "🎉 ¡Felicitaciones! Has ganado el sorteo semanal."
        }
        showSystemNotification(
            channelId = CHANNEL_WINNER,
            title = "🏆 ¡FELICITACIONES, GANADOR!",
            message = finalMessage,
            priority = NotificationCompat.PRIORITY_HIGH,
            notificationType = "winner"
        )
        NotificationEventBus.post(WinnerEvent(finalMessage))
    }

    private fun handleLoserNotification(data: NotificationData) {
        val finalMessage = data.message.ifEmpty {
            "El sorteo ha finalizado. ¡Sigue participando!"
        }
        showSystemNotification(
            channelId = CHANNEL_GENERAL,
            title = "📢 Sorteo Finalizado",
            message = finalMessage,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            notificationType = "loser"
        )
        NotificationEventBus.post(LoserEvent(finalMessage))
    }

    // ✅ FUNCIÓN CORREGIDA
    private fun handleGiftNotification(data: NotificationData) {
        // Construir mensaje en español correcto
        val cantidad = data.amount?.toIntOrNull() ?: 0
        val giftMessage = when (data.unit) {
            "passes" -> {
                if (cantidad == 1) {
                    "Has recibido $cantidad pase"
                } else {
                    "Has recibido $cantidad pases"
                }
            }
            "tickets" -> {
                if (cantidad == 1) {
                    "Has recibido $cantidad ticket"
                } else {
                    "Has recibido $cantidad tickets"
                }
            }
            "spins" -> {
                if (cantidad == 1) {
                    "Has recibido $cantidad giro"
                } else {
                    "Has recibido $cantidad giros"
                }
            }
            else -> "¡Tienes un regalo del administrador!"
        }

        showSystemNotification(
            channelId = CHANNEL_GIFT,
            title = "🎁 Regalo Recibido",  // ✅ Traducido
            message = giftMessage,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            notificationType = "gift"
        )

        NotificationEventBus.post(GiftEvent(
            amount = data.amount ?: "0",
            unit = data.unit ?: "unknown",
            message = giftMessage  // ✅ Usar mensaje en español
        ))
    }


    private fun handleBanNotification(data: NotificationData) {
        showSystemNotification(
            channelId = CHANNEL_BAN,
            title = if (data.ban_type == "permanent") "🚫 Cuenta Suspendida"
            else "⚠️ Suspensión Temporal",
            message = data.message,
            priority = NotificationCompat.PRIORITY_HIGH,
            notificationType = "ban"
        )
        NotificationEventBus.post(BanEvent(
            banType = data.ban_type ?: "unknown",
            reason = data.message,
            expiresAt = data.expires_at?.toLongOrNull() ?: 0L
        ))
    }

    private fun handleUnbanNotification(data: NotificationData) {
        showSystemNotification(
            channelId = CHANNEL_GENERAL,
            title = "✅ Cuenta Reactivada",
            message = data.message.ifEmpty {
                "Tu cuenta ha sido reactivada. ¡Bienvenido de vuelta!"
            },
            priority = NotificationCompat.PRIORITY_DEFAULT,
            notificationType = "unban"
        )
        NotificationEventBus.post(UnbanEvent)
    }

    private fun handleGeneralNotification(data: NotificationData) {
        showSystemNotification(
            channelId = CHANNEL_GENERAL,
            title = data.title,
            message = data.message,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            notificationType = "general"
        )
        NotificationEventBus.post(GeneralEvent(
            title = data.title,
            message = data.message
        ))
    }

    private fun showSystemNotification(
        channelId: String,
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        vibrationPattern: LongArray? = null,
        color: Int = ContextCompat.getColor(context, R.color.accent_color),
        notificationType: String = "general"
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("from_notification", true)
                putExtra("open_notification_center", true)
                putExtra("notification_type", notificationType)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(priority)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(color)
                .build()

            systemNotificationManager.notify(System.currentTimeMillis().toInt(), notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando notificación: ${e.message}")
        }
    }

    fun stopListening() {
        currentListener?.let { listener ->
            currentPlayerId?.let { playerId ->
                firebaseDb.child("notificationQueue").child(playerId)
                    .removeEventListener(listener)
            }
        }
        currentListener = null
        currentPlayerId = null
        scope.coroutineContext.cancelChildren()
    }

    private fun cleanupOldNotifications() {
        scope.launch {
            while (isActive) {
                try {
                    val cutoffTime = System.currentTimeMillis() - NOTIFICATION_EXPIRY
                    synchronized(inMemoryNotifications) {
                        inMemoryNotifications.removeIf { it.timestamp < cutoffTime }
                    }
                    processedNotifications.entries.removeIf {
                        System.currentTimeMillis() - it.value > MAX_NOTIFICATION_AGE
                    }
                    delay(60 * 60 * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en limpieza: ${e.message}")
                }
            }
        }
    }

    // Funciones públicas para acceso externo

    suspend fun getUnreadCount(): Int = unreadCount

    suspend fun getAllNotifications(): List<NotificationData> =
        synchronized(inMemoryNotifications) {
            inMemoryNotifications.toList()
        }

    suspend fun markAsRead(notificationId: String) {
        synchronized(inMemoryNotifications) {
            inMemoryNotifications.find { it.id == notificationId }?.let { notification ->
                notification.isRead = true
                unreadCount = maxOf(0, unreadCount - 1)
            }
        }
        NotificationEventBus.post(CounterUpdatedEvent)
    }

    suspend fun markAllAsRead() {
        synchronized(inMemoryNotifications) {
            inMemoryNotifications.forEach { it.isRead = true }
            unreadCount = 0
        }
        NotificationEventBus.post(CounterUpdatedEvent)
    }

    suspend fun deleteNotification(notificationId: String) {
        synchronized(inMemoryNotifications) {
            val notification = inMemoryNotifications.find { it.id == notificationId }
            if (notification != null) {
                if (!notification.isRead) {
                    unreadCount = maxOf(0, unreadCount - 1)
                }
                inMemoryNotifications.remove(notification)
            }
        }
        NotificationEventBus.post(CounterUpdatedEvent)
    }

    suspend fun deleteAllNotifications() {
        synchronized(inMemoryNotifications) {
            inMemoryNotifications.clear()
            unreadCount = 0
        }
        NotificationEventBus.post(CounterUpdatedEvent)
    }

    fun processDirectNotification(data: NotificationData) {
        scope.launch {
            try {
                if (processedNotifications.containsKey(data.id)) {
                    Log.d(TAG, "Notificación ya procesada: ${data.id}")
                    return@launch
                }

                synchronized(inMemoryNotifications) {
                    inMemoryNotifications.add(data)
                    if (!data.isRead) {
                        unreadCount++
                    }
                }

                withContext(Dispatchers.Main) {
                    showNotification(data)
                }

                processedNotifications[data.id] = System.currentTimeMillis()
                NotificationEventBus.post(CounterUpdatedEvent)

            } catch (e: Exception) {
                Log.e(TAG, "Error procesando notificación directa: ${e.message}")
            }
        }
    }

    fun showSystemNotification(
        type: String,
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        val channelId = when (type) {
            "winner", "win" -> CHANNEL_WINNER
            "gift" -> CHANNEL_GIFT
            "ban" -> CHANNEL_BAN
            else -> CHANNEL_GENERAL
        }

        showSystemNotification(
            channelId = channelId,
            title = title,
            message = message,
            priority = priority,
            notificationType = type
        )
    }

    data class NotificationData(
        val id: String,
        val type: String,
        val title: String,
        val body: String?,
        val message: String,
        val timestamp: Long,
        val amount: String? = null,
        val unit: String? = null,
        val ban_type: String? = null,
        val expires_at: String? = null,
        val sound: Boolean = true,
        val vibrate: Boolean = true,
        var isRead: Boolean = false
    )
}// Updated: 2025-10-15 14:29:27
