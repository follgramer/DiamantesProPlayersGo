package com.follgramer.diamantesproplayersgo.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.follgramer.diamantesproplayersgo.databinding.DialogHowToPlayBinding

class HowToPlayDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogHowToPlayBinding.inflate(LayoutInflater.from(requireContext()))
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        return AlertDialog.Builder(requireContext())
            .setTitle("¿Cómo jugar?")
            .setView(binding.root)
            .setPositiveButton("Entendido") { _, _ ->
                val showAgain = !binding.dontShowAgain.isChecked
                prefs.edit().putBoolean("show_how_to_play", showAgain).apply()
            }
            .setCancelable(false)
            .create()
    }

    companion object {
        private const val TAG = "HowToPlayDialog"

        fun showIfNeeded(fragmentManager: FragmentManager, context: Context) {
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("show_how_to_play", true)) {
                HowToPlayDialogFragment().show(fragmentManager, TAG)
            }
        }
    }
}