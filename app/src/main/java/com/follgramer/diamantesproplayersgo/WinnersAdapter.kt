package com.follgramer.diamantesproplayersgo

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.follgramer.diamantesproplayersgo.ads.NativeAdHelper
import com.google.android.gms.ads.nativead.NativeAdView
import java.text.SimpleDateFormat
import java.util.*

class WinnersAdapter(
    private val activity: Activity,
    private val items: MutableList<WinnerItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_WINNER = 0
        private const val TYPE_AD = 1
        private const val AD_INTERVAL = 3
        private const val TAG = "WinnersAdapter"
    }

    override fun getItemViewType(position: Int): Int {
        return if ((position + 1) % (AD_INTERVAL + 1) == 0) TYPE_AD else TYPE_WINNER
    }

    override fun getItemCount(): Int {
        val winnerCount = items.size
        val adCount = if (winnerCount > 0) winnerCount / AD_INTERVAL else 0
        return winnerCount + adCount
    }

    private fun getWinnerPosition(position: Int): Int {
        val adsBefore = position / (AD_INTERVAL + 1)
        return position - adsBefore
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_winner_native_ad, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_winner, parent, false)
            WinnerViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is WinnerViewHolder) {
            val winnerPosition = getWinnerPosition(position)
            if (winnerPosition < items.size) {
                holder.bind(items[winnerPosition])
            }
        } else if (holder is AdViewHolder) {
            holder.bind(position)
        }
    }

    fun updateData(newItems: List<WinnerItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
        Log.d(TAG, "Datos actualizados: ${items.size} items, total con anuncios: $itemCount")
    }

    inner class WinnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val winnerIdText: TextView = itemView.findViewById(R.id.winner_id)
        private val prizeText: TextView = itemView.findViewById(R.id.prize)
        private val dateText: TextView = itemView.findViewById(R.id.date)

        fun bind(item: WinnerItem) {
            winnerIdText.text = item.winnerName
            prizeText.text = item.prize

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateText.text = dateFormat.format(Date(item.timestamp))
        }
    }

    inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adContainer: ViewGroup = itemView.findViewById(R.id.winner_native_ad_container)

        fun bind(position: Int) {
            val holderId = System.identityHashCode(this)

            try {
                adContainer.visibility = View.VISIBLE
                adContainer.layoutParams = adContainer.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                if (activity.isFinishing || activity.isDestroyed) {
                    Log.w(TAG, "Activity está finalizando, no se puede cargar anuncio")
                    adContainer.visibility = View.GONE
                    return
                }

                // Buscar el NativeAdView dentro del container
                val nativeAdView = adContainer.findViewById<NativeAdView>(R.id.winner_native_ad_view)
                if (nativeAdView != null) {
                    adContainer.post {
                        NativeAdHelper.loadNativeAd(activity, adContainer, nativeAdView, holderId)
                        Log.d(TAG, "Cargando anuncio nativo en posición $position (holder $holderId)")
                    }
                } else {
                    Log.e(TAG, "NativeAdView no encontrado en el layout")
                    adContainer.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading native ad: ${e.message}", e)
                adContainer.visibility = View.GONE
                adContainer.layoutParams.height = 0
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

data class WinnerItem(
    val winnerId: String,
    val winnerName: String,
    val prize: String,
    val timestamp: Long
)// Updated: 2025-10-15 14:29:27
