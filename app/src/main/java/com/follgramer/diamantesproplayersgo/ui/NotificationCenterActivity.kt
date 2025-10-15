package com.follgramer.diamantesproplayersgo.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.follgramer.diamantesproplayersgo.databinding.ActivityNotificationCenterBinding
import com.follgramer.diamantesproplayersgo.notifications.AppNotificationManager
import com.follgramer.diamantesproplayersgo.notifications.CounterUpdatedEvent
import com.follgramer.diamantesproplayersgo.notifications.NotificationEventBus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationCenterBinding
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var notificationContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationManager = AppNotificationManager.getInstance(this)

        setupToolbar()
        setupViews()
        loadNotifications()

        NotificationEventBus.subscribe { event ->
            if (event is CounterUpdatedEvent) {
                runOnUiThread {
                    loadNotifications()
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Centro de Notificaciones"
        }
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupViews() {
        // Crear un contenedor simple para las notificaciones
        notificationContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }

        // Agregar el contenedor al root
        (binding.root as? LinearLayout)?.addView(notificationContainer)
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            val notifications = notificationManager.getAllNotifications()

            // Limpiar contenedor
            notificationContainer.removeAllViews()

            if (notifications.isEmpty()) {
                // Mostrar mensaje vacío
                val emptyText = TextView(this@NotificationCenterActivity).apply {
                    text = "No hay notificaciones"
                    textSize = 16f
                    setTextColor(Color.GRAY)
                    gravity = Gravity.CENTER
                    setPadding(32, 64, 32, 64)
                }
                notificationContainer.addView(emptyText)
            } else {
                // Mostrar notificaciones
                val sortedNotifications = notifications.sortedByDescending { it.timestamp }
                sortedNotifications.forEach { notification ->
                    val notificationView = createNotificationView(notification)
                    notificationContainer.addView(notificationView)
                }
            }

            updateUnreadCount()
        }
    }

    private fun createNotificationView(notification: AppNotificationManager.NotificationData): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            // Color de fondo si no está leído
            if (!notification.isRead) {
                setBackgroundColor(Color.parseColor("#1A00A8FF"))
            } else {
                setBackgroundColor(Color.TRANSPARENT)
            }

            // Icono y título
            val titleRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL

                val icon = TextView(context).apply {
                    text = when (notification.type) {
                        "winner", "win" -> "🏆"
                        "loser", "loss" -> "😢"
                        "gift" -> "🎁"
                        "ban" -> "🚫"
                        "unban" -> "✅"
                        else -> "📢"
                    }
                    textSize = 20f
                    setPadding(0, 0, 16, 0)
                }
                addView(icon)

                val title = TextView(context).apply {
                    text = notification.title
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }
                addView(title)

                val time = TextView(context).apply {
                    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                    text = dateFormat.format(Date(notification.timestamp))
                    textSize = 12f
                    setTextColor(Color.LTGRAY)
                }
                addView(time)
            }
            addView(titleRow)

            // Mensaje
            val message = TextView(context).apply {
                text = notification.message
                textSize = 14f
                setTextColor(Color.parseColor("#CCCCCC"))
                setPadding(36, 8, 0, 0)
            }
            addView(message)

            // Separador
            val divider = View(context).apply {
                setBackgroundColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    topMargin = 16
                }
            }
            addView(divider)

            // Click listener
            setOnClickListener {
                lifecycleScope.launch {
                    notificationManager.markAsRead(notification.id)
                    loadNotifications()
                }
            }
        }
    }

    private suspend fun updateUnreadCount() {
        val unreadCount = notificationManager.getUnreadCount()
        supportActionBar?.subtitle = if (unreadCount > 0) {
            "$unreadCount sin leer"
        } else {
            "Todas leídas"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NotificationEventBus.unsubscribe { }
    }
}// Updated: 2025-10-15 14:29:27
