package com.follgramer.diamantesproplayersgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MiniLeaderboardAdapter(private val players: List<Player>, private val currentPlayerId: String?) :
    RecyclerView.Adapter<MiniLeaderboardAdapter.MiniViewHolder>() {

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
        val player = players[position]
        holder.rank.text = "#${position + 1}"
        holder.playerId.text = if (player.playerId == currentPlayerId) "Tú" else maskPlayerId(player.playerId)
        holder.passes.text = "${player.passes} Pases"

        if (player.playerId == currentPlayerId) {
            holder.itemView.setBackgroundResource(R.drawable.background_my_rank_highlight)
        } else {
            // Asegúrate de que no tenga un fondo si no es el jugador actual
            holder.itemView.background = null
        }
    }

    override fun getItemCount() = players.size

    private fun maskPlayerId(id: String): String {
        return if (id.length > 5) "${id.substring(0, 3)}***${id.substring(id.length - 2)}" else id
    }
}
