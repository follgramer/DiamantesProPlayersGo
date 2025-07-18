package com.follgramer.diamantesproplayersgo.presentation.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.follgramer.diamantesproplayersgo.databinding.ItemLeaderboardBinding
import com.follgramer.diamantesproplayersgo.domain.model.User

class LeaderboardAdapter : ListAdapter<User, LeaderboardAdapter.LeaderboardViewHolder>(LeaderboardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, position + 1)
    }

    class LeaderboardViewHolder(
        private val binding: ItemLeaderboardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User, rank: Int) {
            binding.apply {
                tvRank.text = "#$rank"
                tvPlayerName.text = user.userName
                tvPasses.text = user.passes.toString()
                tvTickets.text = user.tickets.toString()

                // Set rank color based on position
                val rankIcon = when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "#$rank"
                }
                tvRank.text = rankIcon
            }
        }
    }

    private class LeaderboardDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.playerId == newItem.playerId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}