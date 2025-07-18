package com.follgramer.diamantesproplayersgo.presentation.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.follgramer.diamantesproplayersgo.databinding.FragmentLeaderboardBinding
import com.follgramer.diamantesproplayersgo.presentation.ui.adapters.LeaderboardAdapter
import com.follgramer.diamantesproplayersgo.presentation.ui.viewmodels.LeaderboardViewModel
import com.follgramer.diamantesproplayersgo.presentation.ads.AdMobManager

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LeaderboardViewModel by viewModels()

    private lateinit var adMobManager: AdMobManager
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
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
        leaderboardAdapter = LeaderboardAdapter()

        binding.recyclerViewLeaderboard.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = leaderboardAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.leaderboard.observe(viewLifecycleOwner) { users ->
            leaderboardAdapter.submitList(users)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}