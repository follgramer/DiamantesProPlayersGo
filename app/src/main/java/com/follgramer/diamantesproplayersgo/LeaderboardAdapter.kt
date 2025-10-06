package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.follgramer.diamantesproplayersgo.ads.NativeAdHelper
import com.google.android.gms.ads.nativead.NativeAdView

class LeaderboardAdapter(
    private val context: Context,
    private val activity: Activity,
    private val items: MutableList<LeaderboardItem>,
    private var currentPlayerId: String?
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
        return if (viewType == TYPE_AD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaderboard_native_ad, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaderboard, parent, false)
            PlayerViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PlayerViewHolder) {
            val playerPosition = getPlayerPosition(position)
            if (playerPosition < items.size) {
                val item = items[playerPosition]
                holder.bind(item, playerPosition + 1, currentPlayerId)
            }
        } else if (holder is AdViewHolder) {
            holder.bind(position)
        }
    }

    fun updateData(newItems: List<LeaderboardItem>, newCurrentPlayerId: String?) {
        items.clear()
        items.addAll(newItems)
        currentPlayerId = newCurrentPlayerId
        notifyDataSetChanged()
        Log.d(TAG, "Datos actualizados: ${items.size} items, total con anuncios: $itemCount")
    }

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankText: TextView = itemView.findViewById(R.id.rank)
        private val playerIdText: TextView = itemView.findViewById(R.id.player_id)
        private val passesText: TextView = itemView.findViewById(R.id.passes)
        private val ticketsText: TextView = itemView.findViewById(R.id.tickets)

        fun bind(item: LeaderboardItem, rank: Int, currentPlayerId: String?) {
            rankText.text = "#$rank"
            playerIdText.text = item.playerName
            passesText.text = item.passes.toString()
            ticketsText.text = item.tickets.toString()

            if (item.playerId == currentPlayerId) {
                itemView.setBackgroundResource(R.drawable.bg_modal_rounded)
            } else {
                itemView.background = null
            }
        }
    }

    inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: ViewGroup = itemView.findViewById(R.id.leaderboard_native_ad_container)

        fun bind(position: Int) {
            val holderId = System.identityHashCode(this)

            try {
                container.visibility = View.VISIBLE
                container.layoutParams = container.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                if (activity.isFinishing || activity.isDestroyed) {
                    Log.w(TAG, "Activity está finalizando, no se puede cargar anuncio")
                    container.visibility = View.GONE
                    return
                }

                // Buscar el NativeAdView dentro del container
                val nativeAdView = container.findViewById<NativeAdView>(R.id.leaderboard_native_ad_view)
                if (nativeAdView != null) {
                    container.post {
                        NativeAdHelper.loadNativeAd(activity, container, nativeAdView, holderId)
                        Log.d(TAG, "Cargando anuncio nativo en posición $position (holder $holderId)")
                    }
                } else {
                    Log.e(TAG, "NativeAdView no encontrado en el layout")
                    container.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading native ad: ${e.message}", e)
                container.visibility = View.GONE
                container.layoutParams.height = 0
            }
        }

        fun destroy() {
            val holderId = System.identityHashCode(this)
            NativeAdHelper.destroyNativeAd(holderId)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is AdViewHolder) {
            holder.destroy()
        }
    }
}