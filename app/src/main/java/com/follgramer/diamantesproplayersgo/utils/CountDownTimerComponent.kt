package com.follgramer.diamantesproplayersgo.utils

import android.os.CountDownTimer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class CountDownTimerComponent(
    private val callback: TimerCallback
) : DefaultLifecycleObserver {

    private var countDownTimer: CountDownTimer? = null
    private var timeRemaining: Long = 0
    private var isRunning = false

    interface TimerCallback {
        fun onTick(days: Int, hours: Int, minutes: Int, seconds: Int)
        fun onFinish()
    }

    fun startWeeklyCountdown() {
        val targetTime = getNextSundayTimestamp()
        val currentTime = System.currentTimeMillis()
        val duration = targetTime - currentTime

        start(duration)
    }

    private fun start(durationMillis: Long) {
        timeRemaining = durationMillis
        isRunning = true

        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = millisUntilFinished

                val days = (millisUntilFinished / (1000 * 60 * 60 * 24)).toInt()
                val hours = ((millisUntilFinished % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)).toInt()
                val minutes = ((millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                val seconds = ((millisUntilFinished % (1000 * 60)) / 1000).toInt()

                callback.onTick(days, hours, minutes, seconds)
            }

            override fun onFinish() {
                isRunning = false
                callback.onFinish()
                // Restart automatically for next week
                startWeeklyCountdown()
            }
        }.start()
    }

    override fun onPause(owner: LifecycleOwner) {
        countDownTimer?.cancel()
    }

    override fun onResume(owner: LifecycleOwner) {
        if (isRunning && timeRemaining > 0) {
            start(timeRemaining)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        countDownTimer?.cancel()
    }
}
