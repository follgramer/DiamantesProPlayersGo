package com.follgramer.diamantesproplayersgo.presentation.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.follgramer.diamantesproplayersgo.databinding.ItemWinnerBinding
import com.follgramer.diamantesproplayersgo.databinding.ItemAdBannerBinding
import com.follgramer.diamantesproplayersgo.domain.model.Winner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class WinnersAdapter(
    private val onItemClick: (Int) -> Unit
) : ListAdapter<WinnersAdapter.WinnerItem, RecyclerView.ViewHolder>(WinnerDiffCallback()) {

    companion object {
        private const val TYPE_WINNER = 0
        private const val TYPE_AD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_WINNER -> {
                val binding = ItemWinnerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                WinnerViewHolder(binding)
            }
            TYPE_AD -> {
                val binding = ItemAdBannerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                AdViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WinnerViewHolder -> {
                val winner = (getItem(position) as WinnerItem.WinnerData).winner
                holder.bind(winner)
                holder.itemView.setOnClickListener { onItemClick(position) }
            }
            is AdViewHolder -> {
                holder.bind()
            }
        }
    }

    fun submitWinnersWithAds(winners: List<Winner>) {
        val itemsWithAds = mutableListOf<WinnerItem>()

        winners.forEachIndexed { index, winner ->
            itemsWithAds.add(WinnerItem.WinnerData(winner))

            // Add ad every 2 winners
            if ((index + 1) % 2 == 0 && index < winners.size - 1) {
                itemsWithAds.add(WinnerItem.AdData)
            }
        }

        // If no winners, show at least one ad
        if (winners.isEmpty()) {
            itemsWithAds.add(WinnerItem.AdData)
        }

        submitList(itemsWithAds)
    }

    class WinnerViewHolder(private val binding: ItemWinnerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(winner: Winner) {
            binding.apply {
                tvWinnerName.text = maskPlayerId(winner.winnerId)
                tvPrize.text = winner.prize
                tvDate.text = winner.date
            }
        }

        private fun maskPlayerId(id: String): String {
            return if (id.length >= 5) {
                id.substring(0, 3) + "**" + id.substring(id.length - 2)
            } else {
                id
            }
        }
    }

    class AdViewHolder(private val binding: ItemAdBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            val adRequest = AdRequest.Builder().build()
            // CORREGIDO: Usar adView en lugar de ad_view
            binding.adView.loadAd(adRequest)
        }
    }

    sealed class WinnerItem(val type: Int) {
        data class WinnerData(val winner: Winner) : WinnerItem(TYPE_WINNER)
        object AdData : WinnerItem(TYPE_AD)
    }

    class WinnerDiffCallback : DiffUtil.ItemCallback<WinnerItem>() {
        override fun areItemsTheSame(oldItem: WinnerItem, newItem: WinnerItem): Boolean {
            return when {
                oldItem is WinnerItem.WinnerData && newItem is WinnerItem.WinnerData ->
                    oldItem.winner.id == newItem.winner.id
                oldItem is WinnerItem.AdData && newItem is WinnerItem.AdData -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: WinnerItem, newItem: WinnerItem): Boolean {
            return oldItem == newItem
        }
    }
}