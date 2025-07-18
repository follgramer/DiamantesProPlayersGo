package com.follgramer.diamantesproplayersgo.presentation.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.follgramer.diamantesproplayersgo.data.repository.GameRepository
import com.follgramer.diamantesproplayersgo.domain.model.User
import com.follgramer.diamantesproplayersgo.domain.model.Winner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class RouletteViewModel : ViewModel() {

    private val gameRepository = GameRepository()

    private val _playerData = MutableLiveData<User>()
    val playerData: LiveData<User> = _playerData

    private val _spinResult = MutableLiveData<Int>()
    val spinResult: LiveData<Int> = _spinResult

    private val _prizeWon = MutableLiveData<String>()
    val prizeWon: LiveData<String> = _prizeWon

    private val _canSpin = MutableLiveData<Boolean>()
    val canSpin: LiveData<Boolean> = _canSpin

    private val _isSpinning = MutableLiveData<Boolean>()
    val isSpinning: LiveData<Boolean> = _isSpinning

    private val _showRewardDialog = MutableLiveData<Boolean>()
    val showRewardDialog: LiveData<Boolean> = _showRewardDialog

    init {
        loadPlayerData()
        _canSpin.value = true
        _isSpinning.value = false
        _showRewardDialog.value = false
    }

    private fun loadPlayerData() {
        viewModelScope.launch {
            try {
                val user = gameRepository.getPlayerData()
                _playerData.value = user
                _canSpin.value = user.tickets > 0
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun spinRoulette() {
        val currentUser = _playerData.value ?: return
        if (!canSpin.value!! || currentUser.tickets <= 0) return

        viewModelScope.launch {
            try {
                _isSpinning.value = true
                _canSpin.value = false

                // Use ticket
                useTicket()

                // Simulate spinning animation delay
                delay(3000)

                // Generate random result (1-8 for roulette segments)
                val result = Random.nextInt(1, 9)
                _spinResult.value = result

                // Determine prize based on result
                val prize = when (result) {
                    1 -> "🎫 5 Tickets"
                    2 -> "💎 100 Diamantes"
                    3 -> "🎫 10 Tickets"
                    4 -> "💎 200 Diamantes"
                    5 -> "🎫 15 Tickets"
                    6 -> "💎 500 Diamantes"
                    7 -> "🏆 1 Pase"
                    8 -> "💎 1000 Diamantes"
                    else -> "🎫 1 Ticket"
                }

                _prizeWon.value = prize
                processPrize(result)
                _showRewardDialog.value = true

            } catch (e: Exception) {
                // Handle error
            } finally {
                _isSpinning.value = false
                _canSpin.value = (_playerData.value?.tickets ?: 0) > 0
            }
        }
    }

    private suspend fun processPrize(result: Int) {
        when (result) {
            1 -> gameRepository.addTickets(5)
            2 -> { /* Add 100 diamonds - implement when diamonds system is ready */ }
            3 -> gameRepository.addTickets(10)
            4 -> { /* Add 200 diamonds */ }
            5 -> gameRepository.addTickets(15)
            6 -> { /* Add 500 diamonds */ }
            7 -> addPasses(1)
            8 -> { /* Add 1000 diamonds */ }
        }
        loadPlayerData() // Refresh user data
    }

    private suspend fun useTicket() {
        val currentUser = _playerData.value ?: return
        val updatedUser = currentUser.copy(
            tickets = currentUser.tickets - 1,
            totalSpins = currentUser.totalSpins + 1,
            lastSpinTime = System.currentTimeMillis()
        )
        gameRepository.updateUserData(updatedUser)
    }

    private suspend fun addPasses(amount: Int) {
        val currentUser = _playerData.value ?: return
        val updatedUser = currentUser.copy(passes = currentUser.passes + amount)
        gameRepository.updateUserData(updatedUser)
    }

    fun purchaseTicket() {
        viewModelScope.launch {
            if (canBuyTicket()) {
                val result = gameRepository.buyTicket()
                if (result.isSuccess) {
                    loadPlayerData()
                }
            }
        }
    }

    private fun canBuyTicket(): Boolean {
        // Implement logic for buying tickets (e.g., using diamonds)
        return gameRepository.canBuyTicket()
    }

    fun dismissRewardDialog() {
        _showRewardDialog.value = false
    }

    fun resetSpin() {
        _spinResult.value = 0
        _prizeWon.value = ""
    }
}