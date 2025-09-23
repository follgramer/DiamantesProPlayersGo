package com.follgramer.diamantesproplayersgo.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.follgramer.diamantesproplayersgo.R
import com.follgramer.diamantesproplayersgo.database.NotificationEntity
import com.follgramer.diamantesproplayersgo.databinding.ActivityNotificationCenterBinding
import com.follgramer.diamantesproplayersgo.notifications.NotificationEventBus
import com.follgramer.diamantesproplayersgo.notifications.NotificationEvent
import com.follgramer.diamantesproplayersgo.notifications.CounterUpdatedEvent
import com.follgramer.diamantesproplayersgo.notifications.AppNotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationCenterBinding
    private lateinit var notificationManager: AppNotificationManager
    private lateinit var adapter: NotificationAdapter

    private val eventListener: (NotificationEvent) -> Unit = { event ->
        if (event is CounterUpdatedEvent) {
            loadNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar window insets para evitar que tape la barra de estado
        setupWindowInsets()

        notificationManager = AppNotificationManager.getInstance(this)
        NotificationEventBus.subscribe(eventListener)

        setupToolbar()
        setupRecyclerView()
        loadNotifications()
        startAutoRefresh()

        // Marcar todas como leídas después de 3 segundos
        lifecycleScope.launch {
            delay(3000)
            notificationManager.markAllAsRead()
        }
    }

    private fun setupWindowInsets() {
        // Hacer que el contenido no se superponga con la barra de estado
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Configurar color de la barra de estado
        window.statusBarColor = Color.parseColor("#161B22")

        // Aplicar padding para los system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    override fun onDestroy() {
        NotificationEventBus.unsubscribe(eventListener)
        super.onDestroy()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Centro de Notificaciones"
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.markAllReadButton.setOnClickListener {
            markAllAsRead()
        }

        binding.clearAllButton.setOnClickListener {
            showClearConfirmation()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick = { notification ->
                if (!notification.isRead) {
                    markAsRead(notification)
                }
            },
            onItemLongClick = { notification ->
                showDeleteDialog(notification)
            }
        )

        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationCenterActivity)
            adapter = this@NotificationCenterActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            try {
                val notifications = notificationManager.getAllNotifications()

                runOnUiThread {
                    if (notifications.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.notificationsRecyclerView.visibility = View.GONE
                        binding.markAllReadButton.isEnabled = false
                        binding.clearAllButton.isEnabled = false
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.notificationsRecyclerView.visibility = View.VISIBLE
                        binding.markAllReadButton.isEnabled = true
                        binding.clearAllButton.isEnabled = true
                        adapter.submitList(notifications)

                        val unreadCount = notifications.count { !it.isRead }
                        supportActionBar?.title = if (unreadCount > 0) {
                            "Notificaciones ($unreadCount sin leer)"
                        } else {
                            "Centro de Notificaciones"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startAutoRefresh() {
        lifecycleScope.launch {
            while (isActive) {
                loadNotifications()
                delay(10000) // Refrescar cada 10 segundos
            }
        }
    }

    private fun markAsRead(notification: NotificationEntity) {
        lifecycleScope.launch {
            notificationManager.markAsRead(notification.id)
            loadNotifications()
        }
    }

    private fun markAllAsRead() {
        lifecycleScope.launch {
            notificationManager.markAllAsRead()
            loadNotifications()
        }
    }

    private fun showDeleteDialog(notification: NotificationEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar notificación")
            .setMessage("¿Eliminar esta notificación?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteNotification(notification)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteNotification(notification: NotificationEntity) {
        lifecycleScope.launch {
            notificationManager.deleteNotification(notification.id)
            loadNotifications()
        }
    }

    private fun showClearConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar todas las notificaciones")
            .setMessage("¿Eliminar todas las notificaciones? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar todo") { _, _ ->
                clearAllNotifications()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearAllNotifications() {
        lifecycleScope.launch {
            notificationManager.deleteAllNotifications()
            loadNotifications()
        }
    }
}

// Adapter para el RecyclerView
class NotificationAdapter(
    private val onItemClick: (NotificationEntity) -> Unit,
    private val onItemLongClick: (NotificationEntity) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private var notifications = listOf<NotificationEntity>()

    fun submitList(list: List<NotificationEntity>) {
        notifications = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.notificationCard)
        private val iconText: TextView = itemView.findViewById(R.id.notificationIcon)
        private val titleText: TextView = itemView.findViewById(R.id.notificationTitle)
        private val messageText: TextView = itemView.findViewById(R.id.notificationMessage)
        private val timeText: TextView = itemView.findViewById(R.id.notificationTime)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)

        fun bind(notification: NotificationEntity) {
            val (icon, iconColor) = when (notification.type) {
                "win", "winner" -> "🏆" to Color.parseColor("#FFD700")
                "loss", "loser" -> "📢" to Color.parseColor("#00A8FF")
                "gift" -> "🎁" to Color.parseColor("#22c55e")
                "ban" -> "🚫" to Color.parseColor("#EF4444")
                "unban" -> "✅" to Color.parseColor("#22c55e")
                else -> "🔔" to Color.parseColor("#00A8FF")
            }

            iconText.text = icon
            titleText.text = notification.title
            messageText.text = notification.message

            // Formato de tiempo mejorado
            val now = System.currentTimeMillis()
            val diff = now - notification.timestamp
            val timeString = when {
                diff < 60000 -> "Ahora"
                diff < 3600000 -> "${diff / 60000}m"
                diff < 86400000 -> "${diff / 3600000}h"
                else -> SimpleDateFormat("dd/MM", Locale.getDefault())
                    .format(Date(notification.timestamp))
            }
            timeText.text = timeString

            // Indicador de no leído
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

            // Color de fondo según estado
            if (!notification.isRead) {
                card.setCardBackgroundColor(Color.parseColor("#1A2F3F"))
            } else {
                card.setCardBackgroundColor(Color.parseColor("#1F2937"))
            }

            // Click listeners
            itemView.setOnClickListener {
                onItemClick(notification)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(notification)
                true
            }
        }
    }
}