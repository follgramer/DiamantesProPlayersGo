package com.follgramer.diamantesproplayersgo.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object Onboarding {
    private const val PREFS = "onboarding_prefs"
    private const val KEY_SKIP = "skip_onboarding"

    private fun shouldShow(activity: Activity): Boolean {
        return !activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
            .getBoolean(KEY_SKIP, false)
    }

    fun showIfNeeded(activity: Activity) {
        if (!shouldShow(activity)) return

        val check = CheckBox(activity).apply {
            text = "No mostrar este tutorial otra vez"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
        }

        val startButton = Button(activity).apply {
            text = "🚀 ¡EMPEZAR A GANAR!"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            val drawable = GradientDrawable().apply {
                setColor(0xFF00A8FF.toInt())
                cornerRadius = 25f
            }
            background = drawable

            setPadding(
                (32 * resources.displayMetrics.density).toInt(),
                (8 * resources.displayMetrics.density).toInt(),
                (32 * resources.displayMetrics.density).toInt(),
                (8 * resources.displayMetrics.density).toInt()
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (40 * resources.displayMetrics.density).toInt()
            ).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
            }
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)

            val modalDrawable = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 25f
            }
            background = modalDrawable

            // Botón X de cerrar
            val closeContainer = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                addView(TextView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })

                addView(TextView(activity).apply {
                    text = "✕"
                    textSize = 18f
                    setTextColor(0xFF666666.toInt())
                    gravity = android.view.Gravity.CENTER
                    setPadding(
                        (12 * resources.displayMetrics.density).toInt(),
                        (12 * resources.displayMetrics.density).toInt(),
                        (12 * resources.displayMetrics.density).toInt(),
                        (12 * resources.displayMetrics.density).toInt()
                    )
                })
            }
            addView(closeContainer)

            addView(TextView(activity).apply {
                text = "🎮 ¡BIENVENIDO!"
                textSize = 24f
                setTextColor(0xFF000000.toInt())
                gravity = android.view.Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
            })

            addView(TextView(activity).apply {
                text = "DIAMANTES PRO PLAYERS"
                textSize = 14f
                setTextColor(0xFF1976D2.toInt())
                gravity = android.view.Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, (20 * resources.displayMetrics.density).toInt())
            })

            addView(TextView(activity).apply {
                text = """1⃣ Gira la ruleta y gana hasta 100 tickets
2⃣ Cada 1,000 tickets = 1 pase de sorteo  
3⃣ Sorteos de 1,000 diamantes todos los domingos
4⃣ Premios entregados en menos de 24 horas"""
                textSize = 15f
                setTextColor(0xFF000000.toInt())
                setLineSpacing(6f, 1f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, (20 * resources.displayMetrics.density).toInt())
            })

            val checkContainer = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
                addView(check)
            }
            addView(checkContainer)
            addView(startButton)
        }

        val dialog = AlertDialog.Builder(activity)
            .setView(content)
            .setCancelable(true)
            .create()

        val closeButton = content.getChildAt(0).let { container ->
            (container as LinearLayout).getChildAt(1) as TextView
        }
        closeButton.setOnClickListener { dialog.dismiss() }

        startButton.setOnClickListener {
            if (check.isChecked) {
                activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                    .edit().putBoolean(KEY_SKIP, true).apply()
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}