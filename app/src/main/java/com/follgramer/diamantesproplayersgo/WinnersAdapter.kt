package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.follgramer.diamantesproplayersgo.ads.RecyclerViewBannerHelper

data class WinnerItem(
    val id: String,
    val name: String,
    val prize: String,
    val timestamp: Long = System.currentTimeMillis()
)

class WinnersAdapter(
    private val activity: Activity,
    private val items: MutableList<WinnerItem> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_WINNER = 0
        private const val TYPE_AD = 1
        private const val AD_INTERVAL = 2
        private const val TAG = "WinnersAdapter"
    }

    override fun getItemViewType(position: Int): Int {
        return if ((position + 1) % (AD_INTERVAL + 1) == 0) TYPE_AD else TYPE_WINNER
    }

    override fun getItemCount(): Int {
        val winnerCount = items.size
        val adCount = winnerCount / AD_INTERVAL
        return winnerCount + adCount
    }

    private fun getWinnerPosition(position: Int): Int {
        val adsBefore = position / (AD_INTERVAL + 1)
        return position - adsBefore
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_AD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_winner_ad, parent, false)
                BannerViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_winner, parent, false)
                WinnerVH(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BannerViewHolder -> {
                holder.bind(activity, holder.bindingAdapterPosition)
            }
            is WinnerVH -> {
                val winnerIndex = getWinnerPosition(position)
                if (winnerIndex < items.size) {
                    val item = items[winnerIndex]
                    holder.bind(item)
                }
            }
        }
    }

    class WinnerVH(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.date)
        val prize: TextView = view.findViewById(R.id.prize)
        val winnerId: TextView = view.findViewById(R.id.winner_id)

        fun bind(item: WinnerItem) {
            date.text = getRelativeTimeString(item.timestamp)
            prize.text = item.prize
            winnerId.text = "Ganador ID: ${maskPlayerId(item.name)}"
        }

        private fun maskPlayerId(id: String): String {
            return if (id.length > 5) {
                "${id.substring(0, 3)}***${id.substring(id.length - 2)}"
            } else {
                id
            }
        }

        private fun getRelativeTimeString(timestamp: Long): String {
            if (timestamp <= 0) return "Fecha desconocida"

            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 0 -> "Fecha futura"
                diff < 60 * 1000 -> "Hace menos de 1 minuto"
                diff < 2 * 60 * 1000 -> "Hace 1 minuto"
                diff < 60 * 60 * 1000 -> "Hace ${diff / (60 * 1000)} minutos"
                diff < 2 * 60 * 60 * 1000 -> "Hace 1 hora"
                diff < 24 * 60 * 60 * 1000 -> "Hace ${diff / (60 * 60 * 1000)} horas"
                diff < 2 * 24 * 60 * 60 * 1000 -> "Ayer"
                diff < 7 * 24 * 60 * 60 * 1000 -> "Hace ${diff / (24 * 60 * 60 * 1000)} días"
                else -> {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(timestamp))
                }
            }
        }
    }

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adContainer: FrameLayout = itemView.findViewById(R.id.adContainer)

        fun bind(activity: Activity, position: Int) {
            // ✅ CRÍTICO: Iniciar completamente oculto
            adContainer.visibility = View.GONE
            adContainer.layoutParams.height = 0
            adContainer.background = null

            RecyclerViewBannerHelper.loadAdaptiveBanner(
                activity,
                adContainer,
                viewHolderId = position + 1000
            )
        }
    }

    fun submitList(newItems: List<WinnerItem>) {
        items.clear()
        val sortedItems = newItems.sortedByDescending { it.timestamp }
        items.addAll(sortedItems)
        notifyDataSetChanged()
    }

    fun addWinner(winnerItem: WinnerItem) {
        items.add(0, winnerItem)
        notifyItemInserted(0)

        if (items.size > 50) {
            val removedCount = items.size - 50
            repeat(removedCount) {
                items.removeAt(items.size - 1)
            }
            notifyItemRangeRemoved(50, removedCount)
        }
    }

    fun clearAll() {
        val count = items.size
        items.clear()
        notifyItemRangeRemoved(0, count)
    }
}