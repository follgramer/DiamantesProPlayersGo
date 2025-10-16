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
        private const val AD_INTERVAL = 5 // ✅ Anuncio cada 5 items
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
            playerIdText.text = item.getMaskedPlayerId()
            passesText.text = item.passes.toString()
            ticketsText.text = item.tickets.toString()

            // Highlight del jugador actual
            if (item.playerId == currentPlayerId) {
                itemView.setBackgroundResource(R.drawable.bg_highlighted_player)
            } else {
                itemView.setBackgroundResource(R.drawable.background_container)
            }
        }
    }

    inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ✅ Obtener el NativeAdView directamente desde itemView
        private val nativeAdView: NativeAdView? = itemView.findViewById(R.id.leaderboard_native_ad_view)

        fun bind(position: Int) {
            val holderId = System.identityHashCode(this)

            try {
                // ✅ Verificar que el NativeAdView existe
                if (nativeAdView == null) {
                    Log.e(TAG, "❌ NativeAdView no encontrado en el layout inflado")
                    itemView.visibility = View.GONE
                    itemView.layoutParams = itemView.layoutParams.apply {
                        height = 0
                    }
                    return
                }

                if (activity.isFinishing || activity.isDestroyed) {
                    Log.w(TAG, "⚠️ Activity está finalizando, no se puede cargar anuncio")
                    itemView.visibility = View.GONE
                    itemView.layoutParams = itemView.layoutParams.apply {
                        height = 0
                    }
                    return
                }

                // ✅ Obtener el container padre (FrameLayout)
                val container = nativeAdView.parent as? ViewGroup
                if (container == null) {
                    Log.e(TAG, "❌ Container no encontrado")
                    itemView.visibility = View.GONE
                    itemView.layoutParams = itemView.layoutParams.apply {
                        height = 0
                    }
                    return
                }

                // Configurar visibilidad y altura
                container.visibility = View.VISIBLE
                container.layoutParams = container.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                // ✅ Cargar el anuncio nativo
                container.post {
                    NativeAdHelper.loadNativeAd(activity, container, nativeAdView, holderId)
                    Log.d(TAG, "✅ Cargando anuncio nativo en posición $position (holder $holderId)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading native ad: ${e.message}", e)
                itemView.visibility = View.GONE
                itemView.layoutParams = itemView.layoutParams.apply {
                    height = 0
                }
            }
        }

        fun destroy() {
            val holderId = System.identityHashCode(this)
            NativeAdHelper.destroyNativeAd(holderId)
            Log.d(TAG, "🗑️ Anuncio nativo destruido para holder $holderId")
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is AdViewHolder) {
            holder.destroy()
        }
    }
}// Updated: 2025-10-15 14:29:27
