package com.follgramer.diamantesproplayersgo.presentation.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.follgramer.diamantesproplayersgo.data.repository.GameRepository
import com.follgramer.diamantesproplayersgo.domain.model.User
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {

    private val gameRepository = GameRepository()

    private val _leaderboard = MutableLiveData<List<User>>()
    val leaderboard: LiveData<List<User>> = _leaderboard

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val leaderboardData = getLeaderboard()
                _leaderboard.value = leaderboardData
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getLeaderboard(): List<User> {
        // Simulated leaderboard data
        return listOf(
            User(
                playerId = "leader1",
                userName = "Pro***87",
                tickets = 2500,
                passes = 15,
                totalSpins = 150
            ),
            User(
                playerId = "leader2",
                userName = "Gam***23",
                tickets = 2200,
                passes = 12,
                totalSpins = 130
            ),
            User(
                playerId = "leader3",
                userName = "Win***45",
                tickets = 1800,
                passes = 10,
                totalSpins = 110
            ),
            User(
                playerId = "leader4",
                userName = "Luc***12",
                tickets = 1500,
                passes = 8,
                totalSpins = 95
            ),
            User(
                playerId = "leader5",
                userName = "Sup***89",
                tickets = 1200,
                passes = 6,
                totalSpins = 80
            )
        ).sortedByDescending { it.passes }
    }

    fun refreshLeaderboard() {
        loadLeaderboard()
    }
}