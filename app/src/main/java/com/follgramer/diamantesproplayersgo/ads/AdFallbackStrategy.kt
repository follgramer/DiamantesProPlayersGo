package com.follgramer.diamantesproplayersgo.ads

import android.app.Activity
import android.app.Dialog
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.follgramer.diamantesproplayersgo.R

object AdFallbackStrategy {
    private const val TAG = "AdFallback"

    private const val REDUCED_SPINS = 3
    private const val REDUCED_TICKETS = 10
    private const val COUNTDOWN_SECONDS = 10

    fun handleRewardedAdUnavailable(
        activity: Activity,
        rewardType: String,
        rewardAmount: Int,
        onSuccess: () -> Unit
    ) {
        Log.d(TAG, "🔄 No hay rewarded disponible, mostrando countdown")

        // ✅ Ir directo al countdown, sin verificar intersticial
        showCountdownDialog(activity, rewardType, onSuccess)
    }

    private fun showCountdownDialog(
        activity: Activity,
        rewardType: String,
        onSuccess: () -> Unit
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_countdown, null)
        dialog.setContentView(view)
        dialog.setCancelable(false)

        val titleView = view.findViewById<TextView>(R.id.dialog_title)
        val messageView = view.findViewById<TextView>(R.id.dialog_message)
        val countdownView = view.findViewById<TextView>(R.id.countdown_text)
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel)

        // Configurar textos según el tipo
        if (rewardType == "spins") {
            titleView.text = "🎮 Bonus de Continuidad"
            messageView.text = "Tu actividad ha sido registrada. Desbloqueando 3 giros de respaldo."
        } else {
            titleView.text = "⚡ Procesamiento Rápido"
            messageView.text = "Validando tu actividad. Cargando 10 tickets. Completa más misiones para aumentar recompensas."
        }

        var currentSeconds = COUNTDOWN_SECONDS
        countdownView.text = currentSeconds.toString()

        val timer = object : CountDownTimer((COUNTDOWN_SECONDS * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentSeconds = (millisUntilFinished / 1000).toInt() + 1
                countdownView.text = currentSeconds.toString()
            }

            override fun onFinish() {
                dialog.dismiss()
                onSuccess()
                showReducedRewardMessage(activity, rewardType)
            }
        }

        cancelButton.setOnClickListener {
            timer.cancel()
            dialog.dismiss()
        }

        dialog.show()
        timer.start()
    }

    private fun showReducedRewardMessage(
        activity: Activity,
        rewardType: String
    ) {
        val reducedAmount = if (rewardType == "spins") REDUCED_SPINS else REDUCED_TICKETS
        val message = when (rewardType) {
            "spins" -> "¡Has recibido $reducedAmount giros!"
            "tickets" -> "¡Has ganado $reducedAmount tickets!"
            else -> "¡Recompensa obtenida!"
        }

        MaterialDialog(activity).show {
            title(text = "✨ ¡Listo!")
            message(text = message)
            positiveButton(text = "Continuar")
            cancelable(true)
        }
    }

    fun showNoInternetModal(activity: Activity) {
        MaterialDialog(activity).show {
            title(text = "📡 Sin Conexión")
            message(text = "Necesitas conexión a internet para obtener recompensas.\n\nVerifica tu conexión e intenta nuevamente.")
            positiveButton(text = "Entendido")
            cancelable(true)
        }
    }
}