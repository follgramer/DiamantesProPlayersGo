package com.follgramer.diamantesproplayersgo.presentation.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.follgramer.diamantesproplayersgo.data.repository.GameRepository
import com.follgramer.diamantesproplayersgo.domain.model.User
import com.follgramer.diamantesproplayersgo.domain.model.Winner
import com.follgramer.diamantesproplayersgo.utils.CountDownTimerComponent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel : ViewModel() {

    private val gameRepository = GameRepository()

    private val _playerData = MutableLiveData<User>()
    val playerData: LiveData<User> = _playerData

    private val _winners = MutableLiveData<List<Winner>>()
    val winners: LiveData<List<Winner>> = _winners

    private val _countdownTime = MutableLiveData<String>()
    val countdownTime: LiveData<String> = _countdownTime

    private val _navigationEvent = MutableLiveData<String>()
    val navigationEvent: LiveData<String> = _navigationEvent

    init {
        loadPlayerData()
        loadWinners()
        startCountdown()
    }

    private fun loadPlayerData() {
        viewModelScope.launch {
            try {
                val user = gameRepository.getPlayerData()
                _playerData.value = user
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun loadWinners() {
        viewModelScope.launch {
            try {
                val winners = gameRepository.getWinners()
                _winners.value = winners
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun startCountdown() {
        // Countdown to next Sunday 23:59
        val calendar = Calendar.getInstance()
        val currentWeekDay = calendar.get(Calendar.DAY_OF_WEEK)
        val daysUntilSunday = (Calendar.SUNDAY - currentWeekDay + 7) % 7
        val daysToAdd = if (daysUntilSunday == 0) 7 else daysUntilSunday

        calendar.add(Calendar.DAY_OF_WEEK, daysToAdd)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)

        updateCountdown()
    }

    private fun updateCountdown() {
        val calendar = Calendar.getInstance()
        val currentWeekDay = calendar.get(Calendar.DAY_OF_WEEK)
        val daysUntilSunday = (Calendar.SUNDAY - currentWeekDay + 7) % 7
        val daysToAdd = if (daysUntilSunday == 0) 7 else daysUntilSunday

        calendar.add(Calendar.DAY_OF_WEEK, daysToAdd)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)

        val targetTime = calendar.timeInMillis
        val currentTime = System.currentTimeMillis()
        val diff = targetTime - currentTime

        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
        val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)

        _countdownTime.value = "${days}d ${hours}h ${minutes}m"
    }

    fun navigateToGame() {
        _navigationEvent.value = "game"
    }

    fun navigateToRuleta() {
        _navigationEvent.value = "ruleta"
    }

    fun addTickets(amount: Int) {
        viewModelScope.launch {
            try {
                gameRepository.addTickets(amount)
                loadPlayerData()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}