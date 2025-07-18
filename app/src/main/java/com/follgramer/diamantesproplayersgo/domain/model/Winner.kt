package com.follgramer.diamantesproplayersgo.domain.model

data class Winner(
    val id: String = "",
    val winnerId: String = "",
    val prize: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis()
)