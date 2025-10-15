package com.follgramer.diamantesproplayersgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MiniLeaderboardAdapter(
    private val players: List<Player>,
    private val currentPlayerId: String?
) : RecyclerView.Adapter<MiniLeaderboardAdapter.MiniViewHolder>() {

    class MiniViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rank: TextView = view.findViewById(R.id.rank)
        val playerId: TextView = view.findViewById(R.id.player_id)
        val passes: TextView = view.findViewById(R.id.passes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_mini, parent, false)
        return MiniViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiniViewHolder, position: Int) {
        try {
            if (position < 0 || position >= players.size) {
                return
            }

            val player = players[position]
            val rankNumber = position + 1
            val isCurrentPlayer = player.playerId == currentPlayerId

            // Configurar ranking con colores especiales para top 3
            holder.rank.text = "#$rankNumber"
            when (rankNumber) {
                1 -> {
                    holder.rank.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_gold))
                }
                2 -> {
                    holder.rank.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_silver))
                }
                3 -> {
                    holder.rank.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_bronze))
                }
                else -> {
                    holder.rank.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_other))
                }
            }

            // Mostrar "Tú" si es el jugador actual
            holder.playerId.text = if (isCurrentPlayer) {
                "Tú"
            } else {
                maskPlayerId(player.playerId)
            }

            // Pases - formato más limpio
            val passesText = if (player.passes == 1L) "1 Pase" else "${player.passes} Pases"
            holder.passes.text = passesText

            // Color especial para el jugador actual
            if (isCurrentPlayer) {
                holder.playerId.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.accent_color))
                // Hacer toda la fila más destacada
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.state_selected))
            } else {
                holder.playerId.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_color))
                holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, android.R.color.transparent))
            }

            // Configurar colores para los pases según el ranking
            when (rankNumber) {
                1 -> holder.passes.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_gold))
                2 -> holder.passes.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_silver))
                3 -> holder.passes.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_bronze))
                else -> holder.passes.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_secondary))
            }

        } catch (e: Exception) {
            // Valores fallback en caso de error
            holder.rank.text = "#-"
            holder.playerId.text = "Error"
            holder.passes.text = "0 Pases"

            // Colores de fallback
            holder.rank.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_muted))
            holder.playerId.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_muted))
            holder.passes.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_muted))
        }
    }

    override fun getItemCount() = players.size

    /**
     * Enmascara el ID del jugador para privacidad
     */
    private fun maskPlayerId(id: String): String {
        return try {
            when {
                id.length <= 3 -> id
                id.length <= 5 -> "${id.substring(0, 2)}***"
                id.length <= 8 -> "${id.substring(0, 3)}***${id.takeLast(2)}"
                else -> "${id.substring(0, 3)}***${id.takeLast(3)}"
            }
        } catch (e: Exception) {
            "***"
        }
    }

    /**
     * Actualizar la lista de jugadores
     */
    fun updatePlayers(newPlayers: List<Player>) {
        try {
            // En una implementación más robusta, usarías DiffUtil aquí
            notifyDataSetChanged()
        } catch (e: Exception) {
            // Log del error pero no crash
        }
    }

    /**
     * Obtener la posición del jugador actual
     */
    fun getCurrentPlayerPosition(): Int {
        return try {
            players.indexOfFirst { it.playerId == currentPlayerId }
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Verificar si un jugador está en el top 3
     */
    private fun isTopPlayer(position: Int): Boolean {
        return position < 3
    }

    /**
     * Obtener color según la posición
     */
    private fun getRankColor(context: android.content.Context, position: Int): Int {
        return when (position + 1) {
            1 -> ContextCompat.getColor(context, R.color.rank_gold)
            2 -> ContextCompat.getColor(context, R.color.rank_silver)
            3 -> ContextCompat.getColor(context, R.color.rank_bronze)
            else -> ContextCompat.getColor(context, R.color.rank_other)
        }
    }
}