package com.follgramer.diamantesproplayersgo

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BanActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BAN_TYPE = "ban_type"
        const val EXTRA_BAN_REASON = "ban_reason"
        const val EXTRA_EXPIRES_AT = "expires_at"
        const val EXTRA_PLAYER_ID = "player_id"
    }

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solución para la advertencia de deprecación
        setupStatusBar()

        // Manejo moderno del botón de retroceso
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No hacer nada - previene que el usuario regrese
            }
        })

        val banType = intent.getStringExtra(EXTRA_BAN_TYPE) ?: "temporary"
        val banReason = intent.getStringExtra(EXTRA_BAN_REASON) ?: "Violación de términos de servicio"
        val expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, 0L)
        val playerId = intent.getStringExtra(EXTRA_PLAYER_ID) ?: ""

        setupBanScreen(banType, banReason, expiresAt, playerId)
    }

    private fun setupStatusBar() {
        // Método moderno para configurar la barra de estado
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false

        // Solo usar statusBarColor en versiones que lo soportan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.parseColor("#1A1A1A")
        }
    }

    private fun setupBanScreen(banType: String, reason: String, expiresAt: Long, playerId: String) {
        val message = when (banType) {
            "permanent" -> {
                "Tu cuenta ha sido suspendida permanentemente.\n\nMotivo: $reason\n\nSi crees que esto es un error, contacta a soporte técnico."
            }
            "temporary" -> {
                val timeRemaining = expiresAt - System.currentTimeMillis()
                if (timeRemaining > 0) {
                    val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60
                    "Tu cuenta ha sido suspendida temporalmente.\n\nMotivo: $reason\n\nTiempo restante: ${hours}h ${minutes}m"
                } else {
                    "Tu suspensión ha expirado. Puedes intentar acceder nuevamente."
                }
            }
            else -> "Tu cuenta está suspendida."
        }

        AlertDialog.Builder(this)
            .setTitle("Cuenta Suspendida")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Contactar Soporte") { _, _ ->
                openEmailApp(playerId, reason)
            }
            .setNegativeButton(if (banType == "temporary") "Reintentar" else "Salir") { _, _ ->
                if (banType == "temporary" && expiresAt <= System.currentTimeMillis()) {
                    checkBanStatusAndRetry(playerId)
                } else {
                    finishAffinity()
                }
            }
            .show()
    }

    private fun openEmailApp(playerId: String, reason: String) {
        try {
            val subject = "Solicitud de Revisión de Suspensión - ID: $playerId"
            val body = """
                Hola equipo de soporte,
                
                Mi cuenta con ID $playerId ha sido suspendida.
                Motivo: $reason
                Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}
                
                Me gustaría solicitar una revisión del caso.
                
                Gracias.
            """.trimIndent()

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("follgramer@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            startActivity(Intent.createChooser(emailIntent, "Enviar email"))
        } catch (e: Exception) {
            Log.e("BanActivity", "Error opening email: ${e.message}")
        }
    }

    private fun checkBanStatusAndRetry(playerId: String) {
        BanChecker(this).checkBanStatus(playerId) { banStatus ->
            runOnUiThread {
                when (banStatus) {
                    is BanChecker.BanStatus.NotBanned -> {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        finish()
                    }
                    else -> {
                        AlertDialog.Builder(this)
                            .setTitle("Aún Suspendido")
                            .setMessage("Tu cuenta sigue suspendida.")
                            .setPositiveButton("OK") { _, _ -> finishAffinity() }
                            .show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    // NO incluir el método onBackPressed deprecado
}// Updated: 2025-10-15 14:29:27
