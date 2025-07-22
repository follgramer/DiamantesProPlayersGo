package com.follgramer.diamantesproplayersgo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.text.SimpleDateFormat
import java.util.*

class WinnersAdapter(private val winners: List<Winner>) :
    RecyclerView.Adapter<WinnersAdapter.WinnerViewHolder>() {

    class WinnerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.date)
        val prize: TextView = view.findViewById(R.id.prize)
        val winnerId: TextView = view.findViewById(R.id.winner_id)
        val adView: AdView? = view.findViewById(R.id.adViewWinnerItem)
        val contentLayout: View = view.findViewById(R.id.winner_content_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WinnerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_winner, parent, false)
        return WinnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WinnerViewHolder, position: Int) {
        // Mostrar un anuncio cada 3 ganadores
        if (position > 0 && position % 3 == 0) {
            holder.adView?.visibility = View.VISIBLE
            holder.contentLayout.visibility = View.GONE
            holder.adView?.loadAd(AdRequest.Builder().build())
        } else {
            holder.adView?.visibility = View.GONE
            holder.contentLayout.visibility = View.VISIBLE

            val winner = winners[position]
            val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale("es", "ES"))
            holder.date.text = "Sorteo del ${sdf.format(Date(winner.timestamp))}"
            holder.prize.text = winner.prize
            holder.winnerId.text = "Ganador ID: ${maskPlayerId(winner.winnerId)}"
        }
    }

    override fun getItemCount() = winners.size

    private fun maskPlayerId(id: String): String {
        return if (id.length > 5) "${id.substring(0, 3)}***${id.substring(id.length - 2)}" else id
    }
}
