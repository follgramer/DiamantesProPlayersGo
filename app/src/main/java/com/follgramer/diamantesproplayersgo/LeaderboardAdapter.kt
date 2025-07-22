package com.follgramer.diamantesproplayersgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(private val players: List<Player>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    class LeaderboardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rank: TextView = view.findViewById(R.id.rank)
        val playerId: TextView = view.findViewById(R.id.player_id)
        val passes: TextView = view.findViewById(R.id.passes)
        val tickets: TextView = view.findViewById(R.id.tickets)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_full, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val player = players[position]
        holder.rank.text = "#${position + 1}"
        holder.playerId.text = maskPlayerId(player.playerId)
        holder.passes.text = player.passes.toString()
        holder.tickets.text = player.tickets.toString()
    }

    override fun getItemCount() = players.size

    private fun maskPlayerId(id: String): String {
        return if (id.length > 5) "${id.substring(0, 3)}***${id.substring(id.length - 2)}" else id
    }
}
