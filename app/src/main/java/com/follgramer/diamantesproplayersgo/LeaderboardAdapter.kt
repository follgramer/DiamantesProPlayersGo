package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.follgramer.diamantesproplayersgo.ads.NativeAdHelper
import com.google.android.gms.ads.nativead.NativeAdView

class LeaderboardAdapter(
    private val context: Context,
    private val activity: Activity,
    private val items: MutableList<LeaderboardItem>,
    private val currentPlayerId: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PLAYER = 0
        private const val TYPE_AD = 1
        private const val AD_INTERVAL = 3
        private const val TAG = "LeaderboardAdapter"
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
                    .inflate(R.layout.item_leaderboard_native_ad, parent, false)
                NativeAdViewHolder(view)
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
            is NativeAdViewHolder -> {
                holder.bind(activity, holder.bindingAdapterPosition)
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

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is NativeAdViewHolder) {
            holder.destroy()
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
                passes.text = if (item.passes == 1L) "1 Pase" else "${item.passes} Pases"

                if (isCurrentPlayer) {
                    playerId.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_color))
                    itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.current_player_bg))
                } else {
                    playerId.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_color))
                    itemView.setBackgroundResource(R.drawable.rounded_background)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error binding player: ${e.message}")
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
                Log.e(TAG, "Error masking ID: ${e.message}")
                "***"
            }
        }
    }

    class NativeAdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adContainer: FrameLayout? = itemView.findViewById(R.id.leaderboard_native_ad_container)
        private var holderId: Int = 0

        fun bind(activity: Activity, position: Int) {
            adContainer?.let { container ->
                try {
                    // ✅ CRÍTICO: Iniciar completamente oculto
                    container.visibility = View.GONE
                    container.layoutParams.height = 0
                    container.removeAllViews()

                    // ID único para este holder
                    holderId = position + 2000

                    // Inflar el layout del anuncio nativo
                    val nativeAdView = LayoutInflater.from(activity)
                        .inflate(R.layout.ad_native_unified, container, false) as NativeAdView

                    // Cargar anuncio nativo
                    NativeAdHelper.loadNativeAd(activity, container, nativeAdView, holderId)

                    Log.d(TAG, "Cargando anuncio nativo en posición $position (holder $holderId)")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading native ad: ${e.message}", e)
                    container.visibility = View.GONE
                    container.layoutParams.height = 0
                }
            } ?: run {
                Log.e(TAG, "⚠️ Ad container no encontrado")
            }
        }

        fun destroy() {
            NativeAdHelper.destroyNativeAd(holderId)
        }
    }

    fun updateList(newItems: List<LeaderboardItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addPlayer(item: LeaderboardItem) {
        items.add(item)
        notifyItemInserted(itemCount - 1)
    }

    fun clearAll() {
        val count = items.size
        items.clear()
        notifyItemRangeRemoved(0, count)
    }

    fun getPlayerCount(): Int = items.size
}