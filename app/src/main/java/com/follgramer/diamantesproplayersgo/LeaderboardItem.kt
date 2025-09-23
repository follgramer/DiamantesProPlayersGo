package com.follgramer.diamantesproplayersgo

data class LeaderboardItem(
    val playerId: String,
    val playerName: String,
    val passes: Long,
    val tickets: Long = 0L
) {
    // Función para obtener ID enmascarado
    fun getMaskedPlayerId(): String {
        return if (playerId.length > 5) {
            "${playerId.substring(0, 3)}***${playerId.substring(playerId.length - 2)}"
        } else {
            playerId
        }
    }

    // Función para verificar si es válido
    fun isValid(): Boolean {
        return playerId.isNotEmpty() && playerId.matches(Regex("\\d+")) && playerId.length >= 5
    }

    // Función para obtener texto de pases formateado
    fun getFormattedPasses(): String {
        return when {
            passes == 0L -> "Sin pases"
            passes == 1L -> "1 Pase"
            else -> "$passes Pases"
        }
    }

    // Función para obtener texto de tickets formateado
    fun getFormattedTickets(): String {
        return when {
            tickets == 0L -> "0"
            tickets == 1L -> "1"
            else -> "$tickets"
        }
    }

    // Función para calcular puntuación total para ordenamiento
    fun getTotalScore(): Long {
        return (passes * 1000) + tickets
    }
}