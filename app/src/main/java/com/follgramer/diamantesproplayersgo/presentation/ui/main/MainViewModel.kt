package com.follgramer.diamantesproplayersgo.presentation.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainViewModel : ViewModel() {

    private val _showInterstitial = MutableSharedFlow<Boolean>()
    val showInterstitial: SharedFlow<Boolean> = _showInterstitial.asSharedFlow()

    private var lastInterstitialTime = 0L
    private val interstitialInterval = 120_000L // 2 minutos

    fun requestInterstitial() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInterstitialTime >= interstitialInterval) {
            lastInterstitialTime = currentTime
            _showInterstitial.tryEmit(true)
        }
    }
}
