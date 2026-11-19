package com.example.analizermeow.ui

import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.analizermeow.R
import com.example.analizermeow.channels.ChannelAnalyzer

/**
 * Pinta cada canal como una fila con sus estrellas (doradas las llenas,
 * grises las vacías) y resalta en cian el número del canal actual o
 * cualquiera de los recomendados — igual de informativo que el original,
 * sin necesitar íconos nuevos.
 */
class ChannelAdapter(
    private val items: List<ChannelAnalyzer.ChannelScore>,
    private val currentChannel: Int,
    private val recommended: List<Int>
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.item_channel_number)
        val stars: TextView = view.findViewById(R.id.item_channel_stars)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val score = items[position]
        val context = holder.itemView.context

        holder.number.text = score.channel.toString()
        val highlight = score.channel == currentChannel || recommended.contains(score.channel)
        holder.number.setTextColor(
            ContextCompat.getColor(context, if (highlight) R.color.meow_accent else R.color.meow_text_primary)
        )

        val filledColor = ContextCompat.getColor(context, R.color.meow_star_filled)
        val emptyColor = ContextCompat.getColor(context, R.color.meow_star_empty)
        val builder = SpannableStringBuilder()
        repeat(10) { i ->
            val star = "★"
            val start = builder.length
            builder.append(star)
            val color = if (i < score.stars) filledColor else emptyColor
            builder.setSpan(ForegroundColorSpan(color), start, start + star.length, 0)
        }
        holder.stars.text = builder
    }
}
