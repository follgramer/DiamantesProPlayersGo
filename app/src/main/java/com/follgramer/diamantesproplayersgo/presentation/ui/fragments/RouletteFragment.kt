package com.follgramer.diamantesproplayersgo.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.follgramer.diamantesproplayersgo.databinding.FragmentRouletteBinding
import com.follgramer.diamantesproplayersgo.presentation.ui.viewmodels.RouletteViewModel
import com.follgramer.diamantesproplayersgo.presentation.ads.AdMobManager

class RouletteFragment : Fragment() {

    private var _binding: FragmentRouletteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RouletteViewModel by viewModels()

    private lateinit var adMobManager: AdMobManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouletteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdMob()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupAdMob() {
        adMobManager = AdMobManager.getInstance(requireContext())
        adMobManager.loadBannerAd(binding.bannerAdView)
    }

    private fun setupClickListeners() {
        binding.btnSpin.setOnClickListener {
            if (viewModel.canSpin.value == true) {
                viewModel.spinRoulette()
            }
        }

        binding.btnBuyTicket.setOnClickListener {
            viewModel.purchaseTicket()
        }
    }

    private fun observeViewModel() {
        viewModel.playerData.observe(viewLifecycleOwner) { user ->
            binding.apply {
                tvPlayerName.text = user.userName
                tvTicketsCount.text = user.tickets.toString()
                tvPassesCount.text = user.passes.toString()
            }
        }

        viewModel.canSpin.observe(viewLifecycleOwner) { canSpin ->
            binding.btnSpin.isEnabled = canSpin
        }

        viewModel.isSpinning.observe(viewLifecycleOwner) { isSpinning ->
            if (isSpinning) {
                spinWheel()
            }
        }

        viewModel.spinResult.observe(viewLifecycleOwner) { result ->
            if (result > 0) {
                handleSpinResult(result)
            }
        }

        viewModel.prizeWon.observe(viewLifecycleOwner) { prize ->
            if (prize.isNotEmpty()) {
                showPrizeDialog(prize)
            }
        }

        viewModel.showRewardDialog.observe(viewLifecycleOwner) { show ->
            if (show) {
                showAd()
            }
        }
    }

    private fun spinWheel() {
        // Animate the roulette wheel
        binding.rouletteWheel.animate()
            .rotation(binding.rouletteWheel.rotation + 1440f) // 4 full rotations
            .setDuration(3000)
            .start()
    }

    private fun handleSpinResult(result: Int) {
        // Handle the spin result - stop wheel at correct position
        val finalRotation = (result - 1) * 45f // Each segment is 45 degrees
        binding.rouletteWheel.animate()
            .rotation(finalRotation)
            .setDuration(500)
            .start()
    }

    private fun showPrizeDialog(prize: String) {
        // Show prize dialog
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("¡Felicidades!")
            .setMessage("Has ganado: $prize")
            .setPositiveButton("OK") { _, _ ->
                viewModel.dismissRewardDialog()
            }
            .show()
    }

    private fun showAd() {
        adMobManager.showInterstitialAd(requireActivity())
        viewModel.dismissRewardDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}