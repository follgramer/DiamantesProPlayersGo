package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout  // ⚠️ AGREGAR ESTE IMPORT
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.follgramer.diamantesproplayersgo.ads.RecyclerViewBannerHelper

class LeaderboardAdapter(
    private val context: Context,
    private val activity: Activity,
    private val items: MutableList<LeaderboardItem>,
    private val currentPlayerId: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PLAYER = 0
        private const val TYPE_AD = 1
        private const val AD_INTERVAL = 5
    }

    override fun getItemViewType(position: Int): Int {
        return if ((position + 1) % (AD_INTERVAL + 1) == 0) TYPE_AD else TYPE_PLAYER
    }

    override fun getItemCount(): Int {
        val playerCount = items.size
        val adCount = if (playerCount > 0) playerCount / AD_INTERVAL else 0
        return playerCount + adCount
    }

    private fun getPlayerPosition(position: Int): Int {
        val adsBefore = position / (AD_INTERVAL + 1)
        return position - adsBefore
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_AD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_leaderboard_ad, parent, false)
                BannerViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_leaderboard, parent, false)
                PlayerViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BannerViewHolder -> {
                holder.bind()
            }
            is PlayerViewHolder -> {
                val playerIndex = getPlayerPosition(position)
                if (playerIndex < items.size) {
                    val item = items[playerIndex]
                    val realRank = playerIndex + 1
                    holder.bind(item, realRank, currentPlayerId)
                }
            }
        }
    }

    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val rank: TextView = view.findViewById(R.id.rank)
        private val playerId: TextView = view.findViewById(R.id.player_id)
        private val tickets: TextView = view.findViewById(R.id.tickets)
        private val passes: TextView = view.findViewById(R.id.passes)

        fun bind(item: LeaderboardItem, rankNumber: Int, currentPlayerId: String?) {
            try {
                val isCurrentPlayer = item.playerId == currentPlayerId

                rank.text = "#$rankNumber"
                when (rankNumber) {
                    1 -> rank.setTextColor(ContextCompat.getColor(itemView.context, R.color.rank_first))
                    2 -> rank.setTextColor(ContextCompat.getColor(itemView.context, R.color.rank_second))
                    3 -> rank.setTextColor(ContextCompat.getColor(itemView.context, R.color.rank_third))
                    else -> rank.setTextColor(ContextCompat.getColor(itemView.context, R.color.rank_other))
                }

                playerId.text = if (isCurrentPlayer) "Tú" else maskPlayerId(item.playerId)
                tickets.text = item.tickets.toString()
                passes.text = if (item.passes == 1L) "1 Pases" else "${item.passes} Pases"

                if (isCurrentPlayer) {
                    playerId.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_color))
                    itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.current_player_bg))
                } else {
                    playerId.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_color))
                    itemView.setBackgroundResource(R.drawable.rounded_background)
                }

            } catch (e: Exception) {
                rank.text = "#-"
                playerId.text = "Error"
                tickets.text = "0"
                passes.text = "0 Pases"
            }
        }

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

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adLeaderboardContainer: FrameLayout = itemView.findViewById(R.id.adLeaderboardContainer)

        fun bind() {
            // IMPORTANTE: No establecer altura 0 ni GONE
            adLeaderboardContainer.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            adLeaderboardContainer.visibility = View.VISIBLE

            (itemView.context as? Activity)?.let { activity ->
                RecyclerViewBannerHelper.loadAdaptiveBanner(
                    activity,
                    adLeaderboardContainer,
                    viewHolderId = bindingAdapterPosition
                )
            }
        }
    }

    fun updateList(newItems: List<LeaderboardItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addPlayer(item: LeaderboardItem) {
        items.add(item)
        notifyItemInserted(getItemCount() - 1)
    }

    fun clearAll() {
        val count = items.size
        items.clear()
        notifyItemRangeRemoved(0, count)
    }

    fun getPlayerCount(): Int = items.size
}