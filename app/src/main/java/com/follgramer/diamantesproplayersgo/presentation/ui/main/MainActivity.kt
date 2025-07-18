package com.follgramer.diamantesproplayersgo.presentation.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.follgramer.diamantesproplayersgo.R
import com.follgramer.diamantesproplayersgo.databinding.ActivityMainBinding
import com.follgramer.diamantesproplayersgo.presentation.ads.AdMobManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adMobManager: AdMobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupAdMob()
        observeViewModel()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav: BottomNavigationView = binding.bottomNavigation
        bottomNav.setupWithNavController(navController)
    }

    private fun setupAdMob() {
        adMobManager = AdMobManager.getInstance(this)
        adMobManager.initialize()
        adMobManager.loadInterstitialAd()

        // Cargar banner principal
        adMobManager.loadBannerAd(binding.bannerAdView)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.showInterstitial.collect { shouldShow ->
                if (shouldShow) {
                    adMobManager.showInterstitialAd(this@MainActivity)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        adMobManager.destroyBannerAd()
    }
}