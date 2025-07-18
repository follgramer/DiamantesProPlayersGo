package com.follgramer.diamantesproplayersgo.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.follgramer.diamantesproplayersgo.databinding.FragmentHomeBinding
import com.follgramer.diamantesproplayersgo.presentation.ui.adapters.WinnersAdapter
import com.follgramer.diamantesproplayersgo.presentation.ui.viewmodels.HomeViewModel
import com.follgramer.diamantesproplayersgo.presentation.ads.AdMobManager
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var adMobManager: AdMobManager
    private lateinit var winnersAdapter: WinnersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdMob()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupAdMob() {
        adMobManager = AdMobManager.getInstance(requireContext())
        adMobManager.loadBannerAd(binding.bannerAdView)
        adMobManager.loadRewardedAd()
    }

    private fun setupRecyclerView() {
        winnersAdapter = WinnersAdapter { position ->
            // Show interstitial ad every 3 items
            if (position % 3 == 0) {
                showInterstitialAd()
            }
        }

        binding.recyclerViewGanadores.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = winnersAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnJugar.setOnClickListener {
            viewModel.navigateToGame()
        }

        binding.btnRuleta.setOnClickListener {
            viewModel.navigateToRuleta()
        }

        binding.btnRecompensa.setOnClickListener {
            showRewardedAd()
        }
    }

    private fun observeViewModel() {
        viewModel.playerData.observe(viewLifecycleOwner) { player ->
            binding.apply {
                tvPlayerName.text = player.userName
                tvDiamantes.text = player.tickets.toString()
                tvPases.text = player.passes.toString()
                tvTickets.text = player.tickets.toString()
            }
        }

        // CORREGIDO: Usar submitWinnersWithAds en lugar de submitList
        viewModel.winners.observe(viewLifecycleOwner) { winners ->
            winnersAdapter.submitWinnersWithAds(winners)
        }

        viewModel.countdownTime.observe(viewLifecycleOwner) { time ->
            binding.tvCountdown.text = time
        }
    }

    private fun showInterstitialAd() {
        lifecycleScope.launch {
            adMobManager.showInterstitialAd(requireActivity())
        }
    }

    private fun showRewardedAd() {
        lifecycleScope.launch {
            adMobManager.showRewardedAd(requireActivity()) { reward ->
                viewModel.addTickets(reward.amount)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}