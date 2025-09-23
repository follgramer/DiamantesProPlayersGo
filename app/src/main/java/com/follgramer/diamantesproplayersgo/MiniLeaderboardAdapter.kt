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
                    holder.rank.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_first))
                }
                2 -> {
                    holder.rank.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_second))
                }
                3 -> {
                    holder.rank.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.rank_third))
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

            // Pases - CAMBIO AQUÍ: formato más limpio
            val passesText = if (player.passes == 1L) "1 Pases" else "${player.passes} Pases"
            holder.passes.text = passesText

            // Color especial para el jugador actual
            if (isCurrentPlayer) {
                holder.playerId.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.accent_color))
            } else {
                holder.playerId.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_color))
            }

        } catch (e: Exception) {
            // Valores fallback en caso de error
            holder.rank.text = "#-"
            holder.playerId.text = "Error"
            holder.passes.text = "0 Pases"
        }
    }

    override fun getItemCount() = players.size

    private fun maskPlayerId(id: String): String {
        return try {
            if (id.length > 5) {
                "${id.substring(0, 3)}***${id.substring(id.length - 2)}"
            } else {
                id
            }
        } catch (e: Exception) {
            "***"
        }
    }
}