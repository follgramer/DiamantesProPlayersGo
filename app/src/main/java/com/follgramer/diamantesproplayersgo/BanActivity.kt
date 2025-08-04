// ========================================
// ARCHIVO: BanActivity.kt - VERSIÓN CORREGIDA
// ========================================
package com.follgramer.diamantesproplayersgo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.follgramer.diamantesproplayersgo.databinding.ActivityBanBinding
import java.text.SimpleDateFormat
import java.util.*

class BanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBanBinding
    private var countdownTimer: CountDownTimer? = null

    companion object {
        const val EXTRA_BAN_TYPE = "ban_type"
        const val EXTRA_BAN_REASON = "ban_reason"
        const val EXTRA_EXPIRES_AT = "expires_at"
        const val EXTRA_PLAYER_ID = "player_id"

        const val BAN_TYPE_TEMPORARY = "temporary"
        const val BAN_TYPE_PERMANENT = "permanent"

        private const val CONTACT_EMAIL = "contacto@follgramer.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val banType = intent.getStringExtra(EXTRA_BAN_TYPE) ?: BAN_TYPE_PERMANENT
        val reason = intent.getStringExtra(EXTRA_BAN_REASON) ?: getString(R.string.default_ban_reason)
        val expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, 0L)
        val playerId = intent.getStringExtra(EXTRA_PLAYER_ID) ?: ""

        setupUI(banType, reason, expiresAt, playerId)
        setupClickListeners()
    }

    private fun setupUI(banType: String, reason: String, expiresAt: Long, playerId: String) {
        when (banType) {
            BAN_TYPE_TEMPORARY -> {
                binding.banIcon.text = "⚠️"
                binding.banTitle.text = getString(R.string.temporary_ban_title)
                binding.banSubtitle.text = getString(R.string.temporary_ban_subtitle)

                if (expiresAt > 0) {
                    startCountdown(expiresAt)
                    binding.timeRemaining.visibility = android.view.View.VISIBLE
                    binding.countdownContainer.visibility = android.view.View.VISIBLE
                } else {
                    binding.timeRemaining.visibility = android.view.View.GONE
                    binding.countdownContainer.visibility = android.view.View.GONE
                }

                binding.retryButton.visibility = android.view.View.VISIBLE
                binding.retryButton.text = getString(R.string.verify_status)
            }
            BAN_TYPE_PERMANENT -> {
                binding.banIcon.text = "🚫"
                binding.banTitle.text = getString(R.string.permanent_ban_title)
                binding.banSubtitle.text = getString(R.string.permanent_ban_subtitle)
                binding.timeRemaining.visibility = android.view.View.GONE
                binding.countdownContainer.visibility = android.view.View.GONE
                binding.retryButton.visibility = android.view.View.GONE
            }
        }

        binding.banReason.text = reason
        binding.playerId.text = getString(R.string.player_id_format, playerId)
        binding.contactEmail.text = CONTACT_EMAIL
    }

    private fun setupClickListeners() {
        binding.contactButton.setOnClickListener {
            sendEmailToSupport()
        }

        binding.retryButton.setOnClickListener {
            checkBanStatusAgain()
        }

        binding.exitButton.setOnClickListener {
            finishAffinity()
        }
    }

    private fun startCountdown(expiresAt: Long) {
        val currentTime = System.currentTimeMillis()
        val timeRemaining = expiresAt - currentTime

        if (timeRemaining <= 0) {
            // El baneo ya expiró, verificar estado
            checkBanStatusAgain()
            return
        }

        countdownTimer = object : CountDownTimer(timeRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateCountdownDisplay(millisUntilFinished)
            }

            override fun onFinish() {
                binding.countdownTimer.text = getString(R.string.suspension_ended)
                binding.retryButton.text = getString(R.string.continue_to_app)
                binding.retryButton.setBackgroundColor(ContextCompat.getColor(this@BanActivity, R.color.success_color))
            }
        }.start()
    }

    private fun updateCountdownDisplay(millisUntilFinished: Long) {
        val hours = millisUntilFinished / (1000 * 60 * 60)
        val minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millisUntilFinished % (1000 * 60)) / 1000

        binding.countdownTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

        // Mostrar fecha de expiración
        val expiryDate = System.currentTimeMillis() + millisUntilFinished
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.expiryDate.text = getString(R.string.expires_at_format, dateFormat.format(Date(expiryDate)))
    }

    private fun sendEmailToSupport() {
        try {
            val playerId = intent.getStringExtra(EXTRA_PLAYER_ID) ?: ""
            val banType = intent.getStringExtra(EXTRA_BAN_TYPE) ?: BAN_TYPE_PERMANENT

            val subject = getString(R.string.email_subject, playerId)
            val body = getString(
                R.string.email_body,
                playerId,
                if (banType == BAN_TYPE_TEMPORARY) getString(R.string.temporary) else getString(R.string.permanent),
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            )

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_support_email)))
            } else {
                // Fallback: copiar email al portapapeles
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(getString(R.string.support_email), CONTACT_EMAIL)
                clipboard.setPrimaryClip(clip)

                android.widget.Toast.makeText(
                    this,
                    getString(R.string.email_copied, CONTACT_EMAIL),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("BanActivity", "Error enviando email: ${e.message}")
            android.widget.Toast.makeText(this, getString(R.string.error_sending_email), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBanStatusAgain() {
        val playerId = intent.getStringExtra(EXTRA_PLAYER_ID) ?: ""
        if (playerId.isEmpty()) {
            finishAffinity()
            return
        }

        binding.retryButton.isEnabled = false
        binding.retryButton.text = getString(R.string.verifying)

        BanChecker(this).checkBanStatus(playerId) { banStatus ->
            when (banStatus) {
                is BanChecker.BanStatus.NotBanned -> {
                    // Usuario ya no está baneado, continuar a la app
                    android.widget.Toast.makeText(this, getString(R.string.suspension_lifted), android.widget.Toast.LENGTH_LONG).show()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is BanChecker.BanStatus.TemporaryBan -> {
                    // Aún está baneado temporalmente
                    binding.retryButton.isEnabled = true
                    binding.retryButton.text = getString(R.string.verify_status)
                    startCountdown(banStatus.expiresAt)
                    android.widget.Toast.makeText(this, getString(R.string.suspension_still_active), android.widget.Toast.LENGTH_SHORT).show()
                }
                is BanChecker.BanStatus.PermanentBan -> {
                    // Aún está baneado permanentemente
                    binding.retryButton.isEnabled = true
                    binding.retryButton.text = getString(R.string.verify_status)
                    android.widget.Toast.makeText(this, getString(R.string.permanent_suspension_active), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }

    override fun onBackPressed() {
        // No permitir salir de esta pantalla con el botón atrás
        // El usuario debe usar el botón "Salir de la App"
    }
}