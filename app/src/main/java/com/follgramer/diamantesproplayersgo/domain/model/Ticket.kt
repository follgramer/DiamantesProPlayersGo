package com.follgramer.diamantesproplayersgo.domain.model

data class Ticket(
    val id: String = "",
    val playerId: String = "",
    val amount: Int = 0,
    val source: String = "", // "roulette", "task", "video"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)