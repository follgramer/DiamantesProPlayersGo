package com.follgramer.diamantesproplayersgo.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.follgramer.diamantesproplayersgo.databinding.FragmentWinnersBinding
import com.follgramer.diamantesproplayersgo.presentation.ui.adapters.WinnersAdapter
import com.follgramer.diamantesproplayersgo.presentation.ui.viewmodels.HomeViewModel
import com.follgramer.diamantesproplayersgo.presentation.ads.AdMobManager

class WinnersFragment : Fragment() {

    private var _binding: FragmentWinnersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var adMobManager: AdMobManager
    private lateinit var winnersAdapter: WinnersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWinnersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdMob()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupAdMob() {
        adMobManager = AdMobManager.getInstance(requireContext())
        adMobManager.loadBannerAd(binding.bannerAdView)
    }

    private fun setupRecyclerView() {
        winnersAdapter = WinnersAdapter { position ->
            // Show interstitial ad every 2 items in winners list
            if (position % 2 == 0) {
                adMobManager.showInterstitialAd(requireActivity())
            }
        }

        binding.recyclerViewWinners.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = winnersAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.winners.observe(viewLifecycleOwner) { winners ->
            winnersAdapter.submitWinnersWithAds(winners)

            // Show/hide empty state
            if (winners.isEmpty()) {
                binding.recyclerViewWinners.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.recyclerViewWinners.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}