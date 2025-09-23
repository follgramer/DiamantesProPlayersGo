package com.follgramer.diamantesproplayersgo

import com.google.firebase.database.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.*

@IgnoreExtraProperties
data class Winner(
    val winnerId: String? = null,  // ✅ Campo principal que usa el admin panel
    val prize: String? = null,
    val timestamp: Long? = null,
    val week: String? = null,
    val date: String? = null
) {
    // ✅ COMPATIBILIDAD: Getter para código existente que use playerId
    val playerId: String?
        get() = winnerId

    // ✅ Método para ID enmascarado (privacidad)
    fun getMaskedWinnerId(): String {
        val id = winnerId ?: ""
        return if (id.length > 5) {
            "${id.substring(0, 3)}***${id.substring(id.length - 2)}"
        } else {
            id
        }
    }

    // ✅ Método para obtener fecha formateada
    fun getFormattedDate(): String {
        return try {
            if (timestamp != null && timestamp > 0) {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            } else {
                date ?: "Fecha no disponible"
            }
        } catch (e: Exception) {
            date ?: "Fecha no disponible"
        }
    }

    // ✅ Verificar si es un ganador válido
    fun isValid(): Boolean {
        return !winnerId.isNullOrEmpty() &&
                !prize.isNullOrEmpty() &&
                timestamp != null &&
                timestamp > 0
    }

    // ✅ Obtener tiempo relativo (hace cuánto ganó)
    fun getRelativeTime(): String {
        if (timestamp == null || timestamp <= 0) return "Fecha desconocida"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "Hace menos de 1 minuto"
            diff < 60 * 60 * 1000 -> "Hace ${diff / (60 * 1000)} minutos"
            diff < 24 * 60 * 60 * 1000 -> "Hace ${diff / (60 * 60 * 1000)} horas"
            diff < 7 * 24 * 60 * 60 * 1000 -> "Hace ${diff / (24 * 60 * 60 * 1000)} días"
            else -> getFormattedDate()
        }
    }

    // ✅ Para debugging
    override fun toString(): String {
        return "Winner(winnerId=${getMaskedWinnerId()}, prize=$prize, timestamp=$timestamp)"
    }
}