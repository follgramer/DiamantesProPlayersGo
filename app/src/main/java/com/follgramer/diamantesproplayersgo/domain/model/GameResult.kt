package com.follgramer.diamantesproplayersgo.domain.model

data class GameResult(
    val id: String = "",
    val playerId: String = "",
    val spinResult: Int = 0,
    val ticketsWon: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)