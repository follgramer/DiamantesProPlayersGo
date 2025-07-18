package com.follgramer.diamantesproplayersgo.utils

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Long.toDateString(): String {
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return format.format(Date(this))
}

fun String.maskPlayerId(): String {
    return if (this.length >= 5) {
        this.substring(0, 3) + "**" + this.substring(this.length - 2)
    } else {
        this
    }
}

fun Int.formatNumber(): String {
    return when {
        this >= 1000000 -> "${this / 1000000}M"
        this >= 1000 -> "${this / 1000}K"
        else -> this.toString()
    }
}

fun calculateProgress(current: Int, target: Int): Int {
    return if (target > 0) {
        ((current.toFloat() / target.toFloat()) * 100).toInt().coerceAtMost(100)
    } else {
        0
    }
}

fun getNextSundayTimestamp(): Long {
    val calendar = Calendar.getInstance()
    val currentWeekDay = calendar.get(Calendar.DAY_OF_WEEK)
    val daysUntilSunday = (Calendar.SUNDAY - currentWeekDay + 7) % 7
    val daysToAdd = if (daysUntilSunday == 0) 7 else daysUntilSunday

    calendar.add(Calendar.DAY_OF_WEEK, daysToAdd)
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)

    return calendar.timeInMillis
}

fun formatCountdown(millisUntilFinished: Long): String {
    val days = millisUntilFinished / (1000 * 60 * 60 * 24)
    val hours = (millisUntilFinished % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
    val minutes = (millisUntilFinished % (1000 * 60 * 60)) / (1000 * 60)

    return "${days}d ${hours}h ${minutes}m"
}