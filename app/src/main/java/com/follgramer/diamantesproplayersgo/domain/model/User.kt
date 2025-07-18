package com.follgramer.diamantesproplayersgo.domain.model

data class User(
    val playerId: String = "",
    val userName: String = "",
    val tickets: Int = 0,
    val passes: Int = 0,
    val totalSpins: Int = 0,
    val lastSpinTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)