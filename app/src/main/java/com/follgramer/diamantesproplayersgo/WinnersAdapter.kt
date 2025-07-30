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
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_WINNER = 0
        private const val TYPE_AD = 1
    }

    // ViewHolder para ganadores
    class WinnerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.date)
        val prize: TextView = view.findViewById(R.id.prize)
        val winnerId: TextView = view.findViewById(R.id.winner_id)
    }

    // ViewHolder para anuncios
    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val adView: AdView = view.findViewById(R.id.adViewWinnerItem)
    }

    override fun getItemViewType(position: Int): Int {
        // Mostrar anuncio después de cada 2 ganadores
        // Posiciones: 0,1 = ganadores, 2 = anuncio, 3,4 = ganadores, 5 = anuncio, etc.
        return if ((position + 1) % 3 == 0) TYPE_AD else TYPE_WINNER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_AD -> {
                val adView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_winner_ad, parent, false)
                AdViewHolder(adView)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_winner, parent, false)
                WinnerViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AdViewHolder -> {
                // Cargar anuncio
                holder.adView.loadAd(AdRequest.Builder().build())
            }
            is WinnerViewHolder -> {
                // Calcular el índice real del ganador (saltando las posiciones de anuncios)
                val winnerIndex = position - (position / 3)

                if (winnerIndex < winners.size) {
                    val winner = winners[winnerIndex]
                    val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale("es", "ES"))
                    holder.date.text = "Sorteo del ${sdf.format(Date(winner.timestamp))}"
                    holder.prize.text = winner.prize
                    holder.winnerId.text = "Ganador ID: ${maskPlayerId(winner.winnerId)}"
                }
            }
        }
    }

    override fun getItemCount(): Int {
        // Calculamos cuántos anuncios necesitamos insertar
        val adCount = winners.size / 2 // Un anuncio cada 2 ganadores
        return winners.size + adCount
    }

    private fun maskPlayerId(id: String): String {
        return if (id.length > 5) "${id.substring(0, 3)}***${id.substring(id.length - 2)}" else id
    }
}